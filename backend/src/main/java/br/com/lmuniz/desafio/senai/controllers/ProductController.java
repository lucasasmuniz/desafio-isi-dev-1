package br.com.lmuniz.desafio.senai.controllers;

import br.com.lmuniz.desafio.senai.domains.dtos.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.ProductDiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDiscountDTO;
import br.com.lmuniz.desafio.senai.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        productDTO = productService.createProduct(productDTO);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(productDTO.id()).toUri();
        return ResponseEntity.created(uri).body(productDTO);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> softDeleteProduct(@PathVariable Long id) {
        productService.softDeleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<ProductDTO> restoreProduct(@PathVariable Long id) {
        ProductDTO dtoproduct = productService.restoreProduct(id);
        return ResponseEntity.ok(dtoproduct);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO productDTO = productService.getProductById(id);
        return ResponseEntity.ok(productDTO);
    }

    @PostMapping("/{id}/discount/coupon")
    public ResponseEntity<ProductDiscountDTO> applyCouponDiscount(@PathVariable Long id, @RequestBody CouponCodeDTO couponCodeDTO) {
        ProductDiscountDTO result = productService.applyCouponDiscount(id, couponCodeDTO);
        return ResponseEntity.ok(result);
    }
}
