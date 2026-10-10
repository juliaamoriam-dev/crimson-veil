package com.crimsonveil.dto;

import java.util.List;

public record CelularContatosResposta(
        String campanhaId,
        String protagonistaId,
        long versaoCampanha,
        List<CelularContatoResposta> contatos
) {
}
