package com.crimsonveil.narrativa;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Componente responsável por derivar sugestões investigativas contextuais e canônicas
 * a partir do estado atual da campanha (local, personagens presentes, pistas conhecidas e histórico).
 * <p>
 * Atua como garantia de continuidade para que a jogadora sempre tenha entre 3 e 5 opções
 * inteligentes e coerentes com a cena, mesmo caso o modelo de IA omita o bloco de sugestões.
 */
@Component
public class GeradorSugestoesContextuais {

    public List<String> gerar(Map<String, Object> campanha, String textoNarracaoRecente) {
        Set<String> sugestoes = new LinkedHashSet<>();

        String localAtual = extrairLocalAtual(campanha);
        List<String> presentes = extrairPersonagensPresentes(campanha);
        List<String> pistas = extrairTitulosPistas(campanha);
        List<String> acoesRecentes = extrairUltimasAcoes(campanha);

        // 1. Ações com personagens presentes na cena
        for (String personagem : presentes) {
            String pLimpo = personagem.trim();
            if (pLimpo.equalsIgnoreCase("Adrian Hale") || pLimpo.equalsIgnoreCase("Adrian")) {
                adicionarSeNaoRepetida(sugestoes, "Consultar Capitão Adrian sobre a estratégia da diligência", acoesRecentes);
            } else if (pLimpo.equalsIgnoreCase("Dra. Maya Navarro") || pLimpo.equalsIgnoreCase("Dra. Maya") || pLimpo.equalsIgnoreCase("Maya")) {
                adicionarSeNaoRepetida(sugestoes, "Questionar Dra. Maya sobre as conclusões preliminares da perícia", acoesRecentes);
            } else if (pLimpo.equalsIgnoreCase("Noah Whitmore") || pLimpo.equalsIgnoreCase("Noah")) {
                adicionarSeNaoRepetida(sugestoes, "Solicitar a Noah a análise de logs de rede e telemetria", acoesRecentes);
            } else if (pLimpo.equalsIgnoreCase("Helena Voss") || pLimpo.equalsIgnoreCase("Helena")) {
                adicionarSeNaoRepetida(sugestoes, "Pedir a análise tática da Detetive Helena Voss", acoesRecentes);
            } else if (!pLimpo.isBlank()) {
                adicionarSeNaoRepetida(sugestoes, "Interrogar " + pLimpo + " sobre o que presenciou na cena", acoesRecentes);
            }
            if (sugestoes.size() >= 4) break;
        }

        // 2. Ações relacionadas às pistas descobertas
        for (String pista : pistas) {
            if (!pista.isBlank()) {
                adicionarSeNaoRepetida(sugestoes, "Examinar em detalhes a evidência: " + pista, acoesRecentes);
            }
            if (sugestoes.size() >= 4) break;
        }

        // 3. Ações no ambiente / local atual
        if (!localAtual.isBlank()) {
            adicionarSeNaoRepetida(sugestoes, "Vasculhar o perímetro em busca de vestígios ocultos em " + localAtual, acoesRecentes);
        }

        // 4. Ações investigativas forenses gerais para garantir variedade
        adicionarSeNaoRepetida(sugestoes, "Checar acessos, câmeras e registros de entrada do local", acoesRecentes);
        adicionarSeNaoRepetida(sugestoes, "Revisar as últimas anotações no bloco de notas investigativo", acoesRecentes);
        adicionarSeNaoRepetida(sugestoes, "Examinar pontos de arrombamento ou interferência física na área", acoesRecentes);

        // Limita ao intervalo recomendado de 3 a 5 sugestões
        List<String> listaFinal = new ArrayList<>(sugestoes);
        int max = Math.min(5, Math.max(3, listaFinal.size()));
        return listaFinal.subList(0, max);
    }

    private void adicionarSeNaoRepetida(Set<String> sugestoes, String candidata, List<String> acoesRecentes) {
        if (candidata == null || candidata.isBlank()) {
            return;
        }
        String texto = candidata.trim();
        // Evita duplicar na própria lista ou sugerir exatamente o que acabou de ser feito
        for (String recente : acoesRecentes) {
            if (recente.equalsIgnoreCase(texto)) {
                return;
            }
        }
        sugestoes.add(texto);
    }

    private String extrairLocalAtual(Map<String, Object> campanha) {
        Object mundoObj = campanha.get("estadoMundo");
        if (mundoObj instanceof Map<?, ?> mundo && mundo.get("localAtual") != null) {
            return mundo.get("localAtual").toString();
        }
        return "Cena do Crime";
    }

    @SuppressWarnings("unchecked")
    private List<String> extrairPersonagensPresentes(Map<String, Object> campanha) {
        Object mundoObj = campanha.get("estadoMundo");
        if (mundoObj instanceof Map<?, ?> mundo && mundo.get("personagensPresentes") instanceof List<?> lista) {
            return lista.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extrairTitulosPistas(Map<String, Object> campanha) {
        Object pistasObj = campanha.get("pistas");
        if (pistasObj instanceof List<?> lista) {
            List<String> titulos = new ArrayList<>();
            for (Object item : lista) {
                if (item instanceof Map<?, ?> pista) {
                    Object t = pista.get("titulo") != null ? pista.get("titulo") : pista.get("id");
                    if (t != null) {
                        titulos.add(t.toString());
                    }
                }
            }
            return titulos;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extrairUltimasAcoes(Map<String, Object> campanha) {
        Object msgObj = campanha.get("mensagensCena");
        if (msgObj instanceof List<?> lista) {
            List<String> ultimas = new ArrayList<>();
            for (Object item : lista) {
                if (item instanceof Map<?, ?> msg) {
                    Object tipo = msg.get("tipo");
                    Object conteudo = msg.get("conteudo");
                    if (tipo != null && "JOGADOR".equalsIgnoreCase(tipo.toString()) && conteudo != null) {
                        ultimas.add(conteudo.toString().trim());
                    }
                }
            }
            return ultimas;
        }
        return List.of();
    }
}
