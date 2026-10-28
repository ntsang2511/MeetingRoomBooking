ALTER TABLE bookings
    ADD COLUMN attendees INT NOT NULL DEFAULT 1,
    ADD CONSTRAINT chk_booking_attendees CHECK (attendees > 0);

ALTER TABLE users DROP CONSTRAINT users_email_key;
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
