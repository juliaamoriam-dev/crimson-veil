package com.crimsonveil.narrativa;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MotorTempoService {
    public static final int DURACAO_PADRAO_MINUTOS = 5;

    private static final List<String> MESES = List.of(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    );

    private static final Pattern HORARIO_REGEX = Pattern.compile("^([01]?\\d|2[0-3]):([0-5]\\d)$");
    private static final Pattern DATA_EXTENSO_REGEX = Pattern.compile("^(\\d{1,2})\\s+de\\s+([A-Za-zÀ-ÿ]+)\\s+de\\s+(\\d{4})$");

    public int calcularDuracaoAcao(String textoAcao) {
        if (textoAcao == null || textoAcao.isBlank()) {
            return DURACAO_PADRAO_MINUTOS;
        }
        String acaoMinuscula = textoAcao.toLowerCase();
        // Ações que indicam viagens ou deslocamentos mais longos
        if (acaoMinuscula.contains("deslocar") || acaoMinuscula.contains("ir até")
                || acaoMinuscula.contains("ir para") || acaoMinuscula.contains("viagem")
                || acaoMinuscula.contains("pegar a viatura") || acaoMinuscula.contains("dirigir até")) {
            return 15;
        }
        return DURACAO_PADRAO_MINUTOS;
    }

    public ResultadoAvancoTempo avancarTempo(String dataAtual, String horarioAtual, int duracaoMinutos) {
        if (duracaoMinutos < 0) {
            throw new IllegalArgumentException("Duração em minutos não pode ser negativa.");
        }

        Matcher matcher = HORARIO_REGEX.matcher(horarioAtual.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Horário atual com formato inválido: " + horarioAtual);
        }

        int horas = Integer.parseInt(matcher.group(1));
        int minutos = Integer.parseInt(matcher.group(2));
        int totalMinutos = horas * 60 + minutos + duracaoMinutos;

        int minutosPorDia = 1440;
        int diasAvancados = totalMinutos / minutosPorDia;
        int minutosRestantes = totalMinutos % minutosPorDia;

        int novasHoras = minutosRestantes / 60;
        int novosMinutos = minutosRestantes % 60;
        String novoHorario = String.format("%02d:%02d", novasHoras, novosMinutos);

        String novaData = avancarData(dataAtual, diasAvancados);

        return new ResultadoAvancoTempo(novoHorario, novaData, duracaoMinutos, diasAvancados);
    }

    public String avancarData(String dataAtual, int diasParaAvancar) {
        if (diasParaAvancar == 0) {
            return dataAtual;
        }
        if (dataAtual == null || dataAtual.isBlank()) {
            return dataAtual;
        }

        Matcher matcher = DATA_EXTENSO_REGEX.matcher(dataAtual.trim());
        if (!matcher.matches()) {
            return dataAtual;
        }

        int dia = Integer.parseInt(matcher.group(1));
        String nomeMes = matcher.group(2);
        int ano = Integer.parseInt(matcher.group(3));

        int mes = 0;
        for (int i = 0; i < MESES.size(); i++) {
            if (MESES.get(i).equalsIgnoreCase(nomeMes)) {
                mes = i + 1;
                break;
            }
        }
        if (mes == 0) {
            return dataAtual;
        }

        int restoDias = diasParaAvancar;
        while (restoDias > 0) {
            int totalNoMes = diasNoMes(ano, mes);
            int espacoNoMes = totalNoMes - dia;
            if (restoDias <= espacoNoMes) {
                dia += restoDias;
                restoDias = 0;
            } else {
                restoDias -= (espacoNoMes + 1);
                dia = 1;
                mes += 1;
                if (mes > 12) {
                    mes = 1;
                    ano += 1;
                }
            }
        }

        return String.format("%02d de %s de %04d", dia, MESES.get(mes - 1), ano);
    }

    public int diasNoMes(int ano, int mes) {
        if (mes == 2) {
            boolean bissexto = (ano % 4 == 0 && ano % 100 != 0) || (ano % 400 == 0);
            return bissexto ? 29 : 28;
        }
        if (mes == 4 || mes == 6 || mes == 9 || mes == 11) {
            return 30;
        }
        return 31;
    }

    public record ResultadoAvancoTempo(
            String novoHorario,
            String novaData,
            int minutosAvancados,
            int diasAvancados
    ) {
    }
}
