package com.crimsonveil.dto;

public record CelularContatoResultado(
        CelularContatoResposta contato,
        long versaoCampanha,
        boolean repetida
) {
}
