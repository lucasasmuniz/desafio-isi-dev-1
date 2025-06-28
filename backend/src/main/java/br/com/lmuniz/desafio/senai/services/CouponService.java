package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDetailsDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.repositories.ProductCouponApplicationRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.DatabaseException;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                couponDTO.value().setScale(2, RoundingMode.HALF_UP),
                couponDTO.oneShot(),
                maxUses,
                couponDTO.validFrom(),
                couponDTO.validUntil()
        );
        Map<String, String> errors = new HashMap<>();

        validateCoupon(entity, errors);

        if (!errors.isEmpty()) {
            throw new BusinessRuleException(errors);
        }
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
        Coupon entity = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon with ID " + id + " not found."));

        BigDecimal previousValue = entity.getValue();
        CouponEnum previousType = entity.getType();

        Coupon entityToValidate;
        try {
            JsonNode patchedNode = patch.apply(objectMapper.valueToTree(entity));
            entityToValidate = objectMapper.treeToValue(patchedNode, Coupon.class);
        } catch (JsonProcessingException | JsonPatchException e) {
            throw new BusinessRuleException(e.getMessage());
        }

        Map<String, String> errors = new HashMap<>();
        validateCoupon(entityToValidate, errors);

        if (!errors.isEmpty()) {
            throw new BusinessRuleException(errors);
        }

        entity.setType(entityToValidate.getType());
        entity.setValue(entityToValidate.getValue().setScale(2, RoundingMode.DOWN));
        entity.setOneShot(entityToValidate.getOneShot());
        entity.setMaxUses(entityToValidate.getMaxUses());
        entity.setValidFrom(entityToValidate.getValidFrom());
        entity.setValidUntil(entityToValidate.getValidUntil());

        if (!previousValue.equals(entity.getValue()) || !previousType.equals(entity.getType())) {
            int removedCount = productCouponApplicationRepository.removeActiveApplicationsByCouponId(id, Instant.now());
            if (entity.getUsesCount() >= removedCount) {
                entity.setUsesCount(entity.getUsesCount() - removedCount);
            } else {
                throw new DatabaseException("Cannot update coupon, would result in negative usage count.");
            }
        }

        entity.setUpdatedAt(Instant.now());
        Coupon couponResult = couponRepository.saveAndFlush(entity);

        return new CouponDetailsDTO(couponResult);
    }

    private void validateCoupon(Coupon entity, Map<String, String> errors) {
        Long id = (entity.getId() == null) ? -1L : entity.getId();

        validateRequiredFields(entity, errors);
        validateBusinessRules(entity, id, errors);
        validateTemporalRules(entity, errors);
    }

    private void validateRequiredFields(Coupon entity, Map<String, String> errors) {
        if (entity.getType() == null) {
            errors.put("type", "Type is required");
        }
        if (entity.getValue() == null) {
            errors.put("value", "Value is required");
        }
        if (entity.getValidFrom() == null) {
            errors.put("validFrom", "Valid from date is required");
        }
        if (entity.getValidUntil() == null) {
            errors.put("validUntil", "Valid until date is required");
        }
    }

    private void validateBusinessRules(Coupon entity, Long id, Map<String, String> errors) {
        if (couponRepository.findByCodeAndIdNot(entity.getCode(), id).isPresent()) {
            errors.put("code", "Coupon code '" + entity.getCode() + "' already exists");
        }

        List<String> reservedCodes = List.of("admin", "auth", "null", "undefined");
        if (reservedCodes.contains(entity.getCode())) {
            errors.put("code", "Coupon code '" + entity.getCode() + "' is reserved");
        }

        if (entity.getMaxUses() != null) {
            if (entity.getOneShot()) {
                errors.put("maxUses", "If coupon is one shot, max uses must be null");
            } else if (entity.getMaxUses() < entity.getUsesCount()) {
                errors.put("maxUses", "Max uses cannot be set to a value lower than the current usage count (" + entity.getUsesCount() + ")");
            }
        }

        if(entity.getOneShot() && entity.getUsesCount() > 0) {
            errors.put("oneShot", "One-shot coupons that have been used cannot be modified");
        }

        if (entity.getType() != null && entity.getValue() != null) {
            CouponEnum type = entity.getType();
            if (type == CouponEnum.PERCENT) {
                if (entity.getValue().compareTo(BigDecimal.ONE) < 0 || entity.getValue().compareTo(new BigDecimal("80")) > 0) {
                    errors.put("value", "For percent type, value must be between 1 and 80.");
                }
            } else if (type == CouponEnum.FIXED && entity.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                errors.put("value", "For fixed type, value must be positive.");
            }
        }
    }

    private void validateTemporalRules(Coupon entity, Map<String, String> errors) {
        if (entity.getValidFrom() != null && entity.getValidUntil() != null) {
            if (entity.getValidFrom().isAfter(entity.getValidUntil())) {
                errors.put("validFrom", "Valid from date must be before valid until date");
            }
        if (entity.getValidUntil().isBefore(Instant.now())) {
            errors.put("validUntil", "Valid until date must be in the future");
        }

            ZonedDateTime zoned = entity.getValidFrom().atZone(ZoneOffset.UTC);
            ZonedDateTime future = zoned.plusYears(5);
            if (future.toInstant().isBefore(entity.getValidUntil())) {
                errors.put("validUntil", "Valid until date must be within 5 years from valid from date");
            }
        }
    }
}
