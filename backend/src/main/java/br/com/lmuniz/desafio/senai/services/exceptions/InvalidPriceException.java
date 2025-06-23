package br.com.lmuniz.desafio.senai.services.exceptions;

public class InvalidPriceException extends RuntimeException{
    public InvalidPriceException(String message) {
        super(message);
    }
}
