import { readFileSync, readdirSync, mkdirSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

// This extractor targets the DTO conventions in this repository, not arbitrary Java.
const backend = resolve(process.argv[2] || '../../Backend/KITS-BZCOM-BE')
const java = resolve(backend, 'src/main/java/org/example/besmarthelpdesk')
const read = (path) => readFileSync(resolve(java, path), 'utf8')
const ref = (name) => ({ $ref: '#/components/schemas/' + name })
const schemas = {}
for (const name of ['Role', 'RequestCategory', 'RequestPriority', 'RequestStatus', 'HistoryAction', 'AlertType']) {
  schemas[name] = { type: 'string', enum: read('enums/' + name + '.java').split('{')[1].split('}')[0].match(/\b[A-Z][A-Z_]+\b/g) }
}
function typeSchema(type) {
  if (type.startsWith('List<')) return { type: 'array', items: typeSchema(type.slice(5, -1)) }
  if (type.startsWith('Map<')) return { type: 'object', additionalProperties: { type: 'integer', format: 'int64' } }
  return ({ String: { type: 'string' }, UUID: { type: 'string', format: 'uuid' }, Instant: { type: 'string', format: 'date-time' }, Boolean: { type: 'boolean' }, long: { type: 'integer', format: 'int64' }, Long: { type: 'integer', format: 'int64' }, double: { type: 'number', format: 'double' } })[type] || ref(type)
}
function dto(name, source, input) {
  const properties = {}, required = []
  let annotations = ''
  for (const line of source.split('\n')) {
    if (line.trim().startsWith('@')) annotations += line
    const match = line.match(/^\s*(?:private\s+)?(String|UUID|Instant|Boolean|long|double|Role|Request\w+|HistoryAction|AlertType|UserInfo|List<[^>]+>|Map<[^>]+>)\s+(\w+)\s*(?:=\s*([^;]+))?;/)
    if (!match) continue
    const [, type, field, initial] = match
    const schema = typeSchema(type)
    if (input && /@NotBlank|@NotNull/.test(annotations)) required.push(field)
    if (input && /@NotBlank/.test(annotations)) Object.assign(schema, { minLength: 1, pattern: '\\S' })
    if (/@Email/.test(annotations)) schema.format = 'email'
    const max = annotations.match(/@Size\(max\s*=\s*(\d+)\)/)
    if (max) schema.maxLength = Number(max[1])
    if (field === 'password' || field === 'refreshToken' && input) schema.writeOnly = true
    if (initial) {
      const value = initial.trim().replace(/^.*\./, '').replaceAll('"', '')
      if (schema.$ref) properties[field] = { allOf: [schema], default: value }
      else schema.default = value
    }
    properties[field] ||= schema
    annotations = ''
  }
  schemas[name] = { type: 'object', properties, ...(required.length ? { required } : {}) }
}
for (const folder of ['request', 'response']) {
  for (const file of readdirSync(resolve(java, 'dto', folder))) {
    let source = read('dto/' + folder + '/' + file)
    const nested = source.match(/public static class (\w+)\s*\{/)
    if (nested) {
      dto(nested[1], source.slice(nested.index + nested[0].length), false)
      source = source.slice(0, nested.index)
    }
    dto(file.replace('.java', ''), source, folder === 'request')
  }
}
for (const [name, fields] of Object.entries({
  CompanyResponse: ['address', 'phone'], MemberResponse: ['phone', 'companyId', 'companyName', 'createdAt', 'isDeleted'],
  UserInfo: ['phone', 'companyId', 'companyName'], RequestResponse: ['companyName', 'description', 'clientName', 'assignedDeveloperId', 'assignedDeveloperName'],
  RequestDetailResponse: ['companyName', 'companyAddress', 'companyPhone', 'description', 'clientName', 'clientEmail', 'assignedDeveloperId', 'assignedDeveloperName', 'assignedDeveloperEmail'],
  RequestHistoryResponse: ['changedByName', 'fromStatus', 'toStatus', 'memo'], ChatMessageResponse: ['senderName', 'senderRole'],
  RegisterRequest: ['companyId', 'phone'], AssignDeveloperDto: ['developerId'], UpdateRequestStatusDto: ['memo'],
})) for (const field of fields) {
  const property = schemas[name].properties[field]
  schemas[name].properties[field] = property.$ref ? { ...schemas[property.$ref.split('/').pop()], nullable: true } : { ...property, nullable: true }
}
schemas.RegisterRequest.description = 'Backend hiện chấp nhận cả ADMIN, DEVELOPER, CLIENT trên endpoint public. Frontend chỉ cho Client/Developer; Client chọn KR_SAMSUNG/KR_NAVER/KR_KAKAO, Developer gửi KR_BZCOM. Đây là quy tắc UI, chưa được backend cưỡng chế. companyId nếu cung cấp phải tồn tại. Giới hạn DB: email/name 100, phone/companyId 20; DTO chưa có @Size tương ứng.'
schemas.CreateRequestDto.description = 'companyId và clientId lấy từ thành viên đăng nhập. Client phải có công ty. status ban đầu PENDING; priority mặc định MEDIUM. title tối đa 255 theo DB (DTO chưa có @Size).'
schemas.RequestStatsResponse.properties.completionRate = { type: 'number', minimum: 0, maximum: 100, description: 'Phần trăm 0–100; frontend chia 100 để hiển thị.' }
schemas.ChatMessageResponse.properties.persisted.description = 'false: còn trong bộ nhớ; true: đã ghi DB. Flush sau 10 phút không gửi thêm tin, scheduler kiểm tra mỗi 30 giây.'
schemas.ChatMessageResponse.properties.isRead.description = 'Cờ chung của tin nhắn, không phải trạng thái đọc riêng từng thành viên.'
schemas.RequestPage = { type: 'object', properties: { content: { type: 'array', items: ref('RequestResponse') }, number: { type: 'integer', description: 'Trang bắt đầu từ 0' }, size: { type: 'integer' }, totalElements: { type: 'integer', format: 'int64' }, totalPages: { type: 'integer' }, numberOfElements: { type: 'integer' }, first: { type: 'boolean' }, last: { type: 'boolean' }, empty: { type: 'boolean' }, pageable: { type: 'object', additionalProperties: true }, sort: { type: 'object', additionalProperties: true } } }
schemas.Error = { type: 'object', properties: { status: { type: 'integer' }, errorCode: { type: 'string' }, message: { type: 'string' }, data: { type: 'object', nullable: true, additionalProperties: { type: 'string' } } } }
const paths = {}
const json = (schema) => ({ 'application/json': { schema } })
function operation(method, path, tag, summary, result, body, permission, tables, description = '', status = 200) {
  const data = result === null ? { type: 'object', nullable: true, enum: [null] } : result.endsWith('[]') ? { type: 'array', items: ref(result.slice(0, -2)) } : ref(result)
  const op = { tags: [tag], summary, operationId: method + path.replace(/[{}]/g, '').replace(/\//g, '_').replaceAll('-', '_'),
    description: `Quyền: ${permission}. Bảng liên quan: ${tables.join(', ') || 'không ghi bảng nghiệp vụ'}. ${description}`,
    'x-tables': tables, 'x-permission': permission,
    responses: { [status]: { description: 'Thành công; status trong envelope là 200.', content: json({ type: 'object', properties: { status: { type: 'integer', example: 200 }, message: { type: 'string' }, data } }) },
      '400': { description: 'Validation hoặc điều kiện nghiệp vụ không hợp lệ.', content: json(ref('Error')) },
      '401': { description: 'Xác thực thất bại; lỗi từ security filter có thể không có envelope.' },
      '403': { description: 'Không đủ quyền; lỗi từ security filter có thể không có envelope.' },
      '404': { description: 'Không tìm thấy tài nguyên hoặc request ngoài phạm vi truy cập.', content: json(ref('Error')) },
      '500': { description: 'Lỗi hệ thống.', content: json(ref('Error')) } } }
  if (permission === 'Public') op.security = []
  if (body) op.requestBody = { required: body !== 'AssignDeveloperDto', content: json(ref(body)) }
  const parameters = [...path.matchAll(/\{(\w+)\}/g)].map(([, name]) => ({ name, in: 'path', required: true, schema: name === 'companyId' ? { type: 'string' } : { type: 'string', format: 'uuid' } }))
  if (parameters.length) op.parameters = parameters
  ;(paths[path] ||= {})[method] = op
  return op
}
for (const prefix of ['/auth', '/v1/auth']) {
  operation('post', prefix + '/login', 'Auth', 'Đăng nhập', 'LoginAuthResponse', 'LoginRequest', 'Public', ['members', 'companies'])
  operation('post', prefix + '/register', 'Auth', 'Đăng ký', 'MemberResponse', 'RegisterRequest', 'Public', ['members', 'companies'], 'Ghi members, tra cứu companies.', 201)
  const refresh = operation('post', prefix + '/refresh', 'Auth', 'Xoay vòng token', 'RefreshTokenResponse', 'TokenRefreshRequest', 'Public', ['members'], 'Refresh token chỉ dùng một lần; TTL tính bằng milliseconds. Token store nằm trong bộ nhớ, không có bảng refresh_tokens.')
  refresh.parameters = [{ name: 'User-Agent', in: 'header', required: false, schema: { type: 'string' } }]
  const logout = operation('post', prefix + '/logout', 'Auth', 'Đăng xuất', null, null, 'Public', [], 'Security cho phép public; gửi Bearer token để thu hồi refresh token của người đang đăng nhập. Không có body.')
  logout.security = [{}, { bearerAuth: [] }]
}
operation('post', '/members', 'members', 'Đăng ký thành viên (URL thay thế)', 'MemberResponse', 'RegisterRequest', 'Public', ['members', 'companies'], '', 201)
operation('get', '/members', 'members', 'Danh sách thành viên', 'MemberResponse[]', null, 'ADMIN', ['members', 'companies'], 'Không hỗ trợ query role; frontend tự lọc Developer và isDeleted.')
operation('get', '/members/{id}', 'members', 'Chi tiết thành viên', 'MemberResponse', null, 'Đã đăng nhập', ['members', 'companies'])
operation('get', '/companies', 'companies', 'Danh sách công ty', 'CompanyResponse[]', null, 'Public', ['companies'], 'Sắp theo companyName, sau đó companyId. SecurityConfig cho public dù mô tả Tag cũ ghi cần đăng nhập.')
operation('get', '/companies/{companyId}', 'companies', 'Chi tiết công ty', 'CompanyResponse', null, 'Đã đăng nhập', ['companies'])
const visibility = 'ADMIN xem tất cả; CLIENT chỉ request do mình tạo; DEVELOPER chỉ request được giao. Không phân quyền Client theo toàn công ty.'
operation('post', '/requests', 'requests', 'Tạo yêu cầu', 'RequestResponse', 'CreateRequestDto', 'CLIENT', ['requests', 'members', 'companies', 'request_histories', 'alerts'], 'Ghi request và lịch sử CREATE; thông báo Admin theo RequestFacadeImpl.', 201)
const list = operation('get', '/requests', 'requests', 'Danh sách yêu cầu', 'RequestPage', null, 'Đã đăng nhập', ['requests', 'members', 'companies'], visibility)
list.parameters = Object.entries({ category: ref('RequestCategory'), priority: ref('RequestPriority'), status: ref('RequestStatus'), search: { type: 'string' }, page: { type: 'integer', default: 0, minimum: 0 }, size: { type: 'integer', default: 10, minimum: 1 }, sortBy: { type: 'string', default: 'createdAt' }, sortDir: { type: 'string', default: 'desc', enum: ['asc', 'desc'] } }).map(([name, schema]) => ({ name, in: 'query', required: false, schema }))
operation('get', '/requests/stats', 'requests', 'Thống kê KPI', 'RequestStatsResponse', null, 'ADMIN', ['requests', 'members'])
operation('get', '/requests/{id}', 'requests', 'Chi tiết kèm lịch sử', 'RequestDetailResponse', null, 'Có quyền xem request', ['requests', 'members', 'companies', 'request_histories'], visibility)
operation('patch', '/requests/{id}/assign', 'requests', 'Phân công Developer', 'RequestResponse', 'AssignDeveloperDto', 'ADMIN', ['requests', 'members', 'companies', 'request_histories', 'alerts'], 'Bỏ body, gửi {} hoặc developerId:null để tự phân công; truyền UUID để phân công thủ công. Ghi lịch sử ASSIGN và thông báo Developer.')
operation('patch', '/requests/{id}/status', 'requests', 'Chuyển trạng thái', 'RequestResponse', 'UpdateRequestStatusDto', 'ADMIN hoặc Developer được giao', ['requests', 'members', 'companies', 'request_histories', 'alerts'], 'Chỉ PENDING → IN_PROGRESS → DONE; cấm nhảy cóc, đổi cùng trạng thái, mở lại. Ghi lịch sử và thông báo Client.')
operation('get', '/requests/{id}/history', 'request_histories', 'Lịch sử yêu cầu', 'RequestHistoryResponse[]', null, 'Có quyền xem request', ['request_histories', 'requests', 'members'], visibility)
operation('post', '/requests/classify', 'AI', 'Gợi ý phân loại', 'ClassifyResponseDto', 'ClassifyRequestDto', 'Đã đăng nhập', [], 'RuleBasedLlmAdapter xử lý description, không tự tạo request.')
operation('post', '/requests/suggest-priority', 'AI', 'Gợi ý độ ưu tiên', 'SuggestPriorityResponseDto', 'SuggestPriorityDto', 'Đã đăng nhập', [], 'RuleBasedLlmAdapter; không tự cập nhật priority trong DB.')
operation('get', '/requests/{id}/summary', 'AI', 'Tóm tắt yêu cầu', 'RequestSummaryResponseDto', null, 'Có quyền xem request', ['requests'], visibility)
const alerts = operation('get', '/alerts', 'alerts', 'Thông báo của tôi', 'AlertResponse[]', null, 'Đã đăng nhập', ['alerts'])
alerts.parameters = [{ name: 'unreadOnly', in: 'query', required: false, schema: { type: 'boolean', default: false } }]
operation('patch', '/alerts/{id}/read', 'alerts', 'Đánh dấu thông báo đã đọc', 'AlertResponse', null, 'Người nhận thông báo', ['alerts'], 'Không có body.')
const chatDescription = 'ADMIN, Client tạo request hoặc Developer được giao. Không có quyền chat hiện trả 400. Tin chờ trong RAM được gộp với DB; persist sau 10 phút không gửi tin mới. isRead là cờ chung.'
operation('get', '/chat/inbox', 'chat_messages', 'Hộp thư chat', 'ChatMessageResponse[]', null, 'Đã đăng nhập', ['chat_messages', 'requests', 'members'], chatDescription + ' Sắp mới nhất trước, chưa phân trang.')
operation('get', '/chat/requests/{requestId}/messages', 'chat_messages', 'Lịch sử chat', 'ChatMessageResponse[]', null, 'Có quyền xem request', ['chat_messages', 'requests', 'members'], chatDescription + ' Sắp cũ nhất trước.')
operation('post', '/chat/requests/{requestId}/messages', 'chat_messages', 'Gửi tin nhắn', 'ChatMessageResponse', 'SendChatMessageRequest', 'Có quyền xem request', ['chat_messages', 'requests', 'members'], chatDescription + ' Trả HTTP 200, persisted:false.')
operation('patch', '/chat/messages/{messageId}/read', 'chat_messages', 'Đánh dấu tin nhắn đã đọc', 'ChatMessageResponse', null, 'Có quyền xem request', ['chat_messages', 'requests', 'members'], chatDescription + ' Không có body.')
const spec = { openapi: '3.0.3', info: { title: 'Smart Helpdesk — API và bảng dữ liệu', version: '1.0.0', description: 'Đối chiếu mã backend, DTO, SecurityConfig, Liquibase, SQL và frontend ngày 2026-09-12. 6 bảng nghiệp vụ, 25 thao tác chính + 4 alias auth. Đây là bản tĩnh theo mã nguồn; backend localhost:8080 chưa chạy khi xây dựng. UUID sinh phía server, JSON camelCase; không gửi cột audit hoặc password hash. Swagger không tạo API mới.' }, servers: [{ url: '/api', description: 'Cùng origin qua Vite/reverse proxy' }, { url: 'http://localhost:8080/api', description: 'Backend trực tiếp (cần CORS khi khác origin)' }], security: [{ bearerAuth: [] }], tags: ['Auth', 'companies', 'members', 'requests', 'request_histories', 'alerts', 'chat_messages', 'AI'].map(name => ({ name })), paths, components: { securitySchemes: { bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' } }, schemas } }
mkdirSync('public/swagger', { recursive: true })
writeFileSync('public/swagger/openapi.json', JSON.stringify(spec, null, 2) + '\n')
console.log(`Generated ${Object.values(paths).reduce((n, p) => n + Object.keys(p).length, 0)} operations and ${Object.keys(schemas).length} schemas.`)
