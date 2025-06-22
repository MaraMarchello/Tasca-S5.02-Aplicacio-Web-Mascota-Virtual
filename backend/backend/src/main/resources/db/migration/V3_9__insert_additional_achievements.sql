-- Insert additional achievements for better user engagement
INSERT INTO achievements (code, name, description, target_value, points_reward, badge_image_url, active) VALUES

-- Quick Start Achievements (Very Fast to Complete)
('FIRST_LOGIN', 'Welcome Aboard!', 'Complete your first login to CodeMate', 1, 50, null, true),
('PROFILE_EXPLORER', 'Profile Explorer', 'Visit your profile page', 1, 25, null, true),
('DASHBOARD_VISITOR', 'Dashboard Visitor', 'Visit the main dashboard', 1, 25, null, true),
('PET_NAMER', 'Pet Namer', 'Give your pet a custom name', 1, 75, null, true),
('PET_INTERACTOR', 'Pet Lover', 'Interact with your pet 5 times', 5, 100, null, true),
('FIRST_PURCHASE', 'First Purchase', 'Buy your first item from the shop', 1, 150, null, true),
('ITEM_COLLECTOR', 'Item Collector', 'Own 3 different types of items', 3, 200, null, true),

-- Pet Care Achievements
('PET_CARETAKER', 'Caring Owner', 'Keep your pet happy above 80% for 3 days', 3, 300, null, true),
('PET_HAPPINESS_MASTER', 'Happiness Master', 'Achieve 100% pet happiness', 1, 200, null, true),
('DAILY_FEEDER', 'Daily Feeder', 'Feed your pet every day for a week', 7, 400, null, true),
('PET_FASHIONISTA', 'Pet Fashionista', 'Equip 5 different accessories on your pet', 5, 350, null, true),
('FOOD_CONNOISSEUR', 'Food Connoisseur', 'Use 10 different food items', 10, 250, null, true),

-- Learning & Progress Achievements
('QUICK_LEARNER', 'Quick Learner', 'Complete any task in under 2 minutes', 1, 150, null, true),
('HELP_SEEKER', 'Help Seeker', 'Use help or hints 5 times', 5, 100, null, true),
('KNOWLEDGE_SEEKER', 'Knowledge Seeker', 'Complete 3 different types of activities', 3, 300, null, true),
('CONSISTENT_USER', 'Consistent User', 'Use CodeMate for 3 consecutive days', 3, 250, null, true),
('WEEK_WARRIOR', 'Week Warrior', 'Use CodeMate for 7 consecutive days', 7, 500, null, true),
('MONTH_MASTER', 'Month Master', 'Use CodeMate for 30 days', 30, 1000, null, true),

-- AI Interaction Achievements
('AI_CURIOUS', 'AI Curious', 'Ask your first AI question', 1, 100, null, true),
('AI_CONVERSATIONALIST', 'AI Conversationalist', 'Have 5 AI conversations', 5, 200, null, true),
('AI_POWER_USER', 'AI Power User', 'Use AI assistance 50 times', 50, 600, null, true),
('CODE_HELPER', 'Code Helper', 'Get AI help with code 10 times', 10, 400, null, true),

-- Problem Solving Achievements
('DEBUGGER', 'Debugger', 'Resolve your first error', 1, 150, null, true),
('ERROR_HUNTER', 'Error Hunter', 'Resolve 3 different types of errors', 3, 300, null, true),
('STACK_TRACE_EXPERT', 'Stack Trace Expert', 'Resolve 25 stack trace errors', 25, 750, null, true),
('PROBLEM_CRUSHER', 'Problem Crusher', 'Resolve 50 problems total', 50, 1200, null, true),

-- Social & Engagement Achievements
('EXPLORER', 'Explorer', 'Visit all main sections of CodeMate', 1, 200, null, true),
('FEATURE_TESTER', 'Feature Tester', 'Try 5 different features', 5, 300, null, true),
('POWER_USER', 'Power User', 'Earn 1000 total points', 1000, 500, null, true),
('POINT_COLLECTOR', 'Point Collector', 'Earn 5000 total points', 5000, 1000, null, true),
('ACHIEVEMENT_HUNTER', 'Achievement Hunter', 'Unlock 10 achievements', 10, 600, null, true),
('COMPLETIONIST', 'Completionist', 'Unlock 25 achievements', 25, 1500, null, true),

-- Shopping & Economy Achievements
('BIG_SPENDER', 'Big Spender', 'Spend 500 points in the shop', 500, 400, null, true),
('BARGAIN_HUNTER', 'Bargain Hunter', 'Buy 5 items under 50 points each', 5, 300, null, true),
('INVENTORY_MANAGER', 'Inventory Manager', 'Own 20 items', 20, 500, null, true),
('PREMIUM_BUYER', 'Premium Buyer', 'Buy an item worth over 100 points', 1, 250, null, true),

-- Time-based Achievements
('EARLY_BIRD', 'Early Bird', 'Use CodeMate before 9 AM', 1, 100, null, true),
('NIGHT_OWL', 'Night Owl', 'Use CodeMate after 10 PM', 1, 100, null, true),
('WEEKEND_WARRIOR', 'Weekend Warrior', 'Use CodeMate on weekend', 1, 150, null, true),
('SPEED_USER', 'Speed User', 'Complete 5 actions in under 10 minutes', 5, 300, null, true),

-- Milestone Achievements
('HUNDRED_CLUB', 'Hundred Club', 'Perform 100 total actions', 100, 800, null, true),
('VETERAN_USER', 'Veteran User', 'Use CodeMate for 60 days', 60, 2000, null, true),
('DEDICATION_MASTER', 'Dedication Master', 'Complete daily activities for 14 consecutive days', 14, 1000, null, true)

ON CONFLICT (code) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE achievements IS 'Enhanced achievements: Comprehensive gamification system with quick wins and long-term goals'; 