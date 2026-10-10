package com.crimsonveil;

import com.crimsonveil.narrativa.ValidadorAgencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorAgenciaTest {
    private ValidadorAgencia validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorAgencia();
    }

    @Test
    void preservaTextoCinematograficoQueRespeitaAgencia() {
        String texto = "Adrian fecha a pasta tática e aponta para o corredor.\n\nAdrian: — Temos dez minutos antes da coletiva.";
        String resultado = validador.validarEAjustar(texto, "Milena");
        assertEquals(texto, resultado);
    }

    @Test
    void ajustaTentativaDeNarrarSentimentosEmSegundaPessoa() {
        String texto = "Você sentiu um frio na espinha ao notar a sombra no canto da sala.";
        String resultado = validador.validarEAjustar(texto, "Milena");
        assertFalse(resultado.contains("Você sentiu um frio na espinha"));
        assertTrue(resultado.contains("um arrepio percorreu o ambiente"));
    }

    @Test
    void ajustaTentativaDeNarrarEmocoesDiretasDaProtagonistaPeloNome() {
        String texto = "Milena sentiu medo quando os passos ecoaram no corredor.";
        String resultado = validador.validarEAjustar(texto, "Milena");
        assertFalse(resultado.contains("Milena sentiu"));
        assertTrue(resultado.contains("O ambiente permaneceu sob tensão"));
    }
}
