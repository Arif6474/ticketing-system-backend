CREATE TABLE issue_attachments (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE RESTRICT,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    object_key VARCHAR(512) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issue_attachments_issue_id ON issue_attachments(issue_id);
CREATE INDEX idx_issue_attachments_uploaded_by ON issue_attachments(uploaded_by);
CREATE INDEX idx_issue_attachments_created_at ON issue_attachments(created_at);
