package br.com.lmuniz.desafio.senai.tests;

import br.com.lmuniz.desafio.senai.domains.entities.Product;

import java.math.BigDecimal;

public class ProductFactory {
    public static Product createProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("This is a test product.");
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setStock(20);

        return product;
    }
}
