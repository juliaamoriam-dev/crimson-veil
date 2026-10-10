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

CREATE TABLE IF NOT EXISTS character_profiles (
    character_id TEXT PRIMARY KEY,
    profile_json TEXT NOT NULL,
    image_mime_type TEXT CHECK (
        image_mime_type IS NULL OR image_mime_type IN ('image/jpeg', 'image/png', 'image/webp')
    ),
    image_data BLOB,
    updated_at TEXT NOT NULL,
    CHECK ((image_mime_type IS NULL) = (image_data IS NULL))
);

CREATE TABLE IF NOT EXISTS campaign_initial_states (
    campaign_id TEXT PRIMARY KEY,
    initial_json TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS world_state (
    campaign_id TEXT PRIMARY KEY,
    world_date TEXT NOT NULL,
    world_time TEXT NOT NULL CHECK (
        length(world_time) = 5
        AND substr(world_time, 1, 2) BETWEEN '00' AND '23'
        AND substr(world_time, 3, 1) = ':'
        AND substr(world_time, 4, 2) BETWEEN '00' AND '59'
    ),
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

CREATE TABLE IF NOT EXISTS campaign_contacts (
    contact_id TEXT PRIMARY KEY,
    campaign_id TEXT NOT NULL,
    protagonist_id TEXT NOT NULL,
    canonical_character_id TEXT,
    display_name TEXT NOT NULL CHECK (length(trim(display_name)) > 0),
    category TEXT NOT NULL CHECK (category IN ('PESSOAL', 'PROFISSIONAL')),
    creation_key TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (campaign_id, protagonist_id, contact_id),
    UNIQUE (campaign_id, protagonist_id, creation_key),
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_campaign_contacts_canonical_character
    ON campaign_contacts (campaign_id, protagonist_id, canonical_character_id)
    WHERE canonical_character_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_campaign_contacts_owner
    ON campaign_contacts (campaign_id, protagonist_id, display_name, contact_id);

CREATE TABLE IF NOT EXISTS campaign_conversations (
    conversation_id TEXT PRIMARY KEY,
    campaign_id TEXT NOT NULL,
    protagonist_id TEXT NOT NULL,
    contact_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (campaign_id, protagonist_id, contact_id),
    UNIQUE (conversation_id, campaign_id, protagonist_id, contact_id),
    FOREIGN KEY (campaign_id, protagonist_id, contact_id)
        REFERENCES campaign_contacts (campaign_id, protagonist_id, contact_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_campaign_conversations_owner
    ON campaign_conversations (campaign_id, protagonist_id, contact_id);

CREATE TABLE IF NOT EXISTS campaign_messages (
    message_id INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id TEXT NOT NULL,
    protagonist_id TEXT NOT NULL,
    conversation_id TEXT NOT NULL,
    contact_id TEXT NOT NULL,
    direction TEXT NOT NULL CHECK (direction IN ('ENTRADA', 'SAIDA')),
    body TEXT NOT NULL CHECK (length(trim(body)) > 0),
    world_date TEXT NOT NULL,
    world_time TEXT NOT NULL CHECK (
        length(world_time) = 5
        AND substr(world_time, 1, 2) BETWEEN '00' AND '23'
        AND substr(world_time, 3, 1) = ':'
        AND substr(world_time, 4, 2) BETWEEN '00' AND '59'
    ),
    read_at TEXT,
    idempotency_key TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (campaign_id, protagonist_id, idempotency_key),
    FOREIGN KEY (campaign_id, protagonist_id, contact_id)
        REFERENCES campaign_contacts (campaign_id, protagonist_id, contact_id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id, campaign_id, protagonist_id, contact_id)
        REFERENCES campaign_conversations (conversation_id, campaign_id, protagonist_id, contact_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_campaign_messages_conversation_order
    ON campaign_messages (conversation_id, message_id);
CREATE INDEX IF NOT EXISTS idx_campaign_messages_unread
    ON campaign_messages (conversation_id, direction, read_at, message_id);
