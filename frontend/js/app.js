/**
 * Crimson Veil — Aplicação Frontend (Protótipo Navegável Oficial)
 * 
 * Cidade de Blackwood // Divisão de Crimes Especiais (DCE)
 * Integrado ao elenco oficial: Milena, Adrian, Helena, Noah, Maya, Iris, Sofia, Evelyn.
 */

import {
    dadosCampanha,
    dadosCenaAtual,
    dadosCasos,
    dadosPessoas,
    dadosLocais,
    dadosPistas,
    dadosEvidencias,
    dadosTimeline,
    dadosConexoes
} from './mocks/dadosIniciais.js';

import {
    calcularDuracaoAcao,
    aplicarAvancoTempo,
    horarioParaMinutos,
    minutosParaHorario
} from './core/tempo.js';

import {
    ApiError,
    carregarCampanha,
    carregarPerfilPersonagem,
    criarCampanha,
    listarCampanhas,
    migrarCampanhaCanonica,
    salvarAcao,
    salvarEstadoCampanha,
    reiniciarCampanha as reiniciarCampanhaNoServidor,
    removerImagemPersonagem,
    salvarImagemPersonagem,
    urlImagemPersonagem
} from './core/api.js';
import { renderizarCelular } from './core/celular.js';

// Estado local da sessão ativa do protótipo
const estadoLocal = {
    autenticado: false,
    abaAtiva: 'investigacao',
    horarioAtual: dadosCampanha.estadoMundo.horarioAtual,
    mensagensCena: [...dadosCenaAtual.mensagens],
    pistasDesc: [...dadosPistas],
    contadorAcoes: 0
};

/**
 * Cria a campanha canônica inicial (#001 — Milena Ramires & Caso 001).
 * Isolada para nunca sofrer mutação por outras campanhas.
 */
function criarCampanhaCanonicaInicial() {
    return {
        id: 'camp-001',
        titulo: dadosCampanha.titulo,
        codigo: dadosCampanha.casoAtivo.codigo,
        episodio: `Episódio ${dadosCampanha.episodio}`,
        status: 'EM INVESTIGAÇÃO',
        progresso: 'Episódio I — Cena ativa',
        ultimaAtividade: 'Hoje, 03:30',
        ativa: true,
        // Protagonista próprio de camp-001 (cópia isolada)
        protagonista: {
            id: dadosCampanha.protagonista.id,
            nome: dadosCampanha.protagonista.nome,
            idade: dadosCampanha.protagonista.idade,
            altura: dadosCampanha.protagonista.altura,
            cargo: dadosCampanha.protagonista.cargo,
            distintivo: dadosCampanha.protagonista.distintivo,
            origem: dadosCampanha.protagonista.origem,
            personalidade: dadosCampanha.protagonista.personalidade,
            regraOuro: dadosCampanha.protagonista.regraOuro,
            retratoSrc: 'assets/banco-de-personagens.jpg'
        },
        // Caso específico deliberado
        casoAtivo: {
            id: dadosCampanha.casoAtivo.id,
            codigo: dadosCampanha.casoAtivo.codigo,
            titulo: dadosCampanha.casoAtivo.titulo,
            subtitulo: 'A Morte de Arthur Vasconcelos',
            status: dadosCampanha.casoAtivo.status,
            localPrincipal: 'Apartamento 504 — Blackwood'
        },
        // Estado de mundo próprio
        estadoMundo: {
            dataAtual: dadosCampanha.estadoMundo.dataAtual,
            horarioAtual: dadosCampanha.estadoMundo.horarioAtual,
            localAtual: dadosCampanha.estadoMundo.localAtual,
            clima: dadosCampanha.estadoMundo.clima,
            personagensPresentes: [...dadosCampanha.estadoMundo.personagensPresentes]
        },
        cenaAtual: {
            id: dadosCenaAtual.id,
            titulo: dadosCenaAtual.titulo,
            local: dadosCenaAtual.local,
            horario: dadosCenaAtual.horario
        },
        mensagensCena: JSON.parse(JSON.stringify(dadosCenaAtual.mensagens)),
        pistas: JSON.parse(JSON.stringify(dadosPistas)),
        evidencias: JSON.parse(JSON.stringify(dadosEvidencias)),
        eventLog: [
            { tipo: 'SISTEMA', descricao: 'Investigação do Caso #001 iniciada no Apartamento 504', horario: '03:30' }
        ],
        memoria: {
            imediata: ['Corpo de Arthur Vasconcelos na escrivaninha', 'Relógios analógicos travados às 02:17'],
            episodica: ['Parada de 53 segundos no elevador social'],
            campanha: ['Divisão de Crimes Especiais assume inquérito confidencial']
        },
        contadorAcoes: 0,
        // Mundo Vivo — estado autônomo da campanha #001
        mundoVivo: {
            eventosAgendados: [
                {
                    id: 'EV-001-01',
                    tipo: 'EVENTO_NPC_MOVIMENTOU',
                    horarioPrevisto: '03:50',
                    local: 'Apartamento 504',
                    npcAlvo: 'Maya',
                    dados: { localAnterior: 'Apartamento 504', localNovo: 'Necrotério da DCE' },
                    status: 'AGENDADO',
                    descricao: 'Dra. Maya Navarro se deslocou para o Necrotério com o corpo de Arthur.',
                    revelarAoJogador: false
                },
                {
                    id: 'EV-001-02',
                    tipo: 'EVENTO_CLIMA',
                    horarioPrevisto: '04:05',
                    local: null,
                    npcAlvo: null,
                    dados: { climaAnterior: 'Chuva fria e constante sobre os edifícios de Blackwood', climaNovo: 'Tempestade com trovões sobre Blackwood' },
                    status: 'AGENDADO',
                    descricao: 'A tempestade se intensificou sobre Blackwood.',
                    revelarAoJogador: false
                },
                {
                    id: 'EV-001-03',
                    tipo: 'EVENTO_INVESTIGACAO',
                    horarioPrevisto: '04:20',
                    local: 'Central da DCE',
                    npcAlvo: 'Noah',
                    dados: { descoberta: 'anomalia eletromagnética nos logs do elevador isolada' },
                    status: 'AGENDADO',
                    descricao: 'Noah Whitmore identificou irregularidade eletromagnética nos logs do elevador.',
                    revelarAoJogador: false
                },
                {
                    id: 'EV-001-04',
                    tipo: 'EVENTO_LOCAL_ALTERADO',
                    horarioPrevisto: '04:35',
                    local: 'Apartamento 504',
                    npcAlvo: null,
                    dados: { mudanca: 'Levantamento da perícia concluído' },
                    status: 'AGENDADO',
                    descricao: 'Equipe de perícia concluiu o levantamento do Apartamento 504.',
                    revelarAoJogador: false
                }
            ],
            eventosOcorridos: [],
            consequencias: [], // Consequências ativas desta campanha (preenchido em tempo de execução)
            estadoNPCs: {
                'Helena': { local: 'Apartamento 504', disponivel: true },
                'Maya':   { local: 'Apartamento 504', disponivel: true },
                'Noah':   { local: 'Central da DCE',  disponivel: true },
                'Adrian': { local: 'Apartamento 504', disponivel: true }
            },
            estadoLocais: {
                'Apartamento 504': { status: 'isolado', detalhes: 'Cena ativa da DCE — perícia em andamento' },
                'Central da DCE':  { status: 'operacional', detalhes: 'Monitoramento ativo' },
                'Depósito 217':    { status: 'não visitado', detalhes: 'Alvo da próxima diligência' }
            }
        }
    };
}

/**
 * Cria o estado de uma Nova Campanha (Nova História).
 * NOVA CAMPANHA = NOVA HISTÓRIA + NOVO ESTADO.
 * Não herda caso, cena, pistas, evidências nem estado de campanhas anteriores.
 */
function criarNovaCampanhaEstado(novoId, novoProtagonista = null) {
    const nomePersonagem = (novoProtagonista && novoProtagonista.nome && novoProtagonista.nome.trim())
        ? novoProtagonista.nome.trim()
        : 'Investigador(a)';

    const fichaProtagonista = {
        id: (novoProtagonista && novoProtagonista.id) || `char-${novoId}`,
        nome: nomePersonagem,
        idade: (novoProtagonista && novoProtagonista.idade) || '28',
        altura: '—',
        cargo: (novoProtagonista && novoProtagonista.profissao) || 'Investigador(a) Independente',
        distintivo: (novoProtagonista && novoProtagonista.relacaoDCE === 'TRABALHO NA DCE') ? '#5019-DCE' : 'Consultor / Civil',
        origem: (novoProtagonista && novoProtagonista.origemTexto) || 'Chegada recente a Blackwood com credenciais operacionais.',
        detalhe: (novoProtagonista && novoProtagonista.detalheImportante) || '',
        personalidade: 'Observador(a), Resiliente',
        retratoSrc: (novoProtagonista && novoProtagonista.retratoSrc) || 'assets/retrato-mock.svg'
    };

    return {
        id: novoId,
        titulo: `Investigação de ${fichaProtagonista.nome}`,
        codigo: 'NOVA HISTÓRIA',
        episodio: 'Prólogo — Chegada a Blackwood',
        status: 'EM ANDAMENTO',
        progresso: 'Aguardando atribuição de caso',
        ultimaAtividade: 'Agora',
        ativa: true,
        protagonista: fichaProtagonista,
        // NÃO herda caso 001 automaticamente!
        casoAtivo: null,
        estadoMundo: {
            dataAtual: '14 de Outubro de 2026',
            horarioAtual: '21:00',
            localAtual: 'Saguão da DCE — Central de Blackwood',
            clima: 'Névoa fria cobrindo as ruas e sirenes distantes',
            personagensPresentes: [
                `${fichaProtagonista.nome} (Protagonista)`,
                'Capitão Adrian Hale (Comando da DCE)',
                'Noah Whitmore (Monitoramento & Redes)'
            ]
        },
        cenaAtual: {
            id: `cena-${novoId}-01`,
            titulo: `CRIMSON VEIL — Prólogo: A Chegada de ${fichaProtagonista.nome}`,
            local: 'Saguão da DCE',
            horario: '21:00'
        },
        mensagensCena: [
            {
                tipo: 'SISTEMA',
                conteudo: `CRIMSON VEIL // DIVISÃO DE CRIMES ESPECIAIS — CIDADE DE BLACKWOOD\nNOVA HISTÓRIA // PRÓLOGO\nSAGUÃO CENTRAL // 21:00`,
                horario: '21:00'
            },
            {
                tipo: 'NARRADOR',
                conteudo: `Saguão da DCE. 21h.\n\nA equipe trabalha em ritmo normal. Monitores ativos, conversas em volume baixo, café esquentando em alguma bancada.\n\nAdrian Hale está ao lado do quadro tático com uma pasta aberta. Ele levanta os olhos quando ${fichaProtagonista.nome} entra.\n\nAdrian: — Chegou na hora certa. Ou quase.\n\nEle fecha a pasta e faz um sinal em direção aos terminais.\n\nAdrian: — Noah vai te colocar no sistema.\n\nNoah Whitmore gira na cadeira e acena sem muita cerimônia.\n\nNoah: — Terminal configurado. Você tem acesso à rede interna e aos inquéritos em aberto. Qualquer problema, me chama.\n\nAdrian: — Quando estiver pronta, a aba de Casos tem o que temos em aberto. Pode começar por lá.\n\nNoah comenta sem levantar os olhos dos monitores.\n\nNoah: — Tem café ali na máquina do canto, se precisar.\n\nAdrian: — O café da máquina é terrível.\n\nNoah: — É. Mas é o que tem.`,
                horario: '21:00'
            }
        ],
        pistas: [], // Novo progresso: array vazio de pistas
        evidencias: [], // Array vazio de evidências
        eventLog: [
            { tipo: 'INICIO_CAMPANHA', descricao: `Nova investigação iniciada por ${fichaProtagonista.nome}`, horario: '21:00' }
        ],
        memoria: {
            imediata: ['Chegada ao Saguão da DCE', 'Terminal operacional habilitado'],
            episodica: [],
            campanha: [`Início da jornada investigativa de ${fichaProtagonista.nome}`]
        },
        contadorAcoes: 0,
        // Mundo Vivo — estado autônomo desta campanha
        mundoVivo: {
            eventosAgendados: [
                {
                    id: `EV-${novoId}-01`,
                    tipo: 'EVENTO_NOTICIA',
                    horarioPrevisto: '21:15',
                    local: null,
                    npcAlvo: null,
                    dados: { manchete: 'Morte no Edifício Solaris aguarda investigação da DCE' },
                    status: 'AGENDADO',
                    descricao: 'Notícia sobre morte no Edifício Solaris começa a circular nos canais locais.',
                    revelarAoJogador: false
                },
                {
                    id: `EV-${novoId}-02`,
                    tipo: 'EVENTO_CLIMA',
                    horarioPrevisto: '21:30',
                    local: null,
                    npcAlvo: null,
                    dados: { climaAnterior: 'Névoa fria cobrindo as ruas e sirenes distantes', climaNovo: 'Chuva começando sobre Blackwood' },
                    status: 'AGENDADO',
                    descricao: 'Chuva começa a cair sobre Blackwood.',
                    revelarAoJogador: false
                }
            ],
            eventosOcorridos: [],
            consequencias: [], // Consequências ativas desta campanha (preenchido em tempo de execução)
            estadoNPCs: {
                'Adrian': { local: 'Saguão da DCE', disponivel: true },
                'Noah':   { local: 'Saguão da DCE', disponivel: true }
            },
            estadoLocais: {
                'Saguão da DCE': { status: 'operacional', detalhes: 'Turno noturno em andamento' }
            }
        }
    };
}

/**
 * Gerenciador de campanhas autônomas com isolamento total de estado.
 */
const gerenciadorCampanhas = {
    campanhas: [
        criarCampanhaCanonicaInicial()
    ],
    versoes: new Map(),
    operacoesPendentes: new Map(),

    /**
     * Retorna a campanha atualmente ativa.
     */
    campanhaAtiva() {
        return this.campanhas.find(c => c.ativa) || this.campanhas[0];
    },

    async carregarDoServidor() {
        let resumos = await listarCampanhas();
        let idMaisRecente = null;
        if (resumos.length === 0) {
            const inicial = criarCampanhaCanonicaInicial();
            const migrada = await migrarCampanhaCanonica(inicial);
            this.campanhas = [migrada.campanha];
            this.versoes = new Map([[migrada.campanha.id, migrada.versao]]);
        } else {
            idMaisRecente = resumos.reduce((maisRecente, atual) =>
                new Date(atual.atualizadaEm) > new Date(maisRecente.atualizadaEm) ? atual : maisRecente
            ).id;
            const carregadas = await Promise.all(resumos.map(async resumo => {
                const resposta = await carregarCampanha(resumo.id);
                return { campanha: resposta.campanha, versao: resposta.versao };
            }));
            this.campanhas = carregadas.map(item => item.campanha);
            this.versoes = new Map(carregadas.map(item => [item.campanha.id, item.versao]));
        }

        const ativa = this.campanhas.find(campanha => campanha.id === idMaisRecente)
            || this.campanhas.find(campanha => campanha.ativa)
            || this.campanhas[0];
        this.campanhas.forEach(campanha => campanha.ativa = campanha.id === ativa.id);
        this.sincronizarEstadoLocalComAtiva();
    },

    versaoDaCampanha(id) {
        const versao = this.versoes.get(id);
        if (!Number.isInteger(versao)) {
            throw new Error(`Versão persistida indisponível para a campanha ${id}.`);
        }
        return versao;
    },

    definirVersao(id, versao) {
        const atual = this.versoes.get(id) || 0;
        this.versoes.set(id, Math.max(atual, versao));
    },

    substituirCampanhaPersistida(campanha, versao) {
        const indice = this.campanhas.findIndex(item => item.id === campanha.id);
        const eraAtiva = indice >= 0 && this.campanhas[indice].ativa;
        const substituta = { ...campanha, ativa: eraAtiva };
        if (indice >= 0) this.campanhas[indice] = substituta;
        else this.campanhas.push(substituta);
        this.versoes.set(campanha.id, versao);
        if (eraAtiva) this.sincronizarEstadoLocalComAtiva();
        return substituta;
    },

    async atualizarCampanhaDoServidor(id) {
        const resposta = await carregarCampanha(id);
        return this.substituirCampanhaPersistida(resposta.campanha, resposta.versao);
    },

    proximoIdCampanha() {
        let numero = this.campanhas.length + 1;
        let novoId = `camp-${String(numero).padStart(3, '0')}`;
        while (this.campanhas.some(campanha => campanha.id === novoId)) {
            numero++;
            novoId = `camp-${String(numero).padStart(3, '0')}`;
        }
        return novoId;
    },

    /**
     * Cria uma nova campanha com novo ID, novo personagem, nova história e novo estado limpo.
     */
    async criarNovaCampanha(novoProtagonista = null) {
        const novoId = this.proximoIdCampanha();
        const nova = criarNovaCampanhaEstado(novoId, novoProtagonista);
        const resposta = await criarCampanha(nova);
        this.campanhas.forEach(campanha => campanha.ativa = false);
        this.substituirCampanhaPersistida(resposta.campanha, resposta.versao);
        this.ativarCampanha(nova.id);
    },

    async criarNovaCampanhaComPersonagem(campanhaOrigem) {
        const novoId = this.proximoIdCampanha();
        const protagonista = JSON.parse(JSON.stringify(campanhaOrigem.protagonista));
        const nova = criarNovaCampanhaEstado(novoId, {
            ...protagonista,
            profissao: protagonista.cargo
        });
        nova.protagonista = { ...nova.protagonista, ...protagonista };
        const resposta = await criarCampanha(nova);
        this.campanhas.forEach(campanha => campanha.ativa = false);
        this.substituirCampanhaPersistida(resposta.campanha, resposta.versao);
        this.ativarCampanha(nova.id);
    },

    async reiniciarCampanha(id) {
        const campanha = this.campanhas.find(item => item.id === id);
        if (!campanha) throw new Error('A campanha selecionada não está carregada.');
        const inicial = criarEstadoInicialDaCampanha(campanha);
        const resposta = await reiniciarCampanhaNoServidor(id, {
            chaveOperacao: `reset-${crypto.randomUUID()}`,
            versaoEsperada: this.versaoDaCampanha(id),
            campanhaInicial: inicial
        });
        this.substituirCampanhaPersistida(resposta.campanha, resposta.versao);
        return resposta;
    },

    /**
     * Ativa uma campanha existente e restaura o seu estado integralmente.
     */
    ativarCampanha(id) {
        const alvo = this.campanhas.find(c => c.id === id);
        if (!alvo) return;

        this.campanhas.forEach(c => c.ativa = (c.id === id));

        // Sincroniza estadoLocal da sessão com a campanha ativada
        estadoLocal.mensagensCena = [...alvo.mensagensCena];
        estadoLocal.pistasDesc = [...alvo.pistas];
        estadoLocal.horarioAtual = alvo.estadoMundo.horarioAtual;
        estadoLocal.contadorAcoes = alvo.contadorAcoes || 0;

        // Atualiza UI global (Header e Relógio)
        atualizarProtagonistaUI(alvo.protagonista);
        atualizarRelogioUI();
    },

    /**
     * Sincroniza estadoLocal com a campanha atualmente ativa.
     */
    sincronizarEstadoLocalComAtiva() {
        const ativa = this.campanhaAtiva();
        if (ativa) {
            estadoLocal.mensagensCena = [...ativa.mensagensCena];
            estadoLocal.pistasDesc = [...ativa.pistas];
            estadoLocal.horarioAtual = ativa.estadoMundo.horarioAtual;
            estadoLocal.contadorAcoes = ativa.contadorAcoes || 0;
            atualizarProtagonistaUI(ativa.protagonista);
            atualizarRelogioUI();
        }
    }
};

function criarEstadoInicialDaCampanha(campanha) {
    if (campanha.id === 'camp-001') {
        const inicial = criarCampanhaCanonicaInicial();
        inicial.protagonista = JSON.parse(JSON.stringify(campanha.protagonista));
        return inicial;
    }

    const protagonista = JSON.parse(JSON.stringify(campanha.protagonista));
    const inicial = criarNovaCampanhaEstado(campanha.id, {
        ...protagonista,
        profissao: protagonista.cargo
    });
    inicial.protagonista = { ...inicial.protagonista, ...protagonista };
    return inicial;
}

/**
 * Atualiza o painel superior (Header) com as informações do protagonista ativo
 */
function atualizarProtagonistaUI(protagonista) {
    if (!protagonista) return;
    const elAvatar = document.querySelector('.perfil-avatar');
    const elNome = document.querySelector('.perfil-nome');
    const elCargo = document.querySelector('.perfil-cargo');

    if (elNome) elNome.textContent = protagonista.nome;
    if (elCargo) {
        const cargoTexto = protagonista.cargo || 'Investigador';
        const distintivoTexto = protagonista.distintivo ? ` (${protagonista.distintivo})` : '';
        elCargo.textContent = `${cargoTexto}${distintivoTexto}`;
    }
    if (elAvatar) {
        const iniciais = (protagonista.nome || 'MR')
            .split(' ')
            .filter(Boolean)
            .map(n => n[0])
            .slice(0, 2)
            .join('')
            .toUpperCase();
        const textoIniciais = document.createElement('span');
        textoIniciais.className = 'perfil-avatar-iniciais';
        textoIniciais.textContent = iniciais;
        const imagem = document.createElement('img');
        imagem.className = 'perfil-avatar-imagem';
        imagem.alt = '';
        imagem.hidden = true;
        imagem.onload = () => {
            imagem.hidden = false;
            textoIniciais.hidden = true;
        };
        imagem.onerror = () => {
            imagem.hidden = true;
            textoIniciais.hidden = false;
        };
        elAvatar.replaceChildren(textoIniciais, imagem);
        if (protagonista.id) {
            imagem.src = `${urlImagemPersonagem(protagonista.id)}?v=${Date.now()}`;
        }
    }
    atualizarCabecalhoCasoUI();
}

// ─── SISTEMA DE MUNDO VIVO ───────────────────────────────────────────────────

/**
 * Converte uma string "HH:MM" para o total de minutos (delega ao core tempo.js).
 */
function horaParaMinutos(horario) {
    try {
        return horarioParaMinutos(horario);
    } catch {
        const partes = (horario || '00:00').split(':');
        return (parseInt(partes[0], 10) || 0) * 60 + (parseInt(partes[1], 10) || 0);
    }
}

/**
 * Converte minutos totais de volta para string "HH:MM" (delega ao core tempo.js).
 */
function minutosParaHora(minutos) {
    try {
        return minutosParaHorario(minutos).horario;
    } catch {
        const h = Math.floor(minutos / 60) % 24;
        const m = minutos % 60;
        return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    }
}

/**
 * Processa um único evento do mundoVivo:
 * - Atualiza estadoNPCs, clima ou estadoLocais conforme o tipo.
 * - Move o evento para eventosOcorridos.
 * - Registra no eventLog interno (interno: true → não exibido no feed ao jogador automaticamente).
 */
function processarEvento(campAtiva, evento) {
    evento.status = 'CONCLUIDO';
    evento.horarioOcorrido = campAtiva.estadoMundo.horarioAtual;

    const mv = campAtiva.mundoVivo;

    switch (evento.tipo) {
        case 'EVENTO_NPC_MOVIMENTOU':
            if (evento.npcAlvo && mv.estadoNPCs[evento.npcAlvo]) {
                mv.estadoNPCs[evento.npcAlvo].local = evento.dados.localNovo;
            }
            break;

        case 'EVENTO_CLIMA':
            campAtiva.estadoMundo.clima = evento.dados.climaNovo;
            break;

        case 'EVENTO_LOCAL_ALTERADO':
            if (evento.local && mv.estadoLocais[evento.local]) {
                mv.estadoLocais[evento.local].status = evento.dados.mudanca;
            }
            break;

        case 'EVENTO_INVESTIGACAO':
        case 'EVENTO_NOTICIA':
        case 'EVENTO_POLICIAL':
        default:
            // Registrado no log; pode ser interpretado pelo Narrative Engine
            break;
    }

    // Confirma o evento como ocorrido
    mv.eventosOcorridos.push(Object.assign({}, evento));

    // Registra a consequência formal deste evento
    registrarConsequencia(campAtiva, evento);

    // Log interno — não exibido no feed de atividades automaticamente
    campAtiva.eventLog.push({
        tipo: 'EVENTO_MUNDO',
        descricao: `[MUNDO] ${evento.descricao}`,
        horario: evento.horarioOcorrido,
        interno: true
    });
}

/**
 * Mapeia um evento processado para a sua consequência investigativa.
 * O SISTEMA define o fato. A IA apenas o narra.
 *
 * Regras:
 * - Consequências nunca encerram a investigação.
 * - Cada consequência registra alternativas disponíveis.
 * - Consequências pertencem exclusivamente à campanha que as gerou.
 */
function registrarConsequencia(campAtiva, evento) {
    if (!campAtiva.mundoVivo) return;

    const mv = campAtiva.mundoVivo;
    let consequencia = null;

    switch (evento.tipo) {
        case 'EVENTO_NPC_MOVIMENTOU': {
            const npc = evento.npcAlvo || 'NPC';
            const localAnterior = (evento.dados && evento.dados.localAnterior) || evento.local || 'local anterior';
            const localNovo = (evento.dados && evento.dados.localNovo) || 'novo local';
            consequencia = {
                id: `CONSEQ-${evento.id}`,
                tipo: 'NPC_INDISPONIVEL_LOCAL',
                origemEvento: evento.id,
                horario: evento.horarioOcorrido,
                estado: 'ATIVA',
                descricao: `${npc} não está mais disponível em ${localAnterior}.`,
                efeito: {
                    npc,
                    localIndisponivel: localAnterior,
                    localAtual: localNovo
                },
                alternativas: [
                    `Ir a ${localNovo} para consultar ${npc} diretamente`,
                    `Solicitar via rádio através de Noah`,
                    `Perguntar a outro membro da equipe presente no local`,
                    `Verificar anotações ou registros deixados por ${npc} na cena`
                ],
                revelarAoJogador: false
            };
            break;
        }

        case 'EVENTO_CLIMA':
            consequencia = {
                id: `CONSEQ-${evento.id}`,
                tipo: 'CONDICOES_EXTERNAS_ALTERADAS',
                origemEvento: evento.id,
                horario: evento.horarioOcorrido,
                estado: 'ATIVA',
                descricao: `Condições climáticas alteradas: ${evento.dados ? evento.dados.climaNovo : 'clima mudou'}.`,
                efeito: {
                    climaAtual: campAtiva.estadoMundo.clima
                },
                alternativas: [
                    'Condições de campo podem ter sido afetadas',
                    'Verificar se câmeras ou equipamentos externos foram impactados'
                ],
                revelarAoJogador: false
            };
            break;

        case 'EVENTO_LOCAL_ALTERADO':
            consequencia = {
                id: `CONSEQ-${evento.id}`,
                tipo: 'LOCAL_STATUS_ALTERADO',
                origemEvento: evento.id,
                horario: evento.horarioOcorrido,
                estado: 'ATIVA',
                descricao: `Status do local alterado: ${evento.local || 'local'} — ${evento.dados ? evento.dados.mudanca : 'mudança ocorreu'}.`,
                efeito: {
                    local: evento.local,
                    novoStatus: evento.dados ? evento.dados.mudanca : null
                },
                alternativas: [
                    'Verificar se há novos registros ou documentos disponíveis',
                    'Consultar a equipe sobre o que foi encontrado'
                ],
                revelarAoJogador: false
            };
            break;

        case 'EVENTO_INVESTIGACAO':
            consequencia = {
                id: `CONSEQ-${evento.id}`,
                tipo: 'DESCOBERTA_DISPONIVEL',
                origemEvento: evento.id,
                horario: evento.horarioOcorrido,
                estado: 'ATIVA',
                descricao: `Nova descoberta disponível: ${evento.descricao}`,
                efeito: {
                    npc: evento.npcAlvo,
                    local: evento.local,
                    dados: evento.dados
                },
                alternativas: [
                    `Contatar ${evento.npcAlvo || 'o responsável'} para mais detalhes`,
                    'Solicitar relatório formal via terminal da DCE'
                ],
                revelarAoJogador: false
            };
            break;

        case 'EVENTO_NOTICIA':
            consequencia = {
                id: `CONSEQ-${evento.id}`,
                tipo: 'INFORMACAO_PUBLICA_DISPONIVEL',
                origemEvento: evento.id,
                horario: evento.horarioOcorrido,
                estado: 'ATIVA',
                descricao: `Informação de domínio público ativa: ${evento.dados ? evento.dados.manchete : evento.descricao}`,
                efeito: { manchete: evento.dados ? evento.dados.manchete : null },
                alternativas: [
                    'Pressão pública pode afetar o tempo disponível para investigação',
                    'Jornalistas podem estar buscando informações sobre o caso'
                ],
                revelarAoJogador: false
            };
            break;

        default:
            // Evento sem consequência mapeada — apenas registrado no eventLog
            break;
    }

    if (consequencia) {
        mv.consequencias.push(consequencia);
        campAtiva.eventLog.push({
            tipo: 'CONSEQUENCIA',
            descricao: `[CONSEQ] ${consequencia.tipo} — ${consequencia.descricao}`,
            horario: consequencia.horario,
            interno: true
        });
    }
}

/**
 * Verifica todos os eventos agendados da campanha e processa aqueles
 * cujo horário previsto já foi alcançado.
 * Retorna a lista de eventos processados nesta verificação.
 *
 * Deve ser chamada após cada avanço de tempo.
 */
function verificarEventosMundo(campAtiva) {
    if (!campAtiva.mundoVivo) return [];

    const minutosAtual = horaParaMinutos(campAtiva.estadoMundo.horarioAtual);
    const processados = [];

    campAtiva.mundoVivo.eventosAgendados.forEach(evento => {
        if (evento.status !== 'AGENDADO') return;
        if (horaParaMinutos(evento.horarioPrevisto) <= minutosAtual) {
            processarEvento(campAtiva, evento);
            processados.push(evento);
        }
    });

    if (processados.length > 0) {
        campAtiva.eventLog.push({
            tipo: 'SISTEMA',
            descricao: `[INFO] ${processados.length} evento(s) do mundo processado(s) em ${campAtiva.estadoMundo.horarioAtual}`,
            horario: campAtiva.estadoMundo.horarioAtual,
            interno: true
        });
    }

    return processados;
}

/**
 * Gera uma string de contexto com o estado atual do mundo para uso
 * interno do Narrative Engine. NÃO é exibida diretamente ao jogador.
 * Inclui: clima, localização dos NPCs e consequências ativas relevantes.
 */
function gerarContextoMundo(campAtiva) {
    if (!campAtiva.mundoVivo) return '';
    const mv = campAtiva.mundoVivo;

    // Estado dos NPCs
    const npcStatus = Object.entries(mv.estadoNPCs)
        .map(([nome, est]) => `${nome}→${est.local}`)
        .join(', ');

    // Consequências ativas (máximo 3 mais recentes para não sobrecarregar o contexto)
    const consequenciasAtivas = (mv.consequencias || [])
        .filter(c => c.estado === 'ATIVA')
        .slice(-3)
        .map(c => `[${c.tipo}] ${c.descricao} | Alternativas: ${c.alternativas.slice(0, 2).join(' / ')}`)
        .join(' || ');

    const partes = [
        `Clima:${campAtiva.estadoMundo.clima}`,
        `NPCs:[${npcStatus}]`
    ];
    if (consequenciasAtivas) partes.push(`Consequências:[${consequenciasAtivas}]`);

    return partes.join(' | ');
}

// ─────────────────────────────────────────────────────────────────────────────

// Elementos DOM
const telaLogin = document.getElementById('tela-login');
const telaSistema = document.getElementById('tela-sistema');
const formLogin = document.getElementById('form-login');
const btnLogout = document.getElementById('btn-logout');
const navItens = document.querySelectorAll('.nav-item');
const sidebarAlternar = document.getElementById('sidebar-alternar');
const corpoSistema = document.querySelector('.corpo-sistema');
const conteudoPrincipal = document.getElementById('conteudo-principal');
const relogioTopo = document.getElementById('relogio-topo');
const modalOverlay = document.getElementById('modal-overlay');
const modalTitulo = document.getElementById('modal-titulo');
const modalCorpo = document.getElementById('modal-corpo');
const modalFechar = document.getElementById('modal-fechar');
const modalBotaoFechar = document.getElementById('modal-botao-fechar');
const btnEditarPerfil = document.getElementById('btn-editar-perfil');
let urlPrevisualizacaoPerfil = null;

// Elementos DOM do Criador de Personagem
const modalCriador = document.getElementById('modal-criador');
const criadorFechar = document.getElementById('criador-fechar');
const criadorStepper = document.getElementById('criador-stepper');
const criadorConteudo = document.getElementById('criador-conteudo');

/**
 * Inicialização da Aplicação
 */
function inicializarApp() {
    inicializarEstadoSidebar();
    configurarEventosAutenticacao();
    configurarNavegacaoSidebar();
    configurarModal();
    configurarCriadorPersonagem();
    atualizarRelogioUI();
    atualizarProtagonistaUI(gerenciadorCampanhas.campanhaAtiva().protagonista);
}

/**
 * Eventos de Login e Logout
 */
function configurarEventosAutenticacao() {
    formLogin.addEventListener('submit', async (e) => {
        e.preventDefault();
        try {
            await gerenciadorCampanhas.carregarDoServidor();
        } catch (erro) {
            alert(`Não foi possível carregar as campanhas persistidas. ${erro.message}`);
            return;
        }
        estadoLocal.autenticado = true;
        telaLogin.style.display = 'none';
        telaSistema.style.display = 'flex';
        renderizarAba(estadoLocal.abaAtiva);
    });

    btnLogout.addEventListener('click', () => {
        estadoLocal.autenticado = false;
        telaSistema.style.display = 'none';
        telaLogin.style.display = 'flex';
    });
}

/**
 * Navegação pelas abas da Sidebar
 */
function configurarNavegacaoSidebar() {
    navItens.forEach(item => {
        const rotulo = item.querySelector(':scope > span:not(.nav-item-badge)')?.textContent.trim();
        if (rotulo) {
            item.setAttribute('role', 'button');
            item.setAttribute('aria-label', rotulo);
            item.setAttribute('title', rotulo);
            item.tabIndex = 0;
        }
        if (item.classList.contains('ativo')) item.setAttribute('aria-current', 'page');
        item.addEventListener('click', () => {
            const abaAlvo = item.getAttribute('data-aba');
            if (!abaAlvo) return;

            navItens.forEach(nav => {
                nav.classList.remove('ativo');
                nav.removeAttribute('aria-current');
            });
            item.classList.add('ativo');
            item.setAttribute('aria-current', 'page');

            estadoLocal.abaAtiva = abaAlvo;
            if (window.matchMedia('(max-width: 768px)').matches) {
                corpoSistema.classList.add('navegacao-mobile-fechada');
                atualizarBotaoSidebar();
            }
            renderizarAba(abaAlvo);
        });
        item.addEventListener('keydown', evento => {
            if (evento.key === 'Enter' || evento.key === ' ') {
                evento.preventDefault();
                item.click();
            }
        });
    });
}

const CHAVE_PREFERENCIA_SIDEBAR = 'crimson-veil.sidebar-recolhida';

function inicializarEstadoSidebar() {
    let recolhida = false;
    try {
        recolhida = localStorage.getItem(CHAVE_PREFERENCIA_SIDEBAR) === 'true';
    } catch (erro) {
        console.warn('Não foi possível restaurar a preferência visual da navegação.', erro);
    }
    corpoSistema.classList.toggle('sidebar-recolhida', recolhida);
    const consultaMobile = window.matchMedia('(max-width: 768px)');
    if (consultaMobile.matches) {
        corpoSistema.classList.add('navegacao-mobile-fechada');
    }
    atualizarBotaoSidebar();
    consultaMobile.addEventListener('change', evento => {
        corpoSistema.classList.toggle('navegacao-mobile-fechada', evento.matches);
        atualizarBotaoSidebar();
    });

    sidebarAlternar.addEventListener('click', () => {
        if (window.matchMedia('(max-width: 768px)').matches) {
            corpoSistema.classList.toggle('navegacao-mobile-fechada');
        } else {
            const recolhidaAgora = corpoSistema.classList.toggle('sidebar-recolhida');
            try {
                localStorage.setItem(CHAVE_PREFERENCIA_SIDEBAR, String(recolhidaAgora));
            } catch (erro) {
                console.warn('Não foi possível salvar a preferência visual da navegação.', erro);
            }
        }
        atualizarBotaoSidebar();
    });
}

function atualizarBotaoSidebar() {
    const emTelaMenor = window.matchMedia('(max-width: 768px)').matches;
    const navegaFechada = emTelaMenor
        ? corpoSistema.classList.contains('navegacao-mobile-fechada')
        : corpoSistema.classList.contains('sidebar-recolhida');
    const acao = emTelaMenor
        ? (navegaFechada ? 'Abrir navegação' : 'Fechar navegação')
        : (navegaFechada ? 'Expandir navegação' : 'Recolher navegação');
    sidebarAlternar.setAttribute('aria-label', acao);
    sidebarAlternar.setAttribute('title', acao);
    sidebarAlternar.setAttribute('aria-expanded', String(!navegaFechada));
    sidebarAlternar.querySelector('.sidebar-alternar-texto').textContent = acao;
    sidebarAlternar.classList.toggle('recolhida', navegaFechada);
}

/**
 * Atualização do Relógio do Topo
 */
function atualizarRelogioUI() {
    if (relogioTopo) {
        const camp = gerenciadorCampanhas.campanhaAtiva();
        const horario = (camp && camp.estadoMundo && camp.estadoMundo.horarioAtual) || estadoLocal.horarioAtual;
        relogioTopo.textContent = `${dadosCampanha.cidade} — ${horario}`;
    }
    atualizarCabecalhoCasoUI();
}

function atualizarCabecalhoCasoUI() {
    const campanha = gerenciadorCampanhas.campanhaAtiva();
    if (!campanha) return;

    const caso = campanha.casoAtivo;
    const codigo = document.getElementById('header-caso-codigo');
    const titulo = document.getElementById('header-caso-titulo');
    const contexto = document.getElementById('header-caso-contexto');
    const status = document.getElementById('header-caso-status');

    if (codigo) codigo.textContent = caso?.codigo || 'PRÓLOGO';
    if (titulo) titulo.textContent = caso?.titulo || campanha.titulo || 'Campanha em andamento';
    if (contexto) contexto.textContent = caso?.subtitulo || campanha.progresso || campanha.episodio || 'História em curso';
    if (status) status.textContent = caso
        ? (campanha.status || caso.status || 'EM INVESTIGAÇÃO')
        : 'SEM CASO ATRIBUÍDO';
}

/**
 * Roteamento do conteúdo central
 */
function renderizarAba(nomeAba) {
    conteudoPrincipal.innerHTML = '';
    atualizarCabecalhoCasoUI();

    switch (nomeAba) {
        case 'dashboard':
            renderizarDashboard();
            break;
        case 'investigacao':
            renderizarInvestigacao();
            break;
        case 'celular': {
            const campanha = gerenciadorCampanhas.campanhaAtiva();
            renderizarCelular(
                conteudoPrincipal,
                campanha.id,
                () => gerenciadorCampanhas.versaoDaCampanha(campanha.id),
                gerenciadorCampanhas.definirVersao.bind(gerenciadorCampanhas)
            );
            break;
        }
        case 'casos':
            renderizarCasos();
            break;
        case 'pessoas':
            renderizarPessoas();
            break;
        case 'locais':
            renderizarLocais();
            break;
        case 'pistas':
            renderizarPistas();
            break;
        case 'evidencias':
            renderizarEvidencias();
            break;
        case 'timeline':
            renderizarTimeline();
            break;
        case 'conexoes':
            renderizarConexoes();
            break;
        case 'artes':
            renderizarArtes();
            break;
        default:
            renderizarInvestigacao();
    }
}

/* ==========================================================================
   CRIADOR DE PERSONAGEM — PROTÓTIPO VISUAL E FLUXO COMPLETO
   ========================================================================== */

const ORIGEM_MOCK_IA = "Cheguei a Blackwood impulsionada por um rastro de investigações que o departamento convencional preferiu engavetar. Sem vínculos formais com a cúpula da cidade, aprendi a me mover pelas sombras: cruzando anomalias temporais, depoimentos abafados e o símbolo misterioso que se repete há décadas. A morte no Apartamento 504 não é um homicídio comum — é o ponto de ruptura de uma engrenagem que agora estou decidida a desmontar.";

const estadoCriador = {
    etapa: 'escolha', // 'escolha', 1, 2, 3, 4, 'transicao'
    dados: {
        nome: "Julia Almeida",
        idade: "28",
        profissao: "Investigadora Independente",
        descricaoFisica: "Olhar analítico e atento, sobretudo cinza-chumbo, caderno de anotações sempre em mãos.",
        detalheImportante: "Possui anotações pessoais sobre desaparecimentos em Blackwood que a polícia arquivou.",
        relacaoDCE: "NÃO TRABALHO NA DCE",
        experienciaInvestigativa: "EXPERIENTE",
        metodoOrigem: "ia",
        origemTexto: ORIGEM_MOCK_IA,
        origemGerada: false,
        origemEmProgresso: false,
        retratoGerado: false,
        retratoEmProgresso: false,
        retratoSrc: "assets/retrato-mock.svg"
    }
};

function configurarCriadorPersonagem() {
    if (criadorFechar) {
        criadorFechar.addEventListener('click', fecharModalCriador);
    }
    if (modalCriador) {
        modalCriador.addEventListener('click', (e) => {
            if (e.target === modalCriador) {
                fecharModalCriador();
            }
        });
    }
    window.abrirModalNovaCampanha = abrirCriadorPersonagem;
}

function abrirCriadorPersonagem() {
    irParaEtapaCriador('escolha');
    if (modalCriador) {
        modalCriador.classList.add('ativo');
    }
}

function fecharModalCriador() {
    if (modalCriador) {
        modalCriador.classList.remove('ativo');
    }
}

function atualizarStepperUI(etapaNum) {
    if (!criadorStepper) return;
    if (etapaNum === 'escolha' || etapaNum === 'transicao') {
        criadorStepper.style.display = 'none';
        return;
    }
    criadorStepper.style.display = 'flex';
    const passos = criadorStepper.querySelectorAll('.stepper-passo');
    passos.forEach(p => {
        const num = parseInt(p.getAttribute('data-passo'), 10);
        p.classList.remove('ativo', 'concluido');
        if (num === etapaNum) {
            p.classList.add('ativo');
        } else if (num < etapaNum) {
            p.classList.add('concluido');
        }
    });
}

function irParaEtapaCriador(etapa) {
    if (estadoCriador.etapa === 1) {
        salvarCamposEtapa01();
    }
    estadoCriador.etapa = etapa;
    atualizarStepperUI(etapa);

    if (etapa === 'escolha') {
        renderizarEtapaEscolha();
    } else if (etapa === 1) {
        renderizarEtapa01Identidade();
    } else if (etapa === 2) {
        renderizarEtapa02Origem();
    } else if (etapa === 3) {
        renderizarEtapa03Retrato();
    } else if (etapa === 4) {
        renderizarEtapa04Confirmacao();
    } else if (etapa === 'transicao') {
        renderizarEtapaTransicao();
    }
}

function renderizarEtapaEscolha() {
    if (!criadorConteudo) return;
    criadorConteudo.innerHTML = `
        <div class="criador-escolha-container">
            <span class="criador-secao-etiqueta">NOVA INVESTIGAÇÃO</span>
            <h2 class="criador-secao-titulo" style="font-size: 26px;">NOVA CAMPANHA</h2>
            <p class="criador-secao-sub" style="max-width: 520px; margin-bottom: 8px;">
                Uma nova investigação começa com uma nova história. Quem vai investigar Blackwood?
            </p>

            <div class="criador-escolha-grid">
                <!-- OPÇÃO 1: CRIAR PERSONAGEM (DESTAQUE VISUAL TOTAL) -->
                <div class="card-escolha card-escolha-destaque">
                    <div>
                        <span class="badge-foco-escolha">RECOMENDADO // PROTAGONISTA</span>
                        <h3 class="card-escolha-titulo">CRIAR PERSONAGEM</h3>
                        <p class="card-escolha-desc">
                            Crie alguém para viver sua própria história em Crimson Veil. Defina sua identidade, origem, retrato e entre na cena de crime com perspectiva inédita.
                        </p>
                    </div>
                    <button class="btn-primario" id="btn-escolha-criar" style="width: 100%;">
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                        <span>COMEÇAR</span>
                    </button>
                </div>

                <!-- OPÇÃO 2: PERSONAGEM EXISTENTE -->
                <div class="card-escolha card-escolha-secundario">
                    <div>
                        <span class="badge-secundario-escolha">ELENCO CANÔNICO DCE</span>
                        <h3 class="card-escolha-titulo">PERSONAGEM EXISTENTE</h3>
                        <p class="card-escolha-desc">
                            Continue usando um personagem já disponível no universo oficial: <strong>Det. Milena Ramires (#4082-DCE)</strong>.
                        </p>
                    </div>
                    <button class="btn-continuar-campanha" id="btn-escolha-existente" style="width: 100%; padding: 12px; font-weight: 600;">
                        ESCOLHER
                    </button>
                </div>
            </div>
        </div>
    `;

    const btnCriar = document.getElementById('btn-escolha-criar');
    if (btnCriar) {
        btnCriar.addEventListener('click', () => {
            irParaEtapaCriador(1);
        });
    }

    const btnExistente = document.getElementById('btn-escolha-existente');
    if (btnExistente) {
        btnExistente.addEventListener('click', () => {
            btnExistente.disabled = true;
            gerenciadorCampanhas.criarNovaCampanha(null)
                .then(() => {
                    fecharModalCriador();
                    window.navegarAba('investigacao');
                })
                .catch(erro => alert(`Não foi possível criar a campanha. ${erro.message}`))
                .finally(() => { btnExistente.disabled = false; });
        });
    }
}

function salvarCamposEtapa01() {
    const elNome = document.getElementById('criador-nome');
    const elIdade = document.getElementById('criador-idade');
    const elProfissao = document.getElementById('criador-profissao');
    const elDesc = document.getElementById('criador-desc');
    const elDetalhe = document.getElementById('criador-detalhe');

    if (elNome) estadoCriador.dados.nome = elNome.value.trim() || estadoCriador.dados.nome;
    if (elIdade) estadoCriador.dados.idade = elIdade.value.trim() || estadoCriador.dados.idade;
    if (elProfissao) estadoCriador.dados.profissao = elProfissao.value.trim() || estadoCriador.dados.profissao;
    if (elDesc) estadoCriador.dados.descricaoFisica = elDesc.value.trim() || estadoCriador.dados.descricaoFisica;
    if (elDetalhe) estadoCriador.dados.detalheImportante = elDetalhe.value.trim() || estadoCriador.dados.detalheImportante;
}

function renderizarEtapa01Identidade() {
    if (!criadorConteudo) return;
    const d = estadoCriador.dados;
    criadorConteudo.innerHTML = `
        <div class="criador-secao-header">
            <span class="criador-secao-etiqueta">ETAPA 01 // IDENTIDADE</span>
            <h2 class="criador-secao-titulo">QUEM É VOCÊ EM BLACKWOOD?</h2>
            <p class="criador-secao-sub">“Antes de começar a investigação, defina quem está entrando nela.”</p>
        </div>

        <div class="form-identidade-grid">
            <div class="campo-bloco">
                <label class="campo-bloco-rotulo" for="criador-nome">NOME COMPLETO</label>
                <input type="text" id="criador-nome" class="campo-bloco-input" value="${d.nome}" placeholder="Ex: Julia Almeida">
            </div>

            <div class="campo-bloco">
                <label class="campo-bloco-rotulo" for="criador-idade">IDADE</label>
                <input type="text" id="criador-idade" class="campo-bloco-input" value="${d.idade}" placeholder="Ex: 28">
            </div>

            <div class="campo-bloco">
                <label class="campo-bloco-rotulo" for="criador-profissao">PROFISSÃO</label>
                <input type="text" id="criador-profissao" class="campo-bloco-input" value="${d.profissao}" placeholder="Ex: Investigadora">
            </div>
        </div>

        <div class="campo-bloco">
            <label class="campo-bloco-rotulo" for="criador-desc">DESCRIÇÃO FÍSICA</label>
            <input type="text" id="criador-desc" class="campo-bloco-input" value="${d.descricaoFisica}" placeholder="Traços, vestimentas e postura...">
        </div>

        <div class="campo-bloco">
            <label class="campo-bloco-rotulo" for="criador-detalhe">ALGO IMPORTANTE SOBRE VOCÊ</label>
            <input type="text" id="criador-detalhe" class="campo-bloco-input" value="${d.detalheImportante}" placeholder="Um segredo, uma perda ou uma motivação íntima...">
        </div>

        <div class="secao-seletores-dossie">
            <div>
                <span class="campo-bloco-rotulo">RELAÇÃO COM A DCE</span>
                <div class="grupo-botoes-selecionaveis" id="grupo-relacao-dce">
                    <button type="button" class="btn-seletor-opcao ${d.relacaoDCE === 'TRABALHO NA DCE' ? 'ativo' : ''}" data-rel="TRABALHO NA DCE">TRABALHO NA DCE</button>
                    <button type="button" class="btn-seletor-opcao ${d.relacaoDCE === 'NÃO TRABALHO NA DCE' ? 'ativo' : ''}" data-rel="NÃO TRABALHO NA DCE">NÃO TRABALHO NA DCE</button>
                    <button type="button" class="btn-seletor-opcao ${d.relacaoDCE === 'AINDA NÃO DEFINIDO' ? 'ativo' : ''}" data-rel="AINDA NÃO DEFINIDO">AINDA NÃO DEFINIDO</button>
                </div>
            </div>

            <div>
                <span class="campo-bloco-rotulo">EXPERIÊNCIA INVESTIGATIVA</span>
                <div class="grupo-botoes-selecionaveis" id="grupo-exp-investigativa">
                    <button type="button" class="btn-seletor-opcao ${d.experienciaInvestigativa === 'NENHUMA' ? 'ativo' : ''}" data-exp="NENHUMA">NENHUMA</button>
                    <button type="button" class="btn-seletor-opcao ${d.experienciaInvestigativa === 'POUCA' ? 'ativo' : ''}" data-exp="POUCA">POUCA</button>
                    <button type="button" class="btn-seletor-opcao ${d.experienciaInvestigativa === 'EXPERIENTE' ? 'ativo' : ''}" data-exp="EXPERIENTE">EXPERIENTE</button>
                    <button type="button" class="btn-seletor-opcao ${d.experienciaInvestigativa === 'ESPECIALISTA' ? 'ativo' : ''}" data-exp="ESPECIALISTA">ESPECIALISTA</button>
                </div>
            </div>
        </div>

        <div class="criador-rodape-acoes">
            <button class="btn-criador-voltar" id="btn-etapa1-voltar">
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
                VOLTAR
            </button>
            <button class="btn-criador-avancar" id="btn-etapa1-continuar">
                <span>CONTINUAR</span>
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
            </button>
        </div>
    `;

    const btsRelacao = criadorConteudo.querySelectorAll('#grupo-relacao-dce .btn-seletor-opcao');
    btsRelacao.forEach(btn => {
        btn.addEventListener('click', () => {
            btsRelacao.forEach(b => b.classList.remove('ativo'));
            btn.classList.add('ativo');
            estadoCriador.dados.relacaoDCE = btn.getAttribute('data-rel');
        });
    });

    const btsExp = criadorConteudo.querySelectorAll('#grupo-exp-investigativa .btn-seletor-opcao');
    btsExp.forEach(btn => {
        btn.addEventListener('click', () => {
            btsExp.forEach(b => b.classList.remove('ativo'));
            btn.classList.add('ativo');
            estadoCriador.dados.experienciaInvestigativa = btn.getAttribute('data-exp');
        });
    });

    const btnVoltar = document.getElementById('btn-etapa1-voltar');
    if (btnVoltar) {
        btnVoltar.addEventListener('click', () => {
            salvarCamposEtapa01();
            irParaEtapaCriador('escolha');
        });
    }

    const btnContinuar = document.getElementById('btn-etapa1-continuar');
    if (btnContinuar) {
        btnContinuar.addEventListener('click', () => {
            salvarCamposEtapa01();
            irParaEtapaCriador(2);
        });
    }
}

function renderizarEtapa02Origem() {
    if (!criadorConteudo) return;
    const d = estadoCriador.dados;
    const metodoAtivo = d.metodoOrigem;

    criadorConteudo.innerHTML = `
        <div class="criador-secao-header">
            <span class="criador-secao-etiqueta">ETAPA 02 // ORIGEM</span>
            <h2 class="criador-secao-titulo">COMO SUA HISTÓRIA COMEÇA?</h2>
            <p class="criador-secao-sub">“Blackwood já estava aqui antes de você. Sua história começa agora.”</p>
        </div>

        <div class="grid-opcoes-origem">
            <!-- OPÇÃO 1: A IA CRIA -->
            <div class="card-origem-item ${metodoAtivo === 'ia' ? 'selecionado' : ''}" id="card-origem-ia">
                <div>
                    <span class="card-origem-tag">OPÇÃO 01</span>
                    <h3 class="card-origem-titulo">A IA CRIA</h3>
                    <p class="card-origem-desc">“Você fornece algumas informações. A história da sua personagem nasce delas.”</p>
                </div>
                <button class="btn-origem-acao" id="btn-origem-gerar-ia">GERAR ORIGEM</button>
            </div>

            <!-- OPÇÃO 2: EU ESCREVO -->
            <div class="card-origem-item ${metodoAtivo === 'manual' ? 'selecionado' : ''}" id="card-origem-manual">
                <div>
                    <span class="card-origem-tag">OPÇÃO 02</span>
                    <h3 class="card-origem-titulo">EU ESCREVO</h3>
                    <p class="card-origem-desc">“Você decide exatamente de onde veio e o que trouxe você até Blackwood.”</p>
                </div>
                <button class="btn-origem-acao" id="btn-origem-manual">ESCREVER HISTÓRIA</button>
            </div>

            <!-- OPÇÃO 3: CRIAR JUNTO -->
            <div class="card-origem-item ${metodoAtivo === 'co-criacao' ? 'selecionado' : ''}" id="card-origem-cocriacao">
                <div>
                    <span class="card-origem-tag">OPÇÃO 03</span>
                    <h3 class="card-origem-titulo">CRIAR JUNTO</h3>
                    <p class="card-origem-desc">“A IA faz perguntas e você constrói a história aos poucos.”</p>
                </div>
                <button class="btn-origem-acao" id="btn-origem-cocriacao">COMEÇAR</button>
            </div>
        </div>

        <div class="painel-origem-detalhe" id="painel-origem-detalhe">
            ${gerarConteudoPainelOrigem()}
        </div>

        <div class="criador-rodape-acoes">
            <button class="btn-criador-voltar" id="btn-etapa2-voltar">
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
                VOLTAR
            </button>
            <button class="btn-criador-avancar" id="btn-etapa2-continuar">
                <span>CONTINUAR</span>
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
            </button>
        </div>
    `;

    configurarEventosEtapa02();
}

function gerarConteudoPainelOrigem() {
    const d = estadoCriador.dados;

    if (d.origemEmProgresso) {
        return `
            <div class="box-loading-ia">
                <div class="loading-scanner-circulo"></div>
                <div class="loading-texto-passos" id="ia-loading-passo">ANALISANDO PERFIL...</div>
            </div>
        `;
    }

    if (d.metodoOrigem === 'manual') {
        return `
            <div>
                <label class="campo-bloco-rotulo" style="margin-bottom: 8px;">CONTE SUA HISTÓRIA</label>
                <textarea id="textarea-origem-manual" class="campo-bloco-textarea" style="height: 120px;" placeholder="Escreva a origem da sua personagem em Blackwood...">${d.origemTexto || ''}</textarea>
                <div style="display: flex; justify-content: flex-end; margin-top: 10px;">
                    <button class="btn-primario" id="btn-salvar-origem-manual" style="padding: 8px 18px; font-size: 11px; width: auto;">
                        SALVAR HISTÓRIA
                    </button>
                </div>
            </div>
        `;
    }

    if (d.metodoOrigem === 'co-criacao') {
        return `
            <div>
                <div class="box-origem-mock-tag">
                    <svg width="13" height="13" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
                    CO-CRIAÇÃO NARRATIVA // RESPOSTAS GUIADAS
                </div>
                <p style="font-size: 12px; color: var(--texto-secundario); margin-bottom: 12px;">
                    A IA formulou uma questão-chave para seu perfil em Blackwood:
                </p>
                <div style="background: #090a10; border: 1px solid var(--cinza-borda); border-radius: 6px; padding: 12px; margin-bottom: 12px;">
                    <div style="font-family: var(--fonte-mono); font-size: 11px; color: var(--azul-forense); margin-bottom: 4px;">PERGUNTA 01:</div>
                    <div style="font-size: 13px; color: #fff;">O que fez você entrar no Residencial Solaris na mesma madrugada da morte de Arthur?</div>
                </div>
                <textarea id="textarea-cocriacao" class="campo-bloco-textarea" style="height: 70px;" placeholder="Sua resposta rápida...">Eu estava monitorando anotações cruzadas sobre o símbolo e a trilha me levou até aquele apartamento.</textarea>
                <div style="display: flex; justify-content: flex-end; margin-top: 10px;">
                    <button class="btn-primario" id="btn-concluir-cocriacao" style="padding: 8px 18px; font-size: 11px; width: auto;">
                        CONSOLIDAR ORIGEM
                    </button>
                </div>
            </div>
        `;
    }

    return `
        <div class="box-origem-resultado">
            <div class="box-origem-mock-tag">
                <svg width="13" height="13" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                PROTÓTIPO // GERAÇÃO SIMULADA DE ORIGEM
            </div>
            <div class="box-origem-texto">
                “${d.origemTexto || ORIGEM_MOCK_IA}”
            </div>
        </div>
    `;
}

function configurarEventosEtapa02() {
    const btnGerarIA = document.getElementById('btn-origem-gerar-ia');
    const btnManual = document.getElementById('btn-origem-manual');
    const btnCocriacao = document.getElementById('btn-origem-cocriacao');

    const cardIA = document.getElementById('card-origem-ia');
    const cardManual = document.getElementById('card-origem-manual');
    const cardCocriacao = document.getElementById('card-origem-cocriacao');

    if (btnGerarIA) {
        btnGerarIA.addEventListener('click', (e) => {
            e.stopPropagation();
            simularGeracaoOrigemIA();
        });
    }

    if (cardIA) {
        cardIA.addEventListener('click', () => {
            if (estadoCriador.dados.metodoOrigem !== 'ia') {
                estadoCriador.dados.metodoOrigem = 'ia';
                renderizarEtapa02Origem();
            }
        });
    }

    if (btnManual || cardManual) {
        const handler = () => {
            estadoCriador.dados.metodoOrigem = 'manual';
            renderizarEtapa02Origem();
        };
        if (btnManual) btnManual.addEventListener('click', (e) => { e.stopPropagation(); handler(); });
        if (cardManual) cardManual.addEventListener('click', handler);
    }

    if (btnCocriacao || cardCocriacao) {
        const handler = () => {
            estadoCriador.dados.metodoOrigem = 'co-criacao';
            renderizarEtapa02Origem();
        };
        if (btnCocriacao) btnCocriacao.addEventListener('click', (e) => { e.stopPropagation(); handler(); });
        if (cardCocriacao) cardCocriacao.addEventListener('click', handler);
    }

    const btnSalvarManual = document.getElementById('btn-salvar-origem-manual');
    if (btnSalvarManual) {
        btnSalvarManual.addEventListener('click', () => {
            const ta = document.getElementById('textarea-origem-manual');
            if (ta && ta.value.trim()) {
                estadoCriador.dados.origemTexto = ta.value.trim();
                estadoCriador.dados.origemGerada = true;
                alert('História de origem salva com sucesso.');
            }
        });
    }

    const btnConcluirCocriacao = document.getElementById('btn-concluir-cocriacao');
    if (btnConcluirCocriacao) {
        btnConcluirCocriacao.addEventListener('click', () => {
            const ta = document.getElementById('textarea-cocriacao');
            const resp = ta ? ta.value.trim() : '';
            estadoCriador.dados.origemTexto = `Com base nas pistas que você reuniu sobre Blackwood: "${resp}". Assim começa seu inquérito na cena de crime do Apto 504.`;
            estadoCriador.dados.origemGerada = true;
            estadoCriador.dados.metodoOrigem = 'ia';
            renderizarEtapa02Origem();
        });
    }

    const btnVoltar = document.getElementById('btn-etapa2-voltar');
    if (btnVoltar) {
        btnVoltar.addEventListener('click', () => {
            irParaEtapaCriador(1);
        });
    }

    const btnContinuar = document.getElementById('btn-etapa2-continuar');
    if (btnContinuar) {
        btnContinuar.addEventListener('click', () => {
            if (!estadoCriador.dados.origemTexto) {
                estadoCriador.dados.origemTexto = ORIGEM_MOCK_IA;
            }
            irParaEtapaCriador(3);
        });
    }
}

function simularGeracaoOrigemIA() {
    estadoCriador.dados.metodoOrigem = 'ia';
    estadoCriador.dados.origemEmProgresso = true;
    renderizarEtapa02Origem();

    const elPasso = document.getElementById('ia-loading-passo');

    setTimeout(() => {
        if (elPasso) elPasso.textContent = 'CONSTRUINDO ORIGEM...';
    }, 600);

    setTimeout(() => {
        if (elPasso) elPasso.textContent = 'CONECTANDO COM BLACKWOOD...';
    }, 1200);

    setTimeout(() => {
        estadoCriador.dados.origemEmProgresso = false;
        estadoCriador.dados.origemGerada = true;
        estadoCriador.dados.origemTexto = ORIGEM_MOCK_IA;
        renderizarEtapa02Origem();
    }, 1800);
}

function renderizarEtapa03Retrato() {
    if (!criadorConteudo) return;
    const d = estadoCriador.dados;

    criadorConteudo.innerHTML = `
        <div class="criador-secao-header" style="text-align: center;">
            <span class="criador-secao-etiqueta">ETAPA 03 // RETRATO</span>
            <h2 class="criador-secao-titulo">COMO BLACKWOOD VAI LEMBRAR DE VOCÊ?</h2>
            <p class="criador-secao-sub">“Crie a identidade visual da sua personagem.”</p>
        </div>

        <div class="area-retrato-layout">
            <div class="moldura-retrato-central ${d.retratoGerado ? 'gerado' : ''}" id="moldura-retrato">
                <div class="canto-tatico canto-tl"></div>
                <div class="canto-tatico canto-tr"></div>
                <div class="canto-tatico canto-bl"></div>
                <div class="canto-tatico canto-br"></div>
                <div class="reticula-scanner"></div>

                ${gerarConteudoMolduraRetrato()}
            </div>

            <div class="acoes-retrato-botoes">
                ${d.retratoGerado ? `
                    <button class="btn-primario" id="btn-regenerar-retrato" style="padding: 10px 20px; font-size: 11px; width: auto;">
                        <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
                        REGENERAR
                    </button>
                    <button class="btn-continuar-campanha" id="btn-editar-desc-retrato" style="padding: 10px 20px; font-size: 11px;">
                        EDITAR DESCRIÇÃO
                    </button>
                ` : `
                    <button class="btn-primario" id="btn-gerar-retrato" style="padding: 12px 28px; font-size: 12px; width: auto;">
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                        GERAR RETRATO
                    </button>
                `}
            </div>
        </div>

        <div class="criador-rodape-acoes">
            <button class="btn-criador-voltar" id="btn-etapa3-voltar">
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
                VOLTAR
            </button>
            <button class="btn-criador-avancar" id="btn-etapa3-continuar">
                <span>CONTINUAR</span>
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
            </button>
        </div>
    `;

    configurarEventosEtapa03();
}

function gerarConteudoMolduraRetrato() {
    const d = estadoCriador.dados;

    if (d.retratoEmProgresso) {
        return `
            <div class="box-loading-ia" style="padding: 0;">
                <div class="loading-scanner-circulo"></div>
                <div class="loading-texto-passos" style="font-size: 13px;">GERANDO RETRATO...</div>
                <div style="font-family: var(--fonte-mono); font-size: 10px; color: var(--texto-mutado); margin-top: 6px;">A identidade visual está sendo criada.</div>
            </div>
        `;
    }

    if (d.retratoGerado) {
        return `
            <img src="${d.retratoSrc}" class="retrato-renderizado-img" alt="Retrato da Personagem">
            <div class="tarja-retrato-dossie">
                <div class="tarja-nome-personagem">${d.nome}</div>
                <div class="tarja-cargo-personagem">${d.profissao} // ${d.relacaoDCE}</div>
            </div>
        `;
    }

    return `
        <div class="placeholder-icone-silhueta">
            <svg width="64" height="64" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
        </div>
        <div class="placeholder-rotulo-texto">SEU RETRATO</div>
        <span class="placeholder-tag">[ PLACEHOLDER ]</span>
    `;
}

function configurarEventosEtapa03() {
    const btnGerar = document.getElementById('btn-gerar-retrato');
    const btnRegenerar = document.getElementById('btn-regenerar-retrato');
    const btnEditarDesc = document.getElementById('btn-editar-desc-retrato');

    const dispararGeracao = () => {
        estadoCriador.dados.retratoEmProgresso = true;
        renderizarEtapa03Retrato();

        setTimeout(() => {
            estadoCriador.dados.retratoEmProgresso = false;
            estadoCriador.dados.retratoGerado = true;
            renderizarEtapa03Retrato();
        }, 1300);
    };

    if (btnGerar) btnGerar.addEventListener('click', dispararGeracao);
    if (btnRegenerar) btnRegenerar.addEventListener('click', dispararGeracao);

    if (btnEditarDesc) {
        btnEditarDesc.addEventListener('click', () => {
            irParaEtapaCriador(1);
        });
    }

    const btnVoltar = document.getElementById('btn-etapa3-voltar');
    if (btnVoltar) {
        btnVoltar.addEventListener('click', () => {
            irParaEtapaCriador(2);
        });
    }

    const btnContinuar = document.getElementById('btn-etapa3-continuar');
    if (btnContinuar) {
        btnContinuar.addEventListener('click', () => {
            if (!estadoCriador.dados.retratoGerado) {
                estadoCriador.dados.retratoGerado = true;
            }
            irParaEtapaCriador(4);
        });
    }
}

function renderizarEtapa04Confirmacao() {
    if (!criadorConteudo) return;
    const d = estadoCriador.dados;

    criadorConteudo.innerHTML = `
        <div class="criador-secao-header">
            <span class="criador-secao-etiqueta">ETAPA 04 // CONFIRMAÇÃO</span>
            <h2 class="criador-secao-titulo">ESTA É A SUA HISTÓRIA</h2>
            <p class="criador-secao-sub">“Dossiê compilado pela Divisão de Crimes Especiais da Cidade de Blackwood.”</p>
        </div>

        <div class="dossie-confirmacao-grid">
            <!-- COLUNA DO RETRATO -->
            <div class="coluna-retrato-confirmacao">
                <div class="foto-dossie-box">
                    <img src="${d.retratoSrc}" style="width: 100%; height: 100%; object-fit: cover;" alt="Retrato Confirmado">
                    <div class="tarja-retrato-dossie">
                        <div class="tarja-nome-personagem">${d.nome}</div>
                        <div class="tarja-cargo-personagem">${d.profissao}</div>
                    </div>
                </div>

                <div class="acoes-edicao-rapida">
                    <button class="btn-edicao-chip" id="btn-edit-personagem">EDITAR PERSONAGEM</button>
                    <button class="btn-edicao-chip" id="btn-edit-origem">EDITAR ORIGEM</button>
                    <button class="btn-edicao-chip" id="btn-edit-retrato">REGENERAR RETRATO</button>
                </div>
            </div>

            <!-- COLUNA DOS DADOS DO DOSSIÊ -->
            <div class="coluna-ficha-confirmacao">
                <div>
                    <div class="ficha-campo-linha">
                        <div class="ficha-item">
                            <span class="ficha-item-rotulo">NOME COMPLETO</span>
                            <span class="ficha-item-valor">${d.nome}</span>
                        </div>
                        <div class="ficha-item">
                            <span class="ficha-item-rotulo">IDADE</span>
                            <span class="ficha-item-valor">${d.idade} anos</span>
                        </div>
                    </div>

                    <div class="ficha-campo-linha">
                        <div class="ficha-item">
                            <span class="ficha-item-rotulo">PROFISSÃO</span>
                            <span class="ficha-item-valor">${d.profissao}</span>
                        </div>
                        <div class="ficha-item">
                            <span class="ficha-item-rotulo">RELAÇÃO COM A DCE</span>
                            <span class="ficha-item-valor" style="color: var(--azul-forense);">${d.relacaoDCE}</span>
                        </div>
                    </div>

                    <div class="ficha-campo-linha" style="border-bottom: none; margin-bottom: 6px; padding-bottom: 0;">
                        <div class="ficha-item">
                            <span class="ficha-item-rotulo">EXPERIÊNCIA INVESTIGATIVA</span>
                            <span class="ficha-item-valor" style="color: var(--carmesim-brilho);">${d.experienciaInvestigativa}</span>
                        </div>
                        <div class="ficha-item">
                            <span class="ficha-item-rotulo">CLASSIFICAÇÃO</span>
                            <span class="ficha-item-valor" style="font-family: var(--fonte-mono); font-size: 11px;">PROTAGONISTA DA CAMPANHA</span>
                        </div>
                    </div>

                    <div class="ficha-origem-secao">
                        <span class="ficha-item-rotulo" style="color: var(--carmesim-brilho);">ORIGEM NARRATIVA</span>
                        <div class="ficha-origem-texto">
                            “${d.origemTexto || ORIGEM_MOCK_IA}”
                        </div>
                    </div>
                </div>

                <div style="margin-top: 20px;">
                    <button class="btn-primario" id="btn-iniciar-campanha" style="padding: 14px; font-size: 13px; letter-spacing: 1.5px;">
                        <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 3l14 9-14 9V3z"/></svg>
                        <span>INICIAR CAMPANHA</span>
                    </button>
                </div>
            </div>
        </div>

        <div class="criador-rodape-acoes">
            <button class="btn-criador-voltar" id="btn-etapa4-voltar">
                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
                VOLTAR
            </button>
            <span style="font-family: var(--fonte-mono); font-size: 11px; color: var(--texto-mutado);">
                DCE // PRONTO PARA ENTRAR NA CENA
            </span>
        </div>
    `;

    const btnEditP = document.getElementById('btn-edit-personagem');
    if (btnEditP) btnEditP.addEventListener('click', () => irParaEtapaCriador(1));

    const btnEditO = document.getElementById('btn-edit-origem');
    if (btnEditO) btnEditO.addEventListener('click', () => irParaEtapaCriador(2));

    const btnEditR = document.getElementById('btn-edit-retrato');
    if (btnEditR) btnEditR.addEventListener('click', () => irParaEtapaCriador(3));

    const btnVoltar = document.getElementById('btn-etapa4-voltar');
    if (btnVoltar) btnVoltar.addEventListener('click', () => irParaEtapaCriador(3));

    const btnIniciar = document.getElementById('btn-iniciar-campanha');
    if (btnIniciar) {
        btnIniciar.addEventListener('click', () => {
            irParaEtapaCriador('transicao');
        });
    }
}

function renderizarEtapaTransicao() {
    if (!criadorConteudo) return;
    criadorConteudo.innerHTML = `
        <div class="transicao-campanha-box">
            <div class="emblema-transicao">
                <svg width="32" height="32" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
            </div>

            <h2 class="transicao-titulo">PERSONAGEM CONFIRMADO</h2>
            <p class="transicao-sub">Sua investigação começa agora.</p>

            <div class="transicao-meta-tags">
                <span>CIDADE DE BLACKWOOD</span>
                <span>•</span>
                <span>DIVISÃO DE CRIMES ESPECIAIS</span>
                <span>•</span>
                <span>APARTAMENTO 504</span>
            </div>

            <button class="btn-primario" id="btn-entrar-investigacao" style="padding: 16px 36px; font-size: 14px; width: auto;">
                <span>ENTRAR NA INVESTIGAÇÃO</span>
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
            </button>
        </div>
    `;

    const btnEntrar = document.getElementById('btn-entrar-investigacao');
    if (btnEntrar) {
        btnEntrar.addEventListener('click', async () => {
            btnEntrar.disabled = true;
            try {
                await gerenciadorCampanhas.criarNovaCampanha(estadoCriador.dados);
                fecharModalCriador();
                window.navegarAba('investigacao');
            } catch (erro) {
                alert(`Não foi possível criar a campanha. ${erro.message}`);
            } finally {
                btnEntrar.disabled = false;
            }
        });
    }
}

/**
 * 1. TELA: DASHBOARD DA CAMPANHA (ISOLAMENTO MULTI-HISTÓRIA)
 */
function mostrarMensagemOperacaoDashboard(texto, erro = false) {
    const anterior = document.getElementById('feedback-operacao-campanha');
    if (anterior) anterior.remove();
    const feedback = document.createElement('p');
    feedback.id = 'feedback-operacao-campanha';
    feedback.className = `feedback-operacao${erro ? ' erro' : ''}`;
    feedback.setAttribute('role', erro ? 'alert' : 'status');
    feedback.textContent = texto;
    conteudoPrincipal.prepend(feedback);
    window.setTimeout(() => feedback.remove(), 8000);
}

function renderizarDashboard() {
    const campanhaAtiva = gerenciadorCampanhas.campanhaAtiva();
    const todasCampanhas = gerenciadorCampanhas.campanhas;

    // Gera o HTML dos cards de campanha existentes
    const htmlCampanhas = todasCampanhas.map(c => `
        <div class="card-campanha ${c.ativa ? 'card-campanha-ativa' : ''}">
            <div class="card-campanha-topo">
                <div>
                    <div class="card-campanha-rotulo">${c.codigo} // ${c.episodio}</div>
                    <h3 class="card-campanha-titulo">${c.casoAtivo ? c.casoAtivo.titulo : c.titulo}</h3>
                    <div class="card-campanha-progresso">${c.progresso}</div>
                </div>
                ${c.ativa ? `<span class="card-campanha-status-badge">ATIVA</span>` : `<span class="card-campanha-status-badge card-campanha-status-inativa">SALVA</span>`}
            </div>
            <div class="card-campanha-rodape">
                <span class="card-campanha-meta">Protagonista: <b>${c.protagonista.nome}</b> | ${c.ultimaAtividade}</span>
                <div class="acoes-campanha">
                    <button
                        class="btn-continuar-campanha"
                        onclick="window.continuarCampanha('${c.id}')"
                    >
                        ${c.ativa ? 'CONTINUAR →' : 'RETOMAR →'}
                    </button>
                    <button class="btn-acao-campanha"
                        onclick="window.novaCampanhaComPersonagem('${c.id}', this)"
                        aria-label="Nova campanha com ${c.protagonista.nome}">
                        NOVA COM PERSONAGEM
                    </button>
                    <button class="btn-acao-campanha btn-reiniciar"
                        onclick="window.reiniciarCampanha('${c.id}', this)"
                        aria-label="Reiniciar campanha de ${c.protagonista.nome}">
                        REINICIAR CAMPANHA
                    </button>
                </div>
            </div>
        </div>
    `).join('');

    // Gera feed de atividades próprio da campanha ativa
    const htmlFeedAtividades = (campanhaAtiva.eventLog && campanhaAtiva.eventLog.length > 0)
        ? campanhaAtiva.eventLog.slice(-4).reverse().map(ev => `
            <div class="atividade-item">
                <div class="atividade-icone"><svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg></div>
                <div class="atividade-corpo">
                    <div class="atividade-cabecalho"><span class="atividade-titulo">${ev.tipo}</span><span class="atividade-tempo">${ev.horario || '—'}</span></div>
                    <div class="atividade-texto">${ev.descricao}</div>
                </div>
            </div>
        `).join('')
        : `
            <div class="atividade-item">
                <div class="atividade-icone"><svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg></div>
                <div class="atividade-corpo">
                    <div class="atividade-cabecalho"><span class="atividade-titulo">História Inicial</span><span class="atividade-tempo">${campanhaAtiva.estadoMundo.horarioAtual}</span></div>
                    <div class="atividade-texto">Investigação iniciada por ${campanhaAtiva.protagonista.nome}. Registre ações para avançar no inquérito.</div>
                </div>
            </div>
        `;

    const html = `
        <div style="max-width: 820px;">

            <!-- Cabeçalho da seção de campanhas -->
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px;">
                <div>
                    <h2 style="font-family: var(--fonte-titulo); font-size: 18px; letter-spacing: 1px; color: #fff; margin-bottom: 4px;">MINHAS CAMPANHAS</h2>
                    <p style="font-family: var(--fonte-mono); font-size: 11px; color: var(--texto-mutado);">Cidade de Blackwood // Divisão de Crimes Especiais</p>
                </div>
                <button class="btn-nova-campanha" id="btn-nova-campanha" onclick="window.abrirModalNovaCampanha()">
                    <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                    NOVA CAMPANHA
                </button>
            </div>

            <!-- Lista de campanhas existentes -->
            <div class="lista-campanhas">
                ${htmlCampanhas}
            </div>

            <!-- Separador -->
            <div style="border-top: 1px solid var(--cinza-borda); margin: 28px 0;"></div>

            <!-- Indicadores da campanha ativa -->
            <div style="margin-bottom: 16px;">
                <div style="font-family: var(--fonte-mono); font-size: 10px; color: var(--texto-mutado); text-transform: uppercase; letter-spacing: 1px; margin-bottom: 12px;">
                    CAMPANHA ATIVA — ${campanhaAtiva.casoAtivo ? campanhaAtiva.casoAtivo.titulo : campanhaAtiva.titulo} (${campanhaAtiva.protagonista.nome})
                </div>
                <div style="display: flex; gap: 10px; margin-bottom: 28px;">
                    <div class="indicador-compacto">
                        <div class="indicador-rotulo">Pistas</div>
                        <div class="indicador-valor">${campanhaAtiva.pistas.length}</div>
                        <div class="indicador-sub">observadas</div>
                    </div>
                    <div class="indicador-compacto">
                        <div class="indicador-rotulo">Evidências</div>
                        <div class="indicador-valor">${campanhaAtiva.evidencias.length}</div>
                        <div class="indicador-sub">sob custódia</div>
                    </div>
                    <div class="indicador-compacto">
                        <div class="indicador-rotulo">Ações</div>
                        <div class="indicador-valor">${campanhaAtiva.contadorAcoes}</div>
                        <div class="indicador-sub">na cena</div>
                    </div>
                    <div class="indicador-compacto indicador-destaque" onclick="window.navegarAba && window.navegarAba('investigacao')" style="cursor: pointer;">
                        <div class="indicador-rotulo" style="color: var(--carmesim-brilho);">Continuar</div>
                        <div class="indicador-valor" style="font-size: 14px; margin: 6px 0;">${campanhaAtiva.casoAtivo ? 'Caso Ativo' : 'Prólogo'}</div>
                        <div class="indicador-sub">${campanhaAtiva.estadoMundo.localAtual} →</div>
                    </div>
                </div>
            </div>

            <!-- Atividades recentes da campanha ativa -->
            <div class="card-geral">
                <div class="card-cabecalho">
                    <h3 class="card-titulo">
                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
                        ATIVIDADES RECENTES — ${campanhaAtiva.protagonista.nome.toUpperCase()}
                    </h3>
                    <span style="font-family: var(--fonte-mono); font-size: 10px; color: var(--texto-mutado);">${campanhaAtiva.id.toUpperCase()} // Blackwood</span>
                </div>
                <div class="feed-atividades">
                    ${htmlFeedAtividades}
                </div>
            </div>

        </div>
    `;
    conteudoPrincipal.innerHTML = html;

    // Registra handlers de navegação e campanha
    window.navegarAba = (aba) => {
        navItens.forEach(nav => {
            const ativa = nav.getAttribute('data-aba') === aba;
            nav.classList.toggle('ativo', ativa);
            if (ativa) nav.setAttribute('aria-current', 'page');
            else nav.removeAttribute('aria-current');
        });
        if (window.matchMedia('(max-width: 768px)').matches) {
            corpoSistema.classList.add('navegacao-mobile-fechada');
            atualizarBotaoSidebar();
        }
        estadoLocal.abaAtiva = aba;
        renderizarAba(aba);
    };

    window.continuarCampanha = (id) => {
        gerenciadorCampanhas.ativarCampanha(id);
        window.navegarAba('investigacao');
    };

    window.abrirModalNovaCampanha = () => {
        abrirCriadorPersonagem();
    };

    window.reiniciarCampanha = async (id, botao) => {
        const campanha = gerenciadorCampanhas.campanhas.find(item => item.id === id);
        if (!campanha) return;
        const tituloCampanha = campanha.casoAtivo?.titulo || campanha.titulo;
        const confirmar = window.confirm(
            `Reiniciar a campanha "${tituloCampanha}" de ${campanha.protagonista.nome}?\n\n`
            + 'Todo o progresso narrativo desta campanha será reiniciado: pistas, evidências, decisões, '
            + 'consequências, eventos, histórico, contatos e mensagens. A personagem e seu perfil, '
            + 'incluindo a imagem personalizada, serão preservados. As outras campanhas não serão alteradas.'
        );
        if (!confirmar) return;

        botao.disabled = true;
        const textoOriginal = botao.textContent;
        botao.textContent = 'REINICIANDO...';
        try {
            await gerenciadorCampanhas.reiniciarCampanha(id);
            renderizarAba('dashboard');
            mostrarMensagemOperacaoDashboard(`Campanha "${tituloCampanha}" reiniciada. Personagem e perfil preservados.`);
        } catch (erro) {
            botao.disabled = false;
            botao.textContent = textoOriginal;
            mostrarMensagemOperacaoDashboard(`Não foi possível reiniciar a campanha: ${erro.message}`, true);
        }
    };

    window.novaCampanhaComPersonagem = async (id, botao) => {
        const campanha = gerenciadorCampanhas.campanhas.find(item => item.id === id);
        if (!campanha) return;
        botao.disabled = true;
        const textoOriginal = botao.textContent;
        botao.textContent = 'CRIANDO...';
        try {
            await gerenciadorCampanhas.criarNovaCampanhaComPersonagem(campanha);
            renderizarAba('dashboard');
            mostrarMensagemOperacaoDashboard(
                `Nova campanha criada com ${campanha.protagonista.nome}. A campanha anterior permanece intacta.`
            );
        } catch (erro) {
            botao.disabled = false;
            botao.textContent = textoOriginal;
            mostrarMensagemOperacaoDashboard(`Não foi possível criar a campanha: ${erro.message}`, true);
        }
    };

    window.assumirCaso = async (idCaso) => {
        const original = gerenciadorCampanhas.campanhaAtiva();
        const casoRef = dadosCasos.find(c => c.id === idCaso);
        if (!casoRef) return;
        const campAtiva = JSON.parse(JSON.stringify(original));

        campAtiva.casoAtivo = { ...casoRef };
        campAtiva.codigo = casoRef.codigo;
        campAtiva.status = 'EM INVESTIGAÇÃO';
        campAtiva.progresso = `${casoRef.codigo} — Em andamento`;
        campAtiva.estadoMundo.localAtual = casoRef.localPrincipal;
        campAtiva.estadoMundo.horarioAtual = '03:30';
        campAtiva.estadoMundo.personagensPresentes = [
            `${campAtiva.protagonista.nome} (Protagonista)`,
            'Adrian Hale (Capitão da DCE)',
            'Helena Voss (Detetive da DCE)',
            'Dra. Maya Navarro (Médica Legista)'
        ];

        // Se a campanha ainda não tiver pistas desse caso, carrega as pistas iniciais
        if (campAtiva.pistas.length === 0) {
            campAtiva.pistas = JSON.parse(JSON.stringify(dadosPistas));
        }
        if (campAtiva.evidencias.length === 0) {
            campAtiva.evidencias = JSON.parse(JSON.stringify(dadosEvidencias));
        }

        campAtiva.eventLog.push({
            tipo: 'ATRIBUICAO_CASO',
            descricao: `Inquérito ${casoRef.codigo} (${casoRef.titulo}) atribuído deliberadamente a ${campAtiva.protagonista.nome}`,
            horario: '03:30'
        });

        campAtiva.mensagensCena.push({
            tipo: 'SISTEMA',
            conteudo: `[ DESPACHO DA CENTRAL DCE ]\nINQUÉRITO ATRIBUÍDO: ${casoRef.codigo} — ${casoRef.titulo.toUpperCase()}\nLOCAL: ${casoRef.localPrincipal} // DILIGÊNCIA INICIADA`,
            horario: '03:30'
        });

        const operacao = {
            chaveOperacao: `assumir-${campAtiva.id}-${gerenciadorCampanhas.versaoDaCampanha(campAtiva.id)}-${casoRef.id}`,
            versaoEsperada: gerenciadorCampanhas.versaoDaCampanha(campAtiva.id),
            acao: `Atribuição do inquérito ${casoRef.codigo}`,
            campanha: campAtiva
        };
        try {
            const resposta = await salvarEstadoCampanha(campAtiva.id, operacao);
            gerenciadorCampanhas.substituirCampanhaPersistida(resposta.campanha, resposta.versao);
        } catch (erro) {
            if (erro instanceof ApiError && erro.status === 409) {
                try {
                    await gerenciadorCampanhas.atualizarCampanhaDoServidor(campAtiva.id);
                } catch (erroAtualizacao) {
                    alert(`A campanha mudou em outra sessão e não foi possível recarregá-la. ${erroAtualizacao.message}`);
                    return;
                }
            }
            alert(`O caso não foi atribuído porque a alteração não foi confirmada pelo backend. ${erro.message}`);
            return;
        }
        window.navegarAba('investigacao');
    };
}


/**
 * 2. TELA: INVESTIGAÇÃO (TELA PRINCIPAL DE RPG — CONTEXTUAL POR CAMPANHA)
 */
function renderizarInvestigacao() {
    const campAtiva = gerenciadorCampanhas.campanhaAtiva();
    const presentes = campAtiva.estadoMundo.personagensPresentes;
    const pistasPreview = campAtiva.pistas.slice(0, 4);
    const evidenciasPreview = campAtiva.evidencias.slice(0, 3);
    const primeiroNome = (campAtiva.protagonista.nome || 'Investigador').split(' ')[0];
    const casoAtivo = campAtiva.casoAtivo;
    const escaparHTML = valor => String(valor ?? '').replace(/[&<>"']/g, caractere => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[caractere]);
    const iniciaisProtagonista = (campAtiva.protagonista.nome || 'I')
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map(nome => nome[0])
        .join('')
        .toUpperCase();
    const tituloCena = casoAtivo?.titulo || campAtiva.titulo || 'Registro da campanha';
    const identificadorCena = casoAtivo?.codigo || campAtiva.codigo || 'ARQUIVO DE CAMPANHA';
    const subtituloCena = casoAtivo?.subtitulo || campAtiva.progresso || 'Histórico narrativo da campanha';

    // Chips de sugestão contextual
    let htmlChipsSugestao = "";
    if (casoAtivo) {
        htmlChipsSugestao = `
            <button class="btn-chip" data-acao="${primeiroNome} se aproxima da mesa de Arthur e examina as anotações sobre o Instituto Ardens e o símbolo.">Examinar anotações na mesa</button>
            <button class="btn-chip" data-acao="${primeiroNome} pergunta à Dra. Maya Navarro se há marcas no corpo ou sinais de veneno incomum.">Interrogar Dra. Maya</button>
            <button class="btn-chip" data-acao="${primeiroNome} chama Noah pelo rádio comunicador e pede para checar os 53 segundos do elevador.">Acionar Noah — telemetria</button>
            <button class="btn-chip" data-acao="${primeiroNome} questiona o Capitão Adrian Hale sobre a ordem de diligência ao Depósito 217.">Consultar Adrian — Depósito 217</button>
        `;
    } else {
        htmlChipsSugestao = `
            <button class="btn-chip" data-acao="${primeiroNome} se apresenta ao Capitão Adrian Hale e solicita um relatório das investigações prioritárias.">Apresentar-se a Adrian Hale</button>
            <button class="btn-chip" data-acao="${primeiroNome} questiona Noah Whitmore sobre transmissões interceptadas e câmeras de tráfego.">Consultar Noah — central técnica</button>
            <button class="btn-chip" data-acao="${primeiroNome} revisa as fichas de inquéritos e antecedentes da Divisão de Crimes Especiais.">Revisar arquivos da DCE</button>
            <button class="btn-chip" data-acao="${primeiroNome} organiza suas anotações e prepara o equipamento para entrar em campo.">Preparar equipamento de campo</button>
        `;
    }

    const html = `
        <div class="investigacao-layout">
            <!-- COLUNA PRINCIPAL: NARRATIVA + ACAO -->
            <div class="investigacao-terminal">
                <div class="terminal-header">
                    <div class="terminal-info-cena">
                        <span class="tag-local">
                            <svg width="12" height="12" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/></svg>
                            ${escaparHTML(campAtiva.estadoMundo.localAtual)} — BLACKWOOD
                        </span>
                        <span class="tag-status-cena">${escaparHTML(casoAtivo ? (campAtiva.status || 'CENA ATIVA') : 'TERMINAL DA DCE // HISTÓRIA EM CURSO')}</span>
                    </div>
                    <time class="terminal-relogio">${escaparHTML(campAtiva.estadoMundo.horarioAtual)}</time>
                </div>

                <header class="cena-capa">
                    <div class="cena-capa-conteudo">
                        <span class="cena-capa-identificador">${escaparHTML(campAtiva.episodio || 'CENA ATUAL')}</span>
                        <h1>${escaparHTML(campAtiva.estadoMundo.localAtual || 'Local não informado')}</h1>
                        <p>${escaparHTML(campAtiva.estadoMundo.clima || 'Condições do local não registradas')}</p>
                    </div>
                    <div class="cena-capa-protagonista" aria-label="Protagonista da campanha">
                        <span class="cena-capa-iniciais" aria-hidden="true">${escaparHTML(iniciaisProtagonista)}</span>
                        <span class="cena-capa-identidade">
                            <small>EM CENA</small>
                            <strong>${escaparHTML(campAtiva.protagonista.nome || 'Investigador')}</strong>
                            <span>${escaparHTML(campAtiva.protagonista.cargo || 'Investigador')}</span>
                        </span>
                    </div>
                </header>

                <div class="terminal-historico" id="terminal-historico" aria-label="Histórico narrativo da cena">
                    ${gerarHtmlMensagensCena()}
                </div>

                <div class="terminal-input-box">
                    <div class="sugestoes-acoes">
                        ${htmlChipsSugestao}
                    </div>
                    <div class="input-container">
                        <textarea class="textarea-acao" id="input-acao" aria-label="Ação da protagonista" placeholder="O que você faz, ${escaparHTML(campAtiva.protagonista.nome || 'Detetive')}?"></textarea>
                        <button class="btn-enviar-acao" id="btn-enviar-acao">
                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/></svg>
                            <span>Enviar</span>
                        </button>
                    </div>
                </div>
            </div>

            <!-- COLUNA LATERAL: CONTEXTO SECUNDÁRIO COLAPSÁVEL -->
            <div class="investigacao-lateral">

                <!-- Bloco de contexto do caso -->
                ${casoAtivo ? `
                    <div class="contexto-caso-bloco">
                        <span class="contexto-caso-rotulo">${escaparHTML(identificadorCena)}${campAtiva.episodio ? ` — ${escaparHTML(campAtiva.episodio)}` : ''}</span>
                        <div class="contexto-linha">
                            <div class="contexto-item">
                                <span class="contexto-item-rotulo">Local</span>
                                <span class="contexto-item-valor">${escaparHTML(campAtiva.estadoMundo.localAtual)}</span>
                            </div>
                            <div class="contexto-item">
                                <span class="contexto-item-rotulo">Status</span>
                                <span class="contexto-item-valor">${escaparHTML(campAtiva.status || 'Investigação ativa')}</span>
                            </div>
                            <div class="contexto-item">
                                <span class="contexto-item-rotulo">Horário</span>
                                <span class="contexto-item-valor destaque">${escaparHTML(campAtiva.estadoMundo.horarioAtual)}</span>
                            </div>
                        </div>
                    </div>
                ` : `
                    <div class="contexto-caso-bloco" style="border-left: 2px solid var(--azul-forense);">
                        <span class="contexto-caso-rotulo">STATUS OPERACIONAL</span>
                        <h4>Aguardando Caso</h4>
                        <div style="font-size: 11px; color: var(--texto-secundario); margin: 6px 0 10px 0; line-height: 1.4;">
                            Nova história ativa. Você pode escolher e assumir uma investigação específica no catálogo de Casos.
                        </div>
                        <button class="btn-contexto" style="width: 100%; justify-content: center;" onclick="window.navegarAba('casos')">
                            VER CASOS DISPONÍVEIS →
                        </button>
                    </div>
                `}

                <!-- Painel colapsável: Pessoas presentes -->
                <div class="painel-colapsavel" id="painel-pessoas">
                    <button class="painel-col-header" type="button" aria-expanded="true" aria-controls="painel-pessoas-corpo" onclick="togglePainel('painel-pessoas')">
                        <span class="painel-col-titulo">
                            <svg width="13" height="13" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197"/></svg>
                            Presentes na Cena
                            <span class="painel-col-contador">${presentes.length}</span>
                        </span>
                        <svg class="painel-col-toggle" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                    </button>
                    <div class="painel-col-corpo" id="painel-pessoas-corpo" aria-hidden="false">
                        ${presentes.map(nome => `
                            <div class="item-painel">
                                <div class="item-painel-nome">${escaparHTML(nome)}</div>
                                <div class="item-painel-sub">Disponível para interação</div>
                            </div>
                        `).join('')}
                    </div>
                </div>

                <!-- Painel colapsável: Pistas -->
                <div class="painel-colapsavel colapsado" id="painel-pistas">
                    <button class="painel-col-header" type="button" aria-expanded="false" aria-controls="painel-pistas-corpo" onclick="togglePainel('painel-pistas')">
                        <span class="painel-col-titulo">
                            <svg width="13" height="13" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                            Pistas Confirmadas
                            <span class="painel-col-contador">${campAtiva.pistas.length}</span>
                        </span>
                        <svg class="painel-col-toggle" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                    </button>
                    <div class="painel-col-corpo" id="painel-pistas-corpo" aria-hidden="true" inert>
                        ${campAtiva.pistas.length > 0 ? `
                            ${pistasPreview.map(p => `
                                <div class="item-painel" onclick="window.abrirModalDetalhes('${p.id}', 'pista')">
                                    <div class="item-painel-nome">${p.titulo}</div>
                                    <div class="item-painel-sub">${p.status}</div>
                                </div>
                            `).join('')}
                            <button class="btn-contexto" onclick="window.navegarAba && window.navegarAba('pistas')">Ver todas as pistas →</button>
                        ` : `
                            <div style="font-size: 11px; color: var(--texto-mutado); font-family: var(--fonte-mono); padding: 8px 4px;">
                                Nenhuma pista catalogada.
                            </div>
                        `}
                    </div>
                </div>

                <!-- Painel colapsável: Evidências -->
                <div class="painel-colapsavel colapsado" id="painel-evidencias">
                    <button class="painel-col-header" type="button" aria-expanded="false" aria-controls="painel-evidencias-corpo" onclick="togglePainel('painel-evidencias')">
                        <span class="painel-col-titulo">
                            <svg width="13" height="13" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                            Evidências
                            <span class="painel-col-contador">${campAtiva.evidencias.length}</span>
                        </span>
                        <svg class="painel-col-toggle" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                    </button>
                    <div class="painel-col-corpo" id="painel-evidencias-corpo" aria-hidden="true" inert>
                        ${campAtiva.evidencias.length > 0 ? `
                            ${evidenciasPreview.map(e => `
                                <div class="item-painel" onclick="window.abrirModalDetalhes('${e.id}', 'evidencia')">
                                    <div class="item-painel-nome">${e.nome}</div>
                                    <div class="item-painel-sub">${e.statusCustodia}</div>
                                </div>
                            `).join('')}
                            <button class="btn-contexto" onclick="window.navegarAba && window.navegarAba('evidencias')">Ver todas as evidências →</button>
                        ` : `
                            <div style="font-size: 11px; color: var(--texto-mutado); font-family: var(--fonte-mono); padding: 8px 4px;">
                                Nenhuma evidência sob custódia.
                            </div>
                        `}
                    </div>
                </div>

            </div>
        </div>
    `;

    conteudoPrincipal.innerHTML = html;
    configurarInteracaoTerminal();
    configurarNavegacaoContexto();
}

/**
 * Toggle dos painéis colapsáveis do contexto lateral
 */
function togglePainel(id) {
    const painel = document.getElementById(id);
    if (!painel) return;
    const colapsado = painel.classList.toggle('colapsado');
    const cabecalho = painel.querySelector('.painel-col-header');
    const corpo = painel.querySelector('.painel-col-corpo');
    cabecalho?.setAttribute('aria-expanded', String(!colapsado));
    corpo?.setAttribute('aria-hidden', String(colapsado));
    if (corpo) corpo.inert = colapsado;
}

/**
 * Registra window.navegarAba para uso nos botões de contexto
 */
function configurarNavegacaoContexto() {
    window.togglePainel = togglePainel;
    window.navegarAba = (aba) => {
        navItens.forEach(nav => {
            const ativa = nav.getAttribute('data-aba') === aba;
            nav.classList.toggle('ativo', ativa);
            if (ativa) nav.setAttribute('aria-current', 'page');
            else nav.removeAttribute('aria-current');
        });
        if (window.matchMedia('(max-width: 768px)').matches) {
            corpoSistema.classList.add('navegacao-mobile-fechada');
            atualizarBotaoSidebar();
        }
        estadoLocal.abaAtiva = aba;
        renderizarAba(aba);
    };
}

/**
 * Renderiza blocos de mensagens com formatação canônica de diálogos da campanha ativa
 */
function gerarHtmlMensagensCena() {
    const campAtiva = gerenciadorCampanhas.campanhaAtiva();
    const nomeProtagonista = (campAtiva.protagonista && campAtiva.protagonista.nome) 
        ? campAtiva.protagonista.nome.toUpperCase() 
        : 'INVESTIGADOR';
    const escaparHTML = valor => String(valor ?? '').replace(/[&<>"']/g, caractere => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[caractere]);

    return campAtiva.mensagensCena.map(m => {
        if (m.tipo === 'SISTEMA') {
            return `<div class="bloco-mensagem bloco-sistema">${escaparHTML(m.conteudo)}</div>`;
        } else if (m.tipo === 'NARRADOR') {
            const paragrafos = String(m.conteudo ?? '').split(/\n\s*\n/).map(paragrafo => paragrafo.trim()).filter(Boolean);
            const conteudoFormatado = paragrafos.map(paragrafo => {
                const fala = paragrafo.match(/^([^:\n]{1,48}):\s*—\s*([\s\S]*)$/);
                if (fala) {
                    return `<p class="bloco-dialogo"><strong>${escaparHTML(fala[1])}</strong><span>— ${escaparHTML(fala[2])}</span></p>`;
                }
                return `<p class="bloco-narrador-paragrafo">${escaparHTML(paragrafo)}</p>`;
            }).join('');
            return `
                <div class="bloco-mensagem bloco-narrador">
                    <div class="bloco-narrador-header">
                        <span>NARRADOR // AMBIENTE & NPCS</span>
                        <time>${escaparHTML(m.horario)}</time>
                    </div>
                    <div class="bloco-narrador-texto">${conteudoFormatado}</div>
                </div>
            `;
        } else if (m.tipo === 'JOGADOR') {
            return `
                <div class="bloco-mensagem bloco-jogador">
                    <div class="bloco-jogador-header">
                        <span>${escaparHTML(nomeProtagonista)} // PROTAGONISTA</span>
                        <time>${escaparHTML(m.horario)}</time>
                    </div>
                    <div class="bloco-jogador-texto">${escaparHTML(m.conteudo)}</div>
                </div>
            `;
        }
    }).join('');
}

/**
 * Configura ações e resposta reativa dos NPCs no terminal investigativo
 */
function configurarInteracaoTerminal() {
    const inputAcao = document.getElementById('input-acao');
    const btnEnviar = document.getElementById('btn-enviar-acao');
    const historicoBox = document.getElementById('terminal-historico');
    const chipsSugestao = document.querySelectorAll('.btn-chip');
    const campanhaId = gerenciadorCampanhas.campanhaAtiva().id;

    chipsSugestao.forEach(chip => {
        chip.addEventListener('click', () => {
            inputAcao.value = chip.getAttribute('data-acao');
            inputAcao.focus();
        });
    });

    const persistirOperacao = async (operacao) => {
        btnEnviar.disabled = true;
        btnEnviar.innerHTML = `<span>Salvando...</span>`;
        try {
            const resposta = await salvarAcao(campanhaId, operacao);
            gerenciadorCampanhas.substituirCampanhaPersistida(resposta.campanha, resposta.versao);
            gerenciadorCampanhas.operacoesPendentes.delete(campanhaId);
            if (gerenciadorCampanhas.campanhaAtiva().id === campanhaId) {
                estadoLocal.mensagensCena = [...resposta.campanha.mensagensCena];
                estadoLocal.pistasDesc = [...resposta.campanha.pistas];
                estadoLocal.horarioAtual = resposta.campanha.estadoMundo.horarioAtual;
                estadoLocal.contadorAcoes = resposta.campanha.contadorAcoes;
                inputAcao.value = '';
                atualizarRelogioUI();
                historicoBox.innerHTML = gerarHtmlMensagensCena();
                historicoBox.scrollTop = historicoBox.scrollHeight;
            }
        } catch (erro) {
            if (erro instanceof ApiError && erro.status === 409) {
                try {
                    await gerenciadorCampanhas.atualizarCampanhaDoServidor(campanhaId);
                    gerenciadorCampanhas.operacoesPendentes.delete(campanhaId);
                } catch (erroAtualizacao) {
                    alert(`A campanha mudou em outra sessão e não foi possível recarregá-la. ${erroAtualizacao.message}`);
                    return;
                }
            }
            alert(`A ação não foi confirmada pelo backend. O estado local foi preservado. ${erro.message}`);
        } finally {
            btnEnviar.disabled = false;
            btnEnviar.innerHTML = `
                <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"/></svg>
                <span>Enviar</span>
            `;
        }
    };

    const submeterAcao = async () => {
        if (btnEnviar.disabled) return;
        const operacaoPendente = gerenciadorCampanhas.operacoesPendentes.get(campanhaId);
        if (operacaoPendente) {
            inputAcao.value = operacaoPendente.acao;
            await persistirOperacao(operacaoPendente);
            return;
        }

        const textoAcao = inputAcao.value.trim();
        if (!textoAcao) return;

        const original = gerenciadorCampanhas.campanhaAtiva();
        const campAtiva = JSON.parse(JSON.stringify(original));
        const operacao = {
            chaveOperacao: `acao-${campanhaId}-${crypto.randomUUID()}`,
            versaoEsperada: gerenciadorCampanhas.versaoDaCampanha(campanhaId),
            acao: textoAcao,
            campanha: campAtiva
        };
        gerenciadorCampanhas.operacoesPendentes.set(campanhaId, operacao);
        campAtiva.mensagensCena.push({
            tipo: 'JOGADOR',
            conteudo: textoAcao,
            horario: campAtiva.estadoMundo.horarioAtual
        });
        campAtiva.eventLog.push({
            tipo: 'ACAO_PROTAGONISTA',
            descricao: textoAcao,
            horario: campAtiva.estadoMundo.horarioAtual
        });

        btnEnviar.disabled = true;
        btnEnviar.innerHTML = `<span>Processando...</span>`;
        await new Promise(resolve => setTimeout(resolve, 600));

        // 2. Simula resposta narrativa reativa dependendo se há caso ativo ou se é prólogo
        {
            campAtiva.contadorAcoes++;
            
            // Avanço determinístico do tempo (substitui cálculo defeituoso anterior)
            const duracaoAcao = calcularDuracaoAcao(textoAcao);
            const idAcao = `acao-${campAtiva.id}-${campAtiva.contadorAcoes}`;
            aplicarAvancoTempo(campAtiva.estadoMundo, duracaoAcao, idAcao);

            // Verifica eventos do mundo vivo após o avanço de tempo
            const eventosProcessados = verificarEventosMundo(campAtiva);
            // Contexto atual do mundo disponível para o Narrative Engine
            // (não exibido ao jogador diretamente — usado para consistência narrativa)
            const _contextoMundo = gerarContextoMundo(campAtiva);

            let respostaSimulada = "";
            const nomeProt = campAtiva.protagonista.nome;

            if (campAtiva.casoAtivo) {
                // Caso 001 ativo — tom: investigação policial de série de TV
                if (textoAcao.toLowerCase().includes('maya') || textoAcao.toLowerCase().includes('veneno') || textoAcao.toLowerCase().includes('corpo')) {
                    // Verifica o estado real da Maya no mundoVivo antes de responder
                    const localMaya = campAtiva.mundoVivo
                        ? (campAtiva.mundoVivo.estadoNPCs['Maya'] || {}).local
                        : 'Apartamento 504';
                    if (localMaya && localMaya !== 'Apartamento 504') {
                        // Maya já saiu para o necrotério — mundo reflete esse estado
                        respostaSimulada = `Helena olha para o corredor e depois de volta para você.\n\nHelena: — Maya já foi. Levou o corpo para o necrotério há pouco.\n\nUma pausa.\n\nHelena: — Ela disse que liga quando tiver alguma coisa. Você pode tentar o rádio se for urgente.`;
                    } else {
                        respostaSimulada = `Maya olha para cima brevemente, sem parar o que está fazendo.\n\nMaya: — Nenhum trauma externo. Sem marcas, sem odor característico, sem sinal de luta. O coração simplesmente parou.\n\nEla volta ao trabalho.\n\nMaya: — Não sei o que causou isso ainda. Preciso do necrotério para te dar uma resposta de verdade.\n\nHelena: — Quanto tempo?\n\nMaya: — Menos se você parar de me interromper.`;
                    }
                } else if (textoAcao.toLowerCase().includes('noah') || textoAcao.toLowerCase().includes('rádio') || textoAcao.toLowerCase().includes('elevador')) {
                    respostaSimulada = `O rádio crepita.\n\nNoah: — Aqui. Tô com os logs do elevador na tela. O sistema parou por 53 segundos exatos entre o terceiro e o quarto andar. Sem falha elétrica, sem acionamento de alarme.\n\nUma pausa. Som de teclado ao fundo.\n\nNoah: — Tem uma irregularidade de frequência nesse intervalo. Ainda estou analisando. Preciso de mais tempo.\n\nAdrian: — Quanto tempo?\n\nNoah: — Mais do que você quer ouvir.\n\nAdrian: — Liberdade total até as cinco da manhã.\n\nNoah: — Então talvez menos.`;
                } else if (textoAcao.toLowerCase().includes('adrian') || textoAcao.toLowerCase().includes('depósito') || textoAcao.toLowerCase().includes('terminal')) {
                    respostaSimulada = `Adrian abre o tablet e passa uma imagem aérea do local.\n\nAdrian: — Depósito 217. Terminal ferroviário antigo, zona leste. Desativado há quinze anos.\n\nHelena: — Helena conferiu o registro de propriedade hoje cedo. A concessão está ligada a uma empresa que não aparecia em lugar nenhum antes.\n\nAdrian: — Então vai para a lista. Quando a perícia liberar essa sala, continuamos de lá.`;
                } else {
                    respostaSimulada = `Helena circula a escrivaninha e olha os papéis espalhados.\n\nHelena: — Recortes. Datas diferentes, mas o mesmo símbolo aparecendo em todos. E essa frase aqui — marcada duas vezes.\n\nEla aponta para a anotação marginal de Arthur.\n\nHelena: — 'Eles não desaparecem. Eles são apagados.'\n\nAdrian: — Alguém rastreou a origem desse símbolo?\n\nHelena: — Ainda não.\n\nAdrian: — Então é por onde a gente começa.`;
                }
            } else {
                // Prólogo no Saguão da DCE — tom: chegada, apresentação, rotina
                if (textoAcao.toLowerCase().includes('adrian') || textoAcao.toLowerCase().includes('ordem') || textoAcao.toLowerCase().includes('apresentar')) {
                    respostaSimulada = `Adrian levanta os olhos da pasta.\n\nAdrian: — A DCE cuida dos casos que os distritos devolvem sem solução. Se você está aqui, a gente assume que sabe a diferença.\n\nEle fecha o arquivo.\n\nAdrian: — Quando estiver pronto, dá uma olhada nos inquéritos em aberto. Tem coisa esperando atenção.`;
                } else if (textoAcao.toLowerCase().includes('noah') || textoAcao.toLowerCase().includes('terminal') || textoAcao.toLowerCase().includes('sistema')) {
                    respostaSimulada = `Noah gira na cadeira sem tirar os olhos dos monitores.\n\nNoah: — Terminal configurado. Você tem acesso total à rede interna.\n\nEle finalmente olha para cima.\n\nNoah: — Quando pegar um dossiê, me avisa. Eu sincronizo câmeras e rádio do setor em menos de dois minutos. Mais rápido se eu não tiver mais nada aberto.\n\nUma pausa.\n\nNoah: — Que, pra ser honesto, raramente acontece.`;
                } else if (textoAcao.toLowerCase().includes('caso') || textoAcao.toLowerCase().includes('arquivo') || textoAcao.toLowerCase().includes('inquérito')) {
                    respostaSimulada = `Noah digita sem olhar.\n\nNoah: — Tem um inquérito de alta prioridade aberto hoje. Caso #001. Morte no Apartamento 504, zona central.\n\nEle passa uma tela com o resumo do dossiê.\n\nNoah: — Vai aparecer na aba de Casos com tudo que temos até agora. Que não é muito — mas é um começo.`;
                } else {
                    respostaSimulada = `A central da DCE funciona em ritmo baixo nessa hora da noite. Teclados. Rádio em volume mínimo.\n\nAdrian aparece com uma pasta e coloca sobre a mesa.\n\nAdrian: — Quando quiser começar, a aba de Casos tem o que temos em aberto.\n\nEle não espera resposta e volta ao trabalho.\n\nNoah comenta sem levantar a cabeça dos monitores.\n\nNoah: — Ele é assim com todo mundo. Não leva a mal.`;
                }
            }

            campAtiva.mensagensCena.push({
                tipo: 'NARRADOR',
                conteudo: respostaSimulada,
                horario: campAtiva.estadoMundo.horarioAtual
            });

            campAtiva.eventLog.push({
                tipo: 'RESPOSTA_NARRATIVA',
                descricao: `Resposta narrativa no horário ${campAtiva.estadoMundo.horarioAtual}`,
                horario: campAtiva.estadoMundo.horarioAtual
            });
            campAtiva.ultimaAtividade = `Hoje, ${campAtiva.estadoMundo.horarioAtual}`;
            campAtiva.progresso = campAtiva.casoAtivo
                ? `Ações: ${campAtiva.contadorAcoes} | ${campAtiva.casoAtivo.codigo}`
                : `Ações: ${campAtiva.contadorAcoes} | Prólogo`;
        }
        await persistirOperacao(operacao);
    };

    btnEnviar.addEventListener('click', submeterAcao);
    inputAcao.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            submeterAcao();
        }
    });

    historicoBox.scrollTop = historicoBox.scrollHeight;
}

/**
 * 3. TELA: CASOS (SELEÇÃO DELIBERADA DE CASOS)
 */
function renderizarCasos() {
    const campAtiva = gerenciadorCampanhas.campanhaAtiva();

    const html = `
        <div style="margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center;">
            <div>
                <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">DIVISÃO DE CRIMES ESPECIAIS — DOSSIÊS</h2>
                <p style="color: var(--texto-secundario); font-size: 13px;">Inquéritos oficiais da Cidade de Blackwood que desafiam a lógica e a ciência forense.</p>
            </div>
            <div style="font-family: var(--fonte-mono); font-size: 12px; color: var(--azul-forense); background: var(--bg-card); padding: 8px 16px; border-radius: 6px; border: 1px solid var(--cinza-borda);">
                STATUS: ${campAtiva.casoAtivo ? `${campAtiva.casoAtivo.codigo} ATIVO NA CAMPANHA` : 'NENHUM CASO VINCULADO'}
            </div>
        </div>

        <div class="grid-casos">
            ${dadosCasos.map(c => {
                const esteAtivoNaCampanha = campAtiva.casoAtivo && campAtiva.casoAtivo.id === c.id;
                return `
                <div class="card-caso-grande">
                    <div>
                        <div class="caso-header-topo">
                            <span>${c.codigo} // ${c.episodio}</span>
                            <span class="badge-status-caso">${esteAtivoNaCampanha ? 'EM INVESTIGAÇÃO ATIVA' : c.status}</span>
                        </div>
                        <h3 class="caso-nome">${c.titulo}</h3>
                        <div style="font-size: 12px; color: var(--azul-forense); font-family: var(--fonte-mono); margin-bottom: 12px;">${c.subtitulo}</div>
                        <p class="caso-descricao">${c.descricao}</p>
                    </div>

                    <div>
                        <div class="caso-stats-bar">
                            <div>VÍTIMA: <b style="color: #fff;">${c.vitima}</b></div>
                            <div>LOCAL: <b style="color: #fff;">${c.localPrincipal}</b></div>
                            <div>PISTAS: <b style="color: var(--azul-forense);">${c.pistasVinculadas}</b></div>
                            <div>EVIDÊNCIAS: <b style="color: var(--carmesim-brilho);">${c.evidenciasVinculadas}</b></div>
                        </div>

                        ${esteAtivoNaCampanha ? `
                            <button class="btn-primario" onclick="window.navegarAba('investigacao')">
                                ACESSAR TERMINAL DA CENA (APTO 504)
                            </button>
                        ` : `
                            <button class="btn-primario" onclick="window.assumirCaso('${c.id}')">
                                ASSUMIR ESTE CASO (${c.codigo})
                            </button>
                        `}
                    </div>
                </div>
                `;
            }).join('')}
        </div>
    `;
    conteudoPrincipal.innerHTML = html;
}

/**
 * 4. TELA: PESSOAS (BANCO DE PERSONAGENS CANÔNICOS OFICIAIS)
 */
function renderizarPessoas() {
    const html = `
        <div style="margin-bottom: 24px;">
            <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">CRIMSON VEIL — BANCO DE PERSONAGENS</h2>
            <p style="color: var(--texto-secundario); font-size: 13px;">Registros oficiais da Divisão de Crimes Especiais, Perícia Médica, Psicologia e Figuras Notáveis de Blackwood.</p>
        </div>

        <div class="grid-casos">
            ${dadosPessoas.map(p => `
                <div class="card-caso-grande">
                    <div>
                        <div class="caso-header-topo">
                            <span>${p.divisao || 'REGISTRO'}</span>
                            <span class="tag-badge ${p.status === 'Óbito confirmado às 02:17' ? 'tag-badge-carmesim' : 'tag-badge-azul'}">${p.status}</span>
                        </div>
                        <h3 class="caso-nome" style="font-size: 18px;">${p.nome}</h3>
                        <div style="font-size: 11px; font-family: var(--fonte-mono); color: var(--carmesim-brilho); margin-bottom: 4px;">${p.cargo} (${p.idade})</div>
                        <div style="font-size: 11px; font-family: var(--fonte-mono); color: var(--azul-forense); margin-bottom: 10px;">${p.personalidade}</div>
                        <p class="caso-descricao">${p.detalhes}</p>
                    </div>

                    <div class="caso-stats-bar" style="margin-bottom: 0;">
                        <div>PAPEL: <b style="color: #fff;">${p.papel}</b></div>
                        <div>STATUS: <b style="color: var(--azul-forense);">${p.status}</b></div>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
    conteudoPrincipal.innerHTML = html;
}

/**
 * 5. TELA: LOCAIS
 */
function renderizarLocais() {
    const html = `
        <div style="margin-bottom: 24px;">
            <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">MAPEAMENTO DE LOCAIS — CIDADE DE BLACKWOOD</h2>
            <p style="color: var(--texto-secundario); font-size: 13px;">Cenas de crime ativas, base da DCE e pontos urbanos sob investigação.</p>
        </div>

        <div class="grid-casos">
            ${dadosLocais.map(loc => `
                <div class="card-caso-grande">
                    <div>
                        <div class="caso-header-topo">
                            <span>${loc.tipo}</span>
                            <span class="tag-badge ${loc.ativoAgora ? 'tag-badge-carmesim' : 'tag-badge-ouro'}">${loc.statusCena}</span>
                        </div>
                        <h3 class="caso-nome" style="font-size: 18px;">${loc.nome}</h3>
                        <div style="font-size: 12px; font-family: var(--fonte-mono); color: var(--texto-secundario); margin-bottom: 10px;">${loc.endereco}</div>
                        <p class="caso-descricao">${loc.descricao}</p>
                    </div>

                    <div style="border-top: 1px solid var(--cinza-borda); padding-top: 14px;">
                        <button class="btn-primario" style="padding: 10px 16px; font-size: 12px;" ${loc.ativoAgora ? 'disabled style="opacity: 0.6;"' : ''} onclick="alert('Diligência da equipe para ${loc.nome} registrada.')">
                            ${loc.ativoAgora ? 'CENA ATIVA NO MOMENTO' : 'SOLICITAR DESLOCAMENTO COM A EQUIPE'}
                        </button>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
    conteudoPrincipal.innerHTML = html;
}

/**
 * 6. TELA: PISTAS (ISOLADAS POR CAMPANHA)
 */
function renderizarPistas() {
    const campAtiva = gerenciadorCampanhas.campanhaAtiva();
    const pistas = campAtiva.pistas;

    const html = `
        <div style="margin-bottom: 24px;">
            <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">CATÁLOGO DE PISTAS OBSERVADAS</h2>
            <p style="color: var(--texto-secundario); font-size: 13px;">Indícios, declarações e elementos perceptíveis levantados em campo pela DCE para esta campanha.</p>
        </div>

        ${pistas.length > 0 ? `
            <table class="tabela-investigativa">
                <thead>
                    <tr>
                        <th style="width: 110px;">CÓDIGO</th>
                        <th>TÍTULO DA PISTA</th>
                        <th>LOCAL ORIGEM</th>
                        <th>CATEGORIA</th>
                        <th>STATUS</th>
                    </tr>
                </thead>
                <tbody>
                    ${pistas.map(p => `
                        <tr class="linha-item" onclick="window.abrirModalDetalhes('${p.id}', 'pista')">
                            <td style="font-family: var(--fonte-mono); color: var(--azul-forense); font-weight: 600;">${p.id}</td>
                            <td style="font-weight: 600; color: #fff;">${p.titulo}</td>
                            <td style="color: var(--texto-secundario); font-size: 13px;">${p.origem}</td>
                            <td><span class="tag-badge tag-badge-ouro">${p.categoria}</span></td>
                            <td><span class="tag-badge tag-badge-carmesim">${p.status}</span></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        ` : `
            <div style="color: var(--texto-mutado); font-family: var(--fonte-mono); font-size: 13px; padding: 36px 20px; text-align: center; border: 1px dashed var(--cinza-borda); border-radius: 6px;">
                NENHUMA PISTA REGISTRADA NESTA CAMPANHA ATÉ O MOMENTO.<br>
                <span style="font-size: 11px; color: var(--texto-secundario); margin-top: 6px; display: inline-block;">
                    Interaja com a cena ou assuma um caso na aba de Casos para catalogar indícios.
                </span>
            </div>
        `}
    `;
    conteudoPrincipal.innerHTML = html;
}

/**
 * 7. TELA: EVIDÊNCIAS (ISOLADAS POR CAMPANHA)
 */
function renderizarEvidencias() {
    const campAtiva = gerenciadorCampanhas.campanhaAtiva();
    const evidencias = campAtiva.evidencias;

    const html = `
        <div style="margin-bottom: 24px;">
            <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">ACERVO DE EVIDÊNCIAS FORENSES — DCE</h2>
            <p style="color: var(--texto-secundario); font-size: 13px;">Materiais físicos, documentos e laudos oficiais mantidos sob cadeia de custódia da DCE para esta campanha.</p>
        </div>

        ${evidencias.length > 0 ? `
            <table class="tabela-investigativa">
                <thead>
                    <tr>
                        <th style="width: 120px;">ID / PROTOCOLO</th>
                        <th>EVIDÊNCIA / MATERIAL</th>
                        <th>TIPO</th>
                        <th>DATA COLETA</th>
                        <th>STATUS CUSTÓDIA</th>
                    </tr>
                </thead>
                <tbody>
                    ${evidencias.map(e => `
                        <tr class="linha-item" onclick="window.abrirModalDetalhes('${e.id}', 'evidencia')">
                            <td style="font-family: var(--fonte-mono); color: var(--carmesim-brilho); font-weight: 600;">${e.id}</td>
                            <td style="font-weight: 600; color: #fff;">${e.nome}</td>
                            <td><span class="tag-badge tag-badge-azul">${e.tipo}</span></td>
                            <td style="color: var(--texto-secundario); font-size: 13px; font-family: var(--fonte-mono);">${e.dataColeta}</td>
                            <td><span class="tag-badge tag-badge-ouro">${e.statusCustodia}</span></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        ` : `
            <div style="color: var(--texto-mutado); font-family: var(--fonte-mono); font-size: 13px; padding: 36px 20px; text-align: center; border: 1px dashed var(--cinza-borda); border-radius: 6px;">
                NENHUMA EVIDÊNCIA SOB CUSTÓDIA NESTA CAMPANHA.<br>
                <span style="font-size: 11px; color: var(--texto-secundario); margin-top: 6px; display: inline-block;">
                    Itens apreendidos e laudos periciais serão anexados aqui conforme as diligências avançarem.
                </span>
            </div>
        `}
    `;
    conteudoPrincipal.innerHTML = html;
}

/**
 * 8. TELA: LINHA DO TEMPO (TIMELINE)
 */
function renderizarTimeline() {
    const html = `
        <div style="margin-bottom: 24px;">
            <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">LINHA DO TEMPO HISTÓRICA — O PADRÃO DOS SÉCULOS</h2>
            <p style="color: var(--texto-secundario); font-size: 13px;">Marcos canônicos dos desaparecimentos na Cidade de Blackwood conectados pelo mesmo símbolo.</p>
        </div>

        <div class="timeline-container">
            <div class="timeline-linha"></div>
            ${dadosTimeline.map(item => `
                <div class="timeline-marco">
                    <div class="timeline-ano">${item.ano}</div>
                    <div class="timeline-ponto"></div>
                    <div class="timeline-card">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
                            <span class="tag-badge tag-badge-azul">${item.categoria}</span>
                            <span style="font-family: var(--fonte-mono); font-size: 10px; color: var(--carmesim-brilho);">${item.status}</span>
                        </div>
                        <div style="color: #fff; font-size: 13px; font-weight: 500; line-height: 1.6;">${item.evento}</div>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
    conteudoPrincipal.innerHTML = html;
}

/**
 * 9. TELA: QUADRO DE CONEXÕES (INVESTIGATION BOARD)
 */
function renderizarConexoes() {
    const html = `
        <div style="margin-bottom: 24px;">
            <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">QUADRO DE CONEXÕES — DIVISÃO DE CRIMES ESPECIAIS</h2>
            <p style="color: var(--texto-secundario); font-size: 13px;">Mapeamento visual de vítimas, pistas, instituições e figuras de poder da Cidade de Blackwood.</p>
        </div>

        <div class="quadro-conexoes-canvas" id="quadro-canvas">
            <div class="quadro-header">
                DIVISÃO DE CRIMES ESPECIAIS // TEIA DE RELAÇÕES DO CASO 001
            </div>

            <svg class="quadro-svg" id="quadro-svg-linhas">
                ${gerarLinhasSvgConexoes()}
            </svg>

            ${dadosConexoes.nos.map(n => `
                <div class="no-cartao" style="left: ${n.x}px; top: ${n.y}px;">
                    <div class="no-pin"></div>
                    <div class="no-titulo">${n.rotulo}</div>
                    <div class="no-tipo">${n.tipo}</div>
                </div>
            `).join('')}
        </div>
    `;
    conteudoPrincipal.innerHTML = html;
}

function gerarLinhasSvgConexoes() {
    const mapaNos = {};
    dadosConexoes.nos.forEach(n => {
        mapaNos[n.id] = { x: n.x + 90, y: n.y + 35 };
    });

    return dadosConexoes.arestas.map(a => {
        const p1 = mapaNos[a.de];
        const p2 = mapaNos[a.para];
        if (!p1 || !p2) return '';
        return `
            <line x1="${p1.x}" y1="${p1.y}" x2="${p2.x}" y2="${p2.y}" stroke="#e11d48" stroke-width="2" stroke-dasharray="4 2" opacity="0.6"/>
        `;
    }).join('');
}

/**
 * MODAL DE DETALHES
 */
function configurarModal() {
    modalFechar.addEventListener('click', fecharModalDetalhes);
    modalBotaoFechar.addEventListener('click', fecharModalDetalhes);
    btnEditarPerfil.addEventListener('click', abrirGerenciadorImagemPerfil);

    modalOverlay.addEventListener('click', (e) => {
        if (e.target === modalOverlay) {
            fecharModalDetalhes();
        }
    });

    window.abrirModalDetalhes = (id, tipo) => {
        if (tipo === 'pista') {
            const pista = estadoLocal.pistasDesc.find(p => p.id === id);
            if (!pista) return;
            modalTitulo.textContent = `DETALHES DA PISTA: ${pista.id}`;
            modalCorpo.innerHTML = `
                <div style="margin-bottom: 12px; font-family: var(--fonte-mono); font-size: 12px; color: var(--azul-forense);">${pista.categoria} // ${pista.status}</div>
                <h4 style="font-size: 18px; margin-bottom: 12px; color: #fff;">${pista.titulo}</h4>
                <p style="color: var(--texto-secundario); line-height: 1.6; margin-bottom: 16px;">${pista.descricao}</p>
                <div style="font-family: var(--fonte-mono); font-size: 11px; color: var(--texto-mutado); border-top: 1px solid var(--cinza-borda); padding-top: 10px;">
                    ORIGEM: ${pista.origem}
                </div>
            `;
        } else if (tipo === 'evidencia') {
            const evidencia = dadosEvidencias.find(e => e.id === id);
            if (!evidencia) return;
            modalTitulo.textContent = `CUSTÓDIA FORENSE: ${evidencia.id}`;
            modalCorpo.innerHTML = `
                <div style="margin-bottom: 12px; font-family: var(--fonte-mono); font-size: 12px; color: var(--carmesim-brilho);">${evidencia.protocolo} // ${evidencia.statusCustodia}</div>
                <h4 style="font-size: 18px; margin-bottom: 12px; color: #fff;">${evidencia.nome}</h4>
                <p style="color: var(--texto-secundario); line-height: 1.6; margin-bottom: 16px;">${evidencia.resumo}</p>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-family: var(--fonte-mono); font-size: 11px; color: var(--texto-mutado); border-top: 1px solid var(--cinza-borda); padding-top: 10px;">
                    <div>LOCAL: ${evidencia.localColeta}</div>
                    <div>DATA: ${evidencia.dataColeta}</div>
                    <div>PERITO: ${evidencia.peritoResponsavel}</div>
                    <div>TIPO: ${evidencia.tipo}</div>
                </div>
            `;
        }
        modalOverlay.classList.add('ativo');
    };

    window.abrirCasoNaInvestigacao = (idCaso) => {
        navItens.forEach(nav => {
            const ativa = nav.getAttribute('data-aba') === 'investigacao';
            nav.classList.toggle('ativo', ativa);
            if (ativa) nav.setAttribute('aria-current', 'page');
            else nav.removeAttribute('aria-current');
        });
        if (window.matchMedia('(max-width: 768px)').matches) {
            corpoSistema.classList.add('navegacao-mobile-fechada');
            atualizarBotaoSidebar();
        }
        estadoLocal.abaAtiva = 'investigacao';
        renderizarAba('investigacao');
    };

    window.abrirModalImagem = (src, titulo) => {
        modalTitulo.textContent = titulo;
        modalCorpo.innerHTML = `
            <div style="text-align: center;">
                <img src="${src}" alt="${titulo}" style="max-width: 100%; max-height: 70vh; border-radius: 6px; border: 1px solid var(--cinza-borda); box-shadow: 0 0 20px rgba(0,0,0,0.8);">
            </div>
        `;
        modalOverlay.classList.add('ativo');
    };
}

function fecharModalDetalhes() {
    modalOverlay.classList.remove('ativo');
    if (urlPrevisualizacaoPerfil) {
        URL.revokeObjectURL(urlPrevisualizacaoPerfil);
        urlPrevisualizacaoPerfil = null;
    }
}

async function abrirGerenciadorImagemPerfil() {
    const campanha = gerenciadorCampanhas.campanhaAtiva();
    const protagonista = campanha.protagonista;
    modalTitulo.textContent = `PERFIL // ${protagonista.nome}`;
    modalCorpo.innerHTML = `
        <div class="perfil-imagem-gerenciador">
            <img class="perfil-imagem-previa" id="perfil-imagem-previa" alt="Pré-visualização da imagem de perfil" hidden>
            <div id="perfil-avatar-padrao" class="perfil-avatar perfil-avatar-padrao">CV</div>
            <p class="perfil-imagem-status" id="perfil-imagem-status" role="status" aria-live="polite">
                Carregando as preferências do perfil...
            </p>
            <label for="perfil-imagem-arquivo">Escolha uma imagem JPEG, PNG ou WebP</label>
            <input id="perfil-imagem-arquivo" type="file" accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp" disabled>
            <span id="perfil-imagem-limite" class="texto-secundario">Limite configurado pelo backend.</span>
            <div class="perfil-imagem-acoes">
                <button class="btn-primario" id="perfil-imagem-salvar" type="button" disabled>SALVAR IMAGEM</button>
                <button class="btn-acao-campanha" id="perfil-imagem-remover" type="button" disabled>VOLTAR AO AVATAR PADRÃO</button>
            </div>
        </div>
    `;
    modalOverlay.classList.add('ativo');

    const previa = document.getElementById('perfil-imagem-previa');
    const avatarPadrao = document.getElementById('perfil-avatar-padrao');
    const status = document.getElementById('perfil-imagem-status');
    const entrada = document.getElementById('perfil-imagem-arquivo');
    const limiteRotulo = document.getElementById('perfil-imagem-limite');
    const salvar = document.getElementById('perfil-imagem-salvar');
    const remover = document.getElementById('perfil-imagem-remover');
    const formatosAceitos = ['image/jpeg', 'image/png', 'image/webp'];
    let arquivoSelecionado = null;
    let limiteBytes = 2 * 1024 * 1024;
    let possuiImagem = false;
    avatarPadrao.textContent = protagonista.nome.split(/\s+/)
        .filter(Boolean).map(nome => nome[0]).slice(0, 2).join('').toUpperCase();

    const mensagem = (texto, erro = false) => {
        status.textContent = texto;
        status.classList.toggle('erro', erro);
        status.setAttribute('role', erro ? 'alert' : 'status');
    };
    const formatarLimite = bytes => bytes < 1024 * 1024
        ? `${Math.ceil(bytes / 1024)} KB`
        : `${(bytes / (1024 * 1024)).toFixed(1).replace(/\.0$/, '')} MB`;
    const mostrarImagemAtual = () => {
        previa.hidden = true;
        avatarPadrao.hidden = false;
        if (possuiImagem) {
            previa.onload = () => {
                previa.hidden = false;
                avatarPadrao.hidden = true;
            };
            previa.onerror = () => {
                previa.hidden = true;
                avatarPadrao.hidden = false;
            };
            previa.src = `${urlImagemPersonagem(protagonista.id)}?v=${Date.now()}`;
        } else {
            previa.removeAttribute('src');
        }
    };

    try {
        const perfil = await carregarPerfilPersonagem(protagonista.id);
        limiteBytes = perfil.limiteImagemBytes;
        possuiImagem = perfil.possuiImagem;
        limiteRotulo.textContent = `Tamanho máximo: ${formatarLimite(limiteBytes)}. A imagem fica salva no perfil da personagem.`;
        entrada.disabled = false;
        salvar.disabled = true;
        remover.disabled = !possuiImagem;
        mensagem(possuiImagem ? 'Imagem personalizada carregada.' : 'Nenhuma imagem personalizada. O avatar padrão está ativo.');
        mostrarImagemAtual();
    } catch (erro) {
        entrada.disabled = true;
        remover.disabled = true;
        mensagem(`Não foi possível carregar o perfil: ${erro.message}`, true);
    }

    entrada.addEventListener('change', () => {
        arquivoSelecionado = entrada.files?.[0] || null;
        if (!arquivoSelecionado) {
            salvar.disabled = true;
            mostrarImagemAtual();
            return;
        }
        if (!formatosAceitos.includes(arquivoSelecionado.type)) {
            arquivoSelecionado = null;
            entrada.value = '';
            salvar.disabled = true;
            mostrarImagemAtual();
            mensagem('Formato inválido. Selecione uma imagem JPEG, PNG ou WebP.', true);
            return;
        }
        if (arquivoSelecionado.size > limiteBytes) {
            arquivoSelecionado = null;
            entrada.value = '';
            salvar.disabled = true;
            mostrarImagemAtual();
            mensagem(`A imagem excede o limite de ${formatarLimite(limiteBytes)}.`, true);
            return;
        }
        if (urlPrevisualizacaoPerfil) URL.revokeObjectURL(urlPrevisualizacaoPerfil);
        urlPrevisualizacaoPerfil = URL.createObjectURL(arquivoSelecionado);
        previa.onload = null;
        previa.onerror = null;
        previa.src = urlPrevisualizacaoPerfil;
        previa.hidden = false;
        avatarPadrao.hidden = true;
        salvar.disabled = false;
        mensagem('Pré-visualização pronta. Salve para aplicar a imagem ao perfil.');
    });

    salvar.addEventListener('click', async () => {
        if (!arquivoSelecionado) return;
        salvar.disabled = true;
        remover.disabled = true;
        entrada.disabled = true;
        mensagem('Salvando a imagem do perfil...');
        try {
            await salvarImagemPersonagem(protagonista.id, arquivoSelecionado);
            possuiImagem = true;
            arquivoSelecionado = null;
            entrada.value = '';
            atualizarProtagonistaUI(gerenciadorCampanhas.campanhaAtiva().protagonista);
            mostrarImagemAtual();
            remover.disabled = false;
            mensagem('Imagem salva no perfil e vinculada à personagem.');
        } catch (erro) {
            salvar.disabled = false;
            remover.disabled = !possuiImagem;
            mensagem(`Não foi possível salvar a imagem: ${erro.message}`, true);
        } finally {
            entrada.disabled = false;
        }
    });

    remover.addEventListener('click', async () => {
        if (!possuiImagem || !window.confirm('Remover a imagem personalizada e voltar ao avatar padrão?')) return;
        salvar.disabled = true;
        remover.disabled = true;
        entrada.disabled = true;
        mensagem('Removendo a imagem personalizada...');
        try {
            await removerImagemPersonagem(protagonista.id);
            possuiImagem = false;
            arquivoSelecionado = null;
            entrada.value = '';
            atualizarProtagonistaUI(gerenciadorCampanhas.campanhaAtiva().protagonista);
            mostrarImagemAtual();
            mensagem('Imagem removida. O avatar padrão está ativo.');
        } catch (erro) {
            remover.disabled = false;
            mensagem(`Não foi possível remover a imagem: ${erro.message}`, true);
        } finally {
            entrada.disabled = false;
        }
    });
}

/**
 * 10. TELA: IDENTIDADE VISUAL & ARTES CANÔNICAS (BLACKWOOD)
 */
function renderizarArtes() {
    const html = `
        <div style="margin-bottom: 24px;">
            <h2 style="font-family: var(--fonte-titulo); font-size: 22px; letter-spacing: 1px;">IDENTIDADE VISUAL OFICIAL — CIDADE DE BLACKWOOD</h2>
            <p style="color: var(--texto-secundario); font-size: 13px;">Peças visuais, pôster oficial da DCE, infográfico de regras do RPG e banco de personagens.</p>
        </div>

        <div class="galeria-artes-grid">
            <div class="card-arte-oficial">
                <img src="assets/poster-oficial-blackwood.jpg" class="card-arte-preview" alt="Pôster Oficial" onclick="window.abrirModalImagem('assets/poster-oficial-blackwood.jpg', 'PÔSTER OFICIAL — DIVISÃO DE CRIMES ESPECIAIS')">
                <div class="card-arte-info">
                    <h3 class="card-arte-titulo">Pôster Oficial — DCE Blackwood</h3>
                    <p class="card-arte-desc">"Investigamos o impossível. Enfrentamos o desconhecido. Revelamos a verdade."</p>
                </div>
            </div>

            <div class="card-arte-oficial">
                <img src="assets/banco-de-personagens.jpg" class="card-arte-preview" alt="Banco de Personagens" onclick="window.abrirModalImagem('assets/banco-de-personagens.jpg', 'BANCO DE PERSONAGENS — DCE & BLACKWOOD')">
                <div class="card-arte-info">
                    <h3 class="card-arte-titulo">Banco de Personagens Canônicos</h3>
                    <p class="card-arte-desc">Milena, Adrian, Helena, Noah, Maya, Iris, Sofia e Evelyn Cross.</p>
                </div>
            </div>

            <div class="card-arte-oficial">
                <img src="assets/regras-base-rpg.jpg" class="card-arte-preview" alt="Regras Base do RPG" onclick="window.abrirModalImagem('assets/regras-base-rpg.jpg', 'REGRAS BASE DO RPG NARRATIVO')">
                <div class="card-arte-info">
                    <h3 class="card-arte-titulo">Regras Base do RPG Narrativo</h3>
                    <p class="card-arte-desc">Os 11 princípios fundamentais de agência, diálogos e mundo do narrador.</p>
                </div>
            </div>

            <div class="card-arte-oficial">
                <img src="assets/resumo-da-historia.jpg" class="card-arte-preview" alt="Resumo da História" onclick="window.abrirModalImagem('assets/resumo-da-historia.jpg', 'RESUMO DA HISTÓRIA & O VÉU')">
                <div class="card-arte-info">
                    <h3 class="card-arte-titulo">Resumo da História & O Véu</h3>
                    <p class="card-arte-desc">A sociedade oculta de vampiros, conspirações e os relógios parados às 02:17.</p>
                </div>
            </div>
        </div>
    `;
    conteudoPrincipal.innerHTML = html;
}

// Módulos ES6 são deferidos automaticamente — o DOM já está pronto na execução.
// Chamar diretamente evita que o DOMContentLoaded (já disparado) seja ignorado.
inicializarApp();
