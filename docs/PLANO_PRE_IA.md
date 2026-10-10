# Plano de Proteção, Fundação e Preparação Pré-IA — Crimson Veil

> **Status:** Registro histórico do planejamento anterior ao Marco 1.  
> **Data:** Outubro de 2026  
> **Autor:** Engenharia de Software & Game Architecture  
> **Orçamento:** R$ 0,00 (Obrigatório)  

---

> **Atualização do Marco 1:** A auditoria e as propostas abaixo registram o estado do projeto antes da implementação do backend e não descrevem mais o estado atual. O backend Spring Boot, a API local, o SQLite e a integração com o frontend estão implementados. A arquitetura vigente, o esquema e a execução estão em [docs/ARQUITETURA.md](./ARQUITETURA.md), [docs/BANCO_DE_DADOS.md](./BANCO_DE_DADOS.md) e [docs/GUIA_EXECUCAO_LOCAL.md](./GUIA_EXECUCAO_LOCAL.md). Propostas antigas de `localStorage`, PostgreSQL e Flyway foram substituídas para este marco.

## 1. Estado Atual Confirmado do Projeto

### 1.1. Inspeção de Controle de Versão (Git)
* **Branch Ativa:** `main` (sincronizada com `origin/main`).
* **Status do Working Tree:** Limpo (`working tree clean`).
* **Histórico de Commits Recentes:**
  * `beb0297` — *docs: melhora README do projeto* (07/10/2026)
  * `66d5a32` — *feat: atualiza projeto Crimson Veil* (07/10/2026)
* **Alterações Locais:** Nenhuma alteração pendente descartada; integridade total preservada.

### 1.2. Inventário de Arquivos e Tecnologias Ativas
* **Frontend:**
  * `frontend/index.html` (246 linhas) — Casca HTML semântica contendo Tela de Login, Header da DCE, Barra Lateral com 9 abas de contexto, Área Principal de Conteúdo, Modal de Detalhes e Fluxo de Criação de Personagem.
  * `frontend/css/estilo.css` (2.704 linhas, ~61 KB) — Folha de estilos centralizada com tema noir dark.
  * `frontend/js/app.js` (2.556 linhas, ~122 KB) — Controlador monolítico em JavaScript moderno (ES6 Modules) responsável pela orquestração de campanhas, renderização dinâmica de todas as abas, simulador de mundo reativo e criador de personagens.
  * `frontend/js/mocks/dadosIniciais.js` (386 linhas, ~17 KB) — Base de dados em memória contendo entidades do Caso #001, personagens, locais, pistas e conexões canônicas.
* **Backend:**
  * `backend/.gitkeep` — Não há código implementado no backend atualmente. Toda a lógica operacional e o ciclo de vida do jogo rodam exclusivamente no cliente (navegador).
* **Base de Conhecimento e Documentação:**
  * `conhecimento/` (29 documentos markdown e 4 infográficos de alta fidelidade): Universo, regras de agência, fichas de personagens, locais canônicos, inquérito policial e o documento-mestre `MASTER_PROMPT_V1.md`.
  * `docs/` (7 documentos de engenharia): Arquitetura, Modelo de Domínio, Banco de Dados, Requisitos, Visão, Produto e Roadmap.
* **Ambiente de Execução Local Verificado:**
  * Node.js `v24.21.0` (disponível para execução de testes e utilitários).
  * OpenJDK Java 21 LTS (disponível para futura API Spring Boot, conforme documentação).
  * Git `2.47.0.windows.1`.

---

## 2. Ponto de Restauração e Backup

Para assegurar risco zero de perda de dados ou regressão durante as próximas etapas, foram estabelecidos dois mecanismos independentes de restauração:

1. **Tag de Snapshot no Git:**
   * **Tag:** `backup-fundacao-v1.0`
   * **Apontamento:** Commit canônico `beb0297`
   * **Comando para restauração rápida:**
     ```bash
     git checkout backup-fundacao-v1.0
     ```

2. **Arquivo de Backup Físico Compactado:**
   * **Arquivo:** `backup-crimson-veil-etapa1.zip` (3.048.820 bytes)
   * **Conteúdo:** Cópia idêntica e completa de todos os 53 arquivos rastreados pelo repositório (código, documentações e assets binários originais).
   * **Configuração de Segurança:** Adicionada a regra `*.zip`, `*.bundle` e `*.tar.gz` ao [.gitignore](file:///d:/CrimsonVeil/.gitignore) para que backups locais nunca sejam acidentalmente versionados ou sobrescrevam o repositório remoto.

---

## 3. Auditoria de Funcionalidades Existentes (O Que Funciona Hoje)

A validação de sintaxe (`node --check`) e a análise de execução do frontend confirmam:

| Funcionalidade | Status Atual | Detalhes Técnicos |
| :--- | :--- | :--- |
| **Tela de Autenticação Policial** | Operacional | Formulário estilizado que alterna visualização de `#tela-login` para `#tela-sistema`. |
| **Navegação Modular por Abas** | Operacional | Event listeners nos elementos `.nav-item` alternando dinamicamente entre Visão Geral, Investigação, Casos, Pistas, Evidências, Pessoas, Locais, Timeline, Conexões e Artes. |
| **Terminal de Ações e Histórico** | Operacional (Simulado) | Captura de entrada do usuário via Enter/Clique, inserção no array `mensagensCena` e resposta após timeout de 600ms via busca de palavras-chave. |
| **Criador de Personagens (4 Passos)** | Operacional | Modal em etapas: 1. Identidade (nome, idade, distintivo), 2. Origem policial com traços, 3. Retrato SVG gerado, 4. Confirmação e geração de nova campanha isolada. |
| **Isolamento de Campanhas em Memória** | Operacional (Volátil) | Objeto `gerenciadorCampanhas` cria instâncias separadas com clones de dados para evitar vazamento entre campanhas. |
| **Cards e Dossiers de Investigação** | Operacional | Renderização correta dos dados canônicos mockados em cada aba temática. |
| **Regra de Agência da Protagonista** | Respeitada | O narrador simulado não gera pensamentos, emoções ou ações para Milena Ramires. |

---

## 4. Hierarquia das Fontes Oficiais e Arquivos Canônicos

Para impedir inconsistências narrativas e conflitos de regras, estabelece-se a seguinte **hierarquia estrita de verdade**:

```text
[Nível 1] Cânone Primário Absoluto
└── conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md
└── conhecimento/05-documentos/referencias/assets/ (Infográficos Oficiais)

[Nível 2] Regras de Agência e Identidade
└── conhecimento/01-universo/regras/regras-narrativas-e-agencia.md
└── conhecimento/02-personagens/principais/milena-ramires.md

[Nível 3] Lore do Universo, Cronologia e NPCs
└── conhecimento/01-universo/lore/universo-crimson-veil.md
└── conhecimento/01-universo/cronologia/linha-historica-canonica.md
└── conhecimento/02-personagens/npcs/equipe-delegacia.md
└── conhecimento/03-mundo/locais/ e instituicoes/

[Nível 4] Arquitetura e Engenharia de Software
└── docs/MODELO_DE_DOMINIO.md
└── docs/ARQUITETURA.md
└── docs/BANCO_DE_DADOS.md

[Nível 5] Implementação em Código e Mocks
└── frontend/js/mocks/dadosIniciais.js
└── frontend/js/app.js
```

### Regras Mandatórias de Cânone:
1. **Milena Pertence Exclusivamente à Jogadora:** Nenhuma automação, IA ou regra de sistema pode gerar sentimentos, pensamentos, falas ou ações para a protagonista.
2. **Datas Canônicas Invioláveis:** Desaparecimentos históricos em 1891, 1924, 1958; encerramento do Instituto Ardens em 1989; evento da Rua Alderbrook em 2008; Morte de Arthur Vasconcelos em 2026.
3. **Equipe da DCE:** Adrian (Capitão, líder controlado, humor seco), Helena (Detetive racional e direta), Noah (Cibercrime, humor orgânico), Maya (Legista, humor negro), Iris (Psicologia criminal). Nenhuma personalidade pode ser distorcida.

---

## 5. Problemas Críticos Identificados Antes de Integrar a IA

Antes de conectar qualquer modelo de linguagem, os seguintes gargalos estruturais **precisam** ser solucionados no motor do jogo:

### 5.1. Volatilidade e Inexistência de Persistência (Risco Crítico)
* **Diagnóstico:** O código em `app.js` não usa `localStorage` nem `IndexedDB`.
* **Impacto:** Qualquer recarregamento de página (F5), queda de energia ou fechamento de navegador apaga 100% da sessão, decisões, mensagens, pistas encontradas e campanhas criadas.
* **Solução Obrigatória:** Camada de persistência local versionada com salvamento contínuo e exportação/importação de arquivos `.json`.

### 5.2. Relógio Não Determinístico e Matemática Temporal Quebrada
* **Diagnóstico:** O horário avança na função `submeterAcao` através de `const minutos = 17 + (campAtiva.contadorAcoes * 5); campAtiva.estadoMundo.horarioAtual = '02:' + minutos;`.
* **Impacto:** Após 9 ações, o relógio exibe `02:62`, `02:67`, etc. Não existe suporte a virada de hora, mudança de dia, nem diferenciação entre uma ação rápida (conversa de 3 minutos) e uma ação longa (viagem de 45 minutos).
* **Solução Obrigatória:** Motor de tempo em minutos inteiros absolutos com conversor determinístico `minutosParaHorarioFormatado(minutos)` e tabela de custos temporais por tipo de ação.

### 5.3. Narrativa Sem Prólogo Humano (Início Súbito no Crime)
* **Diagnóstico:** A campanha canônica inicia arbitrariamente às 02:17 no Apartamento 504 diante do cadáver de Arthur.
* **Impacto:** A jogadora é arremessada no caso sem vivenciar o primeiro dia da protagonista na Divisão de Crimes Especiais, a dinâmica de rotina na delegacia, a apresentação aos colegas de equipe e o cotidiano policial.
* **Solução Obrigatória:** Estruturação da experiência em fases do Primeiro Dia:
  * Manhã/Tarde: Chegada à DCE, ambiente da delegacia, conversas cotidianas, café, ambientação e pequenas escolhas.
  * Noite/Madrugada: O chamado oficial do caso às 02:17, conferindo peso dramático e contexto à investigação.

### 5.4. Código Monolítico Sem Testes Automatizados
* **Diagnóstico:** `app.js` acumula 2.556 linhas com todas as responsabilidades misturadas e zero testes automatizados.
* **Impacto:** Tentar integrar a IA sobre um monólito sem testes gerará falhas imprevisíveis e regressões difíceis de rastrear.
* **Solução Obrigatória:** Criar suíte de testes com o test runner nativo do Node.js (`node --test`), cobrindo relógio, persistência e validações de regras.

---

## 6. Mapeamento de Arquivos a Serem Modificados

Para manter a intervenção segura, modular e incremental, o escopo de modificações é estritamente delimitado:

| Arquivo / Módulo | Natureza da Alteração | Motivação Técnica |
| :--- | :--- | :--- |
| `frontend/js/core/tempo.js` | **Novo Módulo** | Relógio determinístico, cálculo de minutos, transição de horas/dias e tabela de custo temporal de ações. |
| `frontend/js/core/persistencia.js` | **Novo Módulo** | Gerenciador de `localStorage` com versionamento (`v1.0`), auto-save e exportação/importação de backup. |
| `frontend/js/core/relacionamentos.js` | **Novo Módulo** | Matriz de relações (confiança, respeito, familiaridade, conflito) entre NPCs e a protagonista. |
| `frontend/js/mocks/dadosIniciais.js` | **Extensão Não Destrutiva** | Adicionar os dados canônicos da rotina do Primeiro Dia na DCE (08:30) sem apagar os dados do Caso #001. |
| `frontend/js/app.js` | **Integração Modular** | Conectar os módulos de tempo, persistência e relacionamentos, preservando as telas e interações existentes. |
| `test/tempo.test.js` e `test/persistencia.test.js` | **Novos Arquivos de Teste** | Testes automatizados executáveis com `node --test` para garantir comportamento reproduzível. |

---

## 7. Plano Incremental Pré-IA com Critérios de Aceitação

A evolução deve seguir a menor sequência lógica necessária:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      ETAPAS DE IMPLEMENTAÇÃO PRÉ-IA                    │
├────────────────────────────────────────────────────────────────────────┤
│ 1. Motor de Tempo Determinístico + Testes Automatizados                │
│ 2. Persistência Local (Storage + Backup JSON) + Testes                 │
│ 3. Matriz de Relacionamentos e Conhecimento dos NPCs                   │
│ 4. Estrutura Narrativa do Primeiro Dia (Prólogo + Chamado Noturno)     │
│ 5. Validação Integrada do Ciclo de Jogo Local (R$ 0)                   │
└────────────────────────────────────────────────────────────────────────┘
```

### Critérios de Aceitação por Etapa:

#### Etapa 1: Motor de Tempo Determinístico
* [ ] Função converte minutos para formato `HH:mm` e dias da semana sem nunca ultrapassar `59` minutos na exibição.
* [ ] Ações consomem intervalos coerentes (conversa rápida: 3 min; análise técnica: 15 min; viagem: 30 min).
* [ ] Testes unitários em `node --test` passam com 100% de sucesso.

#### Etapa 2: Persistência Local e Recuperação
* [ ] Ao recarregar a página (`F5`), o estado da campanha, histórico de ações e horário permanecem intactos.
* [ ] Botão de "Exportar Campanha" baixa um arquivo JSON estruturado.
* [ ] Botão de "Importar Campanha" restaura o jogo a partir de um JSON válido sem falhas no console.
* [ ] Ocorrência de corrupção no storage é tratada graciosamente com fallback para dados seguros.

#### Etapa 3: Relacionamentos e Conhecimento dos NPCs
* [ ] Cada NPC possui pontuações separadas para `confiança`, `respeito` e `familiaridade`.
* [ ] As escolhas e conversas da protagonista modificam esses valores de acordo com as regras estabelecidas.
* [ ] Um NPC não responde sobre fatos que não presenciou.

#### Etapa 4: Experiência do Primeiro Dia
* [ ] A narrativa começa pela manhã na Divisão de Crimes Especiais (DCE).
* [ ] É possível conversar sobre assuntos cotidianos (café, equipamentos, cidade, trabalho policial).
* [ ] O caso policial surge de maneira orgânica como consequência do tempo e dos eventos, sem ser forçado no primeiro segundo.

---

## 8. Matriz de Riscos e Procedimentos de Recuperação

| Risco | Severidade | Gatilho Potencial | Procedimento Imediato de Recuperação |
| :--- | :--- | :--- | :--- |
| **Corrupção de Dados no Navegador** | Alta | Falha na serialização do `localStorage` | O módulo de persistência validará o schema JSON antes de carregar; em caso de falha, recupera o último snapshot válido ou carrega os dados canônicos iniciais. |
| **Inconsistência de Fatos Canônicos** | Crítica | Adicionar informações inventadas sobre NPCs ou organizações | Conferência obrigatória com os arquivos em `conhecimento/` e `MASTER_PROMPT_V1.md` antes de qualquer alteração de texto. |
| **Quebra de Telas Existentes** | Média | Alterações na orquestração de abas em `app.js` | Execução contínua dos testes automatizados e verificação no navegador; rollback via `git checkout frontend/js/app.js` se necessário. |
| **Custos Involuntários de API** | Crítica | Dependência de chamadas pagas na inicialização | Toda a fundação funciona estritamente em ambiente local e determinístico com orçamento R$ 0. |

---

## 9. Próximos Passos (Aguardando Aprovação)

Com a fundação protegida, o backup físico e a tag Git estabelecidos e este documento oficial gerado:

1. **Aguardar a aprovação da usuária** deste plano pré-IA.
2. Após o aval, iniciar a implementação da **Etapa 1: Motor de Tempo Determinístico e Suíte de Testes Nativos**.
