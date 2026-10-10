package com.crimsonveil.dto;

import java.util.Map;

public record PerfilPersonagemResposta(
        String personagemId,
        Map<String, Object> perfil,
        boolean possuiImagem,
        long limiteImagemBytes
) {
}
