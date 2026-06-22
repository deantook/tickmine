export interface ErrorResponse {
  error: string;
  message: string;
}

export interface ChatRequest {
  userId: string;
  message: string;
  goalId?: string;
}

export interface ToolCallRecord {
  name: string;
  input: Record<string, unknown>;
  output: unknown;
  durationMs: number;
  success: boolean;
  errorMessage: string | null;
}

export interface ChatResponse {
  goalId: string;
  phase: string;
  reply: string;
  plan: PlanDsl | null;
  missingFields: string[] | null;
  toolCalls?: ToolCallRecord[];
}

export interface PlanDsl {
  projectName: string;
  milestones: MilestoneDsl[];
  destination?: 'inbox' | 'project';
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
  dueTime?: string;
  checklistItems?: { title: string }[];
}

export interface ExecutionResult {
  success: boolean;
  projectId: string;
  taskIds: string[];
  errorMessage: string;
  toolCalls?: ToolCallRecord[];
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

export interface RegisterRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  userId: string;
  email: string;
  expiresAt: string;
}

export interface MeResponse {
  userId: string;
  email: string;
}
