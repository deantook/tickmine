import type { PlanDsl, ExecutionResult } from '@/api/types';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import { ExecutionResultBanner } from './ExecutionResultBanner';
import { ToolCallsPanel } from './ToolCallsPanel';

interface Props {
  plan: PlanDsl;
  executed?: ExecutionResult;
  executing: boolean;
  onConfirm: () => void;
}

export function PlanCard({ plan, executed, executing, onConfirm }: Props) {
  const useInbox = plan.destination === 'inbox';

  return (
    <div className="mt-3 border border-[#e8e8e4] bg-[#fafaf8] p-4">
      <p className="text-[15px] font-medium text-[#1c1c1a]">
        {useInbox ? '收集箱待办' : plan.projectName}
      </p>
      {useInbox && plan.projectName && (
        <p className="mt-1 text-[13px] text-[#5c5c58]">{plan.projectName}</p>
      )}
      <ul className="mt-3 space-y-3">
        {useInbox ? (
          <li>
            <ul className="space-y-1">
              {plan.milestones.flatMap((m) =>
                m.tasks.map((t) => (
                  <li key={t.title} className="text-[14px] text-[#2d2d2a]">
                    {t.title}
                    {t.dueDate && (
                      <span className="ml-2 text-[12px] text-[#8a8a84]">
                        {t.dueDate}
                        {t.dueTime ? ` ${t.dueTime}` : ''}
                      </span>
                    )}
                  </li>
                )),
              )}
            </ul>
          </li>
        ) : (
          plan.milestones.map((m) => (
            <li key={m.name}>
              <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-[#8a8a84]">
                {m.name}
              </p>
              <ul className="mt-1 space-y-1 border-l border-[#e8e8e4] pl-3">
                {m.tasks.map((t) => (
                  <li key={t.title} className="text-[14px] text-[#2d2d2a]">
                    {t.title}
                    {t.dueDate && (
                      <span className="ml-2 text-[12px] text-[#8a8a84]">
                        {t.dueDate}
                        {t.dueTime ? ` ${t.dueTime}` : ''}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            </li>
          ))
        )}
      </ul>
      {executed ? (
        <>
          <ExecutionResultBanner result={executed} />
          {executed.toolCalls && executed.toolCalls.length > 0 && (
            <ToolCallsPanel toolCalls={executed.toolCalls} className="mt-3" />
          )}
        </>
      ) : (
        <Button className="mt-4 w-full" onClick={onConfirm} disabled={executing}>
          {executing && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {useInbox ? '确认写入收集箱' : '确认写入滴答'}
        </Button>
      )}
    </div>
  );
}
