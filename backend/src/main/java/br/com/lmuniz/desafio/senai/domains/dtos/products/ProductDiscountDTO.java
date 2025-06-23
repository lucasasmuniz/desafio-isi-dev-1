package br.com.lmuniz.desafio.senai.domains.dtos.products;

import br.com.lmuniz.desafio.senai.domains.dtos.discounts.DiscountDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDiscountDTO(
        Long id,
        String name,
        String description,
        Integer stock,
        boolean isOutOfStock,
        BigDecimal price,
        BigDecimal finalPrice,
        DiscountDTO discount,
        boolean hasCouponApplied,
        Instant createdAt,
        Instant updatedAt
) {
    public ProductDiscountDTO(Product product, BigDecimal finalPrice, DiscountDTO discount, boolean hasCouponApplied) {
        this(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStock(),
                product.getStock() <= 0,
                product.getPrice(),
                finalPrice,
                discount,
                hasCouponApplied,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
