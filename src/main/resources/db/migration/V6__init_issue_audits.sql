CREATE TABLE issue_audits (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE RESTRICT,
    actor_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(50) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issue_audits_issue_id ON issue_audits(issue_id);
CREATE INDEX idx_issue_audits_actor_id ON issue_audits(actor_id);
CREATE INDEX idx_issue_audits_created_at ON issue_audits(created_at);
