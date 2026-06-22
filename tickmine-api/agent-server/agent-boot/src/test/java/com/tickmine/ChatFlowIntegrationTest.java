package com.tickmine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickmine.domain.model.ChatIntent;
import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalAnalysis;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.IntentClassification;
import com.tickmine.domain.model.MilestoneDsl;
import com.tickmine.domain.model.PlanDsl;
import com.tickmine.domain.model.TaskDsl;
import com.tickmine.domain.port.GoalAnalysisService;
import com.tickmine.domain.port.IntentClassifier;
import com.tickmine.domain.port.Planner;
import com.tickmine.domain.port.TaskQueryService;
import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickTaskRequest;
import com.tickmine.llm.AgentChatService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ChatFlowIntegrationTest {

    private static final String EMAIL = "test@tickmine.local";
    private static final String PASSWORD = "password123";
    private static final String TOKEN = "test-ticktick-token";

    private String userId;
    private String accessToken;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoalAnalysisService goalAnalysisService;

    @MockitoBean
    private IntentClassifier intentClassifier;

    @MockitoBean
    private TaskQueryService taskQueryService;

    @MockitoBean
    private AgentChatService agentChatService;

    @MockitoBean
    private Planner planner;

    @MockitoBean
    private TickTickClient tickTickClient;

    @BeforeEach
    void registerAndLogin() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode auth = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        userId = auth.get("userId").asText();
        accessToken = auth.get("accessToken").asText();
    }

    @Test
    void chatFlow_fromTokenBindThroughExecution() throws Exception {
        mockMvc.perform(put("/api/users/me/ticktick-token")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONNECTED"));

        when(intentClassifier.classify(eq(userId), anyString(), any(), anyList()))
                .thenReturn(new IntentClassification(ChatIntent.PLAN));

        when(goalAnalysisService.analyze(eq(userId), any(Goal.class), anyList()))
                .thenReturn(new GoalAnalysis(
                        false,
                        List.of("budget"),
                        Map.of("city", "上海"),
                        "上海婚礼策划"));

        doAnswer(invocation -> {
                    java.util.function.Consumer<String> consumer = invocation.getArgument(3);
                    consumer.accept("请告诉我您的预算范围？");
                    return null;
                })
                .when(agentChatService)
                .streamChat(eq(userId), anyString(), anyString(), any());

        MvcResult collectingResult = mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","message":"我想在上海策划一场婚礼"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("COLLECTING"))
                .andExpect(jsonPath("$.reply").value("请告诉我您的预算范围？"))
                .andExpect(jsonPath("$.missingFields[0]").value("budget"))
                .andReturn();

        UUID goalId = readGoalId(collectingResult);

        PlanDsl plan = new PlanDsl(
                "上海婚礼计划",
                List.of(new MilestoneDsl(
                        "场地筹备",
                        List.of(new TaskDsl("预订场地", "联系并预订婚礼场地", "high", null, null, List.of())))));

        when(goalAnalysisService.analyze(eq(userId), any(Goal.class), anyList()))
                .thenReturn(new GoalAnalysis(
                        true,
                        List.of(),
                        Map.of("budget", 100000, "city", "上海"),
                        "上海婚礼策划"));

        when(planner.generatePlan(any(Goal.class), any(GoalContext.class))).thenReturn(plan);

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","message":"预算10万","goalId":"%s"}
                                """.formatted(userId, goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("PLAN_READY"))
                .andExpect(jsonPath("$.plan.projectName").value("上海婚礼计划"))
                .andExpect(jsonPath("$.plan.milestones[0].name").value("场地筹备"));

        when(tickTickClient.createProject("上海婚礼计划", TOKEN)).thenReturn("proj-1");
        when(tickTickClient.createTask(any(TickTickTaskRequest.class), eq(TOKEN)))
                .thenReturn("milestone-1", "task-1");

        mockMvc.perform(post("/api/goals/" + goalId + "/execute")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.projectId").value("proj-1"))
                .andExpect(jsonPath("$.taskIds[0]").value("milestone-1"))
                .andExpect(jsonPath("$.taskIds[1]").value("task-1"));
    }

    @Test
    void chatFlow_queryTodayTasks_returnsChatPhase() throws Exception {
        when(intentClassifier.classify(eq(userId), anyString(), any(), anyList()))
                .thenReturn(new IntentClassification(ChatIntent.QUERY));
        when(taskQueryService.answerQuery(userId, "我今天有哪些任务"))
                .thenReturn("你今天有 1 项待办：\n1. 写报告（收集箱）");

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","message":"我今天有哪些任务"}
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("CHAT"))
                .andExpect(jsonPath("$.reply").value("你今天有 1 项待办：\n1. 写报告（收集箱）"));
    }

    private UUID readGoalId(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID goalId = UUID.fromString(body.get("goalId").asText());
        assertThat(goalId).isNotNull();
        return goalId;
    }
}
