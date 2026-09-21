package com.ticket.system.service;

import com.ticket.system.entity.IssueStage;
import com.ticket.system.entity.Role;
import com.ticket.system.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IssueStageTransitionService {

    public void validateTransition(IssueStage currentStage, IssueStage targetStage, Role userRole) {
        if (currentStage == targetStage) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Issue is already in stage " + currentStage);
        }

        // 1. State machine transition rule check
        boolean isStateTransitionValid = switch (currentStage) {
            case SUBMITTED -> targetStage == IssueStage.RECEIVED || targetStage == IssueStage.DECLINED;
            case RECEIVED -> targetStage == IssueStage.UNDER_DEVELOPMENT;
            case UNDER_DEVELOPMENT -> targetStage == IssueStage.TESTING;
            case TESTING -> targetStage == IssueStage.DEPLOYED || targetStage == IssueStage.RESOLVED;
            case DEPLOYED, DECLINED, RESOLVED -> false;
        };

        if (!isStateTransitionValid) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Invalid stage transition from " + currentStage + " to " + targetStage);
        }

        // 2. Role permission check
        if (userRole == Role.CLIENT_USER) {
            boolean isUserAllowed = (currentStage == IssueStage.SUBMITTED && targetStage == IssueStage.DECLINED) ||
                                    (currentStage == IssueStage.TESTING && targetStage == IssueStage.RESOLVED);

            if (!isUserAllowed) {
                throw new AppException(HttpStatus.FORBIDDEN,
                        "CLIENT_USER is not authorized to transition issue from " + currentStage + " to " + targetStage);
            }
        }
    }
}
