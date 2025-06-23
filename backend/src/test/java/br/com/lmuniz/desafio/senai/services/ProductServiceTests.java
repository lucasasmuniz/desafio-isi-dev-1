package br.com.lmuniz.desafio.senai.services;

import br.com.lmuniz.desafio.senai.domains.dtos.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.repositories.ProductRepository;
import br.com.lmuniz.desafio.senai.services.exceptions.ResourceConflictException;
import br.com.lmuniz.desafio.senai.tests.ProductFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class ProductServiceTests {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    private String nonExistingNormalizedName;
    private String existingNormalizedName;
    private Product product;

    @BeforeEach
    void setUp() {
        nonExistingNormalizedName = "cafe premium";
        existingNormalizedName = "cafe normal";
        product = ProductFactory.createProduct();

        when(productRepository.existsByNormalizedName(nonExistingNormalizedName)).thenReturn(false);
        when(productRepository.existsByNormalizedName(existingNormalizedName)).thenReturn(true);
        when(productRepository.save(any())).thenReturn(product);
    }

    @Test
    @DisplayName("create product should throw ResourceNotFoundException when normalized name does not exist and valid data")
    void createProductShouldCreateProductWhenNonExistingNormalizedNameAndValidData() {
        ProductDTO dto = new ProductDTO(1L, nonExistingNormalizedName,product.getDescription(), product.getStock(), product.getPrice());
        ProductDTO result = productService.createProduct(dto);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(product.getName(), result.name());
        assertEquals(product.getDescription(), result.description());
        assertEquals(product.getStock(), result.stock());
        assertEquals(product.getPrice(), result.price());
    }

    @Test
    @DisplayName("create product should throw ResourceNotFoundException when existing normalized name")
    void createProductShouldThrowResourceConflictExceptionWhenExistingNormalizedName() {
        ProductDTO dto = new ProductDTO(1L, existingNormalizedName,product.getDescription(), product.getStock(), product.getPrice());
        assertThrows(ResourceConflictException.class, () -> {
            productService.createProduct(dto);
        });
        verify(productRepository, times(1)).existsByNormalizedName(existingNormalizedName);
    }
}
