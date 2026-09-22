package com.ticket.system.dto.response;

public class IssueAttachmentDownloadResponse {

    private IssueAttachmentResponse attachment;
    private String downloadUrl;

    public IssueAttachmentDownloadResponse() {
    }

    public IssueAttachmentDownloadResponse(IssueAttachmentResponse attachment, String downloadUrl) {
        this.attachment = attachment;
        this.downloadUrl = downloadUrl;
    }

    public IssueAttachmentResponse getAttachment() {
        return attachment;
    }

    public void setAttachment(IssueAttachmentResponse attachment) {
        this.attachment = attachment;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
