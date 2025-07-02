package br.com.lmuniz.desafio.senai.controllers;

import br.com.lmuniz.desafio.senai.domains.dtos.coupons.CouponDTO;
import br.com.lmuniz.desafio.senai.domains.entities.Coupon;
import br.com.lmuniz.desafio.senai.domains.enums.CouponEnum;
import br.com.lmuniz.desafio.senai.tests.CouponFactory;
import br.com.lmuniz.desafio.senai.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
public class CouponControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long existingId;
    private Long nonExistingId;
    private Coupon coupon;
    private String existingCouponCode;

    @BeforeEach
    void setUp() {
        coupon = CouponFactory.createCoupon();
        existingId = 1L;
        nonExistingId = 999L;
        existingCouponCode = "PRIMEIRACOMPRA";
    }

    @Test
    void createCoupon_ShouldReturnCreated_WhenValidCoupon() throws Exception {
        CouponDTO couponDTO = new CouponDTO(coupon);
        String jsonBody = objectMapper.writeValueAsString(couponDTO);

        ResultActions result = mockMvc.perform(post("/api/v1/coupons")
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").isNotEmpty());
        result.andExpect(jsonPath("$.code").value(couponDTO.code()));
        result.andExpect(jsonPath("$.type").value(couponDTO.type()));
        result.andExpect(jsonPath("$.validFrom").value(couponDTO.validFrom().toString()));
        result.andExpect(jsonPath("$.validUntil").value(couponDTO.validUntil().toString()));
    }

    @Test
    void createCoupon_ShouldReturnBadRequest_WhenRequiredFieldsCouponNull() throws Exception {
        coupon.setCode(null);
        coupon.setValue(null);
        coupon.setValidFrom(null);
        coupon.setValidUntil(null);

        String jsonBody = objectMapper.writeValueAsString(new CouponDTO(coupon));

        ResultActions result = mockMvc.perform(post("/api/v1/coupons")
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.error").value("Validation exception"));
        result.andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void createCoupon_ShouldReturnConflict_WhenNormalizedCodeAlreadyExists() throws Exception {
        coupon.setCode(existingCouponCode);
        String normalizedCode = Utils.normalizeName(coupon.getCode());

        String jsonBody = objectMapper.writeValueAsString(new CouponDTO(coupon));

        ResultActions result = mockMvc.perform(post("/api/v1/coupons")
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isConflict());
        result.andExpect(jsonPath("$.error").value("Resource conflict exception"));
        result.andExpect(jsonPath("$.message").value("Coupon with code '%s' already exists".formatted(normalizedCode)));
    }

    @Test
    void createCoupon_ShouldReturnBadRequest_WhenPricePercentValueGreaterThen80() throws Exception {
        coupon.setType(CouponEnum.PERCENT);
        coupon.setValue(BigDecimal.valueOf(100));

        String jsonBody = objectMapper.writeValueAsString(new CouponDTO(coupon));

        ResultActions result = mockMvc.perform(post("/api/v1/coupons")
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.error").value("Validation error"));
        result.andExpect(jsonPath("$.errors[0].fieldName").value("value"));
        result.andExpect(jsonPath("$.errors[0].message").value("For percent type, value must be between 1 and 80."));
    }

    @Test
    void getAllCoupons_ShouldReturnOk_WhenNotOnlyValid() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/coupons")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$").isArray());
        result.andExpect(jsonPath("$[0].code").value("promo10"));
        result.andExpect(jsonPath("$[1].code").value("desconto25"));
        result.andExpect(jsonPath("$[2].code").value("superoferta"));
        result.andExpect(jsonPath("$[8].code").value("limite10"));
    }

    @Test
    void getAllCoupons_ShouldReturnOkAndValidCoupons_WhenOnlyValidTrue() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/coupons?onlyValid=true")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$").isArray());
        result.andExpect(jsonPath("$[0].code").value("promo10"));
        result.andExpect(jsonPath("$[1].code").value("desconto25"));
        result.andExpect(jsonPath("$[2].code").value("superoferta"));
        result.andExpect(jsonPath("$[8].code").value("teste01"));
    }

    @Test
    void getCouponById_ShouldReturnOk_WhenCouponExists() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/coupons/{id}", existingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").value(existingId));
        result.andExpect(jsonPath("$.code").value("promo10"));
        result.andExpect(jsonPath("$.type").value("percent"));
    }


    @Test
    void getCouponById_ShouldReturnNotFound_WhenCouponDoesNotExists() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/coupons/{id}", nonExistingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Coupon with ID %d not found".formatted(nonExistingId)));
    }

    @Test
    void deleteCoupon_ShouldReturnNoContent_WhenCouponExists() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/v1/coupons/{id}", existingId));

        result.andExpect(status().isNoContent());
    }

    @Test
    void deleteCoupon_ShouldReturnNotFound_WhenCouponDoesNotExists() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/v1/coupons/{id}", nonExistingId));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Coupon with ID %d not found".formatted(nonExistingId)));
    }

    @Test
    void partialUpdateCoupon_ShouldReturnBadRequest_WhenRequiredFieldsRemoved() throws Exception {
        String patchString = """
        [
            { "op": "remove", "path": "/validUntil" },
            { "op": "remove", "path": "/validFrom" },
            { "op": "remove", "path": "/type" },
            { "op": "remove", "path": "/value" }
        ]
        """;

        ResultActions result = mockMvc.perform(patch("/api/v1/coupons/{id}", existingId)
                .content(patchString)
                .contentType("application/json-patch+json")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.error").value("Validation error"));
        result.andExpect(jsonPath("$.message").value("One or more fields are invalid. Please check the 'errors' list."));
        result.andExpect(jsonPath("$.errors[0].fieldName").value("validUntil"));
        result.andExpect(jsonPath("$.errors[1].fieldName").value("validFrom"));
        result.andExpect(jsonPath("$.errors[2].fieldName").value("type"));
        result.andExpect(jsonPath("$.errors[3].fieldName").value("value"));
    }

    @Test
    void partialUpdateCoupon_ShouldReturnNotFound_WhenCouponDoesNotExists() throws Exception {
        String patchString = """
        [
            { "op": "replace", "path": "/value", "value": "50" }
        ]
        """;



        ResultActions result = mockMvc.perform(patch("/api/v1/coupons/{id}", nonExistingId)
                .content(patchString)
                .contentType("application/json-patch+json")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.error").value("Resource not found exception"));
        result.andExpect(jsonPath("$.message").value("Coupon with ID %d not found.".formatted(nonExistingId)));
    }

    @Test
    void partialUpdateCoupon_ShouldReturnOk_WhenValidPatch() throws Exception {
        int price = 50;
        String type = "percent";

        String patchString = """
        [
            { "op": "replace", "path": "/value", "value": %d },
            { "op": "replace", "path": "/type", "value": "%s" }
        ]
        """.formatted(price, type);

        ResultActions result = mockMvc.perform(patch("/api/v1/coupons/{id}", existingId)
                .content(patchString)
                .contentType("application/json-patch+json")
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.type").value(type));
        result.andExpect(jsonPath("$.value").value(price));
    }
}
