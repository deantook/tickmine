import { useCallback, useState } from 'react';
import { AppHeader } from '@/components/layout/AppHeader';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { SessionHistoryPanel } from '@/components/chat/SessionHistoryPanel';
import { useChat } from '@/hooks/useChat';

const HISTORY_COLLAPSED_KEY = 'tickmine-history-collapsed';

function readCollapsedPreference(): boolean {
  try {
    return localStorage.getItem(HISTORY_COLLAPSED_KEY) === 'true';
  } catch {
    return false;
  }
}

export function ChatPage() {
  const { messages, isLoading, executing, sendMessage, handleExecute, currentGoalId } = useChat();
  const [historyCollapsed, setHistoryCollapsed] = useState(readCollapsedPreference);

  const toggleHistoryCollapsed = useCallback(() => {
    setHistoryCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(HISTORY_COLLAPSED_KEY, String(next));
      } catch {
        // ignore storage errors
      }
      return next;
    });
  }, []);

  return (
    <div className="flex h-screen flex-col bg-[#f7f7f5]">
      <AppHeader />
      <div className="flex min-h-0 flex-1 overflow-hidden">
        <SessionHistoryPanel
          collapsed={historyCollapsed}
          onToggleCollapsed={toggleHistoryCollapsed}
        />
        <main className="flex min-w-0 flex-1 flex-col overflow-hidden">
          <div className="mx-auto flex h-full w-full max-w-4xl min-h-0 flex-1 flex-col px-4">
            <MessageList
              messages={messages}
              goalId={currentGoalId}
              isLoading={isLoading}
              executing={executing}
              onExecute={handleExecute}
              onQuickAction={sendMessage}
            />
            <ChatInput disabled={isLoading || executing} onSend={sendMessage} />
          </div>
        </main>
      </div>
    </div>
  );
}
