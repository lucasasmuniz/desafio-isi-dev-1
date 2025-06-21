package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;
import br.com.lmuniz.desafio.senai.repositories.CouponRepository;
import br.com.lmuniz.desafio.senai.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService (CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public CouponDTO createCoupon(CouponDTO couponDTO){
        final String normalizedCode = Utils.normalizeName(couponDTO.code());
        validateCoupon(couponDTO, normalizedCode);

        Integer maxUses = couponDTO.maxUses();
        if(couponDTO.oneShot()){
            maxUses = null;
        }

        Coupon entity = new Coupon(
                normalizedCode,
                CouponEnum.valueOf(couponDTO.type().toUpperCase()),
                couponDTO.value(),
                couponDTO.oneShot(),
                maxUses,
                couponDTO.validFrom(),
                couponDTO.validUntil()
        );

        entity = couponRepository.save(entity);
        return new CouponDTO(entity);
    }

    protected void validateCoupon(CouponDTO dto, String normalizedCode){
        if (couponRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Coupon code '" + normalizedCode + "' already exists");
        }
        if (dto.validFrom().isAfter(dto.validUntil())) {
            throw new IllegalArgumentException("Valid from date must be before valid until date");
        }

        List<String> reservedCodes = List.of("admin", "auth", "null", "undefined");
        if (reservedCodes.contains(normalizedCode)) {
            throw new IllegalArgumentException("Coupon code '" + normalizedCode + "' is reserved.");
        }

        CouponEnum type = CouponEnum.valueOf(dto.type().toUpperCase());
        if (type == CouponEnum.PERCENT) {
            if (dto.value().doubleValue() < 1 || dto.value().doubleValue() > 80) {
                throw new IllegalArgumentException("For percent type, value must be between 1 and 80.");
            }
        } else if (type == CouponEnum.FIXED && dto.value().doubleValue() <= 0) {
                throw new IllegalArgumentException("For fixed type, value must be positive.");
            }

        ZonedDateTime zoned = dto.validFrom().atZone(ZoneOffset.UTC);
        ZonedDateTime future = zoned.plusYears(5);

        Instant futureInstant = future.toInstant();

        if (futureInstant.isBefore(dto.validUntil())) {
            throw new IllegalArgumentException("Valid until date must be within 5 years from valid from date");
        }
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
}
