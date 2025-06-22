package br.com.lmuniz.desafio.senai.controllers.exceptions;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends StandardException{

    private List<FieldMessage> errors = new ArrayList<>();

    public List<FieldMessage> getErrors() {
        return errors;
    }
}
