package com.enterprise.knowledge.controller;

import com.enterprise.knowledge.entity.AppUser;
import com.enterprise.knowledge.repository.AppUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionAuthenticationIntegrationTest {

    private static final Account[] SEEDED_ACCOUNTS = {
            new Account("admin", "admin123", "ADMIN",
                    "$2a$10$gaQRR.3Bbg.bs0REIyUzueDZkqZlfmUVYM0tZjnAw0modUgVO9kvq"),
            new Account("alice", "alice123", "EMPLOYEE",
                    "$2a$10$cY5psd0Op5xB9Yk1z3xUaOmkHIRPEQTd1xTlyRT5C9oYGkoIsE3AK"),
            new Account("hrmanager", "hrm123", "HR",
                    "$2a$10$sBLKjIWUmuR44RicG9xvJu5beR0r2E9.8DByv.we7pplWyL9FYMvW")
    };

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void ensureSeededUsersExist() {
        for (Account account : SEEDED_ACCOUNTS) {
            userRepository.findByUsername(account.username()).orElseGet(() -> userRepository.save(AppUser.builder()
                    .username(account.username())
                    .passwordHash(account.passwordHash())
                    .fullName(account.username())
                    .email(account.username() + "@test.local")
                    .role(AppUser.Role.valueOf(account.role()))
                    .enabled(true)
                    .build()));
        }
    }

    @Test
    void seededPasswordHashesAuthenticateEverySeededRoleAndPersistAcrossRequests() throws Exception {
        List<String> mismatchedSeedHashes = Arrays.stream(SEEDED_ACCOUNTS)
                .filter(account -> !passwordEncoder.matches(account.password(), account.passwordHash()))
                .map(Account::username)
                .toList();
        assertTrue(mismatchedSeedHashes.isEmpty(),
                "V2 seed hashes do not match documented passwords for: " + mismatchedSeedHashes);

        for (Account account : SEEDED_ACCOUNTS) {
            LoginSession session = login(account);

            MvcResult currentUser = mockMvc.perform(get("/api/auth/me").session(session.httpSession()))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode user = objectMapper.readTree(currentUser.getResponse().getContentAsString());
            assertEquals(account.username(), user.path("username").asText());
            assertEquals(account.role(), user.path("role").asText());
        }
    }

    @Test
    void incorrectPasswordReturnsJson401WithoutBasicChallenge() throws Exception {
        Account account = SEEDED_ACCOUNTS[1];
        LoginSession session = prepareCsrf();
        MvcResult response = mockMvc.perform(post("/api/auth/login")
                        .session(session.httpSession())
                        .header("X-CSRF-TOKEN", session.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Credentials(account.username(), "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        assertFalse(response.getResponse().containsHeader("WWW-Authenticate"));
        assertEquals("Invalid username or password.",
                objectMapper.readTree(response.getResponse().getContentAsString()).path("message").asText());
    }

    @Test
    void protectedApiReturnsJson401WithoutBasicChallenge() throws Exception {
        MvcResult response = mockMvc.perform(get("/api/knowledge/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        assertFalse(response.getResponse().containsHeader("WWW-Authenticate"));
        assertEquals(401, objectMapper.readTree(response.getResponse().getContentAsString()).path("status").asInt());
    }

    @Test
    void csrfIsRequiredForLoginAndProtectedMutations() throws Exception {
        MvcResult missingLoginCsrf = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Credentials("alice", "alice123"))))
                .andExpect(status().isForbidden())
                .andReturn();
        assertFalse(missingLoginCsrf.getResponse().containsHeader("WWW-Authenticate"));

        LoginSession session = login(SEEDED_ACCOUNTS[1]);
        LoginSession freshCsrf = csrfFor(session.httpSession());
        MvcResult forbidden = mockMvc.perform(post("/api/knowledge/documents/1/archive")
                        .session(session.httpSession())
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isForbidden())
                        .andReturn();
        assertFalse(forbidden.getResponse().containsHeader("WWW-Authenticate"));
        assertTrue(freshCsrf.csrfToken().length() > 0);
    }

    @Test
    void logoutInvalidatesSessionAndExpiredSessionCannotAccessProtectedApi() throws Exception {
        LoginSession session = login(SEEDED_ACCOUNTS[0]);
        LoginSession freshCsrf = csrfFor(session.httpSession());
        MvcResult logout = mockMvc.perform(post("/api/auth/logout")
                        .session(session.httpSession())
                        .header("X-CSRF-TOKEN", freshCsrf.csrfToken()))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(session.httpSession().isInvalid());
        assertFalse(logout.getResponse().containsHeader("WWW-Authenticate"));
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        LoginSession expired = login(SEEDED_ACCOUNTS[1]);
        String expiredSessionId = expired.httpSession().getId();
        expired.httpSession().invalidate();
        mockMvc.perform(get("/api/knowledge/documents")
                        .cookie(new Cookie("JSESSIONID", expiredSessionId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analyticsPreservesAdminOnlyRoleAuthorization() throws Exception {
        LoginSession admin = login(SEEDED_ACCOUNTS[0]);
        mockMvc.perform(get("/api/knowledge/analytics").session(admin.httpSession()))
                .andExpect(status().isOk());

        for (Account account : new Account[] { SEEDED_ACCOUNTS[1], SEEDED_ACCOUNTS[2] }) {
            LoginSession nonAdmin = login(account);
            MvcResult forbidden = mockMvc.perform(get("/api/knowledge/analytics")
                            .session(nonAdmin.httpSession()))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn();
            assertFalse(forbidden.getResponse().containsHeader("WWW-Authenticate"));
        }
    }

    @Test
    void staticResourcesAndMissingIcoLoadWithoutUnhandled404() throws Exception {
        mockMvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/app.js")).andExpect(status().isOk());
        mockMvc.perform(get("/favicon.svg")).andExpect(status().isOk());
        mockMvc.perform(get("/favicon.ico")).andExpect(status().isNoContent());
    }

    private LoginSession login(Account account) throws Exception {
        LoginSession csrf = prepareCsrf();
        MvcResult loginResponse = mockMvc.perform(post("/api/auth/login")
                        .session(csrf.httpSession())
                        .header("X-CSRF-TOKEN", csrf.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new Credentials(account.username(), account.password()))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        JsonNode body = objectMapper.readTree(loginResponse.getResponse().getContentAsString());
        assertEquals(account.role(), body.path("role").asText());
        return new LoginSession((MockHttpSession) loginResponse.getRequest().getSession(false), null);
    }

    private LoginSession prepareCsrf() throws Exception {
        MvcResult response = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(response.getResponse().getContentAsString());
        return new LoginSession((MockHttpSession) response.getRequest().getSession(false),
                body.path("token").asText());
    }

    private LoginSession csrfFor(MockHttpSession session) throws Exception {
        MvcResult response = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(response.getResponse().getContentAsString())
                .path("token").asText();
        return new LoginSession(session, token);
    }

    private record Account(String username, String password, String role, String passwordHash) {
    }

    private record Credentials(String username, String password) {
    }

    private record LoginSession(MockHttpSession httpSession, String csrfToken) {
    }
}
