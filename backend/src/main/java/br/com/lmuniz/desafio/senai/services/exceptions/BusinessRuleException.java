package br.com.lmuniz.desafio.senai.services.exceptions;

import java.util.Map;

public class BusinessRuleException extends RuntimeException {

    private Map<String, String> errors;

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(Map<String, String> errors) {
        super("Validation failed with " + errors.size() + " errors.");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}