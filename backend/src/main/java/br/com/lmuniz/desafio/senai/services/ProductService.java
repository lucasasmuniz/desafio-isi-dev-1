package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.ProductCreateDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.repositories.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductCreateDTO createProduct(ProductCreateDTO dto){
        final String normalizedName = dto.name().trim().replaceAll("\\s+", " ");

        if(productRepository.existsByName(normalizedName)){
            throw new IllegalArgumentException("Product with name '" + normalizedName + "' already exists.");
        }

        Product entity = new Product(
                normalizedName,
                dto.description(),
                dto.price(),
                dto.stock()
        );
        entity = productRepository.save(entity);
        return new ProductCreateDTO(entity);
    }
}
