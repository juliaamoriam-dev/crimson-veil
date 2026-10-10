/**
 * Crimson Veil — Motor de Tempo Determinístico (Core)
 * 
 * Responsável pela representação, cálculo, avanço e formatação do tempo ficcional.
 * Funciona de forma 100% determinística, sem depender do relógio do sistema operacional.
 * Trata rigorosamente viradas de hora, dia, mês (incluindo bissextos) e ano.
 */

// Duração padrão configurável por ação quando nenhuma classificação específica for aplicada.
// Limitação documentada: na Etapa 2, todas as ações cotidianas utilizam este padrão explícito.
export const DURACAO_PADRAO_MINUTOS = 5;

// Nomes canônicos dos meses em português
export const MESES_CANONICOS = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
];

/**
 * Retorna o número de dias de um determinado mês e ano (respeita anos bissextos).
 * @param {number} ano 
 * @param {number} mes (1 a 12)
 * @returns {number}
 */
export function diasNoMes(ano, mes) {
    if (typeof ano !== 'number' || typeof mes !== 'number' || mes < 1 || mes > 12) {
        throw new Error(`Mês ou ano inválido para cálculo de dias: ano=${ano}, mes=${mes}`);
    }
    if (mes === 2) {
        const bissexto = (ano % 4 === 0 && ano % 100 !== 0) || (ano % 400 === 0);
        return bissexto ? 29 : 28;
    }
    if (mes === 4 || mes === 6 || mes === 9 || mes === 11) {
        return 30;
    }
    return 31;
}

/**
 * Converte string "HH:mm" em total de minutos decorridos desde 00:00 (0 a 1439).
 * Lança erro para formatos ou valores inválidos.
 * @param {string} horarioStr 
 * @returns {number}
 */
export function horarioParaMinutos(horarioStr) {
    if (typeof horarioStr !== 'string') {
        throw new Error(`Horário inválido: esperado string no formato HH:mm, recebido ${typeof horarioStr}`);
    }
    const regex = /^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$/;
    const match = horarioStr.trim().match(regex);
    if (!match) {
        throw new Error(`Horário com formato inválido: "${horarioStr}". Formato esperado: "HH:mm" (00:00 a 23:59).`);
    }
    const horas = parseInt(match[1], 10);
    const minutos = parseInt(match[2], 10);
    return horas * 60 + minutos;
}

/**
 * Converte total de minutos em string "HH:mm" e calcula quantos dias inteiros foram completados.
 * @param {number} totalMinutos 
 * @returns {{ horario: string, diasCompletos: number, minutosRestantes: number }}
 */
export function minutosParaHorario(totalMinutos) {
    if (typeof totalMinutos !== 'number' || isNaN(totalMinutos) || totalMinutos < 0) {
        throw new Error(`Total de minutos inválido: ${totalMinutos}`);
    }
    const minutosPorDia = 1440; // 24 * 60
    const diasCompletos = Math.floor(totalMinutos / minutosPorDia);
    const minutosRestantes = totalMinutos % minutosPorDia;
    const horas = Math.floor(minutosRestantes / 60);
    const minutos = minutosRestantes % 60;
    const horario = `${String(horas).padStart(2, '0')}:${String(minutos).padStart(2, '0')}`;
    return { horario, diasCompletos, minutosRestantes };
}

/**
 * Converte uma data canônica por extenso (ex: "14 de Outubro de 2026")
 * ou no padrão ISO/numérico ("14/10/2026", "2026-10-14") para objeto { dia, mes, ano }.
 * @param {string} dataStr 
 * @returns {{ dia: number, mes: number, ano: number }}
 */
export function parseDataCanonica(dataStr) {
    if (typeof dataStr !== 'string') {
        throw new Error(`Data inválida: esperada string, recebido ${typeof dataStr}`);
    }
    const limpa = dataStr.trim();

    // Formato extenso: "14 de Outubro de 2026"
    const regexExtenso = /^(\d{1,2})\s+de\s+([A-Za-zÀ-ÿ]+)\s+de\s+(\d{4})$/i;
    const matchExtenso = limpa.match(regexExtenso);
    if (matchExtenso) {
        const dia = parseInt(matchExtenso[1], 10);
        const nomeMes = matchExtenso[2].toLowerCase();
        const ano = parseInt(matchExtenso[3], 10);
        const indexMes = MESES_CANONICOS.findIndex(m => m.toLowerCase() === nomeMes);
        if (indexMes === -1) {
            throw new Error(`Mês desconhecido na data: "${matchExtenso[2]}"`);
        }
        const mes = indexMes + 1;
        if (dia < 1 || dia > diasNoMes(ano, mes)) {
            throw new Error(`Dia ${dia} inválido para o mês ${nomeMes} de ${ano}`);
        }
        return { dia, mes, ano };
    }

    // Formato barra: "14/10/2026"
    const regexBarra = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/;
    const matchBarra = limpa.match(regexBarra);
    if (matchBarra) {
        const dia = parseInt(matchBarra[1], 10);
        const mes = parseInt(matchBarra[2], 10);
        const ano = parseInt(matchBarra[3], 10);
        if (mes < 1 || mes > 12 || dia < 1 || dia > diasNoMes(ano, mes)) {
            throw new Error(`Data inválida: "${dataStr}"`);
        }
        return { dia, mes, ano };
    }

    // Formato ISO: "2026-10-14"
    const regexIso = /^(\d{4})-(\d{2})-(\d{2})$/;
    const matchIso = limpa.match(regexIso);
    if (matchIso) {
        const ano = parseInt(matchIso[1], 10);
        const mes = parseInt(matchIso[2], 10);
        const dia = parseInt(matchIso[3], 10);
        if (mes < 1 || mes > 12 || dia < 1 || dia > diasNoMes(ano, mes)) {
            throw new Error(`Data inválida: "${dataStr}"`);
        }
        return { dia, mes, ano };
    }

    throw new Error(`Formato de data não reconhecido: "${dataStr}"`);
}

/**
 * Formata componentes { dia, mes, ano } no padrão canônico por extenso: "DD de Mês de AAAA".
 * @param {{ dia: number, mes: number, ano: number }} componentes 
 * @returns {string}
 */
export function formatarDataCanonica(componentes) {
    const { dia, mes, ano } = componentes;
    if (!dia || !mes || !ano || mes < 1 || mes > 12) {
        throw new Error(`Componentes de data inválidos para formatação: ${JSON.stringify(componentes)}`);
    }
    const nomeMes = MESES_CANONICOS[mes - 1];
    return `${String(dia).padStart(2, '0')} de ${nomeMes} de ${ano}`;
}

/**
 * Avança determinísticamente uma data em um número inteiro de dias.
 * Trata corretamente viradas de mês e de ano.
 * @param {{ dia: number, mes: number, ano: number }} dataObj 
 * @param {number} diasParaAvancar 
 * @returns {{ dia: number, mes: number, ano: number }}
 */
export function avancarDataDeterministica(dataObj, diasParaAvancar) {
    if (typeof diasParaAvancar !== 'number' || isNaN(diasParaAvancar) || diasParaAvancar < 0) {
        throw new Error(`Dias para avançar deve ser número >= 0. Recebido: ${diasParaAvancar}`);
    }
    let { dia, mes, ano } = dataObj;
    let restoDias = diasParaAvancar;

    while (restoDias > 0) {
        const totalNoMes = diasNoMes(ano, mes);
        const espacoNoMes = totalNoMes - dia;
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
    return { dia, mes, ano };
}

/**
 * Calcula a duração de uma ação sem modificar o relógio ou estado do jogo.
 * Permite prever e auditar o custo temporal antes da aplicação.
 * @param {string|object} acao Informação da ação ou texto submetido
 * @param {number} [duracaoCustomizada] Opcional, permite sobrescrever explicitamente a duração
 * @returns {number} Minutos calculados (sempre >= 0)
 */
export function calcularDuracaoAcao(acao, duracaoCustomizada = null) {
    if (duracaoCustomizada !== null && duracaoCustomizada !== undefined) {
        if (typeof duracaoCustomizada !== 'number' || isNaN(duracaoCustomizada) || duracaoCustomizada < 0) {
            throw new Error(`Duração customizada inválida: ${duracaoCustomizada}. Deve ser número >= 0.`);
        }
        return Math.floor(duracaoCustomizada);
    }
    // Na fase atual, retorna a duração padrão explícita configurada
    return DURACAO_PADRAO_MINUTOS;
}

/**
 * Aplica o avanço de tempo no estadoMundo de forma controlada e determinística.
 * 
 * Regras:
 * 1. Não cria uma segunda fonte de verdade: muta e valida unicamente o objeto `estadoMundo` oficial da campanha.
 * 2. Protege contra avanço duplicado se `idAcao` for fornecido e já tiver sido registrado em `estadoMundo.acoesProcessadas`.
 * 3. Garante que horários inválidos como "02:62" jamais sejam produzidos.
 * 
 * @param {object} estadoMundo Objeto de estado da campanha ativa (com horarioAtual e dataAtual)
 * @param {number} duracaoMinutos Duração em minutos a avançar (>= 0)
 * @param {string|number|null} [idAcao=null] Identificador único opcional da ação para proteção contra duplicidade
 * @returns {{
 *   sucesso: boolean,
 *   avancou: boolean,
 *   motivo?: string,
 *   horarioAnterior: string,
 *   horarioNovo: string,
 *   dataAnterior: string,
 *   dataNova: string,
 *   minutosAvancados: number,
 *   diasAvancados: number
 * }}
 */
export function aplicarAvancoTempo(estadoMundo, duracaoMinutos, idAcao = null) {
    if (!estadoMundo || typeof estadoMundo !== 'object') {
        throw new Error('estadoMundo é obrigatório e deve ser um objeto.');
    }
    if (typeof duracaoMinutos !== 'number' || isNaN(duracaoMinutos) || duracaoMinutos < 0) {
        throw new Error(`Duração em minutos inválida: ${duracaoMinutos}. Deve ser um número maior ou igual a zero.`);
    }

    // Inicializa estrutura de controle de idempotência se não existir
    if (!Array.isArray(estadoMundo.acoesProcessadas)) {
        estadoMundo.acoesProcessadas = [];
    }

    // Proteção contra avanço duplicado da mesma ação
    if (idAcao !== null && idAcao !== undefined && idAcao !== '') {
        const idStr = String(idAcao);
        if (estadoMundo.acoesProcessadas.includes(idStr)) {
            return {
                sucesso: false,
                avancou: false,
                motivo: 'ACAO_JA_PROCESSADA',
                horarioAnterior: estadoMundo.horarioAtual,
                horarioNovo: estadoMundo.horarioAtual,
                dataAnterior: estadoMundo.dataAtual,
                dataNova: estadoMundo.dataAtual,
                minutosAvancados: 0,
                diasAvancados: 0
            };
        }
        estadoMundo.acoesProcessadas.push(idStr);
    }

    const horarioAnterior = estadoMundo.horarioAtual || '00:00';
    const dataAnterior = estadoMundo.dataAtual || '14 de Outubro de 2026';

    const minutosBase = horarioParaMinutos(horarioAnterior);
    const minutosTotais = minutosBase + Math.floor(duracaoMinutos);

    const { horario: horarioNovo, diasCompletos } = minutosParaHorario(minutosTotais);

    let dataNova = dataAnterior;
    if (diasCompletos > 0) {
        try {
            const componentesData = parseDataCanonica(dataAnterior);
            const componentesNovos = avancarDataDeterministica(componentesData, diasCompletos);
            dataNova = formatarDataCanonica(componentesNovos);
        } catch {
            // Caso dataAtual tenha formato customizado não canônico, mantém data sem interromper o jogo
            dataNova = dataAnterior;
        }
    }

    // Atualiza a única fonte de verdade:
    estadoMundo.horarioAtual = horarioNovo;
    estadoMundo.dataAtual = dataNova;

    return {
        sucesso: true,
        avancou: duracaoMinutos > 0,
        horarioAnterior,
        horarioNovo,
        dataAnterior,
        dataNova,
        minutosAvancados: Math.floor(duracaoMinutos),
        diasAvancados: diasCompletos
    };
}
