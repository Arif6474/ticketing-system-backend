package com.ticket.system.dto.request;

import com.ticket.system.entity.IssueStage;
import jakarta.validation.constraints.NotNull;

public class UpdateIssueStageRequest {

    @NotNull(message = "Target stage is required")
    private IssueStage stage;

    public UpdateIssueStageRequest() {
    }

    public UpdateIssueStageRequest(IssueStage stage) {
        this.stage = stage;
    }

    public IssueStage getStage() {
        return stage;
    }

    public void setStage(IssueStage stage) {
        this.stage = stage;
    }
}
