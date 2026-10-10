# Banco de Dados — Crimson Veil

## Tecnologia e arquivo local

O backend usa SQLite por meio de Xerial SQLite JDBC `3.49.1.0`. SQLite não exige servidor, conta ou serviço pago. O caminho padrão é relativo ao diretório de trabalho do processo. Seguindo os comandos documentados, que iniciam o Spring a partir de `backend`, o arquivo fica em:

```text
backend/data/crimson-veil.sqlite
```

O caminho pode ser substituído pela variável `CRIMSON_VEIL_DB`. O diretório pai do caminho personalizado deve existir. Os arquivos de banco e auxiliares `-wal`/`-shm` são ignorados pelo Git; não os apague nem os versione.

Se o backend for iniciado a partir de outro diretório, `./data/crimson-veil.sqlite` apontará para outro local. Para manter o mesmo banco fora do fluxo documentado, configure `CRIMSON_VEIL_DB` com um caminho absoluto.

## Esquema implantado

O esquema inicial e as extensões aditivas estão em `backend/src/main/resources/schema.sql` e são aplicados automaticamente ao iniciar a aplicação. As novas tabelas usam `CREATE TABLE/INDEX IF NOT EXISTS`; as tabelas anteriores não são recriadas.

### `campaigns`

| Coluna | Tipo | Restrições / finalidade |
| --- | --- | --- |
| `id` | TEXT | Chave primária; preserva IDs como `camp-001` |
| `title`, `code`, `status` | TEXT | Metadados para listagem |
| `campaign_json` | TEXT | Snapshot da campanha e dos dados narrativos já existentes, incluindo protagonista, caso, cena, pistas, evidências, timeline, `eventLog` e mundo vivo. Contatos e mensagens do celular ficam somente nas tabelas relacionais abaixo. |
| `version` | INTEGER | Versão otimista, inicia em 1 e aumenta uma vez por mutação confirmada |
| `created_at`, `updated_at` | TEXT | Instantes ISO-8601 de criação e última alteração |

### `character_profiles` e `campaign_initial_states`

`character_profiles` separa a identidade persistente da personagem do snapshot de uma campanha. `character_id` é chave primária compartilhável por várias campanhas; `profile_json` mantém nome, aparência e atributos; `image_mime_type`/`image_data` guardam a imagem validada como BLOB no SQLite. A imagem aceita JPEG, PNG ou WebP e o limite padrão é 2 MiB, configurável por `CRIMSON_VEIL_MAX_PROFILE_IMAGE_BYTES`. Não são gravados caminhos do computador nem URLs temporárias.

`campaign_initial_states` guarda o snapshot da abertura oficial de cada campanha criada após esta alteração. Assim, o backend reinicia campanhas usando o mesmo estado inicial recebido na criação, sem reconstruir narrativa ou apagar a campanha. Para campanhas antigas sem snapshot inicial, a primeira solicitação de reinício fornece o estado criado pela fábrica oficial do frontend; o backend valida ID e protagonista e persiste esse baseline para os próximos reinícios.

Uma nova campanha pode referenciar o mesmo `character_id`: os snapshots narrativos, relógio, pistas, eventos, histórico e dados do celular permanecem limitados ao ID da campanha. A imagem e os demais dados do perfil são compartilhados.

### `world_state`

Uma linha por campanha, com chave estrangeira `campaign_id` para `campaigns`:

| Coluna | Tipo | Restrições / finalidade |
| --- | --- | --- |
| `world_date` | TEXT | Data ficcional atual |
| `world_time` | TEXT | Horário `HH:mm`, validado também por restrição SQL |
| `world_location` | TEXT | Local atual |
| `action_count` | INTEGER | Contador não negativo |
| `state_json` | TEXT | Campos dinâmicos restantes do estado do mundo |

Ao carregar, data, horário, localização e contador são hidratados das colunas dessa tabela, que é a fonte persistida desses campos.

### `campaign_events`

Chave primária composta (`campaign_id`, `event_id`) impede linhas duplicadas do mesmo evento na campanha. Eventos agendados e ocorridos mantêm seus identificadores canônicos; a linha é atualizada quando muda o status.

### `campaign_operations`

Chave primária composta (`campaign_id`, `operation_key`) torna ações e atualizações idempotentes. Guarda o tipo, a ação original, a resposta confirmada e o instante. Uma repetição retorna o resultado já salvo, sem avançar o relógio, contador ou histórico novamente.

### `campaign_history`

Histórico mínimo de auditoria: tipo e descrição da operação, data/horário do mundo, versão e instante. A chave estrangeira composta referencia a operação correspondente. É consultável pela rota `GET /api/v1/campanhas/{id}/historico`.

### Celular: `campaign_contacts`, `campaign_conversations` e `campaign_messages`

Os dados do celular são relacionais e não são serializados em `campaign_json`. Todas as consultas e chaves estrangeiras de celular são limitadas à campanha e à protagonista.

| Tabela | Dados e integridade |
| --- | --- |
| `campaign_contacts` | Um contato por ID; guarda `campaign_id`, `protagonist_id`, nome, categoria (`PESSOAL` ou `PROFISSIONAL`), chave de criação idempotente e `canonical_character_id` opcional para associação futura com um NPC estável. Índices evitam repetir a chave de criação e associar o mesmo NPC canônico duas vezes à mesma protagonista da campanha. |
| `campaign_conversations` | Uma conversa individual por contato, campanha e protagonista. A chave estrangeira composta assegura que o contato pertença ao mesmo escopo. |
| `campaign_messages` | Mensagens recebidas/enviadas, conteúdo, data e hora ficcionais, instante técnico, leitura e chave idempotente. Chaves estrangeiras compostas garantem que conversa, contato, campanha e protagonista coincidam. |

Índices ordenam o histórico por conversa e identificador e apoiam a contagem de mensagens recebidas não lidas. A exclusão de uma campanha remove os registros dependentes por `ON DELETE CASCADE`; contatos e mensagens não são armazenados em duplicidade no snapshot.

## Integridade e transações

Criação e mutações são transacionais: snapshot, estado do mundo, eventos, operação idempotente e histórico são gravados integralmente ou sofrem rollback. `version` mais a versão esperada na requisição rejeitam gravações obsoletas com HTTP `409`. Operações de celular também comparam e atualizam essa versão dentro da transação; mensagens repetidas com a mesma chave e conteúdo devolvem o registro existente, e reutilização da chave com payload diferente é conflito. O relógio ficcional da mensagem é lido do `world_state` persistido; enviar ou ler mensagens não avança o relógio. IDs de protagonista, Caso 001 e dados de pistas/evidências já existentes são preservados nas atualizações.

## Verificação automatizada da persistência

`PersistenciaReinicioIntegracaoTest` usa um SQLite exclusivo dentro do diretório temporário do JUnit. O teste grava uma campanha e altera o relógio, estado narrativo, pistas, evidências, evento e histórico; também persiste contato e mensagem. Em seguida, fecha o primeiro contexto Spring, abre outro contra o mesmo arquivo e verifica a recuperação de todos esses dados. Nenhum teste da suíte usa o banco local `backend/data/crimson-veil.sqlite`.

`POST /api/v1/campanhas/{id}/reiniciar` verifica versão e chave idempotente, limpa eventos, contatos, conversas, mensagens, operações e histórico daquela campanha, restaura snapshot e mundo iniciais e reinsere os eventos de abertura. A chave técnica de idempotência é mantida sem recriar o histórico narrativo. Tudo ocorre na mesma transação; uma falha desfaz a limpeza e a restauração. A versão avança para rejeitar gravações antigas. Perfil e imagem não são limpos.

Para alterar a imagem, `GET /api/v1/personagens/{id}/perfil` informa o limite configurado; `PUT /api/v1/personagens/{id}/imagem` recebe multipart (`arquivo`) e `DELETE` restaura o avatar padrão. Os bytes ficam no BLOB da personagem e são servidos por `GET /api/v1/personagens/{id}/imagem`.

## Cânone e migração inicial

Na primeira abertura, o frontend consulta a API. Somente se a lista estiver vazia envia o snapshot já montado por `criarCampanhaCanonicaInicial()` para `POST /api/v1/campanhas/migracao-canonica`. O backend aceita somente `camp-001`, `char-milena`/Milena Ramires e `caso-001`; cria essa campanha uma única vez. Em chamadas seguintes, o servidor devolve a campanha do SQLite sem comparar ou substituir com os mocks. As IDs `cena-01`, `PISTA-*`, `EVID-*` e `EV-001-*` são mantidas pelo snapshot canônico atual. A migração inicial não semeia contatos nem mensagens de celular: são criados e persistidos pelo recurso do celular.

Campanhas criadas pelo formulário existente são enviadas integralmente à API. O banco não é preenchido com catálogos duplicados; dados de pessoas, locais, timeline, conexões e dossiês continuam no agregado compatível com o frontend.

## Preservação e evolução

Faça cópia do arquivo SQLite com o backend parado. Preserve também a cópia antes de atualizar código ou esquema. `schema.sql` atende a instalação inicial; não altera nem recria tabelas existentes. Evoluções incompatíveis devem vir acompanhadas de migrações versionadas e backup testado. Não há migração automática de progresso prévio do navegador: o protótipo anterior mantinha estado apenas em memória, sem armazenamento recuperável.
