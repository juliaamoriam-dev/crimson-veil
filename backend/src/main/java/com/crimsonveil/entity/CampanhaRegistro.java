package com.crimsonveil.entity;

import java.time.Instant;
import java.util.Map;

public record CampanhaRegistro(
        String id,
        String titulo,
        String codigo,
        String status,
        Map<String, Object> campanha,
        long versao,
        Instant criadaEm,
        Instant atualizadaEm
) {
}
