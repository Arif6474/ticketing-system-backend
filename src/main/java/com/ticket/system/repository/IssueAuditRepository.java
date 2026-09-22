package com.ticket.system.repository;

import com.ticket.system.entity.IssueAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueAuditRepository extends JpaRepository<IssueAudit, UUID> {
    List<IssueAudit> findByIssueIdOrderByCreatedAtAscIdAsc(UUID issueId);
}
