import { useCallback, useEffect, useState } from 'react';
import { toast } from 'sonner';
import {
  deleteAllGoals,
  deleteGoal,
  getConversation,
  listGoals,
} from '@/api/endpoints';
import { ApiError } from '@/api/client';
import type { ConversationResponse, GoalSummary } from '@/api/types';
import { useSessionStore, type ChatMessage } from '@/stores/sessionStore';

function mapConversationToMessages(conversation: ConversationResponse): ChatMessage[] {
  const messages: ChatMessage[] = conversation.messages.map((msg) => ({
    role: msg.role,
    content: msg.content,
  }));

  if (conversation.phase === 'PLAN_READY' && conversation.latestPlan) {
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role === 'assistant') {
        messages[i] = { ...messages[i], plan: conversation.latestPlan };
        break;
      }
    }
  }

  return messages;
}

export function useHistory() {
  const currentGoalId = useSessionStore((s) => s.currentGoalId);
  const loadSession = useSessionStore((s) => s.loadSession);
  const startNewChat = useSessionStore((s) => s.startNewChat);

  const [goals, setGoals] = useState<GoalSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingGoalId, setLoadingGoalId] = useState<string | null>(null);
  const [deletingGoalId, setDeletingGoalId] = useState<string | null>(null);
  const [clearing, setClearing] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const items = await listGoals();
      setGoals(items);
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : '加载历史失败';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const openGoal = useCallback(
    async (goalId: string) => {
      if (loadingGoalId) return;
      setLoadingGoalId(goalId);
      try {
        const conversation = await getConversation(goalId);
        loadSession(goalId, mapConversationToMessages(conversation));
      } catch (e) {
        const msg = e instanceof ApiError ? e.message : '加载会话失败';
        toast.error(msg);
      } finally {
        setLoadingGoalId(null);
      }
    },
    [loadSession, loadingGoalId],
  );

  const removeGoal = useCallback(
    async (goalId: string) => {
      if (deletingGoalId) return;
      setDeletingGoalId(goalId);
      try {
        await deleteGoal(goalId);
        setGoals((prev) => prev.filter((g) => g.id !== goalId));
        if (currentGoalId === goalId) {
          startNewChat();
        }
        toast.success('已删除');
      } catch (e) {
        const msg = e instanceof ApiError ? e.message : '删除失败';
        toast.error(msg);
      } finally {
        setDeletingGoalId(null);
      }
    },
    [currentGoalId, deletingGoalId, startNewChat],
  );

  const clearAll = useCallback(async () => {
    if (clearing || goals.length === 0) return;
    setClearing(true);
    try {
      await deleteAllGoals();
      setGoals([]);
      startNewChat();
      toast.success('已清空全部历史');
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : '清空失败';
      toast.error(msg);
    } finally {
      setClearing(false);
    }
  }, [clearing, goals.length, startNewChat]);

  return {
    goals,
    loading,
    loadingGoalId,
    deletingGoalId,
    clearing,
    currentGoalId,
    refresh,
    openGoal,
    removeGoal,
    clearAll,
  };
}
