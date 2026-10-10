package com.crimsonveil.dto;

public record CelularLeituraResposta(
        String conversaId,
        long ateMensagemId,
        long mensagensMarcadas,
        long versaoCampanha,
        boolean repetida
) {
}
