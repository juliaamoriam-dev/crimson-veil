package com.crimsonveil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CampanhaCriacaoRequest(
        @NotBlank String id,
        @NotBlank String titulo,
        @NotNull Map<String, Object> campanha
) {
}
