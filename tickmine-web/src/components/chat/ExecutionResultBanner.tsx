import type { ExecutionResult } from '@/api/types';

export function ExecutionResultBanner({ result }: { result: ExecutionResult }) {
  if (!result.success) {
    return (
      <div className="mt-3 border border-[#e8d4c4] bg-[#faf0e8] p-3 text-[14px] text-[#8b4513]">
        {result.errorMessage || '写入失败'}
      </div>
    );
  }
  return (
    <div className="mt-3 border border-[#e8e8e4] bg-[#f0f0ec] p-3 text-[14px] text-[#3d3d3a]">
      {result.projectId === 'inbox'
        ? `已写入收集箱 · ${result.taskIds.length} 个任务`
        : `已写入滴答清单 · 项目 ID: ${result.projectId}${result.taskIds.length > 0 ? ` · ${result.taskIds.length} 个任务` : ''}`}
    </div>
  );
}
