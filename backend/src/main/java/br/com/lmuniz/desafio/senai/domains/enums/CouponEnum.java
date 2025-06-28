package br.com.lmuniz.desafio.senai.domains.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CouponEnum {
    PERCENT("percent"),
    UNKNOWN("unknown"),
    FIXED("fixed");

    private final String type;

    CouponEnum(String type) {
        this.type = type;
    }

    public String getTypeValue() {
        return type;
    }

    @JsonCreator
    public static CouponEnum fromString(String text) {
        for (CouponEnum type : CouponEnum.values()) {
            if (type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return type;
    }
}
