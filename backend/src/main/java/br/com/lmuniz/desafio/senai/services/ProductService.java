package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.ProductCreateDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.repositories.ProductRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.utils.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductCreateDTO createProduct(ProductCreateDTO dto){
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
        return new ProductCreateDTO(entity);
    }
}
