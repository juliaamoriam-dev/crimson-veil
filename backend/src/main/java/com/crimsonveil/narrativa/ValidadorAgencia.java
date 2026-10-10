package com.crimsonveil.narrativa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ValidadorAgencia {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidadorAgencia.class);

    private static final Pattern VIOLACOES_SEGUNDA_PESSOA = Pattern.compile(
            "(?i)\\b(você\\s+(?:sentiu|pensou|decidiu|concluiu|hesitou|começou\\s+a\\s+chorar|ficou\\s+(?:assustad[ao]|em\\s+pânico)|engoliu\\s+em\\s+seco))\\b"
    );

    public String validarEAjustar(String textoNarrativo, String nomeProtagonista) {
        if (textoNarrativo == null || textoNarrativo.isBlank()) {
            return "";
        }

        String textoAjustado = textoNarrativo;

        Matcher matcherSegundaPessoa = VIOLACOES_SEGUNDA_PESSOA.matcher(textoAjustado);
        if (matcherSegundaPessoa.find()) {
            LOGGER.warn("Possível violação de agência (2ª pessoa) detectada no texto da IA: '{}'", matcherSegundaPessoa.group());
            textoAjustado = textoAjustado.replaceAll("(?i)\\bvocê\\s+sentiu\\s+um\\s+frio\\s+na\\s+espinha\\b", "um arrepio percorreu o ambiente");
            textoAjustado = textoAjustado.replaceAll("(?i)\\bvocê\\s+sentiu\\b", "foi perceptível");
            textoAjustado = textoAjustado.replaceAll("(?i)\\bvocê\\s+pensou\\b", "o silêncio sugere");
            textoAjustado = textoAjustado.replaceAll("(?i)\\bvocê\\s+hesitou\\b", "houve um instante de pausa");
        }

        if (nomeProtagonista != null && !nomeProtagonista.isBlank()) {
            String regexProtagonista = "(?i)\\b" + Pattern.quote(nomeProtagonista)
                    + "\\s+(?:sentiu|pensou|decidiu|concluiu|hesitou|ficou\\s+assustad[ao]|tremeu|começou\\s+a\\s+chorar)\\b";
            Pattern patternProtagonista = Pattern.compile(regexProtagonista);
            Matcher matcherProtagonista = patternProtagonista.matcher(textoAjustado);
            if (matcherProtagonista.find()) {
                LOGGER.warn("Possível violação de agência com nome da protagonista detectada: '{}'", matcherProtagonista.group());
                textoAjustado = patternProtagonista.matcher(textoAjustado).replaceAll("O ambiente permaneceu sob tensão enquanto");
            }
        }

        return textoAjustado.trim();
    }
}
