package com.example.meeting_room_booking.booking;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class BookingSpecs {

    private BookingSpecs() {
    }

    /** Lọc theo phòng, người đặt, trạng thái và các lịch giao với khoảng [from, to). */
    public static Specification<Booking> filter(Long roomId, Long userId, BookingStatus status,
                                                Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (roomId != null) {
                predicates.add(cb.equal(root.get("room").get("id"), roomId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThan(root.<Instant>get("endTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.<Instant>get("startTime"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}