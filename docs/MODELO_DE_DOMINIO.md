# Modelo Conceitual do Domínio — Crimson Veil

> **Fontes de Referência:**
> * [conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md](file:///d:/CrimsonVeil/conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md) (Versão 1.0)
> * [docs/PRODUTO.md](file:///d:/CrimsonVeil/docs/PRODUTO.md)
> * [docs/REQUISITOS.md](file:///d:/CrimsonVeil/docs/REQUISITOS.md)

---

## 1. Objetivo

Este documento estabelece a modelagem conceitual do domínio do **Crimson Veil**, mapeando as entidades, objetos de valor (Value Objects), agregados, estados e fronteiras de contexto da aplicação.

Esta etapa antecede o desenho de banco de dados e a arquitetura técnica, visando responder:
* Quais conceitos possuem identidade própria e ciclo de vida independente (Entidades)?
* Quais conceitos são definidos apenas por seus atributos e imutabilidade (Value Objects)?
* Quais elementos pertencem ao **Universo Canônico** versus à **Instância de Campanha**?
* Como o sistema de memória e o `WORLD_STATE` se articulam conceitualmente?

---

## 2. Contextos Delimitados do Domínio (Bounded Contexts)

O sistema opera sobre a intersecção de dois grandes contextos:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                      CONTEXTO DO UNIVERSO / CÂNON                      │
│ (Lore permanente, Instituições, Locais base, Linha Histórica, Casos)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ instancia e referencia
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   CONTEXTO DA PLATAFORMA / CAMPANHA                    │
│ (Usuário, Campanha ativa, Cenas, Diálogos, WORLD_STATE, Memória viva)  │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.1. Contexto do Universo (Cânon / Conteúdo)
Representa o mundo ficcional imutável ou de referência que existe independentemente de uma partida específica:
* Instituições (ex.: Instituto Ardens, Delegacia);
* Locais canônicos da cidade (ex.: Depósito 217, Apartamento 504);
* Cronologia histórica (marcos de 1891, 1924, 1958, 1989, 2008, 2026);
* Fichas base de NPCs (personalidade arquetípica, histórico, cargos);
* Casos oficiais e suas verdades objetivas ocultas.

### 2.2. Contexto da Plataforma (Execução / SaaS)
Representa o motor computacional que hospeda a experiência interativa do jogador:
* Conta do usuário e preferências;
* Campanha instanciada e progresso;
* Execução em tempo real de episódios e cenas;
* Estado momentâneo do mundo (`WORLD_STATE`);
* Memória episódica e de campanha;
* Registro dos diálogos e decisões da jogadora.

---

## 3. Avaliação e Classificação dos Conceitos de Domínio

Com base nos termos identificados nas fontes oficiais (notadamente a Seção 34 do Master Prompt), cada conceito foi analisado e classificado:

| Conceito | Contexto | Classificação Conceitual | Justificativa |
| :--- | :--- | :--- | :--- |
| **USER** | Plataforma | **Entidade** | Possui identidade única, credenciais e é titular das campanhas. |
| **CAMPAIGN** | Plataforma | **Entidade (Raiz de Agregado)** | Representa uma jornada isolada com sua própria continuidade e estado. |
| **SEASON** | Conteúdo / Narrativa | **Entidade Organizadora** | Agrupa episódios de um grande arco narrativo. |
| **EPISODE** | Conteúdo / Narrativa | **Entidade** | Unidade estrutural da série policial com ciclo de vida (início, meio, desfecho). |
| **SCENE** | Narrativa / Execução | **Entidade** | Unidade de tempo, espaço e ação onde a interação ocorre. |
| **CHARACTER** | Universo & Campanha | **Entidade** | Indivíduo (Protagonista ou NPC) com identidade, atributos e autonomia. |
| **LOCATION** | Universo & Campanha | **Entidade** | Espaço físico no mundo com identidade e características ambientais. |
| **CASE** | Universo & Campanha | **Entidade** | Inquérito investigativo com status, verdade interna e dossiê associado. |
| **CLUE** | Investigação | **Entidade / Conteúdo** | Indício ou apontamento descoberto que direciona hipóteses (pode ser falso). |
| **EVIDENCE** | Investigação | **Entidade** | Elemento físico/pericial comprobatório com registro e persistência perene. |
| **RELATIONSHIP** | Campanha | **Entidade Associativa** | Liga dois personagens registrando confiança, respeito e histórico de conflitos. |
| **EVENT** | Campanha / Mundo | **Entidade / Evento de Domínio** | Acontecimento objetivo que modifica fatos no mundo ou na delegacia. |
| **TIMELINE_EVENT** | Investigação | **Projeção / Value Object** | Representação cronológica datada de um fato ocorrido na linha temporal. |
| **CONVERSATION** | Narrativa | **Agregado / Coleção de Mensagens** | A sequência de falas e ações trocadas durante uma cena específica. |
| **WORLD_STATE** | Execução / Sistema | **Estado Consolidado (Snapshot)** | Fotografia das variáveis ativas da campanha em um dado instante. |

---

## 4. Detalhamento das Entidades Candidatas

### 4.1. Usuário (`Usuario` / `User`)
* **Responsabilidade:** Representa o operador ou titular da conta na plataforma web.
* **Identidade:** Identificador único global do usuário.
* **Principais Atributos:** Identificador, nome de exibição, data de cadastro (`A DEFINIR` credenciais e autenticação).
* **Relacionamentos:** Possui 1 ou mais Campanhas (`CAMPAIGN`).
* **Dependências:** Nenhuma do domínio narrativo.
* **Persistência:** Obrigatória (garantir acesso à conta e às campanhas salvas).

### 4.2. Campanha (`Campanha` / `Campaign`)
* **Responsabilidade:** Raiz de agregado que isola a continuidade narrativa de uma jogadora. Contém todo o histórico e estado do mundo.
* **Identidade:** Identificador único da campanha.
* **Principais Atributos:** Título da campanha, data de criação, data de última sessão, status (ativa, pausada, concluída).
* **Relacionamentos:** Pertence a um Usuário; possui uma Protagonista (`CHARACTER`); possui um `WORLD_STATE` ativo; possui múltiplos Episódios (`EPISODE`); possui casos e evidências conhecidas.
* **Dependências:** `User`.
* **Persistência:** Obrigatória (núcleo da retenção da plataforma).

### 4.3. Temporada (`Temporada` / `Season`)
* **Responsabilidade:** Agrupador narrativo dos episódios que compõem um grande arco temporal ou caso central.
* **Identidade:** Número da temporada e identificador.
* **Principais Atributos:** Número da temporada, título do arco, sinopse geral.
* **Relacionamentos:** Pertence ao catálogo canônico; agrupa múltiplos Episódios.
* **Persistência:** Canônica no catálogo.

### 4.4. Episódio (`Episodio` / `Episode`)
* **Responsabilidade:** Unidade episódica que emula um episódio de série policial de TV.
* **Identidade:** Identificador único do episódio dentro da temporada.
* **Principais Atributos:** Número, título, caso principal associado, estado de progressão (não iniciado, em andamento, concluído).
* **Estrutura interna:** Abertura → Cena → Investigação → Desenvolvimento → Consequências → Encerramento.
* **Relacionamentos:** Pertence a uma Temporada; executado no contexto de uma Campanha; contém uma sequência de Cenas (`SCENE`).
* **Persistência:** Obrigatória.

### 4.5. Cena (`Cena` / `Scene`)
* **Responsabilidade:** Unidade dramática e operacional da investigação. Delimita o momento e espaço onde o jogador toma decisões.
* **Identidade:** Identificador único da cena dentro do episódio.
* **Principais Atributos:** Localização atual, horário de início, horário de término, descrição ambiental observável, personagens presentes, elementos investigáveis disponíveis, status do turno.
* **Relacionamentos:** Pertence a um Episódio; ocorre em um Local (`LOCATION`); envolve múltiplos Personagens (`CHARACTER`); contém a Conversa/Ações (`CONVERSATION`); gera alterações no `WORLD_STATE`.
* **Persistência:** Obrigatória (histórico de execução da sessão).

### 4.6. Personagem (`Personagem` / `Character`)
* **Responsabilidade:** Indivíduo com agência no universo (Protagonista ou NPCs).
* **Identidade:** Identificador único do personagem.
* **Principais Atributos:**
  * *Canônicos:* Nome, idade, profissão/cargo, arquétipo, biografia base, rotina padrão.
  * *Específicos de Campanha:* Status (vivo, ferido, desaparecido, foragido, detido), opiniões ativas, localização corrente.
* **Regra Especial:** Se o personagem for a Protagonista (ex.: Milena Ramires), suas falas, pensamentos, decisões e sentimentos pertencem **exclusivamente ao jogador**, sendo vetado ao sistema controlá-la.
* **Relacionamentos:** Participa de Cenas; mantém Relacionamentos com outros Personagens; pode estar vinculado a Casos como suspeito, testemunha ou investigador.
* **Persistência:** Canônica (ficha base) e de Campanha (estado dinâmico).

### 4.7. Local (`Local` / `Location`)
* **Responsabilidade:** Espaço geográfico e ambiental visitável (delegacia, apartamento, armazém, terminal ferroviário).
* **Identidade:** Identificador único do local.
* **Principais Atributos:** Nome, endereço/região, tipo de ambiente, histórico prévio, estado de preservação (ex.: cena de crime isolada, local desativado).
* **Relacionamentos:** É palco de Cenas e Eventos; abriga Evidências e Pistas.
* **Persistência:** Canônica (catálogo geográfico) e de Campanha (estado de alteração pelo jogador).

### 4.8. Caso (`Caso` / `Case`)
* **Responsabilidade:** Dossiê investigativo de um delito ou mistério a ser desvendado.
* **Identidade:** Identificador único do inquérito.
* **Principais Atributos:**
  * Título e código do caso;
  * Status: `aberto`, `resolvido`, `arquivado`, `falhou`;
  * **Verdade Objetiva (Secreta):** Culpado verdadeiro, motivação real, conjunto probatório necessário, armadilhas/pistas falsas, lista de testemunhas chave;
* **Relacionamentos:** Associado a um Episódio; contém Pistas descobertas; contém Evidências vinculadas; envolve múltiplos Personagens (vítimas, suspeitos, testemunhas).
* **Persistência:** Obrigatória.

### 4.9. Evidência (`Evidencia` / `Evidence`)
* **Responsabilidade:** Elemento material, físico, digital ou pericial formalmente coletado e custodiado.
* **Identidade:** Identificador único da evidência.
* **Principais Atributos:** Descrição objetiva observável, local de apreensão, data/hora da coleta, status de análise pericial, integridade física.
* **Relacionamentos:** Pertence a um Caso; coletada em um Local ou de um Personagem; pode se relacionar com outras evidências e pistas.
* **Regra de Domínio:** Nunca é excluída sem evento justificado na história (princípio de preservação probatória inter-episódios).
* **Persistência:** Obrigatória.

### 4.10. Pista (`Pista` / `Clue`)
* **Responsabilidade:** Indício perceptível, informação verbal, fragmento documental ou anotação investigativa.
* **Identidade:** Identificador único da pista.
* **Principais Atributos:** Texto descritivo observável (sem conclusões mastigadas), autenticidade (`verdadeira`, `falsa/engenhada`, `inconclusiva`), status de apuração.
* **Relacionamentos:** Vinculada a um Caso e a um Local ou Personagem de origem; pode apontar para uma Evidência.
* **Persistência:** Obrigatória.

### 4.11. Relacionamento (`Relacionamento` / `Relationship`)
* **Responsabilidade:** Representa o laço interpessoal e o histórico mútuo entre dois personagens (notadamente entre a Protagonista e cada NPC).
* **Identidade:** Par de personagens (`Personagem_A`, `Personagem_B`) dentro de uma Campanha.
* **Principais Atributos:** Grau de confiança, respeito profissional, rivalidade, afinidade pessoal, histórico resumido de atritos ou favores.
* **Persistência:** Obrigatória no contexto da Campanha.

### 4.12. Evento do Mundo (`Evento` / `Event`)
* **Responsabilidade:** Acontecimentos dinâmicos objetivos que ocorrem no mundo (com ou sem a presença do jogador).
* **Identidade:** Identificador do evento.
* **Principais Atributos:** Timestamp no universo, descrição do fato, personagens envolvidos, impacto no mundo (ex.: fuga de suspeito, interrupção de elevador, declaração oficial à imprensa).
* **Persistência:** Registrado na memória episódica e na timeline.

---

## 5. Value Objects (Objetos de Valor) Identificados

Conceitos que não necessitam de identidade própria no banco de dados, sendo caracterizados por seu valor e imutabilidade:

1. **`TimestampInvestigativo`:** Representação temporal no universo de Crimson Veil contendo data fictícia (ex.: 2026-10-14) e hora pontual (ex.: 02:17). Implementa operações de incremento de tempo conforme deslocamento e duração de ações.
2. **`TurnoInteracao`:** Um par indivisível contendo a ação/fala submetida pela jogadora e a subsequente resposta reativa da IA, com timestamp e duração.
3. **`VerdadeDoCaso`:** Estrutura imutável que encapsula o culpado, motivo e provas cabais de um caso policial.
4. **`StatusCasoEnum`:** Valores controlados: `ABERTO`, `RESOLVIDO`, `ARQUIVADO`, `FALHOU`.
5. **`NivelConfiancaEnum`:** Indicador interno de relacionamento: `HOSTIL`, `DESCONFIADO`, `NEUTRO`, `AMIGAVEL`, `LEAL`.

---

## 6. Mapeamento de Relacionamentos do Domínio

```text
Usuario (1) ─────────── (N) Campanha
                              │
                              ├── (1) ──────── (1) Personagem (Protagonista)
                              ├── (1) ──────── (1) WORLD_STATE
                              ├── (1) ──────── (N) Relacionamento
                              └── (1) ──────── (N) Episodio
                                                    │
                                                    ├── (1) ──── (1) Caso (Inquérito Principal)
                                                    │                 │
                                                    │                 ├── (1) ──── (N) Pista
                                                    │                 └── (1) ──── (N) Evidencia
                                                    │
                                                    └── (1) ──── (N) Cena
                                                                      │
                                                                      ├── (N) ──── (1) Local
                                                                      ├── (N) ──── (N) Personagem (Presentes)
                                                                      └── (1) ──── (1) Conversa (Turnos)
```

### Tabela de Relacionamentos Principais

| Origem | Destino | Cardinalidade | Significado | Obrigatoriedade |
| :--- | :--- | :--- | :--- | :--- |
| **Campanha** | **Usuário** | N : 1 | Toda campanha pertence a um jogador específico. | Obrigatório |
| **Campanha** | **Protagonista** | 1 : 1 | Cada campanha possui exatamente uma protagonista ativa. | Obrigatório |
| **Campanha** | **Episódio** | 1 : N | Uma campanha progride ao longo de múltiplos episódios. | Obrigatório |
| **Episódio** | **Cena** | 1 : N | Um episódio é desdobrado em uma sequência de cenas. | Obrigatório |
| **Cena** | **Local** | N : 1 | Toda cena ocorre em uma localização física específica. | Obrigatório |
| **Cena** | **Personagem** | N : N | Uma cena possui um conjunto observável de personagens presentes. | Opcional (cena pode estar vazia) |
| **Cena** | **WORLD_STATE** | N : 1 | O encerramento de cada cena atualiza o estado vivo do mundo. | Obrigatório |
| **Caso** | **Pista** | 1 : N | Um caso congrega um rol de pistas sob investigação. | Opcional (início do inquérito) |
| **Caso** | **Evidência** | 1 : N | Um caso reúne materiais probatórios anexados aos autos. | Opcional |
| **Personagem** | **Personagem** | N : N | Interações entre indivíduos geram um registro de Relacionamento. | Opcional |

---

## 7. Universo Canônico vs. Instância de Campanha

Para permitir escalabilidade e múltiplas partidas sem furos na história, o domínio adota uma separação rigorosa entre **Cânon** e **Estado de Campanha**:

```text
┌─────────────────────────────────────────────────────────────┐
│                    CATÁLOGO CANÔNICO                        │
│  * Locais base da cidade (Depósito 217, Delegacia)          │
│  * Casos oficiais e suas verdades ocultas                   │
│  * Fichas arquetípicas dos NPCs e instituições              │
│  * Cronologia histórica (1891 a 2026)                       │
└──────────────────────────────┬──────────────────────────────┘
                               │ serve de molde para
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  INSTÂNCIA DA CAMPANHA                      │
│  * Pistas e evidências efetivamente descobertas             │
│  * Estado de vida e atitude dos NPCs em relação à jogadora  │
│  * Decisões tomadas e ramificações ocorridas                │
│  * Relógio vivo (data/hora atual) e WORLD_STATE ativo       │
└─────────────────────────────────────────────────────────────┘
```

* **Regra de Ouro:** O Catálogo Canônico nunca é modificado pela execução de uma campanha. A campanha consome o cânon como leitura e grava suas alterações em sua própria camada de estado isolada.

---

## 8. Conceituação do `WORLD_STATE`

O **`WORLD_STATE`** é o estado consolidado do mundo da campanha em um dado instante no tempo.

### 8.1. O que ele representa?
A fotografia operacional do "agora":
* Data e hora exatas no universo;
* Localização geográfica atual da protagonista;
* Episódio e cena em curso;
* Caso sob foco imediato;
* Lista de personagens presentes no mesmo ambiente;
* Resumo das pistas e evidências ativas na memória de trabalho;
* Perguntas investigativas em aberto e eventos mundiais simultâneos.

### 8.2. O que pode alterá-lo?
* Cada ação executada pela protagonista que envolva deslocamento, passagem de tempo, descoberta probatória ou alteração de ambiente;
* Eventos paralelos autônomos gerados pelo motor do mundo (ex.: chegada de reforço policial, fuga de suspeito).

### 8.3. Distinções Fundamentais
* **`WORLD_STATE` vs. Banco de Dados:** O banco armazena o histórico completo, logs, tabelas estruturadas e relacionamentos detalhados. O `WORLD_STATE` é o snapshot compilado do momento presente que serve de entrada rápida para o Mestre/IA.
* **`WORLD_STATE` vs. Sistema de Memória:** A memória engloba a recuperação contextual em quatro camadas temporais. O `WORLD_STATE` é apenas a variável de estado instantânea (o "painel de instrumentos").
* **`WORLD_STATE` vs. Linha do Tempo (Timeline):** A Timeline é o registro cronológico retrospectivo de todos os marcos passados. O `WORLD_STATE` é a agulha no ponteiro do presente.

---

## 9. Sistema de Memória em Quatro Níveis

O modelo de memória organiza a retenção e recuperação cognitiva da IA sem recorrer a termos técnicos prematuros:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 1. MEMÓRIA IMEDIATA                                                    │
│    * Conteúdo: Últimas falas, ações e reações da cena em andamento.    │
│    * Duração: Apenas o turno ativo da cena.                            │
│    * Mutabilidade: Altamente volátil.                                  │
├────────────────────────────────────────────────────────────────────────┤
│ 2. MEMÓRIA EPISÓDICA                                                   │
│    * Conteúdo: Descobertas, conversas chave e ganchos do episódio.     │
│    * Duração: Ao longo do episódio ativo.                              │
│    * Mutabilidade: Mutável (acumula fatos do episódio).                │
├────────────────────────────────────────────────────────────────────────┤
│ 3. MEMÓRIA DE CAMPANHA                                                 │
│    * Conteúdo: Casos concluídos, reputação, traumas e consequências.   │
│    * Duração: Toda a vida útil da campanha do jogador.                 │
│    * Mutabilidade: Cumulativa e persistente.                           │
├────────────────────────────────────────────────────────────────────────┤
│ 4. MEMÓRIA PERMANENTE (CÂNON)                                          │
│    * Conteúdo: História do universo, leis, instituições, verdades.    │
│    * Duração: Eterna / Global.                                         │
│    * Mutabilidade: Imutável.                                           │
└────────────────────────────────────────────────────────────────────────┘
```

* **Princípio de Recuperação Seletiva:** Quando a protagonista está em determinado local e situação, a IA deve receber somente as fatias de memória pertinentes àquela cena, reduzindo ruído e contradições.

---

## 10. Diferenciação: `SCENE`, `CONVERSATION` e `EVENT`

Para evitar proliferação desnecessária de entidades artificiais:

1. **`SCENE` (Cena):** É uma **Entidade** central. Representa a unidade dramática delimitada por tempo, espaço e participantes.
2. **`CONVERSATION` (Conversa):** Não é uma entidade autônoma independente; é o **Agregado de Mensagens/Turnos** contido dentro da Cena. Uma conversa não existe fora de sua respectiva cena.
3. **`EVENT` (Evento):** Representa acontecimentos objetivos (parada de relógios, quebra de elevador, fuga).
4. **`TIMELINE_EVENT` (Evento da Timeline):** É uma **Projeção / Visualização Cronológica** de eventos ocorridos ou descobertos, formatada para consulta na linha do tempo investigativa.

---

## 11. Diferenciação Investigativa: `CASE`, `CLUE` e `EVIDENCE`

A mecânica de investigação policial requer distinções semânticas precisas:

* **Caso (`CASE`):** O processo/inquérito como um todo. Possui um dossiê, um estado formal (`aberto`, `resolvido`, etc.) e uma verdade interna oculta (quem fez, como e por quê).
* **Pista (`CLUE`):** Um indício observável ou dedução em apuração (ex.: marcas de poeira na mesa, contradição no depoimento). Uma pista pode ser legítima, inconclusiva ou intencionalmente falsa.
* **Evidência (`EVIDENCE`):** Elemento físico pericial ou comprobatório apreendido (ex.: arma do crime, laudo toxicológico, gravação de câmera). Possui custódia oficial e validade perene entre episódios.
* **Hipótese:** Interpretação subjetiva construída pela jogadora ou pela equipe (pertence ao pensamento e dedução do jogador, não sendo uma verdade imposta pelo sistema).

---

## 12. Conceitos Descartados ou Reclassificados como Não-Entidades

Durante a modelagem, os seguintes conceitos foram avaliados e **não** constituirão entidades isoladas independentes:

* **`TIMELINE_EVENT`:** Descartado como entidade isolada; reclassificado como projeção/leitura cronológica derivada de `Eventos` e marcos de `Casos`.
* **`CONVERSATION`:** Descartado como tabela ou entidade externa; tratado como o histórico interno de mensagens e ações que compõem o ciclo de vida de uma `SCENE`.
* **`WORLD_STATE`:** Não é uma entidade relacional clássica comum; é o objeto de estado consolidado que representa o contexto ativo da `Campanha`.

---

## 13. Conceitos Ainda Indefinidos (`A DEFINIR`)

Permanecem marcados como `A DEFINIR` para deliberação posterior:

1. **Mecânica de Resolução de Ações:** Se haverá atributos quantitativos (dados, perícias numéricas) ou se o sistema adotará resolução 100% qualitativa/narrativa orientada por lógica de investigação.
2. **Serialização do `WORLD_STATE`:** Se o snapshot será gravado como documento semiestruturado (ex.: JSON) ou fragmentado em colunas relacionais.
3. **Mecanismo de Recuperação de Memória:** Se a busca contextual seletiva usará filtros relacionais estritos, vetores semânticos ou abordagem híbrida.
4. **Estrutura de Usuários e Autenticação:** Modelo de controle de acesso (login único, múltiplos perfis, papéis administrativos).

---

## 14. Decisões Consolidadas

1. A separação entre o **Cânon Global** (imutável) e a **Instância de Campanha** (mutável) é mandatória para garantir integridade e isolamento.
2. A regra de **Agência Absoluta da Protagonista** é um requisito central do modelo de domínio, refletindo-se na separação estrita entre ações da jogadora e respostas do narrador.
3. Pistas e Evidências são entidades distintas: pistas são indícios (podendo ser falsas), enquanto evidências são provas materiais custodiadas de persistência permanente.

---

## 15. Questões Submetidas à Aprovação da Criadora

1. **Resolução de Ações:** O Crimson Veil terá fichas com atributos e testes (ex.: perícias, rolagem de dados/sorte) ou a condução será puramente orientada pela coerência narrativa e investigação dedutiva?
2. **Ciclo de Vida de Episódios:** Um episódio deve ser concluído obrigatoriamente para liberar o seguinte, ou a jogadora pode transitar livremente entre episódios e casos arquivados da mesma temporada?
3. **Escopo do Cânon:** Os NPCs oficiais (colegas de delegacia) terão suas personalidades e biografias iniciais cadastradas em arquivos de texto no repositório (`conhecimento/02-personagens/`) para servir de base ao catálogo canônico?
