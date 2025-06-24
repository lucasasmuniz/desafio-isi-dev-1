package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDetailsDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductCouponApplicationRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.DatabaseException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceNotFoundException;
import br.com.lmuniz.desafio.senai.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final ProductCouponApplicationRepository productCouponApplicationRepository;
    private final ObjectMapper objectMapper;

    public CouponService (CouponRepository couponRepository, ProductCouponApplicationRepository productCouponApplicationRepository, ObjectMapper objectMapper) {
        this.couponRepository = couponRepository;
        this.productCouponApplicationRepository = productCouponApplicationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CouponDTO createCoupon(CouponDTO couponDTO){
        final String normalizedCode = Utils.normalizeName(couponDTO.code());

        Integer maxUses = couponDTO.maxUses();
        if(couponDTO.oneShot()){
            maxUses = null;
        }

        Coupon entity = new Coupon(
                normalizedCode,
                CouponEnum.valueOf(couponDTO.type().toUpperCase()),
                couponDTO.value().setScale(2, BigDecimal.ROUND_HALF_UP),
                couponDTO.oneShot(),
                maxUses,
                couponDTO.validFrom(),
                couponDTO.validUntil()
        );
        validateCoupon(entity);

        entity = couponRepository.save(entity);
        return new CouponDTO(entity);
    }

    @Transactional(readOnly = true)
    public List<CouponDTO> getAllCoupons(boolean onlyValid) {
        List<Coupon> result = null;
        if (onlyValid){
            result = couponRepository.searchValidCoupons(Instant.now());
        }else {
            result = couponRepository.findAll();
        }

        return result.stream().map(CouponDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public CouponDetailsDTO getCouponById(Long id) {
        return couponRepository.findById(id)
                .map(CouponDetailsDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon with ID " + id + " not found"));
    }

    @Transactional
    public void deleteCoupon(Long id){
        Coupon entity = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon with ID " + id + " not found"));
        entity.setDeletedAt(Instant.now());
        couponRepository.save(entity);
    }

    @Transactional
    public CouponDetailsDTO partialUpdateCoupon(Long id, JsonPatch patch) {
        CouponDTO patchedCoupon;
        Coupon entity = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon with ID " + id + " not found"));
        try {
            JsonNode originalNode = objectMapper.valueToTree(entity);

            JsonNode patchedNode = patch.apply(originalNode);
            patchedCoupon = objectMapper.treeToValue(patchedNode, CouponDTO.class);

        } catch (JsonProcessingException | JsonPatchException e) {
            throw new BusinessRuleException(e.getMessage());
        }
        BigDecimal previousValue = entity.getValue();
        CouponEnum previousType = entity.getType();

        BigDecimal actualValue = patchedCoupon.value().setScale(2, RoundingMode.HALF_UP);
        CouponEnum actualType = CouponEnum.valueOf(patchedCoupon.type().toUpperCase());

        entity.setType(actualType);
        entity.setValue(actualValue);
        entity.setOneShot(patchedCoupon.oneShot());
        entity.setMaxUses(patchedCoupon.maxUses());
        entity.setValidFrom(patchedCoupon.validFrom());
        entity.setValidUntil(patchedCoupon.validUntil());

        validateCoupon(entity);
        if (!previousValue.equals(entity.getValue()) || !previousType.equals(entity.getType())) {
            int removedCount = productCouponApplicationRepository.removeActiveApplicationsByCouponId(id, Instant.now());
            if (entity.getUsesCount() >= removedCount) {
                entity.setUsesCount(entity.getUsesCount() - removedCount);
            } else {
                throw new DatabaseException("Cannot update coupon value, as it would result in a negative usage count. Current uses: " + entity.getUsesCount() + ", removed applications: " + removedCount);
            }
        }

        entity.setUpdatedAt(Instant.now());
        Coupon couponResult = couponRepository.saveAndFlush(entity);

        return new CouponDetailsDTO(couponResult);
    }

    protected void validateCoupon(Coupon entity) {
        Long id = (entity.getId() == null) ? -1L : entity.getId();

        validateRequiredFields(entity);
        validateBusinessRules(entity, id);
        validateTemporalRules(entity);
    }

    private void validateRequiredFields(Coupon entity) {
        if (entity.getType() == null) {
            throw new BusinessRuleException("Type is required");
        }
        if (entity.getValue() == null) {
            throw new BusinessRuleException("Value is required");
        }
        if (entity.getValidFrom() == null) {
            throw new BusinessRuleException("Valid from date is required");
        }
        if (entity.getValidUntil() == null) {
            throw new BusinessRuleException("Valid until date is required");
        }
    }

    private void validateBusinessRules(Coupon entity, Long id) {
        if (couponRepository.findByCodeAndIdNot(entity.getCode(), id).isPresent()) {
            throw new ResourceConflictException("Coupon code '" + entity.getCode() + "' already exists");
        }

        List<String> reservedCodes = List.of("admin", "auth", "null", "undefined");
        if (reservedCodes.contains(entity.getCode())) {
            throw new BusinessRuleException("Coupon code '" + entity.getCode() + "' is reserved");
        }

        if (entity.getOneShot() && entity.getMaxUses() != null) {
            throw new BusinessRuleException("If coupon is one shot, max uses must be null");
        }

        if (!entity.getOneShot() && entity.getMaxUses() != null && entity.getMaxUses() < entity.getUsesCount()) {
            throw new BusinessRuleException("Max uses cannot be set to a value lower than the current usage count (" + entity.getUsesCount() + ")");
        }

        if(entity.getOneShot() && entity.getUsesCount() > 0) {
            throw new BusinessRuleException("One-shot coupons cannot have a usage count greater than zero");
        }

        CouponEnum type = entity.getType();
        if (type == CouponEnum.PERCENT) {
            if (entity.getValue().compareTo(BigDecimal.ONE) < 0 || entity.getValue().compareTo(new BigDecimal("80")) > 0) {
                throw new BusinessRuleException("For percent type, value must be between 1 and 80.");
            }
        } else if (type == CouponEnum.FIXED && entity.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("For fixed type, value must be positive.");
            }
    }

    private void validateTemporalRules(Coupon entity) {
        if (entity.getValidUntil().isBefore(Instant.now())) {
            throw new BusinessRuleException("Valid until date must be in the future");
        }
        if (entity.getValidFrom().isAfter(entity.getValidUntil())) {
            throw new BusinessRuleException("Valid from date must be before valid until date");
        }

        ZonedDateTime zoned = entity.getValidFrom().atZone(ZoneOffset.UTC);
        ZonedDateTime future = zoned.plusYears(5);
        if (future.toInstant().isBefore(entity.getValidUntil())) {
            throw new BusinessRuleException("Valid until date must be within 5 years from valid from date");
        }
    }
}
