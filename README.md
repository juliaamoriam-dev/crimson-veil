# 🕵️ Crimson Veil

### Sistema Narrativo Investigativo Interativo

> *Investigue. Conecte pistas. Tome decisões. O mundo continua mesmo quando você para de procurar.*

O **Crimson Veil** é uma plataforma narrativa de investigação policial e mistério que coloca a jogadora no centro de um universo vivo e reativo. Em vez de conduzir a investigação por trilhos pré-determinados, o sistema fornece um mundo persistente com passagem contínua de tempo, autonomia de personagens, consequências duradouras e preservação irrestrita da agência da protagonista.

O projeto é desenvolvido com foco em Engenharia de Software, combinando conceitos de modelagem de domínio rica, arquitetura em camadas e sistemas narrativos emergentes.

---

## 📌 Status do Projeto

O projeto evolui por marcos incrementais. O Marco 1 adiciona backend local e persistência real, mantendo o frontend e o conteúdo canônico existentes.

| Componente / Módulo | Status | Detalhes |
| :--- | :--- | :--- |
| **Frontend / Interface** | 🟢 Funcional (Protótipo) | Interface modular em HTML5/CSS3/JS com abas de investigação e RPG |
| **Sistema de Campanhas** | 🟢 Implementado | Múltiplas campanhas simultâneas com estado 100% isolado |
| **Narrative Engine** | 🟢 Funcional (Regras locais) | Direção de cena por turnos, validação de agência e injeção de contexto |
| **Mundo Vivo (Living World)** | 🟢 Implementado v1 | Passagem contínua de tempo, rotinas e eventos autônomos de NPCs |
| **Sistema de Consequências** | 🟢 Implementado v1 | Desdobramentos objetivos no mundo a partir de eventos e atrasos |
| **Dossiê Investigativo** | 🟢 Funcional | Gestão de casos, suspeitos, pistas, laudos e evidências |
| **Timeline e Logs** | 🟢 Implementado | Registro cronológico contínuo e Event Log interno auditável |
| **Backend (API REST)** | 🟢 Implementado no Marco 1 | Java 21, Spring Boot 3.4.5; campanhas, estado, ações e histórico |
| **Banco de Dados** | 🟢 Implementado no Marco 1 | SQLite local, transações e chaves idempotentes |
| **Integração com LLM Externa** | 🔵 Futuro | IA consultiva integrada via API, sem autonomia sobre regras do sistema |

---

## 🔍 O que é o Crimson Veil?

Crimson Veil é uma experiência investigativa episódica inspirada no ritmo de séries policiais contemporâneas (*The Rookie*, *Brooklyn Nine-Nine*, *Castle*, *Criminal Minds*, *True Detective*).

O sistema articula:
* **RPG Investigativo:** A jogadora controla exclusivamente as ações e falas da protagonista (Detetive), interagindo via texto livre;
* **Mundo com Autonomia:** Personagens secundários possuem compromissos, rotinas e objetivos próprios que ocorrem mesmo fora do campo de visão da investigadora;
* **Passagem Real de Tempo:** Cada ação, deslocamento ou perícia consome tempo do relógio da cidade, afetando a disponibilidade de testemunhas e a conservação de cenas de crime;
* **Investigação Não Linear:** Pistas apresentam fatos observáveis em vez de deduções prontas. Testemunhas podem omitir fatos, e pistas falsas existem;
* **Consequências Orgânicas:** Erros ou atrasos não resultam em telas de *Game Over*, mas transformam o cenário investigativo em novas ramificações.

> **Princípio Central:** A jogadora não segue uma história pronta. Ela investiga um mundo que reage às suas decisões.

---

## ⚙️ Como Funciona o Ciclo Narrativo

O sistema opera através de um ciclo estrito de validação de estado para garantir coerência factual:

```text
       AÇÃO DA JOGADORA
              ↓
    CONTEXTO DA CAMPANHA
 (Local, Horário, Clima, NPCs)
              ↓
       NARRATIVE ENGINE
  (Direção de Cena e Diálogos)
              ↓
           EVENTO
              ↓
         VALIDAÇÃO
  (Regras e Integridade Canônica)
              ↓
        WORLD_STATE
  (Mutações de Estado do Mundo)
              ↓
           MEMÓRIA
 (Imediata, Episódica e Permanente)
              ↓
        PRÓXIMO TURNO
```

### O Fluxo do Mundo Vivo

A autonomia do cenário opera sob a seguinte cadeia causal:

```text
Tempo Avança ──► Eventos Autônomos ──► Mudanças no Mundo ──► Consequências Ativas ──► Novas Possibilidades
```

1. **Ação:** A jogadora executa uma ação investigativa ou deslocamento;
2. **Tempo:** O relógio da investigação avança com base na duração estimada da atividade;
3. **Eventos:** O sistema verifica se horários previstos de eventos autônomos foram alcançados;
4. **Mundo:** NPCs alteram suas localizações e estados independentemente da presença da protagonista;
5. **Consequências:** Novas restrições ou oportunidades são registradas e alimentadas de volta ao motor narrativo.

---

## 💡 Diferenciais

* **Agência Absoluta da Protagonista:** O motor narrativo jamais toma decisões, manifesta pensamentos, descreve sentimentos ou força falas da investigadora. A protagonista pertence exclusivamente à jogadora.
* **Mundo Vivo e Descentralizado:** A cidade de Blackwood não fica pausada aguardando a jogadora. Se uma perícia for adiada, a cena do crime pode ser contaminada; se um depoimento atrasar, a testemunha pode deixar o local.
* **Tempo Persistente e Determinístico:** O horário no universo dita o expediente da delegacia, o trânsito da cidade e a prontidão dos serviços periciais.
* **Investigação Baseada em Fatos:** Evidências e depoimentos trazem indícios brutos. A dedução, a correlação e a conclusão lógica cabem inteiramente à jogadora.
* **Isolamento Total de Campanhas:** Cada campanha possui identificador exclusivo, linha do tempo independente, memória isolada e estado próprio, impedindo vazamento de dados.

---

## 🏛️ Arquitetura

O sistema é desenhado em duas camadas conceituais: **Experiência** (interface e controle de turnos) e **Sistema** (regras, persistência e memória).

### 1. Arquitetura Atual (Frontend e Backend Local)

Atualmente implementada no frontend para validação ágil de domínio e interatividade:

```text
┌────────────────────────────────────────────────────────┐
│                   FRONTEND (Web UI)                    │
│      Interface Modular • Gestão de Telas e Abas        │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP / JSON
                            ▼
┌────────────────────────────────────────────────────────┐
│             API REST Spring Boot                       │
│ Controllers • Services • Repositories JDBC             │
└───────────────────────────┬────────────────────────────┘
                            │ Transação SQLite
                            ▼
┌────────────────────────────────────────────────────────┐
│              campaign + world_state                    │
│       events • operations • campaign_history           │
└────────────────────────────────────────────────────────┘
```

O motor narrativo local existente continua responsável pelas respostas simuladas; ainda não há integração com IA nem autonomia completa de NPCs.

### 2. Próximas Evoluções

```text
┌────────────────────────────────────────────────────────┐
│                   FRONTEND (Web UI)                    │
│        HTML5 Semântico • Vanilla CSS • JavaScript      │
└───────────────────────────┬────────────────────────────┘
                            │ HTTP / JSON (REST API)
                            ▼
┌────────────────────────────────────────────────────────┐
│       Motor de mundo e narrativa validados             │
│       Integração futura com IA opcional                 │
│       Migrações de banco para evolução do schema        │
└────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tecnologias

### Implementadas no Protótipo Atual
* **Linguagens e Estrutura:** HTML5 Semântico, CSS3 Moderno (Vanilla CSS com Design System cinematográfico escuro), JavaScript Moderno (ES6+ modular).
* **Controle de Versão:** Git e repositório estruturado no GitHub.

### Backend implementado no Marco 1
* **Linguagem & Plataforma:** Java 21 (LTS)
* **Framework:** Spring Boot 3.4.5 (`Spring Web`, `Spring JDBC`, `Bean Validation`)
* **Build:** Apache Maven 3.9.9 via Maven Wrapper
* **Banco de Dados:** SQLite com JDBC
* **Comunicação:** API REST padronizada via contratos JSON

---

## 📂 Estrutura do Repositório

```text
crimson-veil/
├── docs/               # Documentação técnica formal e engenharia de software
│   ├── VISAO.md            # Visão geral, missão e princípios
│   ├── PRODUTO.md          # Especificação de produto e módulos conceituais
│   ├── REQUISITOS.md       # Matriz de requisitos funcionais e narrativos
│   ├── MODELO_DE_DOMINIO.md# Entidades conceituais, agregados e bounded contexts
│   ├── ARQUITETURA.md      # Arquitetura técnica, camadas e contratos de API
│   ├── BANCO_DE_DADOS.md   # Modelagem relacional preliminar e entidades
│   └── ROADMAP.md          # Cronograma e fases de desenvolvimento
├── frontend/           # Protótipo funcional interativo
│   ├── index.html          # Ponto de entrada da interface
│   ├── css/                # Folhas de estilo (design system escuro)
│   ├── js/                 # Lógica de aplicação, Narrative Engine e Mundo Vivo
│   └── assets/             # Recursos estáticos e imagens oficiais
├── backend/            # API Spring Boot, testes e banco SQLite local ignorado pelo Git
├── conhecimento/       # Catálogo do universo (Lore, Casos, Pistas, Personagens)
├── .gitignore          # Arquivos e diretórios ignorados pelo Git
└── README.md           # Apresentação do projeto e guia geral
```

---

## 🎯 Funcionalidades Atuais

| Funcionalidade | Estado | Descrição |
| :--- | :---: | :--- |
| **Gestão de Campanhas** | ✅ | Criação, alternância e isolamento completo entre múltiplas campanhas |
| **Criação de Personagem** | ✅ | Definição da ficha da investigadora, perfil e atributos investigativos |
| **Narrative Engine Reativo** | ✅ | Respostas com devolução de turno sem violar ações ou falas da protagonista |
| **Passagem Contínua de Tempo** | ✅ | Relógio interno avança proporcionalmente com ações, deslocamentos e diálogos |
| **Mundo Vivo (v1)** | ✅ | Acontecimentos autônomos e mudanças de localização de NPCs conforme o horário |
| **Sistema de Consequências (v1)**| ✅ | Eventos geram registros objetivos no mundo e influenciam a narrativa subsequente |
| **Dossiê do Caso 001** | ✅ | Acompanhamento de suspeitos, pistas observáveis, depoimentos e evidências |
| **Timeline e Event Log** | ✅ | Linha temporal cronológica e log de eventos auditável |
| **Interface e Design Escuro** | ✅ | Layout imersivo em tons escuros e carmesim com abas de trabalho |
| **Refinamento de Tom Narrativo**| 🚧 | Calibração de diálogos mais naturais e ritmo de série policial |
| **API REST (Spring Boot)** | ✅ | Backend em camadas com campanhas, ações, mutações e histórico |
| **Persistência Relacional** | ✅ | SQLite local com snapshots, estado do mundo e operações idempotentes |
| **Conexão com LLM Externa** | 📋 | Envio seletivo de contexto e geração guiada por prompts parametrizados |

---

## 📸 Screenshots

> Screenshots da interface de investigação e do painel narrativo serão adicionadas nesta seção conforme o protótipo for homologado.

---

## 🚀 Como Executar Localmente no Windows

### Pré-requisitos
* Um navegador web moderno (Chrome, Firefox, Edge).
* Java 21; Maven é baixado localmente pelo wrapper na primeira execução.
* Node.js somente para executar os testes do frontend.

No PowerShell, a partir da raiz do repositório:

```powershell
$javaExe = (Get-Command java).Source
$env:JAVA_HOME = Split-Path (Split-Path $javaExe)
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Abra `http://localhost:8080`. O Spring Boot serve a interface e a API na mesma origem. O arquivo local é `backend\data\crimson-veil.sqlite`; sua preservação está documentada em [docs/BANCO_DE_DADOS.md](docs/BANCO_DE_DADOS.md).

Testes:

```powershell
# Na raiz do repositório
node --test test/*.test.js

# Em backend\
.\mvnw.cmd test
```

---

## 🗺️ Roadmap de Evolução

Planejamento consolidado a partir do [docs/ROADMAP.md](docs/ROADMAP.md):

- [x] **Fase 0 — Fundação:** Estruturação documental, regras de workspace e repositório.
- [x] **Fase 1 — Definição do Produto:** Visão do produto, catálogo de requisitos e premissas.
- [x] **Fase 2 — Modelagem do Domínio:** Entidades conceituais, agregados e regras de fronteira.
- [x] **Fase 3 — Arquitetura Técnica:** Especificação de camadas, contratos conceituais e stack.
- [x] **Fase 4 — Protótipo Frontend Interativo:**
  - [x] Gerenciador de campanhas isoladas
  - [x] Narrative Engine e agência da jogadora
  - [x] Motor de Mundo Vivo e eventos temporais
  - [x] Sistema de Consequências v1
- [x] **Marco 1 — Backend e Persistência Real:** API REST Spring Boot, SQLite, integração frontend, migração canônica e testes automatizados.
- [ ] **Próximo — Evolução do Motor:** Migrações versionadas, regras de turno no backend e expansão dos testes.
- [ ] **Fase 7 — Inteligência Artificial Integrada:** Integração com LLM e injeção de contexto dos 4 níveis de memória.
- [ ] **Fase 8 — Refinamento e Testes:** Testes integrados, segurança, refinamento visual e usabilidade.
- [ ] **Fase 9 — Deploy e Infraestrutura:** Empacotamento de produção e publicação em ambiente de nuvem.

---

## 🧠 Filosofia do Projeto

```text
Agência da Jogadora + Investigação Objetiva + Memória Contínua + Tempo Implacável + Consequências
═══════════════════════════════════════════════════════════════════════════════════════════════
                                 A Experiência Crimson Veil
```

> "Crimson Veil não deve contar uma história pronta para a jogadora. Deve criar um mundo capaz de reagir às suas decisões."

O projeto parte do princípio de que o verdadeiro suspense investigativo nasce da **consequência** e da **incerteza**. Ao delegar ao sistema o papel de árbitro rigoroso do mundo e entregar à jogadora o protagonismo absoluto das deduções, a narrativa emerge naturalmente a cada turno.

---

## 🧪 Estado Atual e Abordagem de Desenvolvimento

O projeto adota uma abordagem de desenvolvimento estritamente **incremental**:

1. Validar a experiência interativa e as regras narrativas;
2. Validar o modelo de domínio na prática;
3. Refinar os módulos e a interface;
4. Implementar a camada de persistência e regras de negócio no backend;
5. Conectar o provedor de IA consultiva;
6. Otimizar a performance e a experiência geral.

O código do frontend atual serve como ferramenta de validação conceitual contínua antes de congelar schemas de banco de dados ou endpoints de backend, evitando retrabalho e arquitetura prematura.

---

## 📄 Documentação Técnica

Para se aprofundar nas decisões de engenharia e modelagem do sistema:

* [docs/VISAO.md](docs/VISAO.md) — Missão, conceitos centrais e diretrizes fundamentais.
* [docs/PRODUTO.md](docs/PRODUTO.md) — Escopo do produto, módulos funcionais e camadas.
* [docs/REQUISITOS.md](docs/REQUISITOS.md) — Matriz de requisitos funcionais, narrativos e não funcionais.
* [docs/MODELO_DE_DOMINIO.md](docs/MODELO_DE_DOMINIO.md) — Modelagem conceitual de entidades, Value Objects e agregados.
* [docs/ARQUITETURA.md](docs/ARQUITETURA.md) — Decisões arquiteturais, camadas e stack tecnológica.
* [docs/BANCO_DE_DADOS.md](docs/BANCO_DE_DADOS.md) — Esquema SQLite, migração canônica e preservação.
* [docs/GUIA_EXECUCAO_LOCAL.md](docs/GUIA_EXECUCAO_LOCAL.md) — Configuração e comandos detalhados para Windows.
* [docs/ROADMAP.md](docs/ROADMAP.md) — Fases de evolução e marcos do projeto.

---

## 👤 Autoria

Projeto idealizado e desenvolvido por **Julia Amoria**.

Crimson Veil é um projeto autoral de experimentação em:
* Desenvolvimento Web e Engenharia de Software;
* Sistemas narrativos e design de jogos investigativos;
* Inteligência Artificial aplicada e arquitetura de contexto;
* Modelagem de domínio e arquitetura em camadas;
* Criação de experiências imersivas interativas.
