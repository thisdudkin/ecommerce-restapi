package org.example.ecommerce.users.repository.specifications;

import org.example.ecommerce.users.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> hasName(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank()) {
                return null;
            }
            return cb.like(
                cb.lower(root.get("name")),
                "%" + name.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> hasSurname(String surname) {
        return (root, query, cb) -> {
            if (surname == null || surname.isBlank()) {
                return null;
            }
            return cb.like(
                cb.lower(root.get("surname")),
                "%" + surname.trim().toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> withFilters(String name, String surname) {
        return Specification
            .where(hasName(name))
            .and(hasSurname(surname));
    }

}
