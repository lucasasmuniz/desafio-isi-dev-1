package br.com.lmuniz.desafio.senai.domains.dtos.coupons;

import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponDTO(
        Long id,

        @NotBlank(message = "Required field")
        @Size(min = 4, max = 20, message = "Code must be between 4 and 20 characters")
        @Pattern(regexp = "^[\\p{L}\\p{N}]+$", message = "Code can only contain letters and numbers")
        String code,

        @NotBlank(message = "Required field")
        @Pattern(regexp = "^(percent|fixed)$", message = "Type must be either 'percent' or 'fixed'")
        String type,

        @NotNull(message = "Required field")
        BigDecimal value,

        @NotNull(message = "Required field")
        boolean oneShot,

        @Positive(message = "Max uses must be a positive number")
        Integer maxUses,

        @NotNull(message = "Required field")
        Instant validFrom,

        @NotNull(message = "Required field")
        @Future(message = "Valid until must be a future date")
        Instant validUntil
) {
    public CouponDTO(Coupon entity) {
        this(
                entity.getId(),
                entity.getCode(),
                entity.getType().getTypeValue(),
                entity.getValue(),
                entity.getOneShot(),
                entity.getMaxUses(),
                entity.getValidFrom(),
                entity.getValidUntil()
        );
    }
}
