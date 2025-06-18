-- Create achievements table for gamification
CREATE TABLE achievements (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    target_value INTEGER NOT NULL CHECK (target_value > 0),
    points_reward BIGINT NOT NULL CHECK (points_reward > 0),
    badge_image_url VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_achievements_code ON achievements(code);
CREATE INDEX idx_achievements_active ON achievements(active);

-- Add comment for documentation
COMMENT ON TABLE achievements IS 'Achievement definitions for gamification system';
COMMENT ON COLUMN achievements.code IS 'Unique code used in application logic';
COMMENT ON COLUMN achievements.target_value IS 'Number of actions needed to complete achievement';

-- Insert 5 basic achievements for MVP
INSERT INTO achievements (code, name, description, target_value, points_reward) VALUES
('PET_OWNER', 'Pet Owner', 'Create your first virtual pet', 1, 100),
('PET_FEEDER', 'Pet Feeder', 'Feed your pet 10 times', 10, 300),
('SHOPPER', 'Shopper', 'Purchase 5 items from the shop', 5, 250),
('PROBLEM_SOLVER', 'Problem Solver', 'Resolve 5 stack trace errors', 5, 400),
('AI_USER', 'AI Assistant User', 'Use AI chat 20 times', 20, 500); 