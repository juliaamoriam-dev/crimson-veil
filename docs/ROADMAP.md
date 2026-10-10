# Roadmap de Desenvolvimento — Crimson Veil

Este documento apresenta as fases de evolução do projeto Crimson Veil. As tarefas específicas de cada fase serão definidas e refinadas de forma incremental ao longo do ciclo de desenvolvimento.

---

## Fases do Projeto

### FASE 0 — Fundação
* **Status:** Concluída
* **Objetivo:** Estabelecimento da estrutura inicial de diretórios, regras, diretrizes de governança e documentação base.

### FASE 1 — Definição do Produto
* **Status:** Concluída
* **Objetivo:** Integração do Master Prompt, definição de escopo, visão do produto e matriz de requisitos.

### FASE 2 — Modelagem do Domínio
* **Status:** Concluída
* **Objetivo:** Mapeamento conceitual de entidades, value objects, relacionamentos, consolidação de regras e povoamento do catálogo canônico.

### FASE 3 — Arquitetura
* **Status:** Concluída
* **Objetivo:** Definição da stack tecnológica, camadas do sistema, contratos conceituais da API REST, estratégia do WORLD_STATE, fluxo do motor narrativo e diretrizes de segurança.

### FASE 4 — MVP
* **Status:** Frontend funcional concluído; base de persistência do Marco 1 implementada.
* **Tarefas específicas:** Interface, campanha canônica e integração da API local.

### FASE 5 — Testes
* **Status:** Testes de aceitação do Marco 1 implementados e executados.
* **Tarefas específicas:** Aumentar cobertura conforme evoluírem as regras de negócio.

### FASE 6 — Refinamento
* **Status:** A Iniciar
* **Tarefas específicas:** `A DEFINIR`

### FASE 7 — Deploy
* **Status:** A Iniciar
* **Tarefas específicas:** `A DEFINIR`

### FASE 8 — Evolução
* **Status:** A Iniciar
* **Tarefas específicas:** `A DEFINIR`

## Marco 1 — Backend e Persistência Real

* **Status:** Concluído e validado localmente.
* **Escopo:** API REST em Java 21/Spring Boot, persistência SQLite, migração inicial protegida de `camp-001`, ações e histórico idempotentes, controle otimista de versão e integração sem alteração visual do frontend.
* **Fora do escopo:** IA, autonomia completa de NPCs, autenticação e hospedagem.
* **Marco seguinte:** Marco 2 — vida pessoal persistente da protagonista, incremental e sem duplicar sistemas de domínio.

## Marco 2 — Celular e Vida Pessoal

* **Status:** Entrega 1 implementada; validação final em andamento.
* **Entrega 1 — Celular funcional:** contatos, conversas individuais e mensagens persistentes por campanha/protagonista; envio pelo jogador, recebimento de teste controlado apenas em `development`, leitura básica, relógio ficcional persistido, idempotência e concorrência integradas à versão da campanha.
* **Próximas entregas (não implementadas):** integrar notificações e mensagens a eventos explícitos do mundo/tempo; modelar compromissos e agenda sobre a mesma fonte de tempo; evoluir rotinas e relacionamentos de NPCs vinculados aos contatos canônicos. Evitar mensagens espontâneas aleatórias e aplicativos sem comportamento real.
* **Fora desta entrega:** IA narrativa, agenda completa, chamadas reais, autonomia geral de NPCs e geração espontânea de mensagens.
