package com.ticket.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateIssueCommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 10000, message = "Comment content cannot exceed 10000 characters")
    private String content;

    public UpdateIssueCommentRequest() {
    }

    public UpdateIssueCommentRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
