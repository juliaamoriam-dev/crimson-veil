package com.crimsonveil.exception;

import java.time.Instant;

public record ErroApiResposta(
        Instant timestamp,
        int status,
        String erro,
        String mensagem
) {
}
