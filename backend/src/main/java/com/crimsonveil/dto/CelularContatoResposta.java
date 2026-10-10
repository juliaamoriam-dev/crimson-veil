package com.crimsonveil.dto;

public record CelularContatoResposta(
        String id,
        String nome,
        String categoria,
        String personagemCanonicoId,
        String conversaId,
        String ultimaMensagem,
        String ultimaDirecao,
        String ultimaDataFiccional,
        String ultimoHorarioFiccional,
        long naoLidas
) {
}
