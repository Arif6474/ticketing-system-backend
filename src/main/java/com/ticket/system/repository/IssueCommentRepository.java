package com.ticket.system.repository;

import com.ticket.system.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, UUID> {

    List<IssueComment> findByIssueIdOrderByCreatedAtAscIdAsc(UUID issueId);

    Optional<IssueComment> findByIdAndIssueId(UUID id, UUID issueId);

    boolean existsByIssueId(UUID issueId);
}
