-- Create item templates table for shop items
CREATE TABLE item_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    type VARCHAR(20) NOT NULL CHECK (type IN ('FOOD', 'ACCESSORY')),
    price BIGINT NOT NULL CHECK (price > 0),
    image_url VARCHAR(255),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    happiness_boost INTEGER DEFAULT 0 CHECK (happiness_boost >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_item_templates_type ON item_templates(type);
CREATE INDEX idx_item_templates_available ON item_templates(available);
CREATE INDEX idx_item_templates_price ON item_templates(price);

-- Add comment for documentation
COMMENT ON TABLE item_templates IS 'Template definitions for purchasable pet items';
COMMENT ON COLUMN item_templates.happiness_boost IS 'Amount of happiness this item provides when used';

-- Insert basic items for MVP
INSERT INTO item_templates (name, description, type, price, happiness_boost) VALUES
('Coffee Bean', 'A tasty coffee bean that makes pets happy', 'FOOD', 50, 20),
('Energy Drink', 'High-energy drink for pets', 'FOOD', 75, 30),
('Healthy Snack', 'Nutritious snack that pets love', 'FOOD', 60, 25),
('Coding Hat', 'A stylish hat for your pet', 'ACCESSORY', 200, 10),
('Bow Tie', 'Elegant bow tie for special occasions', 'ACCESSORY', 300, 15),
('Glasses', 'Smart glasses that make pets look intelligent', 'ACCESSORY', 250, 12); 