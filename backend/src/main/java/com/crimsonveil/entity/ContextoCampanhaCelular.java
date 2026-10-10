package com.crimsonveil.entity;

public record ContextoCampanhaCelular(
        String campanhaId,
        String protagonistaId,
        long versaoCampanha,
        String dataFiccional,
        String horarioFiccional
) {
}
