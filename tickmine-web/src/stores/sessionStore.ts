import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { ExecutionResult, PlanDsl, ToolCallRecord } from '@/api/types';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  plan?: PlanDsl | null;
  executed?: ExecutionResult;
  toolCalls?: ToolCallRecord[];
}

interface SessionState {
  accessToken: string | null;
  email: string | null;
  userId: string | null;
  onboardingComplete: boolean;
  currentGoalId: string | null;
  messages: ChatMessage[];
  isLoading: boolean;
  isAuthenticated: () => boolean;
  setAuth: (accessToken: string, userId: string, email: string) => void;
  clearAuth: () => void;
  completeOnboarding: () => void;
  setGoalId: (goalId: string) => void;
  startNewChat: () => void;
  loadSession: (goalId: string, messages: ChatMessage[]) => void;
  appendMessage: (message: ChatMessage) => void;
  updateLastAssistant: (patch: Partial<ChatMessage>) => void;
  setLoading: (loading: boolean) => void;
}

export const useSessionStore = create<SessionState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      email: null,
      userId: null,
      onboardingComplete: false,
      currentGoalId: null,
      messages: [],
      isLoading: false,

      isAuthenticated: () => Boolean(get().accessToken && get().userId),
      setAuth: (accessToken, userId, email) => set({ accessToken, userId, email }),
      clearAuth: () =>
        set({
          accessToken: null,
          email: null,
          userId: null,
          onboardingComplete: false,
          currentGoalId: null,
          messages: [],
          isLoading: false,
        }),
      completeOnboarding: () => set({ onboardingComplete: true }),
      setGoalId: (goalId) => set({ currentGoalId: goalId }),
      startNewChat: () => set({ currentGoalId: null, messages: [], isLoading: false }),
      loadSession: (goalId, messages) =>
        set({ currentGoalId: goalId, messages, isLoading: false }),
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
        accessToken: s.accessToken,
        email: s.email,
        userId: s.userId,
        onboardingComplete: s.onboardingComplete,
        currentGoalId: s.currentGoalId,
        messages: s.messages,
      }),
    },
  ),
);

export function getAccessToken(): string | null {
  return useSessionStore.getState().accessToken;
}
