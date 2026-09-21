package com.ticket.system.repository;

import com.ticket.system.entity.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IssueSpecification {

    public static Specification<Issue> filterIssues(
            String search,
            UUID projectId,
            UUID moduleId,
            IssueType type,
            IssuePriority priority,
            IssueStage stage,
            VerificationStatus verificationStatus,
            UUID reporterId,
            UUID organizationId,
            UUID memberUserId) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
            }

            if (projectId != null) {
                predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));
            }

            if (moduleId != null) {
                predicates.add(criteriaBuilder.equal(root.get("module").get("id"), moduleId));
            }

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }

            if (stage != null) {
                predicates.add(criteriaBuilder.equal(root.get("stage"), stage));
            }

            if (verificationStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("verificationStatus"), verificationStatus));
            }

            if (reporterId != null) {
                predicates.add(criteriaBuilder.equal(root.get("reporter").get("id"), reporterId));
            }

            if (organizationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("project").get("organization").get("id"), organizationId));
            }

            if (memberUserId != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<ProjectMembership> membershipRoot = subquery.from(ProjectMembership.class);
                subquery.select(membershipRoot.get("project").get("id"))
                        .where(criteriaBuilder.equal(membershipRoot.get("user").get("id"), memberUserId));
                predicates.add(root.get("project").get("id").in(subquery));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
