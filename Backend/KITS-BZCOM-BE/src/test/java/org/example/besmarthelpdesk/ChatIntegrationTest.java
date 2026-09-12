package org.example.besmarthelpdesk;

import tools.jackson.databind.ObjectMapper;
import org.example.besmarthelpdesk.entity.Company;
import org.example.besmarthelpdesk.entity.Member;
import org.example.besmarthelpdesk.entity.Request;
import org.example.besmarthelpdesk.enums.RequestCategory;
import org.example.besmarthelpdesk.enums.RequestPriority;
import org.example.besmarthelpdesk.enums.RequestStatus;
import org.example.besmarthelpdesk.enums.Role;
import org.example.besmarthelpdesk.repository.ChatMessageRepository;
import org.example.besmarthelpdesk.repository.CompanyRepository;
import org.example.besmarthelpdesk.repository.MemberRepository;
import org.example.besmarthelpdesk.repository.RequestRepository;
import org.example.besmarthelpdesk.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Member client;
    private Member developer;
    private Member admin;
    private Request request;

    @BeforeEach
    void setup() {
        chatMessageRepository.deleteAll();
        requestRepository.deleteAll();
        memberRepository.deleteAll();

        Company company = companyRepository.findById("CHAT_TEST_COMPANY").orElseGet(() ->
                companyRepository.save(Company.builder()
                        .id("CHAT_TEST_COMPANY")
                        .companyName("Chat Test Company")
                        .build()));

        client = saveMember("chat-client@example.com", "Chat Client", Role.CLIENT, company.getId());
        developer = saveMember("chat-developer@example.com", "Chat Developer", Role.DEVELOPER, company.getId());
        admin = saveMember("chat-admin@example.com", "Chat Admin", Role.ADMIN, null);

        request = requestRepository.save(Request.builder()
                .title("Chat test request")
                .description("Chat test")
                .category(RequestCategory.BUG)
                .priority(RequestPriority.MEDIUM)
                .status(RequestStatus.IN_PROGRESS)
                .clientId(client.getId())
                .assignedDeveloperId(developer.getId())
                .companyId(company.getId())
                .build());
    }

    @Test
    void allThreeRolesCanSendAndReadRequestChat() throws Exception {
        sendMessage(client, "Hello from client");
        sendMessage(developer, "Hello from developer");
        sendMessage(admin, "Hello from admin");

        mockMvc.perform(get("/api/chat/requests/{requestId}/messages", request.getId())
                        .with(user(UserPrincipal.create(client))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].message", is("Hello from client")))
                .andExpect(jsonPath("$.data[1].message", is("Hello from developer")))
                .andExpect(jsonPath("$.data[2].message", is("Hello from admin")))
                .andExpect(jsonPath("$.data[0].senderId", notNullValue()))
                .andExpect(jsonPath("$.data[0].persisted", is(false)));

        org.junit.jupiter.api.Assertions.assertEquals(0, chatMessageRepository.count());
    }

    @Test
    void unrelatedUserCannotReadRequestChat() throws Exception {
        Member unrelated = saveMember("unrelated@example.com", "Unrelated", Role.CLIENT, "CHAT_TEST_COMPANY");

        mockMvc.perform(get("/api/chat/requests/{requestId}/messages", request.getId())
                        .with(user(UserPrincipal.create(unrelated))))
                .andExpect(status().isBadRequest());
    }

    private void sendMessage(Member sender, String message) throws Exception {
        String body = objectMapper.writeValueAsString(new SendMessagePayload(message));
        mockMvc.perform(post("/api/chat/requests/{requestId}/messages", request.getId())
                        .with(user(UserPrincipal.create(sender)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message", is(message)))
                .andExpect(jsonPath("$.data.persisted", is(false)));
    }

    private Member saveMember(String email, String name, Role role, String companyId) {
        return memberRepository.save(Member.builder()
                .email(email)
                .password("password")
                .name(name)
                .role(role)
                .companyId(companyId)
                .status("active")
                .isDeleted(false)
                .build());
    }

    private record SendMessagePayload(String message) {
    }
}
