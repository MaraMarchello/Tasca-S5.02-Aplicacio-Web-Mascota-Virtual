-- Create git_branches table
CREATE TABLE git_branches (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    head_commit_hash VARCHAR(40) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT false,
    is_merged BOOLEAN NOT NULL DEFAULT false,
    merged_into_commit_hash VARCHAR(40),
    parent_branch VARCHAR(255),
    repository_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_git_branches_repository_id FOREIGN KEY (repository_id) REFERENCES git_repositories(id) ON DELETE CASCADE,
    UNIQUE(repository_id, name)
);

-- Create indexes for better performance
CREATE INDEX idx_git_branches_repository_id ON git_branches(repository_id);
CREATE INDEX idx_git_branches_name ON git_branches(name);
CREATE INDEX idx_git_branches_head_commit_hash ON git_branches(head_commit_hash);
CREATE INDEX idx_git_branches_active ON git_branches(is_active);
CREATE INDEX idx_git_branches_merged ON git_branches(is_merged); 