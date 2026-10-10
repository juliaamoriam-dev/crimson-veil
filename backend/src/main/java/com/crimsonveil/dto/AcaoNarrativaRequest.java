package com.crimsonveil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AcaoNarrativaRequest(
        @NotBlank(message = "A chave de operação é obrigatória.")
        String chaveOperacao,

        @NotNull(message = "A versão esperada é obrigatória.")
        @PositiveOrZero(message = "A versão esperada deve ser maior ou igual a zero.")
        Long versaoEsperada,

        @NotBlank(message = "A ação da jogadora não pode estar vazia.")
        String acao
) {
}
