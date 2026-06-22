# TickMine 统一 Agent 架构设计

> 日期：2026-06-23  
> 状态：已批准实施（方案 1）

## 目标

用**单一对话 Agent + Tools** 替换「意图分类 → 固定流水线」，使大模型：

1. 基于**完整对话**理解用户真实意图
2. **充分知晓**滴答清单（TickTick Open API）全部能力及使用边界
3. 按需调用工具，而非走预设 QUERY / PLAN / CHAT 路径
4. 不确定时先澄清，规划范围与用户描述一致，避免过度发挥

## 非目标

- 不在对话中直接写入滴答（仍由用户点击「确认写入」触发 `executePlan`）
- 不引入 MCP 协议服务器（现有 `agent-mcp` 仍为 REST 客户端）
- 不改动前端 SSE / PlanCard 契约

## 架构

```
用户消息 + 完整 history
        ↓
TickMineAgentOrchestrator（单一 agent.st System Prompt）
        ↓ 按需 tool call
┌───────────────────────────────────────────────────┐
│ query_tasks          查待办（自然语言）              │
│ list_ticktick_projects  列出清单/项目              │
│ get_ticktick_project_tasks  某项目下任务详情         │
│ propose_plan         生成计划草案 → PLAN_READY      │
└───────────────────────────────────────────────────┘
        ↓
自然语言回复 + 可选 PlanDsl（前端 PlanCard）
        ↓ 用户确认
executePlan → TickTickPlanExecutor（create_project / create_task）
```

### 能力边界（写进 System Prompt）

**对话中可调用的工具（读 + 草案）：**

| 工具 | 对应 API | 说明 |
|------|----------|------|
| `query_tasks` | 聚合 inbox + 全项目 | 回答「今天有什么待办」等 |
| `list_ticktick_projects` | `GET /project` | 列出清单 |
| `get_ticktick_project_tasks` | `GET /project/{id}/data` | 某清单任务 |
| `propose_plan` | 内部分析 + `PlanDsl` | 生成待确认计划，**不写入** |

**仅用户确认后可执行（不在 Agent tool 中暴露）：**

- `create_project` / `create_task` / `update_task` → `TickTickPlanExecutor`

## 模块职责

| 模块 | 变更 |
|------|------|
| `agent-domain` | 新增 `AgentOrchestrator` 端口、`AgentRunRequest` / `AgentRunResult` |
| `agent-planner` | `TickMineAgentOrchestrator`、`TickMineAgentTools`、`agent.st`；移除流水线入口依赖 |
| `agent-infra` | `GoalAgentService` 精简为：校验 → 会话 → 调用 Orchestrator → 持久化 |
| `agent-llm` | 无结构性变更，Orchestrator 复用 `ModelResolver` |

## Goal 与会话

- 有 `goalId` → 继续该会话
- 无 `goalId` → 新建 Goal（`CHAT` 阶段）
- Agent 调用 `propose_plan` 成功后 → `PLAN_READY` + 保存 `PlanDsl`
- 其余回复 → 保持/设为 `CHAT`

## 附件

有附件时：Orchestrator 先用多模态提取可见内容，注入上下文后再进入 Agent 循环（与是否调用 `propose_plan` 由模型决定）。

## 废弃

- `LlmIntentClassifier` 正则规则（类保留但不再注入 `GoalAgentService`）
- `prepareQuery` / `prepareFreeChat` / `preparePlanning` 分支
- `follow-up.st` 追问流水线（Agent 自行澄清）

## 测试

- 集成测试改为 mock `AgentOrchestrator`
- 新增 `TickMineAgentTools` 单元测试（可选）
