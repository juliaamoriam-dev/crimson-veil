package com.crimsonveil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public record MutacaoCampanhaRequest(
        @NotBlank String chaveOperacao,
        @NotNull @PositiveOrZero Long versaoEsperada,
        @NotNull Map<String, Object> campanha,
        String acao
) {
}
