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

function formatPriority(priority?: string) {
  switch (priority?.toLowerCase()) {
    case 'high':
      return '高';
    case 'low':
      return '低';
    default:
      return priority ? '中' : null;
  }
}

function TaskLine({ task, index }: { task: PlanDsl['milestones'][0]['tasks'][0]; index?: number }) {
  const priority = formatPriority(task.priority);
  return (
    <li className="text-[14px] text-[#2d2d2a]">
      {index != null && <span className="mr-1 text-[12px] text-[#8a8a84]">{index}.</span>}
      {task.title}
      {(priority || task.estimatedDuration || task.dueDate) && (
        <span className="ml-2 text-[12px] text-[#8a8a84]">
          {priority && <span className="mr-2">{priority}</span>}
          {task.estimatedDuration && <span className="mr-2">预计{task.estimatedDuration}</span>}
          {task.dueDate && (
            <span>
              {task.dueDate}
              {task.dueTime ? ` ${task.dueTime}` : ''}
            </span>
          )}
        </span>
      )}
      {task.checklistItems && task.checklistItems.length > 0 && (
        <ul className="mt-1 space-y-0.5 border-l border-[#e8e8e4] pl-3">
          {task.checklistItems.map((item) => (
            <li key={item.title} className="text-[12px] text-[#5c5c58]">
              {item.title}
            </li>
          ))}
        </ul>
      )}
    </li>
  );
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
                m.tasks.map((t, i) => <TaskLine key={`${m.name}-${t.title}-${i}`} task={t} index={i + 1} />),
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
                {m.tasks.map((t, i) => (
                  <TaskLine key={`${m.name}-${t.title}-${i}`} task={t} index={i + 1} />
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
