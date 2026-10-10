# Guia de Integração da IA Narradora — Google Gemini

Este documento descreve a arquitetura, endpoints, configuração e operação da **IA Narradora com Google Gemini** no **Crimson Veil**.

---

## 1. Visão Geral da Arquitetura

A integração substitui o motor de respostas simuladas por palavras-chave no frontend por um serviço de narrativa orquestrado no backend Spring Boot com persistência relacional no SQLite.

```text
[Frontend — Terminal Investigativo]
             │
             │ POST /api/v1/campanhas/{id}/narrativa/acao
             ▼
[NarrativaController]
             │
             │ Validação Bean Validation + Idempotency-Key
             ▼
[NarrativaService] (Transacional @Transactional)
             │
             ├─► 1. Verifica cache de idempotência (campaign_operations)
             ├─► 2. Valida versão otimista da campanha (versaoEsperada)
             ├─► 3. Carrega estado real no SQLite (campanhas, world_state, pistas)
             │
             ▼
[MotorNarrativoService]
             │
             ├─► Monta System Instruction (Regras canônicas de agência e estilo noir)
             ├─► Monta User Prompt (Cena ativa, pistas descobertas, histórico recente, ação)
             │
             ▼
[GeminiRestClient]
             │
             ├─► POST https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent
             ├─► Header: x-goog-api-key: ${GEMINI_API_KEY}
             ├─► Timeout configurável (30s) com JdkClientHttpRequestFactory
             │
             ▼
[ValidadorAgencia]
             │
             ├─► Higienização de violações de agência da protagonista (MASTER_PROMPT_V1.md)
             │
             ▼
[MotorTempoService]
             │
             ├─► Cálculo determinístico do avanço temporal e virada de dia/mês/ano
             │
             ▼
[Persistência SQLite Atômica]
             ├─► Atualiza campaigns (incrementa versão)
             ├─► Atualiza world_state (horário, data, contador de ações)
             ├─► Salva campaign_operations (idempotência)
             ├─► Salva campaign_history (registro de auditoria)
             │
             ▼
[Retorno Oficial AcaoNarrativaResposta para o Frontend]
```

---

## 2. Endpoint Oficial da Narrativa

* **Rota:** `POST /api/v1/campanhas/{campanhaId}/narrativa/acao`
* **Header Recomendado:** `Idempotency-Key: {chaveOperacao}`
* **Content-Type:** `application/json`

### Payload de Entrada (`AcaoNarrativaRequest`):
```json
{
  "chaveOperacao": "acao-camp-001-a1b2c3d4-e5f6",
  "versaoEsperada": 1,
  "acao": "Examinar a gaveta trancada da escrivaninha de Arthur"
}
```

### Resposta de Sucesso (`AcaoNarrativaResposta` — HTTP 201 ou 200 se repetida):
```json
{
  "campanha": {
    "id": "camp-001",
    "version": 2,
    "contadorAcoes": 1,
    "estadoMundo": {
      "dataAtual": "14 de Outubro de 2026",
      "horarioAtual": "03:35",
      "localAtual": "Apartamento 504 — Blackwood"
    },
    "mensagensCena": [ ... ]
  },
  "versao": 2,
  "repetida": false,
  "textoNarracao": "Adrian se aproxima da escrivaninha e ilumina a fresta com a lanterna forense.\n\nAdrian: — A trava foi forçada por dentro. Quem fechou isso não queria que fosse aberto sem ferramentas.",
  "modelo": "gemini-2.5-flash",
  "horarioAtual": "03:35",
  "duracaoMinutos": 5,
  "sugestoes": [
    "Examinar a fresta arrombada com a ferramenta de perícia",
    "Perguntar a Adrian quem teve acesso à sala antes da perícia",
    "Pedir a Noah que busque registros de crachá do prédio",
    "Verificar a janela em busca de pontos de entrada externos"
  ]
}
```

### Códigos de Retorno Notáveis:
* `201 CREATED`: Turno processado, validado e persistido com sucesso.
* `200 OK`: Ação repetida (idempotência confirmada; retorna resposta em cache sem gastar tokens).
* `400 BAD REQUEST`: Ação em branco ou formato inválido.
* `409 CONFLICT`: Conflito de concorrência (`versaoEsperada` diverge da versão no banco).
* `502 BAD GATEWAY`: Erro retornado pelo Google Gemini (rate limit, indisponibilidade ou timeout). O estado local e o banco de dados são preservados sem alterações.
* `503 SERVICE UNAVAILABLE`: Chave `GEMINI_API_KEY` ausente no servidor. O texto do jogador é preservado no formulário.

---

## 3. Variáveis de Ambiente e Configuração

O Crimson Veil suporta configuração tanto por variáveis de ambiente tradicionais do sistema operacional quanto por um arquivo local `.env` colocado na pasta `backend/`.

### Prioridade de Configuração:
1. **Variáveis do Sistema Operacional (`System.getenv`)**: Maior precedência para ambientes de produção e CI/CD.
2. **Propriedades da JVM (`System.getProperty`)**: Argumentos `-D` passados na inicialização.
3. **Arquivo Local `.env` (`backend/.env`)**: Carregado automaticamente na inicialização local, ideal para desenvolvimento.
4. **Padrões da Aplicação (`application.properties`)**: Valores de fábrica da aplicação.

| Variável | Propriedade Spring | Padrão Central | Descrição |
| :--- | :--- | :--- | :--- |
| `GEMINI_API_KEY` | `crimson-veil.gemini.api-key` | *(vazio)* | **Obrigatória para IA.** Chave privada da API Gemini. |
| `GEMINI_MODEL` | `crimson-veil.gemini.model` | `gemini-3.8-flash` | **Modelo oficial.** Centralizado em `application.properties`. |
| `GEMINI_BASE_URL` | `crimson-veil.gemini.base-url` | `https://generativelanguage.googleapis.com/v1beta` | Endpoint base da API REST Gemini. |
| `GEMINI_TIMEOUT_SECONDS` | `crimson-veil.gemini.timeout-seconds` | `30` | Timeout de conexão e leitura HTTP em segundos. |
| `GEMINI_MAX_OUTPUT_TOKENS` | `crimson-veil.gemini.max-output-tokens` | `1000` | Limite de tokens de saída por turno narrativo. |
| `GEMINI_TEMPERATURE` | `crimson-veil.gemini.temperature` | `0.7` | Temperatura do modelo (criatividade noir balanceada). |
| `PORT` | `server.port` | `8080` | Porta local do servidor HTTP backend. |

> [!NOTE]
> O modelo Gemini padrão está centralizado em um único ponto da aplicação: `backend/src/main/resources/application.properties` (`crimson-veil.gemini.model=${GEMINI_MODEL:gemini-3.8-flash}`). Você pode alterá-lo diretamente nesse arquivo ou sobrescrevê-lo no seu `backend/.env` através de `GEMINI_MODEL`.

---

## 4. Como Configurar a Chave Localmente (Recomendado: `.env`)

### Método Recomendado: Arquivo `.env` na pasta `backend`

1. Na raiz do projeto, acesse a pasta `backend`.
2. Copie o arquivo modelo `backend/.env.example` para `backend/.env`:
   * **Via PowerShell:**
     ```powershell
     Copy-Item backend\.env.example backend\.env
     ```
   * **Ou via VS Code:** clique com o botão direito em `backend/.env.example`, selecione **Copy**, depois **Paste** na pasta `backend` e renomeie para `.env`.
3. Abra o arquivo `backend/.env` e preencha sua chave obtida no [Google AI Studio](https://aistudio.google.com/app/apikey):
   ```env
   GEMINI_API_KEY=AIzaSy...sua_chave_real_aqui...
   ```
4. Pronto! O backend carregará a variável automaticamente ao iniciar, sem necessidade de configurar o terminal toda vez.

> [!IMPORTANT]
> O arquivo `backend/.env` está configurado no `.gitignore` e **nunca** é versionado nem enviado para o repositório Git.

---

### Método Alternativo: Variável de Ambiente do Sistema

Caso prefira não usar o arquivo `.env`, você pode continuar utilizando variáveis de ambiente externas:

* **No PowerShell (Sessão Atual):**
  ```powershell
  $env:GEMINI_API_KEY="AIzaSy...sua_chave_aqui..."
  ```
* **No PowerShell (Permanente para o seu Usuário Windows):**
  ```powershell
  [System.Environment]::SetEnvironmentVariable('GEMINI_API_KEY', 'sua_chave_aqui', 'User')
  ```
* **No Prompt de Comando (CMD):**
  ```cmd
  set GEMINI_API_KEY=sua_chave_aqui
  ```

---

## 5. Como Iniciar o Crimson Veil pelo VS Code

Com o arquivo `backend/.env` configurado, você pode iniciar o Crimson Veil diretamente pelo VS Code usando qualquer uma das opções abaixo:

### Opção 1 — Pelo Terminal Integrado do VS Code (Recomendada)
1. No VS Code, abra o terminal integrado (`Ctrl + '` ou menu **Terminal -> New Terminal**).
2. Navegue até a pasta do backend:
   ```powershell
   cd backend
   ```
3. Inicie o servidor Spring Boot:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
4. O backend inicializará na porta `8080` e exibirá no log a confirmação:
   ```text
   Arquivo .env carregado de '...\backend\.env' (1 variáveis aplicadas ao ambiente local)
   ```

### Opção 2 — Pelo Explorador do VS Code (Extensão Java)
1. Abra o arquivo [CrimsonVeilApplication.java](file:///D:/CrimsonVeil/backend/src/main/java/com/crimsonveil/CrimsonVeilApplication.java) no editor.
2. Clique no link **Run** que aparece logo acima do método `main(String[] args)` (ou pressione `F5`).
3. O `DotenvLoader` carregará o `backend/.env` automaticamente antes da inicialização do contexto Spring.

---

### Acessando a Aplicação:
1. Abra o navegador em:
   ```text
   http://localhost:8080/
   ```
   *(O backend Spring Boot serve a interface estática do frontend diretamente de `frontend/`)*.
2. Faça login com credenciais policiais, entre na aba **Investigação**, digite uma ação no terminal (ex: *"Perguntar a Noah sobre os logs do elevador"*) e clique em **Enviar**.
3. O narrador da IA responderá usando o modelo configurado, persistindo o estado com segurança no SQLite local.


---

## 6. Salvaguardas, Resiliência e Custos

1. **A IA Não Manda no Banco de Dados:** Apenas o backend calcula o relógio determinístico, atualiza versões, registra logs de auditoria e grava o estado no SQLite. O modelo só produz texto descritivo e diálogos.
2. **Proteção de Agência:** O componente `ValidadorAgencia` garante que nenhuma emoção interna ou decisão forçada seja imposta à protagonista.
3. **Idempotência com Risco Zero de Duplo Gasto:** Clicar duas vezes ou retransmitir uma requisição utiliza a `chaveOperacao` para devolver a resposta já salva sem invocar a API do Gemini novamente.
4. **Sem Vazamento de Credenciais:** A chave nunca é enviada ao navegador, nunca é gravada no banco SQLite e é ocultada de qualquer mensagem de log de erro (`[REDACTED_API_KEY]`).
5. **Limitações e Quotas do Google:** O uso gratuito ou pago depende das cotas da conta Google associada à sua chave de API. Respostas HTTP 429 geram erro seguro `502 Bad Gateway` com preservação do texto digitado no frontend.

---

## 7. Sistema de Ações Narrativas Dinâmicas

A cada turno investigativo concluído, o Crimson Veil atualiza automaticamente as sugestões de ação rápida exibidas na interface:

1. **Geração Unificada Sem Custo Extra:** As sugestões (entre 3 e 5) são geradas na mesma chamada à API do Gemini utilizando uma seção estruturada `===SUGESTÕES DE AÇÃO===`, evitando latência duplicada e sem custo adicional de requisições.
2. **Higienização de Prosa:** O backend (`ExtratorSugestoesNarrativas`) separa rigorosamente a narrativa pura (enviada ao histórico da cena) do bloco de ações.
3. **Fallback Contextual Inteligente:** Caso a resposta da IA não forneça o bloco ou traga menos de 3 opções, o `GeradorSugestoesContextuais` deriva opções contextuais específicas analisando personagens presentes na cena, pistas descobertas, local atual e histórico recente, garantindo que o jogador nunca fique sem alternativas válidas.
4. **Execução com 1 Clique:** No frontend, clicar em uma sugestão preenche o campo e dispara imediatamente a ação pelo fluxo oficial com idempotência e persistência, reabilitando opções e preservando o texto caso ocorra qualquer falha de rede.
