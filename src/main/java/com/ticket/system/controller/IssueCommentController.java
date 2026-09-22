package com.ticket.system.controller;

import com.ticket.system.dto.request.CreateIssueCommentRequest;
import com.ticket.system.dto.request.UpdateIssueCommentRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.IssueCommentResponse;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.IssueCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/comments")
public class IssueCommentController {

    private final IssueCommentService commentService;

    public IssueCommentController(IssueCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<IssueCommentResponse> createComment(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId,
            @Valid @RequestBody CreateIssueCommentRequest request) {
        IssueCommentResponse response = commentService.createComment(userDetails.getId(), issueId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<IssueCommentResponse>> getComments(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId) {
        List<IssueCommentResponse> response = commentService.getComments(userDetails.getId(), issueId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<IssueCommentResponse> updateComment(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId,
            @PathVariable("commentId") UUID commentId,
            @Valid @RequestBody UpdateIssueCommentRequest request) {
        IssueCommentResponse response = commentService.updateComment(userDetails.getId(), issueId, commentId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<GenericResponse> deleteComment(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId,
            @PathVariable("commentId") UUID commentId) {
        GenericResponse response = commentService.deleteComment(userDetails.getId(), issueId, commentId);
        return ResponseEntity.ok(response);
    }
}
