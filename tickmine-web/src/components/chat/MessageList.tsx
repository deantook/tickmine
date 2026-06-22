import { useEffect, useRef } from 'react';
import type { ChatMessage } from '@/stores/sessionStore';
import { MessageBubble } from './MessageBubble';

interface Props {
  messages: ChatMessage[];
  goalId: string | null;
  isLoading: boolean;
  executing: boolean;
  onExecute: (goalId: string, messageIndex: number) => void;
  onQuickAction?: (text: string) => void;
}

const QUICK_ACTIONS = [
  '帮我把今天的事理一理',
  '我要策划一场婚礼',
];

export function MessageList({
  messages,
  goalId,
  isLoading,
  executing,
  onExecute,
  onQuickAction,
}: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  if (messages.length === 0 && !isLoading) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4">
        <p className="text-[14px] text-[#bbb]">说说你想完成的事</p>
        {onQuickAction && (
          <div className="flex flex-wrap justify-center gap-x-4 gap-y-2">
            {QUICK_ACTIONS.map((text) => (
              <button
                key={text}
                type="button"
                onClick={() => onQuickAction(text)}
                className="text-[12px] text-[#aaa] transition-colors hover:text-[#5c5c58]"
              >
                {text}
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="flex-1 space-y-6 overflow-y-auto py-6 [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden">
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
        <p className="text-center text-[14px] text-[#aaa]">
          Agent 正在思考
          <span className="inline-flex gap-0.5 ml-0.5">
            <span className="animate-pulse">.</span>
            <span className="animate-pulse [animation-delay:0.2s]">.</span>
            <span className="animate-pulse [animation-delay:0.4s]">.</span>
          </span>
        </p>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
