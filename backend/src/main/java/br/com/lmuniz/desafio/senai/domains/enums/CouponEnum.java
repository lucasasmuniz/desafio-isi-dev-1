package br.com.lmuniz.desafio.senai.domains.enums;

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

    @Override
    public String toString() {
        return type;
    }
}
