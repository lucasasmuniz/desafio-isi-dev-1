package br.com.lmuniz.desafio.senai.controllers;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponCodeDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.discounts.DirectPercentageDiscountDTO;
import br.com.lmuniz.desafio.senai.domains.dtos.products.ProductDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.tests.ProductFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private Long existingId;
    private Long nonExistingId;
    private Long deletedProductId;
    private Long cheapProductId;
    private Long alreadyDiscountedProductId;

    @BeforeEach
    void setUp(){
        product = ProductFactory.createProduct();
        existingName = "Moedor de Café Manual";
        existingId = 1L;
        nonExistingId = 999L;
        deletedProductId = 9L;
        cheapProductId = 4L;
        alreadyDiscountedProductId = 5L;
    }

    @Test
    @DisplayName("createProduct should return 409 Conflict when product name already exists")
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
    @DisplayName("createProduct should return 400 Bad Request for invalid fields")
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
    @DisplayName("createProduct should return 201 Created with valid data")
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

    @Test
    @DisplayName("deleteProduct should return 204 No Content on successful soft-delete")
    void deleteProduct_ShouldReturnNoContent_WhenSoftDeleteProduct() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/v1/products/%d".formatted(existingId))
                .contentType("application/json"));

        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deleteProduct should return 404 Not Found for a non-existent ID")
    void deleteProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/v1/products/%d".formatted(nonExistingId))
                .contentType("application/json"));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Product with id '%d' not found.".formatted(nonExistingId)));
    }

    @Test
    @DisplayName("restoreProduct should return 200 OK when restoring an soft-deleted product")
    void restoreProduct_ShouldReturnOk_WhenProductExistsAndDeleted() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/restore".formatted(deletedProductId))
                .contentType("application/json"));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(deletedProductId));
        result.andExpect(jsonPath("$.name").exists());
        result.andExpect(jsonPath("$.stock").exists());
        result.andExpect(jsonPath("$.price").exists());
    }

    @Test
    @DisplayName("restoreProduct should return 404 Not Found for a non-existent ID")
    void restoreProduct_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/restore".formatted(nonExistingId))
                .contentType("application/json"));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Product with id '%d' not found.".formatted(nonExistingId)));
    }

    @Test
    @DisplayName("restoreProduct should return 400 Bad Request when trying to restore an already active product")
    void restoreProduct_ShouldReturnBadRequest_WhenProductExistsAndNotDeleted() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/restore".formatted(existingId))
                .contentType("application/json"));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.error").value("Business rule exception"));
        result.andExpect(jsonPath("$.errors[0].message").value("Product is already active and cannot be restored."));
    }

    @Test
    @DisplayName("getProductById should return 200 OK with product details when ID exists")
    void getProductById_ShouldReturnOk_WhenProductExists() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products/%d".formatted(existingId))
                .contentType("application/json"));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(existingId));
        result.andExpect(jsonPath("$.name").exists());
        result.andExpect(jsonPath("$.stock").exists());
        result.andExpect(jsonPath("$.price").exists());
    }

    @Test
    @DisplayName("getProductById should return 404 Not Found for a non-existent ID")
    void getProductById_ShouldReturnNotFound_WhenProductDoesNotExist() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products/%d".formatted(nonExistingId))
                .contentType("application/json"));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Product with id '%d' not found.".formatted(nonExistingId)));
    }

    @Test
    @DisplayName("applyCouponDiscount should return 404 Not Found for a non-existent coupon code")
    void applyCouponDiscount_ShouldReturnResourceNotFound_WhenCouponDoesNotExist() throws Exception {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO("INVALIDCODE");
        String jsonBody = objectMapper.writeValueAsString(couponCodeDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/coupon".formatted(existingId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Coupon with code '%s' not found.".formatted(couponCodeDTO.code())));

    }

    @Test
    @DisplayName("applyCouponDiscount should return 404 Not Found for a non-existent product ID")
    void applyCouponDiscount_ShouldReturnResourceNotFound_WhenProductDoesNotExist() throws Exception {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO("PRIMEIRACOMPRA");
        String jsonBody = objectMapper.writeValueAsString(couponCodeDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/coupon".formatted(nonExistingId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Product with id '%d' not found.".formatted(nonExistingId)));
    }

    @Test
    @DisplayName("applyCouponDiscount should return 400 Bad Request for an invalid coupon")
    void applyCouponDiscount_ShouldReturnBadRequest_WhenCouponInvalid() throws Exception {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO("EMBREVE");
        String jsonBody = objectMapper.writeValueAsString(couponCodeDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/coupon".formatted(existingId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.error").value("Business rule exception"));
        result.andExpect(jsonPath("$.message").value("Coupon is not valid for the current date."));
    }

    @Test
    @DisplayName("applyCouponDiscount should return 409 Conflict when reusing a one-shot coupon")
    void applyCouponDiscount_ShouldReturnConflict_WhenCouponOneShotAlreadyUsed() throws Exception {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO("ONESHOTUSADO");
        String jsonBody = objectMapper.writeValueAsString(couponCodeDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/coupon".formatted(existingId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isConflict());
        result.andExpect(jsonPath("$.error").value("Resource conflict exception"));
        result.andExpect(jsonPath("$.message").value("Coupon is one-shot and has already been used."));
    }

    @Test
    @DisplayName("applyCouponDiscount should return 200 OK when applying a valid coupon")
    void applyCouponDiscount_ShouldReturnUnprocessableEntity_WhenFinalPriceInvalid() throws Exception {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO("VALE50");
        String jsonBody = objectMapper.writeValueAsString(couponCodeDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/coupon".formatted(cheapProductId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isUnprocessableEntity());
        result.andExpect(jsonPath("$.error").value("Invalid price exception"));
        result.andExpect(jsonPath("$.message").value("Final price after applying coupon cannot less than 0.01"));
    }

    @Test
    @DisplayName("applyCouponDiscount should return 200 OK when applying a valid coupon")
    void applyCouponDiscount_ShouldReturnOk_WhenValidCouponCode() throws Exception {
        CouponCodeDTO couponCodeDTO = new CouponCodeDTO("PRIMEIRACOMPRA");
        String jsonBody = objectMapper.writeValueAsString(couponCodeDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/coupon".formatted(existingId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(existingId));
        result.andExpect(jsonPath("$.discount").isNotEmpty());
        result.andExpect(jsonPath("$.hasCouponApplied").value(true));
    }

    @Test
    @DisplayName("applyDirectPercentDiscount should return 404 Not Found for a non-existent product ID")
    void applyDirectPercentDiscount_ShouldReturnResourceNotFound_WhenProductDoesNotExist() throws Exception {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.TEN);
        String jsonBody = objectMapper.writeValueAsString(directPercentageDiscountDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/percent".formatted(nonExistingId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Product with id '%d' not found.".formatted(nonExistingId)));
    }

    @Test
    @DisplayName("applyDirectPercentDiscount should return 409 Conflict for a product already having a discount")
    void applyDirectPercentDiscount_ShouldReturnConflict_WhenProductAlreadyHasDiscount() throws Exception {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.TEN);
        String jsonBody = objectMapper.writeValueAsString(directPercentageDiscountDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/percent".formatted(alreadyDiscountedProductId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isConflict());
        result.andExpect(jsonPath("$.error").value("Resource conflict exception"));
        result.andExpect(jsonPath("$.message").value("Direct discount is already applied to this product."));
    }

    @Test
    @DisplayName("applyDirectPercentDiscount should return 200 OK with valid discount")
    void applyDirectPercentDiscount_ShouldReturnOk_WhenValidDirectPercentageDiscount() throws Exception {
        DirectPercentageDiscountDTO directPercentageDiscountDTO = new DirectPercentageDiscountDTO(BigDecimal.TEN);
        String jsonBody = objectMapper.writeValueAsString(directPercentageDiscountDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/products/%d/discount/percent".formatted(existingId))
                .contentType("application/json")
                .content(jsonBody));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(existingId));
        result.andExpect(jsonPath("$.discount").isNotEmpty());
        result.andExpect(jsonPath("$.discount.value").value(directPercentageDiscountDTO.percentage()));
        result.andExpect(jsonPath("$.hasCouponApplied").value(false));
    }

    @Test
    @DisplayName("removeDiscount should return 204 No Content when removing discount")
    void removeDiscount_ShouldReturnNoContent_WhenRemovingDiscount() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/v1/products/%d/discount".formatted(alreadyDiscountedProductId))
                .contentType("application/json"));

        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("getAllProducts should return default page when no params are given")
    void getAllProducts_shouldReturnDefaultPage_whenNoParams() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.content").isArray());
        result.andExpect(jsonPath("$.totalElements").value(9));
    }

    @Test
    @DisplayName("getAllProducts should return filtered products when search param is used")
    void getAllProducts_shouldReturnFilteredProducts_whenSearchParamIsUsed() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products?search=café")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.content[?(@.name == 'Cafeteira Elétrica Mondial')]").exists());
        result.andExpect(jsonPath("$.content[?(@.name == 'Moedor de Café Manual')]").exists());
        result.andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("getAllProducts should return only discounted products when hasDiscount is true")
    void getAllProducts_shouldReturnOnlyDiscountedProducts_whenHasDiscountIsTrue() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/products?hasDiscount=true")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.content").isNotEmpty());
        result.andExpect(jsonPath("$.content[0].discount").exists());
    }

}
