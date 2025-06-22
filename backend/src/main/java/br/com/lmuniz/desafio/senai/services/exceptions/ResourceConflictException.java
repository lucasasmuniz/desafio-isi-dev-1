package br.com.lmuniz.desafio.senai.services.exceptions;

public class ResourceConflictException extends RuntimeException{
    public ResourceConflictException(String message) {
        super(message);
    }
}
