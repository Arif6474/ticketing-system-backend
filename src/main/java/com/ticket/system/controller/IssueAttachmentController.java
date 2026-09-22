package com.ticket.system.controller;

import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.IssueAttachmentDownloadResponse;
import com.ticket.system.dto.response.IssueAttachmentResponse;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.IssueAttachmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/attachments")
public class IssueAttachmentController {

    private final IssueAttachmentService attachmentService;

    public IssueAttachmentController(IssueAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IssueAttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId,
            @RequestParam("file") MultipartFile file) {
        IssueAttachmentResponse response = attachmentService.uploadAttachment(userDetails.getId(), issueId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<IssueAttachmentResponse>> getAttachments(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId) {
        List<IssueAttachmentResponse> response = attachmentService.getAttachments(userDetails.getId(), issueId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<IssueAttachmentDownloadResponse> generateDownloadUrl(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId,
            @PathVariable("attachmentId") UUID attachmentId) {
        IssueAttachmentDownloadResponse response = attachmentService.generateDownloadUrl(userDetails.getId(), issueId, attachmentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<GenericResponse> deleteAttachment(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable("issueId") UUID issueId,
            @PathVariable("attachmentId") UUID attachmentId) {
        GenericResponse response = attachmentService.deleteAttachment(userDetails.getId(), issueId, attachmentId);
        return ResponseEntity.ok(response);
    }
}
