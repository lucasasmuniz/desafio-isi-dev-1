package br.com.lmuniz.desafio.senai.controllers;

import br.com.lmuniz.desafio.senai.domains.dtos.ProductCreateDTO;
import br.com.lmuniz.desafio.senai.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    public ResponseEntity<ProductCreateDTO> createProduct(@Valid @RequestBody ProductCreateDTO productCreateDTO) {
        productCreateDTO = productService.createProduct(productCreateDTO);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(productCreateDTO.id()).toUri();
        return ResponseEntity.created(uri).body(productCreateDTO);
    }
}
