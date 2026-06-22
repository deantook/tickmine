# TickMine Agent 后端设计文档

> 基于 `tickmine-api/prompt.md`，经 brainstorming 确认的设计规格。
> 日期：2025-06-22

## 1. 背景与目标

构建一个**任务管理 Agent 后端**，支持：

1. 与用户多轮对话，澄清目标
2. 生成结构化执行计划（Plan DSL）
3. 用户确认后，将计划写入 TickTick（创建项目与任务）
4. 多租户：每个用户独立 TickTick Token、独立订阅档位与 LLM 模型

### 非目标（MVP 暂缓）

- GoalReviewScheduler（定时巡检提醒）
- OpenTelemetry 全链路追踪
- OAuth 授权流（TickTick 账号绑定）
- JWT / 完整鉴权体系
- LLM 流式输出
- 异步计划执行

---

## 2. 关键决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| TickTick 集成 | **Open API 直连** | 多租户按请求注入用户 token，无需每用户启动 MCP Server |
| TickTick 账号绑定 | **手动粘贴 API Token** | MVP 实现快，token AES 加密存库 |
| LLM 分档 | FREE/VIP → DeepSeek；SVIP → Qwen | 按订阅档位路由模型 |
| FREE vs VIP 差异 | **同模型，配额不同** | FREE 每日对话次数上限，VIP/SVIP 不限 |
| 架构方案 | **8 模块骨架 + MVP 精简实现** | 符合 prompt 模块规划，控制首版工作量 |
| MVP 范围 | **核心闭环 + 基础设施** | 含 Docker/Flyway/Redis/Actuator；不含 Scheduler/深度可观测性 |

---

## 3. 整体架构

```
                    ┌─────────────────────────────────────┐
                    │           agent-api (REST)         │
                    │  /chat  /goals  /users/ticktick    │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │      agent-domain (领域服务)        │
                    │  GoalAgentService                   │
                    │  ConversationService                │
                    │  QuotaService / ModelResolver       │
                    └──────┬──────────┬──────────┬────────┘
                           │          │          │
              ┌────────────▼─┐  ┌─────▼─────┐  ┌▼────────────┐
              │ agent-planner│  │agent-exec │  │  agent-llm  │
              │  LlmPlanner  │  │TickTick   │  │AgentChat    │
              │              │  │PlanExec   │  │Service      │
              └──────┬───────┘  └─────┬─────┘  └──────┬──────┘
                     │                │                │
                     └────────────────┼────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │  agent-mcp (TickTick Open API Client)│
                    │  agent-infra (JPA/Redis/Flyway)    │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │  PostgreSQL  │  Redis  │  TickTick  │
                    └─────────────────────────────────────┘
```

### 核心约束（来自 prompt.md）

- LLM **只产出** `PlanDsl`（Spring AI Structured Output），**禁止** LLM 直接大量调用 TickTick
- `TickTickPlanExecutor` 是唯一执行入口，按 DSL 顺序调用 `TickTickClient`
- `agent-mcp` 模块封装 TickTick HTTP API；模块名保留，实现为 Open API Client，非 MCP 协议运行时

### 多租户请求链路

```
请求 userId
  → TenantContext 解析用户
  → users 表（tier + 加密 token）
  → ModelResolver（tier → DeepSeek / Qwen）
  → QuotaService（FREE 每日限额校验）
  → TickTickClient（请求级注入用户 token）
```

---

## 4. Maven 模块结构

根工程：`tickmine-api/agent-server/`（与 `prompt.md` 同目录）

```
agent-server/
├── agent-api          # REST Controller、DTO、全局异常
├── agent-domain       # 领域服务、领域模型、接口定义
├── agent-planner      # LlmPlanner、GoalAnalyzer
├── agent-executor     # TickTickPlanExecutor
├── agent-llm          # AgentChatService、Provider 配置
├── agent-mcp          # TickTickClient（HTTP 实现）
├── agent-infra        # JPA Entity/Repository、Redis、Flyway、加密
└── agent-boot         # 启动类、application.yml、Docker、Prompt 资源
```

### 技术栈

- Java 21
- Spring Boot 3.5.x
- Spring AI（Structured Output、PromptTemplate）
- PostgreSQL 16 + Spring Data JPA + Flyway
- Redis 7（会话缓存）
- Lombok、Maven
- Micrometer + Actuator（health/metrics，MVP 基础级）

---

## 5. 领域模型

### 5.1 核心对象

```java
public class Goal {
    UUID id;
    String userId;
    String title;
    String description;
    GoalStatus status;       // DRAFT, ACTIVE, DONE
    GoalPhase phase;         // 见状态机
    LocalDate targetDate;
    GoalContext context;
    String ticktickProjectId; // 执行后回填
}

public class GoalContext {
    Map<String, Object> attributes;  // city, budget, guestCount...
}

public record PlanDsl(String projectName, List<MilestoneDsl> milestones) {}
public record MilestoneDsl(String name, List<TaskDsl> tasks) {}
public record TaskDsl(
    String title,
    String description,
    String priority,       // 映射 TickTick: 0/1/3/5
    LocalDate dueDate,
    List<ChecklistItemDsl> checklistItems  // 可选
) {}
public record ChecklistItemDsl(String title) {}

public enum GoalPhase {
    COLLECTING,   // 信息收集中
    PLAN_READY,   // 计划已生成，待确认
    EXECUTING,    // 执行中
    COMPLETED,    // 已完成
    FAILED        // 执行失败
}

public record GoalAnalysis(
    boolean isComplete,
    List<String> missingFields,
    Map<String, Object> extractedAttributes,
    String suggestedTitle
) {}
```

### 5.2 接口定义

```java
public interface Planner {
    PlanDsl generatePlan(Goal goal, GoalContext context);
}

public interface PlanExecutor {
    ExecutionResult execute(PlanDsl plan, String ticktickToken);
}

public interface TickTickClient {
    String createProject(String name, String token);
    String createTask(OpenTask task, String token);
    void updateTask(String taskId, OpenTask task, String token);
    List<OpenTask> listTasks(String projectId, String token);
}
```

> Checklist 通过 `createTask` 的 `items` 字段创建，无独立 `create_checklist` API。

---

## 6. 数据库设计

### 6.1 users

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(100) PK | 业务 userId |
| subscription_tier | varchar(20) NOT NULL | FREE / VIP / SVIP |
| ticktick_token_enc | text | AES-256-GCM 加密 |
| token_status | varchar(20) | CONNECTED / NOT_CONNECTED |
| created_at | timestamp | |
| updated_at | timestamp | |

### 6.2 goals

| 字段 | 类型 | 说明 |
|------|------|------|
| id | uuid PK | |
| user_id | varchar(100) NOT NULL | |
| title | varchar(255) | |
| description | text | |
| status | varchar(50) | DRAFT / ACTIVE / DONE |
| phase | varchar(50) | GoalPhase |
| target_date | date | |
| context | jsonb | GoalContext |
| ticktick_project_id | varchar(100) | 执行后回填 |
| created_at / updated_at | timestamp | |

### 6.3 conversations

| 字段 | 类型 | 说明 |
|------|------|------|
| id | uuid PK | |
| user_id | varchar(100) NOT NULL | |
| goal_id | uuid nullable | |
| messages | jsonb | `[{role, content, timestamp}]` |
| created_at / updated_at | timestamp | |

### 6.4 plans

| 字段 | 类型 | 说明 |
|------|------|------|
| id | uuid PK | |
| goal_id | uuid NOT NULL FK | |
| dsl | jsonb | PlanDsl，`@JdbcTypeCode(SqlTypes.JSON)` |
| version | int DEFAULT 1 | |
| created_at | timestamp | |

### 6.5 quota_usage

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | varchar(100) | |
| usage_date | date | |
| chat_count | int DEFAULT 0 | |
| PRIMARY KEY | (user_id, usage_date) | |

### 6.6 plan_executions

| 字段 | 类型 | 说明 |
|------|------|------|
| id | uuid PK | |
| plan_id | uuid NOT NULL FK | |
| status | varchar(20) | RUNNING / SUCCESS / FAILED |
| ticktick_refs | jsonb | `{projectId, taskIds: [...]}` |
| error_message | text | |
| created_at | timestamp | |

---

## 7. Agent 状态机

```
POST /api/chat
        │
        ▼
GoalAgentService.handleChat()
  1. QuotaService.check(userId)          → FREE 超限返回 429
  2. 识别/创建 Goal
  3. ConversationService.appendMessage() → PG + Redis
  4. GoalAnalyzer（LLM structured output）
     ├─ isComplete=false → phase=COLLECTING，返回追问
     └─ isComplete=true  → Planner.generatePlan() → 保存 plans
                           phase=PLAN_READY，返回计划预览

POST /api/goals/{id}/execute
        │
        ▼
GoalAgentService.executePlan()
  1. 校验 phase == PLAN_READY
  2. 校验 ticktick_token 已绑定
  3. phase → EXECUTING
  4. TickTickPlanExecutor.execute(plan, decryptedToken)
     a. createProject(projectName)
     b. foreach milestone → createTask（里程碑级任务）
     c. foreach task → createTask（含 checklist items）
     d. 记录 plan_executions.ticktick_refs
  5. phase → COMPLETED（失败则 FAILED + error_message）
```

### Prompt 模板（`agent-boot/src/main/resources/prompts/`）

| 文件 | 用途 | MVP |
|------|------|-----|
| `goal-analyzer.st` | 判断信息完整性、提取属性 | ✅ |
| `follow-up.st` | 生成追问 | ✅ |
| `planner.st` | 生成 PlanDsl | ✅ |
| `review.st` | 定时巡检建议 | 预留 |

通过 Spring AI `PromptTemplate` 加载，禁止硬编码 Prompt。

---

## 8. REST API

### 8.1 TickTick 绑定

```
PUT  /api/users/{userId}/ticktick-token
     Request:  { "token": "xxx" }
     Response: { "status": "CONNECTED" }

GET  /api/users/{userId}/ticktick-token/status
     Response: { "connected": true }
```

### 8.2 目标管理

```
POST   /api/goals
       Request:  { "userId", "title", "description?" }
       Response: GoalResponse（含 id, phase）

GET    /api/goals/{id}
       Response: GoalResponse + latestPlan + phase

POST   /api/goals/{id}/plan
       Response: PlanDsl（强制重新生成）

POST   /api/goals/{id}/execute
       Response: ExecutionResult
```

### 8.3 对话

```
POST   /api/chat
       Request:  { "userId", "message", "goalId?" }
       Response: {
         "goalId": "uuid",
         "phase": "COLLECTING | PLAN_READY",
         "reply": "string",
         "plan": PlanDsl | null,
         "missingFields": [] | null
       }
```

### 8.4 配额

```
GET    /api/users/{userId}/quota
       Response: {
         "tier": "FREE",
         "dailyLimit": 10,
         "used": 3,
         "remaining": 7
       }
```

### 8.5 鉴权（MVP）

请求体/路径携带 `userId`，不做 JWT。预留 `TenantFilter` 接口供后期替换。

---

## 9. LLM 与配额配置

```yaml
tickmine:
  encryption:
    secret-key: ${TICKMINE_ENCRYPTION_KEY}  # 32 bytes for AES-256

  models:
    FREE:
      provider: deepseek
      model: deepseek-chat
      base-url: https://api.deepseek.com
    VIP:
      provider: deepseek
      model: deepseek-chat
      base-url: https://api.deepseek.com
    SVIP:
      provider: qwen
      model: qwen-plus
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1

  quota:
    FREE:
      daily-chat-limit: 10
    VIP:
      daily-chat-limit: -1    # 不限
    SVIP:
      daily-chat-limit: -1

  llm:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
    qwen:
      api-key: ${QWEN_API_KEY}
```

### ModelResolver 行为

1. 根据 `userId` 查 `users.subscription_tier`
2. 返回对应 `ChatModel` bean（DeepSeek 或 Qwen）
3. `AgentChatService.chat()` / `structuredOutput()` 统一经 ModelResolver 获取模型

### QuotaService 行为

- 每次 `POST /api/chat` 前检查
- FREE：查 `quota_usage` 当日 `chat_count`，超限抛 `QuotaExceededException`（HTTP 429）
- VIP/SVIP：`daily-chat-limit: -1` 跳过检查
- 成功后 `chat_count++`

---

## 10. TickTick 执行策略

`TickTickPlanExecutor` 执行顺序：

1. `createProject(plan.projectName())` → 获得 `projectId`
2. 遍历 `milestones`：
   - 为每个 milestone 创建父任务（`title = milestone.name`）
   - 遍历 milestone.tasks，创建子任务（`parentId = 父任务 id`）
   - 若 task 含 `checklistItems`，设置 `kind=CHECKLIST` + `items`
3. 映射 `TaskDsl.priority`（字符串）→ TickTick 整数（0/1/3/5）
4. 映射 `TaskDsl.dueDate` → ISO datetime（`isAllDay=true`）
5. 全部成功 → 更新 `goals.ticktick_project_id`，写 `plan_executions`
6. 中途失败 → 记录已创建的 refs，phase=FAILED，支持后续重试（MVP：手动重试 execute）

---

## 11. 缓存策略

- **Key**：`conversation:{userId}:{goalId}`
- **Value**：最近 20 条消息 JSON
- **TTL**：24 小时
- **写入**：appendMessage 时写 PG + Redis
- **读取**：优先 Redis，miss 时读 PG 并回填

---

## 12. 错误处理

| 场景 | HTTP | 处理 |
|------|------|------|
| FREE 配额超限 | 429 | `QuotaExceededException` |
| TickTick token 未绑定 | 400 | 提示先绑定 token |
| TickTick API 失败 | 502 | 记录 error，phase=FAILED |
| LLM 调用失败 | 503 | 重试 1 次后失败 |
| Goal phase 不匹配 | 409 | 如 COLLECTING 时调 execute |
| 用户不存在 | 404 | |

全局 `@RestControllerAdvice` 统一响应格式：

```json
{ "error": "QUOTA_EXCEEDED", "message": "..." }
```

---

## 13. 可观测性（MVP 基础）

- Spring Boot Actuator：`/actuator/health`、`/actuator/info`
- Micrometer 自定义指标：
  - `tickmine.llm.calls`（tag: tier, provider）
  - `tickmine.llm.duration`
  - `tickmine.ticktick.calls`
  - `tickmine.ticktick.failures`
- 日志：结构化 JSON（可选），关键步骤 INFO

---

## 14. 部署

### docker-compose.yml 服务

| 服务 | 镜像 | 端口 |
|------|------|------|
| app | 自建 Dockerfile | 8080 |
| postgres | postgres:16 | 5432 |
| redis | redis:7-alpine | 6379 |

### 环境变量

- `DEEPSEEK_API_KEY`
- `QWEN_API_KEY`
- `TICKMINE_ENCRYPTION_KEY`
- `SPRING_DATASOURCE_URL` / `SPRING_DATA_REDIS_HOST`

### Dockerfile

多阶段构建：Maven build → JRE 21 slim 运行 `agent-boot` jar。

---

## 15. 测试策略

| 层级 | 范围 |
|------|------|
| 单元测试 | ModelResolver、QuotaService、PlanDsl 映射、加密工具 |
| 集成测试 | Flyway 迁移、JPA Repository（Testcontainers PG） |
| 契约测试 | TickTickClient mock，验证 Executor 调用顺序 |
| 手动测试 | docker-compose 起全栈，婚礼策划示例走通 |

---

## 16. 包结构（各模块）

```
agent-api/
  com.tickmine.api.controller.*
  com.tickmine.api.dto.*
  com.tickmine.api.exception.*

agent-domain/
  com.tickmine.domain.model.*
  com.tickmine.domain.service.*
  com.tickmine.domain.port.*        # Planner, PlanExecutor, TickTickClient 接口

agent-planner/
  com.tickmine.planner.LlmPlanner
  com.tickmine.planner.GoalAnalyzer

agent-executor/
  com.tickmine.executor.TickTickPlanExecutor

agent-llm/
  com.tickmine.llm.AgentChatService
  com.tickmine.llm.ModelResolver
  com.tickmine.llm.config.*

agent-mcp/
  com.tickmine.mcp.TickTickClientImpl
  com.tickmine.mcp.dto.*

agent-infra/
  com.tickmine.infra.persistence.entity.*
  com.tickmine.infra.persistence.repository.*
  com.tickmine.infra.redis.*
  com.tickmine.infra.crypto.TokenEncryptor

agent-boot/
  com.tickmine.TickMineApplication
  resources/application.yml
  resources/prompts/*.st
  resources/db/migration/V1__*.sql
```

---

## 17. 实现顺序建议

1. Maven 多模块骨架 + agent-boot 可启动
2. Flyway 迁移 + JPA Entity/Repository
3. agent-llm（双 Provider + ModelResolver）
4. agent-mcp（TickTickClient HTTP + mock 测试）
5. agent-planner（GoalAnalyzer + LlmPlanner + prompts）
6. agent-executor（TickTickPlanExecutor）
7. agent-domain（GoalAgentService 串联状态机）
8. agent-api（REST 端点）
9. Redis 会话缓存 + QuotaService
10. Docker Compose + 端到端验证

---

## 18. 后期扩展（非 MVP）

- OAuth 授权绑定 TickTick
- JWT 鉴权 + TenantFilter
- GoalReviewScheduler
- OpenTelemetry 分布式追踪
- 异步执行 + 执行进度 WebSocket
- OAuth 多模型计费统计
