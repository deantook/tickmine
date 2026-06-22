CREATE TABLE users (
    id              VARCHAR(100) PRIMARY KEY,
    subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ticktick_token_enc TEXT,
    token_status    VARCHAR(20) NOT NULL DEFAULT 'NOT_CONNECTED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE goals (
    id                  UUID PRIMARY KEY,
    user_id             VARCHAR(100) NOT NULL REFERENCES users(id),
    title               VARCHAR(255),
    description         TEXT,
    status              VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    phase               VARCHAR(50) NOT NULL DEFAULT 'COLLECTING',
    target_date         DATE,
    context             JSONB NOT NULL DEFAULT '{}',
    ticktick_project_id VARCHAR(100),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_goals_user_id ON goals(user_id);

CREATE TABLE conversations (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(100) NOT NULL REFERENCES users(id),
    goal_id     UUID REFERENCES goals(id),
    messages    JSONB NOT NULL DEFAULT '[]',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_conversations_user_goal ON conversations(user_id, goal_id);

CREATE TABLE plans (
    id          UUID PRIMARY KEY,
    goal_id     UUID NOT NULL REFERENCES goals(id),
    dsl         JSONB NOT NULL,
    version     INT NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_plans_goal_id ON plans(goal_id);

CREATE TABLE quota_usage (
    user_id     VARCHAR(100) NOT NULL REFERENCES users(id),
    usage_date  DATE NOT NULL,
    chat_count  INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date)
);

CREATE TABLE plan_executions (
    id              UUID PRIMARY KEY,
    plan_id         UUID NOT NULL REFERENCES plans(id),
    status          VARCHAR(20) NOT NULL,
    ticktick_refs   JSONB,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
