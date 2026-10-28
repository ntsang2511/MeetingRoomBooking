package com.example.meeting_room_booking.room;

import com.example.meeting_room_booking.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public PageResponse<RoomResponse> list(@RequestParam(defaultValue = "1") int minCapacity,
                                           @RequestParam(defaultValue = "false") boolean includeInactive,
                                           @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return roomService.list(minCapacity, includeInactive, pageable);
    }

    @GetMapping("/available")
    public PageResponse<RoomResponse> available(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(defaultValue = "1") int minCapacity,
            @PageableDefault(size = 20, sort = "capacity") Pageable pageable) {
        return roomService.findAvailable(start.toInstant(), end.toInstant(), minCapacity, pageable);
    }

    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id) {
        return roomService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        return roomService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) {
        roomService.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse activate(@PathVariable Long id) {
        return roomService.activate(id);
    }
}