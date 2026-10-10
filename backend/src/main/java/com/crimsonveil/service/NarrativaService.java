package com.crimsonveil.service;

import com.crimsonveil.dto.AcaoNarrativaRequest;
import com.crimsonveil.dto.AcaoNarrativaResposta;
import com.crimsonveil.entity.CampanhaRegistro;
import com.crimsonveil.exception.ConflitoVersaoException;
import com.crimsonveil.exception.RegraCampanhaException;
import com.crimsonveil.narrativa.MotorNarrativoService;
import com.crimsonveil.narrativa.ResultadoTurnoNarrativo;
import com.crimsonveil.repository.CampanhaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NarrativaService {
    private final CampanhaRepository repository;
    private final CampanhaService campanhaService;
    private final MotorNarrativoService motorNarrativoService;
    private final ObjectMapper mapper;

    public NarrativaService(
            CampanhaRepository repository,
            CampanhaService campanhaService,
            MotorNarrativoService motorNarrativoService,
            ObjectMapper mapper
    ) {
        this.repository = repository;
        this.campanhaService = campanhaService;
        this.motorNarrativoService = motorNarrativoService;
        this.mapper = mapper;
    }

    @Transactional
    public AcaoNarrativaResposta processarAcao(String campanhaId, AcaoNarrativaRequest request) {
        if (request.acao() == null || request.acao().trim().isBlank()) {
            throw new RegraCampanhaException("A ação da jogadora não pode estar vazia.");
        }

        // 1. Verificação de Idempotência
        Optional<String> repetida = repository.buscarOperacao(campanhaId, request.chaveOperacao());
        if (repetida.isPresent()) {
            AcaoNarrativaResposta original = lerResposta(repetida.get());
            return new AcaoNarrativaResposta(
                    original.campanha(),
                    original.versao(),
                    true,
                    original.textoNarracao(),
                    original.modelo(),
                    original.horarioAtual(),
                    original.duracaoMinutos(),
                    original.sugestoes() != null ? original.sugestoes() : List.of()
            );
        }

        // 2. Validação da versão otimista e carregamento da campanha existente
        CampanhaRegistro atual = campanhaService.buscarObrigatoria(campanhaId);
        if (atual.versao() != request.versaoEsperada()) {
            throw new ConflitoVersaoException(campanhaId);
        }

        // 3. Execução da IA e regras narrativas
        ResultadoTurnoNarrativo resultado = motorNarrativoService.processarTurno(atual.campanha(), request.acao());

        // 4. Clonagem e mutação controlada do snapshot da campanha
        Map<String, Object> snapshotAtualizado = clonarSnapshot(atual.campanha());
        String horarioAntigo = texto(((Map<?, ?>) snapshotAtualizado.get("estadoMundo")).get("horarioAtual"));
        String novoHorario = resultado.novoHorario();
        String novaData = resultado.novaData();

        // 4.1. Registro na cena
        List<Map<String, Object>> mensagensCena = extrairListaMutavel(snapshotAtualizado, "mensagensCena");
        mensagensCena.add(new LinkedHashMap<>(Map.of(
                "tipo", "JOGADOR",
                "conteudo", request.acao().trim(),
                "horario", horarioAntigo
        )));
        mensagensCena.add(new LinkedHashMap<>(Map.of(
                "tipo", "NARRADOR",
                "conteudo", resultado.textoNarracao(),
                "horario", novoHorario
        )));

        // 4.2. Registro no log de eventos
        List<Map<String, Object>> eventLog = extrairListaMutavel(snapshotAtualizado, "eventLog");
        eventLog.add(new LinkedHashMap<>(Map.of(
                "tipo", "ACAO_PROTAGONISTA",
                "descricao", request.acao().trim(),
                "horario", horarioAntigo
        )));
        eventLog.add(new LinkedHashMap<>(Map.of(
                "tipo", "RESPOSTA_NARRATIVA",
                "descricao", "Resposta narrativa no horário " + novoHorario,
                "horario", novoHorario
        )));

        // 4.3. Atualização do estado do mundo e contador
        Map<String, Object> estadoMundo = extrairMapaMutavel(snapshotAtualizado, "estadoMundo");
        estadoMundo.put("horarioAtual", novoHorario);
        estadoMundo.put("dataAtual", novaData);

        int contadorAcoes = ((Number) snapshotAtualizado.getOrDefault("contadorAcoes", 0)).intValue() + 1;
        snapshotAtualizado.put("contadorAcoes", contadorAcoes);
        snapshotAtualizado.put("ultimaAtividade", "Hoje, " + novoHorario);

        Object casoAtivo = snapshotAtualizado.get("casoAtivo");
        if (casoAtivo instanceof Map<?, ?> caso && caso.get("codigo") != null) {
            snapshotAtualizado.put("progresso", "Ações: " + contadorAcoes + " | " + caso.get("codigo"));
        } else {
            snapshotAtualizado.put("progresso", "Ações: " + contadorAcoes + " | Prólogo");
        }

        // 4.4. Registro de sugestões contextuais dinâmicas
        snapshotAtualizado.put("sugestoesAcoes", resultado.sugestoes());

        // 5. Persistência atômica no banco SQLite
        long novaVersao = atual.versao() + 1;
        String now = Instant.now().toString();

        int atualizados = repository.atualizar(
                campanhaId,
                atual.versao(),
                texto(snapshotAtualizado.get("titulo")),
                texto(snapshotAtualizado.get("codigo")),
                texto(snapshotAtualizado.get("status")),
                serializar(snapshotAtualizado),
                now
        );
        if (atualizados != 1) {
            throw new ConflitoVersaoException(campanhaId);
        }

        campanhaService.persistirComplementos(campanhaId, snapshotAtualizado);

        AcaoNarrativaResposta resposta = new AcaoNarrativaResposta(
                snapshotAtualizado,
                novaVersao,
                false,
                resultado.textoNarracao(),
                resultado.modelo(),
                novoHorario,
                resultado.duracaoMinutos(),
                resultado.sugestoes()
        );

        String respostaJson = serializar(resposta);
        repository.salvarOperacao(
                campanhaId,
                request.chaveOperacao(),
                "ACAO_NARRATIVA_GEMINI",
                request.acao().trim(),
                respostaJson,
                now
        );
        repository.salvarHistorico(
                campanhaId,
                request.chaveOperacao(),
                "ACAO_NARRATIVA_GEMINI",
                request.acao().trim(),
                novaData,
                novoHorario,
                novaVersao,
                now
        );

        return resposta;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> clonarSnapshot(Map<String, Object> original) {
        try {
            return mapper.readValue(mapper.writeValueAsString(original), LinkedHashMap.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível clonar o snapshot da campanha.", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extrairListaMutavel(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor instanceof List<?> lista) {
            List<Map<String, Object>> mutavel = new ArrayList<>();
            for (Object item : lista) {
                if (item instanceof Map<?, ?> submap) {
                    mutavel.add(new LinkedHashMap<>((Map<String, Object>) submap));
                }
            }
            mapa.put(chave, mutavel);
            return mutavel;
        }
        List<Map<String, Object>> nova = new ArrayList<>();
        mapa.put(chave, nova);
        return nova;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extrairMapaMutavel(Map<String, Object> mapa, String chave) {
        Object valor = mapa.get(chave);
        if (valor instanceof Map<?, ?> submap) {
            Map<String, Object> mutavel = new LinkedHashMap<>((Map<String, Object>) submap);
            mapa.put(chave, mutavel);
            return mutavel;
        }
        Map<String, Object> novo = new LinkedHashMap<>();
        mapa.put(chave, novo);
        return novo;
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private String serializar(Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Erro ao serializar resposta narrativa.", exception);
        }
    }

    private AcaoNarrativaResposta lerResposta(String valor) {
        try {
            return mapper.readValue(valor, AcaoNarrativaResposta.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Resposta narrativa em cache corrompida.", exception);
        }
    }
}
