package br.com.lmuniz.desafio.senai.controllers;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.tests.ProductFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Product product;
    private String existingName;

    @BeforeEach
    void setUp(){
        product = ProductFactory.createProduct();
        existingName = "Moedor de Café Manual";
    }

    @Test
    void createProduct_ShouldReturnConflict_WhenProductNormalizedNameAlreadyExists() throws Exception {
        product.setName(existingName);
        ProductDTO productDTO = new ProductDTO(product);
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products")
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isConflict());
        result.andExpect(jsonPath("$.error").value("Resource conflict exception"));
        result.andExpect(jsonPath("$.message").value("Product with name '%s' already exists.".formatted(productDTO.name())));
    }

    @Test
    void createProduct_ShouldReturnBadRequest_WhenInvalidProductFields() throws Exception {
        product.setName("/");
        product.setDescription("*".repeat(301));
        product.setStock(null);
        product.setPrice(null);
        ProductDTO productDTO = new ProductDTO(product);
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products")
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.error").value("Validation exception"));
        result.andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createProduct_ShouldReturnCreated_WhenValidProduct() throws Exception {
        ProductDTO productDTO = new ProductDTO(product);
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products")
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").exists());
        result.andExpect(jsonPath("$.name").value(productDTO.name()));
        result.andExpect(jsonPath("$.description").value(productDTO.description()));
        result.andExpect(jsonPath("$.stock").value(productDTO.stock()));
        result.andExpect(jsonPath("$.price").value(productDTO.price()));
    }

}
