package com.crimsonveil.dto;

import java.util.List;

public record CelularHistoricoResposta(
        String campanhaId,
        String protagonistaId,
        String contatoId,
        String conversaId,
        long versaoCampanha,
        List<CelularMensagemResposta> mensagens
) {
}
