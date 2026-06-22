import { apiFetch, ApiError, authHeaders } from './client';
import { sendChatStream } from './sse';
import type {
  AuthResponse,
  BindTokenRequest,
  BindTokenResponse,
  ChatRequest,
  ChatResponse,
  ConversationResponse,
  ExecutionResult,
  GoalResponse,
  GoalSummary,
  LoginRequest,
  MeResponse,
  RegisterRequest,
  TokenStatusResponse,
} from './types';

export function register(req: RegisterRequest) {
  return apiFetch<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export function login(req: LoginRequest) {
  return apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export function logout() {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' });
}

export function getMe() {
  return apiFetch<MeResponse>('/api/auth/me');
}

export function bindTickTickToken(token: string) {
  return apiFetch<BindTokenResponse>('/api/users/me/ticktick-token', {
    method: 'PUT',
    body: JSON.stringify({ token } satisfies BindTokenRequest),
  });
}

export function getTokenStatus() {
  return apiFetch<TokenStatusResponse>('/api/users/me/ticktick-token/status');
}

export function sendChat(req: ChatRequest) {
  return apiFetch<ChatResponse>('/api/chat', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export { sendChatStream };

export function getGoal(goalId: string) {
  return apiFetch<GoalResponse>(`/api/goals/${goalId}`);
}

export function executeGoal(goalId: string) {
  return apiFetch<ExecutionResult>(`/api/goals/${goalId}/execute`, {
    method: 'POST',
  });
}

export function listGoals() {
  return apiFetch<GoalSummary[]>('/api/users/me/goals');
}

export function getConversation(goalId: string) {
  return apiFetch<ConversationResponse>(`/api/goals/${goalId}/conversation`);
}

export function deleteGoal(goalId: string) {
  return apiFetch<void>(`/api/goals/${goalId}`, { method: 'DELETE' });
}

export function deleteAllGoals() {
  return apiFetch<void>('/api/users/me/goals', { method: 'DELETE' });
}

export { authHeaders };
