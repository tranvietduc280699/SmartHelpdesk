# Frontend API Handoff

Tài liệu này mô tả API backend để ChatGPT/frontend sử dụng.

## 1. Runtime

- Base URL local: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- API prefix: `/api`
- JSON content type: `application/json`
- UUID và enum được gửi dưới dạng chuỗi JSON.
- `Instant` được gửi dưới dạng ISO-8601, ví dụ `2026-09-11T14:18:17Z`.
- Frontend khác origin cần proxy dev hoặc backend cần bật CORS.

## 2. Common response envelope

Tất cả API trả về:

```ts
type ApiResponse<T> = {
  status: number;
  errorCode?: string;
  message: string;
  data: T | null;
};
```

Success thường có `status: 200` trong body. HTTP status vẫn phải được kiểm tra.
Lỗi validation có dạng:

```json
{
  "status": 400,
  "errorCode": "VAL_400",
  "message": "...",
  "data": {
    "fieldName": "field error message"
  }
}
```

Các lỗi thường gặp: `AUTH_401` (chưa đăng nhập/token sai), `AUTH_403` (không đủ quyền), `VAL_400` (dữ liệu sai), `RES_404` (không tìm thấy), `SYS_500`.

## 3. Enums

```ts
type Role = "ADMIN" | "DEVELOPER" | "CLIENT";
type RequestCategory = "BUG" | "FEATURE" | "INQUIRY";
type RequestPriority = "HIGH" | "MEDIUM" | "LOW";
type RequestStatus = "PENDING" | "IN_PROGRESS" | "DONE";
type AlertType = "ASSIGNED" | "STATUS_CHANGED" | "HIGH_PRIORITY_REGISTERED";
type HistoryAction = "CREATE" | "ASSIGN" | "UPDATE" | "CLOSE" | "REOPEN";
```

## 4. Authentication

Gửi token cho API cần đăng nhập:

```http
Authorization: Bearer <accessToken>
```

Access token sống khoảng 24 giờ. Refresh token sống khoảng 7 ngày, bị rotate và dùng một lần.

### POST `/api/auth/login`

Public. Alternate path: `/api/v1/auth/login`.

Request:

```json
{ "email": "user@example.com", "password": "password" }
```

Fields bắt buộc: `email` đúng định dạng email, `password` không rỗng.

Response `data`:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "User Name",
    "role": "CLIENT",
    "phone": "0900000000",
    "companyId": "COMPANY001",
    "companyName": "Company",
    "status": "active"
  }
}
```

### POST `/api/auth/register`

Public. Alternate path: `/api/v1/auth/register`.

Request:

```json
{
  "email": "user@example.com",
  "password": "password",
  "name": "User Name",
  "role": "CLIENT",
  "phone": "0900000000",
  "companyId": "COMPANY001"
}
```

Bắt buộc: `email`, `password`, `name`, `role`. Tùy chọn: `phone`, `companyId`.
`role` nhận `ADMIN`, `DEVELOPER`, `CLIENT`; frontend không nên cho người dùng tự chọn `ADMIN` nếu không có chủ đích.
HTTP `201`.

### POST `/api/members`

Public. Cùng request/response với register, HTTP `201`.

### POST `/api/auth/refresh`

Public. Alternate path: `/api/v1/auth/refresh`.

Request:

```json
{ "refreshToken": "..." }
```

Optional header: `User-Agent`.

Response `data`:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "accessTokenTtl": 86400000,
  "refreshTokenTtl": 604800000
}
```

### POST `/api/auth/logout`

Public theo security config. Alternate path: `/api/v1/auth/logout`.
Không có body. Response `data: null`. Khi có user đăng nhập, backend revoke refresh token.

## 5. Member APIs

### GET `/api/members`

Role: `ADMIN`. Không có query/body.

Response `data`: `MemberResponse[]`.

```ts
type MemberResponse = {
  id: string;
  email: string;
  name: string;
  role: Role;
  phone: string | null;
  companyId: string | null;
  companyName: string | null;
  status: string;
  isDeleted: boolean | null;
  createdAt: string | null;
};
```

### GET `/api/members/{id}`

Role: bất kỳ user đã đăng nhập. Path `id` là UUID. Response `data: MemberResponse`.

## 6. Request APIs

### POST `/api/requests`

Role: `CLIENT`.

Request:

```json
{
  "title": "Cannot access dashboard",
  "description": "Details of the problem",
  "category": "BUG",
  "priority": "MEDIUM"
}
```

Bắt buộc: `title`, `category`. Tùy chọn: `description`, `priority`; priority mặc định `MEDIUM`.
Request mới luôn có status `PENDING`. HTTP `201`.

### GET `/api/requests`

Role: user đã đăng nhập.

Query tùy chọn:

```text
category=BUG|FEATURE|INQUIRY
priority=HIGH|MEDIUM|LOW
status=PENDING|IN_PROGRESS|DONE
search=text
page=0
size=10
sortBy=createdAt
sortDir=asc|desc
```

Visibility: `ADMIN` xem tất cả, `DEVELOPER` xem request được giao, `CLIENT` xem request của mình.

Response `data` là Spring Page:

```ts
type Page<T> = {
  content: T[];
  pageable: object;
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: object;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
};
```

`RequestResponse`:

```ts
type RequestResponse = {
  id: string;
  companyId: string | null;
  companyName: string | null;
  title: string;
  description: string | null;
  category: RequestCategory;
  priority: RequestPriority;
  status: RequestStatus;
  clientId: string;
  clientName: string | null;
  assignedDeveloperId: string | null;
  assignedDeveloperName: string | null;
  createdAt: string;
  updatedAt: string;
};
```

### GET `/api/requests/stats`

Role: `ADMIN`. Không có query/body.

Response `data`:

```ts
type RequestStatsResponse = {
  totalRequests: number;
  completedRequests: number;
  completionRate: number;
  requestsByCategory: Record<RequestCategory, number>;
  requestsByDeveloper: DeveloperTaskStats[];
};

type DeveloperTaskStats = {
  developerId: string;
  developerName: string;
  completedCount: number;
  inProgressCount: number;
  pendingCount: number;
  totalAssigned: number;
};
```

### GET `/api/requests/{id}`

Role: user đã đăng nhập nhưng chỉ được xem nếu là `ADMIN`, client tạo request, hoặc developer được assign.

Response `data`:

```ts
type RequestDetailResponse = RequestResponse & {
  companyAddress: string | null;
  companyPhone: string | null;
  clientEmail: string | null;
  assignedDeveloperEmail: string | null;
  history: RequestHistoryResponse[];
};
```

### PATCH `/api/requests/{id}/assign`

Role: `ADMIN`.

Body tùy chọn:

```json
{ "developerId": "developer-uuid" }
```

Nếu body rỗng hoặc `developerId: null`, backend tự chạy thuật toán auto-assignment. Response `data: RequestResponse`.

### PATCH `/api/requests/{id}/status`

Role: `ADMIN` hoặc developer được assign request.

Request:

```json
{ "status": "IN_PROGRESS", "memo": "Started investigation" }
```

Bắt buộc: `status`. `memo` tùy chọn.
Chuyển trạng thái hợp lệ duy nhất: `PENDING -> IN_PROGRESS -> DONE`. Không thể đổi ngược hoặc đổi cùng trạng thái.
Response `data: RequestResponse`.

### GET `/api/requests/{id}/history`

Role: user có quyền xem request.

Response `data: RequestHistoryResponse[]`:

```ts
type RequestHistoryResponse = {
  id: string;
  requestId: string;
  changedBy: string;
  changedByName: string | null;
  action: HistoryAction;
  fromStatus: RequestStatus | null;
  toStatus: RequestStatus | null;
  memo: string | null;
  changedAt: string;
};
```

## 7. Alert APIs

### GET `/api/alerts`

Role: user đã đăng nhập.
Query tùy chọn: `unreadOnly=true|false`, mặc định `false`.

Response `data: AlertResponse[]`:

```ts
type AlertResponse = {
  id: string;
  requestId: string;
  targetMemberId: string;
  alertType: AlertType;
  message: string;
  isRead: boolean;
  createdAt: string;
};
```

### PATCH `/api/alerts/{id}/read`

Role: chỉ user là target của alert đó. Không có body. Response `data: AlertResponse`.

## 8. AI APIs

### POST `/api/requests/classify`

Role: user đã đăng nhập.
Request: `{ "description": "Cannot login after password reset" }`.
`description` bắt buộc và không rỗng.
Response:

```json
{
  "category": "BUG",
  "confidence": 0.95,
  "reason": "The description reports a malfunction."
}
```

### POST `/api/requests/suggest-priority`

Role: user đã đăng nhập.
Request: `{ "description": "Production is completely unavailable" }`.
Response:

```json
{
  "priority": "HIGH",
  "confidence": 0.91,
  "reason": "The issue is urgent and blocks production."
}
```

### GET `/api/requests/{id}/summary`

Role: user có quyền xem request đó.
Response:

```json
{
  "requestId": "uuid",
  "summary": "One or two line summary of the request."
}
```

## 9. Suggested frontend API functions

```ts
// authApi
login(payload: LoginRequest): Promise<ApiResponse<LoginAuthResponse>>
register(payload: RegisterRequest): Promise<ApiResponse<MemberResponse>>
refreshToken(refreshToken: string): Promise<ApiResponse<RefreshTokenResponse>>
logout(): Promise<ApiResponse<null>>

// memberApi
getMembers(): Promise<ApiResponse<MemberResponse[]>>
getMember(id: string): Promise<ApiResponse<MemberResponse>>

// requestApi
createRequest(payload: CreateRequestPayload): Promise<ApiResponse<RequestResponse>>
getRequests(params: RequestListParams): Promise<ApiResponse<Page<RequestResponse>>>
getRequestStats(): Promise<ApiResponse<RequestStatsResponse>>
getRequest(id: string): Promise<ApiResponse<RequestDetailResponse>>
assignDeveloper(id: string, developerId?: string | null): Promise<ApiResponse<RequestResponse>>
updateRequestStatus(id: string, payload: UpdateStatusPayload): Promise<ApiResponse<RequestResponse>>
getRequestHistory(id: string): Promise<ApiResponse<RequestHistoryResponse[]>>
getRequestSummary(id: string): Promise<ApiResponse<RequestSummaryResponse>>

// alertApi
getAlerts(unreadOnly?: boolean): Promise<ApiResponse<AlertResponse[]>>
markAlertAsRead(id: string): Promise<ApiResponse<AlertResponse>>

// aiApi
classifyRequest(description: string): Promise<ApiResponse<ClassifyResponse>>
suggestPriority(description: string): Promise<ApiResponse<SuggestPriorityResponse>>
```

Frontend nên có interceptor: gắn `Authorization`, khi nhận `401` thì gọi refresh một lần rồi retry request; nếu refresh thất bại thì xóa token và đưa về login. Với `403`, hiển thị không đủ quyền và không retry.

## 10. Suggested frontend screens and flow

1. Login: gọi `login`, lưu `accessToken`, `refreshToken`, `user`.
2. Register: gọi `register`; không cho public user tự đăng ký `ADMIN` nếu không được yêu cầu.
3. Client dashboard: `getRequests`, `createRequest`, AI classify/priority trước khi submit, `getAlerts`.
4. Request detail: `getRequest`, `getRequestHistory`, `getRequestSummary`.
5. Developer dashboard: `getRequests`, lọc request được assign, `updateRequestStatus`, xem alerts.
6. Admin dashboard: `getRequests`, `getMembers`, `getStats` nếu backend expose endpoint stats, assign developer, status management, alerts.
7. Logout: gọi `logout`, xóa token local và chuyển về login.

## 11. Important backend notes

- `/` không phải frontend page và có thể trả `403`; dùng Swagger hoặc frontend app riêng.
- `GET /api/requests/stats` là ADMIN-only và trả `RequestStatsResponse` gồm `totalRequests`, `completedRequests`, `completionRate`, `requestsByCategory`, `requestsByDeveloper`.
- `POST /api/requests` trả HTTP `201` nhưng field `status` trong envelope hiện là `200`; frontend nên ưu tiên HTTP status hoặc giữ xử lý thống nhất theo envelope.
- CORS đang disabled trong backend. Khi frontend chạy ở port khác, dùng proxy hoặc bật cấu hình CORS.
- Không hard-code role quyền ở UI thay cho backend; UI chỉ ẩn/hiện chức năng, backend vẫn là nơi quyết định quyền.
