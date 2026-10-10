# Guia de Execução Local — Crimson Veil

## Requisitos

- Windows 10/11.
- JDK 21 disponível no `PATH`.
- Node.js 20 ou superior para os testes JavaScript.
- A primeira execução do Maven Wrapper precisa de internet para baixar Maven e dependências públicas gratuitas. Depois, os artefatos ficam no cache local do Maven.

Não há serviço de hospedagem, banco remoto, chave, conta ou dependência paga.

## Iniciar no Windows

Abra PowerShell na raiz do repositório e execute:

```powershell
$javaExe = (Get-Command java).Source
$env:JAVA_HOME = Split-Path (Split-Path $javaExe)
Set-Location backend
.\mvnw.cmd spring-boot:run
```

O Maven Wrapper usa Maven 3.9.9 e o backend inicia em `http://localhost:8080`. O próprio Spring serve `frontend/index.html`, CSS, JavaScript e assets; abra esse endereço no navegador. Não abra o HTML por `file://`, pois módulos e chamadas HTTP exigem uma origem HTTP.

Para criar o pacote:

```powershell
.\mvnw.cmd package
```

## Testes

Na raiz, os testes do motor temporal e do cliente HTTP do frontend:

```powershell
node --test test/*.test.js
```

Em `backend\`, os testes REST com SQLite temporário:

```powershell
.\mvnw.cmd test
```

Os testes do cliente do celular também fazem parte da suíte JavaScript. A rota de recebimento artificial é habilitada exclusivamente no perfil Spring `development`; para teste manual local, inicie explicitamente com:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'development'
.\mvnw.cmd spring-boot:run
```

Não ative esse perfil em ambientes compartilhados ou de produção. A rota de recebimento de teste não é chamada pelo frontend normal.

## Banco e preservação dos dados

O banco padrão fica em `backend\data\crimson-veil.sqlite`. O diretório deve existir; já está incluído no repositório. O banco e seus arquivos `-wal`/`-shm` são ignorados pelo Git.

Para escolher outro arquivo, defina `CRIMSON_VEIL_DB` antes de iniciar o processo; crie previamente o diretório pai:

```powershell
$env:CRIMSON_VEIL_DB = 'D:/CrimsonVeilData/crimson-veil.sqlite'
.\mvnw.cmd spring-boot:run
```

Não apague o arquivo para “reiniciar” o jogo. Para backup consistente, pare o backend, copie o arquivo SQLite para um local seguro e reinicie. Ao atualizar o esquema em marcos futuros, faça outro backup antes da migração.

## Migração dos mocks canônicos

1. O frontend pede a lista de campanhas persistidas ao entrar.
2. Se a lista estiver vazia, envia uma única vez o objeto `camp-001` produzido pelo construtor canônico existente.
3. O backend valida `camp-001`, Milena Ramires (`char-milena`) e o Caso 001 (`caso-001`) e persiste o snapshot completo, incluindo `cena-01`, mensagens, pistas, evidências, eventos, memória e mundo vivo.
4. Se a campanha já existir, a rota de migração devolve o estado do banco sem modificar nada. Os mocks passam a ser apenas catálogo/base para construir histórias novas; não podem regravar a campanha canônica.
5. Cada ação altera uma cópia local, envia versão esperada e chave idempotente e só atualiza a interface após confirmação do backend. Uma falha mantém a ação e o estado em memória sem declarar sucesso; a repetição usa a mesma chave.

O protótipo anterior não escrevia campanhas no navegador. Portanto, não existe histórico oculto de sessões antigas para importar automaticamente; a migração recupera os dados canônicos já versionados no projeto, não progresso volátil perdido ao fechar/recarregar a página.

## API disponível

- `GET /api/v1/campanhas`
- `GET /api/v1/campanhas/{id}`
- `POST /api/v1/campanhas`
- `POST /api/v1/campanhas/migracao-canonica`
- `POST /api/v1/campanhas/{id}/acoes`
- `POST /api/v1/campanhas/{id}/reiniciar` — reinicia somente o progresso dessa campanha a partir do snapshot inicial, com `chaveOperacao`, `versaoEsperada` e `campanhaInicial` para campanhas antigas sem baseline persistido.
- `PUT /api/v1/campanhas/{id}/estado`
- `GET /api/v1/campanhas/{id}/historico`
- `GET /api/v1/personagens/{id}/perfil` — dados compartilhados do perfil e limite de imagem.
- `GET /api/v1/personagens/{id}/imagem` — imagem personalizada (resposta binária).
- `PUT /api/v1/personagens/{id}/imagem` — upload multipart na parte `arquivo`.
- `DELETE /api/v1/personagens/{id}/imagem` — remove a imagem e restaura o avatar padrão.
- `GET /api/v1/campanhas/{id}/celular/contatos` — contatos da protagonista persistida.
- `POST /api/v1/campanhas/{id}/celular/contatos` — cria contato com `nome`, `categoria`, `chaveOperacao` e `versaoEsperada`; aceita opcionalmente `personagemCanonicoId`.
- `GET /api/v1/campanhas/{id}/celular/contatos/{contatoId}/mensagens` — histórico persistido da conversa.
- `POST /api/v1/campanhas/{id}/celular/contatos/{contatoId}/mensagens` — envia mensagem com `conteudo`, `chaveOperacao` e `versaoEsperada`.
- `PATCH /api/v1/campanhas/{id}/celular/conversas/{conversaId}/leitura` — marca recebidas como lidas com `versaoEsperada`.
- `POST /api/v1/campanhas/{id}/celular/contatos/{contatoId}/mensagens-recebidas-teste` — recebe mensagem controlada **somente** com perfil `development`; não é parte do fluxo normal.

Contatos e mensagens ficam nas tabelas relacionais do celular, associados ao ID da campanha e ao ID da protagonista do snapshot. Eles não são copiados para `campaign_json`. Cada mutação usa a versão da campanha e chave idempotente; um `409` é conflito, não confirmação. A data e hora da mensagem usam o relógio persistido e o celular não avança o tempo ficcional.

Na aba **Visão Geral**, cada campanha oferece **Reiniciar campanha** e **Nova campanha com esta personagem**. O primeiro confirma o nome da campanha/personagem, apaga somente o progresso vinculado a esse ID e mantém perfil, atributos e imagem. O segundo abre uma história nova com a mesma personagem, sem alterar a anterior. No cabeçalho, selecione o bloco da personagem para escolher uma imagem, conferir a prévia e salvar ou voltar ao avatar padrão. JPEG, PNG e WebP são aceitos; o padrão é 2 MiB. Para ajustar o limite no Windows antes de iniciar o backend:

```powershell
$env:CRIMSON_VEIL_MAX_PROFILE_IMAGE_BYTES = '4194304'
```

O perfil e a imagem são armazenados em `character_profiles` no SQLite. Snapshots de início das novas campanhas ficam em `campaign_initial_states`; o esquema é aditivo e não remove campanhas anteriores.

## Limitações desta etapa

- API destinada ao uso local: não há autenticação/autorização nem implantação multiusuário.
- A resposta narrativa continua usando o comportamento local já existente; não foi conectado motor narrativo novo ou IA.
- Ainda não existe autonomia completa dos NPCs nem modelagem normalizada de cada tipo de pista/evidência/personagem.
- A entrega inicial do celular persiste contatos e mensagens e permite envio; recebimento é determinístico e restrito ao perfil `development`. Não há mensagens espontâneas, agenda, chamadas, autonomia geral dos NPCs nem integração com IA.
- O esquema inicial é criado por `schema.sql`; alterações futuras precisam de estratégia de migração versionada antes de atualizar bancos existentes.
- A API é acessível no localhost, mas não deve ser exposta à rede pública sem autenticação, proteção CSRF/CORS adequada e revisão de segurança.

## Próximos passos

Criar migrações versionadas, evoluir o motor de ações para o serviço do backend, testar cópias/restauração do SQLite e só então avaliar autenticação e autonomia de NPCs. Integração com IA permanece fora deste marco.
