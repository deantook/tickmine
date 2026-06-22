import { createElement, type ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { useSessionStore } from '@/stores/sessionStore';

function wrapper({ children }: { children: ReactNode }) {
  return createElement(MemoryRouter, null, children);
}

vi.mock('@/api/endpoints', () => ({
  sendChatStream: vi.fn(),
  executeGoal: vi.fn(),
  getGoal: vi.fn(),
}));

import { sendChatStream } from '@/api/endpoints';
import { useChat } from './useChat';
import { ApiError } from '@/api/client';
import { toast } from 'sonner';

vi.mock('sonner', () => ({ toast: { error: vi.fn(), success: vi.fn() } }));

describe('useChat', () => {
  beforeEach(() => {
    localStorage.clear();
    useSessionStore.setState({
      accessToken: 'test-token',
      userId: 'u1',
      email: 'u1@example.com',
      onboardingComplete: true,
      currentGoalId: null,
      messages: [],
      isLoading: false,
    });
    vi.clearAllMocks();
  });

  it('sendMessage appends user and assistant messages on COLLECTING', async () => {
    vi.mocked(sendChatStream).mockImplementation(async (_req, handlers) => {
      handlers.onDelta('请问');
      handlers.onDelta('婚礼什么时候？');
      handlers.onDone({
        goalId: 'g1',
        phase: 'COLLECTING',
        reply: '请问婚礼什么时候？',
        plan: null,
        missingFields: ['date'],
      });
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
    vi.mocked(sendChatStream).mockRejectedValue(new ApiError(429, 'QUOTA_EXCEEDED', 'limit'));

    const { result } = renderHook(() => useChat(), { wrapper });
    await act(async () => {
      await result.current.sendMessage('hello');
    });

    expect(toast.error).toHaveBeenCalledWith('今日对话次数已用完');
  });
});
