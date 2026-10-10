# Banco de Dados — Crimson Veil (Marco 1)

## Tecnologia e arquivo local

O backend usa SQLite por meio de Xerial SQLite JDBC `3.49.1.0`. SQLite não exige servidor, conta ou serviço pago. O padrão é:

```text
backend/data/crimson-veil.sqlite
```

O caminho pode ser substituído pela variável `CRIMSON_VEIL_DB`. O diretório pai do caminho personalizado deve existir. Os arquivos de banco e auxiliares `-wal`/`-shm` são ignorados pelo Git; não os apague nem os versione.

## Esquema implantado

O esquema inicial está em `backend/src/main/resources/schema.sql` e é criado automaticamente ao iniciar a aplicação.

### `campaigns`

| Coluna | Tipo | Restrições / finalidade |
| --- | --- | --- |
| `id` | TEXT | Chave primária; preserva IDs como `camp-001` |
| `title`, `code`, `status` | TEXT | Metadados para listagem |
| `campaign_json` | TEXT | Snapshot completo da campanha, incluindo protagonista, caso, cena, mensagens, pistas, evidências, timeline, `eventLog` e mundo vivo |
| `version` | INTEGER | Versão otimista, inicia em 1 e aumenta uma vez por mutação confirmada |
| `created_at`, `updated_at` | TEXT | Instantes ISO-8601 de criação e última alteração |

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

## Integridade e transações

Criação e mutações são transacionais: snapshot, estado do mundo, eventos, operação idempotente e histórico são gravados integralmente ou sofrem rollback. `version` mais a versão esperada na requisição rejeitam gravações obsoletas com HTTP `409`. IDs de protagonista, Caso 001 e dados de pistas/evidências já existentes são preservados nas atualizações.

## Cânone e migração inicial

Na primeira abertura, o frontend consulta a API. Somente se a lista estiver vazia envia o snapshot já montado por `criarCampanhaCanonicaInicial()` para `POST /api/v1/campanhas/migracao-canonica`. O backend aceita somente `camp-001`, `char-milena`/Milena Ramires e `caso-001`; cria essa campanha uma única vez. Em chamadas seguintes, o servidor devolve a campanha do SQLite sem comparar ou substituir com os mocks. As IDs `cena-01`, `PISTA-*`, `EVID-*` e `EV-001-*` são mantidas pelo snapshot canônico atual.

Campanhas criadas pelo formulário existente são enviadas integralmente à API. O banco não é preenchido com catálogos duplicados; dados de pessoas, locais, timeline, conexões e dossiês continuam no agregado compatível com o frontend.

## Preservação e evolução

Faça cópia do arquivo SQLite com o backend parado. Preserve também a cópia antes de atualizar código ou esquema. `schema.sql` atende a instalação inicial; não altera nem recria tabelas existentes. Evoluções incompatíveis devem vir acompanhadas de migrações versionadas e backup testado. Não há migração automática de progresso prévio do navegador: o protótipo anterior mantinha estado apenas em memória, sem armazenamento recuperável.
