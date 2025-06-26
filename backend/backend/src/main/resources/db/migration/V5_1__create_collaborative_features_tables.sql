-- Create ENUM types for PostgreSQL
CREATE TYPE workspace_visibility AS ENUM ('PRIVATE', 'PUBLIC', 'INVITE_ONLY');
CREATE TYPE workspace_member_role AS ENUM ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER');
CREATE TYPE conversation_permission AS ENUM ('VIEW_ONLY', 'COMMENT_ONLY', 'FULL_ACCESS');
CREATE TYPE reaction_type AS ENUM ('LIKE', 'LOVE', 'HELPFUL', 'INSIGHTFUL', 'BOOKMARK', 'THUMBS_UP', 'THUMBS_DOWN');

-- Create team workspaces table
CREATE TABLE team_workspaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    owner_id BIGINT NOT NULL,
    visibility workspace_visibility NOT NULL DEFAULT 'PRIVATE',
    invite_code VARCHAR(20) UNIQUE,
    max_members INT DEFAULT 10,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for team_workspaces
CREATE INDEX idx_workspace_owner ON team_workspaces(owner_id);
CREATE INDEX idx_workspace_invite_code ON team_workspaces(invite_code);
CREATE INDEX idx_workspace_visibility ON team_workspaces(visibility);

-- Create team workspace members table
CREATE TABLE team_workspace_members (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role workspace_member_role NOT NULL DEFAULT 'MEMBER',
    is_active BOOLEAN DEFAULT TRUE,
    invited_by BIGINT,
    joined_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (workspace_id) REFERENCES team_workspaces(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (workspace_id, user_id)
);

-- Create indexes for team_workspace_members
CREATE INDEX idx_member_workspace ON team_workspace_members(workspace_id);
CREATE INDEX idx_member_user ON team_workspace_members(user_id);
CREATE INDEX idx_member_role ON team_workspace_members(role);

-- Create shared conversations table
CREATE TABLE shared_conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    shared_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    permission conversation_permission NOT NULL DEFAULT 'VIEW_ONLY',
    is_pinned BOOLEAN DEFAULT FALSE,
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (workspace_id) REFERENCES team_workspaces(id) ON DELETE CASCADE,
    FOREIGN KEY (shared_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for shared_conversations
CREATE INDEX idx_shared_conversation ON shared_conversations(conversation_id);
CREATE INDEX idx_shared_workspace ON shared_conversations(workspace_id);
CREATE INDEX idx_shared_by ON shared_conversations(shared_by);
CREATE INDEX idx_shared_pinned ON shared_conversations(is_pinned);

-- Create conversation comments table
CREATE TABLE conversation_comments (
    id BIGSERIAL PRIMARY KEY,
    shared_conversation_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_comment_id BIGINT,
    is_edited BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shared_conversation_id) REFERENCES shared_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES conversation_comments(id) ON DELETE CASCADE
);

-- Create indexes for conversation_comments
CREATE INDEX idx_comment_shared_conversation ON conversation_comments(shared_conversation_id);
CREATE INDEX idx_comment_author ON conversation_comments(author_id);
CREATE INDEX idx_comment_parent ON conversation_comments(parent_comment_id);

-- Create conversation reactions table
CREATE TABLE conversation_reactions (
    id BIGSERIAL PRIMARY KEY,
    shared_conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type reaction_type NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shared_conversation_id) REFERENCES shared_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (shared_conversation_id, user_id, reaction_type)
);

-- Create indexes for conversation_reactions
CREATE INDEX idx_reaction_shared_conversation ON conversation_reactions(shared_conversation_id);
CREATE INDEX idx_reaction_user ON conversation_reactions(user_id);
CREATE INDEX idx_reaction_type ON conversation_reactions(reaction_type); 