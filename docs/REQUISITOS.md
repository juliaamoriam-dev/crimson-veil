# Especificação de Requisitos — Crimson Veil

> **Fonte de Referência:** [conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md](file:///d:/CrimsonVeil/conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md) (Versão 1.0).

Este documento formaliza os requisitos funcionais, narrativos, de persistência e não funcionais do Crimson Veil, tratando os elementos como **conceitos do domínio** de negócio antes de qualquer conversão para modelos físicos de dados.

---

## 1. Requisitos Funcionais (RF)

### 1.1. Gerenciamento de Campanhas
* **RF-01.1:** O sistema deve permitir criar, carregar e alternar entre campanhas persistentes de um usuário.
* **RF-01.2:** Cada campanha deve manter seu próprio estado isolado, histórico e inventário narrativo.

### 1.2. Protagonista e Personagens
* **RF-02.1:** O sistema deve permitir a configuração da protagonista da campanha (atualmente detetive **Milena Ramires**).
* **RF-02.2:** O sistema deve gerenciar fichas conceituais de NPCs (nome, idade, cargo, personalidade, objetivos, medos, rotina, histórico).
* **RF-02.3:** O sistema deve rastrear estados de relacionamento entre NPCs e a protagonista (confiança, amizade, respeito, rivalidade, desconfiança).

### 1.3. Episódios e Cenas
* **RF-03.1:** O sistema deve estruturar a campanha no formato episódico de séries policiais: Temporada, Episódio e Título.
* **RF-03.2:** A progressão de cada episódio deve obedecer ao ciclo: Abertura → Cena → Investigação → Desenvolvimento → Consequências → Encerramento.
* **RF-03.3:** Toda cena gerada pelo narrador/IA deve conter: localização, horário, descrição ambiental observável, acontecimentos, diálogos, elementos investigáveis e devolução explícita do turno para o jogador agir.

### 1.4. Interação Narrativa e Diálogos
* **RF-04.1:** O sistema deve fornecer uma interface de diálogo e ação onde o jogador submete suas jogadas em texto livre.
* **RF-04.2:** O sistema deve registrar o histórico cronológico de interações e mensagens da cena atual.

### 1.5. Investigação, Casos, Pistas e Evidências
* **RF-05.1:** O sistema deve manter dossiês de casos policiais com status controlados (*aberto*, *resolvido*, *arquivado*, *falhou*).
* **RF-05.2:** Cada caso deve possuir internamente uma **verdade objetiva** (culpado, motivação, evidências necessárias, pistas falsas, testemunhas), inacessível ao jogador até ser desvendada.
* **RF-05.3:** O sistema deve gerenciar **evidências** com identidade persistente (ID, caso, local, data, horário, descrição, status, relações), mantendo sua validade entre episódios distintos.
* **RF-05.4:** O sistema deve gerenciar **pistas** como indícios observáveis (incluindo pistas falsas, equívocos e contradições de testemunhas), sem conclusões pré-mastigadas.
* **RF-05.5:** O sistema deve permitir a consulta a documentos, laudos, depoimentos e relatórios anexados ao inquérito.

### 1.6. Controle de Tempo e Linha do Tempo
* **RF-06.1:** O sistema deve manter um relógio contínuo e persistente que avança conforme a duração estimada de diálogos, deslocamentos e perícias.
* **RF-06.2:** O sistema deve alimentar um registro cronológico de eventos e marcos temporais da campanha.

### 1.7. Locais e Mapa
* **RF-07.1:** O sistema deve gerenciar o catálogo de locais visitáveis (endereços, ambientes, cenas de crime, delegacia, estabelecimentos).
* **RF-07.2:** O sistema deve registrar a localização corrente da protagonista no mundo.

---

## 2. Requisitos Narrativos (RNar)

Diretrizes obrigatórias de comportamento da inteligência artificial e da dinâmica de jogo:

* **RNar-01 — Agência Absoluta da Protagonista:** A protagonista pertence exclusivamente ao jogador. O narrador/IA **jamais** deve descrever sentimentos, pensamentos, falas, decisões, expressões corporais, intenções ou reações internas da protagonista.
* **RNar-02 — Continuidade Reativa:** Toda mensagem do jogador é considerada uma ação finalizada. A resposta da IA deve iniciar exatamente a partir do momento seguinte ao último caractere digitado, sem resumir, repetir ou reescrever a ação recém-executada.
* **RNar-03 — Estilo Cinematográfico e Visual:** A narração deve emular o ritmo de uma câmera de série televisiva, priorizando diálogos, sons, movimentos observáveis e silêncios, evitando textos excessivamente literários ou monólogos expositivos.
* **RNar-04 — Mundo Vivo e Acontecimentos Paralelos:** A cidade e os NPCs operam independentemente do jogador. Testemunhas podem sumir, provas podem degradar-se e colegas policiais tomam decisões por conta própria.
* **RNar-05 — Consequências Persistentes:** Erros de dedução ou atrasos não provocam *Game Over* sumário, mas geram ramificações orgânicas (perda de pistas, suspeitos foragidos, tensões na equipe).
* **RNar-06 — Investigação Não Linear:** Não há uma ordem única obrigatória de resolução; a verdade objetiva permanece oculta aguardando a interpretação legítima do jogador.

---

## 3. Arquitetura de Memória da IA

Para sustentar a continuidade sem alucinações e com uso eficiente de contexto, o sistema adota quatro níveis conceituais de memória:

```text
┌─────────────────────────────────────────────────────────────┐
│ 1. MEMÓRIA IMEDIATA                                         │
│    Últimas falas e ações imediatas da cena em andamento     │
├─────────────────────────────────────────────────────────────┤
│ 2. MEMÓRIA EPISÓDICA                                        │
│    Marcos, descobertas e acontecimentos do episódio atual   │
├─────────────────────────────────────────────────────────────┤
│ 3. MEMÓRIA DE CAMPANHA                                      │
│    Casos passados, decisões chave, estado das relações      │
├─────────────────────────────────────────────────────────────┤
│ 4. MEMÓRIA PERMANENTE                                       │
│    Leis, geografia, instituições e cânon fixo do universo   │
└─────────────────────────────────────────────────────────────┘
```

* **Princípio de Recuperação Seletiva:** A IA deve receber em seu prompt apenas o contexto relevante para a cena em questão (ex.: ao entrar em um armazém, priorizar o histórico daquele local, os presentes e as pistas associadas), evitando consumo desnecessário de contexto e contradições.

---

## 4. Estado do Mundo (`WORLD_STATE`)

O **`WORLD_STATE`** é o conceito central de persistência que representa o estado vivo da campanha em um dado instante.

### Atributos Conceituais Conhecidos:
* `current_date` (data no universo)
* `current_time` (horário no universo)
* `current_location` (localização ativa)
* `current_episode` (episódio em andamento)
* `active_case` (caso sob investigação primária)
* `characters_present` (NPCs no ambiente)
* `known_clues` (pistas descobertas até o momento)
* `known_evidence` (evidências catalogadas)
* `relationships` (estado das ligações interpessoais)
* `open_questions` (hipóteses e perguntas em aberto)
* `world_events` (acontecimentos externos em curso)

*Estrutura técnica de serialização, esquema relacional e armazenamento:* `A DEFINIR`.

---

## 5. Requisitos de Persistência e Segurança Narrativa (RP)

* **RP-01 — Persistência Integral de Domínio:** Dados de tempo, estado de casos, pistas, evidências e árvores relacionais devem persistir entre sessões sem degradação.
* **RP-02 — Preservação de Evidências:** Nenhuma evidência é excluída sem justificativa lógica da narrativa (ex.: furto, destruição pericial documentada).
* **RP-03 — Segurança Narrativa:** O sistema não pode modificar fatos já estabelecidos de forma oculta. Em caso de discrepância, deve prevalecer o fato canônico mais recente e explícito, ou a divergência deve ser incorporada como mistério investigativo.

---

## 6. Requisitos Não Funcionais (RNF)

* **RNF-01 — Consistência de Dados e Narrativa:** Garantia de sincronismo estrito entre as saídas textuais da IA e as mutações salvas no `WORLD_STATE`.
* **RNF-02 — Desempenho e Latência:** A composição do prompt e a recuperação de memória devem responder dentro de parâmetros aceitáveis para navegação web interativa (`A DEFINIR` métricas de SLA).
* **RNF-03 — Manutenibilidade e Separação de Camadas:** Isolamento rigoroso entre a Camada 1 (Experiência/Interface) e a Camada 2 (Sistema/Persistência).
* **RNF-04 — Segurança e Isolamento:** Cada usuário/campanha deve possuir dados estritamente isolados sem vazamento entre instâncias (`A DEFINIR` mecânica de autenticação).
* **RNF-05 — Escalabilidade:** Capacidade futura de suportar múltiplos episódios e campanhas volumosas sem degradação de histórico.
* **RNF-06 — Usabilidade e Acessibilidade:** Interface focada em legibilidade e imersão visual, compatível com navegadores modernos (`A DEFINIR`).
