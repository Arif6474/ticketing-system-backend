package com.ticket.system.controller;

import com.ticket.system.dto.request.CreateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueStageRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.IssueAuditResponse;
import com.ticket.system.dto.response.IssueResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.entity.IssuePriority;
import com.ticket.system.entity.IssueStage;
import com.ticket.system.entity.IssueType;
import com.ticket.system.entity.VerificationStatus;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<IssueResponse> createIssue(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @Valid @RequestBody CreateIssueRequest request) {
        IssueResponse response = issueService.createIssue(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<IssueResponse>> getIssues(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID moduleId,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) IssuePriority priority,
            @RequestParam(required = false) IssueStage stage,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) UUID reporterId) {
        PageResponse<IssueResponse> response = issueService.getIssues(
                userDetails.getId(), page, size, search, projectId, moduleId,
                type, priority, stage, verificationStatus, reporterId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> getIssueById(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        IssueResponse response = issueService.getIssueById(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/audits")
    public ResponseEntity<List<IssueAuditResponse>> getIssueAudits(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        List<IssueAuditResponse> response = issueService.getIssueAudits(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IssueResponse> updateIssue(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIssueRequest request) {
        IssueResponse response = issueService.updateIssue(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<IssueResponse> updateIssueStage(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIssueStageRequest request) {
        IssueResponse response = issueService.updateIssueStage(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<GenericResponse> deleteIssue(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        GenericResponse response = issueService.deleteIssue(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }
}
