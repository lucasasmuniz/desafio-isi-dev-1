package br.com.lmuniz.desafio.senai.controllers;

import br.com.lmuniz.desafio.senai.domains.dtos.discounts.DirectPercentageDiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDiscountDTO;
import br.com.lmuniz.desafio.senai.services.ProductService;
import com.github.fge.jsonpatch.JsonPatch;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
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
        return ResponseEntity.ok(productService.restoreProduct(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO productDTO = productService.getProductById(id);
        return ResponseEntity.ok(productDTO);
    }

    @PostMapping("/{id}/discount/coupon")
    public ResponseEntity<ProductDiscountDTO> applyCouponDiscount(@PathVariable Long id, @Valid @RequestBody CouponCodeDTO couponCodeDTO) {
        ProductDiscountDTO result = productService.applyCouponDiscount(id, couponCodeDTO);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/discount/percent")
    public ResponseEntity<ProductDiscountDTO> applyDirectPercentDiscount(@PathVariable Long id, @Valid @RequestBody DirectPercentageDiscountDTO directPercentageDiscountDTO) {
        ProductDiscountDTO result = productService.applyDirectPercentDiscount(id, directPercentageDiscountDTO);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}/discount")
    public ResponseEntity<Void> removeDiscount(@PathVariable Long id) {
        productService.removeDiscount(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "{id}", consumes = "application/json-patch+json")
    public ResponseEntity<ProductDTO> partialUpdateProduct(
            @PathVariable Long id,
            @RequestBody JsonPatch patch) {
        return ResponseEntity.ok(productService.partialUpdateProduct(id, patch));
    }

    @GetMapping
    public ResponseEntity<Page<ProductDiscountDTO>> getAllProducts(Pageable pageable,
                               @RequestParam(name = "search", required = false) String search,
                               @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                               @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                               @RequestParam(name = "hasDiscount", required = false) Boolean hasDiscount,
                               @RequestParam(name = "includeDeleted", required = false, defaultValue = "false") Boolean includeDeleted,
                               @RequestParam(name = "onlyOutOfStock", required = false) Boolean onlyOutOfStock,
                               @RequestParam(name = "withCouponApplied", required = false) Boolean withCouponApplied) {

        return ResponseEntity.ok(productService.getAllProducts(pageable, search, minPrice, maxPrice, hasDiscount, includeDeleted, onlyOutOfStock, withCouponApplied));

    }
}
