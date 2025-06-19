-- Create git_commits table
CREATE TABLE git_commits (
    id BIGSERIAL PRIMARY KEY,
    hash VARCHAR(40) NOT NULL UNIQUE,
    message TEXT NOT NULL,
    author VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    changes TEXT,
    repository_id BIGINT NOT NULL,
    commit_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_git_commits_repository_id FOREIGN KEY (repository_id) REFERENCES git_repositories(id) ON DELETE CASCADE
);

-- Create git_commit_parents table for parent relationships
CREATE TABLE git_commit_parents (
    commit_id BIGINT NOT NULL,
    parent_hash VARCHAR(40) NOT NULL,
    
    CONSTRAINT fk_git_commit_parents_commit_id FOREIGN KEY (commit_id) REFERENCES git_commits(id) ON DELETE CASCADE,
    PRIMARY KEY (commit_id, parent_hash)
);

-- Create git_commit_files table for modified files
CREATE TABLE git_commit_files (
    commit_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    
    CONSTRAINT fk_git_commit_files_commit_id FOREIGN KEY (commit_id) REFERENCES git_commits(id) ON DELETE CASCADE,
    PRIMARY KEY (commit_id, file_path)
);

-- Create indexes for better performance
CREATE INDEX idx_git_commits_hash ON git_commits(hash);
CREATE INDEX idx_git_commits_repository_id ON git_commits(repository_id);
CREATE INDEX idx_git_commits_branch_name ON git_commits(branch_name);
CREATE INDEX idx_git_commits_commit_time ON git_commits(commit_time);
CREATE INDEX idx_git_commit_parents_parent_hash ON git_commit_parents(parent_hash); 