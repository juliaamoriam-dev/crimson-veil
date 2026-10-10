package com.crimsonveil;

import com.crimsonveil.repository.CampanhaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CampanhaApiIntegrationTest {
    private static final Path DATABASE = Path.of(
            System.getProperty("java.io.tmpdir"),
            "crimson-veil-test-" + UUID.randomUUID() + ".sqlite"
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
    private ObjectMapper mapper;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private CampanhaRepository repository;

    @Test
    void criaCarregaAtualizaUmaVezERecuperaEstadoEHistoricoPersistidos() throws Exception {
        String id = "camp-test-" + UUID.randomUUID();
        Map<String, Object> initial = campanha(id, "14 de Outubro de 2026", "03:30", 0);
        ResponseEntity<JsonNode> created = http.postForEntity(
                url("/api/v1/campanhas"),
                Map.of("id", id, "titulo", initial.get("titulo"), "campanha", initial),
                JsonNode.class
        );

        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals(1, created.getBody().path("versao").asLong());
        assertEquals(id, created.getBody().path("campanha").path("id").asText());

        Map<String, Object> changed = campanha(id, "14 de Outubro de 2026", "03:35", 1);
        Map<String, Object> world = (Map<String, Object>) changed.get("estadoMundo");
        world.put("localAtual", "Apartamento 504 — Cidade de Blackwood");
        Map<String, Object> operation = Map.of(
                "chaveOperacao", "op-repetivel-1",
                "versaoEsperada", 1,
                "acao", "Examinar as anotações de Arthur.",
                "campanha", changed
        );
        ResponseEntity<JsonNode> first = http.postForEntity(
                url("/api/v1/campanhas/" + id + "/acoes"), operation, JsonNode.class);
        ResponseEntity<JsonNode> repeated = http.postForEntity(
                url("/api/v1/campanhas/" + id + "/acoes"), operation, JsonNode.class);

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(2, first.getBody().path("versao").asLong());
        assertEquals(HttpStatus.OK, repeated.getStatusCode());
        assertTrue(repeated.getBody().path("repetida").asBoolean());
        assertEquals(2, repeated.getBody().path("versao").asLong());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_history WHERE campaign_id = ? AND operation_key = ?",
                Integer.class, id, "op-repetivel-1"));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ? AND event_id = ?",
                Integer.class, id, "EV-TEST-01"));

        ResponseEntity<JsonNode> loaded = http.getForEntity(url("/api/v1/campanhas/" + id), JsonNode.class);
        assertEquals(HttpStatus.OK, loaded.getStatusCode());
        assertEquals("03:35", loaded.getBody().path("campanha").path("estadoMundo").path("horarioAtual").asText());
        assertEquals("14 de Outubro de 2026",
                loaded.getBody().path("campanha").path("estadoMundo").path("dataAtual").asText());
        assertEquals(1, loaded.getBody().path("campanha").path("contadorAcoes").asInt());
        assertEquals("Apartamento 504 — Cidade de Blackwood",
                loaded.getBody().path("campanha").path("estadoMundo").path("localAtual").asText());

        ResponseEntity<JsonNode> history = http.getForEntity(
                url("/api/v1/campanhas/" + id + "/historico"), JsonNode.class);
        assertEquals(HttpStatus.OK, history.getStatusCode());
        assertEquals(2, history.getBody().size());
        assertEquals("Examinar as anotações de Arthur.",
                history.getBody().get(1).path("descricao").asText());

        ResponseEntity<JsonNode> listing = http.getForEntity(url("/api/v1/campanhas"), JsonNode.class);
        assertTrue(listing.getBody().toString().contains(id));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_history WHERE campaign_id = ?", Integer.class, id));
    }

    @Test
    void rejeitaVersaoDesatualizadaEInvalidacaoSemApagarCampanha() {
        String id = "camp-concorrencia-" + UUID.randomUUID();
        criar(id);
        Map<String, Object> candidate = campanha(id, "14 de Outubro de 2026", "03:35", 1);
        Map<String, Object> staleOperation = Map.of(
                "chaveOperacao", "op-stale",
                "versaoEsperada", 0,
                "acao", "Ação concorrente.",
                "campanha", candidate
        );

        ResponseEntity<JsonNode> conflict = http.postForEntity(
                url("/api/v1/campanhas/" + id + "/acoes"), staleOperation, JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());

        Map<String, Object> invalidWorld = (Map<String, Object>) candidate.get("estadoMundo");
        invalidWorld.put("horarioAtual", "03:62");
        Map<String, Object> invalidOperation = Map.of(
                "chaveOperacao", "op-invalid",
                "versaoEsperada", 1,
                "acao", "Horário inválido.",
                "campanha", candidate
        );
        ResponseEntity<JsonNode> invalid = http.postForEntity(
                url("/api/v1/campanhas/" + id + "/acoes"), invalidOperation, JsonNode.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());

        ResponseEntity<JsonNode> unchanged = http.getForEntity(url("/api/v1/campanhas/" + id), JsonNode.class);
        assertEquals(1, unchanged.getBody().path("versao").asLong());
        assertEquals("03:30",
                unchanged.getBody().path("campanha").path("estadoMundo").path("horarioAtual").asText());
    }

    @Test
    void falhaDePersistenciaNaoConfirmaNemAplicaAtualizacaoParcial() {
        String id = "camp-falha-" + UUID.randomUUID();
        criar(id);
        Map<String, Object> candidate = campanha(id, "14 de Outubro de 2026", "03:35", 1);
        Map<String, Object> operation = Map.of(
                "chaveOperacao", "op-falha",
                "versaoEsperada", 1,
                "acao", "Operação cuja auditoria falhará.",
                "campanha", candidate
        );
        doThrow(new DataAccessResourceFailureException("falha de persistência simulada"))
                .when(repository)
                .salvarHistorico(eq(id), eq("op-falha"), anyString(), anyString(),
                        anyString(), anyString(), eq(2L), anyString());

        ResponseEntity<JsonNode> failed = http.postForEntity(
                url("/api/v1/campanhas/" + id + "/acoes"), operation, JsonNode.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failed.getStatusCode());
        ResponseEntity<JsonNode> unchanged = http.getForEntity(url("/api/v1/campanhas/" + id), JsonNode.class);
        assertEquals(1, unchanged.getBody().path("versao").asLong());
        assertEquals("03:30",
                unchanged.getBody().path("campanha").path("estadoMundo").path("horarioAtual").asText());
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_operations WHERE campaign_id = ? AND operation_key = ?",
                Integer.class, id, "op-falha"));

        reset(repository);
    }

    @Test
    void migracaoCanonicaCriaSomenteUmaVezESemSobrescreverEstadoMaisRecente() {
        Map<String, Object> initial = campanha("camp-001", "14 de Outubro de 2026", "03:30", 0);
        Map<String, Object> request = Map.of(
                "id", "camp-001",
                "titulo", initial.get("titulo"),
                "campanha", initial
        );
        ResponseEntity<JsonNode> first = http.postForEntity(
                url("/api/v1/campanhas/migracao-canonica"), request, JsonNode.class);
        Map<String, Object> changed = campanha("camp-001", "14 de Outubro de 2026", "04:15", 4);
        http.exchange(
                url("/api/v1/campanhas/camp-001/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "chaveOperacao", "seed-update",
                        "versaoEsperada", first.getBody().path("versao").asLong(),
                        "acao", "Avanço canônico de teste.",
                        "campanha", changed
                )),
                JsonNode.class
        );

        ResponseEntity<JsonNode> repeated = http.postForEntity(
                url("/api/v1/campanhas/migracao-canonica"), request, JsonNode.class);
        assertTrue(repeated.getBody().path("repetida").asBoolean());
        assertEquals("04:15",
                repeated.getBody().path("campanha").path("estadoMundo").path("horarioAtual").asText());
        assertEquals(4, repeated.getBody().path("campanha").path("contadorAcoes").asInt());
    }

    private void criar(String id) {
        Map<String, Object> campaign = campanha(id, "14 de Outubro de 2026", "03:30", 0);
        http.postForEntity(url("/api/v1/campanhas"),
                Map.of("id", id, "titulo", campaign.get("titulo"), "campanha", campaign), JsonNode.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Map<String, Object> campanha(String id, String date, String time, int actions) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("id", "EV-TEST-01");
        event.put("status", "AGENDADO");
        event.put("horarioPrevisto", "03:50");

        Map<String, Object> liveWorld = new LinkedHashMap<>();
        liveWorld.put("eventosAgendados", new ArrayList<>(List.of(event)));
        liveWorld.put("eventosOcorridos", List.of(Map.of(
                "id", "EV-TEST-01", "status", "CONCLUIDO", "horarioOcorrido", "03:50")));

        Map<String, Object> world = new LinkedHashMap<>();
        world.put("dataAtual", date);
        world.put("horarioAtual", time);
        world.put("localAtual", "Apartamento 504");
        world.put("personagensPresentes", List.of("Milena Ramires"));

        boolean canonical = "camp-001".equals(id);
        Map<String, Object> protagonist = Map.of(
                "id", canonical ? "char-milena" : "char-" + id,
                "nome", canonical ? "Milena Ramires" : "Investigadora de Teste"
        );
        Map<String, Object> campaign = new LinkedHashMap<>();
        campaign.put("id", id);
        campaign.put("titulo", "Campanha de Teste");
        campaign.put("codigo", "TEST");
        campaign.put("status", "EM INVESTIGAÇÃO");
        campaign.put("ativa", true);
        campaign.put("protagonista", protagonist);
        campaign.put("casoAtivo", Map.of("id", canonical ? "caso-001" : "caso-" + id));
        campaign.put("cenaAtual", Map.of("id", canonical ? "cena-01" : "cena-" + id));
        campaign.put("estadoMundo", world);
        campaign.put("contadorAcoes", actions);
        campaign.put("pistas", List.of(Map.of("id", "PISTA-" + id)));
        campaign.put("evidencias", List.of(Map.of("id", "EVID-" + id)));
        campaign.put("eventLog", new ArrayList<>());
        campaign.put("mensagensCena", new ArrayList<>());
        campaign.put("mundoVivo", liveWorld);
        return campaign;
    }
}
