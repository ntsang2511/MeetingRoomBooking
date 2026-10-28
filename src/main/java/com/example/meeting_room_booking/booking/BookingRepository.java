package com.example.meeting_room_booking.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Override
    @EntityGraph(attributePaths = {"room", "user"})
    Page<Booking> findAll(Specification<Booking> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "user"})
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findDetailById(@Param("id") Long id);
}