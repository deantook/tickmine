import { AppHeader } from '@/components/layout/AppHeader';
import { MessageList } from '@/components/chat/MessageList';
import { ChatInput } from '@/components/chat/ChatInput';
import { useChat } from '@/hooks/useChat';

export function ChatPage() {
  const { messages, isLoading, executing, sendMessage, handleExecute, currentGoalId } = useChat();

  return (
    <div className="flex h-screen flex-col bg-[#f7f7f5]">
      <AppHeader />
      <main className="mx-auto flex w-full max-w-2xl flex-1 flex-col overflow-hidden">
        <MessageList
          messages={messages}
          goalId={currentGoalId}
          isLoading={isLoading}
          executing={executing}
          onExecute={handleExecute}
          onQuickAction={sendMessage}
        />
        <ChatInput disabled={isLoading || executing} onSend={sendMessage} />
      </main>
    </div>
  );
}
