package com.crimsonveil.dto;

import java.util.Map;

public record CampanhaResposta(
        Map<String, Object> campanha,
        long versao,
        boolean repetida
) {
}
