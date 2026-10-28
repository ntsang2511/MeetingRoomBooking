package com.example.meeting_room_booking.room;

public record RoomResponse(Long id, String name, int capacity, String location,
                           String description, boolean active) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(),
                room.getLocation(), room.getDescription(), room.isActive());
    }
}