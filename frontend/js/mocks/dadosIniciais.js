/**
 * Crimson Veil — Dados Mockados Oficiais do Frontend
 * 
 * Fontes Canônicas: Infográficos Oficiais do Banco de Personagens,
 * Regras Base do RPG Narrativo e Pôster Oficial da Divisão de Crimes Especiais.
 * Cidade de Blackwood.
 */

export const dadosCampanha = {
    id: "camp-001",
    titulo: "Campanha Principal — Investigação Crimson Veil",
    cidade: "Cidade de Blackwood",
    divisao: "Divisão de Crimes Especiais (DCE)",
    lema: "Alguns segredos nunca morrem. E algumas sombras nunca desaparecem.",
    temporada: 1,
    episodio: 1,
    episodioTitulo: "Quando o Sangue Desaparece",
    casoAtivo: {
        id: "caso-001",
        codigo: "CASO #001",
        titulo: "Quando o Sangue Desaparece",
        status: "EM INVESTIGAÇÃO",
        prioridade: "DIVISÃO DE CRIMES ESPECIAIS",
        dataAbertura: "14/10/2026",
        horarioAbertura: "02:17",
        resumo: "Arthur Vasconcelos foi encontrado morto em seu apartamento sob circunstâncias anômalas. Relógios parados simultaneamente às 02:17, interrupção misteriosa de 53 segundos no elevador e anotações cruzando desaparecimentos históricos vinculados ao mesmo símbolo.",
        classificacaoBadge: "CONFIDENCIAL // CRIMES ESPECIAIS"
    },
    protagonista: {
        id: "char-milena",
        nome: "Milena Ramires",
        idade: 25,
        altura: "1,70m",
        cargo: "Detetive da Divisão de Crimes Especiais",
        distintivo: "#4082-DCE",
        origem: "Recém-formada, contratada pela maior delegacia de Blackwood",
        personalidade: "Determinada, Inteligente, Investigativa, Persistente",
        regraOuro: "A MILENA É DA JOGADORA (JULIA). O narrador nunca escreve falas, pensamentos ou ações da Milena."
    },
    estadoMundo: {
        dataAtual: "14 de Outubro de 2026",
        horarioAtual: "02:17",
        localAtual: "Apartamento 504 — Cidade de Blackwood",
        clima: "Chuva fria e constante sobre os edifícios de Blackwood",
        personagensPresentes: [
            "Milena Ramires (Protagonista)",
            "Adrian Hale (Capitão da DCE)",
            "Helena Voss (Detetive da DCE)",
            "Dra. Maya Navarro (Médica Legista)"
        ]
    }
};

export const dadosCenaAtual = {
    id: "cena-01",
    titulo: "CRIMSON VEIL — Episódio I: Quando o Sangue Desaparece",
    local: "Apartamento 504",
    horario: "02:17",
    mensagens: [
        {
            tipo: "SISTEMA",
            conteudo: "CRIMSON VEIL // DIVISÃO DE CRIMES ESPECIAIS — CIDADE DE BLACKWOOD\nEPISÓDIO I — QUANDO O SANGUE DESAPARECE\nAPARTAMENTO 504 // 02:17",
            horario: "02:17"
        },
        {
            tipo: "NARRADOR",
            conteudo: "Apartamento 504. Décimo andar. 02:17.\n\nChuva fina contra os vidros. As luzes das viaturas chegam lá de baixo, refletindo no asfalto.\n\nArthur Vasconcelos está na poltrona, diante da escrivaninha. Imóvel. Três relógios no cômodo mostram o mesmo horário — o pêndulo na parede, o despertador de cabeceira e o mostrador no pulso da vítima. Todos parados em 02:17.\n\nA equipe da DCE trabalha em silêncio. Perícia fotografa. Helena examina a fechadura da porta.\n\nAdrian: — Sem sinal de arrombamento?\n\nHelena: — Nenhum. Quem entrou foi convidado ou tinha chave.\n\nAdrian: — Câmeras do corredor?\n\nHelena: — Noah está verificando.\n\nMaya se afasta da poltrona e puxa as luvas.\n\nMaya: — Nenhum trauma visível. Nenhuma marca. O coração parou, mas não consigo te dizer o motivo agora. Preciso do necrotério.\n\nAdrian olha para os três relógios parados. Não diz nada por um momento.\n\nAdrian: — Sem queda de energia no edifício, de acordo com a portaria.",
            horario: "02:17"
        }
    ]
};

export const dadosCasos = [
    {
        id: "caso-001",
        codigo: "CASO #001",
        titulo: "Quando o Sangue Desaparece",
        subtitulo: "A Morte de Arthur Vasconcelos",
        episodio: "Episódio I",
        status: "EM INVESTIGAÇÃO",
        localPrincipal: "Apartamento 504 — Blackwood",
        vitima: "Arthur Vasconcelos",
        dataOcorrencia: "14/10/2026 — 02:17",
        pistasVinculadas: 6,
        evidenciasVinculadas: 4,
        descricao: "Investigação de homicídio em circunstâncias anômalas. Relógios congelados às 02:17, elevador interrompido por 53 segundos e anotações cruzando desaparecimentos de 1891 a 2026 sob um mesmo símbolo misterioso."
    }
];

export const dadosPessoas = [
    {
        id: "p-milena",
        nome: "Milena Ramires",
        cargo: "Detetive da Divisão de Crimes Especiais",
        divisao: "DCE — Cidade de Blackwood",
        idade: "25 anos (1,70m)",
        papel: "Protagonista Principal (Jogadora)",
        status: "Em Investigação Ativa",
        personalidade: "Determinada, Inteligente, Investigativa, Persistente",
        detalhes: "Recém-formada e contratada pela Divisão de Crimes Especiais. Possui frieza e aguda capacidade de enxergar padrões onde outros veem apenas caos. Pertence exclusivamente à jogadora."
    },
    {
        id: "p-adrian",
        nome: "Adrian Hale",
        cargo: "Capitão da Divisão de Crimes Especiais",
        divisao: "Comando da DCE",
        idade: "36 anos",
        papel: "Líder da Equipe",
        status: "Presente na Cena",
        personalidade: "Liderança forte, Controlado, Estratégico",
        detalhes: "Costuma aliviar situações tensas com humor seco e comentários discretos. Mantém o foco no objetivo e protege os seus oficiais."
    },
    {
        id: "p-helena",
        nome: "Helena Voss",
        cargo: "Detetive da Divisão de Crimes Especiais",
        divisao: "Investigação de Campo",
        idade: "34 anos",
        papel: "Detetive Sênior / Parceira de Milena",
        status: "Presente na Cena",
        personalidade: "Experiente, Racional, Direta, Profissional",
        detalhes: "Tem dificuldade com sarcasmo e ironias, mantendo postura estritamente séria dentro da equipe. Acompanha Milena na linha de frente."
    },
    {
        id: "p-noah",
        nome: "Noah Whitmore",
        cargo: "Especialista em Inteligência Digital / Cibercrime",
        divisao: "Suporte Tecnológico DCE",
        idade: "29 anos",
        papel: "Alívio Cômico & Inteligência Digital",
        status: "Na Central de Monitores",
        personalidade: "Inteligente, Comunicativo, Mais descontraído",
        detalhes: "Monitora sistemas de vigilância, telecomunicações e registros da cidade. Quebra o clima pesado do suspense com humor bem-humorado."
    },
    {
        id: "p-maya",
        nome: "Dra. Maya Navarro",
        cargo: "Médica Legista",
        divisao: "Área Médica e Pericial",
        idade: "31 anos",
        papel: "Perícia Forense e Necrópsia",
        status: "Presente na Cena",
        personalidade: "Extremamente inteligente, Observadora, Peculiar",
        detalhes: "Usa humor negro em situações inadequadas. Tem uma forma estranha, porém cirurgicamente eficiente, de lidar com mortes e cenas de crime."
    },
    {
        id: "p-iris",
        nome: "Iris Bell",
        cargo: "Psicóloga Criminal",
        divisao: "Consultoria Comportamental",
        idade: "38 anos",
        papel: "Perfilamento Psicológico",
        status: "Na Base da DCE",
        personalidade: "Analítica, Perspicaz, Observadora",
        detalhes: "Ajuda a interpretar criminosos, detectar anomalias comportamentais e desvendar padrões psicológicos profundos."
    },
    {
        id: "p-sofia",
        nome: "Sofia Álvarez",
        cargo: "Jornalista Investigativa",
        divisao: "Imprensa Independente",
        idade: "27 anos",
        papel: "Investigadora Externa",
        status: "Em Campo",
        personalidade: "Curiosa, Persistente, Corajosa",
        detalhes: "Investiga casos que a polícia muitas vezes não consegue alcançar oficialmente. Pode encontrar pistas ligadas aos segredos da cidade."
    },
    {
        id: "p-evelyn",
        nome: "Evelyn Cross",
        cargo: "Executiva de Empresa de Segurança Privada",
        divisao: "Elite Financeira de Blackwood",
        idade: "Parece ter 29 anos",
        papel: "Poder / Sociedade Secreta",
        status: "Observando das Sombras",
        personalidade: "Elegante, Influente, Misteriosa",
        detalhes: "Figura de grande poder na cidade. Possui forte conexão com o lado oculto da sociedade (sociedade secreta de vampiros)."
    },
    {
        id: "p-arthur",
        nome: "Arthur Vasconcelos",
        cargo: "Pesquisador Civil Independente",
        divisao: "Vítima do Caso #001",
        idade: "Falecido",
        papel: "Objeto do Inquérito",
        status: "Óbito confirmado às 02:17",
        personalidade: "Obcecado por padrões históricos",
        detalhes: "Compilava anotações ligando desaparecimentos de 1891 a 2026 com o mesmo símbolo. Autor da frase: 'Eles não desaparecem. Eles são apagados.'"
    }
];

export const dadosLocais = [
    {
        id: "loc-01",
        nome: "Apartamento 504 — Residencial Solaris",
        tipo: "Cena de Crime Principal",
        statusCena: "Isolada pela DCE",
        endereco: "Distrito Central — Cidade de Blackwood",
        descricao: "Cena da morte de Arthur Vasconcelos. Relógios parados às 02:17, telemetria de 53 segundos de elevador parado e anotações originais.",
        ativoAgora: true
    },
    {
        id: "loc-02",
        nome: "Sede da Divisão de Crimes Especiais",
        tipo: "Quartel-General Policial",
        statusCena: "Operação Contínua",
        endereco: "Complexo Cívico de Blackwood",
        descricao: "Base de comando do Capitão Adrian Hale, laboratório forense da Dra. Maya e central de monitoramento digital de Noah.",
        ativoAgora: false
    },
    {
        id: "loc-03",
        nome: "Depósito 217 (Terminal Ferroviário Abandonado)",
        tipo: "Instalação Industrial Desativada",
        statusCena: "Alvo da Próxima Diligência",
        endereco: "Complexo Ferroviário Desativado — Zona Leste",
        descricao: "Galpão indicado em registro oficial obtido pela polícia. Complexo ferroviário desativado há quinze anos.",
        ativoAgora: false
    },
    {
        id: "loc-04",
        nome: "Rua Alderbrook",
        tipo: "Ponto Histórico Conspiratório",
        statusCena: "Arquivo Histórico",
        endereco: "Distrito Norte — Blackwood",
        descricao: "Ponto citado nos manuscritos de Arthur relativo ao evento misterioso registrado em 2008 associado ao mesmo símbolo.",
        ativoAgora: false
    }
];

export const dadosPistas = [
    {
        id: "PISTA-01",
        titulo: "Relógios Parados Simultaneamente às 02:17",
        origem: "Apartamento 504",
        categoria: "Anomalia Temporal / Física",
        status: "CONFIRMADA",
        descricao: "Três relógios analógicos e o mostrador do pulso da vítima pararam exatamente às 02:17 sem interrupção de energia no imóvel."
    },
    {
        id: "PISTA-02",
        titulo: "Elevador Travado por 53 Segundos",
        origem: "Painel de Telemetria do Edifício",
        categoria: "Registro Técnico Forense",
        status: "CONFIRMADA",
        descricao: "O elevador social permaneceu estático entre o 3º e o 4º andar por 53 segundos exatos sem registro de acionamento do alarme sonoro."
    },
    {
        id: "PISTA-03",
        titulo: "Registros de CFTV Desaparecidos / Sem Suspeitos",
        origem: "Central de Segurança de Blackwood",
        categoria: "Vigilância Digital",
        status: "SOB ANÁLISE DE NOAH",
        descricao: "Noah Whitmore confirmou que nenhuma imagem de estranho foi gravada nas câmeras durante a janela de horário do óbito."
    },
    {
        id: "PISTA-04",
        titulo: "O Glifo Circular / Símbolo Recorrente (1891 - 2026)",
        origem: "Cadernos de Arthur Vasconcelos",
        categoria: "Padrão Oculto / Conspiração",
        status: "CONFIRMADA",
        descricao: "O mesmo símbolo arquetípico aparece desenhado em recortes de desaparecimentos ocorridos em 1891, 1924, 1958, 1989 e 2008."
    },
    {
        id: "PISTA-05",
        titulo: "Frase de Arthur: 'Eles não desaparecem. Eles são apagados.'",
        origem: "Anotação Marginal Periciada",
        categoria: "Hipótese Investigativa",
        status: "EM APURAÇÃO",
        descricao: "Arthur deduziu que os desaparecimentos não são fortuitos, mas operações deliberadas de eliminação sistemática ao longo dos séculos."
    },
    {
        id: "PISTA-06",
        titulo: "Registro Cadastral do Depósito 217",
        origem: "Divisão de Crimes Especiais",
        categoria: "Próximo Ponto de Campo",
        status: "NOVA DILIGÊNCIA",
        descricao: "Documento oficial revelando movimentação recente ligada a Arthur no antigo terminal ferroviário desativado há 15 anos."
    }
];

export const dadosEvidencias = [
    {
        id: "EVID-01",
        protocolo: "DCE-PERICIA-001/26",
        nome: "Cadernos de Apontamentos Históricos de Arthur",
        tipo: "Documento Material Apreendido",
        localColeta: "Mesa de Estudos — Apto 504",
        dataColeta: "14/10/2026 — 02:45",
        peritoResponsavel: "Divisão de Perícia Técnica DCE",
        statusCustodia: "Sob Custódia Legal",
        resumo: "Cadernos com recortes correlacionando desaparecimentos entre 1891 e 2026 e o símbolo compartilhado da sociedade secreta."
    },
    {
        id: "EVID-02",
        protocolo: "DCE-PERICIA-002/26",
        nome: "Logs Eletrônicos de Telemetria do Elevador",
        tipo: "Mídia Técnica Pericial",
        localColeta: "Painel Central do Edifício Solaris",
        dataColeta: "14/10/2026 — 03:05",
        peritoResponsavel: "Noah Whitmore (Cibercrime DCE)",
        statusCustodia: "Análise Digital Concluída",
        resumo: "Comprovante eletrônico do intervalo de 53 segundos de paralisação no poço do elevador sem falha elétrica no quadro geral."
    },
    {
        id: "EVID-03",
        protocolo: "DCE-PERICIA-003/26",
        nome: "Laudo Preliminar de Necrópsia de Arthur",
        tipo: "Laudo Médico-Legal",
        localColeta: "Necrotério Central — DCE",
        dataColeta: "14/10/2026 — 03:30",
        peritoResponsavel: "Dra. Maya Navarro (Médica Legista)",
        statusCustodia: "Laudo Provisório Emitido",
        resumo: "Cessação instantânea das funções vitais sem traumatismos ósseos ou toxinas comuns detectadas nos exames preliminares."
    },
    {
        id: "EVID-04",
        protocolo: "DCE-PERICIA-004/26",
        nome: "Concessão Cadastral do Depósito 217",
        tipo: "Registro Público / Documento",
        localColeta: "Cartório Notarial de Blackwood",
        dataColeta: "14/10/2026 — 03:50",
        peritoResponsavel: "Detetive Helena Voss",
        statusCustodia: "Anexado aos Autos do Caso 001",
        resumo: "Documentação de concessão do galpão industrial no terminal ferroviário abandonado há 15 anos."
    }
];

export const dadosTimeline = [
    {
        ano: "1891",
        evento: "Três pessoas desaparecem em Blackwood sob o mesmo símbolo.",
        categoria: "Histórico Canônico",
        status: "Símbolo Registrado"
    },
    {
        ano: "1924",
        evento: "Sete pessoas desaparecidas sem pistas em circunstâncias anômalas.",
        categoria: "Histórico Canônico",
        status: "Ciclo Identificado"
    },
    {
        ano: "1958",
        evento: "Quatro pessoas desaparecidas na mesma área urbana.",
        categoria: "Histórico Canônico",
        status: "Ciclo Identificado"
    },
    {
        ano: "1989",
        evento: "Instituto Ardens: Encerramento repentino das atividades da instituição de pesquisa privada.",
        categoria: "Instituição Oculta",
        status: "Conexão Canônica"
    },
    {
        ano: "2008",
        evento: "Ocorrência misteriosa na Rua Alderbrook associada ao mesmo símbolo.",
        categoria: "Histórico Recente",
        status: "Arquivo Aberto"
    },
    {
        ano: "2026 (Hoje — 02:17)",
        evento: "Morte de Arthur Vasconcelos no Apartamento 504. Início da investigação da DCE com Milena Ramires.",
        categoria: "Investigação Ativa",
        status: "Em Andamento"
    }
];

export const dadosConexoes = {
    nos: [
        { id: "n-arthur", rotulo: "Arthur Vasconcelos", tipo: "VÍTIMA", x: 260, y: 180 },
        { id: "n-apto", rotulo: "Apartamento 504", tipo: "CENA DE CRIME", x: 90, y: 70 },
        { id: "n-0217", rotulo: "Relógios 02:17", tipo: "ANOMALIA", x: 430, y: 70 },
        { id: "n-simbolo", rotulo: "Símbolo Histórico", tipo: "GLIFO OCULTO", x: 260, y: 320 },
        { id: "n-ardens", rotulo: "Instituto Ardens (1989)", tipo: "INSTITUIÇÃO", x: 90, y: 410 },
        { id: "n-deposito", rotulo: "Depósito 217", tipo: "PRÓXIMO DESTINO", x: 440, y: 390 },
        { id: "n-evelyn", rotulo: "Evelyn Cross / Sociedade", tipo: "PODER OCULTO", x: 260, y: 460 }
    ],
    arestas: [
        { de: "n-arthur", para: "n-apto", rotulo: "Local do Óbito" },
        { de: "n-arthur", para: "n-0217", rotulo: "Hora Paralisada" },
        { de: "n-arthur", para: "n-simbolo", rotulo: "Compilava Pesquisa" },
        { de: "n-simbolo", para: "n-ardens", rotulo: "Marco de 1989" },
        { de: "n-simbolo", para: "n-deposito", rotulo: "Destino da DCE" },
        { de: "n-simbolo", para: "n-evelyn", rotulo: "O Véu Oculto" }
    ]
};
