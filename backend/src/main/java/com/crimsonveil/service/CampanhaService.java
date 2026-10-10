package com.crimsonveil.service;

import com.crimsonveil.dto.CampanhaCriacaoRequest;
import com.crimsonveil.dto.CampanhaResposta;
import com.crimsonveil.dto.CampanhaResumoResposta;
import com.crimsonveil.dto.HistoricoCampanhaResposta;
import com.crimsonveil.dto.MutacaoCampanhaRequest;
import com.crimsonveil.dto.ReiniciarCampanhaRequest;
import com.crimsonveil.entity.CampanhaRegistro;
import com.crimsonveil.exception.CampanhaNaoEncontradaException;
import com.crimsonveil.exception.ConflitoVersaoException;
import com.crimsonveil.exception.RegraCampanhaException;
import com.crimsonveil.repository.CampanhaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CampanhaService {
    private static final String CAMPANHA_CANONICA_ID = "camp-001";
    private final CampanhaRepository repository;
    private final ObjectMapper mapper;
    private final PersonagemService personagemService;

    public CampanhaService(CampanhaRepository repository, ObjectMapper mapper, PersonagemService personagemService) {
        this.repository = repository;
        this.mapper = mapper;
        this.personagemService = personagemService;
    }

    public List<CampanhaResumoResposta> listar() {
        return repository.listar();
    }

    public CampanhaResposta carregar(String id) {
        CampanhaRegistro registro = buscarObrigatoria(id);
        return new CampanhaResposta(registro.campanha(), registro.versao(), false);
    }

    public List<HistoricoCampanhaResposta> listarHistorico(String id) {
        buscarObrigatoria(id);
        return repository.listarHistorico(id);
    }

    @Transactional
    public CampanhaResposta criar(CampanhaCriacaoRequest request) {
        validarSnapshot(request.id(), request.campanha());
        String now = Instant.now().toString();
        Map<String, Object> campanha = personagemService.assegurarEAplicarPerfil(request.campanha());
        CampanhaResposta resposta = new CampanhaResposta(campanha, 1, false);
        repository.criar(request.id(), request.titulo(), texto(request.campanha().get("codigo")),
                texto(request.campanha().get("status")), serializar(campanha), now);
        repository.salvarEstadoInicialSeAusente(request.id(), serializar(campanha), now);
        persistirComplementos(request.id(), campanha);
        repository.salvarOperacao(request.id(), "criacao-" + request.id(), "CAMPANHA_CRIADA", "",
                serializarResposta(resposta), now);
        repository.salvarHistorico(request.id(), "criacao-" + request.id(), "CAMPANHA_CRIADA",
                "Campanha criada.", dataAtual(request.campanha()), horarioAtual(request.campanha()), 1, now);
        return resposta;
    }

    @Transactional
    public CampanhaResposta migrarCanonica(CampanhaCriacaoRequest request) {
        if (!CAMPANHA_CANONICA_ID.equals(request.id())) {
            throw new RegraCampanhaException("A migração inicial só aceita o identificador canônico camp-001.");
        }
        validarSnapshot(request.id(), request.campanha());
        Optional<CampanhaRegistro> existente = repository.buscar(CAMPANHA_CANONICA_ID);
        if (existente.isPresent()) {
            CampanhaRegistro registro = existente.get();
            return new CampanhaResposta(registro.campanha(), registro.versao(), true);
        }
        try {
            return criar(request);
        } catch (DuplicateKeyException exception) {
            CampanhaRegistro registro = buscarObrigatoria(CAMPANHA_CANONICA_ID);
            return new CampanhaResposta(registro.campanha(), registro.versao(), true);
        }
    }

    @Transactional
    public CampanhaResposta salvarEstado(String id, MutacaoCampanhaRequest request) {
        return mutar(id, request, "ESTADO_ATUALIZADO", false);
    }

    @Transactional
    public CampanhaResposta registrarAcao(String id, MutacaoCampanhaRequest request) {
        return mutar(id, request, "ACAO_JOGADOR", true);
    }

    @Transactional
    public CampanhaResposta reiniciar(String id, ReiniciarCampanhaRequest request) {
        Optional<String> repetida = repository.buscarOperacao(id, request.chaveOperacao());
        if (repetida.isPresent()) {
            CampanhaResposta original = lerResposta(repetida.get());
            return new CampanhaResposta(original.campanha(), original.versao(), true);
        }

        CampanhaRegistro atual = buscarObrigatoria(id);
        if (atual.versao() != request.versaoEsperada()) {
            throw new ConflitoVersaoException(id);
        }

        Map<String, Object> inicial = repository.buscarEstadoInicial(id)
                .orElseGet(request::campanhaInicial);
        validarSnapshot(id, inicial);
        Map<?, ?> protagonistaAtual = (Map<?, ?>) atual.campanha().get("protagonista");
        Map<?, ?> protagonistaInicial = (Map<?, ?>) inicial.get("protagonista");
        if (!protagonistaAtual.get("id").equals(protagonistaInicial.get("id"))) {
            throw new RegraCampanhaException("O reinício precisa manter a mesma personagem da campanha.");
        }
        if (CAMPANHA_CANONICA_ID.equals(id)
                && (!(inicial.get("casoAtivo") instanceof Map<?, ?> caso)
                || !"caso-001".equals(caso.get("id")))) {
            throw new RegraCampanhaException("O reinício canônico precisa usar o estado inicial do Caso 001.");
        }

        Map<String, Object> campanhaInicial = personagemService.assegurarEAplicarPerfil(inicial);
        String now = Instant.now().toString();
        repository.salvarEstadoInicialSeAusente(id, serializar(campanhaInicial), now);
        long novaVersao = atual.versao() + 1;
        if (repository.reiniciar(id, atual.versao(), texto(campanhaInicial.get("titulo")),
                texto(campanhaInicial.get("codigo")), texto(campanhaInicial.get("status")),
                serializar(campanhaInicial), now) != 1) {
            throw new ConflitoVersaoException(id);
        }
        repository.limparProgresso(id);
        persistirComplementos(id, campanhaInicial);

        CampanhaResposta resposta = new CampanhaResposta(campanhaInicial, novaVersao, false);
        repository.salvarOperacao(id, request.chaveOperacao(), "CAMPANHA_REINICIADA", "",
                serializarResposta(resposta), now);
        return resposta;
    }

    private CampanhaResposta mutar(String id, MutacaoCampanhaRequest request, String type, boolean action) {
        validarSnapshot(id, request.campanha());
        String actionText = request.acao() == null ? "" : request.acao().trim();
        if (action && actionText.isBlank()) {
            throw new RegraCampanhaException("A ação da jogadora não pode estar vazia.");
        }

        Optional<String> repetida = repository.buscarOperacao(id, request.chaveOperacao());
        if (repetida.isPresent()) {
            CampanhaResposta original = lerResposta(repetida.get());
            return new CampanhaResposta(original.campanha(), original.versao(), true);
        }

        CampanhaRegistro atual = buscarObrigatoria(id);
        if (atual.versao() != request.versaoEsperada()) {
            throw new ConflitoVersaoException(id);
        }
        validarIdentidadePersistida(atual.campanha(), request.campanha(), id);

        long novaVersao = atual.versao() + 1;
        String now = Instant.now().toString();
        int updated = repository.atualizar(id, atual.versao(), texto(request.campanha().get("titulo")),
                texto(request.campanha().get("codigo")), texto(request.campanha().get("status")),
                serializar(request.campanha()), now);
        if (updated != 1) {
            throw new ConflitoVersaoException(id);
        }

        persistirComplementos(id, request.campanha());
        CampanhaResposta resposta = new CampanhaResposta(request.campanha(), novaVersao, false);
        String responseJson = serializarResposta(resposta);
        repository.salvarOperacao(id, request.chaveOperacao(), type, actionText, responseJson, now);
        repository.salvarHistorico(id, request.chaveOperacao(), type,
                action ? actionText : "Estado da campanha atualizado.",
                dataAtual(request.campanha()), horarioAtual(request.campanha()), novaVersao, now);
        return resposta;
    }

    private void persistirComplementos(String id, Map<String, Object> campaign) {
        Object world = campaign.get("estadoMundo");
        if (!(world instanceof Map<?, ?> worldState)) {
            throw new RegraCampanhaException("O estado do mundo da campanha está ausente.");
        }
        Map<String, Object> typedWorldState = new java.util.LinkedHashMap<>();
        worldState.forEach((key, value) -> {
            if (key instanceof String stringKey) {
                typedWorldState.put(stringKey, value);
            }
        });
        repository.salvarEstadoMundo(id, typedWorldState, serializar(campaign));
        repository.salvarEventos(id, campaign);
    }

    private void validarSnapshot(String id, Map<String, Object> campaign) {
        if (campaign == null || !id.equals(campaign.get("id"))) {
            throw new RegraCampanhaException("O identificador da campanha não corresponde ao snapshot enviado.");
        }
        if (!(campaign.get("protagonista") instanceof Map<?, ?> protagonist)
                || texto(protagonist.get("id")).isBlank() || texto(protagonist.get("nome")).isBlank()) {
            throw new RegraCampanhaException("A campanha precisa preservar a identidade da protagonista.");
        }
        if (!(campaign.get("estadoMundo") instanceof Map<?, ?> world)) {
            throw new RegraCampanhaException("O estado do mundo da campanha é obrigatório.");
        }
        String currentDate = texto(world.get("dataAtual"));
        String currentTime = texto(world.get("horarioAtual"));
        String location = texto(world.get("localAtual"));
        if (currentDate.isBlank() || location.isBlank()
                || !currentTime.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
            throw new RegraCampanhaException("Data, horário HH:mm e localização atuais são obrigatórios.");
        }
        Object counter = campaign.get("contadorAcoes");
        if (!(counter instanceof Number number) || number.doubleValue() < 0
                || !Double.isFinite(number.doubleValue())
                || number.doubleValue() != Math.rint(number.doubleValue())) {
            throw new RegraCampanhaException("O contador de ações deve ser um inteiro não negativo.");
        }
        if (CAMPANHA_CANONICA_ID.equals(id)) {
            if (!"char-milena".equals(protagonist.get("id"))
                    || !"Milena Ramires".equals(protagonist.get("nome"))) {
                throw new RegraCampanhaException("A campanha camp-001 deve preservar Milena Ramires.");
            }
            if (!(campaign.get("casoAtivo") instanceof Map<?, ?> activeCase)
                    || !"caso-001".equals(activeCase.get("id"))) {
                throw new RegraCampanhaException("A campanha camp-001 deve preservar o identificador do Caso 001.");
            }
        }
    }

    private void validarIdentidadePersistida(Map<String, Object> current, Map<String, Object> candidate, String id) {
        Map<?, ?> savedProtagonist = (Map<?, ?>) current.get("protagonista");
        Map<?, ?> nextProtagonist = (Map<?, ?>) candidate.get("protagonista");
        if (!savedProtagonist.get("id").equals(nextProtagonist.get("id"))) {
            throw new RegraCampanhaException("O identificador da protagonista não pode ser substituído.");
        }
        for (String collection : List.of("pistas", "evidencias")) {
            List<String> savedIds = ids(current.get(collection));
            List<String> nextIds = ids(candidate.get(collection));
            if (!nextIds.containsAll(savedIds)) {
                throw new RegraCampanhaException("A operação não pode remover dados persistidos de " + collection + ".");
            }
        }
        if (CAMPANHA_CANONICA_ID.equals(id)) {
            if (!"char-milena".equals(nextProtagonist.get("id"))
                    || !"Milena Ramires".equals(nextProtagonist.get("nome"))) {
                throw new RegraCampanhaException("A identidade canônica da protagonista não pode ser alterada.");
            }
            Object savedCase = current.get("casoAtivo");
            Object nextCase = candidate.get("casoAtivo");
            if (savedCase instanceof Map<?, ?> saved
                    && (!(nextCase instanceof Map<?, ?> next) || !saved.get("id").equals(next.get("id")))) {
                throw new RegraCampanhaException("O identificador do Caso 001 não pode ser substituído.");
            }
        }
    }

    private List<String> ids(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map && map.get("id") != null) {
                result.add(map.get("id").toString());
            }
        }
        return result;
    }

    private CampanhaRegistro buscarObrigatoria(String id) {
        CampanhaRegistro registro = repository.buscar(id)
                .orElseThrow(() -> new CampanhaNaoEncontradaException(id));
        Map<String, Object> campanha = personagemService.assegurarEAplicarPerfil(registro.campanha());
        return new CampanhaRegistro(registro.id(), registro.titulo(), registro.codigo(), registro.status(),
                campanha, registro.versao(), registro.criadaEm(), registro.atualizadaEm());
    }

    private String dataAtual(Map<String, Object> campaign) {
        return texto(((Map<?, ?>) campaign.get("estadoMundo")).get("dataAtual"));
    }

    private String horarioAtual(Map<String, Object> campaign) {
        return texto(((Map<?, ?>) campaign.get("estadoMundo")).get("horarioAtual"));
    }

    private String texto(Object value) {
        return value == null ? "" : value.toString();
    }

    private String serializar(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Não foi possível serializar os dados da campanha.", exception);
        }
    }

    private String serializarResposta(CampanhaResposta response) {
        return serializar(response);
    }

    private CampanhaResposta lerResposta(String value) {
        try {
            return mapper.readValue(value, CampanhaResposta.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Resposta de operação persistida inválida.", exception);
        }
    }
}
