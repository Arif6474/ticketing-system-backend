package com.ticket.system.service;

import com.ticket.system.dto.response.IssueAuditResponse;
import com.ticket.system.entity.AuditEventType;
import com.ticket.system.entity.Issue;
import com.ticket.system.entity.IssueAudit;
import com.ticket.system.entity.User;
import com.ticket.system.repository.IssueAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IssueAuditService {

    private final IssueAuditRepository auditRepository;

    public IssueAuditService(IssueAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public void recordIssueCreated(Issue issue, User actor) {
        IssueAudit audit = new IssueAudit(issue, actor, AuditEventType.ISSUE_CREATED, null, null, null);
        auditRepository.save(audit);
    }

    @Transactional
    public void recordFieldChange(Issue issue, User actor, String fieldName, String oldValue, String newValue) {
        IssueAudit audit = new IssueAudit(issue, actor, AuditEventType.FIELD_CHANGED, fieldName, oldValue, newValue);
        auditRepository.save(audit);
    }

    @Transactional
    public void recordStageChange(Issue issue, User actor, String oldValue, String newValue) {
        IssueAudit audit = new IssueAudit(issue, actor, AuditEventType.STAGE_CHANGED, "stage", oldValue, newValue);
        auditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<IssueAuditResponse> getAuditsForIssue(UUID issueId) {
        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueId);
        return audits.stream().map(IssueAuditResponse::fromEntity).collect(Collectors.toList());
    }
}
