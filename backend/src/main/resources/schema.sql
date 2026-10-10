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
