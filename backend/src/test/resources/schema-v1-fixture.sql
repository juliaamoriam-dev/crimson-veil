CREATE TABLE IF NOT EXISTS campaigns (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    code TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT '',
    campaign_json TEXT NOT NULL,
    version INTEGER NOT NULL CHECK (version >= 1),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS world_state (
    campaign_id TEXT PRIMARY KEY,
    world_date TEXT NOT NULL,
    world_time TEXT NOT NULL,
    world_location TEXT NOT NULL,
    action_count INTEGER NOT NULL CHECK (action_count >= 0),
    state_json TEXT NOT NULL,
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS campaign_events (
    campaign_id TEXT NOT NULL,
    event_id TEXT NOT NULL,
    event_status TEXT NOT NULL DEFAULT '',
    event_json TEXT NOT NULL,
    PRIMARY KEY (campaign_id, event_id),
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS campaign_operations (
    campaign_id TEXT NOT NULL,
    operation_key TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    action_text TEXT NOT NULL DEFAULT '',
    response_json TEXT NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY (campaign_id, operation_key),
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS campaign_history (
    campaign_id TEXT NOT NULL,
    operation_key TEXT NOT NULL,
    entry_type TEXT NOT NULL,
    description TEXT NOT NULL,
    world_date TEXT NOT NULL,
    world_time TEXT NOT NULL,
    campaign_version INTEGER NOT NULL CHECK (campaign_version >= 1),
    created_at TEXT NOT NULL,
    PRIMARY KEY (campaign_id, operation_key),
    FOREIGN KEY (campaign_id, operation_key)
        REFERENCES campaign_operations(campaign_id, operation_key) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_campaign_history_created
    ON campaign_history (campaign_id, created_at);
CREATE INDEX IF NOT EXISTS idx_campaign_events_status
    ON campaign_events (campaign_id, event_status);

INSERT INTO campaigns (
    id, title, code, status, campaign_json, version, created_at, updated_at
) VALUES (
    'camp-migration-fixture', 'Legacy campaign', 'OLD-001', 'ACTIVE',
    '{"id":"camp-migration-fixture","protagonista":{"id":"char-legacy","nome":"Legacy Hero"},"estadoMundo":{"dataAtual":"14 de Outubro de 2026","horarioAtual":"03:35","localAtual":"Legacy Location"},"contadorAcoes":1}',
    2, '2026-10-09T20:00:00Z', '2026-10-09T20:05:00Z'
);

INSERT INTO world_state (
    campaign_id, world_date, world_time, world_location, action_count, state_json
) VALUES (
    'camp-migration-fixture', '14 de Outubro de 2026', '03:35', 'Legacy Location', 1,
    '{"dataAtual":"14 de Outubro de 2026","horarioAtual":"03:35","localAtual":"Legacy Location","clima":"Chuva"}'
);

INSERT INTO campaign_events (campaign_id, event_id, event_status, event_json)
VALUES (
    'camp-migration-fixture', 'EV-LEGACY-001', 'AGENDADO',
    '{"id":"EV-LEGACY-001","status":"AGENDADO","horarioPrevisto":"03:50"}'
);

INSERT INTO campaign_operations (
    campaign_id, operation_key, operation_type, action_text, response_json, created_at
) VALUES (
    'camp-migration-fixture', 'legacy-op-1', 'ACAO_JOGADOR', 'Legacy action',
    '{"campanha":{"id":"camp-migration-fixture"},"versao":2,"repetida":false}',
    '2026-10-09T20:05:00Z'
);

INSERT INTO campaign_history (
    campaign_id, operation_key, entry_type, description, world_date, world_time, campaign_version, created_at
) VALUES (
    'camp-migration-fixture', 'legacy-op-1', 'ACAO_JOGADOR', 'Legacy history entry',
    '14 de Outubro de 2026', '03:35', 2, '2026-10-09T20:05:00Z'
);
