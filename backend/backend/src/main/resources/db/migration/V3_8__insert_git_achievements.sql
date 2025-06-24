-- Insert Git-specific achievements
INSERT INTO achievements (code, name, description, target_value, points_reward, badge_image_url, active) VALUES

-- Quick Start Git Achievements (Very Fast to Complete)
('GIT_VISITOR', 'Git Explorer', 'Visit the Git Coach page for the first time', 1, 25, null, true),
('GIT_TERMINAL_USER', 'Terminal Explorer', 'Open the Git terminal interface', 1, 50, null, true),
('GIT_VISUALIZATION_USER', 'Visual Learner', 'Open the Git visualization interface', 1, 50, null, true),
('GIT_FIRST_COMMAND', 'First Command', 'Execute your first Git command', 1, 75, null, true),
('GIT_SCENARIO_STARTER', 'Scenario Starter', 'Start your first Git scenario', 1, 100, null, true),

-- Beginner Git achievements
('GIT_FIRST_STEPS', 'Git Novice', 'Complete your first Git scenario', 1, 100, null, true),
('GIT_BEGINNER', 'Git Beginner', 'Complete 3 beginner Git scenarios', 3, 250, null, true),
('GIT_BASICS_MASTER', 'Git Basics Master', 'Complete all basic Git scenarios', 5, 500, null, true),

-- Intermediate Git achievements
('GIT_BRANCHING_PRO', 'Branching Pro', 'Complete 5 branching scenarios', 5, 300, null, true),
('GIT_MERGE_MASTER', 'Merge Master', 'Complete 3 merging scenarios', 3, 400, null, true),
('GIT_CONFLICT_RESOLVER', 'Conflict Resolver', 'Resolve 5 merge conflicts', 5, 600, null, true),

-- Advanced Git achievements
('GIT_ADVANCED_USER', 'Git Advanced User', 'Complete 5 advanced Git scenarios', 5, 750, null, true),
('GIT_WORKFLOW_EXPERT', 'Workflow Expert', 'Complete all collaboration scenarios', 3, 800, null, true),
('GIT_GURU', 'Git Guru', 'Complete all Git scenarios', 20, 1500, null, true),

-- Performance-based achievements
('GIT_EFFICIENT', 'Efficient Coder', 'Complete 5 scenarios without using hints', 5, 500, null, true),
('GIT_PERFECTIONIST', 'Git Perfectionist', 'Complete 10 scenarios with perfect score', 10, 1000, null, true),
('GIT_SPEED_DEMON', 'Speed Demon', 'Complete 5 scenarios in under 5 minutes each', 5, 750, null, true),

-- Consistency achievements
('GIT_DAILY_LEARNER', 'Daily Learner', 'Complete Git scenarios on 7 consecutive days', 7, 400, null, true),
('GIT_DEDICATED', 'Dedicated Student', 'Complete Git scenarios on 30 different days', 30, 1200, null, true),
('GIT_PERSISTENT', 'Persistent Learner', 'Complete 50 Git scenarios total', 50, 2000, null, true),

-- Special achievements
('GIT_HELP_SEEKER', 'Help Seeker', 'Use hints 25 times (learning is important!)', 25, 200, null, true),
('GIT_EXPERIMENTER', 'Experimenter', 'Try 100 different Git commands', 100, 600, null, true),
('GIT_COMMAND_MASTER', 'Command Master', 'Use all basic Git commands (init, add, commit, push, pull, merge)', 6, 800, null, true),

-- Learning Engagement Achievements
('GIT_CURIOUS', 'Git Curious', 'Execute 5 different Git commands', 5, 150, null, true),
('GIT_STUDENT', 'Eager Student', 'Complete any scenario step', 1, 50, null, true),
('GIT_PRACTITIONER', 'Git Practitioner', 'Execute 25 Git commands total', 25, 300, null, true)

ON CONFLICT (code) DO NOTHING;

-- Add comments for documentation
COMMENT ON TABLE achievements IS 'Git achievements: Comprehensive achievement system for Git learning progression'; 