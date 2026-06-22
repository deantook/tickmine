# TickMine Agent 后端 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建可启动的多租户任务管理 Agent 后端，实现对话澄清 → Plan DSL 生成 → TickTick 执行写入的完整 MVP 闭环。

**Architecture:** 8 模块 Maven 工程（DDD + Agent 分层），LLM 仅产出结构化 PlanDsl，TickTickPlanExecutor 通过 HTTP Open API 按用户 token 执行。FREE/VIP 用 DeepSeek，SVIP 用 Qwen，配额按 tier 控制。

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring AI, PostgreSQL 16, Redis 7, JPA, Flyway, Lombok, Maven, Docker Compose

**Spec:** `docs/superpowers/specs/2025-06-22-tickmine-agent-design.md`

**Worktree:** 建议在独立 git worktree 中实现（`using-git-worktrees` skill）。

---

## File Structure Overview

```
tickmine-api/agent-server/
├── pom.xml                          # parent BOM
├── agent-domain/
│   └── src/main/java/com/tickmine/domain/
│       ├── model/                   # Goal, PlanDsl, enums, records
│       ├── port/                    # Planner, PlanExecutor, TickTickClient
│       └── service/                 # GoalAgentService, QuotaService, ConversationService
├── agent-infra/
│   └── src/main/java/com/tickmine/infra/
│       ├── persistence/entity/      # UserEntity, GoalEntity, ...
│       ├── persistence/repository/    # Spring Data repos
│       ├── crypto/TokenEncryptor.java
│       ├── redis/ConversationCache.java
│       └── config/TickMineProperties.java
├── agent-llm/
│   └── src/main/java/com/tickmine/llm/
│       ├── AgentChatService.java
│       ├── ModelResolver.java
│       └── config/LlmConfig.java
├── agent-mcp/
│   └── src/main/java/com/tickmine/mcp/
│       ├── TickTickClientImpl.java
│       └── dto/OpenTaskRequest.java
├── agent-planner/
│   └── src/main/java/com/tickmine/planner/
│       ├── GoalAnalyzer.java
│       └── LlmPlanner.java
├── agent-executor/
│   └── src/main/java/com/tickmine/executor/
│       └── TickTickPlanExecutor.java
├── agent-api/
│   └── src/main/java/com/tickmine/api/
│       ├── controller/              # Chat, Goal, User controllers
│       ├── dto/
│       └── exception/GlobalExceptionHandler.java
└── agent-boot/
    ├── src/main/java/com/tickmine/TickMineApplication.java
    ├── src/main/resources/
    │   ├── application.yml
    │   ├── prompts/*.st
    │   └── db/migration/V1__init_schema.sql
    ├── Dockerfile
    └── docker-compose.yml (at agent-server root)
```

---

### Task 1: Maven 多模块骨架

**Files:**
- Create: `tickmine-api/agent-server/pom.xml`
- Create: `tickmine-api/agent-server/agent-domain/pom.xml`
- Create: `tickmine-api/agent-server/agent-infra/pom.xml`
- Create: `tickmine-api/agent-server/agent-llm/pom.xml`
- Create: `tickmine-api/agent-server/agent-mcp/pom.xml`
- Create: `tickmine-api/agent-server/agent-planner/pom.xml`
- Create: `tickmine-api/agent-server/agent-executor/pom.xml`
- Create: `tickmine-api/agent-server/agent-api/pom.xml`
- Create: `tickmine-api/agent-server/agent-boot/pom.xml`

- [ ] **Step 1: 创建 parent pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.tickmine</groupId>
    <artifactId>agent-server</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>TickMine Agent Server</name>

    <modules>
        <module>agent-domain</module>
        <module>agent-infra</module>
        <module>agent-llm</module>
        <module>agent-mcp</module>
        <module>agent-planner</module>
        <module>agent-executor</module>
        <module>agent-api</module>
        <module>agent-boot</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.5.0</spring-boot.version>
        <spring-ai.version>1.0.0</spring-ai.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.ai</groupId>
                <artifactId>spring-ai-bom</artifactId>
                <version>${spring-ai.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.tickmine</groupId>
                <artifactId>agent-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tickmine</groupId>
                <artifactId>agent-infra</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tickmine</groupId>
                <artifactId>agent-llm</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tickmine</groupId>
                <artifactId>agent-mcp</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tickmine</groupId>
                <artifactId>agent-planner</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tickmine</groupId>
                <artifactId>agent-executor</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.tickmine</groupId>
                <artifactId>agent-api</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <release>${java.version}</release>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: 创建 agent-domain/pom.xml（纯 Java，无 Spring）**

```xml
<project>
    <parent>
        <groupId>com.tickmine</groupId>
        <artifactId>agent-server</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>
    <artifactId>agent-domain</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 创建其余模块 pom（依赖链）**

`agent-infra/pom.xml` 依赖: `agent-domain`, `spring-boot-starter-data-jpa`, `spring-boot-starter-data-redis`, `flyway-core`, `postgresql`

`agent-llm/pom.xml` 依赖: `agent-domain`, `agent-infra`, `spring-ai-starter-model-openai`, `spring-boot-starter`

`agent-mcp/pom.xml` 依赖: `agent-domain`, `spring-boot-starter-webflux`（WebClient）

`agent-planner/pom.xml` 依赖: `agent-domain`, `agent-llm`, `spring-boot-starter`

`agent-executor/pom.xml` 依赖: `agent-domain`, `agent-mcp`, `agent-infra`

`agent-api/pom.xml` 依赖: `agent-domain`, `spring-boot-starter-web`

`agent-boot/pom.xml` 依赖: 所有模块 + `spring-boot-starter-actuator` + `spring-boot-starter-test` + `testcontainers-postgresql`

- [ ] **Step 4: 验证编译**

```bash
cd tickmine-api/agent-server && mvn -q validate
```
Expected: BUILD SUCCESS（空模块）

- [ ] **Step 5: Commit**

```bash
git add tickmine-api/agent-server/
git commit -m "feat: scaffold Maven multi-module agent-server skeleton"
```

---

### Task 2: 领域模型与 Port 接口

**Files:**
- Create: `agent-domain/src/main/java/com/tickmine/domain/model/*.java`
- Create: `agent-domain/src/main/java/com/tickmine/domain/port/*.java`

- [ ] **Step 1: 创建枚举与 record**

`GoalStatus.java`: `DRAFT, ACTIVE, DONE`

`GoalPhase.java`: `COLLECTING, PLAN_READY, EXECUTING, COMPLETED, FAILED`

`SubscriptionTier.java`: `FREE, VIP, SVIP`

`TokenStatus.java`: `CONNECTED, NOT_CONNECTED`

`PlanDsl.java`:
```java
package com.tickmine.domain.model;

import java.time.LocalDate;
import java.util.List;

public record PlanDsl(String projectName, List<MilestoneDsl> milestones) {}
public record MilestoneDsl(String name, List<TaskDsl> tasks) {}
public record TaskDsl(
    String title,
    String description,
    String priority,
    LocalDate dueDate,
    List<ChecklistItemDsl> checklistItems
) {
    public TaskDsl {
        if (checklistItems == null) checklistItems = List.of();
    }
}
public record ChecklistItemDsl(String title) {}
```

`GoalContext.java`:
```java
package com.tickmine.domain.model;

import java.util.HashMap;
import java.util.Map;

public class GoalContext {
    private Map<String, Object> attributes = new HashMap<>();
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    public void merge(Map<String, Object> attrs) { if (attrs != null) attributes.putAll(attrs); }
}
```

`Goal.java`:
```java
package com.tickmine.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder
public class Goal {
    private UUID id;
    private String userId;
    private String title;
    private String description;
    private GoalStatus status;
    private GoalPhase phase;
    private LocalDate targetDate;
    private GoalContext context;
    private String ticktickProjectId;
}
```

`GoalAnalysis.java`:
```java
public record GoalAnalysis(
    boolean isComplete,
    List<String> missingFields,
    Map<String, Object> extractedAttributes,
    String suggestedTitle
) {}
```

`ChatMessage.java`:
```java
public record ChatMessage(String role, String content, Instant timestamp) {}
```

`ExecutionResult.java`:
```java
public record ExecutionResult(
    boolean success,
    String projectId,
    List<String> taskIds,
    String errorMessage
) {}
```

- [ ] **Step 2: 创建 Port 接口**

`Planner.java`:
```java
package com.tickmine.domain.port;

import com.tickmine.domain.model.Goal;
import com.tickmine.domain.model.GoalContext;
import com.tickmine.domain.model.PlanDsl;

public interface Planner {
    PlanDsl generatePlan(Goal goal, GoalContext context);
}
```

`PlanExecutor.java`:
```java
public interface PlanExecutor {
    ExecutionResult execute(PlanDsl plan, String ticktickToken);
}
```

`TickTickClient.java`:
```java
public interface TickTickClient {
    String createProject(String name, String token);
    String createTask(TickTickTaskRequest task, String token);
    void updateTask(String taskId, TickTickTaskRequest task, String token);
    List<TickTickTaskResponse> listTasks(String projectId, String token);
}
```

`TickTickTaskRequest.java` / `TickTickTaskResponse.java` 放在 `agent-domain` 或 `agent-mcp`（计划放 agent-mcp/dto，domain 用接口参数 record）。

- [ ] **Step 3: Commit**

```bash
git add agent-domain/
git commit -m "feat: add domain models and port interfaces"
```

---

### Task 3: Flyway 数据库迁移

**Files:**
- Create: `agent-boot/src/main/resources/db/migration/V1__init_schema.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
CREATE TABLE users (
    id              VARCHAR(100) PRIMARY KEY,
    subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE',
    ticktick_token_enc TEXT,
    token_status    VARCHAR(20) NOT NULL DEFAULT 'NOT_CONNECTED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE goals (
    id                  UUID PRIMARY KEY,
    user_id             VARCHAR(100) NOT NULL REFERENCES users(id),
    title               VARCHAR(255),
    description         TEXT,
    status              VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    phase               VARCHAR(50) NOT NULL DEFAULT 'COLLECTING',
    target_date         DATE,
    context             JSONB NOT NULL DEFAULT '{}',
    ticktick_project_id VARCHAR(100),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_goals_user_id ON goals(user_id);

CREATE TABLE conversations (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(100) NOT NULL REFERENCES users(id),
    goal_id     UUID REFERENCES goals(id),
    messages    JSONB NOT NULL DEFAULT '[]',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_conversations_user_goal ON conversations(user_id, goal_id);

CREATE TABLE plans (
    id          UUID PRIMARY KEY,
    goal_id     UUID NOT NULL REFERENCES goals(id),
    dsl         JSONB NOT NULL,
    version     INT NOT NULL DEFAULT 1,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_plans_goal_id ON plans(goal_id);

CREATE TABLE quota_usage (
    user_id     VARCHAR(100) NOT NULL REFERENCES users(id),
    usage_date  DATE NOT NULL,
    chat_count  INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, usage_date)
);

CREATE TABLE plan_executions (
    id              UUID PRIMARY KEY,
    plan_id         UUID NOT NULL REFERENCES plans(id),
    status          VARCHAR(20) NOT NULL,
    ticktick_refs   JSONB,
    error_message   TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

- [ ] **Step 2: Commit**

```bash
git add agent-boot/src/main/resources/db/migration/
git commit -m "feat: add Flyway V1 schema migration"
```

---

### Task 4: JPA Entity 与 Repository

**Files:**
- Create: `agent-infra/src/main/java/com/tickmine/infra/persistence/entity/*.java`
- Create: `agent-infra/src/main/java/com/tickmine/infra/persistence/repository/*.java`
- Create: `agent-infra/src/main/java/com/tickmine/infra/persistence/mapper/DomainMapper.java`

- [ ] **Step 1: UserEntity**

```java
@Entity @Table(name = "users")
@Data
public class UserEntity {
    @Id private String id;
    @Column(name = "subscription_tier") @Enumerated(EnumType.STRING)
    private SubscriptionTier subscriptionTier;
    @Column(name = "ticktick_token_enc") private String ticktickTokenEnc;
    @Column(name = "token_status") @Enumerated(EnumType.STRING)
    private TokenStatus tokenStatus;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
```

- [ ] **Step 2: GoalEntity（context/dsl 用 JSONB）**

```java
@Entity @Table(name = "goals")
public class GoalEntity {
    @Id private UUID id;
    private String userId;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING) private GoalStatus status;
    @Enumerated(EnumType.STRING) private GoalPhase phase;
    private LocalDate targetDate;
    @JdbcTypeCode(SqlTypes.JSON)
    private GoalContext context;
    private String ticktickProjectId;
    private Instant createdAt;
    private Instant updatedAt;
}
```

同理创建 `ConversationEntity`, `PlanEntity`, `QuotaUsageEntity`, `PlanExecutionEntity`。

`PlanEntity.dsl` 字段：
```java
@JdbcTypeCode(SqlTypes.JSON)
private PlanDsl dsl;
```

- [ ] **Step 3: Repository 接口**

```java
public interface UserRepository extends JpaRepository<UserEntity, String> {}
public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {
    List<GoalEntity> findByUserId(String userId);
}
public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
    Optional<ConversationEntity> findByUserIdAndGoalId(String userId, UUID goalId);
}
public interface PlanRepository extends JpaRepository<PlanEntity, UUID> {
    Optional<PlanEntity> findFirstByGoalIdOrderByVersionDesc(UUID goalId);
}
public interface QuotaUsageRepository extends JpaRepository<QuotaUsageEntity, QuotaUsageId> {}
public interface PlanExecutionRepository extends JpaRepository<PlanExecutionEntity, UUID> {}
```

- [ ] **Step 4: DomainMapper 双向转换**

```java
@Component
public class DomainMapper {
    public Goal toDomain(GoalEntity e) { /* map fields */ }
    public GoalEntity toEntity(Goal g) { /* map fields */ }
    // users, plans, conversations 同理
}
```

- [ ] **Step 5: 写 Repository 集成测试（Testcontainers）**

`agent-boot/src/test/java/com/tickmine/FlywayMigrationTest.java`:
```java
@Testcontainers
@SpringBootTest
class FlywayMigrationTest {
    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
    @Test
    void contextLoads() { /* verify tables exist */ }
}
```

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: add JPA entities, repositories, and domain mapper"
```

---

### Task 5: TokenEncryptor（AES-256-GCM）

**Files:**
- Create: `agent-infra/src/main/java/com/tickmine/infra/crypto/TokenEncryptor.java`
- Test: `agent-infra/src/test/java/com/tickmine/infra/crypto/TokenEncryptorTest.java`

- [ ] **Step 1: 写失败测试**

```java
class TokenEncryptorTest {
    private final TokenEncryptor encryptor = new TokenEncryptor(
        Base64.getDecoder().decode("0123456789ABCDEF0123456789ABCDEF")
    );

    @Test
    void roundTrip() {
        String plain = "ticktick-token-abc123";
        String enc = encryptor.encrypt(plain);
        assertNotEquals(plain, enc);
        assertEquals(plain, encryptor.decrypt(enc));
    }
}
```

- [ ] **Step 2: 实现 TokenEncryptor**

```java
public class TokenEncryptor {
    private static final String ALGO = "AES/GCM/NoPadding";
    private final SecretKey key;

    public TokenEncryptor(byte[] keyBytes) {
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plain) {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
        buf.put(iv).put(cipherText);
        return Base64.getEncoder().encodeToString(buf.array());
    }

    public String decrypt(String encoded) {
        byte[] input = Base64.getDecoder().decode(encoded);
        byte[] iv = Arrays.copyOfRange(input, 0, 12);
        byte[] cipherText = Arrays.copyOfRange(input, 12, input.length);
        Cipher cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }
}
```

- [ ] **Step 3: 运行测试**

```bash
mvn -pl agent-infra test -Dtest=TokenEncryptorTest
```
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add AES-256-GCM TokenEncryptor"
```

---

### Task 6: 配置属性 TickMineProperties

**Files:**
- Create: `agent-infra/src/main/java/com/tickmine/infra/config/TickMineProperties.java`

- [ ] **Step 1: 实现配置类**

```java
@ConfigurationProperties(prefix = "tickmine")
@Data
public class TickMineProperties {
    private Encryption encryption = new Encryption();
    private Map<String, ModelConfig> models = new HashMap<>();
    private Map<String, QuotaConfig> quota = new HashMap<>();
    private LlmKeys llm = new LlmKeys();

    @Data public static class Encryption { private String secretKey; }
    @Data public static class ModelConfig {
        private String provider;
        private String model;
        private String baseUrl;
    }
    @Data public static class QuotaConfig { private int dailyChatLimit; }
    @Data public static class LlmKeys {
        private String deepseekApiKey;
        private String qwenApiKey;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: add TickMineProperties configuration binding"
```

---

### Task 7: ModelResolver 与 LLM 双 Provider

**Files:**
- Create: `agent-llm/src/main/java/com/tickmine/llm/ModelResolver.java`
- Create: `agent-llm/src/main/java/com/tickmine/llm/config/LlmConfig.java`
- Test: `agent-llm/src/test/java/com/tickmine/llm/ModelResolverTest.java`

- [ ] **Step 1: LlmConfig 注册两个 ChatModel bean**

```java
@Configuration
@EnableConfigurationProperties(TickMineProperties.class)
public class LlmConfig {

    @Bean
    public ChatModel deepseekChatModel(TickMineProperties props) {
        var cfg = props.getModels().get("FREE");
        return OpenAiChatModel.builder()
            .openAiApi(OpenAiApi.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(props.getLlm().getDeepseekApiKey())
                .build())
            .defaultOptions(OpenAiChatOptions.builder()
                .model(cfg.getModel())
                .build())
            .build();
    }

    @Bean
    public ChatModel qwenChatModel(TickMineProperties props) {
        var cfg = props.getModels().get("SVIP");
        return OpenAiChatModel.builder()
            .openAiApi(OpenAiApi.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(props.getLlm().getQwenApiKey())
                .build())
            .defaultOptions(OpenAiChatOptions.builder()
                .model(cfg.getModel())
                .build())
            .build();
    }
}
```

- [ ] **Step 2: ModelResolver**

```java
@Service
@RequiredArgsConstructor
public class ModelResolver {
    private final UserRepository userRepository;
    private final ChatModel deepseekChatModel;
    private final ChatModel qwenChatModel;
    private final TickMineProperties props;

    public ChatModel resolve(String userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        ModelConfig cfg = props.getModels().get(user.getSubscriptionTier().name());
        return "qwen".equals(cfg.getProvider()) ? qwenChatModel : deepseekChatModel;
    }

    public String resolveModelName(String userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        return props.getModels().get(user.getSubscriptionTier().name()).getModel();
    }
}
```

- [ ] **Step 3: ModelResolver 单元测试（mock UserRepository）**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add tier-based ModelResolver with DeepSeek and Qwen"
```

---

### Task 8: AgentChatService

**Files:**
- Create: `agent-llm/src/main/java/com/tickmine/llm/AgentChatService.java`

- [ ] **Step 1: 实现 chat 与 structuredOutput**

```java
@Service
@RequiredArgsConstructor
public class AgentChatService {
    private final ModelResolver modelResolver;
    private final MeterRegistry meterRegistry;

    public String chat(String userId, String systemPrompt, String userPrompt) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ChatModel model = modelResolver.resolve(userId);
            String response = ChatClient.create(model)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
            meterRegistry.counter("tickmine.llm.calls", "tier", tierOf(userId)).increment();
            return response;
        } finally {
            sample.stop(meterRegistry.timer("tickmine.llm.duration"));
        }
    }

    public <T> T structuredOutput(String userId, String systemPrompt, String userPrompt, Class<T> type) {
        ChatModel model = modelResolver.resolve(userId);
        return ChatClient.create(model)
            .prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .entity(type);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat: add AgentChatService with structured output support"
```

---

### Task 9: TickTickClient HTTP 实现

**Files:**
- Create: `agent-mcp/src/main/java/com/tickmine/mcp/TickTickClientImpl.java`
- Create: `agent-mcp/src/main/java/com/tickmine/mcp/dto/*.java`
- Test: `agent-mcp/src/test/java/com/tickmine/mcp/TickTickClientImplTest.java`

- [ ] **Step 1: DTO**

```java
@Data @Builder
public class TickTickTaskRequest {
    private String title;
    private String projectId;
    private String content;
    private Integer priority;
    private String dueDate;      // yyyy-MM-dd'T'00:00:00+0000
    private Boolean isAllDay;
    private String parentId;
    private String kind;         // TEXT or CHECKLIST
    private List<TickTickChecklistItem> items;
}
```

- [ ] **Step 2: TickTickClientImpl**

```java
@Component
public class TickTickClientImpl implements TickTickClient {
    private static final String BASE = "https://api.ticktick.com/open/v1";
    private final WebClient.Builder webClientBuilder;
    private final MeterRegistry meterRegistry;

    @Override
    public String createProject(String name, String token) {
        Map<String, Object> body = Map.of("name", name, "viewMode", "list");
        Map response = post("/project", body, token);
        meterRegistry.counter("tickmine.ticktick.calls", "op", "createProject").increment();
        return (String) response.get("id");
    }

    @Override
    public String createTask(TickTickTaskRequest task, String token) {
        Map response = post("/task", task, token);
        meterRegistry.counter("tickmine.ticktick.calls", "op", "createTask").increment();
        return (String) response.get("id");
    }

    private Map post(String path, Object body, String token) {
        try {
            return webClientBuilder.build()
                .post()
                .uri(BASE + path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        } catch (WebClientResponseException e) {
            meterRegistry.counter("tickmine.ticktick.failures").increment();
            throw new TickTickApiException(e.getStatusCode(), e.getResponseBodyAsString());
        }
    }
}
```

- [ ] **Step 3: MockWebServer 测试 createProject/createTask**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add TickTick Open API client implementation"
```

---

### Task 10: TickTickPlanExecutor

**Files:**
- Create: `agent-executor/src/main/java/com/tickmine/executor/TickTickPlanExecutor.java`
- Create: `agent-executor/src/main/java/com/tickmine/executor/PriorityMapper.java`
- Test: `agent-executor/src/test/java/com/tickmine/executor/TickTickPlanExecutorTest.java`

- [ ] **Step 1: PriorityMapper 测试**

```java
class PriorityMapperTest {
    @Test void mapsHigh() { assertEquals(5, PriorityMapper.toTickTick("high")); }
    @Test void mapsUnknown() { assertEquals(0, PriorityMapper.toTickTick("unknown")); }
}
```

```java
public final class PriorityMapper {
    public static int toTickTick(String priority) {
        if (priority == null) return 0;
        return switch (priority.toLowerCase()) {
            case "low" -> 1;
            case "medium" -> 3;
            case "high" -> 5;
            default -> 0;
        };
    }
}
```

- [ ] **Step 2: Executor 实现**

```java
@Service
@RequiredArgsConstructor
public class TickTickPlanExecutor implements PlanExecutor {
    private final TickTickClient tickTickClient;

    @Override
    public ExecutionResult execute(PlanDsl plan, String ticktickToken) {
        List<String> taskIds = new ArrayList<>();
        try {
            String projectId = tickTickClient.createProject(plan.projectName(), ticktickToken);
            for (MilestoneDsl milestone : plan.milestones()) {
                String parentId = tickTickClient.createTask(
                    TickTickTaskRequest.builder()
                        .title(milestone.name())
                        .projectId(projectId)
                        .build(),
                    ticktickToken);
                taskIds.add(parentId);
                for (TaskDsl task : milestone.tasks()) {
                    String taskId = tickTickClient.createTask(toRequest(task, projectId, parentId), ticktickToken);
                    taskIds.add(taskId);
                }
            }
            return new ExecutionResult(true, projectId, taskIds, null);
        } catch (Exception e) {
            return new ExecutionResult(false, null, taskIds, e.getMessage());
        }
    }

    private TickTickTaskRequest toRequest(TaskDsl task, String projectId, String parentId) {
        var builder = TickTickTaskRequest.builder()
            .title(task.title())
            .projectId(projectId)
            .content(task.description())
            .priority(PriorityMapper.toTickTick(task.priority()))
            .parentId(parentId)
            .isAllDay(true);
        if (task.dueDate() != null) {
            builder.dueDate(task.dueDate() + "T00:00:00+0000");
        }
        if (!task.checklistItems().isEmpty()) {
            builder.kind("CHECKLIST");
            builder.items(task.checklistItems().stream()
                .map(c -> new TickTickChecklistItem(c.title()))
                .toList());
        }
        return builder.build();
    }
}
```

- [ ] **Step 3: Mockito 验证调用顺序**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add TickTickPlanExecutor with milestone/task hierarchy"
```

---

### Task 11: Prompt 模板

**Files:**
- Create: `agent-boot/src/main/resources/prompts/goal-analyzer.st`
- Create: `agent-boot/src/main/resources/prompts/follow-up.st`
- Create: `agent-boot/src/main/resources/prompts/planner.st`
- Create: `agent-boot/src/main/resources/prompts/review.st`（占位，MVP 不用）

- [ ] **Step 1: goal-analyzer.st**

```
你是一个目标分析助手。根据用户目标和已有上下文，判断信息是否足够生成执行计划。

目标标题: {title}
目标描述: {description}
已收集属性: {attributes}
最近对话:
{conversation}

请分析并返回 JSON：
- isComplete: 是否已收集足够信息（时间、地点、规模、预算等关键要素）
- missingFields: 仍缺失的字段名列表
- extractedAttributes: 从对话中新提取的属性键值对
- suggestedTitle: 建议的目标标题
```

- [ ] **Step 2: follow-up.st**

```
你是友好的任务规划助手。用户正在规划「{title}」，还缺少以下信息：{missingFields}。
已已知信息：{attributes}

请用简洁的中文提出 2-4 个追问，帮助收集缺失信息。语气自然，不要列表编号过多。
```

- [ ] **Step 3: planner.st**

```
你是专业项目规划师。根据以下目标信息，生成 TickTick 项目执行计划。

目标: {title}
描述: {description}
上下文: {attributes}
截止日期: {targetDate}

要求：
1. projectName 简洁明了
2. milestones 按阶段划分（3-6 个）
3. 每个 milestone 包含 2-5 个具体可执行任务
4. priority 使用 low/medium/high
5. dueDate 合理分布在目标日期之前

返回结构化计划。
```

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add Spring AI prompt templates"
```

---

### Task 12: GoalAnalyzer 与 LlmPlanner

**Files:**
- Create: `agent-planner/src/main/java/com/tickmine/planner/GoalAnalyzer.java`
- Create: `agent-planner/src/main/java/com/tickmine/planner/LlmPlanner.java`
- Create: `agent-planner/src/main/java/com/tickmine/planner/PromptLoader.java`

- [ ] **Step 1: PromptLoader**

```java
@Component
public class PromptLoader {
    private final ResourceLoader resourceLoader;

    public String load(String name, Map<String, Object> variables) {
        Resource resource = resourceLoader.getResource("classpath:prompts/" + name);
        PromptTemplate template = new PromptTemplate(resource);
        return template.render(variables);
    }
}
```

- [ ] **Step 2: GoalAnalyzer**

```java
@Service
@RequiredArgsConstructor
public class GoalAnalyzer {
    private final AgentChatService chatService;
    private final PromptLoader promptLoader;

    public GoalAnalysis analyze(String userId, Goal goal, List<ChatMessage> history) {
        String prompt = promptLoader.load("goal-analyzer.st", Map.of(
            "title", goal.getTitle(),
            "description", nullToEmpty(goal.getDescription()),
            "attributes", goal.getContext().getAttributes(),
            "conversation", formatHistory(history)
        ));
        return chatService.structuredOutput(userId, "你是结构化分析助手。", prompt, GoalAnalysis.class);
    }
}
```

- [ ] **Step 3: LlmPlanner implements Planner**

```java
@Service
@RequiredArgsConstructor
public class LlmPlanner implements Planner {
    private final AgentChatService chatService;
    private final PromptLoader promptLoader;

    @Override
    public PlanDsl generatePlan(Goal goal, GoalContext context) {
        String prompt = promptLoader.load("planner.st", Map.of(
            "title", goal.getTitle(),
            "description", nullToEmpty(goal.getDescription()),
            "attributes", context.getAttributes(),
            "targetDate", String.valueOf(goal.getTargetDate())
        ));
        return chatService.structuredOutput(
            goal.getUserId(), "你是项目规划师。", prompt, PlanDsl.class);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add GoalAnalyzer and LlmPlanner"
```

---

### Task 13: QuotaService

**Files:**
- Create: `agent-domain/src/main/java/com/tickmine/domain/service/QuotaService.java`
- Test: `agent-domain/src/test/java/com/tickmine/domain/service/QuotaServiceTest.java`

- [ ] **Step 1: 测试 FREE 用户超限**

- [ ] **Step 2: 实现**

```java
@Service
@RequiredArgsConstructor
public class QuotaService {
    private final UserRepository userRepository;
    private final QuotaUsageRepository quotaUsageRepository;
    private final TickMineProperties props;

    public void checkAndConsume(String userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        int limit = props.getQuota()
            .get(user.getSubscriptionTier().name())
            .getDailyChatLimit();
        if (limit < 0) return; // unlimited

        LocalDate today = LocalDate.now();
        QuotaUsageEntity usage = quotaUsageRepository
            .findById(new QuotaUsageId(userId, today))
            .orElseGet(() -> new QuotaUsageEntity(userId, today, 0));

        if (usage.getChatCount() >= limit) {
            throw new QuotaExceededException(userId, limit);
        }
        usage.setChatCount(usage.getChatCount() + 1);
        quotaUsageRepository.save(usage);
    }

    public QuotaStatus getStatus(String userId) { /* return tier, limit, used, remaining */ }
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add QuotaService with FREE daily limit"
```

---

### Task 14: ConversationService 与 Redis 缓存

**Files:**
- Create: `agent-infra/src/main/java/com/tickmine/infra/redis/ConversationCache.java`
- Create: `agent-domain/src/main/java/com/tickmine/domain/service/ConversationService.java`

- [ ] **Step 1: ConversationCache**

```java
@Component
@RequiredArgsConstructor
public class ConversationCache {
    private static final int MAX_MESSAGES = 20;
    private static final Duration TTL = Duration.ofHours(24);
    private final StringRedisTemplate redis;

    public List<ChatMessage> get(String userId, UUID goalId) {
        String json = redis.opsForValue().get(key(userId, goalId));
        if (json == null) return null;
        return parse(json);
    }

    public void put(String userId, UUID goalId, List<ChatMessage> messages) {
        List<ChatMessage> trimmed = messages.size() > MAX_MESSAGES
            ? messages.subList(messages.size() - MAX_MESSAGES, messages.size())
            : messages;
        redis.opsForValue().set(key(userId, goalId), serialize(trimmed), TTL);
    }

    private String key(String userId, UUID goalId) {
        return "conversation:" + userId + ":" + goalId;
    }
}
```

- [ ] **Step 2: ConversationService**

```java
@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationCache cache;
    private final DomainMapper mapper;

    public List<ChatMessage> loadHistory(String userId, UUID goalId) {
        List<ChatMessage> cached = cache.get(userId, goalId);
        if (cached != null) return cached;
        return conversationRepository.findByUserIdAndGoalId(userId, goalId)
            .map(e -> mapper.toMessages(e.getMessages()))
            .orElse(List.of());
    }

    public void appendMessage(String userId, UUID goalId, ChatMessage message) {
        List<ChatMessage> history = new ArrayList<>(loadHistory(userId, goalId));
        history.add(message);
        // upsert conversation entity in PG
        cache.put(userId, goalId, history);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add ConversationService with Redis cache"
```

---

### Task 15: GoalAgentService（状态机核心）

**Files:**
- Create: `agent-domain/src/main/java/com/tickmine/domain/service/GoalAgentService.java`
- Create: `agent-domain/src/main/java/com/tickmine/domain/service/UserService.java`

- [ ] **Step 1: UserService（token 绑定）**

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TokenEncryptor tokenEncryptor;

    public void bindTickTickToken(String userId, String plainToken) {
        UserEntity user = findOrCreate(userId);
        user.setTicktickTokenEnc(tokenEncryptor.encrypt(plainToken));
        user.setTokenStatus(TokenStatus.CONNECTED);
        userRepository.save(user);
    }

    public String getDecryptedToken(String userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getTokenStatus() != TokenStatus.CONNECTED) {
            throw new TickTickNotConnectedException(userId);
        }
        return tokenEncryptor.decrypt(user.getTicktickTokenEnc());
    }

    public UserEntity findOrCreate(String userId) {
        return userRepository.findById(userId).orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setId(userId);
            u.setSubscriptionTier(SubscriptionTier.FREE);
            u.setTokenStatus(TokenStatus.NOT_CONNECTED);
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return userRepository.save(u);
        });
    }
}
```

- [ ] **Step 2: GoalAgentService.handleChat**

```java
@Service
@RequiredArgsConstructor
public class GoalAgentService {
    private final QuotaService quotaService;
    private final ConversationService conversationService;
    private final GoalAnalyzer goalAnalyzer;
    private final Planner planner;
    private final GoalRepository goalRepository;
    private final PlanRepository planRepository;
    private final PromptLoader promptLoader;
    private final AgentChatService chatService;

    @Transactional
    public ChatResponse handleChat(String userId, String message, UUID goalId) {
        quotaService.checkAndConsume(userId);
        Goal goal = resolveGoal(userId, message, goalId);
        conversationService.appendMessage(userId, goal.getId(),
            new ChatMessage("user", message, Instant.now()));

        List<ChatMessage> history = conversationService.loadHistory(userId, goal.getId());
        GoalAnalysis analysis = goalAnalyzer.analyze(userId, goal, history);
        goal.getContext().merge(analysis.extractedAttributes());
        if (analysis.suggestedTitle() != null) goal.setTitle(analysis.suggestedTitle());

        if (!analysis.isComplete()) {
            goal.setPhase(GoalPhase.COLLECTING);
            goalRepository.save(mapper.toEntity(goal));
            String reply = chatService.chat(userId, "你是助手",
                promptLoader.load("follow-up.st", Map.of(
                    "title", goal.getTitle(),
                    "missingFields", analysis.missingFields(),
                    "attributes", goal.getContext().getAttributes())));
            conversationService.appendMessage(userId, goal.getId(),
                new ChatMessage("assistant", reply, Instant.now()));
            return ChatResponse.collecting(goal.getId(), reply, analysis.missingFields());
        }

        PlanDsl plan = planner.generatePlan(goal, goal.getContext());
        savePlan(goal.getId(), plan);
        goal.setPhase(GoalPhase.PLAN_READY);
        goal.setStatus(GoalStatus.ACTIVE);
        goalRepository.save(mapper.toEntity(goal));

        String preview = formatPlanPreview(plan);
        conversationService.appendMessage(userId, goal.getId(),
            new ChatMessage("assistant", preview, Instant.now()));
        return ChatResponse.planReady(goal.getId(), preview, plan);
    }
}
```

- [ ] **Step 3: GoalAgentService.executePlan**

```java
@Transactional
public ExecutionResult executePlan(UUID goalId) {
    GoalEntity entity = goalRepository.findById(goalId)
        .orElseThrow(() -> new GoalNotFoundException(goalId));
    if (entity.getPhase() != GoalPhase.PLAN_READY) {
        throw new InvalidGoalPhaseException(entity.getPhase());
    }
    String token = userService.getDecryptedToken(entity.getUserId());
    PlanEntity planEntity = planRepository.findFirstByGoalIdOrderByVersionDesc(goalId)
        .orElseThrow(() -> new PlanNotFoundException(goalId));

    entity.setPhase(GoalPhase.EXECUTING);
    goalRepository.save(entity);

    ExecutionResult result = planExecutor.execute(planEntity.getDsl(), token);
    if (result.success()) {
        entity.setPhase(GoalPhase.COMPLETED);
        entity.setTicktickProjectId(result.projectId());
        entity.setStatus(GoalStatus.DONE);
    } else {
        entity.setPhase(GoalPhase.FAILED);
    }
    goalRepository.save(entity);
    savePlanExecution(planEntity.getId(), result);
    return result;
}
```

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add GoalAgentService state machine orchestration"
```

---

### Task 16: REST API Controllers

**Files:**
- Create: `agent-api/src/main/java/com/tickmine/api/controller/ChatController.java`
- Create: `agent-api/src/main/java/com/tickmine/api/controller/GoalController.java`
- Create: `agent-api/src/main/java/com/tickmine/api/controller/UserController.java`
- Create: `agent-api/src/main/java/com/tickmine/api/dto/*.java`

- [ ] **Step 1: DTO**

```java
public record ChatRequest(String userId, String message, UUID goalId) {}
public record ChatResponseDto(UUID goalId, String phase, String reply, PlanDsl plan, List<String> missingFields) {}
public record BindTokenRequest(String token) {}
public record CreateGoalRequest(String userId, String title, String description) {}
public record QuotaResponseDto(String tier, int dailyLimit, int used, int remaining) {}
```

- [ ] **Step 2: Controllers**

```java
@RestController @RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final GoalAgentService goalAgentService;
    @PostMapping
    public ChatResponseDto chat(@RequestBody ChatRequest req) {
        return toDto(goalAgentService.handleChat(req.userId(), req.message(), req.goalId()));
    }
}

@RestController @RequestMapping("/api/goals")
public class GoalController {
    @PostMapping public GoalResponseDto create(@RequestBody CreateGoalRequest req) { ... }
    @GetMapping("/{id}") public GoalResponseDto get(@PathVariable UUID id) { ... }
    @PostMapping("/{id}/plan") public PlanDsl replan(@PathVariable UUID id) { ... }
    @PostMapping("/{id}/execute") public ExecutionResult execute(@PathVariable UUID id) { ... }
}

@RestController @RequestMapping("/api/users")
public class UserController {
    @PutMapping("/{userId}/ticktick-token") public void bindToken(...) { ... }
    @GetMapping("/{userId}/ticktick-token/status") public TokenStatusDto status(...) { ... }
    @GetMapping("/{userId}/quota") public QuotaResponseDto quota(...) { ... }
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add REST API controllers for chat, goals, and users"
```

---

### Task 17: 全局异常处理

**Files:**
- Create: `agent-api/src/main/java/com/tickmine/api/exception/GlobalExceptionHandler.java`
- Create: domain/infra 层 exception 类

- [ ] **Step 1: 异常类**

`QuotaExceededException`, `UserNotFoundException`, `GoalNotFoundException`, `InvalidGoalPhaseException`, `TickTickNotConnectedException`, `TickTickApiException`

- [ ] **Step 2: GlobalExceptionHandler**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(QuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handle(QuotaExceededException e) {
        return new ErrorResponse("QUOTA_EXCEEDED", e.getMessage());
    }
    @ExceptionHandler(InvalidGoalPhaseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handle(InvalidGoalPhaseException e) {
        return new ErrorResponse("INVALID_PHASE", e.getMessage());
    }
    // 404, 400, 502, 503 同理
}
```

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add global exception handler with standard error responses"
```

---

### Task 18: agent-boot 启动配置

**Files:**
- Create: `agent-boot/src/main/java/com/tickmine/TickMineApplication.java`
- Create: `agent-boot/src/main/resources/application.yml`

- [ ] **Step 1: 主类**

```java
@SpringBootApplication(scanBasePackages = "com.tickmine")
@EnableConfigurationProperties(TickMineProperties.class)
public class TickMineApplication {
    public static void main(String[] args) {
        SpringApplication.run(TickMineApplication.class, args);
    }
}
```

- [ ] **Step 2: application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: tickmine-agent
  datasource:
    url: jdbc:postgresql://localhost:5432/tickmine
    username: tickmine
    password: tickmine
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
  data:
    redis:
      host: localhost
      port: 6379

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

tickmine:
  encryption:
    secret-key: ${TICKMINE_ENCRYPTION_KEY}
  models:
    FREE:  { provider: deepseek, model: deepseek-chat, base-url: https://api.deepseek.com }
    VIP:   { provider: deepseek, model: deepseek-chat, base-url: https://api.deepseek.com }
    SVIP:  { provider: qwen, model: qwen-plus, base-url: https://dashscope.aliyuncs.com/compatible-mode/v1 }
  quota:
    FREE:  { daily-chat-limit: 10 }
    VIP:   { daily-chat-limit: -1 }
    SVIP:  { daily-chat-limit: -1 }
  llm:
    deepseek-api-key: ${DEEPSEEK_API_KEY}
    qwen-api-key: ${QWEN_API_KEY}
```

- [ ] **Step 3: 验证启动**

```bash
# 先 docker compose up postgres redis
mvn -pl agent-boot spring-boot:run
curl http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"}`

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add Spring Boot application entry and configuration"
```

---

### Task 19: Docker Compose 部署

**Files:**
- Create: `tickmine-api/agent-server/Dockerfile`
- Create: `tickmine-api/agent-server/docker-compose.yml`
- Create: `tickmine-api/agent-server/.env.example`

- [ ] **Step 1: Dockerfile（多阶段构建）**

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY agent-*/pom.xml agent-*/
RUN mvn -q dependency:go-offline -B
COPY . .
RUN mvn -q package -DskipTests -pl agent-boot -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/agent-boot/target/agent-boot-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: docker-compose.yml**

```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: tickmine
      POSTGRES_USER: tickmine
      POSTGRES_PASSWORD: tickmine
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  app:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/tickmine
      SPRING_DATA_REDIS_HOST: redis
      DEEPSEEK_API_KEY: ${DEEPSEEK_API_KEY}
      QWEN_API_KEY: ${QWEN_API_KEY}
      TICKMINE_ENCRYPTION_KEY: ${TICKMINE_ENCRYPTION_KEY}
    depends_on: [postgres, redis]

volumes:
  pgdata:
```

- [ ] **Step 3: .env.example**

```
DEEPSEEK_API_KEY=sk-xxx
QWEN_API_KEY=sk-xxx
TICKMINE_ENCRYPTION_KEY=0123456789ABCDEF0123456789ABCDEF
```

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: add Dockerfile and docker-compose for local deployment"
```

---

### Task 20: 端到端冒烟测试

**Files:**
- Create: `agent-boot/src/test/java/com/tickmine/ChatFlowIntegrationTest.java`

- [ ] **Step 1: 集成测试（Mock LLM + Mock TickTick）**

使用 `@MockBean` 替换 `AgentChatService` 和 `TickTickClient`，验证：
1. `PUT /api/users/u1/ticktick-token` → connected
2. `POST /api/chat` → phase=COLLECTING（mock GoalAnalysis.isComplete=false）
3. `POST /api/chat` → phase=PLAN_READY（mock isComplete=true + PlanDsl）
4. `POST /api/goals/{id}/execute` → success

- [ ] **Step 2: 运行全量测试**

```bash
mvn test
```
Expected: ALL PASS

- [ ] **Step 3: 手动验证清单**

```bash
docker compose up -d
# 1. 绑定 token
curl -X PUT http://localhost:8080/api/users/u1/ticktick-token \
  -H 'Content-Type: application/json' -d '{"token":"YOUR_TICKTICK_TOKEN"}'
# 2. 发起对话
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1","message":"我要策划一场婚礼"}'
# 3. 多轮补充后确认执行
curl -X POST http://localhost:8080/api/goals/{goalId}/execute
```

- [ ] **Step 4: Commit**

```bash
git commit -m "test: add chat flow integration test and verify e2e smoke path"
```

---

## Self-Review Checklist

| Spec 章节 | 对应 Task |
|-----------|-----------|
| 8 模块结构 | Task 1 |
| 领域模型 | Task 2 |
| 数据库 6 张表 | Task 3, 4 |
| Agent 状态机 | Task 15 |
| REST API | Task 16 |
| LLM 分档 | Task 7, 8 |
| TickTick 执行 | Task 9, 10 |
| Prompt 模板 | Task 11, 12 |
| 配额 | Task 13 |
| Redis 缓存 | Task 14 |
| Token 加密 | Task 5 |
| 错误处理 | Task 17 |
| Actuator/Metrics | Task 8, 9, 18 |
| Docker | Task 19 |
| 测试策略 | Task 5, 10, 13, 20 |

**暂缓项（spec 非目标）无对应 Task：** Scheduler, OpenTelemetry, OAuth, JWT — 符合预期。

**类型一致性：** `PlanDsl`/`GoalPhase`/`ExecutionResult` 在 Task 2 定义，后续 Task 均引用同一包路径 `com.tickmine.domain.model`。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2025-06-22-tickmine-agent.md`. Two execution options:

**1. Subagent-Driven (recommended)** — 每个 Task 派发独立 subagent，任务间做 review，迭代快

**2. Inline Execution** — 在本 session 用 executing-plans 按 Task 批量执行，设置检查点

Which approach?
