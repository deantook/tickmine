# TickMine Web UI 设计文档

> 基于 brainstorming 确认的设计规格，视觉参考 [Toka](https://tokaweb-dp51981toco5.edgeone.dev/)。
> 日期：2026-06-22

## 1. 背景与目标

为已实现的 TickMine Agent 后端（`tickmine-api/agent-server`）构建 Web 端产品界面，让用户通过自然语言对话澄清目标、预览执行计划，并一键写入滴答清单。

### 目标

1. 首次打开完成 onboarding（userId + TickTick Token）
2. 单会话对话：多轮澄清 → 内嵌计划卡片 → 确认执行
3. 视觉风格参考 Toka：深色、克制、对话为核心
4. localStorage 持久化会话，刷新可恢复

### 非目标（MVP）

- 营销落地页、SEO
- 侧边栏会话历史
- 服务端拉取对话历史 API
- LLM 流式输出
- JWT / 密码登录
- 配额 UI 展示
- 多语言 i18n
- 移动端专门适配

---

## 2. 关键决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| MVP 范围 | 仅产品界面 | 不做营销页，快速闭环 |
| 用户身份 | localStorage userId（自动生成或手动输入） | 对齐后端 MVP 无 JWT |
| 技术栈 | React + Vite + TS + Tailwind + shadcn/ui | 轻量 SPA，对接 REST 直接 |
| 架构方案 | Zustand + React Router + fetch | MVP 体量小，持久化简单 |
| 会话历史 | 单会话，无侧边栏 | 降低 MVP 复杂度 |
| 计划确认 | 对话内 PlanCard + 按钮 execute | 结构清晰，一步确认 |
| Onboarding | 强制先绑 Token 再进对话 | 用户选择 |
| 会话持久化 | localStorage 存 goalId + messages | 刷新可恢复；无后端 history API |
| 新对话 | 顶栏按钮清空 goalId 与 messages | 无历史列表但可开新目标 |
| 配额 | MVP 不展示 | 429 时 Toast 提示即可 |
| 开发代理 | Vite proxy → `:8080` | 后端暂无 CORS |

---

## 3. 页面流与信息架构

```
首次访问 → /onboarding
  Step ① 确认/生成 userId → localStorage
  Step ② 填入 TickTick Token → PUT /api/users/{userId}/ticktick-token
完成 → /chat（主界面）

/chat 顶栏：
  · 品牌名 TickMine
  · Token 连接状态点（GET .../ticktick-token/status）
  · 「新对话」按钮
  · 设置入口（修改 Token，复用 TokenStep 表单）

/settings → 需已有 userId；Token 表单与 onboarding Step ② 相同
```

### 路由守卫

- `/`：未完成 onboarding → 重定向 `/onboarding`；已完成 → `/chat`
- `/chat`：未完成 onboarding → `/onboarding`
- `/settings`：无 userId → `/onboarding`

---

## 4. 布局与视觉规范

参考 Toka「界面克制干净，专注对话本身」：

| 维度 | 规范 |
|------|------|
| 主题 | 深色（`zinc-950` 背景，`zinc-100` 正文） |
| 强调色 | `emerald-500`（主按钮、连接状态、用户气泡 accent） |
| 字体 | Inter / 系统 sans-serif |
| 圆角 | 气泡 `rounded-2xl`，卡片 `rounded-xl` |
| 布局 | 桌面优先，主内容 max-width ~768px 居中 |
| 结构 | 顶栏 48px 固定；消息区 flex-1 scroll；输入区 sticky 底部 |

发送消息后显示 muted 文案「Agent 正在思考…」。MVP 不做 Toka 四角色切换 UI。

---

## 5. 前端工程结构

```
tickmine-web/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── routes/
│   │   ├── OnboardingPage.tsx
│   │   ├── ChatPage.tsx
│   │   └── SettingsPage.tsx
│   ├── components/
│   │   ├── layout/AppHeader.tsx
│   │   ├── chat/
│   │   │   ├── MessageList.tsx
│   │   │   ├── MessageBubble.tsx
│   │   │   ├── ChatInput.tsx
│   │   │   ├── PlanCard.tsx
│   │   │   └── ExecutionResult.tsx
│   │   └── onboarding/
│   │       ├── UserIdStep.tsx
│   │       └── TokenStep.tsx
│   ├── stores/sessionStore.ts    # Zustand + persist
│   ├── api/
│   │   ├── client.ts
│   │   └── types.ts
│   └── hooks/
│       ├── useChat.ts
│       └── useTokenStatus.ts
├── vite.config.ts                # dev proxy
└── components.json               # shadcn
```

### shadcn/ui 组件

`Button`、`Input`、`Label`、`Card`、`Dialog`、`Sonner`（toast）、`Loader2` icon。

---

## 6. 状态模型

```typescript
interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  plan?: PlanDsl | null;       // PLAN_READY 时挂载
  executed?: ExecutionResult; // execute 成功后填充
}

interface SessionState {
  userId: string | null;
  onboardingComplete: boolean;
  currentGoalId: string | null;
  messages: ChatMessage[];
  isLoading: boolean;
}
// persist key: "tickmine-session"
```

### 持久化与恢复

- **全部 persist**：`userId`、`onboardingComplete`、`currentGoalId`、`messages`
- **新对话**：清空 `currentGoalId` + `messages`
- **页面加载**：若有 `currentGoalId`，调 `GET /api/goals/{id}` 同步 `phase` / `latestPlan`（补全 PlanCard，不重建历史气泡）
- **注意**：清 localStorage 后消息不可从服务端恢复（后端无 history API）

---

## 7. 对话与计划流程

```
用户发消息 → POST /api/chat { userId, message, goalId? }
  ├─ 追加 user bubble
  ├─ COLLECTING → assistant bubble（reply 文本）
  └─ PLAN_READY → assistant bubble（reply + PlanCard）
                    PlanCard: projectName → milestones → tasks
                    「确认写入滴答」→ POST /api/goals/{id}/execute
                    成功 → inline ExecutionResult，按钮禁用
                    失败 → Toast errorMessage
```

- 首次响应返回 `goalId` 后写入 store；后续消息携带同一 `goalId`
- 可选：`POST /api/goals/{id}/plan` 重新规划（V1 可不做 UI 入口，留 API 层）

---

## 8. API 对接

| 操作 | 方法 | 路径 |
|------|------|------|
| 绑 Token | PUT | `/api/users/{userId}/ticktick-token` |
| Token 状态 | GET | `/api/users/{userId}/ticktick-token/status` |
| 发消息 | POST | `/api/chat` |
| 同步 goal | GET | `/api/goals/{id}` |
| 确认执行 | POST | `/api/goals/{id}/execute` |

### TypeScript 类型

```typescript
interface ChatRequest {
  userId: string;
  message: string;
  goalId?: string;
}

interface ChatResponse {
  goalId: string;
  phase: 'COLLECTING' | 'PLAN_READY' | 'EXECUTING' | string;
  reply: string;
  plan: PlanDsl | null;
  missingFields: string[] | null;
}

interface PlanDsl {
  projectName: string;
  milestones: { name: string; tasks: TaskDsl[] }[];
}

interface TaskDsl {
  title: string;
  description?: string;
  priority?: string;
  dueDate?: string;
  checklistItems?: { title: string }[];
}

interface ExecutionResult {
  success: boolean;
  projectId: string;
  taskIds: string[];
  errorMessage: string;
}
```

### 开发与部署

**开发**：Vite proxy 避免 CORS

```typescript
// vite.config.ts
server: { proxy: { '/api': 'http://localhost:8080' } }
```

**生产**：后端加 CORS 配置，或 Nginx 同域反代前后端。

### 错误处理

| HTTP | 场景 | UI |
|------|------|-----|
| 429 | FREE 配额用尽 | Toast「今日对话次数已用完」 |
| 400 | Token 未绑定就 execute | Toast + 跳转设置 |
| 404 | goalId 失效 | 清空 goalId，提示开新对话 |
| 5xx / 网络 | 请求失败 | Toast，输入保留可重试 |

---

## 9. Onboarding 细节

**Step ① 用户标识**
- 默认 `crypto.randomUUID()`
- 允许手动编辑
- 文案：「ID 保存在本浏览器，用于识别你的账户」

**Step ② 滴答 Token**
- placeholder：`dp_...`
- 折叠说明：滴答清单 → 设置 → 账户与安全 → API 口令管理（参考 Toka 文案）
- 「连接并进入」→ bind API → `onboardingComplete = true` → `/chat`

`bindTickTickToken` 会 `findOrCreate` 用户，无需单独注册 API。

---

## 10. 测试与验证

### 手动测试路径

1. 首次打开 → onboarding 两步
2. 发送目标描述 → 多轮 COLLECTING 追问
3. 信息完整 → PLAN_READY + PlanCard
4. 确认写入 → execute 成功展示 projectId
5. 刷新页面 → 会话恢复
6. 新对话 → 清空并开始新 goal

### 自动化（建议）

- Vitest：`sessionStore` persist、「新对话」清空
- mock fetch：`useChat` 对 429/400 分支

---

## 11. 后续迭代（V2）

1. 侧边栏 goal 列表 + `GET /api/users/{userId}/goals`
2. 服务端对话历史 API + 刷新完整恢复
3. LLM SSE 流式输出
4. 配额/订阅档位 UI
5. 营销落地页
6. Docker Compose 一键启前后端 + 正式 CORS

---

## 12. 方案备选记录

| 方案 | 简述 | 未选原因 |
|------|------|----------|
| TanStack Query + Zustand | Query 管服务端缓存 | MVP 端点少，过度设计 |
| 无路由状态机 | 单 App 切换视图 | 扩展 settings/营销页困难 |
| localStorage goal 列表 | 不改后端做侧边栏 | 用户选 C，MVP 不做历史 |
| 对话式确认 execute | 输入「确认执行」 | 用户选 A，按钮更直观 |
