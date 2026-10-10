package com.crimsonveil;

import com.crimsonveil.repository.CelularRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@ActiveProfiles("development")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CelularApiIntegrationTest {
    private static final Path DATABASE = Path.of(
            System.getProperty("java.io.tmpdir"),
            "crimson-veil-celular-test-" + UUID.randomUUID() + ".sqlite"
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

    @MockitoSpyBean
    private CelularRepository repository;

    @Test
    void persisteMensagensIdempotentesLeituraEProtagonistaNaoCanonica() {
        String campaignId = criarCampanha("char-milena", "Milena Ramires");
        String contactsUrl = url(campaignId, "/celular/contatos");
        JsonNode emptyContacts = http.getForObject(contactsUrl, JsonNode.class);
        assertEquals(0, emptyContacts.path("contatos").size());
        assertEquals("char-milena", emptyContacts.path("protagonistaId").asText());

        ResponseEntity<JsonNode> createdContact = http.postForEntity(contactsUrl, Map.of(
                "chaveOperacao", "contact-op-1",
                "versaoEsperada", 1,
                "nome", "Adrian Hale",
                "categoria", "PROFISSIONAL",
                "personagemCanonicoId", "p-adrian"
        ), JsonNode.class);
        assertEquals(HttpStatus.CREATED, createdContact.getStatusCode());
        assertEquals("p-adrian", createdContact.getBody().path("contato")
                .path("personagemCanonicoId").asText());
        String contactId = createdContact.getBody().path("contato").path("id").asText();
        assertEquals(2, createdContact.getBody().path("versaoCampanha").asLong());

        ResponseEntity<JsonNode> repeatedContact = http.postForEntity(contactsUrl, Map.of(
                "chaveOperacao", "contact-op-1",
                "versaoEsperada", 1,
                "nome", "Adrian Hale",
                "categoria", "PROFISSIONAL",
                "personagemCanonicoId", "p-adrian"
        ), JsonNode.class);
        assertEquals(HttpStatus.OK, repeatedContact.getStatusCode());
        assertTrue(repeatedContact.getBody().path("repetida").asBoolean());
        assertEquals(contactId, repeatedContact.getBody().path("contato").path("id").asText());

        String messagesUrl = url(campaignId, "/celular/contatos/" + contactId + "/mensagens");
        Map<String, Object> outbound = Map.of(
                "chaveOperacao", "message-op-out-1",
                "versaoEsperada", 2,
                "conteudo", "Voce ainda esta na central?"
        );
        ResponseEntity<JsonNode> sent = http.postForEntity(messagesUrl, outbound, JsonNode.class);
        assertEquals(HttpStatus.CREATED, sent.getStatusCode());
        assertEquals("SAIDA", sent.getBody().path("mensagem").path("direcao").asText());
        assertEquals("03:30", sent.getBody().path("mensagem").path("horarioFiccional").asText());
        assertEquals("PERSISTIDA", sent.getBody().path("mensagem").path("estado").asText());

        ResponseEntity<JsonNode> replay = http.postForEntity(messagesUrl, outbound, JsonNode.class);
        assertEquals(HttpStatus.OK, replay.getStatusCode());
        assertTrue(replay.getBody().path("repetida").asBoolean());
        assertEquals(sent.getBody().path("mensagem").path("id").asLong(),
                replay.getBody().path("mensagem").path("id").asLong());
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_messages WHERE campaign_id = ?",
                Integer.class, campaignId));

        Map<String, Object> payloadConflict = Map.of(
                "chaveOperacao", "message-op-out-1",
                "versaoEsperada", 3,
                "conteudo", "Outra mensagem usando a mesma chave."
        );
        ResponseEntity<JsonNode> conflict = http.postForEntity(messagesUrl, payloadConflict, JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());

        ResponseEntity<JsonNode> inbound = http.postForEntity(
                url(campaignId, "/celular/contatos/" + contactId + "/mensagens-recebidas-teste"),
                Map.of("chaveOperacao", "message-op-in-1", "versaoEsperada", 3,
                        "conteudo", "Resposta controlada de teste."),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, inbound.getStatusCode());
        assertEquals("ENTRADA", inbound.getBody().path("mensagem").path("direcao").asText());
        assertEquals("NAO_LIDA", inbound.getBody().path("mensagem").path("estado").asText());

        JsonNode history = http.getForObject(messagesUrl, JsonNode.class);
        assertEquals(2, history.path("mensagens").size());
        assertEquals("14 de Outubro de 2026",
                history.path("mensagens").get(1).path("dataFiccional").asText());
        String conversationId = history.path("conversaId").asText();
        long inboundMessageId = inbound.getBody().path("mensagem").path("id").asLong();

        String readUrl = url(campaignId, "/celular/conversas/" + conversationId + "/leitura");
        ResponseEntity<JsonNode> read = http.exchange(readUrl, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("versaoEsperada", 4, "ateMensagemId", inboundMessageId)),
                JsonNode.class);
        assertEquals(HttpStatus.OK, read.getStatusCode());
        assertEquals(1, read.getBody().path("mensagensMarcadas").asInt());
        assertEquals(5, read.getBody().path("versaoCampanha").asLong());

        ResponseEntity<JsonNode> readAgain = http.exchange(readUrl, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("versaoEsperada", 4, "ateMensagemId", inboundMessageId)),
                JsonNode.class);
        assertEquals(HttpStatus.OK, readAgain.getStatusCode());
        assertTrue(readAgain.getBody().path("repetida").asBoolean());
        JsonNode finalHistory = http.getForObject(messagesUrl, JsonNode.class);
        assertEquals("LIDA", finalHistory.path("mensagens").get(1).path("estado").asText());
        assertEquals(0, http.getForObject(contactsUrl, JsonNode.class)
                .path("contatos").get(0).path("naoLidas").asInt());
        assertFalse(finalHistory.path("mensagens").get(0).has("protagonistaId"));

        String customCampaignId = criarCampanha("char-player-created-88", "Investigadora Criada");
        JsonNode customContacts = http.getForObject(url(customCampaignId, "/celular/contatos"), JsonNode.class);
        assertEquals("char-player-created-88", customContacts.path("protagonistaId").asText());
        assertEquals(0, customContacts.path("contatos").size());
        ResponseEntity<JsonNode> foreignContact = http.getForEntity(
                url(customCampaignId, "/celular/contatos/" + contactId + "/mensagens"),
                JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, foreignContact.getStatusCode());
    }

    @Test
    void corpoJsonMalformadoRetornaBadRequestSemAlterarCampanha() {
        String campaignId = criarCampanha("char-json-fixture", "JSON Fixture");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<JsonNode> response = http.postForEntity(
                url(campaignId, "/celular/contatos"),
                new HttpEntity<>("{", headers),
                JsonNode.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(1, http.getForObject(url(campaignId, ""), JsonNode.class)
                .path("versao").asLong());
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_contacts WHERE campaign_id = ?",
                Integer.class, campaignId));
    }

    @Test
    void rejeitaVersaoObsoletaESemGravacaoParcialQuandoBancoFalha() {
        String campaignId = criarCampanha("char-celular-failure", "Protagonista de Teste");
        ResponseEntity<JsonNode> contact = http.postForEntity(url(campaignId, "/celular/contatos"), Map.of(
                "chaveOperacao", "contact-fail-1",
                "versaoEsperada", 1,
                "nome", "Contato de teste",
                "categoria", "PESSOAL"
        ), JsonNode.class);
        String contactId = contact.getBody().path("contato").path("id").asText();
        String messagesUrl = url(campaignId, "/celular/contatos/" + contactId + "/mensagens");
        Map<String, Object> stale = Map.of(
                "chaveOperacao", "message-stale-1",
                "versaoEsperada", 1,
                "conteudo", "Versao antiga."
        );
        assertEquals(HttpStatus.CONFLICT,
                http.postForEntity(messagesUrl, stale, JsonNode.class).getStatusCode());
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_messages WHERE campaign_id = ?",
                Integer.class, campaignId));

        doThrow(new DataAccessResourceFailureException("falha de teste"))
                .when(repository).criarMensagem(anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        try {
            ResponseEntity<JsonNode> failure = http.postForEntity(messagesUrl, Map.of(
                    "chaveOperacao", "message-fail-1",
                    "versaoEsperada", 2,
                    "conteudo", "Falha transacional."
            ), JsonNode.class);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, failure.getStatusCode());
            assertEquals(2, jdbc.queryForObject(
                    "SELECT version FROM campaigns WHERE id = ?", Integer.class, campaignId));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM campaign_conversations WHERE campaign_id = ?",
                    Integer.class, campaignId));
            assertEquals(0, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM campaign_messages WHERE campaign_id = ?",
                    Integer.class, campaignId));
        } finally {
            reset(repository);
        }
    }

    @Test
    void recusaAssociacaoDeContatoCanonicoDuplicadaSemAvancarVersao() {
        String campaignId = criarCampanha("char-celular-canonical", "Protagonista");
        String contactsUrl = url(campaignId, "/celular/contatos");
        ResponseEntity<JsonNode> first = http.postForEntity(contactsUrl, Map.of(
                "chaveOperacao", "canonical-contact-1",
                "versaoEsperada", 1,
                "nome", "Adrian",
                "categoria", "PROFISSIONAL",
                "personagemCanonicoId", "p-adrian"
        ), JsonNode.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        ResponseEntity<JsonNode> duplicate = http.postForEntity(contactsUrl, Map.of(
                "chaveOperacao", "canonical-contact-2",
                "versaoEsperada", 2,
                "nome", "Adrian duplicado",
                "categoria", "PROFISSIONAL",
                "personagemCanonicoId", "p-adrian"
        ), JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        JsonNode contacts = http.getForObject(contactsUrl, JsonNode.class);
        assertEquals(1, contacts.path("contatos").size());
        assertEquals(2, contacts.path("versaoCampanha").asLong());
    }

    private String criarCampanha(String protagonistId, String protagonistName) {
        String id = "camp-phone-" + UUID.randomUUID();
        Map<String, Object> campaign = campanha(id, protagonistId, protagonistName);
        ResponseEntity<JsonNode> response = http.postForEntity(
                "http://localhost:" + port + "/api/v1/campanhas",
                Map.of("id", id, "titulo", campaign.get("titulo"), "campanha", campaign),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return id;
    }

    private Map<String, Object> campanha(String id, String protagonistId, String protagonistName) {
        Map<String, Object> world = new LinkedHashMap<>();
        world.put("dataAtual", "14 de Outubro de 2026");
        world.put("horarioAtual", "03:30");
        world.put("localAtual", "Sala de teste");
        world.put("personagensPresentes", List.of(protagonistName));

        Map<String, Object> scheduledEvent = new LinkedHashMap<>();
        scheduledEvent.put("id", "EV-" + id);
        scheduledEvent.put("tipo", "EVENTO_POLICIAL");
        scheduledEvent.put("status", "AGENDADO");
        scheduledEvent.put("horarioPrevisto", "03:50");

        Map<String, Object> liveWorld = new LinkedHashMap<>();
        liveWorld.put("eventosAgendados", new ArrayList<>(List.of(scheduledEvent)));
        liveWorld.put("eventosOcorridos", new ArrayList<>());

        Map<String, Object> campaign = new LinkedHashMap<>();
        campaign.put("id", id);
        campaign.put("titulo", "Campanha de teste");
        campaign.put("codigo", "PHONE-TEST");
        campaign.put("status", "EM TESTE");
        campaign.put("protagonista", Map.of("id", protagonistId, "nome", protagonistName));
        campaign.put("casoAtivo", null);
        campaign.put("estadoMundo", world);
        campaign.put("contadorAcoes", 0);
        campaign.put("pistas", new ArrayList<>());
        campaign.put("evidencias", new ArrayList<>());
        campaign.put("mensagensCena", new ArrayList<>());
        campaign.put("eventLog", new ArrayList<>());
        campaign.put("mundoVivo", liveWorld);
        return campaign;
    }

    private String url(String campaignId, String suffix) {
        return "http://localhost:" + port + "/api/v1/campanhas/" + campaignId + suffix;
    }
}
