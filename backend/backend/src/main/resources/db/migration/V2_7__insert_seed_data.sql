-- Insert basic item templates for testing
INSERT INTO item_templates (name, description, type, price, image_url, happiness_boost, available) VALUES
('Coffee Bean', 'A delicious coffee bean that boosts happiness', 'FOOD', 10, null, 15, true),
('Energy Drink', 'High-energy drink for tired pets', 'FOOD', 25, null, 30, true),
('Premium Coffee', 'The finest coffee blend for your pet', 'FOOD', 50, null, 50, true),
('Coding Hat', 'A stylish hat for programming pets', 'ACCESSORY', 100, null, 0, true),
('Glasses', 'Smart glasses for intellectual pets', 'ACCESSORY', 75, null, 0, true)
ON CONFLICT (name) DO NOTHING;

-- Insert basic achievements for testing
INSERT INTO achievements (code, name, description, target_value, points_reward, badge_image_url, active) VALUES
('PET_OWNER', 'Pet Owner', 'Create your first virtual pet', 1, 100, null, true),
('PROBLEM_SOLVER', 'Problem Solver', 'Resolve 10 stack trace errors', 10, 500, null, true),
('AI_USER', 'AI Assistant', 'Use AI chat 25 times', 25, 250, null, true),
('PET_FEEDER', 'Pet Feeder', 'Feed your pet 50 times', 50, 300, null, true),
('SHOPPER', 'Shopper', 'Purchase 10 items from the shop', 10, 200, null, true)
ON CONFLICT (code) DO NOTHING;

-- Add comment for documentation
COMMENT ON TABLE item_templates IS 'Seed data: Basic items for testing the pet shop functionality';
COMMENT ON TABLE achievements IS 'Seed data: Basic achievements for testing the gamification system'; 