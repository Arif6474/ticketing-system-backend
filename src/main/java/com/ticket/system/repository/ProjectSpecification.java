package com.ticket.system.repository;

import com.ticket.system.entity.Project;
import com.ticket.system.entity.ProjectMembership;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectSpecification {

    public static Specification<Project> filterProjects(String search, Boolean active, UUID organizationId, UUID memberUserId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate codePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("shortCode")), searchPattern);
                predicates.add(criteriaBuilder.or(namePredicate, codePredicate));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), active));
            }

            if (organizationId != null) {
                predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), organizationId));
            }

            if (memberUserId != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<ProjectMembership> membershipRoot = subquery.from(ProjectMembership.class);
                subquery.select(membershipRoot.get("project").get("id"))
                        .where(criteriaBuilder.equal(membershipRoot.get("user").get("id"), memberUserId));
                predicates.add(root.get("id").in(subquery));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
