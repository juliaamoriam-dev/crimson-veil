package com.crimsonveil.dto;

import java.time.Instant;

public record CelularMensagemResposta(
        long id,
        String conversaId,
        String contatoId,
        String direcao,
        String conteudo,
        String dataFiccional,
        String horarioFiccional,
        Instant lidaEm,
        Instant registradaEm,
        String estado
) {
}
