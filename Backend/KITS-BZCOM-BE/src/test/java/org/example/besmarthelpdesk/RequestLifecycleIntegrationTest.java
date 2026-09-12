package org.example.besmarthelpdesk;

import tools.jackson.databind.ObjectMapper;
import org.example.besmarthelpdesk.dto.request.AssignDeveloperDto;
import org.example.besmarthelpdesk.dto.request.CreateRequestDto;
import org.example.besmarthelpdesk.dto.request.UpdateRequestStatusDto;
import org.example.besmarthelpdesk.entity.Company;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.HistoryAction;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.repository.*;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class RequestLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RequestHistoryRepository requestHistoryRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member client;
    private Member admin;
    private Member dev1;
    private Member dev2;
    private Company company;

    @BeforeEach
    public void setup() {
        alertRepository.deleteAll();
        requestHistoryRepository.deleteAll();
        requestRepository.deleteAll();
        memberRepository.deleteAll();

        // Ensure company exists
        company = companyRepository.findById("KR_CLIENT_Ss").orElseGet(() ->
                companyRepository.save(Company.builder()
                        .id("KR_CLIENT_Ss")
                        .companyName("Samsung C&T Corporation")
                        .address("Seoul")
                        .phone("+82-2145-1114")
                        .build())
        );

        client = memberRepository.save(Member.builder()
                .email("client@samsung.com")
                .password("pwd")
                .name("Samsung Client")
                .role(Role.CLIENT)
                .companyId(company.getId())
                .phone("01012345678")
                .status("active")
                .isDeleted(false)
                .build());

        admin = memberRepository.save(Member.builder()
                .email("admin@bizcom.com")
                .password("pwd")
                .name("Bizcom Admin")
                .role(Role.ADMIN)
                .status("active")
                .isDeleted(false)
                .build());

        dev1 = memberRepository.save(Member.builder()
                .email("dev1@bizcom.com")
                .password("pwd")
                .name("Developer One")
                .role(Role.DEVELOPER)
                .status("active")
                .isDeleted(false)
                .build());

        dev2 = memberRepository.save(Member.builder()
                .email("dev2@bizcom.com")
                .password("pwd")
                .name("Developer Two")
                .role(Role.DEVELOPER)
                .status("active")
                .isDeleted(false)
                .build());
    }

    @Test
    public void testCreateRequest_Success() throws Exception {
        CreateRequestDto dto = CreateRequestDto.builder()
                .title("Login button not working")
                .description("Clicking login does nothing on Chrome")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.MEDIUM)
                .build();

        UserPrincipal clientPrincipal = UserPrincipal.create(client);

        mockMvc.perform(post("/api/requests")
                        .with(user(clientPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.title", is("Login button not working")))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.category", is("BUG")))
                .andExpect(jsonPath("$.data.companyId", is("KR_CLIENT_Ss")))
                .andExpect(jsonPath("$.data.companyName", is("Samsung C&T Corporation")))
                .andExpect(jsonPath("$.data.clientId", is(client.getId().toString())));

        // Verify history automatically created
        assertEquals(1, requestHistoryRepository.count());
        assertEquals(HistoryAction.CREATE, requestHistoryRepository.findAll().get(0).getAction());
        assertEquals(RequestStatus.PENDING, requestHistoryRepository.findAll().get(0).getToStatus());
    }

    @ParameterizedTest
    @EnumSource(RequestPriority.class)
    public void testCreateRequest_AllPriorities_GeneratesAlertForAdmins(RequestPriority priority) throws Exception {
        Member secondAdmin = memberRepository.save(Member.builder()
                .email("second-admin@bizcom.com").password("pwd").name("Second Admin")
                .role(Role.ADMIN).isDeleted(false).build());
        memberRepository.save(Member.builder()
                .email("deleted-admin@bizcom.com").password("pwd").name("Deleted Admin")
                .role(Role.ADMIN).isDeleted(true).build());
        CreateRequestDto dto = CreateRequestDto.builder()
                .title("Production DB Down")
                .description("Critical outage in production")
                .category(RequestCategory.BUG)
                .priority(priority)
                .build();

        UserPrincipal clientPrincipal = UserPrincipal.create(client);

        mockMvc.perform(post("/api/requests")
                        .with(user(clientPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Verify admin alert was generated
        var alerts = alertRepository.findAll();
        assertEquals(2, alerts.size());
        assertEquals(java.util.Set.of(admin.getId(), secondAdmin.getId()), alerts.stream()
                .map(org.example.besmarthelpdesk.entity.Alert::getTargetMemberId)
                .collect(java.util.stream.Collectors.toSet()));
        var createdRequest = requestRepository.findAll().get(0);
        for (var alert : alerts) {
            assertEquals(createdRequest.getId(), alert.getRequestId());
            assertEquals(false, alert.getIsRead());
            assertEquals(priority == RequestPriority.HIGH ? "HIGH_PRIORITY_REGISTERED" : "REQUEST_REGISTERED",
                    alert.getAlertType().name());
        }
    }

    @Test
    public void testGetRequests_RBAC_DataIsolation() throws Exception {
        // Create request for client
        Request req1 = requestRepository.save(Request.builder()
                .title("Client 1 task")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.LOW)
                .status(RequestStatus.PENDING)
                .clientId(client.getId())
                .companyId(company.getId())
                .build());

        // Create second client and request
        Member client2 = memberRepository.save(Member.builder()
                .email("client2@naver.com")
                .password("pwd")
                .name("Naver Client")
                .role(Role.CLIENT)
                .companyId("KR_CLIENT_Nv")
                .status("active")
                .isDeleted(false)
                .build());

        Request req2 = requestRepository.save(Request.builder()
                .title("Client 2 task")
                .category(RequestCategory.FEATURE)
                .priority(RequestPriority.MEDIUM)
                .status(RequestStatus.IN_PROGRESS)
                .clientId(client2.getId())
                .assignedDeveloperId(dev1.getId())
                .companyId("KR_CLIENT_Nv")
                .build());

        // 1. Client 1 should only see req1
        mockMvc.perform(get("/api/requests")
                        .with(user(UserPrincipal.create(client))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(req1.getId().toString())));

        // 2. Dev 1 should only see assigned req2
        mockMvc.perform(get("/api/requests")
                        .with(user(UserPrincipal.create(dev1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(req2.getId().toString())));

        // 3. Admin should see both req1 and req2
        mockMvc.perform(get("/api/requests")
                        .with(user(UserPrincipal.create(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(2)));
    }

    @Test
    public void testAutoAssignment_Algorithm_LowestTasksAndTieBreaker() throws Exception {
        // Give Dev1 2 active tasks (PENDING or IN_PROGRESS)
        requestRepository.save(Request.builder()
                .title("Task 1")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.LOW)
                .status(RequestStatus.IN_PROGRESS)
                .clientId(client.getId())
                .assignedDeveloperId(dev1.getId())
                .companyId(company.getId())
                .build());

        requestRepository.save(Request.builder()
                .title("Task 2")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.LOW)
                .status(RequestStatus.PENDING)
                .clientId(client.getId())
                .assignedDeveloperId(dev1.getId())
                .companyId(company.getId())
                .build());

        // Dev2 has 0 active tasks!
        Request newRequest = requestRepository.save(Request.builder()
                .title("New Unassigned Request")
                .category(RequestCategory.FEATURE)
                .priority(RequestPriority.HIGH)
                .status(RequestStatus.PENDING)
                .clientId(client.getId())
                .companyId(company.getId())
                .build());

        // Admin triggers auto-assignment (no developerId in body)
        mockMvc.perform(patch("/api/requests/" + newRequest.getId() + "/assign")
                        .with(user(UserPrincipal.create(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.assignedDeveloperId", is(dev2.getId().toString())))
                .andExpect(jsonPath("$.data.assignedDeveloperName", is("Developer Two")));

        // Verify alert sent to dev2
        assertEquals(1, alertRepository.findByTargetMemberIdOrderByCreatedAtDesc(dev2.getId()).size());
    }

    @Test
    public void testAutoAssignment_TieBreaker_MostRecentlyCompleted() throws Exception {
        // Both dev1 and dev2 have 0 active tasks.
        // But Dev1 completed a task 1 hour ago, while Dev2 completed a task 5 days ago!
        Request doneDev1 = Request.builder()
                .title("Done task Dev1")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.LOW)
                .status(RequestStatus.DONE)
                .clientId(client.getId())
                .assignedDeveloperId(dev1.getId())
                .companyId(company.getId())
                .build();
        doneDev1 = requestRepository.save(doneDev1);
        requestRepository.updateUpdatedAt(doneDev1.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

        Request doneDev2 = Request.builder()
                .title("Done task Dev2")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.LOW)
                .status(RequestStatus.DONE)
                .clientId(client.getId())
                .assignedDeveloperId(dev2.getId())
                .companyId(company.getId())
                .build();
        doneDev2 = requestRepository.save(doneDev2);
        requestRepository.updateUpdatedAt(doneDev2.getId(), Instant.now().minus(5, ChronoUnit.DAYS));

        Request newRequest = requestRepository.save(Request.builder()
                .title("Tie-breaker Request")
                .category(RequestCategory.FEATURE)
                .priority(RequestPriority.MEDIUM)
                .status(RequestStatus.PENDING)
                .clientId(client.getId())
                .companyId(company.getId())
                .build());

        // Auto assignment should choose dev1 because dev1 completed a task more recently!
        mockMvc.perform(patch("/api/requests/" + newRequest.getId() + "/assign")
                        .with(user(UserPrincipal.create(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedDeveloperId", is(dev1.getId().toString())));
    }

    @Test
    public void testStatusTransitionRules_ValidAndInvalid() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .title("Status flow task")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.MEDIUM)
                .status(RequestStatus.PENDING)
                .clientId(client.getId())
                .assignedDeveloperId(dev1.getId())
                .companyId(company.getId())
                .build());

        UserPrincipal devPrincipal = UserPrincipal.create(dev1);

        // 1. Invalid: PENDING -> DONE (skip IN_PROGRESS)
        UpdateRequestStatusDto skipDto = new UpdateRequestStatusDto(RequestStatus.DONE, "Direct done");
        mockMvc.perform(patch("/api/requests/" + request.getId() + "/status")
                        .with(user(devPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skipDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));

        // 2. Valid: PENDING -> IN_PROGRESS
        UpdateRequestStatusDto inProgressDto = new UpdateRequestStatusDto(RequestStatus.IN_PROGRESS, "Working on it");
        mockMvc.perform(patch("/api/requests/" + request.getId() + "/status")
                        .with(user(devPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inProgressDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));

        // 3. Valid: IN_PROGRESS -> DONE
        UpdateRequestStatusDto doneDto = new UpdateRequestStatusDto(RequestStatus.DONE, "Completed and verified");
        mockMvc.perform(patch("/api/requests/" + request.getId() + "/status")
                        .with(user(devPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doneDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DONE")));

        // 4. Invalid: DONE -> IN_PROGRESS (cannot reopen completed request)
        mockMvc.perform(patch("/api/requests/" + request.getId() + "/status")
                        .with(user(devPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inProgressDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    public void testGetRequestStats_Success() throws Exception {
        // Create 2 DONE, 1 IN_PROGRESS, 1 PENDING
        requestRepository.save(Request.builder().title("T1").category(RequestCategory.BUG).status(RequestStatus.DONE).clientId(client.getId()).assignedDeveloperId(dev1.getId()).companyId(company.getId()).build());
        requestRepository.save(Request.builder().title("T2").category(RequestCategory.BUG).status(RequestStatus.DONE).clientId(client.getId()).assignedDeveloperId(dev1.getId()).companyId(company.getId()).build());
        requestRepository.save(Request.builder().title("T3").category(RequestCategory.FEATURE).status(RequestStatus.IN_PROGRESS).clientId(client.getId()).assignedDeveloperId(dev1.getId()).companyId(company.getId()).build());
        requestRepository.save(Request.builder().title("T4").category(RequestCategory.INQUIRY).status(RequestStatus.PENDING).clientId(client.getId()).assignedDeveloperId(dev2.getId()).companyId(company.getId()).build());

        mockMvc.perform(get("/api/requests/stats")
                        .with(user(UserPrincipal.create(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.totalRequests", is(4)))
                .andExpect(jsonPath("$.data.completedRequests", is(2)))
                .andExpect(jsonPath("$.data.completionRate", is(50.0)))
                .andExpect(jsonPath("$.data.requestsByCategory.BUG", is(2)))
                .andExpect(jsonPath("$.data.requestsByCategory.FEATURE", is(1)))
                .andExpect(jsonPath("$.data.requestsByCategory.INQUIRY", is(1)));
    }

    @Test
    public void testConcurrentStatusUpdates_PessimisticLockProtection() throws Exception {
        Request request = requestRepository.save(Request.builder()
                .title("Concurrency test task")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.HIGH)
                .status(RequestStatus.PENDING)
                .clientId(client.getId())
                .assignedDeveloperId(dev1.getId())
                .companyId(company.getId())
                .build());

        int numThreads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    UpdateRequestStatusDto inProgressDto = new UpdateRequestStatusDto(RequestStatus.IN_PROGRESS, "Thread start");
                    mockMvc.perform(patch("/api/requests/" + request.getId() + "/status")
                                    .with(user(UserPrincipal.create(dev1)))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(inProgressDto)))
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successCount.incrementAndGet();
                                } else {
                                    failCount.incrementAndGet();
                                }
                            });
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        // Fire all threads simultaneously
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // Exactly ONE thread succeeds in transitioning PENDING -> IN_PROGRESS!
        // The other threads attempt IN_PROGRESS -> IN_PROGRESS (same status) which gets safely blocked!
        assertEquals(1, successCount.get());
        assertEquals(3, failCount.get());
    }
}
