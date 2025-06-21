package br.com.lmuniz.desafio.senai.serializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.math.BigDecimal;

public class PriceDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String priceString = node.asText();

        if (node.isNumber()) {
            return node.decimalValue();
        }

        if (node.isTextual()) {
            priceString = priceString.replaceAll("\\.", "");
            priceString = priceString.replaceAll(",", ".");
            try {
                return new BigDecimal(priceString);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid price format: " + node.asText(), e);
            }
        }

        throw new IOException("Price must be a number or a formatted string.");
    }
}
