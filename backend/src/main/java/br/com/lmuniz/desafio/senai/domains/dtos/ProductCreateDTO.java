package br.com.lmuniz.desafio.senai.domains.dtos;

import br.com.lmuniz.desafio.senai.domains.entities.Product;
import br.com.lmuniz.desafio.senai.serializers.PriceDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateDTO(Long id,

                               @NotBlank(message = "Required field")
                               @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
                               @Pattern(regexp = "^[\\p{L}0-9\\s\\-_,.]+$", message = "Name can only contain letters, numbers, spaces, and special characters (-, _, , .)")
                               String name,

                               @Size(max = 300, message = "Description have less than 300 characters")
                               String description,

                               @NotNull(message = "Required field")
                               @Min(value = 0, message = "Stock must be a positive number")
                               @Max(value = 999999, message = "Stock must be less than or equal to 999999")
                               Integer stock,

                               @NotNull(message = "Required field")
                               @DecimalMin(value = "0.01", inclusive = true, message = "Price must be greater than or equal to 0.01")
                               @DecimalMax(value = "1000000.00", inclusive = true, message = "Price must be less than or equal to 1000000.00")
                               @JsonDeserialize(using = PriceDeserializer.class)
                               BigDecimal price) {
    public ProductCreateDTO(Product entity) {
        this(entity.getId(), entity.getName(), entity.getDescription(), entity.getStock(), entity.getPrice());
    }
}
