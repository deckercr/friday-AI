-- Required for gen_random_uuid() on PostgreSQL < 13; harmless on 13+
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- Required for the pgvector vector store used by ConversationMemoryService
CREATE EXTENSION IF NOT EXISTS "vector";

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
