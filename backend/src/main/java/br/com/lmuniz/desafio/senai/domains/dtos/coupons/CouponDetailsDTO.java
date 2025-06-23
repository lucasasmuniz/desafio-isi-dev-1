package br.com.lmuniz.desafio.senai.domains.dtos.coupons;

import br.com.lmuniz.desafio.senai.domains.entities.Coupon;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponDetailsDTO(
        Long id,
        String code,
        String type,
        BigDecimal value,
        boolean oneShot,
        Integer maxUses,
        Integer usesCount,
        Instant validFrom,
        Instant validUntil,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt
) {
    public CouponDetailsDTO(Coupon entity) {
        this(
                entity.getId(),
                entity.getCode(),
                entity.getType().getTypeValue(),
                entity.getValue(),
                entity.getOneShot(),
                entity.getMaxUses(),
                entity.getUsesCount(),
                entity.getValidFrom(),
                entity.getValidUntil(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}