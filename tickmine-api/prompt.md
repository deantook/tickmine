

Cursor Prompt

你是一位资深 Java 架构师，请帮我开发一个基于 Spring Boot + Spring AI + MCP 的 Agent 后端项目。

项目目标

构建一个任务管理 Agent。

Agent 可以：

1. 与用户进行多轮对话
2. 澄清用户目标
3. 生成执行计划
4. 拆解任务
5. 调用 TickTick MCP 创建项目和任务
6. 后续持续维护任务状态

示例：

用户：

我要策划一场婚礼

Agent：

请问：
1. 婚礼什么时候举办？
2. 在哪个城市？
3. 预算是多少？
4. 预计多少宾客？

收集信息后生成：

婚礼筹备
├── 场地确定
├── 婚庆供应商
├── 宾客管理
└── 婚礼执行

然后调用 MCP 创建对应项目和任务。

⸻

技术栈

必须使用：

Java 21
Spring Boot 3.5.x
Spring AI
PostgreSQL
Redis
Spring Data JPA
Flyway
Lombok
Maven

项目结构采用 DDD + Agent 分层。

⸻

系统架构

采用：

User
 ↓
Conversation Agent
 ↓
Planner
 ↓
Plan DSL
 ↓
Executor
 ↓
TickTick MCP

禁止：

User
 ↓
LLM
 ↓
直接几十次MCP调用

必须先生成 Plan DSL。

再由 Executor 执行。

⸻

Maven模块

生成多模块工程：

agent-server
├── agent-api
├── agent-domain
├── agent-planner
├── agent-executor
├── agent-llm
├── agent-mcp
├── agent-infra
└── agent-boot

⸻

核心领域模型

Goal

public class Goal {
    UUID id;
    String title;
    String description;
    GoalStatus status;
    LocalDate targetDate;
}

⸻

GoalContext

public class GoalContext {
    Map<String,Object> attributes;
}

用于保存：

{
  "city":"上海",
  "budget":200000,
  "guestCount":150
}

⸻

PlanDsl

public record PlanDsl(
        String projectName,
        List<MilestoneDsl> milestones
) {}

⸻

MilestoneDsl

public record MilestoneDsl(
        String name,
        List<TaskDsl> tasks
) {}

⸻

TaskDsl

public record TaskDsl(
        String title,
        String description,
        String priority,
        LocalDate dueDate
) {}

⸻

数据库设计

PostgreSQL

使用 Flyway 管理。

创建表：

goals

id uuid primary key
title varchar(255)
description text
status varchar(50)
target_date date
created_at timestamp
updated_at timestamp

⸻

conversations

id uuid primary key
user_id varchar(100)
messages jsonb
created_at timestamp
updated_at timestamp

⸻

plans

id uuid primary key
goal_id uuid
dsl jsonb
created_at timestamp

⸻

JPA

使用：

Spring Data JPA
Hibernate

Plan DSL 使用 jsonb 保存。

示例：

@JdbcTypeCode(SqlTypes.JSON)
private PlanDsl dsl;

⸻

Planner模块

创建接口：

public interface Planner {
    PlanDsl generatePlan(
            Goal goal,
            GoalContext context
    );
}

⸻

实现：

public class LlmPlanner

职责：

1. 分析目标
2. 识别缺失信息
3. 生成计划
4. 输出 PlanDsl

使用 Spring AI Structured Output。

不要返回字符串 JSON。

直接映射：

PlanDsl.class

⸻

Executor模块

创建接口：

public interface PlanExecutor {
    void execute(PlanDsl plan);
}

⸻

实现：

TickTickPlanExecutor

职责：

调用 MCP：

create_project
create_task
create_checklist

根据 PlanDsl 创建项目和任务。

⸻

MCP模块

创建：

TickTickMcpClient

封装：

createProject()
createTask()
updateTask()
listTasks()
createChecklist()

不要把 MCP 调用散落在业务代码中。

统一由 Client 管理。

⸻

LLM模块

创建：

AgentChatService

封装 Spring AI ChatClient。

提供：

String chat(...)
<T> T structuredOutput(...)

⸻

Conversation模块

支持：

appendMessage()
loadHistory()
saveHistory()

会话记录存 PostgreSQL。

最近对话缓存 Redis。

⸻

Scheduler

创建：

GoalReviewScheduler

使用：

@Scheduled

每天执行：

检查未完成目标
检查即将到期任务
生成提醒建议

⸻

REST API

生成以下接口：

创建目标

POST /api/goals

⸻

获取目标

GET /api/goals/{id}

⸻

为目标生成计划

POST /api/goals/{id}/plan

⸻

执行计划

POST /api/goals/{id}/execute

⸻

对话

POST /api/chat

请求：

{
  "userId":"u1",
  "message":"我要策划婚礼"
}

⸻

Agent流程

实现：

GoalAgentService

流程：

用户输入
 ↓
识别目标
 ↓
判断信息是否完整
 ↓
如果缺失
    返回追问
 ↓
如果完整
    Planner生成PlanDsl
 ↓
保存Plan
 ↓
返回计划预览
 ↓
用户确认
 ↓
Executor执行
 ↓
调用MCP

⸻

Prompt管理

不要硬编码 Prompt。

创建：

resources/prompts/

目录：

planner.st
goal-analyzer.st
follow-up.st
review.st

通过 Spring AI PromptTemplate 加载。

⸻

可观测性

集成：

Micrometer
OpenTelemetry
Actuator

统计：

LLM调用次数
LLM耗时
MCP调用次数
MCP失败率

⸻

输出要求

请直接生成：

1. 完整 Maven 多模块结构
2. 所有 package 结构
3. application.yml
4. pom.xml
5. Flyway SQL
6. Entity
7. Repository
8. Service
9. Controller
10. Planner实现
11. Executor实现
12. MCP Client接口
13. Spring AI配置
14. Redis配置
15. Dockerfile
16. docker-compose.yml

要求项目启动后可运行，并遵循生产级代码规范。