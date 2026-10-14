package com.impact.event;

import org.springframework.data.jpa.domain.Specification;
import java.time.Instant;
import java.util.List;

public class EventSpecifications {

    public static Specification<Event> isStatusIn(List<EventStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Event> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("purpose")), pattern)
            );
        };
    }

    public static Specification<Event> hasCategory(EventCategory category) {
        return (root, query, cb) -> {
            if (category == null) {
                return null;
            }
            return cb.equal(root.get("category"), category);
        };
    }

    public static Specification<Event> hasCity(String city) {
        return (root, query, cb) -> {
            if (city == null || city.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("city")), "%" + city.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Event> startsAfter(Instant startFrom) {
        return (root, query, cb) -> {
            if (startFrom == null) {
                return null;
            }
            return cb.greaterThanOrEqualTo(root.get("startAt"), startFrom);
        };
    }

    public static Specification<Event> startsBefore(Instant startTo) {
        return (root, query, cb) -> {
            if (startTo == null) {
                return null;
            }
            return cb.lessThanOrEqualTo(root.get("startAt"), startTo);
        };
    }
}