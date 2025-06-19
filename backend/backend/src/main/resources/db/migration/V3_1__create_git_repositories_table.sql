-- Create git_repositories table
CREATE TABLE git_repositories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    scenario_id VARCHAR(255) NOT NULL,
    current_state TEXT,
    current_branch VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_git_repositories_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_git_repositories_user_id ON git_repositories(user_id);
CREATE INDEX idx_git_repositories_scenario_id ON git_repositories(scenario_id);
CREATE INDEX idx_git_repositories_user_scenario ON git_repositories(user_id, scenario_id);
CREATE INDEX idx_git_repositories_active ON git_repositories(is_active);
CREATE INDEX idx_git_repositories_updated_at ON git_repositories(updated_at); 