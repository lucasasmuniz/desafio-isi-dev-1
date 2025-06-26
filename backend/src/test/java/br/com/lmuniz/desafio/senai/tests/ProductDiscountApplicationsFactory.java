package br.com.lmuniz.desafio.senai.tests;

import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.domains.entities.ProductCouponApplication;
import br.com.lmuniz.desafio.senai.domains.entities.ProductDirectDiscountApplication;

import java.math.BigDecimal;
import java.time.Instant;

public class ProductDiscountApplicationsFactory {

    public static ProductDirectDiscountApplication createProductDirectDiscountApplication(Product product){
        ProductDirectDiscountApplication entity = new ProductDirectDiscountApplication();
        entity.setAppliedAt(Instant.now());
        entity.setProduct(product);
        entity.setDiscountPercentage(BigDecimal.valueOf(40));
        entity.setId(1L);
        return entity;
    }

    public static ProductCouponApplication createProductCouponApplication(Product product, Coupon coupon){
        ProductCouponApplication entity = new ProductCouponApplication();
        entity.setAppliedAt(Instant.now());
        entity.setCoupon(coupon);
        entity.setProduct(product);
        entity.setId(1L);
        return entity;
    }
}
