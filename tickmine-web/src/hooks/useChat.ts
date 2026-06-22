import { useCallback, useState, useEffect } from 'react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import { sendChatStream, executeGoal, getGoal } from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { useSessionStore } from '@/stores/sessionStore';

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
      let accumulated = '';
      let assistantStarted = false;
      try {
        await sendChatStream(
          {
            userId,
            message: text,
            goalId: currentGoalId ?? undefined,
          },
          {
            onDelta: (chunk) => {
              accumulated += chunk;
              if (!assistantStarted) {
                appendMessage({ role: 'assistant', content: accumulated });
                assistantStarted = true;
                setLoading(false);
              } else {
                updateLastAssistant({ content: accumulated });
              }
            },
            onDone: (res) => {
              setGoalId(res.goalId);
              const patch = {
                content: res.reply,
                plan: res.phase === 'PLAN_READY' ? res.plan ?? undefined : undefined,
                toolCalls: res.toolCalls?.length ? res.toolCalls : undefined,
              };
              if (!assistantStarted) {
                appendMessage({ role: 'assistant', ...patch });
              } else {
                updateLastAssistant(patch);
              }
            },
          },
        );
      } catch (e) {
        if (e instanceof ApiError) {
          if (e.status === 429) toast.error('今日对话次数已用完');
          else if (e.code === 'TICKTICK_NOT_CONNECTED' || e.code === 'TICKTICK_TOKEN_INVALID') {
            toast.error(e.code === 'TICKTICK_TOKEN_INVALID' ? '滴答 Token 已失效，请重新绑定' : '请先绑定滴答 Token');
            navigate('/settings');
          } else toast.error(e.message);
        } else {
          toast.error('发送失败，请重试');
        }
      } finally {
        setLoading(false);
      }
    },
    [userId, currentGoalId, isLoading, appendMessage, setGoalId, setLoading, updateLastAssistant, navigate],
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
        if (e instanceof ApiError && (e.code === 'TICKTICK_NOT_CONNECTED' || e.code === 'TICKTICK_TOKEN_INVALID')) {
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
