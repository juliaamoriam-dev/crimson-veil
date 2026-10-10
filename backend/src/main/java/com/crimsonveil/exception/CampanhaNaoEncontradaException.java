package com.crimsonveil.exception;

public class CampanhaNaoEncontradaException extends RuntimeException {
    public CampanhaNaoEncontradaException(String id) {
        super("Campanha não encontrada: " + id);
    }
}
