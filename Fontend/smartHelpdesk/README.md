# Smart Helpdesk

Frontend Vue 3 + TypeScript + Pinia kết nối backend Spring Boot. Dữ liệu lấy từ API; không có chế độ mock hay tài khoản demo tích hợp.

Tài liệu đối chiếu ngày **12/09/2026** với frontend, controller/DTO/security/service tại `../../Backend/KITS-BZCOM-BE`, SQL và migration Liquibase. Chưa xác minh trên database hoặc backend đang chạy.

## Chạy local

Yêu cầu Node.js `^20.19.0 || >=22.12.0` theo package.json.

1. Chạy `npm ci`.
2. Sao chép `.env.example` thành `.env` nếu chưa có.
3. Chạy backend riêng tại `http://localhost:8080`, theo [README backend](../../Backend/KITS-BZCOM-BE/README.md).
4. Chạy `npm run dev`, mở địa chỉ Vite in ra.

```dotenv
VITE_API_BASE_URL=/api
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

Vite dev và preview chuyển `/api` tới backend. Backend hiện tắt CORS, vì vậy dùng proxy cùng origin. Production cần reverse proxy `/api` hoặc bật CORS nếu API khác origin. Frontend không tự khởi tạo DB.

## Swagger / OpenAPI

- Swagger frontend: **`/swagger/index.html`**, ví dụ `http://localhost:5173/swagger/index.html`.
- File import Swagger Editor/Postman: [public/swagger/openapi.json](public/swagger/openapi.json), phục vụ tại `/swagger/openapi.json`.
- Swagger có sẵn của backend: `http://localhost:8080/swagger-ui/index.html` hoặc `/swagger-ui.html`.
- OpenAPI tự sinh của backend: `http://localhost:8080/v3/api-docs`.

Bản Swagger frontend mô tả **29 thao tác HTTP: 25 thao tác chính và 4 alias auth**, gồm request/response schema, query, JWT, quyền và bảng liên quan. Đây là bản tĩnh theo mã nguồn; không tạo controller mới. Springdoc có sẵn vẫn do backend tự phục vụ khi chạy.

Để thử API: chạy backend, mở Swagger frontend, chọn server `/api`, gọi `POST /auth/login`, nhập `data.accessToken` vào **Authorize**, rồi thử API theo vai trò. Swagger không tự refresh; gọi `/auth/refresh` và cập nhật Authorize khi cần. Logout nên gửi JWT để backend xác định người cần thu hồi refresh token. POST/PATCH thực hiện thay đổi thật trên backend đang kết nối.

Swagger UI cần Internet để tải thư viện CDN; nếu CDN lỗi, tải JSON để import vào công cụ OpenAPI. Cách nhúng theo [tài liệu Swagger UI chính thức](https://swagger.io/docs/open-source-tools/swagger-ui/usage/installation/).

```powershell
# Tạo lại JSON từ backend cùng cấu trúc thư mục hiện tại
node scripts/build-openapi.mjs
# Hoặc chỉ định backend khác
node scripts/build-openapi.mjs "D:/path/to/KITS-BZCOM-BE"
# Đối chiếu endpoint với controller và kiểm tra tham chiếu
node scripts/check-openapi.mjs
```

Script đọc DTO/enum theo cấu trúc Java hiện tại. Danh sách endpoint, quyền và mô tả được khai báo trong script; khi controller/security/service thay đổi cần cập nhật phần đó rồi tạo lại JSON.

## Các bảng và API sử dụng

Chi tiết đủ 29 thao tác nằm trong [docs/API_TABLE_MAP.md](docs/API_TABLE_MAP.md).

| Bảng | API trực tiếp | Sử dụng |
| --- | --- | --- |
| `companies` | `GET /api/companies`, `GET /api/companies/{companyId}` | Danh mục công ty; tra cứu khi đăng ký, hiển thị member/request. Chưa có API tạo/sửa/xóa. |
| `members` | Nhóm `/api/auth/*`; `POST/GET /api/members`, `GET /api/members/{id}` | Tài khoản, đăng nhập, hồ sơ, chọn Developer. Chưa có API sửa/xóa. |
| `requests` | `GET/POST /api/requests`, `GET /api/requests/{id}`, `PATCH .../{id}/assign`, `PATCH .../{id}/status`, `GET .../stats`, `GET .../{id}/summary` | Tạo, tìm kiếm, phân công, trạng thái, KPI và tóm tắt. |
| `request_histories` | `GET /api/requests/{id}/history`; có trong chi tiết request | Tự ghi khi tạo, phân công, đổi trạng thái; không có CRUD lịch sử độc lập. |
| `alerts` | `GET /api/alerts`, `PATCH /api/alerts/{id}/read` | Thông báo người nhận; tự tạo từ vòng đời request. |
| `chat_messages` | `GET/POST /api/chat/requests/{requestId}/messages`, `GET /api/chat/inbox`, `PATCH /api/chat/messages/{messageId}/read` | Chat theo request, hộp thư, đánh dấu đã đọc. |

`POST /api/requests/classify` và `/api/requests/suggest-priority` xử lý description bằng `RuleBasedLlmAdapter`, không tự ghi request và không có bảng AI. Token refresh/revocation lưu trong bộ nhớ backend, không có bảng `refresh_tokens`. `DATABASECHANGELOG` và `DATABASECHANGELOGLOCK` thuộc Liquibase, không có API nghiệp vụ.

## Màn hình và phân quyền

| URL frontend | Chức năng |
| --- | --- |
| `/login` | Đăng nhập/đăng ký, lấy danh sách công ty public |
| `/` | Chuyển trang theo vai trò |
| `/client` | Request tự tạo, tạo mới, gợi ý category/priority |
| `/dev` | Request được giao |
| `/admin` | Danh sách request, KPI, chọn Developer, phân công |
| `/requests/:id` | Chi tiết, lịch sử, tóm tắt, trạng thái, phân công; chat tại `#chat` |
| `/403` | Không đủ quyền |
| URL không khớp | Trang không tìm thấy |

Thanh điều hướng có thông báo và hộp thư chat. Backend thực thi quyền:

- Admin xem tất cả request, phân công và xem thống kê.
- Client chỉ xem request **do chính mình tạo**, không phải mọi request cùng công ty. Chỉ Client được tạo request.
- Developer xem request được giao; được đổi trạng thái request đó. Admin cũng được đổi trạng thái.
- Lịch sử, tóm tắt, chat theo quyền truy cập request. Alert chỉ dành cho người nhận.
- `GET /api/members` chỉ Admin; chi tiết member cho người đã đăng nhập.
- Danh sách công ty public; chi tiết công ty cần đăng nhập.

## Hợp đồng API và nghiệp vụ

JSON dùng camelCase, UUID dạng chuỗi, thời gian ISO-8601. API role `ADMIN/DEVELOPER/CLIENT` đổi thành `admin/dev/client` trong UI. Response ứng dụng dùng envelope:

```json
{ "status": 200, "message": "Success", "data": {} }
```

Đăng ký/tạo request trả HTTP `201`, nhưng `status` trong envelope vẫn là `200`. Lỗi ứng dụng có `errorCode`; validation có ánh xạ tên trường → thông báo trong `data`. Lỗi tại security filter có thể không theo envelope. HTTP service bóc `data`, refresh và retry một lần khi `401`; không refresh khi `403`.

Danh sách request dùng `category`, `priority`, `status`, `search`, `page`, `size`, `sortBy`, `sortDir`. Spring Page trả `number` từ `0`, frontend đổi thành `page`. KPI `completionRate` backend là `0–100`, frontend đổi về `0–1`.

Request chỉ chuyển `PENDING → IN_PROGRESS → DONE`, không nhảy cóc hay mở lại. Backend tự ghi lịch sử. Mọi request mới đều thông báo Admin: HIGH dùng `HIGH_PRIORITY_REGISTERED`, LOW/MEDIUM dùng `REQUEST_REGISTERED`. Phân công thông báo Developer, đổi trạng thái thông báo Client.

Chat đã tích hợp trên UI. Tin mới trả HTTP `200`, `persisted:false`, lưu tạm trong RAM. Backend ghi DB sau **10 phút không gửi thêm tin trong request**, scheduler kiểm tra mỗi 30 giây. API đọc gộp tin DB và tin đang chờ. `isRead` là cờ chung, không phải trạng thái riêng từng người. Tin chưa persist có thể mất khi backend khởi động lại. Chưa có API WebSocket hoặc khôi phục mật khẩu.

## Công ty và các điểm cần đồng bộ

Frontend lấy danh sách/tên công ty từ `GET /api/companies`. Client bắt buộc chọn `KR_SAMSUNG`, `KR_NAVER` hoặc `KR_KAKAO`; Developer luôn gửi `KR_BZCOM`. Không có dữ liệu dự phòng. API lỗi hoặc không có công ty phù hợp sẽ chặn đăng ký Client. Developer vẫn gửi được nhưng backend từ chối nếu `KR_BZCOM` không tồn tại.

**SQL/Liquibase hiện seed ID cũ `KR_CLIENT_Ss`, `KR_CLIENT_Nv`, `KR_CLIENT_Kk`.** Cần đối chiếu dữ liệu thực tế và đồng bộ trước khi dùng form đăng ký. Thay đổi tài liệu không sửa DB.

Backend public register (`/api/auth/register`, `/api/v1/auth/register`, `/api/members`) hiện chấp nhận cả `ADMIN`. UI chỉ cho Client/Developer chưa phải ràng buộc backend; Swagger ghi đúng hành vi này.

Giới hạn DB: email/name member 100 ký tự, phone/companyId 20, title request 255. DTO đăng ký/tạo request chưa có đầy đủ `@Size`, nên lỗi vượt độ dài không chắc trả validation `400`. Không gửi UUID tự sinh, cột audit hoặc password hash. `members.company_id` cho phép null; `requests.company_id` bắt buộc, lấy từ Client đăng nhập.

Backend có alert type `REQUEST_REGISTERED` nhưng type frontend chưa khai báo; cần đồng bộ khi phát triển tiếp. Swagger dùng enum backend.

## Kiểm tra và build

```powershell
node scripts/check-openapi.mjs
npm run build
npm run lint:check
npm test
npm run preview
```

Build sao chép `public/swagger` sang `dist/swagger`. Dữ liệu test chỉ dùng kiểm thử tự động, không nạp vào ứng dụng hay database backend.
