import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
    DURACAO_PADRAO_MINUTOS,
    diasNoMes,
    horarioParaMinutos,
    minutosParaHorario,
    parseDataCanonica,
    formatarDataCanonica,
    avancarDataDeterministica,
    calcularDuracaoAcao,
    aplicarAvancoTempo
} from '../frontend/js/core/tempo.js';

describe('Motor de Tempo Determinístico — Crimson Veil', () => {

    describe('Conversão e Formatação de Horários', () => {
        test('Converte horário "HH:mm" em minutos corretamente', () => {
            assert.strictEqual(horarioParaMinutos('00:00'), 0);
            assert.strictEqual(horarioParaMinutos('02:17'), 137);
            assert.strictEqual(horarioParaMinutos('23:59'), 1439);
        });

        test('Rejeita formatos de horários inválidos ou fora de faixa', () => {
            assert.throws(() => horarioParaMinutos('24:00'), /Horário com formato inválido/);
            assert.throws(() => horarioParaMinutos('02:60'), /Horário com formato inválido/);
            assert.throws(() => horarioParaMinutos('02:62'), /Horário com formato inválido/);
            assert.throws(() => horarioParaMinutos(''), /Horário com formato inválido/);
            assert.throws(() => horarioParaMinutos(null), /Horário inválido/);
            assert.throws(() => horarioParaMinutos(undefined), /Horário inválido/);
        });

        test('Converte minutos para string formatada "HH:mm"', () => {
            assert.deepStrictEqual(minutosParaHorario(0), { horario: '00:00', diasCompletos: 0, minutosRestantes: 0 });
            assert.deepStrictEqual(minutosParaHorario(137), { horario: '02:17', diasCompletos: 0, minutosRestantes: 137 });
            assert.deepStrictEqual(minutosParaHorario(180), { horario: '03:00', diasCompletos: 0, minutosRestantes: 180 });
            assert.deepStrictEqual(minutosParaHorario(1440), { horario: '00:00', diasCompletos: 1, minutosRestantes: 0 });
        });

        test('Rejeita valores inválidos de minutos', () => {
            assert.throws(() => minutosParaHorario(-1), /Total de minutos inválido/);
            assert.throws(() => minutosParaHorario(NaN), /Total de minutos inválido/);
            assert.throws(() => minutosParaHorario('137'), /Total de minutos inválido/);
        });
    });

    describe('Cálculo de Dias no Mês e Anos Bissextos', () => {
        test('Calcula dias em meses comuns (30 e 31 dias)', () => {
            assert.strictEqual(diasNoMes(2026, 1), 31);  // Janeiro
            assert.strictEqual(diasNoMes(2026, 4), 30);  // Abril
            assert.strictEqual(diasNoMes(2026, 10), 31); // Outubro
        });

        test('Trata Fevereiro em ano não bissexto (2026 = 28 dias)', () => {
            assert.strictEqual(diasNoMes(2026, 2), 28);
        });

        test('Trata Fevereiro em ano bissexto (2024 e 2028 = 29 dias)', () => {
            assert.strictEqual(diasNoMes(2024, 2), 29);
            assert.strictEqual(diasNoMes(2028, 2), 29);
            assert.strictEqual(diasNoMes(2000, 2), 29); // Bissexto divisível por 400
            assert.strictEqual(diasNoMes(1900, 2), 28); // Século não divisível por 400
        });
    });

    describe('Manipulação Determinística de Datas', () => {
        test('Converte datas canônicas por extenso e formata de volta', () => {
            const data = parseDataCanonica('14 de Outubro de 2026');
            assert.deepStrictEqual(data, { dia: 14, mes: 10, ano: 2026 });
            assert.strictEqual(formatarDataCanonica(data), '14 de Outubro de 2026');
        });

        test('Avança data com virada simples de dia', () => {
            const res = avancarDataDeterministica({ dia: 14, mes: 10, ano: 2026 }, 1);
            assert.deepStrictEqual(res, { dia: 15, mes: 10, ano: 2026 });
        });

        test('Trata virada de mês (31 de Outubro para 1 de Novembro)', () => {
            const res = avancarDataDeterministica({ dia: 31, mes: 10, ano: 2026 }, 1);
            assert.deepStrictEqual(res, { dia: 1, mes: 11, ano: 2026 });
            assert.strictEqual(formatarDataCanonica(res), '01 de Novembro de 2026');
        });

        test('Trata virada de ano (31 de Dezembro de 2026 para 1 de Janeiro de 2027)', () => {
            const res = avancarDataDeterministica({ dia: 31, mes: 12, ano: 2026 }, 1);
            assert.deepStrictEqual(res, { dia: 1, mes: 1, ano: 2027 });
            assert.strictEqual(formatarDataCanonica(res), '01 de Janeiro de 2027');
        });
    });

    describe('Separação de Cálculo de Duração e Ausência de Efeito Colateral', () => {
        test('calcularDuracaoAcao retorna duração padrão sem alterar relógio', () => {
            const duracao = calcularDuracaoAcao('Examinar poltrona');
            assert.strictEqual(duracao, DURACAO_PADRAO_MINUTOS);
            assert.strictEqual(duracao, 5);
        });

        test('calcularDuracaoAcao aceita duração customizada explícita', () => {
            assert.strictEqual(calcularDuracaoAcao('Deslocamento longo', 45), 45);
        });

        test('calcularDuracaoAcao rejeita valores negativos ou inválidos', () => {
            assert.throws(() => calcularDuracaoAcao('Ação', -10), /Duração customizada inválida/);
            assert.throws(() => calcularDuracaoAcao('Ação', NaN), /Duração customizada inválida/);
        });
    });

    describe('Aplicação Controlada do Avanço de Tempo', () => {
        test('1. Avanço regular de 5 minutos preserva o formato', () => {
            const estadoMundo = { horarioAtual: '02:17', dataAtual: '14 de Outubro de 2026' };
            const res = aplicarAvancoTempo(estadoMundo, 5);

            assert.strictEqual(res.sucesso, true);
            assert.strictEqual(res.avancou, true);
            assert.strictEqual(res.horarioAnterior, '02:17');
            assert.strictEqual(res.horarioNovo, '02:22');
            assert.strictEqual(estadoMundo.horarioAtual, '02:22');
            assert.strictEqual(estadoMundo.dataAtual, '14 de Outubro de 2026');
        });

        test('2. Passagem de 02:55 para 03:00 (virada de hora, nunca gerando 02:60)', () => {
            const estadoMundo = { horarioAtual: '02:55', dataAtual: '14 de Outubro de 2026' };
            const res = aplicarAvancoTempo(estadoMundo, 5);

            assert.strictEqual(res.horarioNovo, '03:00');
            assert.strictEqual(estadoMundo.horarioAtual, '03:00');
            assert.strictEqual(res.diasAvancados, 0);
        });

        test('3. Passagem da meia-noite (23:55 para 00:05)', () => {
            const estadoMundo = { horarioAtual: '23:55', dataAtual: '14 de Outubro de 2026' };
            const res = aplicarAvancoTempo(estadoMundo, 10);

            assert.strictEqual(res.horarioNovo, '00:05');
            assert.strictEqual(res.diasAvancados, 1);
            assert.strictEqual(estadoMundo.horarioAtual, '00:05');
            assert.strictEqual(estadoMundo.dataAtual, '15 de Outubro de 2026');
        });

        test('4. Mudança de dia e mês através do avanço de horário', () => {
            const estadoMundo = { horarioAtual: '23:50', dataAtual: '31 de Outubro de 2026' };
            const res = aplicarAvancoTempo(estadoMundo, 15);

            assert.strictEqual(res.horarioNovo, '00:05');
            assert.strictEqual(res.diasAvancados, 1);
            assert.strictEqual(estadoMundo.dataAtual, '01 de Novembro de 2026');
        });

        test('5. Mudança de ano através do avanço de horário', () => {
            const estadoMundo = { horarioAtual: '23:50', dataAtual: '31 de Dezembro de 2026' };
            const res = aplicarAvancoTempo(estadoMundo, 20);

            assert.strictEqual(res.horarioNovo, '00:10');
            assert.strictEqual(res.diasAvancados, 1);
            assert.strictEqual(estadoMundo.dataAtual, '01 de Janeiro de 2027');
        });

        test('6. Avanço por várias horas (36 horas = 2160 minutos)', () => {
            const estadoMundo = { horarioAtual: '02:00', dataAtual: '14 de Outubro de 2026' };
            const res = aplicarAvancoTempo(estadoMundo, 2160);

            assert.strictEqual(res.horarioNovo, '14:00');
            assert.strictEqual(res.diasAvancados, 1);
            assert.strictEqual(estadoMundo.dataAtual, '15 de Outubro de 2026');
        });

        test('7. Duração zero é permitida e não altera o relógio nem a data', () => {
            const estadoMundo = { horarioAtual: '02:17', dataAtual: '14 de Outubro de 2026' };
            const res = aplicarAvancoTempo(estadoMundo, 0);

            assert.strictEqual(res.sucesso, true);
            assert.strictEqual(res.avancou, false);
            assert.strictEqual(res.horarioNovo, '02:17');
            assert.strictEqual(estadoMundo.horarioAtual, '02:17');
            assert.strictEqual(estadoMundo.dataAtual, '14 de Outubro de 2026');
        });

        test('8. Rejeição de valores negativos ou inválidos no avanço', () => {
            const estadoMundo = { horarioAtual: '02:17', dataAtual: '14 de Outubro de 2026' };
            assert.throws(() => aplicarAvancoTempo(estadoMundo, -5), /Duração em minutos inválida/);
            assert.throws(() => aplicarAvancoTempo(estadoMundo, NaN), /Duração em minutos inválida/);
            assert.throws(() => aplicarAvancoTempo(null, 5), /estadoMundo é obrigatório/);
        });

        test('9. Determinismo: mesmas entradas produzem rigorosamente a mesma saída', () => {
            const estado1 = { horarioAtual: '02:17', dataAtual: '14 de Outubro de 2026' };
            const estado2 = { horarioAtual: '02:17', dataAtual: '14 de Outubro de 2026' };

            const res1 = aplicarAvancoTempo(estado1, 23);
            const res2 = aplicarAvancoTempo(estado2, 23);

            assert.deepStrictEqual(res1, res2);
            assert.deepStrictEqual(estado1, estado2);
        });

        test('10. Proteção contra avanço duplicado (idempotência por idAcao)', () => {
            const estadoMundo = { horarioAtual: '02:17', dataAtual: '14 de Outubro de 2026' };
            const idAcao = 'acao-turno-42';

            // Primeira aplicação: deve avançar
            const res1 = aplicarAvancoTempo(estadoMundo, 5, idAcao);
            assert.strictEqual(res1.sucesso, true);
            assert.strictEqual(res1.avancou, true);
            assert.strictEqual(estadoMundo.horarioAtual, '02:22');

            // Segunda aplicação do mesmo id: deve bloquear
            const res2 = aplicarAvancoTempo(estadoMundo, 5, idAcao);
            assert.strictEqual(res2.sucesso, false);
            assert.strictEqual(res2.avancou, false);
            assert.strictEqual(res2.motivo, 'ACAO_JA_PROCESSADA');
            assert.strictEqual(estadoMundo.horarioAtual, '02:22'); // Permanece inalterado
        });

        test('11. Teste de Regressão: 10 ações consecutivas a partir de 02:17 nunca geram 02:62', () => {
            const estadoMundo = { horarioAtual: '02:17', dataAtual: '14 de Outubro de 2026' };
            const historicoHorarios = [];

            for (let i = 1; i <= 10; i++) {
                const res = aplicarAvancoTempo(estadoMundo, 5, `acao-caso001-${i}`);
                assert.strictEqual(res.sucesso, true);
                historicoHorarios.push(estadoMundo.horarioAtual);
            }

            // Garante que todos os horários são válidos (minutos <= 59)
            historicoHorarios.forEach(h => {
                const partes = h.split(':');
                const min = parseInt(partes[1], 10);
                assert.ok(min >= 0 && min < 60, `Minutos inválidos no horário gerado: ${h}`);
            });

            // Na 9ª ação, o bug antigo gerava '02:62'. O motor determinístico deve gerar '03:02':
            assert.strictEqual(historicoHorarios[7], '02:57'); // Ação 8
            assert.strictEqual(historicoHorarios[8], '03:02'); // Ação 9 (antigo bug 02:62)
            assert.strictEqual(historicoHorarios[9], '03:07'); // Ação 10
            assert.strictEqual(estadoMundo.horarioAtual, '03:07');
        });
    });
});
