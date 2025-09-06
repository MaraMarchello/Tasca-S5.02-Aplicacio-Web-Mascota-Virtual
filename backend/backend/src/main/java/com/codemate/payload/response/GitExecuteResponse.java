package com.codemate.payload.response;

import com.codemate.model.GitUserProgress;
import com.codemate.service.git.GitRepositoryManagementService;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitExecuteResponse {
    private GitRepositoryManagementService.GitCommandResult result;
    private Boolean stepCompleted;
    private Integer nextStepNumber;
    private GitUserProgress progress;
    private GitRepositoryManagementService.GitRepositoryState repositoryState;
    private String tutorMessage;
}


