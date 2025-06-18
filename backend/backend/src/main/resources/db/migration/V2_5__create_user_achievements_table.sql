-- Create user achievements table for tracking progress
CREATE TABLE user_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    achievement_id BIGINT NOT NULL REFERENCES achievements(id),
    current_progress INTEGER NOT NULL DEFAULT 0 CHECK (current_progress >= 0),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, achievement_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_completed ON user_achievements(completed);
CREATE INDEX idx_user_achievements_user_completed ON user_achievements(user_id, completed);

-- Add comment for documentation
COMMENT ON TABLE user_achievements IS 'User progress tracking for achievements';
COMMENT ON COLUMN user_achievements.current_progress IS 'Current progress towards achievement completion';
COMMENT ON COLUMN user_achievements.completed_at IS 'Timestamp when achievement was completed'; 