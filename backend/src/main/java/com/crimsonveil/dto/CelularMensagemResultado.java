package com.crimsonveil.dto;

public record CelularMensagemResultado(
        CelularMensagemResposta mensagem,
        long versaoCampanha,
        boolean repetida
) {
}
