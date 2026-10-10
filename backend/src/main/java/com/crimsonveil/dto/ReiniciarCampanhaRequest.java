package com.crimsonveil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public record ReiniciarCampanhaRequest(
        @NotBlank String chaveOperacao,
        @NotNull @PositiveOrZero Long versaoEsperada,
        @NotNull Map<String, Object> campanhaInicial
) {
}
