-- Create pet items table for inventory management
CREATE TABLE pet_items (
    id BIGSERIAL PRIMARY KEY,
    pet_id BIGINT NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    item_template_id BIGINT NOT NULL REFERENCES item_templates(id),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    equipped BOOLEAN NOT NULL DEFAULT FALSE,
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_pet_items_pet_id ON pet_items(pet_id);
CREATE INDEX idx_pet_items_template_id ON pet_items(item_template_id);
CREATE INDEX idx_pet_items_equipped ON pet_items(equipped);

-- Add unique constraint to prevent duplicate items per pet
CREATE UNIQUE INDEX idx_pet_items_unique ON pet_items(pet_id, item_template_id);

-- Add comment for documentation
COMMENT ON TABLE pet_items IS 'Items owned by pets - simplified inventory system';
COMMENT ON COLUMN pet_items.equipped IS 'Whether this accessory item is currently equipped'; 