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

    private Coupon coupon;
    private String existingCouponCode = "PRIMEIRACOMPRA";

    @BeforeEach
    void setUp() {
        coupon = CouponFactory.createCoupon();
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
}
