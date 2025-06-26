package br.com.lmuniz.desafio.senai.tests;

import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CouponFactory {

    public static Coupon createCoupon() {
        return new Coupon(
                "promo",
                CouponEnum.PERCENT,
                BigDecimal.TEN,
                false,
                null,
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(10, ChronoUnit.DAYS)
        );
    }
}
