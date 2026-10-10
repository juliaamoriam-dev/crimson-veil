package com.crimsonveil.exception;

public class ConflitoVersaoException extends RuntimeException {
    public ConflitoVersaoException(String id) {
        super("A campanha " + id + " foi alterada em outra operação. Recarregue o estado mais recente.");
    }
}
