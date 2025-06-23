package br.com.lmuniz.desafio.senai.domains.dtos.discounts;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DirectPercentageDiscountDTO(
        @NotNull(message = "Required field")
        @Min(value = 1, message = "Discount must be at least 1%")
        @Max(value = 80, message = "Discount cannot exceed 80%")
        BigDecimal percentage) {
}
