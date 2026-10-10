package com.crimsonveil.dto;

import java.time.Instant;

public record CampanhaResumoResposta(
        String id,
        String titulo,
        String codigo,
        String status,
        long versao,
        Instant criadaEm,
        Instant atualizadaEm
) {
}
