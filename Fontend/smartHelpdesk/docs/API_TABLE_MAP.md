# Bảng dữ liệu → API

Đối chiếu 2026-09-12: SQL tại `../../DB/setup-mysql.sql`, `../../Backend/KITS-BZCOM-BE/database/setup-mysql.sql`, migration Liquibase, controller/DTO/security/service backend và service/view frontend. Không truy cập dữ liệu DB thật.

URL dưới đây có prefix `/api`. R = đọc, W = ghi. Xác thực JWT còn có thể đọc members trên các API được bảo vệ.

| Method | URL sau `/api` | Quyền | Bảng / tác động chính |
| --- | --- | --- | --- |
| POST | `/auth/login` | Public | R members, companies; cấp token |
| POST | `/auth/register` | Public | W members; R companies |
| POST | `/auth/refresh` | Refresh token hợp lệ | R members; xoay token trong bộ nhớ |
| POST | `/auth/logout` | Public; gửi JWT để revoke | Thu hồi refresh token trong bộ nhớ |
| POST | `/v1/auth/login` | Public | Alias login |
| POST | `/v1/auth/register` | Public | Alias register |
| POST | `/v1/auth/refresh` | Refresh token hợp lệ | Alias refresh |
| POST | `/v1/auth/logout` | Public; JWT tùy chọn | Alias logout |
| POST | `/members` | Public | W members; R companies; đăng ký thay thế |
| GET | `/members` | ADMIN | R members, companies |
| GET | `/members/{id}` | Đã đăng nhập | R members, companies |
| GET | `/companies` | Public | R companies |
| GET | `/companies/{companyId}` | Đã đăng nhập | R companies |
| POST | `/requests` | CLIENT có công ty | W requests, request_histories, alerts; R members, companies |
| GET | `/requests` | Phạm vi request | R requests, members, companies |
| GET | `/requests/stats` | ADMIN | R requests, members |
| GET | `/requests/{id}` | Phạm vi request | R requests, members, companies, request_histories |
| PATCH | `/requests/{id}/assign` | ADMIN | W requests, request_histories, alerts; R members, companies |
| PATCH | `/requests/{id}/status` | ADMIN / Developer được giao | W requests, request_histories, alerts; R members, companies |
| GET | `/requests/{id}/history` | Phạm vi request | R request_histories, requests, members |
| POST | `/requests/classify` | Đã đăng nhập | Xử lý description, không ghi bảng |
| POST | `/requests/suggest-priority` | Đã đăng nhập | Xử lý description, không ghi bảng |
| GET | `/requests/{id}/summary` | Phạm vi request | R requests |
| GET | `/alerts` | Người nhận | R alerts |
| PATCH | `/alerts/{id}/read` | Người nhận | W alerts.is_read |
| GET | `/chat/inbox` | Phạm vi request | R chat_messages, requests, members + RAM |
| GET | `/chat/requests/{requestId}/messages` | Phạm vi request | R chat_messages, requests, members + RAM |
| POST | `/chat/requests/{requestId}/messages` | Phạm vi request | R requests, members; buffer RAM → W chat_messages sau inactivity |
| PATCH | `/chat/messages/{messageId}/read` | Phạm vi request | W isRead trong DB/RAM; R requests, members |

Phạm vi request: Admin tất cả; Client theo client_id của chính mình; Developer theo assigned_developer_id. Chat kiểm tra quan hệ tương tự, nhưng từ chối truy cập hiện trả lỗi nghiệp vụ HTTP 400.

## Khóa và ánh xạ DTO

| Bảng | Khóa / liên kết | Ánh xạ đáng chú ý |
| --- | --- | --- |
| companies | PK company_id | companyId, companyName, address, phone |
| members | PK id; company_id → companies; email unique | companyId, isDeleted, createdAt; companyName tra cứu; không xuất password hash |
| requests | PK id; company_id → companies; client_id/assigned_developer_id → members | clientId/clientName, assignedDeveloperId/assignedDeveloperName; chi tiết thêm email, địa chỉ, history |
| request_histories | PK id; request_id → requests; changed_by → members | changedBy UUID, changedByName tra cứu; fromStatus/toStatus có thể null |
| alerts | PK id; request_id → requests; target_member_id → members | targetMemberId, alertType, isRead; DTO không có requestTitle |
| chat_messages | PK id; request_id → requests; sender_id → members | senderName/senderRole/requestTitle tra cứu; persisted là metadata, không phải cột DB; isRead là cờ chung |

DB cascade xóa request tới lịch sử/alert/chat; hiện không có controller DELETE request. Cột soft-delete member không đồng nghĩa có endpoint xóa. Không tự suy CRUD từ bảng.

Frontend chưa gọi alias `/v1/auth/*`, `POST /members`, `GET /companies/{companyId}`. `GET /members/{id}` có service nhưng chưa có trang hồ sơ riêng. Các URL màn hình và khác biệt tài liệu cũ được tổng hợp trong [README](../README.md).
