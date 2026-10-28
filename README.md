# Hệ thống đặt phòng họp — REST API

Backend cho công ty có ~10 phòng họp và ~200 nhân viên, thay thế cách đặt phòng qua tin nhắn (dễ bị trùng lịch) bằng API đặt phòng có kiểm tra chặt chẽ.

**Tech stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Flyway · Spring Security + JWT (HS256) · springdoc OpenAPI · spring-boot-starter-test + Testcontainers

---

## 1. Cách chạy

### Docker

```bash
docker compose up --build
```

- API: <http://localhost:8080/api/v1>
- Health: <http://localhost:8080/actuator/health>
- Admin mặc định (seed lần đầu): `admin@company.com` / `Admin@12345`

Các biến môi trường quan trọng:

| Biến | Dev mặc định | Production |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `localhost:5432/booking` | bắt buộc set |
| `JWT_SECRET` | dev-only (cảnh báo khi dùng) | **bắt buộc**, tối thiểu 32 ký tự |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@company.com` / `Admin@12345` | **bắt buộc** |
| `BOOKING_TIMEZONE` | `Asia/Ho_Chi_Minh` | tùy công ty |
| `BOOKING_WORK_START` / `BOOKING_WORK_END` | `08:00` / `18:00` | tùy công ty |
| `BOOKING_MAX_ADVANCE_DAYS` | `60` | tùy công ty |

Với profile `prod`, thiếu biến môi trường là ứng dụng **dừng khởi động** để tránh chạy thật bằng secret mặc định.

## 2. Thiết kế

### Kiến trúc

```
Controller → Service → Repository (Spring Data JPA / Specifications)
```

- Stateless: Bearer JWT, `SessionCreationPolicy.STATELESS`, `@EnableMethodSecurity`, `@PreAuthorize("hasRole('ADMIN')")` cho API quản trị.
- Mọi response là record DTO, entity không lộ ra ngoài.
- Lỗi trả về dạng `ProblemDetail` (RFC 7807); validation trả map lỗi theo field.
- Flyway quản lý schema, `ddl-auto=validate`.

### Database

Bảng `users`, `rooms`, `bookings` với index và ràng buộc:

- **Chống trùng lịch:** `EXCLUDE USING gist (room_id WITH =, tstzrange(start_time, end_time, '[)') WITH &&) WHERE status = 'CONFIRMED'`.
- `chk_booking_time` (`end > start`), `chk_booking_attendees` (`attendees > 0`), unique email ở dạng `lower(email)`.
- Index: `idx_bookings_room_time`, `idx_bookings_user_time`.

---

## 3. Quyết định thiết kế & lý do

1. **Chống trùng bằng ràng buộc DB (`EXCLUDE GIST`), không check ở code.** Tránh race condition giữa 2 request đồng thời; DB từ chối INSERT/UPDATE tạo khoảng thời gian giao nhau → app chỉ map thành `409 Conflict`.
2. **Lưu `TIMESTAMPTZ` + `Instant`, giờ làm việc theo múi giờ công ty** (`Asia/Ho_Chi_Minh` mặc định) để tránh lệch giờ.
3. **Soft delete** cho phòng và booking (`status = CANCELLED`): giữ lịch sử, giải phóng slot sau khi huỷ.
4. **JWT stateless HS256**, token 60 phút. (Chưa refresh token — chưa cần ở phạm vi bài.)
5. **Unique email ở tầng DB** bằng index trên `lower(email)` — chặn cả case trùng khác hoa thường ở tầng đồng thời.
6. **Flyway + `ddl-auto=validate`** — migration là nguồn sự thật schema.

---

## 4. API chính (base: `/api/v1`, cần `Authorization: Bearer <token>`)

| Nhóm | Method | Path | USER | ADMIN |
|---|---|---|---|---|
| Auth | POST | `/auth/login` | ✅ | ✅ |
| Phòng | GET | `/rooms?minCapacity=&includeInactive=` | ✅ | ✅ |
| Phòng | GET | `/rooms/available?start=&end=&minCapacity=` | ✅ | ✅ |
| Phòng | GET | `/rooms/{id}` | ✅ | ✅ |
| Phòng | POST / PUT / DELETE / POST activate | `/rooms` | ❌ | ✅ |
| Đặt phòng | POST | `/bookings` | ✅ | ✅ |
| Đặt phòng | GET | `/bookings/me` | ✅ | ✅ |
| Đặt phòng | GET | `/bookings/{id}` / PATCH / POST cancel | chủ sở hữu | + tất cả |
| Đặt phòng | GET | `/bookings` (lọc phòng/người/trạng thái) | ❌ | ✅ |
| Đặt phòng | GET | `/rooms/{roomId}/bookings` | ✅ | ✅ |
| Users | GET `/users/me`, POST `/users`, GET `/users`, PATCH `/users/{id}`, POST `/users/{id}/reset-password` | — | tùy | ✅ |

Mã lỗi: `400` (validation), `401` (auth), `403` (không quyền), `404` (không tồn tại), `409` (trùng lịch/xung đột), `429` (rate limit login).


## 5. Hạn chế đã biết (production-readiness)

- Swagger UI chưa được permit trong `SecurityConfig` (trả 401 khi mở).
- JWT chưa có refresh token / logout / thu hồi.
- Rate limit đăng nhập lưu trong bộ nhớ (chỉ đúng khi chạy 1 instance).
- Chưa gửi email/thông báo; chưa check-in & no-show.
