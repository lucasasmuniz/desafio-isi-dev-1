package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.repositories.ProductRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.BusinessRuleException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceNotFoundException;
import br.com.lmuniz.desafio.senai.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO dto){
        final String name = dto.name().trim().replaceAll("\\s+", " ");
        final String normalizedName = Utils.normalizeName(name);

        if(productRepository.existsByNormalizedName(normalizedName)){
            throw new ResourceConflictException("Product with name '" + name + "' already exists.");
        }

        Product entity = new Product(
                name,
                normalizedName,
                dto.description(),
                dto.price(),
                dto.stock()
        );
        entity = productRepository.save(entity);
        return new ProductDTO(entity);
    }

    @Transactional
    public void softDeleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));

        product.setDeletedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    @Transactional
    public ProductDTO restoreProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));

        if (product.getDeletedAt() == null) {
            throw new BusinessRuleException("Product is already active and cannot be restored.");
        }

        product.setDeletedAt(null);
        product.setUpdatedAt(Instant.now());
        product = productRepository.save(product);
        return new ProductDTO(product);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        return productRepository.findById(id)
                .map(ProductDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id '" + id + "' not found."));
    }
}
