-- Create git_user_progress table
CREATE TABLE git_user_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    scenario_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'ABANDONED')),
    current_step INTEGER NOT NULL DEFAULT 0,
    total_steps INTEGER NOT NULL DEFAULT 0,
    commands_executed INTEGER NOT NULL DEFAULT 0,
    hints_used INTEGER NOT NULL DEFAULT 0,
    points_earned INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    progress_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_git_user_progress_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_git_user_progress_scenario_id FOREIGN KEY (scenario_id) REFERENCES git_scenarios(id) ON DELETE CASCADE,
    UNIQUE(user_id, scenario_id)
);

-- Create indexes for better performance
CREATE INDEX idx_git_user_progress_user_id ON git_user_progress(user_id);
CREATE INDEX idx_git_user_progress_scenario_id ON git_user_progress(scenario_id);
CREATE INDEX idx_git_user_progress_status ON git_user_progress(status);
CREATE INDEX idx_git_user_progress_completed_at ON git_user_progress(completed_at);
CREATE INDEX idx_git_user_progress_updated_at ON git_user_progress(updated_at); 