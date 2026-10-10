package com.crimsonveil.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CelularLeituraRequest(
        @NotNull @PositiveOrZero Long versaoEsperada,
        @NotNull @Positive Long ateMensagemId
) {
}
