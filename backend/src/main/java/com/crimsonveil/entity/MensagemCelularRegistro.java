package com.crimsonveil.entity;

import java.time.Instant;

public record MensagemCelularRegistro(
        long id,
        String campanhaId,
        String protagonistaId,
        String conversaId,
        String contatoId,
        String direcao,
        String conteudo,
        String dataFiccional,
        String horarioFiccional,
        String chaveOperacao,
        Instant lidaEm,
        Instant registradaEm
) {
}
