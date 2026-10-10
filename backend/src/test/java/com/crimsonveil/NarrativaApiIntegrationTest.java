package com.crimsonveil;

import com.crimsonveil.dto.AcaoNarrativaRequest;
import com.crimsonveil.dto.CampanhaCriacaoRequest;
import com.crimsonveil.exception.ConfiguracaoIaPendenteException;
import com.crimsonveil.exception.FalhaIntegracaoIaException;
import com.crimsonveil.ia.GeminiClient;
import com.crimsonveil.ia.ResultadoIa;
import com.crimsonveil.service.CampanhaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NarrativaApiIntegrationTest {
    private static final Path DATABASE = Path.of(
            System.getProperty("java.io.tmpdir"),
            "crimson-veil-narrativa-test-" + UUID.randomUUID() + ".sqlite"
    );

    @DynamicPropertySource
    static void configurarBancoDeTeste(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CampanhaService campanhaService;

    @MockitoBean
    private GeminiClient geminiClient;

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1/campanhas" + path;
    }

    @BeforeEach
    void setUp() {
        reset(geminiClient);
    }

    @Test
    void executaAcaoNarrativaComSucessoEAtualizaEstadoNoBanco() throws Exception {
        String campanhaId = "camp-narrativa-" + UUID.randomUUID();
        Map<String, Object> inicial = snapshotInicial(campanhaId);
        campanhaService.criar(new CampanhaCriacaoRequest(campanhaId, "Caso Narrativo", inicial));

        when(geminiClient.gerarNarracao(anyString(), anyString())).thenReturn(
                new ResultadoIa(
                        "Adrian fecha a pasta tática e olha para o quadro.\n\nAdrian: — Nada consta nos arquivos da vítima.",
                        "gemini-2.5-flash",
                        150,
                        35
                )
        );

        AcaoNarrativaRequest request = new AcaoNarrativaRequest(
                "op-narrativa-1",
                1L,
                "Examinar anotações sobre a escrivaninha de Arthur"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "op-narrativa-1");
        HttpEntity<AcaoNarrativaRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = http.exchange(
                url("/" + campanhaId + "/narrativa/acao"),
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());

        assertEquals(2, json.get("versao").asLong());
        assertFalse(json.get("repetida").asBoolean());
        assertEquals("03:35", json.get("horarioAtual").asText());
        assertEquals(5, json.get("duracaoMinutos").asInt());
        assertTrue(json.get("textoNarracao").asText().contains("Adrian fecha a pasta tática"));

        // Validação no SQLite
        Integer versaoNoBanco = jdbc.queryForObject(
                "SELECT version FROM campaigns WHERE id = ?",
                Integer.class,
                campanhaId
        );
        assertEquals(2, versaoNoBanco);

        String horarioNoBanco = jdbc.queryForObject(
                "SELECT world_time FROM world_state WHERE campaign_id = ?",
                String.class,
                campanhaId
        );
        assertEquals("03:35", horarioNoBanco);

        Integer acoesNoBanco = jdbc.queryForObject(
                "SELECT action_count FROM world_state WHERE campaign_id = ?",
                Integer.class,
                campanhaId
        );
        assertEquals(1, acoesNoBanco);

        Integer operacoesSalvas = jdbc.queryForObject(
                "SELECT count(*) FROM campaign_operations WHERE campaign_id = ? AND operation_key = ?",
                Integer.class,
                campanhaId,
                "op-narrativa-1"
        );
        assertEquals(1, operacoesSalvas);

        Integer historicoSalvo = jdbc.queryForObject(
                "SELECT count(*) FROM campaign_history WHERE campaign_id = ? AND operation_key = ?",
                Integer.class,
                campanhaId,
                "op-narrativa-1"
        );
        assertEquals(1, historicoSalvo);
    }

    @Test
    void garanteIdempotenciaSemRechamarGemini() throws Exception {
        String campanhaId = "camp-narrativa-idemp-" + UUID.randomUUID();
        Map<String, Object> inicial = snapshotInicial(campanhaId);
        campanhaService.criar(new CampanhaCriacaoRequest(campanhaId, "Caso Idempotente", inicial));

        when(geminiClient.gerarNarracao(anyString(), anyString())).thenReturn(
                new ResultadoIa("Noah digita velozmente no terminal.", "gemini-2.5-flash", 100, 20)
        );

        AcaoNarrativaRequest request = new AcaoNarrativaRequest(
                "op-repetida-key",
                1L,
                "Perguntar sobre os logs para Noah"
        );

        // Primeira chamada
        ResponseEntity<String> res1 = http.postForEntity(
                url("/" + campanhaId + "/narrativa/acao"),
                request,
                String.class
        );
        assertEquals(HttpStatus.CREATED, res1.getStatusCode());
        JsonNode json1 = mapper.readTree(res1.getBody());
        assertFalse(json1.get("repetida").asBoolean());

        // Segunda chamada (idêntica)
        ResponseEntity<String> res2 = http.postForEntity(
                url("/" + campanhaId + "/narrativa/acao"),
                request,
                String.class
        );
        assertEquals(HttpStatus.OK, res2.getStatusCode());
        JsonNode json2 = mapper.readTree(res2.getBody());
        assertTrue(json2.get("repetida").asBoolean());
        assertEquals(json1.get("textoNarracao").asText(), json2.get("textoNarracao").asText());

        // Confirma que o Gemini só foi invocado 1 vez
        verify(geminiClient, times(1)).gerarNarracao(anyString(), anyString());
    }

    @Test
    void retornaConflitoQuandoVersaoEsperadaEstiverDesatualizada() {
        String campanhaId = "camp-narrativa-conflito-" + UUID.randomUUID();
        Map<String, Object> inicial = snapshotInicial(campanhaId);
        campanhaService.criar(new CampanhaCriacaoRequest(campanhaId, "Caso Conflito", inicial));

        AcaoNarrativaRequest request = new AcaoNarrativaRequest(
                "op-conflito-1",
                999L, // Versão incorreta
                "Qualquer ação"
        );

        ResponseEntity<String> response = http.postForEntity(
                url("/" + campanhaId + "/narrativa/acao"),
                request,
                String.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void retorna503QuandoChaveIaNaoEstiverConfiguradaSemModificarBanco() {
        String campanhaId = "camp-narrativa-no-key-" + UUID.randomUUID();
        Map<String, Object> inicial = snapshotInicial(campanhaId);
        campanhaService.criar(new CampanhaCriacaoRequest(campanhaId, "Caso Sem Chave", inicial));

        when(geminiClient.gerarNarracao(anyString(), anyString())).thenThrow(
                new ConfiguracaoIaPendenteException("A integração com a IA requer a variável de ambiente GEMINI_API_KEY configurada no servidor.")
        );

        AcaoNarrativaRequest request = new AcaoNarrativaRequest(
                "op-sem-chave",
                1L,
                "Inspecionar a porta do apartamento"
        );

        ResponseEntity<String> response = http.postForEntity(
                url("/" + campanhaId + "/narrativa/acao"),
                request,
                String.class
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().contains("GEMINI_API_KEY"));

        // Garante integridade do banco: versão continua 1
        Integer versao = jdbc.queryForObject("SELECT version FROM campaigns WHERE id = ?", Integer.class, campanhaId);
        assertEquals(1, versao);
    }

    @Test
    void retorna502QuandoGeminiFalharSemModificarBanco() {
        String campanhaId = "camp-narrativa-falha-" + UUID.randomUUID();
        Map<String, Object> inicial = snapshotInicial(campanhaId);
        campanhaService.criar(new CampanhaCriacaoRequest(campanhaId, "Caso Falha", inicial));

        when(geminiClient.gerarNarracao(anyString(), anyString())).thenThrow(
                new FalhaIntegracaoIaException("Tempo limite esgotado ao consultar o Google Gemini.")
        );

        AcaoNarrativaRequest request = new AcaoNarrativaRequest(
                "op-falha-gemini",
                1L,
                "Tentar acessar a câmera do corredor"
        );

        ResponseEntity<String> response = http.postForEntity(
                url("/" + campanhaId + "/narrativa/acao"),
                request,
                String.class
        );

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertTrue(response.getBody().contains("Tempo limite esgotado"));

        Integer versao = jdbc.queryForObject("SELECT version FROM campaigns WHERE id = ?", Integer.class, campanhaId);
        assertEquals(1, versao);
    }

    @Test
    void validaAgenciaEProtegeProtagonistaNaRespostaNarrativa() throws Exception {
        String campanhaId = "camp-narrativa-agencia-" + UUID.randomUUID();
        Map<String, Object> inicial = snapshotInicial(campanhaId);
        campanhaService.criar(new CampanhaCriacaoRequest(campanhaId, "Caso Agencia", inicial));

        when(geminiClient.gerarNarracao(anyString(), anyString())).thenReturn(
                new ResultadoIa("Você sentiu um frio na espinha ao ver os relógios parados às 02:17.", "gemini-2.5-flash", 100, 25)
        );

        AcaoNarrativaRequest request = new AcaoNarrativaRequest(
                "op-agencia-1",
                1L,
                "Examinar o relógio analógico"
        );

        ResponseEntity<String> response = http.postForEntity(
                url("/" + campanhaId + "/narrativa/acao"),
                request,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());
        String narracao = json.get("textoNarracao").asText();

        assertFalse(narracao.contains("Você sentiu um frio na espinha"));
        assertTrue(narracao.contains("um arrepio percorreu o ambiente"));
    }

    @Test
    void geraSugestoesDinamicasComSucessoAoProcessarAcao() throws Exception {
        String campanhaId = "camp-sugestoes-" + UUID.randomUUID();
        Map<String, Object> inicial = snapshotInicial(campanhaId);
        campanhaService.criar(new CampanhaCriacaoRequest(campanhaId, "Caso Sugestões", inicial));

        String respostaIaComSugestoes = """
                Noah aponta para a oscilação de frequência nos registros da subestação.

                Noah: — O apagão às 02:17 não foi acidental, foi um pico induzido.

                ===SUGESTÕES DE AÇÃO===
                - Solicitar a Noah o endereço do terminal que disparou o pico
                - Examinar o quadro elétrico do prédio vizinho
                - Consultar Adrian sobre a autorização de busca na concessionária de energia
                - Verificar os registros de vigilância do saguão durante os 53 segundos
                """;

        when(geminiClient.gerarNarracao(anyString(), anyString())).thenReturn(
                new ResultadoIa(respostaIaComSugestoes, "gemini-2.5-flash", 180, 45)
        );

        AcaoNarrativaRequest request = new AcaoNarrativaRequest(
                "op-sugestoes-1",
                1L,
                "Pedir a Noah para filtrar os logs da subestação elétrica"
        );

        ResponseEntity<String> response = http.postForEntity(
                url("/" + campanhaId + "/narrativa/acao"),
                request,
                String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode json = mapper.readTree(response.getBody());

        assertTrue(json.has("sugestoes"), "A resposta deve conter o campo 'sugestoes'");
        JsonNode sugestoes = json.get("sugestoes");
        assertTrue(sugestoes.isArray());
        assertEquals(4, sugestoes.size());
        assertEquals("Solicitar a Noah o endereço do terminal que disparou o pico", sugestoes.get(0).asText());
        assertEquals("Examinar o quadro elétrico do prédio vizinho", sugestoes.get(1).asText());

        // A narração retornada não deve conter as tags de sugestão
        String narracao = json.get("textoNarracao").asText();
        assertFalse(narracao.contains("===SUGESTÕES DE AÇÃO==="));
        assertTrue(narracao.contains("Noah aponta para a oscilação"));
    }

    private Map<String, Object> snapshotInicial(String id) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", id);
        snapshot.put("titulo", "Investigação de Teste");
        snapshot.put("codigo", "CASO-TESTE");
        snapshot.put("status", "EM ANDAMENTO");
        snapshot.put("contadorAcoes", 0);
        snapshot.put("mensagensCena", new ArrayList<>());
        snapshot.put("eventLog", new ArrayList<>());
        snapshot.put("pistas", List.of(Map.of("id", "PISTA-1", "titulo", "Relógio travado", "detalhes", "Marcando 02:17")));

        Map<String, Object> protagonista = new LinkedHashMap<>();
        protagonista.put("id", "char-" + id);
        protagonista.put("nome", "Milena Ramires");
        protagonista.put("cargo", "Detetive da DCE");
        protagonista.put("distintivo", "#5019-DCE");
        snapshot.put("protagonista", protagonista);

        Map<String, Object> mundo = new LinkedHashMap<>();
        mundo.put("dataAtual", "14 de Outubro de 2026");
        mundo.put("horarioAtual", "03:30");
        mundo.put("localAtual", "Apartamento 504");
        mundo.put("clima", "Chuva fria sobre Blackwood");
        mundo.put("personagensPresentes", List.of("Adrian Hale", "Helena Voss"));
        snapshot.put("estadoMundo", mundo);

        snapshot.put("casoAtivo", Map.of(
                "id", "caso-001",
                "codigo", "CASO #001",
                "titulo", "O Enigma do Relógio Parado"
        ));

        return snapshot;
    }
}
