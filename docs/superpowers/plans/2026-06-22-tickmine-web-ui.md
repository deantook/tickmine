# TickMine Web UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `tickmine-web/` 构建 MVP 产品界面——onboarding 绑 Token、单会话对话、内嵌计划卡片确认写入滴答，视觉参考 Toka 深色克制风格。

**Architecture:** React SPA（Vite + React Router），Zustand persist 管理 userId/goalId/messages，fetch 封装对接现有 REST API，开发期 Vite proxy 绕过 CORS。

**Tech Stack:** React 19, Vite 6, TypeScript, Tailwind CSS 4, shadcn/ui, Zustand, React Router 7, Vitest, Sonner

**Spec:** `docs/superpowers/specs/2026-06-22-tickmine-web-ui-design.md`

---

## File Structure Overview

```
tickmine-web/
├── index.html
├── package.json
├── vite.config.ts                  # /api → localhost:8080
├── vitest.config.ts
├── tsconfig.json
├── components.json                 # shadcn
├── src/
│   ├── main.tsx
│   ├── App.tsx                     # Router + Toaster
│   ├── index.css                   # Tailwind + dark zinc theme
│   ├── api/
│   │   ├── types.ts                # DTO 类型
│   │   ├── client.ts               # fetch + ApiError
│   │   └── endpoints.ts            # bindToken, chat, getGoal, execute
│   ├── stores/
│   │   └── sessionStore.ts         # Zustand + persist
│   ├── hooks/
│   │   ├── useChat.ts
│   │   └── useTokenStatus.ts
│   ├── routes/
│   │   ├── RootRedirect.tsx
│   │   ├── OnboardingPage.tsx
│   │   ├── ChatPage.tsx
│   │   └── SettingsPage.tsx
│   ├── components/
│   │   ├── layout/AppHeader.tsx
│   │   ├── onboarding/UserIdStep.tsx
│   │   ├── onboarding/TokenStep.tsx
│   │   ├── chat/MessageList.tsx
│   │   ├── chat/MessageBubble.tsx
│   │   ├── chat/ChatInput.tsx
│   │   ├── chat/PlanCard.tsx
│   │   └── chat/ExecutionResultBanner.tsx
│   └── lib/utils.ts                # shadcn cn()
├── src/stores/sessionStore.test.ts
└── src/hooks/useChat.test.ts
```

---

### Task 1: Vite 项目脚手架

**Files:**
- Create: `tickmine-web/`（整个目录）

- [ ] **Step 1: 创建 Vite React TS 项目**

```bash
cd /Users/dean/code/tickmine
npm create vite@latest tickmine-web -- --template react-ts
cd tickmine-web
npm install
```

Expected: `tickmine-web/` 目录存在，`npm run dev` 可启动（暂不验证 UI）

- [ ] **Step 2: 安装运行时依赖**

```bash
cd /Users/dean/code/tickmine/tickmine-web
npm install react-router-dom zustand sonner lucide-react clsx tailwind-merge class-variance-authority
npm install -D tailwindcss @tailwindcss/vite vitest @testing-library/react @testing-library/jest-dom jsdom
```

- [ ] **Step 3: 配置 Vite proxy 与 Vitest**

Replace `tickmine-web/vite.config.ts`:

```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
});
```

Create `tickmine-web/src/test/setup.ts`:

```typescript
import '@testing-library/jest-dom/vitest';
```

Add to `tickmine-web/package.json` scripts:

```json
"test": "vitest run",
"test:watch": "vitest"
```

Update `tickmine-web/tsconfig.app.json` — add to `compilerOptions`:

```json
"baseUrl": ".",
"paths": { "@/*": ["./src/*"] }
```

- [ ] **Step 4: 配置 Tailwind 深色主题**

Replace `tickmine-web/src/index.css`:

```css
@import "tailwindcss";

@theme inline {
  --color-background: oklch(0.09 0 0);
  --color-foreground: oklch(0.95 0 0);
  --color-card: oklch(0.12 0 0);
  --color-card-foreground: oklch(0.95 0 0);
  --color-primary: oklch(0.7 0.17 162);
  --color-primary-foreground: oklch(0.1 0 0);
  --color-muted: oklch(0.2 0 0);
  --color-muted-foreground: oklch(0.65 0 0);
  --color-border: oklch(0.22 0 0);
  --radius-lg: 0.75rem;
}

body {
  @apply bg-background text-foreground antialiased min-h-screen;
}
```

Create `tickmine-web/src/lib/utils.ts`:

```typescript
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

- [ ] **Step 5: Commit**

```bash
cd /Users/dean/code/tickmine
git add tickmine-web/
git commit -m "feat(web): scaffold Vite React project with Tailwind and proxy"
```

---

### Task 2: shadcn/ui 基础组件

**Files:**
- Create: `tickmine-web/components.json`
- Create: `tickmine-web/src/components/ui/*.tsx`

- [ ] **Step 1: 初始化 shadcn**

```bash
cd /Users/dean/code/tickmine/tickmine-web
npx shadcn@latest init -y --defaults
```

若交互式失败，手动创建 `tickmine-web/components.json`:

```json
{
  "$schema": "https://ui.shadcn.com/schema.json",
  "style": "new-york",
  "rsc": false,
  "tsx": true,
  "tailwind": {
    "config": "",
    "css": "src/index.css",
    "baseColor": "zinc",
    "cssVariables": true
  },
  "aliases": {
    "components": "@/components",
    "utils": "@/lib/utils",
    "ui": "@/components/ui",
    "lib": "@/lib",
    "hooks": "@/hooks"
  },
  "iconLibrary": "lucide"
}
```

- [ ] **Step 2: 添加 UI 组件**

```bash
cd /Users/dean/code/tickmine/tickmine-web
npx shadcn@latest add button input label card sonner -y
```

- [ ] **Step 3: 在 main.tsx 挂载 Toaster**

Replace `tickmine-web/src/main.tsx`:

```tsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Toaster } from '@/components/ui/sonner';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
    <Toaster theme="dark" position="top-center" />
  </StrictMode>,
);
```

- [ ] **Step 4: Commit**

```bash
git add tickmine-web/
git commit -m "feat(web): add shadcn ui components and toaster"
```

---

### Task 3: API 类型与客户端

**Files:**
- Create: `tickmine-web/src/api/types.ts`
- Create: `tickmine-web/src/api/client.ts`
- Create: `tickmine-web/src/api/endpoints.ts`

- [ ] **Step 1: 定义 types.ts**

Create `tickmine-web/src/api/types.ts`:

```typescript
export interface ErrorResponse {
  error: string;
  message: string;
}

export interface ChatRequest {
  userId: string;
  message: string;
  goalId?: string;
}

export interface ChatResponse {
  goalId: string;
  phase: string;
  reply: string;
  plan: PlanDsl | null;
  missingFields: string[] | null;
}

export interface PlanDsl {
  projectName: string;
  milestones: MilestoneDsl[];
}

export interface MilestoneDsl {
  name: string;
  tasks: TaskDsl[];
}

export interface TaskDsl {
  title: string;
  description?: string;
  priority?: string;
  dueDate?: string;
  checklistItems?: { title: string }[];
}

export interface ExecutionResult {
  success: boolean;
  projectId: string;
  taskIds: string[];
  errorMessage: string;
}

export interface GoalResponse {
  id: string;
  userId: string;
  title: string;
  description: string;
  phase: string;
  status: string;
  latestPlan: PlanDsl | null;
  ticktickProjectId: string | null;
}

export interface TokenStatusResponse {
  connected: boolean;
}

export interface BindTokenRequest {
  token: string;
}

export interface BindTokenResponse {
  status: string;
}
```

- [ ] **Step 2: 实现 client.ts**

Create `tickmine-web/src/api/client.ts`:

```typescript
import type { ErrorResponse } from './types';

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function parseError(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as ErrorResponse;
    return new ApiError(res.status, body.error, body.message);
  } catch {
    return new ApiError(res.status, 'UNKNOWN', res.statusText);
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  });
  if (!res.ok) throw await parseError(res);
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}
```

- [ ] **Step 3: 实现 endpoints.ts**

Create `tickmine-web/src/api/endpoints.ts`:

```typescript
import { apiFetch } from './client';
import type {
  BindTokenRequest,
  BindTokenResponse,
  ChatRequest,
  ChatResponse,
  ExecutionResult,
  GoalResponse,
  TokenStatusResponse,
} from './types';

export function bindTickTickToken(userId: string, token: string) {
  return apiFetch<BindTokenResponse>(`/api/users/${userId}/ticktick-token`, {
    method: 'PUT',
    body: JSON.stringify({ token } satisfies BindTokenRequest),
  });
}

export function getTokenStatus(userId: string) {
  return apiFetch<TokenStatusResponse>(`/api/users/${userId}/ticktick-token/status`);
}

export function sendChat(req: ChatRequest) {
  return apiFetch<ChatResponse>('/api/chat', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export function getGoal(goalId: string) {
  return apiFetch<GoalResponse>(`/api/goals/${goalId}`);
}

export function executeGoal(goalId: string) {
  return apiFetch<ExecutionResult>(`/api/goals/${goalId}/execute`, {
    method: 'POST',
  });
}
```

- [ ] **Step 4: Commit**

```bash
git add tickmine-web/src/api/
git commit -m "feat(web): add API types and fetch client"
```

---

### Task 4: Session Store（TDD）

**Files:**
- Create: `tickmine-web/src/stores/sessionStore.ts`
- Create: `tickmine-web/src/stores/sessionStore.test.ts`

- [ ] **Step 1: 写失败测试**

Create `tickmine-web/src/stores/sessionStore.test.ts`:

```typescript
import { beforeEach, describe, expect, it } from 'vitest';
import { useSessionStore } from './sessionStore';

describe('sessionStore', () => {
  beforeEach(() => {
    localStorage.clear();
    useSessionStore.setState({
      userId: null,
      onboardingComplete: false,
      currentGoalId: null,
      messages: [],
      isLoading: false,
    });
  });

  it('setUserId stores userId', () => {
    useSessionStore.getState().setUserId('user-abc');
    expect(useSessionStore.getState().userId).toBe('user-abc');
  });

  it('startNewChat clears goal and messages', () => {
    useSessionStore.setState({
      currentGoalId: 'goal-1',
      messages: [{ role: 'user', content: 'hello' }],
    });
    useSessionStore.getState().startNewChat();
    expect(useSessionStore.getState().currentGoalId).toBeNull();
    expect(useSessionStore.getState().messages).toEqual([]);
  });

  it('appendMessage adds to messages array', () => {
    useSessionStore.getState().appendMessage({ role: 'user', content: 'test' });
    expect(useSessionStore.getState().messages).toHaveLength(1);
    expect(useSessionStore.getState().messages[0].content).toBe('test');
  });

  it('completeOnboarding sets flag', () => {
    useSessionStore.getState().completeOnboarding();
    expect(useSessionStore.getState().onboardingComplete).toBe(true);
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/dean/code/tickmine/tickmine-web && npm test
```

Expected: FAIL — `sessionStore` module not found

- [ ] **Step 3: 实现 sessionStore.ts**

Create `tickmine-web/src/stores/sessionStore.ts`:

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { ExecutionResult, PlanDsl } from '@/api/types';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  plan?: PlanDsl | null;
  executed?: ExecutionResult;
}

interface SessionState {
  userId: string | null;
  onboardingComplete: boolean;
  currentGoalId: string | null;
  messages: ChatMessage[];
  isLoading: boolean;
  setUserId: (userId: string) => void;
  completeOnboarding: () => void;
  setGoalId: (goalId: string) => void;
  startNewChat: () => void;
  appendMessage: (message: ChatMessage) => void;
  updateLastAssistant: (patch: Partial<ChatMessage>) => void;
  setLoading: (loading: boolean) => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set, get) => ({
      userId: null,
      onboardingComplete: false,
      currentGoalId: null,
      messages: [],
      isLoading: false,

      setUserId: (userId) => set({ userId }),
      completeOnboarding: () => set({ onboardingComplete: true }),
      setGoalId: (goalId) => set({ currentGoalId: goalId }),
      startNewChat: () => set({ currentGoalId: null, messages: [], isLoading: false }),
      appendMessage: (message) => set({ messages: [...get().messages, message] }),
      updateLastAssistant: (patch) => {
        const messages = [...get().messages];
        for (let i = messages.length - 1; i >= 0; i--) {
          if (messages[i].role === 'assistant') {
            messages[i] = { ...messages[i], ...patch };
            break;
          }
        }
        set({ messages });
      },
      setLoading: (isLoading) => set({ isLoading }),
    }),
    {
      name: 'tickmine-session',
      partialize: (s) => ({
        userId: s.userId,
        onboardingComplete: s.onboardingComplete,
        currentGoalId: s.currentGoalId,
        messages: s.messages,
      }),
    },
  ),
);
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/dean/code/tickmine/tickmine-web && npm test
```

Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add tickmine-web/src/stores/
git commit -m "feat(web): add persisted session store with tests"
```

---

### Task 5: React Router 与路由守卫

**Files:**
- Create: `tickmine-web/src/routes/RootRedirect.tsx`
- Modify: `tickmine-web/src/App.tsx`

- [ ] **Step 1: 实现 RootRedirect**

Create `tickmine-web/src/routes/RootRedirect.tsx`:

```tsx
import { Navigate } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';

export function RootRedirect() {
  const onboardingComplete = useSessionStore((s) => s.onboardingComplete);
  return <Navigate to={onboardingComplete ? '/chat' : '/onboarding'} replace />;
}
```

- [ ] **Step 2: 配置 App.tsx 路由**

Replace `tickmine-web/src/App.tsx`:

```tsx
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';
import { RootRedirect } from '@/routes/RootRedirect';
import { OnboardingPage } from '@/routes/OnboardingPage';
import { ChatPage } from '@/routes/ChatPage';
import { SettingsPage } from '@/routes/SettingsPage';

function RequireOnboarding({ children }: { children: React.ReactNode }) {
  const done = useSessionStore((s) => s.onboardingComplete);
  if (!done) return <Navigate to="/onboarding" replace />;
  return <>{children}</>;
}

function RequireUserId({ children }: { children: React.ReactNode }) {
  const userId = useSessionStore((s) => s.userId);
  if (!userId) return <Navigate to="/onboarding" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootRedirect />} />
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route
          path="/chat"
          element={
            <RequireOnboarding>
              <ChatPage />
            </RequireOnboarding>
          }
        />
        <Route
          path="/settings"
          element={
            <RequireUserId>
              <SettingsPage />
            </RequireUserId>
          }
        />
        <Route path="*" element={<RootRedirect />} />
      </Routes>
    </BrowserRouter>
  );
}
```

- [ ] **Step 3: 创建占位页面（临时）**

Create minimal placeholders so app compiles:

`tickmine-web/src/routes/OnboardingPage.tsx`:

```tsx
export function OnboardingPage() {
  return <div className="p-8">Onboarding</div>;
}
```

`tickmine-web/src/routes/ChatPage.tsx`:

```tsx
export function ChatPage() {
  return <div className="p-8">Chat</div>;
}
```

`tickmine-web/src/routes/SettingsPage.tsx`:

```tsx
export function SettingsPage() {
  return <div className="p-8">Settings</div>;
}
```

- [ ] **Step 4: 验证编译**

```bash
cd /Users/dean/code/tickmine/tickmine-web && npm run build
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add tickmine-web/src/
git commit -m "feat(web): add router with onboarding guards"
```

---

### Task 6: Onboarding 页面

**Files:**
- Create: `tickmine-web/src/components/onboarding/UserIdStep.tsx`
- Create: `tickmine-web/src/components/onboarding/TokenStep.tsx`
- Modify: `tickmine-web/src/routes/OnboardingPage.tsx`

- [ ] **Step 1: UserIdStep 组件**

Create `tickmine-web/src/components/onboarding/UserIdStep.tsx`:

```tsx
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

interface Props {
  onNext: (userId: string) => void;
}

export function UserIdStep({ onNext }: Props) {
  const [userId, setUserId] = useState(() => crypto.randomUUID());

  return (
    <Card className="w-full max-w-md border-border bg-card">
      <CardHeader>
        <CardTitle>① 用户标识</CardTitle>
        <CardDescription>ID 保存在本浏览器，用于识别你的账户</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="userId">User ID</Label>
          <Input
            id="userId"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            className="font-mono text-sm"
          />
        </div>
        <Button className="w-full" onClick={() => onNext(userId.trim())} disabled={!userId.trim()}>
          下一步
        </Button>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 2: TokenStep 组件**

Create `tickmine-web/src/components/onboarding/TokenStep.tsx`:

```tsx
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { bindTickTickToken } from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';

interface Props {
  userId: string;
  submitLabel?: string;
  onSuccess: () => void;
}

export function TokenStep({ userId, submitLabel = '连接并进入', onSuccess }: Props) {
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [showHelp, setShowHelp] = useState(false);

  async function handleSubmit() {
    if (!token.trim()) return;
    setLoading(true);
    try {
      await bindTickTickToken(userId, token.trim());
      onSuccess();
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : '连接失败，请重试';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="w-full max-w-md border-border bg-card">
      <CardHeader>
        <CardTitle>② 滴答 Token</CardTitle>
        <CardDescription>绑定 API 口令后才能写入滴答清单</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-2">
          <Label htmlFor="token">API 口令</Label>
          <Input
            id="token"
            placeholder="dp_..."
            value={token}
            onChange={(e) => setToken(e.target.value)}
            type="password"
          />
        </div>
        <button
          type="button"
          className="text-sm text-muted-foreground underline"
          onClick={() => setShowHelp(!showHelp)}
        >
          如何获取 API 口令？
        </button>
        {showHelp && (
          <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground">
            <li>打开滴答清单网页版</li>
            <li>进入「设置 → 账户与安全 → API 口令管理」</li>
            <li>复制 API 口令（格式 dp_...）粘贴到上方</li>
          </ol>
        )}
        <Button className="w-full" onClick={handleSubmit} disabled={loading || !token.trim()}>
          {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {submitLabel}
        </Button>
      </CardContent>
    </Card>
  );
}
```

- [ ] **Step 3: OnboardingPage 组装**

Replace `tickmine-web/src/routes/OnboardingPage.tsx`:

```tsx
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';
import { UserIdStep } from '@/components/onboarding/UserIdStep';
import { TokenStep } from '@/components/onboarding/TokenStep';

export function OnboardingPage() {
  const navigate = useNavigate();
  const { onboardingComplete, userId, setUserId, completeOnboarding } = useSessionStore();
  const step = userId ? 2 : 1;

  useEffect(() => {
    if (onboardingComplete) navigate('/chat', { replace: true });
  }, [onboardingComplete, navigate]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center px-4 bg-background">
      <div className="mb-8 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">TickMine</h1>
        <p className="text-muted-foreground mt-1">用说的，管清单</p>
      </div>
      {step === 1 && <UserIdStep onNext={setUserId} />}
      {step === 2 && userId && (
        <TokenStep
          userId={userId}
          onSuccess={() => {
            completeOnboarding();
            navigate('/chat');
          }}
        />
      )}
    </div>
  );
}
```

- [ ] **Step 4: Commit**

```bash
git add tickmine-web/src/components/onboarding/ tickmine-web/src/routes/OnboardingPage.tsx
git commit -m "feat(web): add two-step onboarding flow"
```

---

### Task 7: AppHeader 与 Settings

**Files:**
- Create: `tickmine-web/src/components/layout/AppHeader.tsx`
- Create: `tickmine-web/src/hooks/useTokenStatus.ts`
- Modify: `tickmine-web/src/routes/SettingsPage.tsx`

- [ ] **Step 1: useTokenStatus hook**

Create `tickmine-web/src/hooks/useTokenStatus.ts`:

```typescript
import { useEffect, useState } from 'react';
import { getTokenStatus } from '@/api/endpoints';

export function useTokenStatus(userId: string | null) {
  const [connected, setConnected] = useState<boolean | null>(null);

  useEffect(() => {
    if (!userId) {
      setConnected(null);
      return;
    }
    getTokenStatus(userId)
      .then((r) => setConnected(r.connected))
      .catch(() => setConnected(false));
  }, [userId]);

  return connected;
}
```

- [ ] **Step 2: AppHeader**

Create `tickmine-web/src/components/layout/AppHeader.tsx`:

```tsx
import { Link } from 'react-router-dom';
import { Settings, MessageSquarePlus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useSessionStore } from '@/stores/sessionStore';
import { useTokenStatus } from '@/hooks/useTokenStatus';
import { cn } from '@/lib/utils';

export function AppHeader() {
  const userId = useSessionStore((s) => s.userId);
  const startNewChat = useSessionStore((s) => s.startNewChat);
  const connected = useTokenStatus(userId);

  return (
    <header className="sticky top-0 z-10 flex h-12 items-center justify-between border-b border-border bg-background/95 px-4 backdrop-blur">
      <div className="flex items-center gap-2">
        <span className="font-semibold">TickMine</span>
        <span
          className={cn(
            'h-2 w-2 rounded-full',
            connected === true && 'bg-emerald-500',
            connected === false && 'bg-zinc-500',
            connected === null && 'bg-zinc-700',
          )}
          title={connected ? '已连接滴答' : '未连接'}
        />
      </div>
      <div className="flex items-center gap-1">
        <Button variant="ghost" size="sm" onClick={startNewChat}>
          <MessageSquarePlus className="mr-1 h-4 w-4" />
          新对话
        </Button>
        <Button variant="ghost" size="icon" asChild>
          <Link to="/settings">
            <Settings className="h-4 w-4" />
          </Link>
        </Button>
      </div>
    </header>
  );
}
```

- [ ] **Step 3: SettingsPage**

Replace `tickmine-web/src/routes/SettingsPage.tsx`:

```tsx
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { TokenStep } from '@/components/onboarding/TokenStep';
import { useSessionStore } from '@/stores/sessionStore';
import { toast } from 'sonner';

export function SettingsPage() {
  const navigate = useNavigate();
  const userId = useSessionStore((s) => s.userId)!;

  return (
    <div className="min-h-screen bg-background">
      <div className="mx-auto max-w-md px-4 py-8">
        <Button variant="ghost" size="sm" className="mb-6" onClick={() => navigate('/chat')}>
          <ArrowLeft className="mr-1 h-4 w-4" />
          返回对话
        </Button>
        <TokenStep
          userId={userId}
          submitLabel="保存 Token"
          onSuccess={() => {
            toast.success('Token 已更新');
            navigate('/chat');
          }}
        />
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Commit**

```bash
git add tickmine-web/src/components/layout/ tickmine-web/src/hooks/useTokenStatus.ts tickmine-web/src/routes/SettingsPage.tsx
git commit -m "feat(web): add app header and settings page"
```

---

### Task 8: 对话 UI 组件

**Files:**
- Create: `tickmine-web/src/components/chat/MessageBubble.tsx`
- Create: `tickmine-web/src/components/chat/PlanCard.tsx`
- Create: `tickmine-web/src/components/chat/ExecutionResultBanner.tsx`
- Create: `tickmine-web/src/components/chat/MessageList.tsx`
- Create: `tickmine-web/src/components/chat/ChatInput.tsx`

- [ ] **Step 1: MessageBubble**

Create `tickmine-web/src/components/chat/MessageBubble.tsx`:

```tsx
import type { ChatMessage } from '@/stores/sessionStore';
import { cn } from '@/lib/utils';
import { PlanCard } from './PlanCard';

interface Props {
  message: ChatMessage;
  goalId: string | null;
  onExecute: (goalId: string, messageIndex: number) => void;
  messageIndex: number;
  executing: boolean;
}

export function MessageBubble({ message, goalId, onExecute, messageIndex, executing }: Props) {
  const isUser = message.role === 'user';

  return (
    <div className={cn('flex', isUser ? 'justify-end' : 'justify-start')}>
      <div
        className={cn(
          'max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed',
          isUser
            ? 'bg-emerald-500/10 border border-emerald-500/30 text-foreground'
            : 'bg-card border border-border text-foreground',
        )}
      >
        <p className="whitespace-pre-wrap">{message.content}</p>
        {!isUser && message.plan && goalId && (
          <PlanCard
            plan={message.plan}
            executed={message.executed}
            executing={executing}
            onConfirm={() => onExecute(goalId, messageIndex)}
          />
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: PlanCard + ExecutionResultBanner**

Create `tickmine-web/src/components/chat/ExecutionResultBanner.tsx`:

```tsx
import type { ExecutionResult } from '@/api/types';
import { CheckCircle2, XCircle } from 'lucide-react';

export function ExecutionResultBanner({ result }: { result: ExecutionResult }) {
  if (!result.success) {
    return (
      <div className="mt-3 flex items-start gap-2 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm">
        <XCircle className="h-4 w-4 shrink-0 text-red-400 mt-0.5" />
        <span>{result.errorMessage || '写入失败'}</span>
      </div>
    );
  }
  return (
    <div className="mt-3 flex items-start gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/10 p-3 text-sm">
      <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400 mt-0.5" />
      <span>
        已写入滴答清单 · 项目 ID: <code className="font-mono text-xs">{result.projectId}</code>
        {result.taskIds.length > 0 && ` · ${result.taskIds.length} 个任务`}
      </span>
    </div>
  );
}
```

Create `tickmine-web/src/components/chat/PlanCard.tsx`:

```tsx
import type { PlanDsl, ExecutionResult } from '@/api/types';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import { ExecutionResultBanner } from './ExecutionResultBanner';

interface Props {
  plan: PlanDsl;
  executed?: ExecutionResult;
  executing: boolean;
  onConfirm: () => void;
}

export function PlanCard({ plan, executed, executing, onConfirm }: Props) {
  return (
    <div className="mt-3 rounded-xl border border-border bg-background/50 p-4">
      <p className="font-medium text-foreground">{plan.projectName}</p>
      <ul className="mt-3 space-y-3">
        {plan.milestones.map((m) => (
          <li key={m.name}>
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{m.name}</p>
            <ul className="mt-1 space-y-1 border-l border-border pl-3">
              {m.tasks.map((t) => (
                <li key={t.title} className="text-sm text-foreground/90">
                  {t.title}
                  {t.dueDate && (
                    <span className="ml-2 text-xs text-muted-foreground">{t.dueDate}</span>
                  )}
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
      {executed ? (
        <ExecutionResultBanner result={executed} />
      ) : (
        <Button className="mt-4 w-full" onClick={onConfirm} disabled={executing}>
          {executing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          确认写入滴答
        </Button>
      )}
    </div>
  );
}
```

- [ ] **Step 3: MessageList + ChatInput**

Create `tickmine-web/src/components/chat/MessageList.tsx`:

```tsx
import { useEffect, useRef } from 'react';
import type { ChatMessage } from '@/stores/sessionStore';
import { MessageBubble } from './MessageBubble';

interface Props {
  messages: ChatMessage[];
  goalId: string | null;
  isLoading: boolean;
  executing: boolean;
  onExecute: (goalId: string, messageIndex: number) => void;
}

export function MessageList({ messages, goalId, isLoading, executing, onExecute }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  if (messages.length === 0 && !isLoading) {
    return (
      <div className="flex flex-1 items-center justify-center text-muted-foreground text-sm">
        例如：「帮我把今天的事理一理」或「我要策划一场婚礼」
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto px-4 py-6 space-y-4">
      {messages.map((msg, i) => (
        <MessageBubble
          key={i}
          message={msg}
          messageIndex={i}
          goalId={goalId}
          executing={executing}
          onExecute={onExecute}
        />
      ))}
      {isLoading && (
        <p className="text-center text-sm text-muted-foreground">Agent 正在思考…</p>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
```

Create `tickmine-web/src/components/chat/ChatInput.tsx`:

```tsx
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Send } from 'lucide-react';

interface Props {
  disabled: boolean;
  onSend: (text: string) => void;
}

export function ChatInput({ disabled, onSend }: Props) {
  const [text, setText] = useState('');

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setText('');
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="sticky bottom-0 border-t border-border bg-background px-4 py-3"
    >
      <div className="mx-auto flex max-w-3xl gap-2">
        <input
          value={text}
          onChange={(e) => setText(e.target.value)}
          disabled={disabled}
          placeholder="说说你想完成的事…"
          className="flex-1 rounded-lg border border-border bg-card px-4 py-2.5 text-sm outline-none focus:ring-1 focus:ring-emerald-500/50 disabled:opacity-50"
        />
        <Button type="submit" size="icon" disabled={disabled || !text.trim()}>
          <Send className="h-4 w-4" />
        </Button>
      </div>
    </form>
  );
}
```

- [ ] **Step 4: Commit**

```bash
git add tickmine-web/src/components/chat/
git commit -m "feat(web): add chat message list, plan card, and input"
```

---

### Task 9: useChat Hook（TDD）

**Files:**
- Create: `tickmine-web/src/hooks/useChat.ts`
- Create: `tickmine-web/src/hooks/useChat.test.ts`

- [ ] **Step 1: 写失败测试（mock fetch）**

Create `tickmine-web/src/hooks/useChat.test.ts`:

```typescript
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';

function wrapper({ children }: { children: React.ReactNode }) {
  return <MemoryRouter>{children}</MemoryRouter>;
}

vi.mock('@/api/endpoints', () => ({
  sendChat: vi.fn(),
  executeGoal: vi.fn(),
  getGoal: vi.fn(),
}));

import { sendChat, executeGoal } from '@/api/endpoints';
import { useChat } from './useChat';
import { ApiError } from '@/api/client';
import { toast } from 'sonner';

vi.mock('sonner', () => ({ toast: { error: vi.fn(), success: vi.fn() } }));

describe('useChat', () => {
  beforeEach(() => {
    localStorage.clear();
    useSessionStore.setState({
      userId: 'u1',
      onboardingComplete: true,
      currentGoalId: null,
      messages: [],
      isLoading: false,
    });
    vi.clearAllMocks();
  });

  it('sendMessage appends user and assistant messages on COLLECTING', async () => {
    vi.mocked(sendChat).mockResolvedValue({
      goalId: 'g1',
      phase: 'COLLECTING',
      reply: '请问婚礼什么时候？',
      plan: null,
      missingFields: ['date'],
    });

    const { result } = renderHook(() => useChat(), { wrapper });
    await act(async () => {
      await result.current.sendMessage('我要策划婚礼');
    });

    const { messages, currentGoalId } = useSessionStore.getState();
    expect(currentGoalId).toBe('g1');
    expect(messages).toHaveLength(2);
    expect(messages[0].role).toBe('user');
    expect(messages[1].content).toBe('请问婚礼什么时候？');
  });

  it('sendMessage shows toast on 429', async () => {
    vi.mocked(sendChat).mockRejectedValue(new ApiError(429, 'QUOTA_EXCEEDED', 'limit'));

    const { result } = renderHook(() => useChat(), { wrapper });
    await act(async () => {
      await result.current.sendMessage('hello');
    });

    expect(toast.error).toHaveBeenCalledWith('今日对话次数已用完');
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/dean/code/tickmine/tickmine-web && npm test
```

Expected: FAIL — `useChat` not found

- [ ] **Step 3: 实现 useChat.ts**

Create `tickmine-web/src/hooks/useChat.ts`:

```typescript
import { useCallback, useState } from 'react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { sendChat, executeGoal, getGoal } from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { useSessionStore } from '@/stores/sessionStore';
import { useEffect } from 'react';

export function useChat() {
  const navigate = useNavigate();
  const userId = useSessionStore((s) => s.userId);
  const currentGoalId = useSessionStore((s) => s.currentGoalId);
  const messages = useSessionStore((s) => s.messages);
  const isLoading = useSessionStore((s) => s.isLoading);
  const {
    setGoalId,
    appendMessage,
    setLoading,
    updateLastAssistant,
  } = useSessionStore();
  const [executing, setExecuting] = useState(false);

  // 刷新后同步 goal 状态到已有 assistant 消息上的 plan
  useEffect(() => {
    if (!currentGoalId || messages.length === 0) return;
    getGoal(currentGoalId)
      .then((goal) => {
        if (goal.latestPlan && goal.phase === 'PLAN_READY') {
          updateLastAssistant({ plan: goal.latestPlan });
        }
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 404) {
          useSessionStore.setState({ currentGoalId: null });
          toast.error('会话已失效，请开新对话');
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const sendMessage = useCallback(
    async (text: string) => {
      if (!userId || isLoading) return;
      appendMessage({ role: 'user', content: text });
      setLoading(true);
      try {
        const res = await sendChat({
          userId,
          message: text,
          goalId: currentGoalId ?? undefined,
        });
        setGoalId(res.goalId);
        appendMessage({
          role: 'assistant',
          content: res.reply,
          plan: res.phase === 'PLAN_READY' ? res.plan : undefined,
        });
      } catch (e) {
        if (e instanceof ApiError) {
          if (e.status === 429) toast.error('今日对话次数已用完');
          else if (e.code === 'TICKTICK_NOT_CONNECTED') {
            toast.error('请先绑定滴答 Token');
            navigate('/settings');
          } else toast.error(e.message);
        } else {
          toast.error('发送失败，请重试');
        }
      } finally {
        setLoading(false);
      }
    },
    [userId, currentGoalId, isLoading, appendMessage, setGoalId, setLoading, navigate],
  );

  const handleExecute = useCallback(
    async (goalId: string, messageIndex: number) => {
      setExecuting(true);
      try {
        const result = await executeGoal(goalId);
        const msgs = [...useSessionStore.getState().messages];
        if (msgs[messageIndex]) {
          msgs[messageIndex] = { ...msgs[messageIndex], executed: result };
          useSessionStore.setState({ messages: msgs });
        }
        if (!result.success) toast.error(result.errorMessage || '写入失败');
        else toast.success('已写入滴答清单');
      } catch (e) {
        const msg = e instanceof ApiError ? e.message : '执行失败';
        toast.error(msg);
        if (e instanceof ApiError && e.code === 'TICKTICK_NOT_CONNECTED') {
          navigate('/settings');
        }
      } finally {
        setExecuting(false);
      }
    },
    [navigate],
  );

  return { messages, isLoading, executing, sendMessage, handleExecute, currentGoalId };
}
```

- [ ] **Step 4: 运行测试**

```bash
cd /Users/dean/code/tickmine/tickmine-web && npm test
```

Expected: PASS (all tests)

- [ ] **Step 5: Commit**

```bash
git add tickmine-web/src/hooks/
git commit -m "feat(web): add useChat hook with error handling and tests"
```

---

### Task 10: ChatPage 组装

**Files:**
- Modify: `tickmine-web/src/routes/ChatPage.tsx`

- [ ] **Step 1: 实现 ChatPage**

Replace `tickmine-web/src/routes/ChatPage.tsx`:

```tsx
import { AppHeader } from '@/components/layout/AppHeader';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { useChat } from '@/hooks/useChat';

export function ChatPage() {
  const { messages, isLoading, executing, sendMessage, handleExecute, currentGoalId } = useChat();

  return (
    <div className="flex h-screen flex-col bg-background">
      <AppHeader />
      <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col overflow-hidden">
        <MessageList
          messages={messages}
          goalId={currentGoalId}
          isLoading={isLoading}
          executing={executing}
          onExecute={handleExecute}
        />
        <ChatInput disabled={isLoading || executing} onSend={sendMessage} />
      </main>
    </div>
  );
}
```

- [ ] **Step 2: 更新 index.html title**

In `tickmine-web/index.html`, set `<title>TickMine · 用说的，管清单</title>`

- [ ] **Step 3: 全量构建验证**

```bash
cd /Users/dean/code/tickmine/tickmine-web && npm run build && npm test
```

Expected: BUILD SUCCESS, all tests PASS

- [ ] **Step 4: Commit**

```bash
git add tickmine-web/
git commit -m "feat(web): wire chat page with full conversation flow"
```

---

### Task 11: 手动联调验证

**Files:** 无代码变更

**前置：** 后端 `agent-server` 在 `localhost:8080` 运行，且配置了有效的 `DEEPSEEK_API_KEY`。

- [ ] **Step 1: 启动后端**

```bash
cd /Users/dean/code/tickmine/tickmine-api/agent-server
# 按项目现有方式启动，例如：
docker compose up -d   # 或 mvn -pl agent-boot spring-boot:run
```

- [ ] **Step 2: 启动前端**

```bash
cd /Users/dean/code/tickmine/tickmine-web && npm run dev
```

Open: `http://localhost:5173`

- [ ] **Step 3: 走通 onboarding**

1. 确认/编辑 userId → 下一步
2. 粘贴有效 `dp_...` Token → 连接并进入
3. 应跳转到 `/chat`

- [ ] **Step 4: 走通对话闭环**

1. 发送「我要策划一场婚礼」
2. 多轮回答 Agent 追问（COLLECTING）
3. 信息完整后出现 PlanCard
4. 点击「确认写入滴答」→ 成功 banner 显示 projectId

- [ ] **Step 5: 验证持久化与新对话**

1. 刷新页面 → 消息仍在
2. 点击「新对话」→ 消息清空
3. 设置页修改 Token → 保存成功

- [ ] **Step 6: 验证错误路径**

1. 用 FREE 用户超限（或 mock 429）→ Toast「今日对话次数已用完」

---

### Task 12: 项目 README

**Files:**
- Create: `tickmine-web/README.md`

- [ ] **Step 1: 编写 README**

Create `tickmine-web/README.md`:

```markdown
# TickMine Web

对话式任务管理 Web 客户端，对接 TickMine Agent 后端。

## 开发

```bash
# 后端需在 localhost:8080 运行
npm install
npm run dev
```

API 请求通过 Vite proxy 转发到 `http://localhost:8080`。

## 测试

```bash
npm test
```

## 构建

```bash
npm run build
```
```

- [ ] **Step 2: Commit**

```bash
git add tickmine-web/README.md
git commit -m "docs(web): add README with dev instructions"
```

---

## Spec Coverage Checklist

| Spec 要求 | 对应 Task |
|-----------|-----------|
| Onboarding userId + Token | Task 6 |
| 强制 onboarding 再进 chat | Task 5 |
| 单会话对话 | Task 8, 10 |
| PlanCard + execute | Task 8, 9 |
| localStorage 持久化 | Task 4 |
| 新对话按钮 | Task 7 |
| 设置页改 Token | Task 7 |
| Vite proxy | Task 1 |
| 429/400/404 错误 | Task 9 |
| 深色 Toka 风格 | Task 1, 8 |
| Vitest store/hook 测试 | Task 4, 9 |
| 刷新同步 goal plan | Task 9 |
| 不做侧边栏/配额/流式 | 未纳入 Task（符合非目标） |
