package com.crimsonveil.ia;

public record ResultadoIa(
        String texto,
        String modelo,
        int tokensPrompt,
        int tokensResposta
) {
}
