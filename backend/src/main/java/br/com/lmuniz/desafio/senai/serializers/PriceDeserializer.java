package br.com.lmuniz.desafio.senai.serializers;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class PriceDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String priceString = node.asText();

        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isTextual()){
            if (priceString.contains(",")) {
                priceString = priceString.replaceAll("\\.", "");
                priceString = priceString.replaceAll(",", ".");
                try {
                    return new BigDecimal(priceString);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid price format: " + node.asText(), e);
                }
            } else {
                return new BigDecimal(priceString);
            }
        }

        throw new IOException("Price must be a number or a formatted string.");
    }
}
