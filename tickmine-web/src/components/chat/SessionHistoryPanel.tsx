import { useState } from 'react';
import { ChevronLeft, ChevronRight, Loader2, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { useHistory } from '@/hooks/useHistory';

interface SessionHistoryPanelProps {
  collapsed: boolean;
  onToggleCollapsed: () => void;
}

function formatRelativeTime(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  const diffMs = Date.now() - date.getTime();
  const diffMin = Math.floor(diffMs / 60_000);
  if (diffMin < 1) return '刚刚';
  if (diffMin < 60) return `${diffMin} 分钟前`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour} 小时前`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 7) return `${diffDay} 天前`;
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
}

function phaseLabel(phase: string): string {
  switch (phase) {
    case 'PLAN_READY':
      return '待确认';
    case 'EXECUTING':
      return '执行中';
    case 'CHAT':
      return '闲聊';
    default:
      return '进行中';
  }
}

function CollapseToggle({
  collapsed,
  onClick,
}: {
  collapsed: boolean;
  onClick: () => void;
}) {
  const Icon = collapsed ? ChevronRight : ChevronLeft;
  const label = collapsed ? '展开历史会话' : '收起侧边栏';

  return (
    <button
      type="button"
      onClick={onClick}
      className="absolute top-1/2 left-full z-20 flex size-6 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full border border-[#e8e8e4] bg-[#fafaf8] text-[#aaa] shadow-sm transition-colors hover:border-[#ccc] hover:text-[#5c5c58]"
      title={label}
      aria-label={label}
    >
      <Icon className="size-3.5" />
    </button>
  );
}

export function SessionHistoryPanel({ collapsed, onToggleCollapsed }: SessionHistoryPanelProps) {
  const {
    goals,
    loading,
    loadingGoalId,
    deletingGoalId,
    clearing,
    currentGoalId,
    openGoal,
    removeGoal,
    clearAll,
  } = useHistory();

  const [confirmClear, setConfirmClear] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  const handleToggleCollapsed = () => {
    setConfirmClear(false);
    setConfirmDeleteId(null);
    onToggleCollapsed();
  };

  const handleDelete = async (goalId: string) => {
    if (confirmDeleteId !== goalId) {
      setConfirmDeleteId(goalId);
      return;
    }
    setConfirmDeleteId(null);
    await removeGoal(goalId);
  };

  const handleClearAll = async () => {
    if (!confirmClear) {
      setConfirmClear(true);
      return;
    }
    setConfirmClear(false);
    await clearAll();
  };

  return (
    <aside
      className={cn(
        'relative shrink-0 overflow-visible transition-[width] duration-200',
        collapsed ? 'w-0' : 'w-72',
      )}
    >
      <div
        className={cn(
          'flex w-72 flex-col border-r border-[#e8e8e4] bg-[#fafaf8]',
          collapsed
            ? 'pointer-events-none invisible absolute inset-y-0 left-0'
            : 'h-full',
        )}
      >
        <div className="relative min-h-0 flex-1 overflow-y-auto px-2 py-2">
          {loading && goals.length === 0 ? (
            <div className="flex items-center justify-center py-12 text-[#aaa]">
              <Loader2 className="size-4 animate-spin" />
            </div>
          ) : goals.length === 0 ? (
            <p className="px-2 py-8 text-center text-[12px] text-[#aaa]">暂无历史会话</p>
          ) : (
            <ul className="space-y-1">
              {goals.map((goal) => {
                const isActive = goal.id === currentGoalId;
                const isOpening = loadingGoalId === goal.id;
                const isDeleting = deletingGoalId === goal.id;
                const pendingDeleteConfirm = confirmDeleteId === goal.id;

                return (
                  <li key={goal.id}>
                    <div
                      className={cn(
                        'group flex items-start gap-2 rounded-lg px-2 py-2 transition-colors',
                        isActive ? 'bg-[#ececea]' : 'hover:bg-[#f0f0ec]',
                      )}
                    >
                      <button
                        type="button"
                        disabled={Boolean(loadingGoalId) || Boolean(deletingGoalId)}
                        onClick={() => void openGoal(goal.id)}
                        className="min-w-0 flex-1 text-left"
                      >
                        <p className="truncate text-[13px] font-medium text-[#1c1c1a]">
                          {goal.preview || goal.title || '新对话'}
                        </p>
                        <div className="mt-0.5 flex items-center gap-2 text-[11px] text-[#aaa]">
                          <span>{phaseLabel(goal.phase)}</span>
                          <span>·</span>
                          <span>{formatRelativeTime(goal.updatedAt)}</span>
                          {isOpening && <Loader2 className="size-3 animate-spin" />}
                        </div>
                      </button>
                      <button
                        type="button"
                        disabled={Boolean(deletingGoalId) || Boolean(loadingGoalId)}
                        onClick={() => void handleDelete(goal.id)}
                        className={cn(
                          'shrink-0 rounded p-1 transition-colors',
                          pendingDeleteConfirm
                            ? 'bg-[#8b4513]/10 text-[#8b4513]'
                            : 'text-[#ccc] opacity-0 group-hover:opacity-100 hover:bg-[#f0f0ec] hover:text-[#8b4513]',
                        )}
                        title={pendingDeleteConfirm ? '再次点击确认删除' : '删除'}
                        aria-label={pendingDeleteConfirm ? '确认删除' : '删除会话'}
                      >
                        {isDeleting ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : (
                          <Trash2 className="size-3.5" />
                        )}
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {goals.length > 0 && (
          <div className="border-t border-[#e8e8e4]/80 p-3">
            <Button
              variant={confirmClear ? 'destructive' : 'outline'}
              size="sm"
              className="w-full"
              disabled={clearing}
              onClick={() => void handleClearAll()}
            >
              {clearing ? (
                <Loader2 className="size-3.5 animate-spin" />
              ) : confirmClear ? (
                '确认清空全部？'
              ) : (
                '一键清除全部'
              )}
            </Button>
          </div>
        )}
      </div>

      <CollapseToggle collapsed={collapsed} onClick={handleToggleCollapsed} />
    </aside>
  );
}
