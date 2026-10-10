package com.crimsonveil.entity;

import java.time.Instant;

public record ContatoCelularRegistro(
        String id,
        String campanhaId,
        String protagonistaId,
        String personagemCanonicoId,
        String nome,
        String categoria,
        String chaveCriacao,
        Instant criadoEm
) {
}
