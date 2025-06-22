package br.com.lmuniz.desafio.senai.domains.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CouponEnum {
    PERCENT("percent"),
    FIXED("fixed");

    private final String type;

    CouponEnum(String type) {
        this.type = type;
    }

    public String getTypeValue() {
        return type;
    }

    @JsonCreator
    public static CouponEnum fromString(String value) {
        if (value == null) {
            return null;
        }
        for (CouponEnum couponEnum : CouponEnum.values()) {
            if (couponEnum.getTypeValue().equalsIgnoreCase(value)) {
                return couponEnum;
            }
        }
        throw new IllegalArgumentException("Invalid coupon type: " + value);
    }

    @Override
    public String toString() {
        return type;
    }
}
