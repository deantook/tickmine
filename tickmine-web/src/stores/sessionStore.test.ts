import { beforeEach, describe, expect, it } from 'vitest';
import { useSessionStore } from './sessionStore';

describe('sessionStore', () => {
  beforeEach(() => {
    localStorage.clear();
    useSessionStore.setState({
      accessToken: null,
      email: null,
      userId: null,
      subscriptionTier: null,
      onboardingComplete: false,
      currentGoalId: null,
      messages: [],
      isLoading: false,
    });
  });

  it('setAuth stores credentials', () => {
    useSessionStore.getState().setAuth('token-abc', 'user-abc', 'test@example.com');
    const state = useSessionStore.getState();
    expect(state.userId).toBe('user-abc');
    expect(state.accessToken).toBe('token-abc');
    expect(state.email).toBe('test@example.com');
    expect(state.isAuthenticated()).toBe(true);
  });

  it('clearAuth resets session', () => {
    useSessionStore.getState().setAuth('token-abc', 'user-abc', 'test@example.com');
    useSessionStore.getState().completeOnboarding();
    useSessionStore.getState().clearAuth();
    const state = useSessionStore.getState();
    expect(state.isAuthenticated()).toBe(false);
    expect(state.onboardingComplete).toBe(false);
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
