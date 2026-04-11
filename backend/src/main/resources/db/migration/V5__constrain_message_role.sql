ALTER TABLE messages
    ADD CONSTRAINT chk_message_role CHECK (role IN ('user', 'assistant'));
