package com.crimsonveil;

import com.crimsonveil.repository.CampanhaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CampanhaPerfilApiIntegrationTest {
    private static final Path DATABASE = Path.of(
            System.getProperty("java.io.tmpdir"),
            "crimson-veil-campanha-perfil-" + UUID.randomUUID() + ".sqlite"
    );

    @DynamicPropertySource
    static void configurarBancoDeTeste(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE);
        registry.add("crimson-veil.profile.max-image-bytes", () -> 1024);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private CampanhaRepository repository;

    @Test
    void reiniciaUmaCampanhaPreservandoPersonagemEIsolandoOutrasCampanhas() {
        String id = "camp-reset-" + UUID.randomUUID();
        String outraId = "camp-preservada-" + UUID.randomUUID();
        criar(id, "char-compartilhada");
        criar(outraId, "char-outra");

        Map<String, Object> alterada = campanha(id, "char-compartilhada");
        ((Map<String, Object>) alterada.get("estadoMundo")).put("horarioAtual", "04:10");
        alterada.put("contadorAcoes", 5);
        alterada.put("decisoes", List.of("A decisão da partida anterior."));
        alterada.put("consequencias", List.of("Uma consequência ativa."));
        alterada.put("pistas", List.of(Map.of("id", "PISTA-" + id, "estado", "ANALISADA")));
        alterada.put("eventLog", List.of(Map.of("id", "EV-" + id, "descricao", "Evento anterior.")));
        ResponseEntity<JsonNode> acao = http.postForEntity(url("/api/v1/campanhas/" + id + "/acoes"),
                Map.of("chaveOperacao", "acao-" + id, "versaoEsperada", 1, "acao", "Investigar.",
                        "campanha", alterada), JsonNode.class);
        assertEquals(HttpStatus.OK, acao.getStatusCode());

        ResponseEntity<JsonNode> contato = http.postForEntity(
                url("/api/v1/campanhas/" + id + "/celular/contatos"),
                Map.of("chaveOperacao", "contato-" + id, "versaoEsperada", 2, "nome", "Contato temporário",
                        "categoria", "PROFISSIONAL"), JsonNode.class);
        assertEquals(HttpStatus.CREATED, contato.getStatusCode());
        String contatoId = contato.getBody().path("contato").path("id").asText();
        ResponseEntity<JsonNode> mensagem = http.postForEntity(
                url("/api/v1/campanhas/" + id + "/celular/contatos/" + contatoId + "/mensagens"),
                Map.of("chaveOperacao", "mensagem-" + id, "versaoEsperada", 3,
                        "conteudo", "Mensagem da partida anterior."), JsonNode.class);
        assertEquals(HttpStatus.CREATED, mensagem.getStatusCode());

        Map<String, Object> inicial = campanha(id, "char-compartilhada");
        ResponseEntity<JsonNode> reiniciada = http.postForEntity(url("/api/v1/campanhas/" + id + "/reiniciar"),
                Map.of("chaveOperacao", "reset-" + id, "versaoEsperada", 4,
                        "campanhaInicial", inicial), JsonNode.class);

        assertEquals(HttpStatus.OK, reiniciada.getStatusCode());
        assertEquals(5, reiniciada.getBody().path("versao").asLong());
        assertEquals("char-compartilhada",
                reiniciada.getBody().path("campanha").path("protagonista").path("id").asText());
        assertEquals("03:30",
                reiniciada.getBody().path("campanha").path("estadoMundo").path("horarioAtual").asText());
        assertEquals(0, reiniciada.getBody().path("campanha").path("contadorAcoes").asInt());
        assertEquals(0, reiniciada.getBody().path("campanha").path("decisoes").size());
        assertEquals(0, reiniciada.getBody().path("campanha").path("consequencias").size());
        assertEquals("NOVA",
                reiniciada.getBody().path("campanha").path("pistas").get(0).path("estado").asText());
        assertEquals(1, reiniciada.getBody().path("campanha").path("mundoVivo")
                .path("eventosAgendados").size());
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_history WHERE campaign_id = ?", Integer.class, id));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_contacts WHERE campaign_id = ?", Integer.class, id));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_messages WHERE campaign_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_history WHERE campaign_id = ?", Integer.class, outraId));
        assertEquals("03:30", http.getForEntity(url("/api/v1/campanhas/" + outraId), JsonNode.class)
                .getBody().path("campanha").path("estadoMundo").path("horarioAtual").asText());

        ResponseEntity<JsonNode> repetida = http.postForEntity(url("/api/v1/campanhas/" + id + "/reiniciar"),
                Map.of("chaveOperacao", "reset-" + id, "versaoEsperada", 4,
                        "campanhaInicial", inicial), JsonNode.class);
        assertEquals(HttpStatus.OK, repetida.getStatusCode());
        assertTrue(repetida.getBody().path("repetida").asBoolean());
        assertEquals(5, repetida.getBody().path("versao").asLong());
    }

    @Test
    void falhaAoGravarEventosDesfazTodaAOperacaoDeReinicio() {
        String id = "camp-reset-rollback-" + UUID.randomUUID();
        criar(id, "char-rollback");
        Map<String, Object> alterada = campanha(id, "char-rollback");
        ((Map<String, Object>) alterada.get("estadoMundo")).put("horarioAtual", "04:20");
        alterada.put("contadorAcoes", 1);
        http.postForEntity(url("/api/v1/campanhas/" + id + "/acoes"),
                Map.of("chaveOperacao", "acao-rollback-" + id, "versaoEsperada", 1,
                        "acao", "Atualizar antes do teste.", "campanha", alterada), JsonNode.class);

        doThrow(new DataAccessResourceFailureException("falha simulada no salvamento dos eventos"))
                .when(repository).salvarEventos(eq(id), anyMap());
        ResponseEntity<JsonNode> falha = http.postForEntity(url("/api/v1/campanhas/" + id + "/reiniciar"),
                Map.of("chaveOperacao", "reset-rollback-" + id, "versaoEsperada", 2,
                        "campanhaInicial", campanha(id, "char-rollback")), JsonNode.class);
        reset(repository);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, falha.getStatusCode());
        JsonNode recuperada = http.getForEntity(url("/api/v1/campanhas/" + id), JsonNode.class).getBody();
        assertEquals(2, recuperada.path("versao").asLong());
        assertEquals("04:20", recuperada.path("campanha").path("estadoMundo").path("horarioAtual").asText());
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_history WHERE campaign_id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ?", Integer.class, id));
    }

    @Test
    void novaCampanhaPodeReutilizarPerfilSemAlterarCampanhaAnterior() {
        String personagemId = "char-reutilizada-" + UUID.randomUUID();
        String primeira = "camp-mesma-personagem-1-" + UUID.randomUUID();
        String segunda = "camp-mesma-personagem-2-" + UUID.randomUUID();
        criar(primeira, personagemId);
        Map<String, Object> outra = campanha(segunda, personagemId);
        ((Map<String, Object>) outra.get("protagonista")).put("nome", "Nome divergente no cliente");
        ResponseEntity<JsonNode> criada = http.postForEntity(url("/api/v1/campanhas"),
                Map.of("id", segunda, "titulo", outra.get("titulo"), "campanha", outra), JsonNode.class);

        assertEquals(HttpStatus.CREATED, criada.getStatusCode());
        assertEquals("Personagem compartilhada",
                criada.getBody().path("campanha").path("protagonista").path("nome").asText());
        assertEquals("03:30", http.getForEntity(url("/api/v1/campanhas/" + primeira), JsonNode.class)
                .getBody().path("campanha").path("estadoMundo").path("horarioAtual").asText());
        assertEquals(1, http.getForEntity(url("/api/v1/campanhas/" + primeira), JsonNode.class)
                .getBody().path("versao").asInt());
    }

    @Test
    void imagemDePerfilEValidadaPersistidaERemovivel() throws Exception {
        String id = "camp-profile-" + UUID.randomUUID();
        String personagemId = "char-profile-" + UUID.randomUUID();
        criar(id, personagemId);
        ByteArrayOutputStream pngBuffer = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", pngBuffer);
        byte[] png = pngBuffer.toByteArray();

        ResponseEntity<JsonNode> salva = enviarImagem(personagemId, "retrato.png", "image/png", png);
        assertEquals(HttpStatus.OK, salva.getStatusCode());
        assertTrue(salva.getBody().path("possuiImagem").asBoolean());
        ResponseEntity<byte[]> recuperada = http.getForEntity(
                url("/api/v1/personagens/" + personagemId + "/imagem"), byte[].class);
        assertEquals(HttpStatus.OK, recuperada.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, recuperada.getHeaders().getContentType());
        assertArrayEquals(png, recuperada.getBody());

        ResponseEntity<JsonNode> formatoInvalido = enviarImagem(
                personagemId, "retrato.svg", "image/svg+xml", "<svg/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(HttpStatus.BAD_REQUEST, formatoInvalido.getStatusCode());
        ResponseEntity<JsonNode> arquivoTruncado = enviarImagem(
                personagemId, "truncado.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        assertEquals(HttpStatus.BAD_REQUEST, arquivoTruncado.getStatusCode());
        byte[] grande = new byte[1025];
        System.arraycopy(png, 0, grande, 0, png.length);
        ResponseEntity<JsonNode> tamanhoExcedido = enviarImagem(personagemId, "grande.png", "image/png", grande);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, tamanhoExcedido.getStatusCode());

        ResponseEntity<JsonNode> removida = http.exchange(
                url("/api/v1/personagens/" + personagemId + "/imagem"), HttpMethod.DELETE, null, JsonNode.class);
        assertEquals(HttpStatus.OK, removida.getStatusCode());
        assertFalse(removida.getBody().path("possuiImagem").asBoolean());
        assertEquals(HttpStatus.NOT_FOUND, http.getForEntity(
                url("/api/v1/personagens/" + personagemId + "/imagem"), byte[].class).getStatusCode());
    }

    private ResponseEntity<JsonNode> enviarImagem(String personagemId, String nome, String tipo, byte[] dados) {
        ByteArrayResource arquivo = new ByteArrayResource(dados) {
            @Override
            public String getFilename() {
                return nome;
            }
        };
        HttpHeaders cabecalhosArquivo = new HttpHeaders();
        cabecalhosArquivo.setContentType(MediaType.parseMediaType(tipo));
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("arquivo", new HttpEntity<>(arquivo, cabecalhosArquivo));
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(MediaType.MULTIPART_FORM_DATA);
        return http.exchange(url("/api/v1/personagens/" + personagemId + "/imagem"), HttpMethod.PUT,
                new HttpEntity<>(partes, cabecalhos), JsonNode.class);
    }

    private void criar(String id, String personagemId) {
        Map<String, Object> campanha = campanha(id, personagemId);
        http.postForEntity(url("/api/v1/campanhas"),
                Map.of("id", id, "titulo", campanha.get("titulo"), "campanha", campanha), JsonNode.class);
    }

    private String url(String caminho) {
        return "http://localhost:" + port + caminho;
    }

    private Map<String, Object> campanha(String id, String personagemId) {
        Map<String, Object> protagonista = new LinkedHashMap<>();
        protagonista.put("id", personagemId);
        protagonista.put("nome", "Personagem compartilhada");
        protagonista.put("cargo", "Detetive");
        protagonista.put("atributos", Map.of("observacao", 8, "intuicao", 7));

        Map<String, Object> mundo = new LinkedHashMap<>();
        mundo.put("dataAtual", "14 de Outubro de 2026");
        mundo.put("horarioAtual", "03:30");
        mundo.put("localAtual", "Apartamento 504");
        mundo.put("personagensPresentes", List.of("Personagem compartilhada"));
        Map<String, Object> evento = Map.of("id", "EV-" + id, "status", "AGENDADO",
                "horarioPrevisto", "03:50");
        Map<String, Object> mundoVivo = new LinkedHashMap<>();
        mundoVivo.put("eventosAgendados", new ArrayList<>(List.of(evento)));
        mundoVivo.put("eventosOcorridos", new ArrayList<>());
        mundoVivo.put("consequencias", new ArrayList<>());

        Map<String, Object> campanha = new LinkedHashMap<>();
        campanha.put("id", id);
        campanha.put("titulo", "Investigação " + id);
        campanha.put("codigo", "TEST");
        campanha.put("status", "EM INVESTIGAÇÃO");
        campanha.put("ativa", true);
        campanha.put("protagonista", protagonista);
        campanha.put("casoAtivo", Map.of("id", "caso-" + id));
        campanha.put("cenaAtual", Map.of("id", "cena-" + id));
        campanha.put("estadoMundo", mundo);
        campanha.put("contadorAcoes", 0);
        campanha.put("pistas", List.of(Map.of("id", "PISTA-" + id, "estado", "NOVA")));
        campanha.put("evidencias", List.of(Map.of("id", "EVID-" + id, "estado", "COLETADA")));
        campanha.put("decisoes", new ArrayList<>());
        campanha.put("consequencias", new ArrayList<>());
        campanha.put("eventLog", new ArrayList<>());
        campanha.put("mensagensCena", new ArrayList<>());
        campanha.put("mundoVivo", mundoVivo);
        return campanha;
    }
}
