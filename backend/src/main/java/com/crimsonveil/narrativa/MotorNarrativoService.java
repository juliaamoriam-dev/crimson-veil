package com.crimsonveil.narrativa;

import com.crimsonveil.ia.GeminiClient;
import com.crimsonveil.ia.ResultadoIa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MotorNarrativoService {
    private final GeminiClient geminiClient;
    private final ValidadorAgencia validadorAgencia;
    private final MotorTempoService motorTempoService;
    private final ExtratorSugestoesNarrativas extratorSugestoes;

    @Autowired
    public MotorNarrativoService(
            GeminiClient geminiClient,
            ValidadorAgencia validadorAgencia,
            MotorTempoService motorTempoService,
            ExtratorSugestoesNarrativas extratorSugestoes
    ) {
        this.geminiClient = geminiClient;
        this.validadorAgencia = validadorAgencia;
        this.motorTempoService = motorTempoService;
        this.extratorSugestoes = extratorSugestoes;
    }

    public MotorNarrativoService(
            GeminiClient geminiClient,
            ValidadorAgencia validadorAgencia,
            MotorTempoService motorTempoService
    ) {
        this(geminiClient, validadorAgencia, motorTempoService, new ExtratorSugestoesNarrativas(new GeradorSugestoesContextuais()));
    }

    public ResultadoTurnoNarrativo processarTurno(Map<String, Object> campanha, String acao) {
        String systemInstruction = montarInstrucaoSistema();
        String promptUsuario = montarPromptUsuario(campanha, acao);

        ResultadoIa resultadoIa = geminiClient.gerarNarracao(systemInstruction, promptUsuario);

        ExtratorSugestoesNarrativas.ResultadoExtracao extracao = extratorSugestoes.extrair(resultadoIa.texto(), campanha);

        String nomeProtagonista = extrairNomeProtagonista(campanha);
        String textoValidado = validadorAgencia.validarEAjustar(extracao.textoNarracao(), nomeProtagonista);

        int duracaoMinutos = motorTempoService.calcularDuracaoAcao(acao);
        Map<?, ?> estadoMundo = (Map<?, ?>) campanha.get("estadoMundo");
        String horarioAtual = estadoMundo != null && estadoMundo.get("horarioAtual") != null
                ? estadoMundo.get("horarioAtual").toString()
                : "03:30";
        String dataAtual = estadoMundo != null && estadoMundo.get("dataAtual") != null
                ? estadoMundo.get("dataAtual").toString()
                : "14 de Outubro de 2026";

        MotorTempoService.ResultadoAvancoTempo avanco = motorTempoService.avancarTempo(dataAtual, horarioAtual, duracaoMinutos);

        return new ResultadoTurnoNarrativo(
                textoValidado,
                resultadoIa.modelo(),
                duracaoMinutos,
                avanco.novoHorario(),
                avanco.novaData(),
                extracao.sugestoes()
        );
    }

    public String montarInstrucaoSistema() {
        return """
                Você é o Narrador oficial e Mestre de RPG do jogo 'Crimson Veil', uma série policial investigativa interativa ambientada na chuvosa e vertical Cidade de Blackwood.

                DIRETRIZES FUNDAMENTAIS:
                1. REGRA ABSOLUTA DE AGÊNCIA DA PROTAGONISTA:
                A protagonista pertence EXCLUSIVAMENTE à jogadora. Você NUNCA deve narrar pensamentos, sentimentos internos, decisões, falas, intenções ou reações corporais da protagonista.
                A sua resposta começa no instante imediatamente SEGUINTE à ação da jogadora. NUNCA repita, resuma ou reformule o que a protagonista acabou de fazer.

                2. ESTILO NARRATIVO:
                - Noir investigativo, cinematográfico, dinâmico e atmosférico (chuva nas janelas, reflexos úmidos no asfalto, lanternas periciais, café frio da delegacia).
                - Ritmo de série policial televisiva (Brooklyn Nine-Nine, The Rookie, Castle, True Detective).
                - Equilíbrio entre tensão investigativa, suspense e camaradagem profissional entre colegas da DCE.
                - Seja conciso e direto: produza de 2 a 4 parágrafos focados em acontecimentos observáveis, reações dos NPCs e detalhes do ambiente.

                3. DIÁLOGOS DE NPCS:
                - Quando NPCs falarem, use SEMPRE o formato canônico:
                Nome: — Fala do NPC.
                - Respeite as personalidades canônicas:
                * Adrian Hale: Capitão, líder firme, estratégico, humor seco discreto.
                * Helena Voss: Detetive sênior, racional, séria, profissional, tem dificuldade com ironias.
                * Noah Whitmore: Tecnologia e redes, inteligente, descontraído, café, alívio cômico orgânico.
                * Dra. Maya Navarro: Médica legista observadora, peculiar, humor negro científico.
                * Sofia Ramirez / Evelyn Vance / Iris Bell: quando presentes.

                4. INVESTIGAÇÃO E MISTÉRIO:
                - Não entregue segredos de bandeja. Responda realisticamente ao que a ação da protagonista investiga.
                - Se a jogadora examinar um ponto sem relevância, descreva o que ela realmente vê sem inventar inconsistências com o caso.
                - Termine a cena passando a iniciativa de volta para a jogadora agir.

                5. SUGESTÕES DE AÇÃO DINÂMICAS:
                Ao final de sua narração, adicione OBRIGATORIAMENTE uma seção com entre 3 e 5 sugestões concisas de ações que a protagonista pode tomar no próximo instante.
                Utilize rigorosamente o cabeçalho e formato a seguir:

                ===SUGESTÕES DE AÇÃO===
                - [Ação investigativa ou tática 1]
                - [Ação investigativa ou tática 2]
                - [Ação investigativa ou tática 3]
                - [Ação investigativa ou tática 4]

                Regras para as sugestões:
                - Devem ser concisas, focadas e em tom investigativo (ex: 'Examinar o batente da porta arrombada', 'Perguntar à Dra. Maya sobre a causa mortis', 'Consultar Adrian sobre o Depósito 217').
                - Devem decorrer diretamente dos fatos revelados, das pistas na cena ou dos NPCs presentes.
                - Nunca sugira ações genéricas como 'Continuar' ou 'Pensar sobre o caso'.
                - Não afirme fatos não comprovados nem tire a agência da protagonista.
                """;
    }

    public String montarPromptUsuario(Map<String, Object> campanha, String acao) {
        StringBuilder sb = new StringBuilder();

        // 1. Protagonista
        Object protObj = campanha.get("protagonista");
        String nomeProt = "Investigadora";
        String cargoProt = "DCE";
        String distintivoProt = "DCE";
        if (protObj instanceof Map<?, ?> prot) {
            if (prot.get("nome") != null) nomeProt = prot.get("nome").toString();
            if (prot.get("cargo") != null) cargoProt = prot.get("cargo").toString();
            if (prot.get("distintivo") != null) distintivoProt = prot.get("distintivo").toString();
        }

        // 2. Caso Ativo
        Object casoObj = campanha.get("casoAtivo");
        String casoInfo = "Prólogo — Saguão da Divisão de Crimes Especiais (Aguardando atribuição de inquérito)";
        if (casoObj instanceof Map<?, ?> caso && caso.get("titulo") != null) {
            casoInfo = caso.get("codigo") + " — " + caso.get("titulo");
            if (caso.get("subtitulo") != null) {
                casoInfo += " (" + caso.get("subtitulo") + ")";
            }
        }

        // 3. Estado do Mundo
        Object mundoObj = campanha.get("estadoMundo");
        String localAtual = "Central da DCE";
        String dataAtual = "14 de Outubro de 2026";
        String horarioAtual = "03:30";
        String clima = "Chuva constante sobre Blackwood";
        String presentes = "Helena Voss, Adrian Hale";
        if (mundoObj instanceof Map<?, ?> mundo) {
            if (mundo.get("localAtual") != null) localAtual = mundo.get("localAtual").toString();
            if (mundo.get("dataAtual") != null) dataAtual = mundo.get("dataAtual").toString();
            if (mundo.get("horarioAtual") != null) horarioAtual = mundo.get("horarioAtual").toString();
            if (mundo.get("clima") != null) clima = mundo.get("clima").toString();
            if (mundo.get("personagensPresentes") instanceof List<?> lista) {
                presentes = String.join(", ", lista.stream().map(Object::toString).toList());
            }
        }

        sb.append("=== CONTEXTO DA CENA ===\n");
        sb.append("Protagonista: ").append(nomeProt).append(" (Cargo: ").append(cargoProt).append(", Distintivo: ").append(distintivoProt).append(")\n");
        sb.append("Caso Ativo: ").append(casoInfo).append("\n");
        sb.append("Local Atual: ").append(localAtual).append("\n");
        sb.append("Data e Horário: ").append(dataAtual).append(", ").append(horarioAtual).append("\n");
        sb.append("Clima: ").append(clima).append("\n");
        sb.append("Pessoas Presentes: ").append(presentes).append("\n\n");

        // 4. Pistas conhecidas
        Object pistasObj = campanha.get("pistas");
        sb.append("=== PISTAS CONHECIDAS NA CENA ===\n");
        if (pistasObj instanceof List<?> pistas && !pistas.isEmpty()) {
            for (Object item : pistas) {
                if (item instanceof Map<?, ?> pista) {
                    Object tit = pista.get("titulo") != null ? pista.get("titulo") : pista.get("id");
                    Object desc = pista.get("detalhes") != null ? pista.get("detalhes") : pista.get("descricao");
                    sb.append("- ").append(tit != null ? tit.toString() : "Pista").append(": ")
                            .append(desc != null ? desc.toString() : "").append("\n");
                }
            }
        } else {
            sb.append("Nenhuma pista registrada até o momento.\n");
        }
        sb.append("\n");

        // 5. Histórico recente de mensagens da cena
        Object msgObj = campanha.get("mensagensCena");
        sb.append("=== HISTÓRICO RECENTE DA INVESTIGAÇÃO ===\n");
        if (msgObj instanceof List<?> mensagens && !mensagens.isEmpty()) {
            int total = mensagens.size();
            int inicio = Math.max(0, total - 8);
            for (int i = inicio; i < total; i++) {
                Object item = mensagens.get(i);
                if (item instanceof Map<?, ?> msg) {
                    String tipo = msg.get("tipo") != null ? msg.get("tipo").toString() : "SISTEMA";
                    String conteudo = msg.get("conteudo") != null ? msg.get("conteudo").toString() : "";
                    if (!conteudo.isBlank()) {
                        sb.append("[").append(tipo).append("]: ").append(conteudo.replace("\n", " ")).append("\n");
                    }
                }
            }
        } else {
            sb.append("Início da cena investigativa.\n");
        }
        sb.append("\n");

        // 6. Ação da Jogadora
        sb.append("=== AÇÃO CONCLUÍDA DA PROTAGONISTA ===\n");
        sb.append("\"").append(acao.trim()).append("\"\n\n");
        sb.append("Continue a narrativa a partir desta ação, descrevendo a reação imediata do ambiente e dos NPCs, e finalize com a seção ===SUGESTÕES DE AÇÃO===.");

        return sb.toString();
    }

    private String extrairNomeProtagonista(Map<String, Object> campanha) {
        Object protObj = campanha.get("protagonista");
        if (protObj instanceof Map<?, ?> prot && prot.get("nome") != null) {
            return prot.get("nome").toString();
        }
        return "Milena";
    }
}
