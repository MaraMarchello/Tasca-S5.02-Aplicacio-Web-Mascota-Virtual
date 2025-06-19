-- Create git_scenarios table
CREATE TABLE git_scenarios (
    id BIGSERIAL PRIMARY KEY,
    scenario_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    level VARCHAR(50) NOT NULL CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    category VARCHAR(50) NOT NULL CHECK (category IN ('BASICS', 'BRANCHING', 'MERGING', 'CONFLICTS', 'COLLABORATION', 'ADVANCED_WORKFLOWS')),
    initial_state TEXT,
    expected_commands TEXT,
    success_criteria TEXT,
    points_reward INTEGER NOT NULL DEFAULT 0,
    estimated_minutes INTEGER NOT NULL DEFAULT 10,
    is_active BOOLEAN NOT NULL DEFAULT true,
    order_index INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create git_scenario_tags table for tags
CREATE TABLE git_scenario_tags (
    scenario_id BIGINT NOT NULL,
    tag VARCHAR(100) NOT NULL,
    
    CONSTRAINT fk_git_scenario_tags_scenario_id FOREIGN KEY (scenario_id) REFERENCES git_scenarios(id) ON DELETE CASCADE,
    PRIMARY KEY (scenario_id, tag)
);

-- Create indexes for better performance
CREATE INDEX idx_git_scenarios_scenario_id ON git_scenarios(scenario_id);
CREATE INDEX idx_git_scenarios_level ON git_scenarios(level);
CREATE INDEX idx_git_scenarios_category ON git_scenarios(category);
CREATE INDEX idx_git_scenarios_active ON git_scenarios(is_active);
CREATE INDEX idx_git_scenarios_order ON git_scenarios(order_index);
CREATE INDEX idx_git_scenario_tags_tag ON git_scenario_tags(tag); 