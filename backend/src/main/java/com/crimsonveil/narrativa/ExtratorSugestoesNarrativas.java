package com.crimsonveil.narrativa;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Componente responsável por extrair a narração limpa e as sugestões de ação dinâmicas
 * a partir da resposta gerada pelo modelo Google Gemini.
 * <p>
 * Garante que o texto exibido no terminal fique sem tags estruturais e que a jogadora
 * receba de 3 a 5 sugestões específicas e válidas.
 */
@Component
public class ExtratorSugestoesNarrativas {

    private static final List<String> MARCADORES = List.of(
            "===SUGESTÕES DE AÇÃO===",
            "===SUGESTOES DE ACAO===",
            "===SUGESTÕES===",
            "===SUGESTOES===",
            "---SUGESTÕES DE AÇÃO---",
            "---SUGESTOES DE ACAO---",
            "---SUGESTOES---",
            "SUGESTÕES DE AÇÃO:",
            "SUGESTOES DE ACAO:"
    );

    private static final Pattern PREFIXO_LISTA = Pattern.compile("^\\s*([\\-*•]|\\d+[.)])\\s*");

    private final GeradorSugestoesContextuais geradorContextual;

    public ExtratorSugestoesNarrativas(GeradorSugestoesContextuais geradorContextual) {
        this.geradorContextual = geradorContextual;
    }

    public record ResultadoExtracao(String textoNarracao, List<String> sugestoes) {
    }

    public ResultadoExtracao extrair(String textoBruto, Map<String, Object> campanha) {
        if (textoBruto == null || textoBruto.isBlank()) {
            List<String> fallback = geradorContextual.gerar(campanha, "");
            return new ResultadoExtracao("", fallback);
        }

        String texto = textoBruto.trim();
        int indiceMarcador = -1;
        int tamanhoMarcador = 0;

        for (String marcador : MARCADORES) {
            int pos = texto.toUpperCase().indexOf(marcador.toUpperCase());
            if (pos != -1) {
                indiceMarcador = pos;
                tamanhoMarcador = marcador.length();
                break;
            }
        }

        // Caso o modelo não inclua marcador reconhecido
        if (indiceMarcador == -1) {
            List<String> sugestoesFallback = geradorContextual.gerar(campanha, texto);
            return new ResultadoExtracao(texto, sugestoesFallback);
        }

        String parteNarracao = texto.substring(0, indiceMarcador).trim();
        String parteSugestoes = texto.substring(indiceMarcador + tamanhoMarcador).trim();

        List<String> sugestoesExtraidas = processarLinhasSugestoes(parteSugestoes);

        // Se o modelo retornou menos de 3 sugestões válidas, complementa com as contextuais
        if (sugestoesExtraidas.size() < 3) {
            List<String> contextuais = geradorContextual.gerar(campanha, parteNarracao);
            Set<String> combinadas = new LinkedHashSet<>(sugestoesExtraidas);
            for (String ctx : contextuais) {
                if (combinadas.size() >= 5) break;
                combinadas.add(ctx);
            }
            sugestoesExtraidas = new ArrayList<>(combinadas);
        }

        // Limita a no máximo 5 sugestões
        if (sugestoesExtraidas.size() > 5) {
            sugestoesExtraidas = sugestoesExtraidas.subList(0, 5);
        }

        return new ResultadoExtracao(parteNarracao, sugestoesExtraidas);
    }

    private List<String> processarLinhasSugestoes(String blocoSugestoes) {
        List<String> resultado = new ArrayList<>();
        if (blocoSugestoes == null || blocoSugestoes.isBlank()) {
            return resultado;
        }

        String[] linhas = blocoSugestoes.split("\\r?\\n");
        for (String linha : linhas) {
            String limpa = linha.trim();
            if (limpa.isEmpty() || limpa.startsWith("#")) {
                continue;
            }

            // Remove marcadores de item: -, *, 1., 2)
            limpa = PREFIXO_LISTA.matcher(limpa).replaceFirst("").trim();

            // Remove aspas ou colchetes ao redor da sugestão se houver
            if (limpa.startsWith("[") && limpa.endsWith("]") && limpa.length() > 2) {
                limpa = limpa.substring(1, limpa.length() - 1).trim();
            }
            if (limpa.startsWith("\"") && limpa.endsWith("\"") && limpa.length() > 2) {
                limpa = limpa.substring(1, limpa.length() - 1).trim();
            }

            if (limpa.length() >= 5 && !resultado.contains(limpa)) {
                resultado.add(limpa);
            }
        }
        return resultado;
    }
}
