package org.example.besmarthelpdesk.specification;

import jakarta.persistence.criteria.Predicate;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RequestSpecification {

    public static Specification<Request> build(UserPrincipal currentUser,
                                               RequestCategory category,
                                               RequestPriority priority,
                                               RequestStatus status,
                                               String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // RBAC Filtering
            if (currentUser != null) {
                boolean isAdmin = currentUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
                boolean isDeveloper = currentUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.DEVELOPER.name()));
                boolean isClient = currentUser.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.CLIENT.name()));

                if (isClient) {
                    predicates.add(cb.equal(root.get("clientId"), currentUser.getId()));
                } else if (isDeveloper && !isAdmin) {
                    predicates.add(cb.equal(root.get("assignedDeveloperId"), currentUser.getId()));
                }
                // Admin has full visibility (no predicate added)
            }

            // Category filter
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // Priority filter
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Search filter (keyword in title or description)
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titlePredicate, descPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
