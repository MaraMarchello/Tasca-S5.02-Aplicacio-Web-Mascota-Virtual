-- AI Conversations and Messages Tables
-- V4_1__create_ai_conversations_table.sql

-- Create AI conversations table
CREATE TABLE ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    context_type VARCHAR(50) DEFAULT 'general', -- 'general', 'debug', 'explain', 'refactor', 'generate'
    programming_language VARCHAR(50) DEFAULT 'java',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    
    CONSTRAINT fk_ai_conversations_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create AI messages table
CREATE TABLE ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL, -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    code_snippet TEXT,
    programming_language VARCHAR(50),
    context_data JSONB, -- Store additional context like code editor content, error details, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_ai_messages_conversation_id 
        FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_ai_conversations_user_id ON ai_conversations(user_id);
CREATE INDEX idx_ai_conversations_updated_at ON ai_conversations(updated_at DESC);
CREATE INDEX idx_ai_conversations_active ON ai_conversations(is_active, user_id);

CREATE INDEX idx_ai_messages_conversation_id ON ai_messages(conversation_id);
CREATE INDEX idx_ai_messages_created_at ON ai_messages(created_at DESC);
CREATE INDEX idx_ai_messages_type ON ai_messages(message_type);

-- Add some useful comments
COMMENT ON TABLE ai_conversations IS 'Stores AI conversation sessions with context and metadata';
COMMENT ON TABLE ai_messages IS 'Stores individual messages within AI conversations';
COMMENT ON COLUMN ai_conversations.context_type IS 'The type of AI assistance: general, debug, explain, refactor, generate';
COMMENT ON COLUMN ai_messages.context_data IS 'JSON data for storing additional context like code editor state, error details, etc.'; 