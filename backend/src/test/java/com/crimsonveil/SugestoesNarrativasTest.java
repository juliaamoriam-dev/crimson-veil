package com.crimsonveil;

import com.crimsonveil.narrativa.ExtratorSugestoesNarrativas;
import com.crimsonveil.narrativa.GeradorSugestoesContextuais;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SugestoesNarrativasTest {

    private GeradorSugestoesContextuais geradorContextual;
    private ExtratorSugestoesNarrativas extrator;

    @BeforeEach
    void setup() {
        geradorContextual = new GeradorSugestoesContextuais();
        extrator = new ExtratorSugestoesNarrativas(geradorContextual);
    }

    @Test
    void extraiNarracaoESugestoesComMarcadorPadrao() {
        String textoBrutoIa = """
                Adrian aproxima a lanterna do chão úmido e aponta para as marcas recentes de pneus perto do galpão.

                Adrian: — Alguém saiu daqui com pressa logo após a meia-noite.

                ===SUGESTÕES DE AÇÃO===
                - Seguir as marcas de pneus até a saída secundária
                - Pedir a Noah para buscar registros das câmeras de tráfego
                - Examinar o cadeado quebrado no portão dos fundos
                - Interrogar o vigilante noturno da guarita
                """;

        Map<String, Object> campanha = Map.of(
                "estadoMundo", Map.of(
                        "localAtual", "Depósito 217",
                        "personagensPresentes", List.of("Adrian Hale")
                )
        );

        ExtratorSugestoesNarrativas.ResultadoExtracao resultado = extrator.extrair(textoBrutoIa, campanha);

        // A narração não deve conter o bloco de sugestões
        assertFalse(resultado.textoNarracao().contains("===SUGESTÕES DE AÇÃO==="));
        assertTrue(resultado.textoNarracao().contains("Adrian aproxima a lanterna"));
        assertTrue(resultado.textoNarracao().contains("Adrian: — Alguém saiu daqui"));

        // As sugestões devem ser extraídas limpas
        List<String> sugestoes = resultado.sugestoes();
        assertEquals(4, sugestoes.size());
        assertEquals("Seguir as marcas de pneus até a saída secundária", sugestoes.get(0));
        assertEquals("Pedir a Noah para buscar registros das câmeras de tráfego", sugestoes.get(1));
        assertEquals("Examinar o cadeado quebrado no portão dos fundos", sugestoes.get(2));
        assertEquals("Interrogar o vigilante noturno da guarita", sugestoes.get(3));
    }

    @Test
    void fazFallbackContextualQuandoIaNaoRetornarBlocoDeSugestoes() {
        String textoSemMarcador = "Helena fecha a pasta de inquéritos e olha pela janela da delegacia.";

        Map<String, Object> campanha = Map.of(
                "estadoMundo", Map.of(
                        "localAtual", "Gabinete da DCE",
                        "personagensPresentes", List.of("Helena Voss", "Noah Whitmore")
                ),
                "pistas", List.of(
                        Map.of("titulo", "Relógio parado às 02:17", "detalhes", "Marcação temporal idêntica")
                )
        );

        ExtratorSugestoesNarrativas.ResultadoExtracao resultado = extrator.extrair(textoSemMarcador, campanha);

        assertEquals(textoSemMarcador, resultado.textoNarracao());
        List<String> sugestoes = resultado.sugestoes();

        assertTrue(sugestoes.size() >= 3 && sugestoes.size() <= 5, "Deve gerar entre 3 e 5 sugestões");
        assertTrue(sugestoes.stream().anyMatch(s -> s.contains("Helena Voss")), "Deve incluir opção para Helena Voss");
        assertTrue(sugestoes.stream().anyMatch(s -> s.contains("Noah")), "Deve incluir opção para Noah");
        assertTrue(sugestoes.stream().anyMatch(s -> s.contains("Relógio parado às 02:17")), "Deve incluir opção para a pista");
    }

    @Test
    void complementaComSugestoesContextuaisSeIaRetornarMenosDeTres() {
        String textoComApenasUmaSugestao = """
                A chuva bate pesada contra as janelas do necrotério.

                ===SUGESTÕES DE AÇÃO===
                - Examinar o relatório toxicológico
                """;

        Map<String, Object> campanha = Map.of(
                "estadoMundo", Map.of(
                        "localAtual", "Laboratório Forense",
                        "personagensPresentes", List.of("Dra. Maya Navarro")
                )
        );

        ExtratorSugestoesNarrativas.ResultadoExtracao resultado = extrator.extrair(textoComApenasUmaSugestao, campanha);

        List<String> sugestoes = resultado.sugestoes();
        assertTrue(sugestoes.size() >= 3, "Deve ter complementado para pelo menos 3 sugestões");
        assertEquals("Examinar o relatório toxicológico", sugestoes.get(0));
        assertTrue(sugestoes.stream().anyMatch(s -> s.contains("Dra. Maya")));
    }

    @Test
    void geradorContextualEvitaRepetirUltimaAcaoDoJogador() {
        String acaoRecente = "Consultar Capitão Adrian sobre a estratégia da diligência";

        Map<String, Object> campanha = Map.of(
                "estadoMundo", Map.of(
                        "localAtual", "Apartamento 504",
                        "personagensPresentes", List.of("Adrian Hale")
                ),
                "mensagensCena", List.of(
                        Map.of("tipo", "JOGADOR", "conteudo", acaoRecente),
                        Map.of("tipo", "NARRADOR", "conteudo", "Adrian assentiu com um gesto discreto.")
                )
        );

        List<String> sugestoes = geradorContextual.gerar(campanha, "");

        assertTrue(sugestoes.size() >= 3);
        assertFalse(sugestoes.contains(acaoRecente), "Não deve repetir a mesma ação executada imediatamente antes");
    }
}
