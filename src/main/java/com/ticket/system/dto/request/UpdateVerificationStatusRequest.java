package com.ticket.system.dto.request;

import com.ticket.system.entity.VerificationStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateVerificationStatusRequest {

    @NotNull(message = "Verification status is required")
    private VerificationStatus status;

    public UpdateVerificationStatusRequest() {
    }

    public UpdateVerificationStatusRequest(VerificationStatus status) {
        this.status = status;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }
}
