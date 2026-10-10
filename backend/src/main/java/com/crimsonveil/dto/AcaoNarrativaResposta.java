package com.crimsonveil.dto;

import java.util.List;
import java.util.Map;

public record AcaoNarrativaResposta(
        Map<String, Object> campanha,
        long versao,
        boolean repetida,
        String textoNarracao,
        String modelo,
        String horarioAtual,
        int duracaoMinutos,
        List<String> sugestoes
) {
    public AcaoNarrativaResposta(
            Map<String, Object> campanha,
            long versao,
            boolean repetida,
            String textoNarracao,
            String modelo,
            String horarioAtual,
            int duracaoMinutos
    ) {
        this(campanha, versao, repetida, textoNarracao, modelo, horarioAtual, duracaoMinutos, List.of());
    }
}
