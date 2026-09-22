package com.ticket.system.service;

import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.IssueAttachmentDownloadResponse;
import com.ticket.system.dto.response.IssueAttachmentResponse;
import com.ticket.system.entity.*;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.IssueAttachmentRepository;
import com.ticket.system.repository.IssueRepository;
import com.ticket.system.repository.ProjectMembershipRepository;
import com.ticket.system.repository.UserRepository;
import com.ticket.system.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IssueAttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf",
            "text/plain"
    );

    private final IssueAttachmentRepository attachmentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final StorageService storageService;

    public IssueAttachmentService(IssueAttachmentRepository attachmentRepository,
                                  IssueRepository issueRepository,
                                  UserRepository userRepository,
                                  ProjectMembershipRepository membershipRepository,
                                  StorageService storageService) {
        this.attachmentRepository = attachmentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.storageService = storageService;
    }

    @Transactional
    public IssueAttachmentResponse uploadAttachment(UUID currentUserId, UUID issueId, MultipartFile file) {
        User currentUser = getCurrentUser(currentUserId);
        Issue issue = verifyIssueAccess(currentUser, issueId);

        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? sanitizeFilename(file.getOriginalFilename())
                : "unnamed_file";

        String extension = getExtension(originalFilename);
        String randomUuid = UUID.randomUUID().toString();
        String storedFilename = randomUuid + (extension.isEmpty() ? "" : "." + extension);
        String objectKey = "issues/" + issueId + "/" + storedFilename;

        try (InputStream inputStream = file.getInputStream()) {
            storageService.upload(objectKey, inputStream, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file stream");
        }

        IssueAttachment attachment = new IssueAttachment(
                issue,
                currentUser,
                originalFilename,
                storedFilename,
                objectKey,
                file.getContentType(),
                file.getSize()
        );

        IssueAttachment saved;
        try {
            saved = attachmentRepository.save(attachment);
        } catch (Exception ex) {
            // Attempt R2 cleanup on DB save failure
            try {
                storageService.delete(objectKey);
            } catch (Exception cleanupEx) {
                // Log cleanup exception
            }
            throw ex;
        }

        return IssueAttachmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<IssueAttachmentResponse> getAttachments(UUID currentUserId, UUID issueId) {
        User currentUser = getCurrentUser(currentUserId);
        verifyIssueAccess(currentUser, issueId);

        List<IssueAttachment> attachments = attachmentRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueId);
        return attachments.stream()
                .map(IssueAttachmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IssueAttachmentDownloadResponse generateDownloadUrl(UUID currentUserId, UUID issueId, UUID attachmentId) {
        User currentUser = getCurrentUser(currentUserId);
        verifyIssueAccess(currentUser, issueId);

        IssueAttachment attachment = attachmentRepository.findByIdAndIssueId(attachmentId, issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Attachment not found for specified issue"));

        String downloadUrl = storageService.generateDownloadUrl(
                attachment.getObjectKey(),
                attachment.getOriginalFilename(),
                Duration.ofMinutes(15)
        );

        return new IssueAttachmentDownloadResponse(IssueAttachmentResponse.fromEntity(attachment), downloadUrl);
    }

    @Transactional
    public GenericResponse deleteAttachment(UUID currentUserId, UUID issueId, UUID attachmentId) {
        User currentUser = getCurrentUser(currentUserId);
        verifyIssueAccess(currentUser, issueId);

        IssueAttachment attachment = attachmentRepository.findByIdAndIssueId(attachmentId, issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Attachment not found for specified issue"));

        if (currentUser.getRole() != Role.APP_ADMIN && !Objects.equals(attachment.getUploadedBy().getId(), currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the uploader or an APP_ADMIN can delete this attachment");
        }

        // Delete from object storage first (throws exception if R2 deletion fails)
        storageService.delete(attachment.getObjectKey());

        // Delete metadata from DB
        attachmentRepository.delete(attachment);

        return new GenericResponse("Attachment deleted successfully");
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private Issue verifyIssueAccess(User currentUser, UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot access attachments of another organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(issue.getProject().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only access attachments of member projects");
            }
        }

        return issue;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "File size exceeds maximum allowed limit of 10 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + contentType);
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        validateExtensionConsistency(contentType.toLowerCase(), originalFilename);

        // Validate binary magic bytes for supported binary types (buffer size 12 for WEBP RIFF+WEBP check)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read > 0) {
                validateMagicBytes(contentType.toLowerCase(), header, read);
            }
        } catch (IOException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Failed to read file header for magic byte validation");
        }
    }

    private void validateExtensionConsistency(String contentType, String filename) {
        String ext = getExtension(filename);
        if (ext.isEmpty()) {
            return;
        }

        Set<String> forbiddenExecExtensions = Set.of(
                "exe", "sh", "bat", "cmd", "dll", "so", "dylib", "py", "js", "vbs", "jar", "bin", "com", "msi"
        );
        if (forbiddenExecExtensions.contains(ext)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Executable file extensions are strictly forbidden");
        }

        switch (contentType) {
            case "image/jpeg":
                if (!ext.equals("jpg") && !ext.equals("jpeg")) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File extension ." + ext + " does not match declared MIME type " + contentType);
                }
                break;
            case "image/png":
                if (!ext.equals("png")) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File extension ." + ext + " does not match declared MIME type " + contentType);
                }
                break;
            case "image/webp":
                if (!ext.equals("webp")) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File extension ." + ext + " does not match declared MIME type " + contentType);
                }
                break;
            case "application/pdf":
                if (!ext.equals("pdf")) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File extension ." + ext + " does not match declared MIME type " + contentType);
                }
                break;
            case "text/plain":
                Set<String> validTextExtensions = Set.of("txt", "text", "log", "md", "csv");
                if (!validTextExtensions.contains(ext)) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File extension ." + ext + " is not valid for text/plain content");
                }
                break;
        }
    }

    private void validateMagicBytes(String contentType, byte[] header, int read) {
        switch (contentType) {
            case "image/jpeg":
                if (read < 3 || (header[0] & 0xFF) != 0xFF || (header[1] & 0xFF) != 0xD8 || (header[2] & 0xFF) != 0xFF) {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File content does not match JPEG format signature");
                }
                break;
            case "image/png":
                if (read < 4 || (header[0] & 0xFF) != 0x89 || header[1] != 'P' || header[2] != 'N' || header[3] != 'G') {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File content does not match PNG format signature");
                }
                break;
            case "application/pdf":
                if (read < 4 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F') {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File content does not match PDF format signature");
                }
                break;
            case "image/webp":
                if (read < 12 || header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F'
                        || header[8] != 'W' || header[9] != 'E' || header[10] != 'B' || header[11] != 'P') {
                    throw new AppException(HttpStatus.BAD_REQUEST, "File content does not match WEBP format signature");
                }
                break;
            case "text/plain":
                for (int i = 0; i < read; i++) {
                    int val = header[i] & 0xFF;
                    if (val == 0) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "File content contains binary NUL bytes and is not valid text");
                    }
                    if (val != 0x09 && val != 0x0A && val != 0x0D && (val < 0x20 || (val > 0x7E && val < 0xA0))) {
                        throw new AppException(HttpStatus.BAD_REQUEST, "File content contains invalid control characters for text");
                    }
                }
                break;
        }
    }

    private String sanitizeFilename(String filename) {
        String name = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
