package com.crimsonveil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CelularMensagemCriacaoRequest(
        @NotBlank @Size(max = 100) String chaveOperacao,
        @NotNull @PositiveOrZero Long versaoEsperada,
        @NotBlank @Size(max = 4000) String conteudo
) {
}
