import type { ToolCallRecord } from '@/api/types';
import { cn } from '@/lib/utils';

interface Props {
  toolCalls: ToolCallRecord[];
  className?: string;
}

function formatJson(value: unknown): string {
  if (value == null) return 'null';
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

export function ToolCallsPanel({ toolCalls, className }: Props) {
  if (!toolCalls.length) return null;

  return (
    <div className={cn('space-y-2', className)}>
      <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-[#8a8a84]">
        tool call ({toolCalls.length})
      </p>
      {toolCalls.map((call, index) => (
        <details
          key={`${call.name}-${index}`}
          className="border border-[#e8e8e4] bg-[#fafaf8] open:border-[#dcdcd8] open:bg-white"
        >
          <summary className="flex cursor-pointer list-none items-center gap-2 px-3 py-2 text-[12px] [&::-webkit-details-marker]:hidden">
            <span className="font-mono text-[#3d3d3a]">{call.name}</span>
            <span
              className={cn(
                'ml-auto shrink-0 border px-1 py-0.5 text-[10px] font-medium',
                call.success
                  ? 'border-[#dcdcd8] bg-[#f0f0ec] text-[#5c5c58]'
                  : 'border-[#e8d4c4] bg-[#faf0e8] text-[#8b4513]',
              )}
            >
              {call.success ? `${call.durationMs}ms` : '失败'}
            </span>
          </summary>
          <div className="space-y-3 border-t border-[#e8e8e4] px-3 py-2">
            <div>
              <p className="mb-1 text-[10px] uppercase tracking-[0.18em] text-[#8a8a84]">in</p>
              <pre className="max-h-48 overflow-auto border border-[#e8e8e4] bg-white px-2 py-1.5 font-mono text-[11px] leading-relaxed text-[#3d3d3a]">
                {formatJson(call.input)}
              </pre>
            </div>
            <div>
              <p className="mb-1 text-[10px] uppercase tracking-[0.18em] text-[#8a8a84]">out</p>
              <pre className="max-h-48 overflow-auto border border-[#e8e8e4] bg-white px-2 py-1.5 font-mono text-[11px] leading-relaxed text-[#3d3d3a]">
                {call.success ? formatJson(call.output) : call.errorMessage ?? '未知错误'}
              </pre>
            </div>
          </div>
        </details>
      ))}
    </div>
  );
}
