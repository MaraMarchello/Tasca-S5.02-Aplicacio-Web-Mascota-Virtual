package com.codemate.config;

import com.codemate.model.GitScenario;
import com.codemate.repository.GitScenarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ScenarioSeeder {

    private final GitScenarioRepository gitScenarioRepository;

    @PostConstruct
    public void seedBasicsScenarios() {
        // First cleanup any existing scenarios from SQL migrations that we'll be managing
        cleanupDuplicateScenarios();
        
        seedBasicScenarios();
        seedAdvancedScenarios();
    }
    
    /**
     * Remove scenarios from SQL migrations that are now managed by this seeder
     * This prevents duplicates and ensures our Java-based scenarios are authoritative
     */
    private void cleanupDuplicateScenarios() {
        String[] sqlScenariosToRemove = {
            "git-basics-001",
            "git-basics-002", 
            "git-branching-001",
            "git-merging-001",
            "git-conflicts-001",
            "git-rebase-001"
        };
        
        try {
            for (String scenarioId : sqlScenariosToRemove) {
                Optional<GitScenario> existing = gitScenarioRepository.findByScenarioId(scenarioId);
                if (existing.isPresent()) {
                    gitScenarioRepository.delete(existing.get());
                    log.info("Removed duplicate SQL scenario: {}", scenarioId);
                }
            }
        } catch (Exception e) {
            log.warn("Error cleaning up duplicate scenarios", e);
            // Don't fail startup for cleanup issues
        }
    }

    private void seedBasicScenarios() {
        seedScenario(
            "BASICS_INIT_COMMIT",
            "Basics: Initialize and Commit",
            "Initialize a repository, create a file, add, and commit it.",
            "{\n  \"version\": \"1\",\n  \"steps\": [\n    {\n      \"title\": \"Initialize repository\",\n      \"instructions\": \"Run git init to create a new repository.\",\n      \"guidance\": \"Use git init to start version control.\",\n      \"hint\": \"Type: git init\",\n      \"acceptableCommands\": [\"^git\\\\s+init$\"]\n    },\n    {\n      \"title\": \"Create a file\",\n      \"instructions\": \"Create README.md with text 'Hello'.\",\n      \"guidance\": \"Use git fs create README.md Hello to simulate creating a file.\",\n      \"hint\": \"Try: git fs create README.md Hello\",\n      \"acceptableCommands\": [\"^git\\\\s+fs\\\\s+create\\\\s+README\\\\.md(\\\\s+.*)?$\"],\n      \"stateAssertions\": [ { \"type\": \"FILE_EXISTS\", \"target\": \"README.md\" }, { \"type\": \"FILE_CONTENT\", \"target\": \"README.md\", \"condition\": \"Hello\" } ]\n    },\n    {\n      \"title\": \"Check status\",\n      \"instructions\": \"Use git status to view the working tree.\",\n      \"guidance\": \"git status shows you which files are tracked, modified, or staged.\",\n      \"hint\": \"Type: git status\",\n      \"acceptableCommands\": [\"^git\\\\s+status$\"]\n    },\n    {\n      \"title\": \"Add changes\",\n      \"instructions\": \"Stage changes using git add .\",\n      \"guidance\": \"Use git add . to stage all changes.\",\n      \"hint\": \"Type: git add .\",\n      \"acceptableCommands\": [\"^git\\\\s+add\\\\s+\\\\.$\", \"^git\\\\s+add\\\\s+-A$\"]\n    },\n    {\n      \"title\": \"Create commit\",\n      \"instructions\": \"Create a commit with a message.\",\n      \"guidance\": \"Use git commit -m \"Add README\".\",\n      \"hint\": \"Type: git commit -m \"Add README\"\",\n      \"acceptableCommands\": [\"^git\\\\s+commit\\\\s+-m\\\\s+.+$\"],\n      \"stateAssertions\": [ { \"type\": \"COMMIT_MESSAGE\", \"condition\": \"\", \"errorMessage\": \"Include a commit message with -m.\" } ]\n    },\n    {\n      \"title\": \"View log\",\n      \"instructions\": \"View recent commits using git log --oneline.\",\n      \"guidance\": \"git log shows commit history. The --oneline flag displays a condensed view.\",\n      \"hint\": \"Type: git log --oneline\",\n      \"acceptableCommands\": [\"^git\\\\s+log(\\\\s+--oneline)?$\"]\n    }\n  ]\n}"
        );

        seedScenario(
            "BASICS_BRANCH_SWITCH",
            "Basics: Branch and Switch",
            "Create a branch, switch to it, create and commit a file on that branch, and view the log.",
            "{\n  \"version\": \"1\",\n  \"steps\": [\n    {\n      \"title\": \"List branches\",\n      \"instructions\": \"List branches using git branch.\",\n      \"guidance\": \"git branch shows all local branches. The current branch is marked with *.\",\n      \"hint\": \"Type: git branch\",\n      \"acceptableCommands\": [\"^git\\\\s+branch(\\\\s+-a)?$\"]\n    },\n    {\n      \"title\": \"Create new branch\",\n      \"instructions\": \"Create a new branch named feature/demo.\",\n      \"guidance\": \"Branches allow you to work on features independently. Use descriptive names like feature/feature-name.\",\n      \"hint\": \"Try: git branch feature/demo\",\n      \"acceptableCommands\": [\"^git\\\\s+branch\\\\s+feature/demo$\", \"^git\\\\s+checkout\\\\s+-b\\\\s+feature/demo$\", \"^git\\\\s+switch\\\\s+-c\\\\s+feature/demo$\"]\n    },\n    {\n      \"title\": \"Switch to branch\",\n      \"instructions\": \"Switch to feature/demo branch.\",\n      \"guidance\": \"Use git switch feature/demo.\",\n      \"hint\": \"Type: git switch feature/demo\",\n      \"acceptableCommands\": [\"^git\\\\s+checkout\\\\s+feature/demo$\", \"^git\\\\s+switch\\\\s+feature/demo$\"],\n      \"stateAssertions\": [ { \"type\": \"CURRENT_BRANCH\", \"target\": \"feature/demo\" } ]\n    },\n    {\n      \"title\": \"Create a file\",\n      \"instructions\": \"Create FEATURE.md with text 'Feature'.\",\n      \"guidance\": \"Use git fs create FEATURE.md Feature\",\n      \"hint\": \"Try: git fs create FEATURE.md Feature\",\n      \"acceptableCommands\": [\"^git\\\\s+fs\\\\s+create\\\\s+FEATURE\\\\.md(\\\\s+.*)?$\"],\n      \"stateAssertions\": [ { \"type\": \"FILE_EXISTS\", \"target\": \"FEATURE.md\" }, { \"type\": \"FILE_CONTENT\", \"target\": \"FEATURE.md\", \"condition\": \"Feature\" } ]\n    },\n    {\n      \"title\": \"Add and commit on branch\",\n      \"instructions\": \"Stage and commit the new file on the current branch.\",\n      \"guidance\": \"Use git add . then git commit -m \"Add feature\"\",\n      \"acceptableCommands\": [\"^git\\\\s+add\\\\s+\\\\.$\", \"^git\\\\s+add\\\\s+-A$\", \"^git\\\\s+commit\\\\s+-m\\\\s+.+$\"],\n      \"stateAssertions\": [ { \"type\": \"CURRENT_BRANCH\", \"target\": \"feature/demo\" } ]\n    },\n    {\n      \"title\": \"Log\",\n      \"instructions\": \"View commits with git log --oneline.\",\n      \"hint\": \"Type: git log --oneline\",\n      \"acceptableCommands\": [\"^git\\\\s+log(\\\\s+--oneline)?$\"]\n    }\n  ]\n}"
        );  

        // Add missing scenarios that were in the SQL migration but not duplicating advanced ones
        seedScenario(
            "BASICS_STATUS_LOG",
            "Git Status and Log",
            "Learn to check repository status and view commit history",
            "{\n  \"version\": \"1\",\n  \"steps\": [\n    {\n      \"title\": \"Check repository status\",\n      \"instructions\": \"Use git status to check the current status of your repository.\",\n      \"guidance\": \"git status shows you which files are tracked, modified, or staged.\",\n      \"hint\": \"Type: git status\",\n      \"acceptableCommands\": [\"^git\\\\s+status$\"]\n    },\n    {\n      \"title\": \"View commit history\",\n      \"instructions\": \"Use git log to view the commit history.\",\n      \"guidance\": \"git log shows all commits. Use --oneline for a compact view.\",\n      \"hint\": \"Type: git log or git log --oneline\",\n      \"acceptableCommands\": [\"^git\\\\s+log(\\\\s+--oneline)?$\"]\n    }\n  ]\n}"
        );

        // Intermediate scenarios with proper metadata
        seedAdvancedScenario(
            "INTERMEDIATE_BRANCHING",
            "Creating and Switching Branches",
            "Learn how to create new branches and switch between them",
            GitScenario.GitScenarioLevel.INTERMEDIATE,
            GitScenario.GitScenarioCategory.BRANCHING,
            150,
            20,
            "{\n  \"version\": \"1\",\n  \"steps\": [\n    {\n      \"title\": \"Create a new branch\",\n      \"instructions\": \"Create a new branch called 'feature'.\",\n      \"guidance\": \"Branches allow you to work on features independently.\",\n      \"hint\": \"Type: git branch feature\",\n      \"acceptableCommands\": [\"^git\\\\s+branch\\\\s+feature$\"]\n    },\n    {\n      \"title\": \"Switch to the feature branch\",\n      \"instructions\": \"Switch to the feature branch you just created.\",\n      \"guidance\": \"Use git checkout or git switch to change branches.\",\n      \"hint\": \"Type: git checkout feature or git switch feature\",\n      \"acceptableCommands\": [\"^git\\\\s+checkout\\\\s+feature$\", \"^git\\\\s+switch\\\\s+feature$\"],\n      \"stateAssertions\": [ { \"type\": \"CURRENT_BRANCH\", \"target\": \"feature\" } ]\n    },\n    {\n      \"title\": \"Create and switch in one command\",\n      \"instructions\": \"Learn the shortcut to create and switch to a new branch called 'quick-feature'.\",\n      \"guidance\": \"You can create and switch to a branch in one command.\",\n      \"hint\": \"Type: git checkout -b quick-feature or git switch -c quick-feature\",\n      \"acceptableCommands\": [\"^git\\\\s+checkout\\\\s+-b\\\\s+quick-feature$\", \"^git\\\\s+switch\\\\s+-c\\\\s+quick-feature$\"],\n      \"stateAssertions\": [ { \"type\": \"CURRENT_BRANCH\", \"target\": \"quick-feature\" } ]\n    }\n  ]\n}"
        );

        seedAdvancedScenario(
            "INTERMEDIATE_MERGING",
            "Basic Merge Operations",
            "Learn how to merge branches and handle simple merges",
            GitScenario.GitScenarioLevel.INTERMEDIATE,
            GitScenario.GitScenarioCategory.MERGING,
            200,
            25,
            "{\n  \"version\": \"1\",\n  \"steps\": [\n    {\n      \"title\": \"Switch to main branch\",\n      \"instructions\": \"Switch to the main branch before merging.\",\n      \"guidance\": \"Always make sure you're on the target branch before merging.\",\n      \"hint\": \"Type: git checkout main or git switch main\",\n      \"acceptableCommands\": [\"^git\\\\s+checkout\\\\s+main$\", \"^git\\\\s+switch\\\\s+main$\"],\n      \"stateAssertions\": [ { \"type\": \"CURRENT_BRANCH\", \"target\": \"main\" } ]\n    },\n    {\n      \"title\": \"Merge the feature branch\",\n      \"instructions\": \"Merge the feature branch into main.\",\n      \"guidance\": \"Use git merge to combine changes from another branch.\",\n      \"hint\": \"Type: git merge feature\",\n      \"acceptableCommands\": [\"^git\\\\s+merge\\\\s+feature$\"]\n    },\n    {\n      \"title\": \"Delete the merged branch\",\n      \"instructions\": \"Clean up by deleting the merged feature branch.\",\n      \"guidance\": \"Use -d flag to safely delete merged branches.\",\n      \"hint\": \"Type: git branch -d feature\",\n      \"acceptableCommands\": [\"^git\\\\s+branch\\\\s+-d\\\\s+feature$\"]\n    }\n  ]\n}"
        );
    }

    private void seedAdvancedScenarios() {
        seedAdvancedScenario(
            "INTERMEDIATE_FEATURE_BRANCH",
            "Intermediate: Feature Branch Workflow",
            "Learn the complete feature branch workflow used in professional development.",
            GitScenario.GitScenarioLevel.INTERMEDIATE,
            GitScenario.GitScenarioCategory.ADVANCED_WORKFLOWS,
            100,
            20,
            createFeatureBranchWorkflowScenario()
        );

        seedAdvancedScenario(
            "ADVANCED_MERGE_CONFLICTS",
            "Advanced: Merge Conflict Resolution",
            "Master the art of resolving complex merge conflicts in team environments.",
            GitScenario.GitScenarioLevel.ADVANCED,
            GitScenario.GitScenarioCategory.COLLABORATION,
            150,
            25,
            createMergeConflictScenario()
        );

        seedAdvancedScenario(
            "ADVANCED_INTERACTIVE_REBASE",
            "Advanced: Interactive Rebase",
            "Learn to clean up commit history with interactive rebase.",
            GitScenario.GitScenarioLevel.ADVANCED,
            GitScenario.GitScenarioCategory.ADVANCED_WORKFLOWS,
            120,
            15,
            createInteractiveRebaseScenario()
        );

        seedAdvancedScenario(
            "ADVANCED_CHERRY_PICK",
            "Advanced: Cherry-pick Workflow", 
            "Selectively apply commits across branches using cherry-pick.",
            GitScenario.GitScenarioLevel.INTERMEDIATE,
            GitScenario.GitScenarioCategory.ADVANCED_WORKFLOWS,
            80,
            15,
            createCherryPickScenario()
        );

        seedAdvancedScenario(
            "INTERMEDIATE_STASH_MANAGEMENT",
            "Intermediate: Stash Management",
            "Learn proper stash usage for managing work-in-progress changes.",
            GitScenario.GitScenarioLevel.INTERMEDIATE,
            GitScenario.GitScenarioCategory.ADVANCED_WORKFLOWS,
            70,
            12,
            createStashManagementScenario()
        );

        seedAdvancedScenario(
            "ADVANCED_HOTFIX_WORKFLOW",
            "Advanced: Hotfix Workflow",
            "Practice handling urgent production fixes with proper branching strategy.",
            GitScenario.GitScenarioLevel.ADVANCED,
            GitScenario.GitScenarioCategory.ADVANCED_WORKFLOWS,
            130,
            18,
            createHotfixWorkflowScenario()
        );

        seedAdvancedScenario(
            "ADVANCED_MERGE_CONFLICTS_DETAILED",
            "Resolving Merge Conflicts",
            "Learn how to identify and resolve merge conflicts",
            GitScenario.GitScenarioLevel.ADVANCED,
            GitScenario.GitScenarioCategory.COLLABORATION,
            300,
            35,
            createDetailedMergeConflictScenario()
        );
    }

    private void seedAdvancedScenario(String scenarioId, String title, String description, 
                                    GitScenario.GitScenarioLevel level, GitScenario.GitScenarioCategory category,
                                    int points, int estimatedMinutes, String expectedSchemaJson) {
        Optional<GitScenario> existingOpt = gitScenarioRepository.findByScenarioId(scenarioId);
        try {
            if (existingOpt.isPresent()) {
                GitScenario scenario = existingOpt.get();
                scenario.setTitle(title);
                scenario.setDescription(description);
                scenario.setExpectedCommands(expectedSchemaJson);
                scenario.setIsActive(true);
                scenario.setLevel(level);
                scenario.setCategory(category);
                scenario.setPointsReward(points);
                scenario.setEstimatedMinutes(estimatedMinutes);
                gitScenarioRepository.save(scenario);
                log.info("Updated advanced scenario {}", scenarioId);
                return;
            }

            GitScenario scenario = GitScenario.builder()
                .scenarioId(scenarioId)
                .title(title)
                .description(description)
                .level(level)
                .category(category)
                .initialState("{}")
                .expectedCommands(expectedSchemaJson)
                .successCriteria(null)
                .pointsReward(points)
                .estimatedMinutes(estimatedMinutes)
                .isActive(true)
                .orderIndex(0)
                .build();
            gitScenarioRepository.save(scenario);
            log.info("Seeded advanced scenario {}", scenarioId);
        } catch (Exception e) {
            log.warn("Failed to seed advanced scenario {}", scenarioId, e);
        }
    }

    private void seedScenario(String scenarioId, String title, String description, String expectedSchemaJson) {
        Optional<GitScenario> existingOpt = gitScenarioRepository.findByScenarioId(scenarioId);
        try {
            if (existingOpt.isPresent()) {
                GitScenario scenario = existingOpt.get();
                scenario.setTitle(title);
                scenario.setDescription(description);
                scenario.setExpectedCommands(expectedSchemaJson);
                scenario.setIsActive(true);
                scenario.setLevel(GitScenario.GitScenarioLevel.BEGINNER);
                scenario.setCategory(GitScenario.GitScenarioCategory.BASICS);
                gitScenarioRepository.save(scenario);
                log.info("Updated scenario {}", scenarioId);
                return;
            }

            GitScenario scenario = GitScenario.builder()
                .scenarioId(scenarioId)
                .title(title)
                .description(description)
                .level(GitScenario.GitScenarioLevel.BEGINNER)
                .category(GitScenario.GitScenarioCategory.BASICS)
                .initialState("{}")
                .expectedCommands(expectedSchemaJson)
                .successCriteria(null)
                .pointsReward(50)
                .estimatedMinutes(10)
                .isActive(true)
                .orderIndex(0)
                .build();
            gitScenarioRepository.save(scenario);
            log.info("Seeded scenario {}", scenarioId);
        } catch (Exception e) {
            log.warn("Failed to seed scenario {}", scenarioId, e);
        }
    }

    private String createFeatureBranchWorkflowScenario() {
        return """
            {
              "version": "1",
              "steps": [
                {
                  "title": "Create feature branch",
                  "instructions": "Create a new feature branch called 'feature/user-auth'.",
                  "guidance": "Feature branches isolate development work from the main branch.",
                  "hint": "Type: git checkout -b feature/user-auth",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+-b\\\\s+feature/user-auth$", "^git\\\\s+switch\\\\s+-c\\\\s+feature/user-auth$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "feature/user-auth" }
                  ]
                },
                {
                  "title": "Create authentication module",
                  "instructions": "Create an auth.js file with authentication logic.",
                  "guidance": "Create the main feature file.",
                  "hint": "Type: git fs create auth.js 'module.exports = { login, logout }'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+create\\\\s+auth\\.js.*$"],
                  "stateAssertions": [
                    { "type": "FILE_EXISTS", "target": "auth.js" }
                  ]
                },
                {
                  "title": "Add and commit feature",
                  "instructions": "Stage and commit the authentication module.",
                  "guidance": "Use descriptive commit messages for features.",
                  "hint": "git add . && git commit -m 'feat: add user authentication module'",
                  "acceptableCommands": ["^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*auth.*$"],
                  "stateAssertions": [
                    { "type": "COMMIT_MESSAGE", "condition": "auth", "errorMessage": "Commit should mention authentication" }
                  ]
                },
                {
                  "title": "Switch to main branch",
                  "instructions": "Switch back to the main branch for merging.",
                  "guidance": "Always merge features into the main branch.",
                  "hint": "Type: git checkout main",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+main$", "^git\\\\s+switch\\\\s+main$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "main" }
                  ]
                },
                {
                  "title": "Merge feature branch",
                  "instructions": "Merge the feature branch into main.",
                  "guidance": "Use --no-ff to preserve branch history.",
                  "hint": "Type: git merge --no-ff feature/user-auth",
                  "acceptableCommands": ["^git\\\\s+merge.*feature/user-auth$"],
                  "stateAssertions": [
                    { "type": "MERGE_COMPLETED" }
                  ]
                }
              ]
            }
            """;
    }

    private String createMergeConflictScenario() {
        return """
            {
              "version": "1",
              "steps": [
                {
                  "title": "Create conflicting branches",
                  "instructions": "Create two branches that will conflict: feature/ui and feature/backend.",
                  "guidance": "We'll simulate a scenario where two developers work on the same file.",
                  "hint": "git checkout -b feature/ui && git checkout main && git checkout -b feature/backend",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+-b\\\\s+feature/ui$", "^git\\\\s+checkout.*main$", "^git\\\\s+checkout\\\\s+-b\\\\s+feature/backend$"]
                },
                {
                  "title": "Make changes in UI branch",
                  "instructions": "Switch to feature/ui and modify app.js with UI-related changes.",
                  "guidance": "First developer adds UI functionality.",
                  "hint": "git checkout feature/ui && git fs create app.js 'console.log(\\\\\"UI loaded\\\\\");'",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+feature/ui$", "^git\\\\s+fs\\\\s+create\\\\s+app\\.js.*UI.*$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "feature/ui" },
                    { "type": "FILE_EXISTS", "target": "app.js" }
                  ]
                },
                {
                  "title": "Commit UI changes",
                  "instructions": "Commit the UI changes.",
                  "hint": "git add . && git commit -m 'Add UI initialization'",
                  "acceptableCommands": ["^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*UI.*$"]
                },
                {
                  "title": "Make conflicting backend changes",
                  "instructions": "Switch to feature/backend and modify the same app.js file differently.",
                  "guidance": "Second developer adds backend functionality to the same file.",
                  "hint": "git checkout feature/backend && git fs create app.js 'console.log(\\\\\"Backend ready\\\\\");'",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+feature/backend$", "^git\\\\s+fs\\\\s+create\\\\s+app\\.js.*Backend.*$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "feature/backend" }
                  ]
                },
                {
                  "title": "Commit backend changes",
                  "instructions": "Commit the backend changes.",
                  "hint": "git add . && git commit -m 'Add backend initialization'",
                  "acceptableCommands": ["^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*backend.*$"]
                },
                {
                  "title": "Attempt merge",
                  "instructions": "Try to merge feature/ui into feature/backend to create a conflict.",
                  "guidance": "This will create a merge conflict that needs resolution.",
                  "hint": "Type: git merge feature/ui",
                  "acceptableCommands": ["^git\\\\s+merge\\\\s+feature/ui$"]
                },
                {
                  "title": "Resolve conflict",
                  "instructions": "Edit the conflicted file to resolve the merge conflict.",
                  "guidance": "Remove conflict markers and combine both functionalities.",
                  "hint": "git fs edit app.js 'console.log(\\\\\"UI loaded\\\\\"); console.log(\\\\\"Backend ready\\\\\");'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+edit\\\\s+app\\.js.*$"]
                },
                {
                  "title": "Complete merge",
                  "instructions": "Add the resolved file and complete the merge.",
                  "hint": "git add app.js && git commit -m 'Resolve merge conflict'",
                  "acceptableCommands": ["^git\\\\s+add\\\\s+app\\.js$", "^git\\\\s+commit.*$"]
                }
              ]
            }
            """;
    }

    private String createInteractiveRebaseScenario() {
        return """
            {
              "version": "1",
              "steps": [
                {
                  "title": "Create first commit",
                  "instructions": "Create file1.txt and make the first commit.",
                  "guidance": "We'll create multiple small commits that can be squashed together.",
                  "hint": "git fs create file1.txt 'Initial content' && git add . && git commit -m 'Add file1'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+create\\\\s+file1\\\\.txt.*$", "^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*file1.*$"]
                },
                {
                  "title": "Create second commit",
                  "instructions": "Create file2.txt and make a second commit.",
                  "guidance": "Add another file to create commit history that needs cleanup.",
                  "hint": "git fs create file2.txt 'Second file' && git add . && git commit -m 'Add file2'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+create\\\\s+file2\\\\.txt.*$", "^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*file2.*$"]
                },
                {
                  "title": "Create third commit",
                  "instructions": "Create file3.txt and make a third commit.",
                  "guidance": "This will be our third commit to include in the interactive rebase.",
                  "hint": "git fs create file3.txt 'Third file' && git add . && git commit -m 'Add file3'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+create\\\\s+file3\\\\.txt.*$", "^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*file3.*$"]
                },
                {
                  "title": "View current history",
                  "instructions": "Check the current commit history before rebasing.",
                  "guidance": "You should see three recent commits that we'll squash together.",
                  "hint": "Type: git log --oneline -5",
                  "acceptableCommands": ["^git\\\\s+log\\\\s+--oneline.*$", "^git\\\\s+log.*$"]
                },
                {
                  "title": "Start interactive rebase",
                  "instructions": "Start an interactive rebase to modify the last 3 commits.",
                  "guidance": "Interactive rebase opens an editor where you can choose actions for each commit (pick, squash, edit, etc.).",
                  "hint": "Type: git rebase -i HEAD~3",
                  "acceptableCommands": ["^git\\\\s+rebase\\\\s+-i\\\\s+HEAD~3$", "^git\\\\s+rebase\\\\s+--interactive\\\\s+HEAD~3$"]
                },
                {
                  "title": "Complete rebase and check history",
                  "instructions": "After the rebase simulation, check the cleaned up commit history.",
                  "guidance": "The commits have been combined. You should see a cleaner history with fewer commits.",
                  "hint": "Type: git log --oneline -5",
                  "acceptableCommands": ["^git\\\\s+log\\\\s+--oneline.*$", "^git\\\\s+log.*$"]
                }
              ]
            }
            """;
    }

    private String createCherryPickScenario() {
        return """
            {
              "version": "1",
              "steps": [
                {
                  "title": "Create feature branch with commits",
                  "instructions": "Create a feature branch and add some commits.",
                  "guidance": "We'll cherry-pick specific commits to another branch.",
                  "hint": "git checkout -b feature/fix && git fs create bugfix.js 'fixed bug' && git add . && git commit -m 'Fix critical bug'",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+-b\\\\s+feature/fix$", "^git\\\\s+fs\\\\s+create.*$", "^git\\\\s+add.*$", "^git\\\\s+commit.*bug.*$"]
                },
                {
                  "title": "Get commit hash",
                  "instructions": "View the commit log to see the commit hash.",
                  "guidance": "You'll need the commit hash for cherry-picking.",
                  "hint": "Type: git log --oneline",
                  "acceptableCommands": ["^git\\\\s+log.*$"]
                },
                {
                  "title": "Switch to main branch", 
                  "instructions": "Switch back to main branch for cherry-picking.",
                  "hint": "Type: git checkout main",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+main$", "^git\\\\s+switch\\\\s+main$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "main" }
                  ]
                },
                {
                  "title": "Cherry-pick the commit",
                  "instructions": "Cherry-pick the bug fix commit using a 7-character hash prefix.",
                  "guidance": "Cherry-pick applies a specific commit to the current branch.",
                  "hint": "Type: git cherry-pick abcdefg (replace with actual hash prefix)",
                  "acceptableCommands": ["^git\\\\s+cherry-pick\\\\s+[a-f0-9]{7,}$"]
                }
              ]
            }
            """;
    }

    private String createStashManagementScenario() {
        return """
            {
              "version": "1",
              "steps": [
                {
                  "title": "Create work-in-progress changes",
                  "instructions": "Create some changes but don't commit them yet.",
                  "guidance": "Simulate working on a feature when you need to switch contexts.",
                  "hint": "git fs create temp.js 'work in progress'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+create\\\\s+temp\\.js.*$"],
                  "stateAssertions": [
                    { "type": "FILE_EXISTS", "target": "temp.js" }
                  ]
                },
                {
                  "title": "Stash the changes",
                  "instructions": "Stash your work-in-progress changes with a descriptive message.",
                  "guidance": "Stash saves changes temporarily without committing.",
                  "hint": "Type: git stash push -m 'WIP: new feature'",
                  "acceptableCommands": ["^git\\\\s+stash\\\\s+(push\\\\s+)?(-m\\\\s+.*)?$", "^git\\\\s+stash\\\\s+save.*$"]
                },
                {
                  "title": "Verify clean working tree",
                  "instructions": "Check that your working directory is now clean.",
                  "guidance": "Stash should have cleared your working directory.",
                  "hint": "Type: git status",
                  "acceptableCommands": ["^git\\\\s+status$"]
                },
                {
                  "title": "List stashes",
                  "instructions": "View your stash list to see the saved changes.",
                  "hint": "Type: git stash list",
                  "acceptableCommands": ["^git\\\\s+stash\\\\s+list$"]
                },
                {
                  "title": "Pop the stash",
                  "instructions": "Restore your stashed changes and remove them from the stash.",
                  "guidance": "Pop applies and removes the latest stash entry.",
                  "hint": "Type: git stash pop",
                  "acceptableCommands": ["^git\\\\s+stash\\\\s+pop(\\\\s+stash@\\{\\d+\\})?$"]
                }
              ]
            }
            """;
    }

    private String createHotfixWorkflowScenario() {
        return """
            {
              "version": "1",
              "steps": [
                {
                  "title": "Create hotfix branch from main",
                  "instructions": "Create a hotfix branch to address a critical production issue.",
                  "guidance": "Hotfix branches are created from main for urgent fixes.",
                  "hint": "Type: git checkout -b hotfix/security-patch",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+-b\\\\s+hotfix/security-patch$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "hotfix/security-patch" }
                  ]
                },
                {
                  "title": "Implement the fix",
                  "instructions": "Create a security patch file with the fix.",
                  "guidance": "Make the minimal change needed to fix the issue.",
                  "hint": "git fs create security.patch 'Fixed XSS vulnerability'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+create\\\\s+security\\.patch.*$"],
                  "stateAssertions": [
                    { "type": "FILE_EXISTS", "target": "security.patch" }
                  ]
                },
                {
                  "title": "Commit the hotfix",
                  "instructions": "Commit the security fix with a clear message.",
                  "guidance": "Hotfix commits should be descriptive and reference the issue.",
                  "hint": "git add . && git commit -m 'hotfix: patch XSS vulnerability in user input'",
                  "acceptableCommands": ["^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*hotfix.*$"],
                  "stateAssertions": [
                    { "type": "COMMIT_MESSAGE", "condition": "hotfix", "errorMessage": "Commit should mention hotfix" }
                  ]
                },
                {
                  "title": "Merge hotfix to main",
                  "instructions": "Switch to main and merge the hotfix.",
                  "guidance": "Hotfixes are immediately merged to main for deployment.",
                  "hint": "git checkout main && git merge hotfix/security-patch",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+main$", "^git\\\\s+merge\\\\s+hotfix/security-patch$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "main" },
                    { "type": "MERGE_COMPLETED" }
                  ]
                },
                {
                  "title": "Tag the release",
                  "instructions": "Create a tag for the hotfix release.",
                  "guidance": "Tag hotfix releases for tracking and deployment.",
                  "hint": "Type: git tag v1.0.1",
                  "acceptableCommands": ["^git\\\\s+tag\\\\s+v\\d+\\.\\d+\\.\\d+$"]
                }
              ]
            }
            """;
    }

    private String createDetailedMergeConflictScenario() {
        return """
            {
              "version": "1",
              "steps": [
                {
                  "title": "Prepare conflict scenario",
                  "instructions": "Create two branches that will have conflicting changes.",
                  "guidance": "We'll simulate a realistic conflict scenario between two developers.",
                  "hint": "git checkout -b branch-a && git checkout main && git checkout -b branch-b",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+-b\\\\s+branch-a$", "^git\\\\s+checkout.*main$", "^git\\\\s+checkout\\\\s+-b\\\\s+branch-b$"]
                },
                {
                  "title": "Make changes in first branch",
                  "instructions": "Switch to branch-a and create conflicting content.",
                  "guidance": "First developer adds their version of the feature.",
                  "hint": "git checkout branch-a && git fs create config.js 'const version = 1.0;'",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+branch-a$", "^git\\\\s+fs\\\\s+create\\\\s+config\\.js.*$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "branch-a" }
                  ]
                },
                {
                  "title": "Commit first changes",
                  "instructions": "Commit the changes in branch-a.",
                  "hint": "git add . && git commit -m 'Update config version'",
                  "acceptableCommands": ["^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*$"]
                },
                {
                  "title": "Make conflicting changes",
                  "instructions": "Switch to branch-b and make conflicting changes to the same file.",
                  "guidance": "Second developer modifies the same line differently.",
                  "hint": "git checkout branch-b && git fs create config.js 'const version = 2.0;'",
                  "acceptableCommands": ["^git\\\\s+checkout\\\\s+branch-b$", "^git\\\\s+fs\\\\s+create\\\\s+config\\.js.*$"],
                  "stateAssertions": [
                    { "type": "CURRENT_BRANCH", "target": "branch-b" }
                  ]
                },
                {
                  "title": "Commit conflicting changes",
                  "instructions": "Commit the conflicting changes in branch-b.",
                  "hint": "git add . && git commit -m 'Update config differently'",
                  "acceptableCommands": ["^git\\\\s+add.*$", "^git\\\\s+commit\\\\s+-m.*$"]
                },
                {
                  "title": "Attempt merge",
                  "instructions": "Try to merge branch-a into branch-b to create a conflict.",
                  "guidance": "This merge will fail due to conflicting changes.",
                  "hint": "Type: git merge branch-a",
                  "acceptableCommands": ["^git\\\\s+merge\\\\s+branch-a$"]
                },
                {
                  "title": "Check conflict status",
                  "instructions": "Use git status to see which files have conflicts.",
                  "guidance": "Status will show files marked as 'both modified'.",
                  "hint": "Type: git status",
                  "acceptableCommands": ["^git\\\\s+status$"]
                },
                {
                  "title": "Resolve conflicts",
                  "instructions": "Edit the conflicted file to resolve the merge conflict.",
                  "guidance": "Remove conflict markers and decide how to combine the changes.",
                  "hint": "git fs edit config.js 'const version = 2.1; // Combined changes'",
                  "acceptableCommands": ["^git\\\\s+fs\\\\s+edit\\\\s+config\\.js.*$"]
                },
                {
                  "title": "Complete merge",
                  "instructions": "Add the resolved file and complete the merge.",
                  "guidance": "After resolving conflicts, stage and commit to complete the merge.",
                  "hint": "git add config.js && git commit -m 'Resolve version conflict'",
                  "acceptableCommands": ["^git\\\\s+add\\\\s+config\\.js$", "^git\\\\s+commit.*$"]
                }
              ]
            }
            """;
    }
}


