package br.com.lmuniz.desafio.senai.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CouponCodeDTO(
        @NotBlank(message = "Required field")
        @Size(min = 4, max = 20, message = "Code must be between 4 and 20 characters")
        @Pattern(regexp = "^[\\p{L}\\p{N}]+$", message = "Code can only contain letters and numbers")
        String code
) {
}
