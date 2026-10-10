package com.crimsonveil.ia;

public interface GeminiClient {
    ResultadoIa gerarNarracao(String systemInstruction, String promptUsuario);
}
