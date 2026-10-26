package com.example.meeting_room_booking;

import org.springframework.boot.SpringApplication;

public class TestMeetingRoomBookingApplication {

	public static void main(String[] args) {
		SpringApplication.from(MeetingRoomBookingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
