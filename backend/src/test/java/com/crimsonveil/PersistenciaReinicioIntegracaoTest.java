package com.crimsonveil;

import com.crimsonveil.dto.CampanhaCriacaoRequest;
import com.crimsonveil.dto.CelularContatoCriacaoRequest;
import com.crimsonveil.dto.CelularMensagemCriacaoRequest;
import com.crimsonveil.dto.MutacaoCampanhaRequest;
import com.crimsonveil.service.CampanhaService;
import com.crimsonveil.service.CelularService;
import com.crimsonveil.service.PersonagemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenciaReinicioIntegracaoTest {
    @TempDir
    Path diretorioTemporario;

    @Test
    void recuperaCampanhaHistoricoContatosEMensagensAposRecriarContexto() throws Exception {
        Path banco = diretorioTemporario.resolve("persistencia-reinicio.sqlite").toAbsolutePath().normalize();
        String campanhaId = "camp-reinicio";
        String contatoId;

        try (ConfigurableApplicationContext contexto = iniciarAplicacao(banco)) {
            CampanhaService campanhas = contexto.getBean(CampanhaService.class);
            CelularService celular = contexto.getBean(CelularService.class);
            PersonagemService personagens = contexto.getBean(PersonagemService.class);
            Map<String, Object> inicial = campanhaInicial(campanhaId);
            campanhas.criar(new CampanhaCriacaoRequest(campanhaId, "Campanha de reinício", inicial));

            Map<String, Object> atualizada = campanhaInicial(campanhaId);
            Map<String, Object> estadoMundo = (Map<String, Object>) atualizada.get("estadoMundo");
            estadoMundo.put("horarioAtual", "03:45");
            estadoMundo.put("localAtual", "Arquivo Municipal");
            atualizada.put("contadorAcoes", 1);
            atualizada.put("decisoes", List.of("Preservou o livro-caixa como evidência."));
            atualizada.put("consequencias", List.of("O arquivo permanece sob vigilância."));
            atualizada.put("pistas", List.of(Map.of("id", "PISTA-REINICIO", "estado", "ANALISADA")));
            atualizada.put("eventLog", List.of(Map.of("id", "EV-REINICIO", "descricao", "Livro-caixa encontrado.")));

            campanhas.registrarAcao(campanhaId, new MutacaoCampanhaRequest(
                    "acao-reinicio",
                    1L,
                    atualizada,
                    "Examinar o livro-caixa."
            ));

            contatoId = celular.criarContato(campanhaId, new CelularContatoCriacaoRequest(
                    "contato-reinicio",
                    2L,
                    "Inspetora Vale",
                    "PROFISSIONAL",
                    "npc-inspetora-vale"
            )).contato().id();
            celular.enviarMensagem(campanhaId, contatoId, new CelularMensagemCriacaoRequest(
                    "mensagem-reinicio",
                    3L,
                    "O livro-caixa foi preservado."
            ));
            personagens.salvarImagem("char-teste", "image/png", pngInicial());
        }

        try (ConfigurableApplicationContext contexto = iniciarAplicacao(banco)) {
            CampanhaService campanhas = contexto.getBean(CampanhaService.class);
            CelularService celular = contexto.getBean(CelularService.class);
            PersonagemService personagens = contexto.getBean(PersonagemService.class);
            JdbcTemplate jdbc = contexto.getBean(JdbcTemplate.class);

            var recuperada = campanhas.carregar(campanhaId);
            assertEquals(4, recuperada.versao());
            assertEquals("14 de Outubro de 2026",
                    valorAninhado(recuperada.campanha(), "estadoMundo", "dataAtual"));
            assertEquals("03:45", valorAninhado(recuperada.campanha(), "estadoMundo", "horarioAtual"));
            assertEquals("Arquivo Municipal", valorAninhado(recuperada.campanha(), "estadoMundo", "localAtual"));
            assertEquals(1, recuperada.campanha().get("contadorAcoes"));
            assertEquals(List.of("Preservou o livro-caixa como evidência."),
                    recuperada.campanha().get("decisoes"));
            assertEquals(List.of("O arquivo permanece sob vigilância."),
                    recuperada.campanha().get("consequencias"));
            assertEquals("ANALISADA",
                    ((Map<?, ?>) ((List<?>) recuperada.campanha().get("pistas")).get(0)).get("estado"));
            assertEquals("COLETADA",
                    ((Map<?, ?>) ((List<?>) recuperada.campanha().get("evidencias")).get(0)).get("estado"));
            assertEquals(1, ((List<?>) recuperada.campanha().get("eventLog")).size());
            assertEquals(2, campanhas.listarHistorico(campanhaId).size());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ? AND event_id = ?",
                    Integer.class, campanhaId, "EV-REINICIO"));
            assertEquals("Examinar o livro-caixa.",
                    campanhas.listarHistorico(campanhaId).get(1).descricao());

            var contatos = celular.listarContatos(campanhaId);
            assertEquals(1, contatos.contatos().size());
            assertEquals(contatoId, contatos.contatos().get(0).id());
            assertEquals("npc-inspetora-vale", contatos.contatos().get(0).personagemCanonicoId());

            var mensagens = celular.listarMensagens(campanhaId, contatoId);
            assertEquals(1, mensagens.mensagens().size());
            assertEquals("O livro-caixa foi preservado.", mensagens.mensagens().get(0).conteudo());
            assertEquals("03:45", mensagens.mensagens().get(0).horarioFiccional());
            assertEquals("image/png", personagens.buscarImagem("char-teste").mimeType());
            assertEquals(pngInicial().length, personagens.buscarImagem("char-teste").dados().length);
        }
    }

    private ConfigurableApplicationContext iniciarAplicacao(Path banco) {
        return new SpringApplicationBuilder(CrimsonVeilApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=jdbc:sqlite:" + banco,
                        "--spring.sql.init.mode=always",
                        "--spring.main.banner-mode=off"
                );
    }

    private Map<String, Object> campanhaInicial(String id) {
        Map<String, Object> protagonista = Map.of("id", "char-teste", "nome", "Investigadora de Teste");
        Map<String, Object> estadoMundo = new LinkedHashMap<>();
        estadoMundo.put("dataAtual", "14 de Outubro de 2026");
        estadoMundo.put("horarioAtual", "03:30");
        estadoMundo.put("localAtual", "Apartamento 504");
        estadoMundo.put("personagensPresentes", List.of("Investigadora de Teste"));

        Map<String, Object> mundoVivo = new LinkedHashMap<>();
        mundoVivo.put("eventosAgendados", new ArrayList<>(List.of(
                Map.of("id", "EV-REINICIO", "status", "AGENDADO", "horarioPrevisto", "04:00")
        )));
        mundoVivo.put("eventosOcorridos", new ArrayList<>());

        Map<String, Object> campanha = new LinkedHashMap<>();
        campanha.put("id", id);
        campanha.put("titulo", "Campanha de reinício");
        campanha.put("codigo", "REINICIO");
        campanha.put("status", "EM INVESTIGAÇÃO");
        campanha.put("protagonista", protagonista);
        campanha.put("casoAtivo", Map.of("id", "caso-reinicio"));
        campanha.put("cenaAtual", Map.of("id", "cena-reinicio"));
        campanha.put("estadoMundo", estadoMundo);
        campanha.put("contadorAcoes", 0);
        campanha.put("pistas", List.of(Map.of("id", "PISTA-REINICIO", "estado", "NOVA")));
        campanha.put("evidencias", List.of(Map.of("id", "EVID-REINICIO", "estado", "COLETADA")));
        campanha.put("eventLog", new ArrayList<>());
        campanha.put("mensagensCena", new ArrayList<>());
        campanha.put("mundoVivo", mundoVivo);
        return campanha;
    }

    private String valorAninhado(Map<String, Object> objeto, String primeiro, String segundo) {
        return (String) ((Map<?, ?>) objeto.get(primeiro)).get(segundo);
    }

    private byte[] pngInicial() throws Exception {
        ByteArrayOutputStream imagem = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", imagem);
        return imagem.toByteArray();
    }
}
