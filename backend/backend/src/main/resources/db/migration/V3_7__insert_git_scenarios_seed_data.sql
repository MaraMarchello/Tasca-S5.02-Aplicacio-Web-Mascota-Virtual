-- Insert seed Git scenarios

-- Beginner scenarios
INSERT INTO git_scenarios (scenario_id, title, description, level, category, initial_state, expected_commands, success_criteria, points_reward, estimated_minutes, order_index) VALUES
('git-basics-001', 'Git Basics: First Commit', 'Learn how to initialize a repository and make your first commit', 'BEGINNER', 'BASICS', 
'{"files": {"README.md": "# My First Repository"}, "branches": ["main"], "current_branch": "main"}',
'[
  {"step": 1, "command": "git init", "guidance": "Initialize a new Git repository", "hint": "Use git init to create a new repository"},
  {"step": 2, "command": "git add", "guidance": "Add files to the staging area", "hint": "Use git add . to add all files or git add filename to add specific files"},
  {"step": 3, "command": "git commit", "guidance": "Commit your changes with a message", "hint": "Use git commit -m \"Your message\" to commit with a message"}
]',
'{"commits_count": 1, "files_added": ["README.md"], "commit_message_required": true}',
100, 15, 1),

('git-basics-002', 'Git Status and Log', 'Learn to check repository status and view commit history', 'BEGINNER', 'BASICS',
'{"files": {"index.html": "<h1>Hello World</h1>", "style.css": "body { margin: 0; }"}, "branches": ["main"], "current_branch": "main", "commits": [{"hash": "abc123", "message": "Initial commit"}]}',
'[
  {"step": 1, "command": "git status", "guidance": "Check the current status of your repository", "hint": "Use git status to see which files are modified, staged, or untracked"},
  {"step": 2, "command": "git log", "guidance": "View the commit history", "hint": "Use git log to see all commits, or git log --oneline for a compact view"}
]',
'{"commands_executed": ["git status", "git log"]}',
50, 10, 2),

-- Intermediate scenarios
('git-branching-001', 'Creating and Switching Branches', 'Learn how to create new branches and switch between them', 'INTERMEDIATE', 'BRANCHING',
'{"files": {"app.js": "console.log(\"Hello\");"}, "branches": ["main"], "current_branch": "main", "commits": [{"hash": "def456", "message": "Initial app"}]}',
'[
  {"step": 1, "command": "git branch feature", "guidance": "Create a new branch called feature", "hint": "Use git branch <branch-name> to create a new branch"},
  {"step": 2, "command": "git checkout feature", "guidance": "Switch to the feature branch", "hint": "Use git checkout <branch-name> to switch branches"},
  {"step": 3, "command": "git checkout -b", "guidance": "Learn the shortcut to create and switch in one command", "hint": "Use git checkout -b <branch-name> to create and switch to a new branch"}
]',
'{"branches_created": ["feature"], "current_branch": "feature"}',
150, 20, 3),

('git-merging-001', 'Basic Merge Operations', 'Learn how to merge branches and handle simple merges', 'INTERMEDIATE', 'MERGING',
'{"files": {"README.md": "# Project"}, "branches": ["main", "feature"], "current_branch": "main", "commits": [{"hash": "ghi789", "message": "Feature complete", "branch": "feature"}]}',
'[
  {"step": 1, "command": "git checkout main", "guidance": "Switch to the main branch", "hint": "Make sure you are on the target branch before merging"},
  {"step": 2, "command": "git merge feature", "guidance": "Merge the feature branch into main", "hint": "Use git merge <branch-name> to merge another branch into the current branch"},
  {"step": 3, "command": "git branch -d feature", "guidance": "Delete the merged feature branch", "hint": "Use git branch -d <branch-name> to delete a merged branch"}
]',
'{"merged_branches": ["feature"], "branches_deleted": ["feature"]}',
200, 25, 4),

-- Advanced scenarios
('git-conflicts-001', 'Resolving Merge Conflicts', 'Learn how to identify and resolve merge conflicts', 'ADVANCED', 'CONFLICTS',
'{"files": {"config.js": "const config = { version: \"1.0\" };"}, "branches": ["main", "feature"], "current_branch": "main", "conflict": true}',
'[
  {"step": 1, "command": "git merge feature", "guidance": "Attempt to merge the feature branch (this will create a conflict)", "hint": "This merge will fail due to conflicts"},
  {"step": 2, "command": "git status", "guidance": "Check which files have conflicts", "hint": "Look for files marked as \"both modified\""},
  {"step": 3, "command": "git add", "guidance": "After resolving conflicts manually, add the resolved files", "hint": "Edit the conflicted files, then use git add to stage them"},
  {"step": 4, "command": "git commit", "guidance": "Complete the merge with a commit", "hint": "Use git commit to finalize the merge after resolving conflicts"}
]',
'{"conflicts_resolved": true, "merge_completed": true}',
300, 35, 5),

('git-rebase-001', 'Interactive Rebase', 'Learn how to use interactive rebase to clean up commit history', 'ADVANCED', 'ADVANCED_WORKFLOWS',
'{"files": {"app.py": "print(\"Hello World\")"}, "branches": ["main", "feature"], "current_branch": "feature", "commits": [{"hash": "jkl012", "message": "WIP"}, {"hash": "mno345", "message": "Fix typo"}, {"hash": "pqr678", "message": "Add feature"}]}',
'[
  {"step": 1, "command": "git rebase -i HEAD~3", "guidance": "Start an interactive rebase for the last 3 commits", "hint": "Use git rebase -i HEAD~n to interactively rebase the last n commits"},
  {"step": 2, "command": "squash", "guidance": "Squash the WIP and typo commits into the feature commit", "hint": "Change pick to squash (or s) for commits you want to combine"},
  {"step": 3, "command": "git rebase --continue", "guidance": "Continue the rebase after editing", "hint": "Use git rebase --continue after resolving any conflicts or editing commit messages"}
]',
'{"commits_squashed": true, "history_cleaned": true}',
400, 45, 6);

-- Insert tags for scenarios
INSERT INTO git_scenario_tags (scenario_id, tag) VALUES
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-basics-001'), 'beginner'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-basics-001'), 'first-steps'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-basics-001'), 'commit'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-basics-002'), 'beginner'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-basics-002'), 'status'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-basics-002'), 'log'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-branching-001'), 'intermediate'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-branching-001'), 'branching'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-branching-001'), 'workflow'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-merging-001'), 'intermediate'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-merging-001'), 'merging'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-merging-001'), 'collaboration'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-conflicts-001'), 'advanced'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-conflicts-001'), 'conflicts'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-conflicts-001'), 'troubleshooting'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-rebase-001'), 'advanced'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-rebase-001'), 'rebase'),
((SELECT id FROM git_scenarios WHERE scenario_id = 'git-rebase-001'), 'history-cleanup'); 