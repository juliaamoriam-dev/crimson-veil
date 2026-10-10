package com.crimsonveil;

import com.crimsonveil.narrativa.MotorTempoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotorTempoServiceTest {
    private MotorTempoService motorTempo;

    @BeforeEach
    void setUp() {
        motorTempo = new MotorTempoService();
    }

    @Test
    void avancaHorarioNormalmenteEmCincoMinutos() {
        MotorTempoService.ResultadoAvancoTempo resultado =
                motorTempo.avancarTempo("14 de Outubro de 2026", "03:30", 5);

        assertEquals("03:35", resultado.novoHorario());
        assertEquals("14 de Outubro de 2026", resultado.novaData());
        assertEquals(5, resultado.minutosAvancados());
        assertEquals(0, resultado.diasAvancados());
    }

    @Test
    void trataViradaDeHoraSemGerarMinutosInvalidos() {
        MotorTempoService.ResultadoAvancoTempo resultado =
                motorTempo.avancarTempo("14 de Outubro de 2026", "02:55", 5);

        assertEquals("03:00", resultado.novoHorario());
        assertEquals("14 de Outubro de 2026", resultado.novaData());
    }

    @Test
    void trataViradaDeMeiaNoiteEAvancoDeDia() {
        MotorTempoService.ResultadoAvancoTempo resultado =
                motorTempo.avancarTempo("14 de Outubro de 2026", "23:55", 10);

        assertEquals("00:05", resultado.novoHorario());
        assertEquals("15 de Outubro de 2026", resultado.novaData());
        assertEquals(1, resultado.diasAvancados());
    }

    @Test
    void trataViradaDeMes() {
        MotorTempoService.ResultadoAvancoTempo resultado =
                motorTempo.avancarTempo("31 de Outubro de 2026", "23:50", 15);

        assertEquals("00:05", resultado.novoHorario());
        assertEquals("01 de Novembro de 2026", resultado.novaData());
    }

    @Test
    void trataViradaDeAno() {
        MotorTempoService.ResultadoAvancoTempo resultado =
                motorTempo.avancarTempo("31 de Dezembro de 2026", "23:55", 10);

        assertEquals("00:05", resultado.novoHorario());
        assertEquals("01 de Janeiro de 2027", resultado.novaData());
    }

    @Test
    void calculaDuracaoMaisLongaParaAcoesDeDeslocamento() {
        assertEquals(15, motorTempo.calcularDuracaoAcao("Dirigir até o depósito 217"));
        assertEquals(15, motorTempo.calcularDuracaoAcao("Deslocar para o necrotério"));
        assertEquals(5, motorTempo.calcularDuracaoAcao("Examinar a escrivaninha de Arthur"));
    }

    @Test
    void rejeitaHorarioOuDuracaoInvalidos() {
        assertThrows(IllegalArgumentException.class, () ->
                motorTempo.avancarTempo("14 de Outubro de 2026", "25:00", 5));
        assertThrows(IllegalArgumentException.class, () ->
                motorTempo.avancarTempo("14 de Outubro de 2026", "03:30", -1));
    }
}
