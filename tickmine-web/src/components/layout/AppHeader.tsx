import { Link } from 'react-router-dom';
import { Logo } from '@/components/layout/Logo';
import { useSessionStore } from '@/stores/sessionStore';
import { useTokenStatus } from '@/hooks/useTokenStatus';
import { cn } from '@/lib/utils';

export function AppHeader() {
  const startNewChat = useSessionStore((s) => s.startNewChat);
  const connected = useTokenStatus();

  return (
    <header className="sticky top-0 z-10 flex h-14 items-center justify-between border-b border-[#e8e8e4]/80 bg-[#f7f7f5]/90 px-6 backdrop-blur-sm">
      <div className="flex items-center gap-3">
        <Logo showText={false} />
        <span className="text-[14px] font-semibold text-[#1c1c1a]">TickMine</span>
        <span
          className={cn(
            'h-1.5 w-1.5 rounded-full',
            connected === true && 'bg-[#1c1c1a]',
            connected === false && 'bg-[#ccc]',
            connected === null && 'bg-[#ddd]',
          )}
          title={connected ? '已连接滴答' : '未连接'}
          aria-label={connected ? '已连接滴答' : '未连接'}
        />
      </div>
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={startNewChat}
          className="text-[12px] text-[#aaa] transition-colors hover:text-[#5c5c58]"
        >
          新对话
        </button>
        <Link
          to="/settings"
          className="text-[12px] text-[#aaa] transition-colors hover:text-[#5c5c58]"
          aria-label="设置"
        >
          设置
        </Link>
      </div>
    </header>
  );
}
