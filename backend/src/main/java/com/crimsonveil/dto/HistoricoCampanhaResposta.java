package com.crimsonveil.dto;

import java.time.Instant;

public record HistoricoCampanhaResposta(
        String chaveOperacao,
        String tipo,
        String descricao,
        String dataNoMundo,
        String horarioNoMundo,
        long versaoCampanha,
        Instant registradoEm
) {
}
