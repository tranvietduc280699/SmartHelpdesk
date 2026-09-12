# Thống kê API và hướng dẫn Swagger

Đối chiếu source backend ngày 12/09/2026: **6 controller, 25 handler, 29 tổ hợp HTTP method/URL, 26 đường dẫn khác nhau**. Auth có 4 alias dưới `/api/v1/auth`. Ba API AI nằm trong RequestController nhưng được nhóm riêng để tra cứu.

## Tệp bàn giao

- [openapi.yaml](openapi.yaml): đặc tả OpenAPI 3.0.3 để import vào Swagger Editor hoặc Postman.
- [openapi.json](openapi.json): cùng nội dung dạng JSON.
- Đặc tả gồm 63 schema: DTO, enum, phân trang, envelope thành công và lỗi.

Đây là đặc tả độc lập; endpoint `/v3/api-docs` và Swagger UI của ứng dụng hiện vẫn được sinh từ annotation cũ. Các file này không tự thay thế tài liệu runtime.

## Cách sử dụng

1. Mở Swagger Editor, chọn **File → Import file**, chọn `docs/openapi.yaml` (hoặc JSON).
2. Kiểm tra server, mặc định `http://localhost:8080`.
3. Đăng ký tài khoản rồi gọi `POST /api/auth/login`.
4. Chọn **Authorize**, dán giá trị `data.accessToken` (không thêm tiền tố Bearer trong ô nhập).
5. Mở endpoint, chọn **Try it out**, điền tham số/body và **Execute**. UUID/mã công ty trong ví dụ là minh họa, cần thay bằng ID thực tế.

Swagger Editor ở origin khác backend có thể không gọi được API do cấu hình hiện tại tắt CORS. Có thể import đặc tả vào Postman để gọi trực tiếp hoặc dùng proxy cùng origin. Đọc tài liệu không cần khởi động backend.

## Số lượng theo nhóm

| Nhóm | Method/URL |
|---|---:|
| Auth | 8 |
| Members | 3 |
| Companies | 2 |
| Requests | 7 |
| AI | 3 |
| Alerts | 2 |
| Chat | 4 |
| **Tổng** | **29** |

## Toàn bộ endpoint

“Đã đăng nhập” là mọi role có JWT hợp lệ. Quyền theo chủ request/developer được giao được kiểm tra theo ID user. Public không cần token; logout có thể nhận token để thu hồi refresh token.

| Method | URL | Chức năng | Quyền | Request body | data trả về |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | Đăng nhập | Public | LoginRequest | LoginAuthResponse |
| POST | `/api/auth/register` | Đăng ký tài khoản | Public | RegisterRequest | MemberResponse |
| POST | `/api/auth/refresh` | Làm mới token | Public | TokenRefreshRequest | RefreshTokenResponse |
| POST | `/api/auth/logout` | Đăng xuất | Public | Không | Không có field data |
| POST | `/api/v1/auth/login` | Đăng nhập | Public | LoginRequest | LoginAuthResponse |
| POST | `/api/v1/auth/register` | Đăng ký tài khoản | Public | RegisterRequest | MemberResponse |
| POST | `/api/v1/auth/refresh` | Làm mới token | Public | TokenRefreshRequest | RefreshTokenResponse |
| POST | `/api/v1/auth/logout` | Đăng xuất | Public | Không | Không có field data |
| POST | `/api/members` | Đăng ký member | Public | RegisterRequest | MemberResponse |
| GET | `/api/members` | Danh sách member | ADMIN | Không | MemberResponse[] |
| GET | `/api/members/{id}` | Chi tiết member | Đã đăng nhập | Không | MemberResponse |
| GET | `/api/companies` | Danh sách công ty | Public | Không | CompanyResponse[] |
| GET | `/api/companies/{companyId}` | Chi tiết công ty | Đã đăng nhập | Không | CompanyResponse |
| POST | `/api/requests` | Tạo yêu cầu hỗ trợ | CLIENT | CreateRequestDto | RequestResponse |
| GET | `/api/requests` | Danh sách yêu cầu | Đã đăng nhập | Không | RequestPage |
| GET | `/api/requests/stats` | Thống kê yêu cầu | ADMIN | Không | RequestStatsResponse |
| GET | `/api/requests/{id}` | Chi tiết yêu cầu | ADMIN hoặc client tạo request hoặc developer được giao | Không | RequestDetailResponse |
| PATCH | `/api/requests/{id}/assign` | Phân công developer | ADMIN | AssignDeveloperDto (tùy chọn) | RequestResponse |
| PATCH | `/api/requests/{id}/status` | Cập nhật trạng thái | ADMIN hoặc developer được giao | UpdateRequestStatusDto | RequestResponse |
| GET | `/api/requests/{id}/history` | Lịch sử yêu cầu | ADMIN hoặc client tạo request hoặc developer được giao | Không | RequestHistoryResponse[] |
| POST | `/api/requests/classify` | Phân loại yêu cầu | Đã đăng nhập | ClassifyRequestDto | ClassifyResponseDto |
| POST | `/api/requests/suggest-priority` | Gợi ý độ ưu tiên | Đã đăng nhập | SuggestPriorityDto | SuggestPriorityResponseDto |
| GET | `/api/requests/{id}/summary` | Tóm tắt yêu cầu | Đã đăng nhập | Không | RequestSummaryResponseDto |
| GET | `/api/alerts` | Thông báo của tôi | Đã đăng nhập | Không | AlertResponse[] |
| PATCH | `/api/alerts/{id}/read` | Đánh dấu thông báo đã đọc | Đúng người nhận thông báo | Không | AlertResponse |
| GET | `/api/chat/inbox` | Hộp thư chat | Đã đăng nhập | Không | ChatMessageResponse[] |
| GET | `/api/chat/requests/{requestId}/messages` | Lịch sử chat | ADMIN hoặc client tạo request hoặc developer được giao | Không | ChatMessageResponse[] |
| POST | `/api/chat/requests/{requestId}/messages` | Gửi tin nhắn | ADMIN hoặc client tạo request hoặc developer được giao | SendChatMessageRequest | ChatMessageResponse |
| PATCH | `/api/chat/messages/{messageId}/read` | Đánh dấu tin nhắn đã đọc | ADMIN hoặc client tạo request hoặc developer được giao | Không | ChatMessageResponse |

## Quy ước và hành vi thực tế

- Content-Type: `application/json`; UUID là chuỗi; thời gian Instant theo ISO-8601 UTC.
- Response thành công có `status`, `message`, `data`; ResponseGeneral bỏ field null. Logout chỉ có `status: 200` và `message: "Logout successful"`.
- Tạo member/đăng ký/tạo request trả HTTP **201**, nhưng `status` trong body vẫn **200**.
- `VAL_400`: validation/body JSON sai, có thể kèm `data` là map field → lỗi. `BAD_400`: email trùng, chuyển trạng thái sai, không có developer phù hợp hoặc không có quyền tài nguyên ở tầng nghiệp vụ.
- `AUTH_401`: sai thông tin đăng nhập/refresh token. Thiếu hoặc sai bearer ở endpoint bảo vệ hiện trả **403** theo SecurityConfig và AuthFeatureTest; lỗi filter không bảo đảm envelope JSON.
- `AUTH_403`: AccessDeniedException đến controller advice; `RES_404`: không tồn tại; `SYS_500`: catch-all. Chuyển kiểu path/query sai, page âm/size không dương hoặc sortBy sai có thể bị catch-all thành 500.
- Không bổ sung ràng buộc độ dài từ DB thành validation DTO: title/email/name/phone hiện không có @Size, dù cột DB có giới hạn. Vì vậy không cam kết lỗi 400 khi vượt giới hạn DB.
- Enum nhận đúng chữ hoa. `AlertType` gồm cả `REQUEST_REGISTERED`. `HistoryAction.REOPEN` tồn tại trong enum nhưng chưa có API mở lại.

## Xác thực và quyền truy cập

- Access token mặc định 24 giờ; refresh token 7 ngày. TTL trong response refresh tính bằng **mili giây**, phụ thuộc cấu hình expiration-ms.
- Refresh xoay vòng một lần, lưu trong bộ nhớ tiến trình. Restart làm mất refresh token store. User-Agent chỉ lưu metadata, không ràng buộc thiết bị.
- Logout có JWT sẽ thu hồi toàn bộ refresh token của user, nhưng access token đã phát vẫn có hiệu lực đến khi hết hạn.
- Đăng ký public hiện cho phép truyền cả ADMIN, DEVELOPER và CLIENT. Chi tiết member chỉ yêu cầu đăng nhập, không giới hạn chính chủ. Danh sách member là ADMIN-only và dùng findAll.
- Danh sách company là public; chi tiết company yêu cầu đăng nhập. companyId là chuỗi, không phải UUID.
- Danh sách request lọc theo user: ADMIN toàn bộ, CLIENT do mình tạo, DEVELOPER được giao. Detail/history/chat kiểm tra admin hoặc chủ request hoặc người được giao; không phải mọi người cùng công ty.
- Summary hiện chỉ kiểm tra đăng nhập và request tồn tại, **chưa kiểm tra quyền sở hữu/phân công**. Đặc tả phản ánh source hiện tại.

## Quy trình request, AI và chat

1. CLIENT thuộc công ty tạo request: trạng thái PENDING, chưa gán developer; ghi lịch sử CREATE và thông báo admin ở mọi mức ưu tiên.
2. ADMIN phân công thủ công bằng developerId hoặc tự động bằng body rỗng/`{}`/developerId null. Tự động chọn developer active chưa xóa, ít task PENDING/IN_PROGRESS nhất; hòa thì chọn người hoàn thành gần nhất, sau đó ID nhỏ nhất theo chuỗi. Thủ công chỉ kiểm tra tồn tại và role.
3. ADMIN hoặc người được giao chuyển PENDING → IN_PROGRESS → DONE. Không cho bỏ bước, đi lùi, cùng trạng thái hoặc reopen. Mỗi lần đổi có history và alert cho client.
4. Thông báo sắp xếp mới nhất trước; chỉ người nhận được đánh dấu đọc.
5. AI là RuleBasedLlmAdapter nội bộ: phân loại theo keyword, gợi ý ưu tiên theo keyword, summary theo chuỗi định dạng. **Không có lời gọi HTTP đến API bên thứ ba trong source hiện tại.** JDBC MySQL/H2 là kết nối CSDL, không phải REST endpoint.
6. Chat là REST, không có WebSocket/SSE. History tăng dần; inbox giảm dần, chứa tất cả tin của các request có quyền xem. Tin gửi mới ở buffer, persisted=false; sau 10 phút không gửi mới, tác vụ mỗi 30 giây lưu DB. isRead là cờ chung, không phải receipt theo từng người.

## Phạm vi đối chiếu và kiểm tra

Nguồn chính: controller, DTO, SecurityConfig/SecurityConstants, JWT filter/provider, facade, service, validator, specification, repository, entity và exception advice. Đối chiếu thêm cấu hình, migration và các trường hợp kiểm thử sẵn có. README và FRONTEND_API_HANDOFF cũ có chỗ khác implementation nên không dùng làm chuẩn thay source.

Các URL hạ tầng `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/h2-console/**` không tính vào 29 API nghiệp vụ. H2 console phụ thuộc profile local. Chưa có controller CRUD công ty đầy đủ, sửa/xóa member, xóa request, upload file hoặc chat realtime.

Chạy kiểm tra đặc tả và độ phủ source bằng:

```powershell
py -3 docs/validate_openapi.py
```

Script kiểm tra JSON, tham chiếu, operationId, path parameter, ví dụ request/response, và đối chiếu toàn bộ method/URL của controller. Đây là kiểm tra tĩnh; không thay thế kiểm thử HTTP runtime hay validator OpenAPI tiêu chuẩn. Không chạy lại backend trong lần bàn giao tài liệu này.

