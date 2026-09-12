package org.example.besmarthelpdesk;

import tools.jackson.databind.ObjectMapper;
import org.example.besmarthelpdesk.dto.request.ClassifyRequestDto;
import org.example.besmarthelpdesk.dto.request.SuggestPriorityDto;
import org.example.besmarthelpdesk.entity.Alert;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.AlertType;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.repository.AlertRepository;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.repository.RequestRepository;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AlertAndLlmIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member user1;
    private Member user2;
    private Request sampleRequest;

    @BeforeEach
    public void setup() {
        alertRepository.deleteAll();
        requestRepository.deleteAll();
        memberRepository.deleteAll();

        user1 = memberRepository.save(Member.builder()
                .email("user1@example.com")
                .password("pwd")
                .name("User One")
                .role(Role.CLIENT)
                .status("active")
                .isDeleted(false)
                .build());

        user2 = memberRepository.save(Member.builder()
                .email("user2@example.com")
                .password("pwd")
                .name("User Two")
                .role(Role.DEVELOPER)
                .status("active")
                .isDeleted(false)
                .build());

        sampleRequest = requestRepository.save(Request.builder()
                .title("Sample Bug Task")
                .description("Cannot click submit button on checkout page")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.HIGH)
                .status(RequestStatus.PENDING)
                .clientId(user1.getId())
                .companyId("KR_CLIENT_Ss")
                .build());
    }

    @Test
    public void testGetAlerts_AndMarkAsRead_Success() throws Exception {
        Alert alert1 = alertRepository.save(Alert.builder()
                .requestId(sampleRequest.getId())
                .targetMemberId(user1.getId())
                .alertType(AlertType.STATUS_CHANGED)
                .message("Status changed for your request")
                .isRead(false)
                .createdAt(Instant.now())
                .build());

        Alert alert2 = alertRepository.save(Alert.builder()
                .requestId(sampleRequest.getId())
                .targetMemberId(user2.getId())
                .alertType(AlertType.ASSIGNED)
                .message("You were assigned to a request")
                .isRead(false)
                .createdAt(Instant.now())
                .build());

        // 1. User1 fetches their alerts - should see only alert1
        mockMvc.perform(get("/api/alerts")
                        .with(user(UserPrincipal.create(user1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(alert1.getId().toString())))
                .andExpect(jsonPath("$.data[0].isRead", is(false)));

        // 2. User1 marks alert1 as read
        mockMvc.perform(patch("/api/alerts/" + alert1.getId() + "/read")
                        .with(user(UserPrincipal.create(user1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.isRead", is(true)));

        assertTrue(alertRepository.findById(alert1.getId()).get().getIsRead());

        // 3. User1 attempts to mark alert2 as read -> should be forbidden/bad request (access denied)
        mockMvc.perform(patch("/api/alerts/" + alert2.getId() + "/read")
                        .with(user(UserPrincipal.create(user1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    public void testLlm_Classify_Bug() throws Exception {
        ClassifyRequestDto dto = new ClassifyRequestDto("Hệ thống bị crash và lỗi 500 khi người dùng bấm thanh toán");

        mockMvc.perform(post("/api/requests/classify")
                        .with(user(UserPrincipal.create(user1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.category", is("BUG")))
                .andExpect(jsonPath("$.data.confidence", greaterThan(0.8)));
    }

    @Test
    public void testLlm_Classify_Feature() throws Exception {
        ClassifyRequestDto dto = new ClassifyRequestDto("Cần thêm chức năng xuất báo cáo Excel cho trang quản trị");

        mockMvc.perform(post("/api/requests/classify")
                        .with(user(UserPrincipal.create(user1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.category", is("FEATURE")))
                .andExpect(jsonPath("$.data.confidence", greaterThan(0.8)));
    }

    @Test
    public void testLlm_SuggestPriority_High() throws Exception {
        SuggestPriorityDto dto = new SuggestPriorityDto("Lỗi khẩn cấp sập server thanh toán trên production ngay lập tức!");

        mockMvc.perform(post("/api/requests/suggest-priority")
                        .with(user(UserPrincipal.create(user1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.priority", is("HIGH")))
                .andExpect(jsonPath("$.data.confidence", greaterThan(0.9)));
    }

    @Test
    public void testLlm_Summarize_Success() throws Exception {
        mockMvc.perform(get("/api/requests/" + sampleRequest.getId() + "/summary")
                        .with(user(UserPrincipal.create(user1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.requestId", is(sampleRequest.getId().toString())))
                .andExpect(jsonPath("$.data.summary", containsString("Sample Bug Task")));
    }
}
