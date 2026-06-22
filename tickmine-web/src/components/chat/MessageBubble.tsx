import type { ChatMessage } from '@/stores/sessionStore';
import { cn } from '@/lib/utils';
import { PlanCard } from './PlanCard';
import { ToolCallsPanel } from './ToolCallsPanel';

interface Props {
  message: ChatMessage;
  goalId: string | null;
  onExecute: (goalId: string, messageIndex: number) => void;
  messageIndex: number;
  executing: boolean;
}

export function MessageBubble({ message, goalId, onExecute, messageIndex, executing }: Props) {
  const isUser = message.role === 'user';

  return (
    <div className={cn('flex flex-col', isUser ? 'items-end' : 'items-start')}>
      <span className="mb-1 text-[11px] text-[#aaa]">{isUser ? '你' : '回复'}</span>
      <div
        className={cn(
          'max-w-full text-[14px] leading-relaxed',
          isUser
            ? 'text-right text-[#1c1c1a]'
            : 'border-l-2 border-[#dcdcd8] pl-4 text-[#2d2d2a]',
        )}
      >
        {message.content && (
          <p className="whitespace-pre-wrap">{message.content}</p>
        )}
        {!isUser && message.toolCalls && message.toolCalls.length > 0 && (
          <ToolCallsPanel toolCalls={message.toolCalls} className="mt-3" />
        )}
        {!isUser && message.plan && goalId && (
          <PlanCard
            plan={message.plan}
            executed={message.executed}
            executing={executing}
            onConfirm={() => onExecute(goalId, messageIndex)}
          />
        )}
      </div>
    </div>
  );
}
