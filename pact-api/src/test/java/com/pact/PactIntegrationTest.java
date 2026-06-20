package com.pact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PactIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "integration-test-secret-key-at-least-32-bytes-long");
        registry.add("jwt.expiration-hours", () -> "24");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    // ---------- helpers ----------

    private record Registered(String token, UUID memberId, String email) {
    }

    private Registered registerMember(String displayName) throws Exception {
        String email = displayName.toLowerCase().replace(" ", ".") + "."
                + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        Map<String, Object> body = Map.of(
                "email", email,
                "password", "password123",
                "displayName", displayName
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/register", body, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode json = objectMapper.readTree(response.getBody());
        String token = json.get("token").asText();
        UUID memberId = UUID.fromString(json.get("member").get("id").asText());

        return new Registered(token, memberId, email);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<String> post(String path, String token, Object body) {
        HttpHeaders headers = authHeaders(token);
        return restTemplate.exchange(
                baseUrl + path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = authHeaders(token);
        return restTemplate.exchange(
                baseUrl + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    // ---------- auth ----------

    @Test
    void registerThenLogin_succeeds() throws Exception {
        Registered registered = registerMember("Login Test User");

        Map<String, Object> loginBody = Map.of(
                "email", registered.email(),
                "password", "password123"
        );

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", loginBody, String.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(loginResponse.getBody());
        assertThat(json.get("token").asText()).isNotBlank();
        assertThat(json.get("member").get("email").asText()).isEqualTo(registered.email());
    }

    @Test
    void registerTwiceWithSameEmail_secondAttemptReturns409() throws Exception {
        String email = "dup." + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        Map<String, Object> body = Map.of(
                "email", email,
                "password", "password123",
                "displayName", "Dup User"
        );

        ResponseEntity<String> first = restTemplate.postForEntity(
                baseUrl + "/api/auth/register", body, String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = restTemplate.postForEntity(
                baseUrl + "/api/auth/register", body, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        JsonNode json = objectMapper.readTree(second.getBody());
        assertThat(json.get("error").get("code").asText()).isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void loginWithWrongPassword_returns401() throws Exception {
        Registered registered = registerMember("Wrong Password User");

        Map<String, Object> loginBody = Map.of(
                "email", registered.email(),
                "password", "totally-wrong-password"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", loginBody, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("error").get("code").asText()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void protectedEndpointWithNoToken_returns401WithSpecErrorShape() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/squads", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("error").get("code").asText()).isEqualTo("UNAUTHENTICATED");
        assertThat(json.get("error").get("message").asText()).isNotBlank();
    }

    // ---------- squads ----------

    @Test
    void createSquad_secondMemberJoinsByInviteCode_bothAppearInSquadDetail() throws Exception {
        Registered creator = registerMember("Squad Creator");
        Registered joiner = registerMember("Squad Joiner");

        Map<String, Object> createBody = Map.of("name", "Test Squad");
        ResponseEntity<String> createResponse = post("/api/squads", creator.token(), createBody);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode createJson = objectMapper.readTree(createResponse.getBody());
        UUID squadId = UUID.fromString(createJson.get("id").asText());
        String inviteCode = createJson.get("inviteCode").asText();

        Map<String, Object> joinBody = Map.of("inviteCode", inviteCode);
        ResponseEntity<String> joinResponse = post("/api/squads/join", joiner.token(), joinBody);
        assertThat(joinResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> detailResponse = get("/api/squads/" + squadId, creator.token());
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode detailJson = objectMapper.readTree(detailResponse.getBody());
        List<String> memberIds = new java.util.ArrayList<>();
        detailJson.get("members").forEach(m -> memberIds.add(m.get("id").asText()));

        assertThat(memberIds)
                .contains(creator.memberId().toString(), joiner.memberId().toString());
    }

    @Test
    void nonMember_gettingSquadDetail_returns403() throws Exception {
        Registered creator = registerMember("Squad Owner");
        Registered outsider = registerMember("Squad Outsider");

        Map<String, Object> createBody = Map.of("name", "Private Squad");
        ResponseEntity<String> createResponse = post("/api/squads", creator.token(), createBody);
        JsonNode createJson = objectMapper.readTree(createResponse.getBody());
        UUID squadId = UUID.fromString(createJson.get("id").asText());

        ResponseEntity<String> response = get("/api/squads/" + squadId, outsider.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("error").get("code").asText()).isEqualTo("NOT_SQUAD_MEMBER");
    }

    // ---------- goals & check-ins ----------

    private UUID createSquadAndReturnId(String token) throws Exception {
        Map<String, Object> body = Map.of("name", "Goals Test Squad " + UUID.randomUUID());
        ResponseEntity<String> response = post("/api/squads", token, body);
        return UUID.fromString(objectMapper.readTree(response.getBody()).get("id").asText());
    }

    private String getInviteCode(UUID squadId, String creatorToken) throws Exception {
        ResponseEntity<String> response = get("/api/squads/" + squadId, creatorToken);
        return objectMapper.readTree(response.getBody()).get("inviteCode").asText();
    }

    @Test
    void sharedGoal_twoMembersCheckInSameDay_eachHasIndependentStreak() throws Exception {
        Registered memberA = registerMember("Shared Goal A");
        Registered memberB = registerMember("Shared Goal B");

        UUID squadId = createSquadAndReturnId(memberA.token());
        String inviteCode = getInviteCode(squadId, memberA.token());
        post("/api/squads/join", memberB.token(), Map.of("inviteCode", inviteCode));

        Map<String, Object> goalBody = Map.of("title", "Drink Water", "shared", true);
        ResponseEntity<String> goalResponse = post(
                "/api/squads/" + squadId + "/goals", memberA.token(), goalBody);
        assertThat(goalResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID goalId = UUID.fromString(objectMapper.readTree(goalResponse.getBody()).get("id").asText());

        String today = LocalDate.now().toString();

        ResponseEntity<String> checkInA = post(
                "/api/goals/" + goalId + "/check-ins", memberA.token(), Map.of("date", today));
        ResponseEntity<String> checkInB = post(
                "/api/goals/" + goalId + "/check-ins", memberB.token(), Map.of("date", today));

        assertThat(checkInA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(checkInB.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode checkInAJson = objectMapper.readTree(checkInA.getBody());
        JsonNode checkInBJson = objectMapper.readTree(checkInB.getBody());

        // Each member's check-in is independent — both show streak 1, separate ids.
        assertThat(checkInAJson.get("currentStreak").asInt()).isEqualTo(1);
        assertThat(checkInBJson.get("currentStreak").asInt()).isEqualTo(1);
        assertThat(checkInAJson.get("id").asText()).isNotEqualTo(checkInBJson.get("id").asText());
    }

    @Test
    void personalGoal_nonOwnerCheckIn_returns403NotGoalOwner() throws Exception {
        Registered owner = registerMember("Personal Goal Owner");
        Registered other = registerMember("Personal Goal Intruder");

        UUID squadId = createSquadAndReturnId(owner.token());
        String inviteCode = getInviteCode(squadId, owner.token());
        post("/api/squads/join", other.token(), Map.of("inviteCode", inviteCode));

        Map<String, Object> goalBody = Map.of("title", "Run 5k", "shared", false);
        ResponseEntity<String> goalResponse = post(
                "/api/squads/" + squadId + "/goals", owner.token(), goalBody);
        UUID goalId = UUID.fromString(objectMapper.readTree(goalResponse.getBody()).get("id").asText());

        String today = LocalDate.now().toString();
        ResponseEntity<String> response = post(
                "/api/goals/" + goalId + "/check-ins", other.token(), Map.of("date", today));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("error").get("code").asText()).isEqualTo("NOT_GOAL_OWNER");
    }

    @Test
    void checkInTwiceSameDate_firstIs201_secondIs200WithSameIdAndUnchangedStreak() throws Exception {
        Registered member = registerMember("Idempotent Checker");
        UUID squadId = createSquadAndReturnId(member.token());

        Map<String, Object> goalBody = Map.of("title", "Read", "shared", false);
        ResponseEntity<String> goalResponse = post(
                "/api/squads/" + squadId + "/goals", member.token(), goalBody);
        UUID goalId = UUID.fromString(objectMapper.readTree(goalResponse.getBody()).get("id").asText());

        String today = LocalDate.now().toString();

        ResponseEntity<String> first = post(
                "/api/goals/" + goalId + "/check-ins", member.token(), Map.of("date", today));
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode firstJson = objectMapper.readTree(first.getBody());

        ResponseEntity<String> second = post(
                "/api/goals/" + goalId + "/check-ins", member.token(), Map.of("date", today));
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode secondJson = objectMapper.readTree(second.getBody());

        assertThat(secondJson.get("id").asText()).isEqualTo(firstJson.get("id").asText());
        assertThat(secondJson.get("currentStreak").asInt())
                .isEqualTo(firstJson.get("currentStreak").asInt());
    }

    @Test
    void threeConsecutiveDayCheckIns_listGoalsReportsCurrentStreakThree() throws Exception {
        Registered member = registerMember("Streak Builder");
        UUID squadId = createSquadAndReturnId(member.token());

        Map<String, Object> goalBody = Map.of("title", "Meditate", "shared", false);
        ResponseEntity<String> goalResponse = post(
                "/api/squads/" + squadId + "/goals", member.token(), goalBody);
        UUID goalId = UUID.fromString(objectMapper.readTree(goalResponse.getBody()).get("id").asText());

        LocalDate today = LocalDate.now();
        // Check in on day-before-yesterday, yesterday, and today — 3 consecutive days
        // ending today, which the grace-period rule treats as a live streak of 3.
        post("/api/goals/" + goalId + "/check-ins", member.token(),
                Map.of("date", today.minusDays(2).toString()));
        post("/api/goals/" + goalId + "/check-ins", member.token(),
                Map.of("date", today.minusDays(1).toString()));
        post("/api/goals/" + goalId + "/check-ins", member.token(),
                Map.of("date", today.toString()));

        ResponseEntity<String> listResponse = get("/api/squads/" + squadId + "/goals", member.token());
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode goalsJson = objectMapper.readTree(listResponse.getBody());
        JsonNode targetGoal = null;
        for (JsonNode node : goalsJson) {
            if (node.get("id").asText().equals(goalId.toString())) {
                targetGoal = node;
                break;
            }
        }

        assertThat(targetGoal).isNotNull();
        assertThat(targetGoal.get("currentStreak").asInt()).isEqualTo(3);
    }

    // ---------- leaderboard ----------

    @Test
    void leaderboard_twoMembersDifferentStreaks_sortedDescendingByTotalCurrentStreak() throws Exception {
        Registered memberA = registerMember("Leaderboard Low");
        Registered memberB = registerMember("Leaderboard High");

        UUID squadId = createSquadAndReturnId(memberA.token());
        String inviteCode = getInviteCode(squadId, memberA.token());
        post("/api/squads/join", memberB.token(), Map.of("inviteCode", inviteCode));

        Map<String, Object> goalBody = Map.of("title", "Leaderboard Goal", "shared", true);
        ResponseEntity<String> goalResponse = post(
                "/api/squads/" + squadId + "/goals", memberA.token(), goalBody);
        UUID goalId = UUID.fromString(objectMapper.readTree(goalResponse.getBody()).get("id").asText());

        LocalDate today = LocalDate.now();

        // memberA: only today → streak 1
        post("/api/goals/" + goalId + "/check-ins", memberA.token(),
                Map.of("date", today.toString()));

        // memberB: 3 consecutive days ending today → streak 3
        post("/api/goals/" + goalId + "/check-ins", memberB.token(),
                Map.of("date", today.minusDays(2).toString()));
        post("/api/goals/" + goalId + "/check-ins", memberB.token(),
                Map.of("date", today.minusDays(1).toString()));
        post("/api/goals/" + goalId + "/check-ins", memberB.token(),
                Map.of("date", today.toString()));

        ResponseEntity<String> leaderboardResponse = get(
                "/api/squads/" + squadId + "/leaderboard", memberA.token());
        assertThat(leaderboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode leaderboardJson = objectMapper.readTree(leaderboardResponse.getBody());
        assertThat(leaderboardJson.size()).isEqualTo(2);

        // First entry should be memberB (higher streak), second memberA.
        assertThat(leaderboardJson.get(0).get("memberId").asText())
                .isEqualTo(memberB.memberId().toString());
        assertThat(leaderboardJson.get(0).get("totalCurrentStreak").asInt()).isEqualTo(3);

        assertThat(leaderboardJson.get(1).get("memberId").asText())
                .isEqualTo(memberA.memberId().toString());
        assertThat(leaderboardJson.get(1).get("totalCurrentStreak").asInt()).isEqualTo(1);
    }
    @Test
    void outsiderNotInSquad_checkingInOnPersonalGoal_returns403NotSquadMember() throws Exception {
        Registered owner = registerMember("Outsider Test Owner");
        Registered outsider = registerMember("Outsider Test Stranger");

        // outsider never joins this squad at all.
        UUID squadId = createSquadAndReturnId(owner.token());

        Map<String, Object> goalBody = Map.of("title", "Private Journaling", "shared", false);
        ResponseEntity<String> goalResponse = post(
                "/api/squads/" + squadId + "/goals", owner.token(), goalBody);
        UUID goalId = UUID.fromString(objectMapper.readTree(goalResponse.getBody()).get("id").asText());

        String today = LocalDate.now().toString();
        ResponseEntity<String> response = post(
                "/api/goals/" + goalId + "/check-ins", outsider.token(), Map.of("date", today));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("error").get("code").asText()).isEqualTo("NOT_SQUAD_MEMBER");
    }
}