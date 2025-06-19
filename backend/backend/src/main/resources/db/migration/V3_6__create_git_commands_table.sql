-- Create git_commands table
CREATE TABLE git_commands (
    id BIGSERIAL PRIMARY KEY,
    command TEXT NOT NULL,
    output TEXT,
    error_output TEXT,
    successful BOOLEAN NOT NULL DEFAULT true,
    exit_code INTEGER NOT NULL,
    user_id BIGINT NOT NULL,
    scenario_id VARCHAR(255),
    step_number INTEGER,
    repository_id BIGINT NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_git_commands_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_commands_repository_id FOREIGN KEY (repository_id) REFERENCES git_repositories(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_git_commands_user_id ON git_commands(user_id);
CREATE INDEX idx_git_commands_repository_id ON git_commands(repository_id);
CREATE INDEX idx_git_commands_scenario_id ON git_commands(scenario_id);
CREATE INDEX idx_git_commands_executed_at ON git_commands(executed_at);
CREATE INDEX idx_git_commands_successful ON git_commands(successful);
CREATE INDEX idx_git_commands_command ON git_commands(command); 