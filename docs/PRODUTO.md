# Definição do Produto — Crimson Veil

> **Fonte de Referência:** [conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md](file:///d:/CrimsonVeil/conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md) (Versão 1.0).

---

## 1. O que é o Crimson Veil

O **Crimson Veil** é uma plataforma web interativa (Micro SaaS) que combina:

* **Universo Narrativo:** Um mundo fictício de investigação policial, mistério, drama e conspiração histórica;
* **Experiência de RPG:** Interpretação de papéis onde o jogador assume o controle exclusivo da protagonista, enquanto o sistema/IA atua como Mestre de RPG e diretor de cena;
* **Sistema Investigativo:** Coleta, cruzamento e interpretação de pistas, evidências, laudos e testemunhos sem soluções artificiais ou lineares;
* **Micro SaaS / Site Interativo:** Uma aplicação web com persistência de campanhas, gerenciamento de mundo e interface imersiva.

Inspirado em séries policiais consagradas (*Brooklyn Nine-Nine*, *The Rookie*, *Castle*, *Criminal Minds* e *True Detective*), o produto proporciona a sensação de vivenciar uma **série policial interativa episódica**, combinando casos independentes e um mistério maior em segundo plano.

---

## 2. Camadas do Sistema

O Crimson Veil opera fundamentalmente em duas camadas integradas:

```text
┌─────────────────────────────────────────────────────────────┐
│                 CAMADA 1: EXPERIÊNCIA                       │
│  (Interface do Jogador: Cenas, Diálogos, Ações, UI)         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                  CAMADA 2: SISTEMA                          │
│  (Memória em 4 níveis, WORLD_STATE, Tempo, Persistência)    │
└─────────────────────────────────────────────────────────────┘
```

### Camada 1 — Experiência
É a camada visível ao jogador, composta por:
* Cenas cinematográficas estruturadas;
* Diálogos dinâmicos e reações observáveis de NPCs;
* Campo de ação e tomadas de decisão da protagonista;
* Interfaces de investigação e consulta de informações;
* Painel de controle da narrativa.

### Camada 2 — Sistema
É o motor invisível que sustenta a coerência e a continuidade do universo:
* **Banco de Memória Narrativa:** Hierarquia de memória que evita contradições e esquecimentos;
* **Estado do Mundo (`WORLD_STATE`):** Registro persistente do contexto em tempo real;
* **Controle Temporal:** Avanço persistente de horário e datas;
* **Bancos de Domínio:** Casos, pessoas/NPCs, locais, evidências e pistas;
* **Motor de Consequências:** Rastreamento de desdobramentos de curto e longo prazo.

---

## 3. Experiência do Usuário

O objetivo central é colocar o jogador dentro de uma série policial viva, equilibrando:

* **Investigação e Descoberta:** Pistas mostram fatos observáveis (e não deduções prontas); pistas falsas e ambiguidades fazem parte da rotina;
* **Suspense e Mistério:** Casos complexos e conspirações históricas que avançam progressivamente;
* **Drama e Relações:** Dinâmica profissional e interpessoal de longo prazo entre os membros da equipe e testemunhas;
* **Humor Natural de Delegacia:** Interações espontâneas, provocações e rotina cotidiana (com respeito absoluto em cenas de sofrimento de vítimas);
* **Continuidade Implacável:** Fatos passados não são apagados; o mundo não espera pelo jogador e continua operando.

---

## 4. Sistemas Principais (Módulos da Aplicação)

Conforme definido no Master Prompt, o sistema é projetado com os seguintes módulos conceituais:

### 4.1. Dashboard
Painel de visão geral da investigação ativa:
* Campanha e episódio atual;
* Localização e horário correntes;
* Casos ativos;
* Pistas recentes e notificações de mundo.

### 4.2. Tela de RPG (Interface Principal de Narrativa)
Espaço central de interação com a história:
* Apresentação da cena e acontecimentos;
* Histórico recente de diálogos e interações;
* Campo de texto para envio de ações e falas da protagonista;
* Indicadores contextuais: localização, horário e personagens presentes na cena.

### 4.3. Módulo de Investigação
Painel de trabalho investigativo:
* **Casos:** Detalhamento do dossiê, status (aberto, resolvido, arquivado, falhou) e histórico;
* **Suspeitos e Pessoas:** Fichas de indivíduos com informações observadas e vínculos;
* **Evidências:** Acervo de provas físicas, laudos e materiais apreendidos persistentes entre episódios;
* **Pistas:** Fatos, anotações e hipóteses sob apuração;
* **Documentos:** Transcrições, registros históricos e arquivos oficiais.

### 4.4. Banco de Personagens
Catálogo dos indivíduos do universo:
* Nome, idade, cargo, postura e histórico conhecido;
* Acompanhamento de relacionamentos (confiança, respeito, conflitos).

### 4.5. Mapa
Visualização territorial do universo de Crimson Veil:
* Cidade, delegacia, locais de crime, residências e pontos sob vigilância;
* Apoio ao deslocamento com impacto na linha do tempo.

### 4.6. Linha do Tempo (Timeline)
Visualização cronológica dos acontecimentos:
* Ordenação de eventos passados e recentes do caso e da campanha;
* Cruzamento de horários e depoimentos.

### 4.7. Sistema de Memória e Continuidade
Mecanismo de recuperação seletiva de contexto para apoiar o narrador/IA na manutenção da coerência da cena.

---

## 5. O que o Sistema NÃO Pretende Ser

Para preservar sua identidade original:

* **NÃO** é um simples gerenciador passivo de fichas de RPG de mesa;
* **NÃO** é um jogo de ação rápida, combate em tempo real ou mecânicas puramente arcade;
* **NÃO** é um livro tradicional ou romance com narração longa e passiva;
* **NÃO** é um bloco de notas desprovido de regras investigativas e de continuidade.

---

## 6. Escopo do Produto

### 6.1. Funcionalidades Definidas (Núcleo do Produto)
* Criação e seleção de campanha;
* Criação/configuração da protagonista (com agência exclusiva do jogador);
* Execução episódica estruturada (Abertura → Cena → Investigação → Desenvolvimento → Consequências → Encerramento);
* Envio de ações pela jogadora e retorno reativo da IA após a ação;
* Registro e consulta de casos, pistas, evidências e personagens;
* Controle contínuo de data, horário e localização;
* Persistência de estado por meio de `WORLD_STATE`;
* Recuperação de memória em 4 níveis contextuais.

### 6.2. Funcionalidades Planejadas (Refinamento)
* Mapa interativo visual integrado com cálculo de deslocamento;
* Visualização em teia/quadro de investigação de conexões entre suspeitos e evidências;
* Interface dedicada para conferência cronológica detalhada da linha do tempo.

### 6.3. Funcionalidades Futuras (Backlog Pós-MVP)
* Sistema multijogador / cooperação entre múltiplos investigadores;
* Exportação de relatórios de casos;
* `A DEFINIR`.

### 6.4. Itens Ainda Não Definidos
* Regras numéricas ou testes formais de atributos/dados (se haverá mecânica de dados ou resolução puramente qualitativa/narrativa): `A DEFINIR`;
* Modelo de monetização e planos do Micro SaaS: `A DEFINIR`;
* Perfis administrativos de sistema e auditoria técnica: `A DEFINIR`.
