package com.example.meeting_room_booking.room;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Page<Room> findByActiveTrueAndCapacityGreaterThanEqual(int minCapacity, Pageable pageable);

    Page<Room> findByCapacityGreaterThanEqual(int minCapacity, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM bookings b
                WHERE b.room_id = :roomId AND b.status = 'CONFIRMED' AND b.end_time > now())
            """, nativeQuery = true)
    boolean hasFutureBookings(@Param("roomId") Long roomId);

    @Query("""
            select r from Room r
            where r.active = true
              and r.capacity >= :minCapacity
              and not exists (
                  select 1 from Booking b
                  where b.room.id = r.id
                    and b.status = com.example.meeting_room_booking.booking.BookingStatus.CONFIRMED
                    and b.startTime < :rangeEnd
                    and b.endTime > :rangeStart)
            """)
    Page<Room> findAvailable(@Param("rangeStart") Instant rangeStart,
                             @Param("rangeEnd") Instant rangeEnd,
                             @Param("minCapacity") int minCapacity,
                             Pageable pageable);
}