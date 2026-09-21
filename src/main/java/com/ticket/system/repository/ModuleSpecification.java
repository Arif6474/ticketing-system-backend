package com.ticket.system.repository;

import com.ticket.system.entity.Module;
import com.ticket.system.entity.ProjectMembership;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModuleSpecification {

    public static Specification<Module> filterModules(String search, Boolean active, UUID projectId, UUID organizationId, UUID memberUserId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern);
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate));
            }

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), active));
            }

            if (projectId != null) {
                predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));
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
