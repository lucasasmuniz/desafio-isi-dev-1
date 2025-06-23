package br.com.lmuniz.desafio.senai.domains.dtos;

import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;

import java.math.BigDecimal;
import java.time.Instant;

public record DiscountDTO(
        String type,
        BigDecimal value,
        Instant appliedAt
) {
    public DiscountDTO(Coupon coupon, ProductCouponApplication productCouponApplication) {
        this(
                coupon.getType().getTypeValue(),
                coupon.getValue(),
                productCouponApplication.getAppliedAt()
        );
    }
}
