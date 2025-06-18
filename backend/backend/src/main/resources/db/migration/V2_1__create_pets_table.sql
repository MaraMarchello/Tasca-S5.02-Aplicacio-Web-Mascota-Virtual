-- Create pets table with simplified structure for MVP
CREATE TABLE pets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    happiness INTEGER NOT NULL DEFAULT 100 CHECK (happiness >= 0 AND happiness <= 100),
    total_points_earned BIGINT NOT NULL DEFAULT 0,
    last_fed TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_pets_user_id ON pets(user_id);
CREATE INDEX idx_pets_type ON pets(type);
CREATE INDEX idx_pets_created_at ON pets(created_at);

-- Add comment for documentation
COMMENT ON TABLE pets IS 'Virtual pets owned by users - simplified MVP version';
COMMENT ON COLUMN pets.happiness IS 'Pet happiness level (0-100)';
COMMENT ON COLUMN pets.total_points_earned IS 'Total points earned by the pet owner'; 