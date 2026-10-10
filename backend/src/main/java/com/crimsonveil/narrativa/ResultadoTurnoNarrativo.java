package com.crimsonveil.narrativa;

import java.util.List;

public record ResultadoTurnoNarrativo(
        String textoNarracao,
        String modelo,
        int duracaoMinutos,
        String novoHorario,
        String novaData,
        List<String> sugestoes
) {
    public ResultadoTurnoNarrativo(
            String textoNarracao,
            String modelo,
            int duracaoMinutos,
            String novoHorario,
            String novaData
    ) {
        this(textoNarracao, modelo, duracaoMinutos, novoHorario, novaData, List.of());
    }
}
