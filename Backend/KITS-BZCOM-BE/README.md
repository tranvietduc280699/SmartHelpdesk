# Smart Helpdesk CRM - Backend API Documentation & Architecture

> Hệ thống Quản Lý Yêu Cầu Hỗ Trợ Khách Hàng Thông Minh (Smart Helpdesk CRM) dành cho Bizcom, tích hợp thuật toán phân công tự động, tính năng AI/LLM, kiểm soát trạng thái nghiêm ngặt, bảo mật phân quyền RBAC và chống Race Condition.

---

## Mục Lục
1. [Tổng Quan Dự Án](#1-tổng-quan-dự-án)
2. [Kiến Trúc & Design Patterns](#2-kiến-trúc--design-patterns)
3. [Giải Đáp: Phía Backend Có Cần CQRS Để Frontend Hoạt Động Được Không?](#3-giải-đáp-phía-backend-có-cần-cqrs-để-frontend-hoạt-động-được-không)
4. [Hướng Dẫn Khởi Chạy Local](#4-hướng-dẫn-khởi-chạy-local)
5. [Tài Khoản & Dữ Liệu Mẫu Thử Nghiệm](#5-tài-khoản--dữ-liệu-mẫu-thử-nghiệm)
6. [Đặc Tả API Cho Frontend (Frontend API Contracts)](#6-đặc-tả-api-cho-frontend-frontend-api-contracts)
    - [Định Dạng Response Chuẩn (Envelope Pattern)](#định-dạng-response-chuẩn-envelope-pattern)
    - [Hướng Dẫn Cấu Hình Axios Cho Frontend](#hướng-dẫn-cấu-hình-axios-cho-frontend)
    - [Bảng Tổng Hợp Danh Sách API](#bảng-tổng-hợp-danh-sách-api)
    - [Chi Tiết Từng Nhóm API](#chi-tiết-từng-nhóm-api)
7. [Postman Test Collection & Newman](#7-postman-test-collection--newman)

---

## 1. Tổng Quan Dự Án

* **Ngôn ngữ & Framework**: Java 21, Spring Boot 3.5.x
* **Cơ sở dữ liệu**: H2 Database (In-memory cho Local/Test) / PostgreSQL (Production)
* **Quản lý Migration**: Liquibase (7 changesets theo dõi cấu trúc DB)
* **Tài liệu API tự động**: Springdoc OpenAPI 3.0 (Swagger UI)
* **Bảo mật**: Spring Security 6, JWT (Access Token + Refresh Token rotation với Token Revocation Store)

---

## 2. Kiến Trúc & Design Patterns

Dự án được xây dựng theo chuẩn mực Senior Software Engineering:

1. **Zero JPA Relationship Mapping (Decoupled Entity Model)**:
    * **Tuyệt đối không dùng** `@ManyToOne`, `@OneToMany`, `@JoinColumn`.
    * Các Entity (`Member`, `Company`, `Request`, `RequestHistory`, `Alert`, `ChatMessage`) chỉ lưu trữ các trường ID nguyên thủy (`UUID` hoặc `String`).
    * Tránh hoàn toàn các vấn đề N+1 queries, lazy loading serialization exception và cascading xóa nhầm dữ liệu. Dữ liệu liên kết được nạp thủ công bằng **Batch Queries** (`findAllById`) ở tầng Facade.
2. **Mẫu Facade Pattern**:
    * `RequestFacade`, `AlertFacade`, `LlmFacade`, `AuthFacade` đóng vai trò giao diện trung gian, gom các Service lại, xử lý nghiệp vụ phân quyền (RBAC) và chuyển đổi Entity $\leftrightarrow$ DTO trước khi trả về Controller.
3. **Mẫu Adapter Pattern**:
    * `LlmAdapter` (cài đặt cụ thể qua `RuleBasedLlmAdapter`): Chuẩn hóa giao diện xử lý ngôn ngữ tự nhiên (phân loại category, gợi ý độ ưu tiên, tóm tắt ticket). Dễ dàng cắm thêm OpenAI API / Gemini API mà không làm thay đổi luồng nghiệp vụ cốt lõi.
4. **Mẫu State Transition / Validator Pattern**:
    * `RequestStatusValidator`: Kiểm soát chặt chẽ luồng vòng đời ticket: `PENDING` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`. Ngăn chặn nhảy cóc trạng thái, ngăn mở lại ticket đã hoàn thành.
5. **Chống Race Condition & Toàn Vẹn Giao Dịch**:
    * Áp dụng **Pessimistic Write Lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)** trên phương thức `findByIdForUpdate` trong `RequestRepository`.
    * Khi 2 request cùng lúc cố gắng nhận việc hoặc cập nhật trạng thái trên cùng 1 ticket, database lock sẽ tuần tự hóa giao dịch, bảo đảm chỉ 1 thao tác thành công.
6. **Thuật Toán Phân Công Tự Động (Auto-Assignment)**:
    * **Quy tắc 1 (Workload thấp nhất)**: Chọn Developer có số lượng task đang làm (`PENDING` + `IN_PROGRESS`) ít nhất.
    * **Quy tắc 2 (Tie-Breaker)**: Nếu hòa nhau, chọn Developer có task hoàn thành gần đây nhất (`MAX(updatedAt)` với trạng thái `DONE`).
    * **Fallback**: So sánh ID tăng dần để đảm bảo tính xác định (deterministic).

---
## 3. Hướng Dẫn Khởi Chạy Local & Tùy Chọn Cơ Sở Dữ Liệu (H2 hoặc MySQL)

### Yêu cầu môi trường
* **JDK 21** (hoặc JDK 17 trở lên)
* **Maven 3.8+** (hoặc dùng trực tiếp `./mvnw` có sẵn trong dự án)
* **MySQL 8.0+** (Nếu muốn chạy với MySQL, hoặc dùng H2 in-memory không cần cài đặt gì thêm)

---

### Tùy Chọn 1: Chạy Với H2 Database (Mặc định - Zero Setup)
Không cần cài đặt cơ sở dữ liệu bên ngoài, phù hợp cho việc test nhanh hoặc chạy unit/integration test:
```bash
# Mặc định kích hoạt profile "local" (H2 in-memory)
./mvnw spring-boot:run
```
* H2 Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:helpdeskdb`, Username: `sa`, Password: `password`)

---

### Tùy Chọn 2: Chạy Với MySQL Database (Lưu trữ bền vững & Làm việc nhóm)
Hệ thống hỗ trợ sẵn cấu hình `mysql` profile trong `src/main/resources/application-mysql.yml`.

#### 1. Khởi động MySQL (hoặc dùng Docker nếu chưa có MySQL cục bộ)
```bash
docker run -d --name local-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=08072004 mysql:8.0
```

#### 2. Khởi chạy ứng dụng với Profile MySQL
Bạn có thể khởi chạy bằng 1 trong các cách sau:

* **Cách A: Dùng tham số dòng lệnh Maven**:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
  ```

* **Cách B: Thiết lập biến môi trường**:
  ```bash
  export SPRING_PROFILES_ACTIVE=mysql
  # Tùy chỉnh thông tin kết nối (nếu khác mặc định):
  export MYSQL_HOST=localhost
  export MYSQL_PORT=3306
  export MYSQL_DB=smart_helpdesk_db
  export MYSQL_USER=root
  export MYSQL_PASSWORD=08072004

  ./mvnw spring-boot:run
  ```

* **Cách C: Cấu hình trong IDE (IntelliJ IDEA / VS Code)**:
    * Trong **Run/Debug Configuration** $\rightarrow$ **Active Profiles**: điền `mysql`.

Khi chạy lần đầu, Liquibase sẽ **tự động khởi tạo database `smart_helpdesk_db`**, tự động tạo toàn bộ bảng (`members`, `companies`, `requests`, `request_histories`, `alerts`, `chat_messages`) và seed sẵn 3 công ty đối tác Hàn Quốc mà bạn không cần phải chạy SQL thủ công!

---

### Sau Khi Khởi Chạy Thành Công (Port 8080)
* **Swagger UI (Tài liệu OpenAPI tương tác trực tiếp)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Schema (JSON)**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Chạy Toàn Bộ Test Suite Tự Động
```bash
./mvnw test
# Kết quả: 26/26 tests PASS 100%
```

---

## 4. Tài Khoản & Dữ Liệu Mẫu Thử Nghiệm

Khi hệ thống khởi chạy, Liquibase tự động seed các công ty đối tác:
* `KR_CLIENT_Ss`: Samsung C&T Corporation
* `KR_CLIENT_Nv`: Naver Corporation
* `KR_CLIENT_Kk`: Kakao Corp

Bạn có thể dùng các tài khoản mẫu sau để test hoặc đăng ký tài khoản mới:

| Vai Trò (Role) | Email | Mật Khẩu | Công Ty / Quyền Hạn |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@bizcom.com` | `Admin@123456` | Quản trị toàn hệ thống, phân công task, xem báo cáo KPI |
| **DEVELOPER** | `dev1@bizcom.com` | `Dev@123456` | Kỹ sư xử lý ticket, cập nhật trạng thái tiến độ |
| **DEVELOPER** | `dev2@bizcom.com` | `Dev@123456` | Kỹ sư xử lý ticket |
| **CLIENT** | `client.samsung@samsung.com` | `Client@123456` | Khách hàng thuộc `Samsung C&T Corporation` (`KR_CLIENT_Ss`) |

---

## 5. Đặc Tả API Cho Frontend (Frontend API Contracts)

### Định Dạng Response Chuẩn (Envelope Pattern)
Mọi API trong hệ thống đều trả về cấu trúc đồng nhất:

#### Khi thành công (HTTP Status 200 / 201):
```json
{
  "status": 200,
  "message": "Success",
  "data": { ... }
}
```

#### Khi có lỗi (HTTP Status 400, 401, 403, 404, 500):
```json
{
  "status": 400,
  "errorCode": "VALIDATION_INVALID_INPUT",
  "message": "Tiêu đề yêu cầu không được để trống",
  "data": {
    "title": "must not be blank"
  }
}
```

---

### Hướng Dẫn Cấu Hình Axios Cho Frontend

Frontend nên tạo một instance Axios kèm Interceptor để tự động gắn Access Token và tự động làm mới token khi Access Token hết hạn:

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Gắn Bearer Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response Interceptor: Tự động refresh khi gặp 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem('refresh_token');
        const res = await axios.post('http://localhost:8080/api/auth/refresh', { refreshToken });
        const newAccessToken = res.data.data.accessToken;
        const newRefreshToken = res.data.data.refreshToken;

        localStorage.setItem('access_token', newAccessToken);
        localStorage.setItem('refresh_token', newRefreshToken);

        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshErr) {
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

### Bảng Tổng Hợp Danh Sách API

| Module | Method | Endpoint | Quyền (Role) | Mô Tả |
| :--- | :---: | :--- | :---: | :--- |
| **Auth** | `POST` | `/api/auth/register` | Public | Đăng ký thành viên mới (kèm `companyId`, `phone`) |
| **Auth** | `POST` | `/api/auth/login` | Public | Đăng nhập lấy `accessToken` và `refreshToken` |
| **Auth** | `POST` | `/api/auth/refresh` | Public | Xoay vòng cấp mới access token qua refresh token |
| **Auth** | `POST` | `/api/auth/logout` | Authenticated | Đăng xuất và thu hồi toàn bộ token đang hoạt động |
| **Requests** | `POST` | `/api/requests` | **CLIENT** | Tạo ticket hỗ trợ mới |
| **Requests** | `GET` | `/api/requests` | Authenticated | Lấy danh sách ticket (Hỗ trợ phân trang, lọc, search, RBAC) |
| **Requests** | `GET` | `/api/requests/{id}` | Authenticated | Xem chi tiết ticket + toàn bộ lịch sử thay đổi (`history`) |
| **Requests** | `GET` | `/api/requests/{id}/history` | Authenticated | Lấy danh sách lịch sử thay đổi (Audit trail) của ticket |
| **Assignment** | `PATCH` | `/api/requests/{id}/assign` | **ADMIN** | Phân công Developer (Tự động thông minh hoặc chỉ định thủ công) |
| **Lifecycle** | `PATCH` | `/api/requests/{id}/status` | **Assigned DEV / ADMIN** | Chuyển trạng thái ticket (`PENDING` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`) |
| **Dashboard** | `GET` | `/api/requests/stats` | **ADMIN** | Báo cáo KPI: Tỷ lệ hoàn thành, theo danh mục, theo Developer |
| **AI Features** | `POST` | `/api/requests/classify` | Authenticated | AI tự động phân loại category (`BUG`, `FEATURE`, `INQUIRY`) |
| **AI Features** | `POST` | `/api/requests/suggest-priority` | Authenticated | AI phân tích mức độ khẩn cấp và gợi ý độ ưu tiên (`HIGH`, ...) |
| **AI Features** | `GET` | `/api/requests/{id}/summary` | Authenticated | AI tóm tắt ngắn gọn nội dung ticket |
| **Alerts** | `GET` | `/api/alerts` | Authenticated | Lấy danh sách thông báo cá nhân (hỗ trợ lọc `?unreadOnly=true`) |
| **Alerts** | `PATCH` | `/api/alerts/{id}/read` | **Owner của Alert** | Đánh dấu thông báo đã đọc |

---

### Chi Tiết Từng Nhóm API

#### 1. Nhóm Xác Thực (Authentication)

##### 1.1 Đăng ký tài khoản (`POST /api/auth/register`)
* **Headers**: `Content-Type: application/json`
* **Request Body**:
```json
{
  "email": "client.samsung@samsung.com",
  "password": "Client@123456",
  "name": "Kim Samsung",
  "role": "CLIENT",
  "companyId": "KR_CLIENT_Ss",
  "phone": "0901234567"
}
```
* **Response Body (201 Created)**:
```json
{
  "status": 200,
  "message": "User registered successfully",
  "data": {
    "id": "7a353609-b68a-4467-932f-a9cb6db34571",
    "email": "client.samsung@samsung.com",
    "name": "Kim Samsung",
    "role": "CLIENT",
    "phone": "0901234567",
    "companyId": "KR_CLIENT_Ss",
    "companyName": "Samsung C&T Corporation",
    "status": "active",
    "isDeleted": false
  }
}
```

##### 1.2 Đăng nhập (`POST /api/auth/login`)
* **Headers**: `Content-Type: application/json`
* **Request Body**:
```json
{
  "email": "client.samsung@samsung.com",
  "password": "Client@123456"
}
```
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": "7a353609-b68a-4467-932f-a9cb6db34571",
      "email": "client.samsung@samsung.com",
      "name": "Kim Samsung",
      "role": "CLIENT",
      "phone": "0901234567",
      "companyId": "KR_CLIENT_Ss",
      "companyName": "Samsung C&T Corporation",
      "status": "active"
    }
  }
}
```

##### 1.3 Cấp mới Access Token (`POST /api/auth/refresh`)
* **Headers**: `Content-Type: application/json`
* **Request Body**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.new...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.new...",
    "accessTokenTtl": 86400000,
    "refreshTokenTtl": 604800000
  }
}
```

---

#### 2. Nhóm Quản Lý Yêu Cầu (Customer Requests)

##### 2.1 Tạo yêu cầu hỗ trợ mới (`POST /api/requests`)
* **Quyền yêu cầu**: `ROLE_CLIENT`
* **Headers**: `Authorization: Bearer <client_token>`
* **Request Body**:
```json
{
  "title": "Giao diện thanh toán bị lỗi 500 khi bấm xác nhận",
  "description": "Khi người dùng ấn nút thanh toán trên trình duyệt Chrome thì xuất hiện màn hình trắng và mã lỗi 500.",
  "category": "BUG",
  "priority": "HIGH"
}
```
* **Ghi chú**: Nếu `priority = HIGH`, hệ thống tự động sinh thông báo cảnh báo khẩn cấp tới tất cả các Admin!
* **Response Body (201 Created)**:
```json
{
  "status": 200,
  "message": "Request created successfully",
  "data": {
    "id": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
    "companyId": "KR_CLIENT_Ss",
    "companyName": "Samsung C&T Corporation",
    "title": "Giao diện thanh toán bị lỗi 500 khi bấm xác nhận",
    "description": "Khi người dùng ấn nút thanh toán trên trình duyệt Chrome thì xuất hiện màn hình trắng và mã lỗi 500.",
    "category": "BUG",
    "priority": "HIGH",
    "status": "PENDING",
    "clientId": "7a353609-b68a-4467-932f-a9cb6db34571",
    "clientName": "Kim Samsung",
    "assignedDeveloperId": null,
    "assignedDeveloperName": null,
    "createdAt": "2026-09-10T15:20:00Z",
    "updatedAt": "2026-09-10T15:20:00Z"
  }
}
```

##### 2.2 Lấy danh sách yêu cầu (`GET /api/requests`)
* **Headers**: `Authorization: Bearer <token>`
* **Query Parameters (tùy chọn)**:
    * `category`: `BUG`, `FEATURE`, `INQUIRY`
    * `priority`: `HIGH`, `MEDIUM`, `LOW`
    * `status`: `PENDING`, `IN_PROGRESS`, `DONE`
    * `search`: Từ khóa tìm kiếm trong title và description
    * `page`: Trang cần xem (mặc định: `0`)
    * `size`: Số phần tử trên trang (mặc định: `10`)
    * `sortBy`: Trường sắp xếp (mặc định: `createdAt`)
    * `sortDir`: Hướng sắp xếp: `asc` hoặc `desc` (mặc định: `desc`)
* **RBAC Data Isolation (Phân quyền dữ liệu)**:
    * User `CLIENT`: Chỉ thấy các ticket thuộc công ty của mình.
    * User `DEVELOPER`: Chỉ thấy các ticket được phân công cho mình.
    * User `ADMIN`: Thấy toàn bộ ticket của toàn hệ thống.
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
        "companyId": "KR_CLIENT_Ss",
        "companyName": "Samsung C&T Corporation",
        "title": "Giao diện thanh toán bị lỗi 500 khi bấm xác nhận",
        "description": "Khi người dùng ấn nút thanh toán...",
        "category": "BUG",
        "priority": "HIGH",
        "status": "PENDING",
        "clientId": "7a353609-b68a-4467-932f-a9cb6db34571",
        "clientName": "Kim Samsung",
        "assignedDeveloperId": null,
        "assignedDeveloperName": null,
        "createdAt": "2026-09-10T15:20:00Z",
        "updatedAt": "2026-09-10T15:20:00Z"
      }
    ],
    "pageable": { ... },
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

##### 2.3 Xem chi tiết yêu cầu (`GET /api/requests/{id}`)
* **Headers**: `Authorization: Bearer <token>`
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
    "companyId": "KR_CLIENT_Ss",
    "companyName": "Samsung C&T Corporation",
    "companyAddress": "Seoul, South Korea",
    "companyPhone": "+82-2145-1114",
    "title": "Giao diện thanh toán bị lỗi 500 khi bấm xác nhận",
    "description": "Khi người dùng ấn nút thanh toán...",
    "category": "BUG",
    "priority": "HIGH",
    "status": "IN_PROGRESS",
    "clientId": "7a353609-b68a-4467-932f-a9cb6db34571",
    "clientName": "Kim Samsung",
    "clientEmail": "client.samsung@samsung.com",
    "assignedDeveloperId": "349fa812-78d1-446a-9bf4-e2239d1045a1",
    "assignedDeveloperName": "Nguyen Van Dev1",
    "assignedDeveloperEmail": "dev1@bizcom.com",
    "createdAt": "2026-09-10T15:20:00Z",
    "updatedAt": "2026-09-10T15:25:00Z",
    "history": [
      {
        "id": "c1f75b6a-921c-4b53-8324-118817da3829",
        "requestId": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
        "changedBy": "7a353609-b68a-4467-932f-a9cb6db34571",
        "changedByName": "Kim Samsung",
        "changedByEmail": "client.samsung@samsung.com",
        "action": "CREATE",
        "fromStatus": null,
        "toStatus": "PENDING",
        "memo": "Initial request creation",
        "changedAt": "2026-09-10T15:20:00Z"
      },
      {
        "id": "d2f85b6a-921c-4b53-8324-118817da3830",
        "requestId": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
        "changedBy": "8a353609-b68a-4467-932f-a9cb6db34570",
        "changedByName": "Bizcom System Admin",
        "changedByEmail": "admin@bizcom.com",
        "action": "ASSIGN",
        "fromStatus": "PENDING",
        "toStatus": "PENDING",
        "memo": "Assigned to developer Nguyen Van Dev1",
        "changedAt": "2026-09-10T15:22:00Z"
      }
    ]
  }
}
```

---

#### 3. Nhóm Phân Công & Vòng Đời Ticket

##### 3.1 Phân công Developer (`PATCH /api/requests/{id}/assign`)
* **Quyền yêu cầu**: `ROLE_ADMIN`
* **Headers**: `Authorization: Bearer <admin_token>`
* **Cách 1 - Kích hoạt thuật toán tự động thông minh**: Gửi Body rỗng `{}`
* **Cách 2 - Phân công thủ công chỉ định**: Gửi Body kèm `developerId`:
```json
{
  "developerId": "349fa812-78d1-446a-9bf4-e2239d1045a1"
}
```
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Developer assigned successfully",
  "data": {
    "id": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
    "assignedDeveloperId": "349fa812-78d1-446a-9bf4-e2239d1045a1",
    "assignedDeveloperName": "Nguyen Van Dev1",
    "status": "PENDING"
  }
}
```

##### 3.2 Cập nhật trạng thái ticket (`PATCH /api/requests/{id}/status`)
* **Quyền yêu cầu**: `ROLE_ADMIN` hoặc **Chính Developer được phân công task đó**.
* **Headers**: `Authorization: Bearer <dev_token>`
* **Quy tắc chuyển đổi nghiêm ngặt (State Pattern)**:
    * Hợp lệ: `PENDING` $\rightarrow$ `IN_PROGRESS`
    * Hợp lệ: `IN_PROGRESS` $\rightarrow$ `DONE`
    * Bị chặn (400 Bad Request): `PENDING` $\rightarrow$ `DONE` (Nhảy cóc)
    * Bị chặn (400 Bad Request): `DONE` $\rightarrow$ `IN_PROGRESS` (Không cho mở lại ticket đã đóng)
    * Bị chặn (400 Bad Request): Cập nhật sang cùng trạng thái hiện tại
* **Request Body**:
```json
{
  "status": "IN_PROGRESS",
  "memo": "Bắt đầu kiểm tra log thanh toán trên server"
}
```
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Status updated successfully",
  "data": {
    "id": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
    "status": "IN_PROGRESS"
  }
}
```

---

#### 4. Nhóm Báo Cáo & Thống Kê Dashboard

##### 4.1 Báo cáo thống kê tổng quan (`GET /api/requests/stats`)
* **Quyền yêu cầu**: `ROLE_ADMIN`
* **Headers**: `Authorization: Bearer <admin_token>`
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "totalRequests": 12,
    "completedRequests": 8,
    "completionRate": 66.67,
    "requestsByCategory": {
      "BUG": 6,
      "FEATURE": 4,
      "INQUIRY": 2
    },
    "requestsByDeveloper": [
      {
        "developerId": "349fa812-78d1-446a-9bf4-e2239d1045a1",
        "developerName": "Nguyen Van Dev1",
        "completedCount": 5,
        "inProgressCount": 1,
        "pendingCount": 1,
        "totalAssigned": 7
      },
      {
        "developerId": "459fa812-78d1-446a-9bf4-e2239d1045a2",
        "developerName": "Tran Thi Dev2",
        "completedCount": 3,
        "inProgressCount": 2,
        "pendingCount": 0,
        "totalAssigned": 5
      }
    ]
  }
}
```

---

#### 5. Nhóm Tính Năng Trợ Lý AI (LLM)

##### 5.1 AI Gợi ý phân loại danh mục (`POST /api/requests/classify`)
* **Headers**: `Authorization: Bearer <token>`
* **Request Body**:
```json
{
  "description": "Ứng dụng bị sập và hiển thị lỗi 500 khi người dùng thực hiện thanh toán qua cổng VNPAY."
}
```
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "category": "BUG",
    "confidence": 0.85,
    "reason": "Matched keyword related to bug/error: lỗi"
  }
}
```

##### 5.2 AI Đề xuất mức độ ưu tiên (`POST /api/requests/suggest-priority`)
* **Headers**: `Authorization: Bearer <token>`
* **Request Body**:
```json
{
  "description": "Sự cố khẩn cấp: Toàn bộ server database bị sập, khách hàng không thể đặt phòng!"
}
```
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "priority": "HIGH",
    "confidence": 0.9,
    "reason": "Matched urgent keyword: khẩn cấp"
  }
}
```

##### 5.3 AI Tóm tắt nội dung ticket (`GET /api/requests/{id}/summary`)
* **Headers**: `Authorization: Bearer <token>`
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "requestId": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
    "summary": "Request 'Giao diện thanh toán bị lỗi 500' summarizes: Khi người dùng ấn nút thanh toán trên trình duyệt Chrome..."
  }
}
```

---

#### 6. Nhóm Thông Báo & Cảnh Báo (Alerts)

##### 6.1 Lấy danh sách thông báo của tôi (`GET /api/alerts`)
* **Headers**: `Authorization: Bearer <token>`
* **Query Parameters**: `unreadOnly=true` (chỉ lấy thông báo chưa đọc) hoặc để trống để lấy tất cả.
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Success",
  "data": [
    {
      "id": "e8a93100-34fa-4112-921c-8ad3a5f2ec3e",
      "requestId": "bb33100e-11df-4cf6-9289-4ad3a5f2ec3e",
      "targetMemberId": "8a353609-b68a-4467-932f-a9cb6db34570",
      "alertType": "HIGH_PRIORITY_REGISTERED",
      "message": "High priority request created: Giao diện thanh toán bị lỗi 500 khi bấm xác nhận",
      "isRead": false,
      "createdAt": "2026-09-10T15:20:00Z"
    }
  ]
}
```

##### 6.2 Đánh dấu thông báo đã đọc (`PATCH /api/alerts/{id}/read`)
* **Headers**: `Authorization: Bearer <token>`
* **Response Body (200 OK)**:
```json
{
  "status": 200,
  "message": "Alert marked as read successfully",
  "data": {
    "id": "e8a93100-34fa-4112-921c-8ad3a5f2ec3e",
    "isRead": true
  }
}
```

---

## 7. Postman Test Collection & Newman

Bộ test tự động hóa đầy đủ 23 kịch bản được lưu sẵn trong thư mục `postman/`:
* `postman/Smart_Helpdesk_CRM.postman_collection.json`: Toàn bộ 23 API có script tự động trích xuất Bearer Token, Request ID, Alert ID và kiểm tra `pm.test` assertions.
* `postman/Smart_Helpdesk_CRM.postman_environment.json`: Cấu hình sẵn `baseUrl = http://localhost:8080`.
* `postman/README_POSTMAN.md`: Hướng dẫn chi tiết cách chạy bằng Postman Runner hoặc Newman CLI.
