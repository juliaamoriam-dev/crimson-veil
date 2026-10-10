package com.crimsonveil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CelularContatoCriacaoRequest(
        @NotBlank @Size(max = 100) String chaveOperacao,
        @NotNull @PositiveOrZero Long versaoEsperada,
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Pattern(regexp = "PESSOAL|PROFISSIONAL") String categoria,
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}") String personagemCanonicoId
) {
}
