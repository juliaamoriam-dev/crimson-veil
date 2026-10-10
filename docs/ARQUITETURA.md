# Arquitetura Técnica — Crimson Veil

> **Fontes de Referência:**
> * [conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md](file:///d:/CrimsonVeil/conhecimento/05-documentos/oficiais/MASTER_PROMPT_V1.md)
> * [docs/PRODUTO.md](file:///d:/CrimsonVeil/docs/PRODUTO.md)
> * [docs/REQUISITOS.md](file:///d:/CrimsonVeil/docs/REQUISITOS.md)
> * [docs/MODELO_DE_DOMINIO.md](file:///d:/CrimsonVeil/docs/MODELO_DE_DOMINIO.md)
> * [docs/ROADMAP.md](file:///d:/CrimsonVeil/docs/ROADMAP.md)

---

## 1. Visão Geral e Princípios Arquiteturais

A arquitetura do **Crimson Veil** foi projetada com base nos princípios de **simplicidade, clareza, manutenibilidade e baixo acoplamento**, priorizando o aprendizado contínuo de Engenharia de Software sem adicionar complexidades corporativas desnecessárias.

Uma arquitetura profissional é aquela que resolve o problema com robustez e elegância, permitindo que o sistema evolua de forma previsível e incremental.

### Princípios Norteadores:
1. **Separação Estrita de Responsabilidades:** Cada camada possui um único propósito bem delimitado.
2. **Modelo em Duas Camadas:** A Camada 1 (Experiência/Interface) consome contratos da API; a Camada 2 (Sistema/Persistência) garante a integridade das regras e a memória narrativa.
3. **Mundo Orientado a Estado:** A IA não é uma simples caixa de diálogo aberta; ela é um componente consultivo que recebe estado controlado e retorna propostas de narrativa que são validadas pelo sistema.
4. **Isolamento de Domínio:** Dados de uma campanha jamais vazam para outra.

---

## 2. Stack Tecnológica Implementada no Marco 1

```text
┌────────────────────────────────────────────────────────┐
│                   FRONTEND (Web UI)                    │
│    HTML5 Semântico + Vanilla CSS / Tailwind CSS        │
│          JavaScript Moderno (ES6+ Modular)             │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP / JSON (REST API)
                            ▼
┌────────────────────────────────────────────────────────┐
│                   BACKEND (API REST)                   │
│   Java 21 LTS + Spring Boot 3.4.5 (Web + JDBC)        │
│        Bean Validation + Maven Wrapper                 │
└───────────────────────────┬────────────────────────────┘
                            │ JDBC / SQL
                            ▼
┌────────────────────────────────────────────────────────┐
│                BANCO DE DADOS RELACIONAL               │
│              SQLite (arquivo local)                    │
└────────────────────────────────────────────────────────┘
```

### 2.1. Backend
* **Linguagem:** Java 21 (versão LTS com Virtual Threads e estabilidade de longo prazo).
* **Framework:** Spring Boot 3.4.5:
  * `Spring Web`: endpoints REST e entrega local dos arquivos estáticos existentes;
  * `Spring JDBC`: repositório SQL explícito e transações JDBC compatíveis com SQLite;
  * `Bean Validation`: validação dos DTOs de entrada.
* **Gerenciador de Dependências e Build:** Maven 3.9.9 pelo Maven Wrapper versionado.

### 2.2. Banco de Dados
* **SGBD:** SQLite em arquivo local, sem serviço ou custo operacional.
* **Driver:** Xerial SQLite JDBC 3.49.1.0.
* **Schema:** `backend/src/main/resources/schema.sql`, com chaves estrangeiras, restrições, índices e unicidade. Alterações futuras devem ser versionadas como migrations antes de alterar instalações existentes.

### 2.3. Frontend
* **Tecnologias:** HTML5 semântico, JavaScript moderno (ES6+ nativo com módulos) e CSS com Tailwind CSS.
* **Justificativa para não usar React/Vue/Angular no MVP:**
  * Elimina tooling complexo de build (Vite/Webpack/Babel), gerenciadores pesados de dependências e estado global desnecessário;
  * Reduz a barreira de aprendizado da desenvolvedora, mantendo controle direto do ciclo de vida das requisições via `fetch()`;
  * Permite criar interfaces elegantes, escuras e cinematográficas com CSS puro e Tailwind sem complexidade arquitetural externa.

### 2.4. Infraestrutura do MVP
* Execução local: Spring Boot via Maven Wrapper e banco SQLite em `backend/data/crimson-veil.sqlite`; a aplicação serve a interface existente em `http://localhost:8080`.
* **Sem** Docker obrigatório, Kubernetes, microsserviços, mensageria assíncrona (RabbitMQ/Kafka) ou Redis nesta fase.

---

## 3. Decisão Arquitetural: Camadas do Backend

Adotamos a **Arquitetura em Camadas Clássica (Layered Architecture)**, padrão amplamente estabelecido na indústria e de fácil compreensão:

```text
Controlador (Controller)
       ↓  (recebe DTO de requisição, retorna DTO de resposta)
Serviço (Service)
       ↓  (aplica regras de negócio, orquestra IA e mutações)
Repositório (Repository / JdbcTemplate)
       ↓  (SQL transacional)
Banco de Dados (SQLite)
```

### 3.1. Responsabilidades por Camada

| Camada | Responsabilidade | O que PODE fazer | O que NÃO DEVE fazer |
| :--- | :--- | :--- | :--- |
| **Controlador** (`Controller`) | Ponto de entrada HTTP da API REST. | Receber requisições, acionar validações de DTO, delegar ao Service e retornar status HTTP correto (200, 201, 400, 404). | Conter regras de negócio, acessar o banco de dados diretamente ou manipular registros persistidos. |
| **Serviço** (`Service`) | Coração da aplicação (Lógica de Negócio). | Implementar regras do RPG, avançar o relógio de investigação, validar turnos, acionar a IA e atualizar o `WORLD_STATE`. | Manipular objetos `HttpServletRequest`/`HttpServletResponse` ou acoplar-se a protocolos de transporte. |
| **Repositório** (`Repository`) | Abstração de Acesso a Dados. | Executar consultas SQL/JPQL, persistir entidades e garantir consultas transacionais. | Conter regras de negócio ou validações de domínio. |
| **Entidade** (`Entity`) | Estado Persistido no Banco. | Mapear tabelas, chaves primárias, relacionamentos e restrições estruturais. | Chamar serviços, repositórios ou depender de DTOs. |
| **DTO** (`Data Transfer Object`) | Contrato de Transporte da API. | Estruturas leves (`records` Java) para entrada e saída de dados, blindando o banco. | Conter lógica de persistência ou regras de negócio. |

### 3.2. Regras de Dependência
* As chamadas sempre fluem de cima para baixo: `Controller` → `Service` → `Repository`.
* É expressamente proibido um `Controller` injetar um `Repository` diretamente (burlar a camada de serviço quebra a integridade das regras).
* É proibido o trânsito de dependências circulares entre serviços.

---

## 4. Organização de Pacotes no Backend

```text
backend/src/main/java/com/crimsonveil/
├── configuration/              # CORS local
├── controller/                 # Rotas REST /api/v1/campanhas
├── dto/                        # Contratos de entrada e saída
├── entity/                     # Registro de campanha
├── service/                    # Regras, validação e transações
├── repository/                 # JDBC e persistência SQLite
└── exception/                  # Erros HTTP padronizados
```

---

## 5. Arquitetura do Frontend

O frontend será estruturado como uma **Single Page Application (SPA) leve ou Multi-Page Application (MPA) desacoplada**, organizada por módulos:

```text
frontend/
├── index.html                  # Ponto de entrada / Seleção de campanhas
├── app.html                    # Interface principal da aplicação
│
├── css/
│   ├── principal.css           # Variáveis de tema (tons escuros, vermelho carmesim, terminal)
│   └── componentes.css         # Estilização de painéis, caixas de diálogo e botões
│
├── js/
│   ├── api/
│   │   ├── clienteHttp.js      # Wrapper padronizado do fetch() com tratamento de erros
│   │   ├── campanhaApi.js      # Chamadas de endpoints de campanha
│   │   └── cenaApi.js          # Chamadas de envio de ações e atualização de cena
│   │
│   ├── estado/
│   │   └── estadoLocal.js      # Estado reativo simples da sessão no navegador
│   │
│   ├── componentes/
│   │   ├── telaRpg.js          # Renderizador do feed de cenas e campo de ação
│   │   ├── painelInvestigacao.js # Dossiê de casos, evidências e pistas
│   │   ├── painelPersonagens.js # Fichas de NPCs e indicadores de relação
│   │   └── relogioMundo.js     # Exibição da data, hora local e localização
│   │
│   └── app.js                  # Inicialização e roteamento de abas
│
└── assets/                     # Imagens, brasões, ícones
```

---

## 6. Especificação Conceitual da API REST (`/api/v1`)

A API adota padrão RESTful, payloads em formato JSON e respostas padronizadas.

### 6.1. Campanhas
* **`POST /api/v1/campanhas`** — cria campanha a partir do snapshot atual do criador de campanha; retorna `201`.
* **`GET /api/v1/campanhas`** — lista resumos das campanhas locais.
* **`GET /api/v1/campanhas/{id}`** — carrega o snapshot persistido e sua versão.
* **`POST /api/v1/campanhas/migracao-canonica`** — cria `camp-001` apenas se não existir; nova tentativa retorna o registro atual sem sobrescrevê-lo.
* **`POST /api/v1/campanhas/{id}/acoes`** — registra uma ação com chave idempotente e versão esperada.
* **`PUT /api/v1/campanhas/{id}/estado`** — salva mutação validada do estado com controle otimista de versão.
* **`GET /api/v1/campanhas/{id}/historico`** — consulta registros auditáveis de criação e alterações.

### 6.2. Cenas e Execução de Turnos (Tela de RPG)
* **`GET /api/v1/campanhas/{id}/cenas/atual`**
  * *Responsabilidade:* Retornar a cena ativa, histórico de mensagens da cena e indicadores.
  * *Saída (200 OK):* Objeto contendo localização, horário fictício, personagens no local e mensagens.
* **`POST /api/v1/campanhas/{id}/cenas/atual/acao`**
  * *Responsabilidade:* Submeter a ação/fala da jogadora para processamento do turno.
  * *Entrada:* `{ "acaoJogador": "Milena examina os relógios sobre a mesa e anota os horários." }`
  * *Saída (200 OK):* `{ "respostaNarrador": "...", "novoHorario": "02:25", "pistasObservadas": [...] }`
  * *Erros:* 400 (ação inválida/em branco), 404 (campanha/cena não encontrada), 500 (erro de IA/motor).

### 6.3. Investigação (Casos, Pistas e Evidências)
* **`GET /api/v1/campanhas/{id}/casos`**
  * *Responsabilidade:* Listar casos ativos, resolvidos e arquivados da campanha.
* **`GET /api/v1/campanhas/{id}/casos/{casoId}`**
  * *Responsabilidade:* Dossiê completo do caso com pistas descobertas e evidências anexadas.
* **`GET /api/v1/campanhas/{id}/pistas`**
  * *Responsabilidade:* Catálogo de pistas descobertas e seu status de apuração.
* **`GET /api/v1/campanhas/{id}/evidencias`**
  * *Responsabilidade:* Lista de materiais probatórios custodiados formalmente.

### 6.4. Personagens e Relacionamentos
* **`GET /api/v1/campanhas/{id}/personagens`**
  * *Responsabilidade:* Listar NPCs conhecidos pela protagonista.
* **`GET /api/v1/campanhas/{id}/personagens/{personagemId}/relacionamento`**
  * *Responsabilidade:* Retornar o histórico e o nível de relacionamento (confiança, respeito).

### 6.5. Linha do Tempo e Estado do Mundo
* **`GET /api/v1/campanhas/{id}/timeline`**
  * *Responsabilidade:* Retornar a sequência cronológica dos eventos registrados.
* O estado do mundo é carregado junto ao agregado; data, horário, localização e contador vêm das colunas tipadas em `world_state`.

---

## 7. Estratégia de Persistência e Banco de Dados

> O esquema físico implantado está descrito em [docs/BANCO_DE_DADOS.md](./BANCO_DE_DADOS.md).

### Diretrizes Gerais:
1. **Agregado por campanha:** o snapshot serializado preserva as estruturas e identidades atuais do domínio.
2. **WORLD_STATE relacional:** data, horário, localização e contador usam colunas tipadas; o restante permanece em JSON.
3. **Transação:** campanha, WORLD_STATE, eventos, operação e histórico são gravados atomicamente.
4. **Idempotência e concorrência:** unicidade por operação e versão otimista bloqueiam repetições e atualizações obsoletas.

---

## 8. Estratégia do `WORLD_STATE`

O `WORLD_STATE` é o registro consolidado do momento presente da campanha.

### Responsabilidades Arquiteturais:
* **Pertencimento:** Pertence exclusivamente a uma `Campanha` (1 : 1).
* **Modificação:** Modificado de forma atômica e exclusiva pelo `TurnoService` ao término da validação de cada turno.
* **Consulta:** Consultado pelo `MotorNarrativoService` para injeção de contexto na IA e pela interface web para exibição do relógio/local.
* **Implementado:** tabela `world_state` por campanha com data, horário, localização e contador tipados; `state_json` mantém os atributos contextuais em JSON.

---

## 9. Arquitetura Conceitual de Memória da IA

Para garantir o respeito ao *Princípio de Recuperação Seletiva* sem ferramentas pesadas no MVP:

```text
┌────────────────────────────────────────────────────────┐
│ 1. MEMÓRIA IMEDIATA                                    │
│    Últimas mensagens da tabela `mensagens_cena`.       │
├────────────────────────────────────────────────────────┤
│ 2. MEMÓRIA EPISÓDICA                                   │
│    Registros de eventos marcados no `episodio_atual`.  │
├────────────────────────────────────────────────────────┤
│ 3. MEMÓRIA DE CAMPANHA                                 │
│    Tabela `casos` (concluídos) e `relacionamentos`.    │
├────────────────────────────────────────────────────────┤
│ 4. MEMÓRIA PERMANENTE (CÂNON)                          │
│    Catálogo em arquivos ou tabelas de leitura pura.    │
└────────────────────────────────────────────────────────┘
```

* **Filtro de Relevância por Parâmetros:** A seleção da memória é feita pelo `MotorNarrativoService` através de parâmetros do `WORLD_STATE`. Se a cena é no "Depósito 217", o sistema busca apenas fatos ligados ao depósito e ao caso ativo, descartando informações alheias.
* **Garantia de Isolamento:** Todas as consultas trazem obrigatoriamente a cláusula `WHERE campanha_id = :id`.

---

## 10. Fluxo Arquitetural do Turno Narrativo

O ciclo de execução de um turno investigativo segue um pipeline rigoroso de validação e persistência:

```text
[1] Jogadora envia ação via Web UI
         ↓ (POST /api/v1/campanhas/{id}/cenas/atual/acao)
[2] Controlador valida formato e payload (Bean Validation)
         ↓
[3] TurnoService carrega WORLD_STATE e cena ativa
         ↓
[4] MotorNarrativo recupera fatias de memória pertinentes (Seletiva)
         ↓
[5] MotorNarrativo monta Prompt Estruturado:
    - Regras Absolutas de Agência e Estilo Cinematográfico
    - Contexto da Cena (Local, Hora, Presentes)
    - Pistas e Fatos Relevantes
    - Ação Finalizada da Jogadora
         ↓
[6] Chamada HTTP ao Provedor de IA
         ↓
[7] Validador de Agência analisa a resposta da IA:
    - Se a IA tentou controlar a protagonista → Tratamento/Ajuste
         ↓
[8] Parser de Consequências processa:
    - Tempo decorrido estimado (avança relógio)
    - Pistas observadas na descrição
    - Eventos disparados
         ↓
[9] Atualização transacional (@Transactional):
    - Salva mensagem na cena
    - Atualiza WORLD_STATE
    - Registra pistas/evidências
         ↓
[10] Retorno JSON para a Web UI e renderização imediata
```

---

## 11. Segurança da Aplicação

1. **Isolamento de Dados:** Cada campanha é vinculada a um `usuario_id`. Nenhuma rota permite acesso a dados de campanhas sem validar a posse da conta.
2. **Gerenciamento Seguro de Credenciais:** Chaves de API de inteligência artificial e credenciais de banco são injetadas estritamente via variáveis de ambiente (`${CHAVE_IA}`, `${DB_PASSWORD}`), nunca versionadas no Git.
3. **Validação de Entrada:** Uso sistemático de `@Valid` e anotações do Bean Validation nos DTOs para impedir ataques de injeção ou estados corrompidos.
4. **Tratamento Centralizado de Exceções:** `@RestControllerAdvice` captura erros e padroniza respostas sem expor stack traces ou detalhes internos da infraestrutura ao usuário.

---

## 12. Observabilidade do MVP

Para manter a simplicidade técnica:
* **Logs Estruturados (SLF4J / Logback):**
  * `INFO`: Transições de campanha, início/fim de episódios e criação de evidências;
  * `WARN`: Violações de agência corrigidas pelo validador ou chamadas lentas de IA;
  * `ERROR`: Falhas de banco, erros 500 e exceções não mapeadas.
* **MDC (Mapped Diagnostic Context):** Inclusão automática de `campanhaId` nos logs para facilitar rastreamento de fluxos.

---

## 13. Matriz de Decisões Arquiteturais

### 13.1. Decisões Aprovadas
* Arquitetura em camadas desacopladas (`Controller` → `Service` → `Repository`).
* Separação conceitual entre Camada 1 (Experiência) e Camada 2 (Sistema).
* Resolução narrativa/dedutiva para o MVP (sem rolagens de dados ou atributos numéricos).
* API RESTful com versionamento unificado sob `/api/v1`.
* Agência absoluta da protagonista validada antes da entrega da resposta à usuária.

### 13.2. Decisões do Marco 1
* Backend local: Java 21 + Spring Boot 3.4.5 + Maven Wrapper.
* Persistência inicial: SQLite com `JdbcTemplate`, snapshot JSON e colunas tipadas do WORLD_STATE.
* Integração HTTP/JSON sem IA, hospedagem ou serviço pago obrigatório.

### 13.3. Itens que Permanecem `A DEFINIR`
* Provedor e modelo específico de IA (OpenAI, Gemini, local, etc.).
* Mecânica exata do validador de agência (regex/heurística leve vs. chamada rápida de julgamento).
* Mecanismo definitivo de autenticação de usuários (JWT stateless vs. Cookie/Session simples).
