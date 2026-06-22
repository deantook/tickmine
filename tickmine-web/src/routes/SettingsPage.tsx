import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { TokenStep } from '@/components/onboarding/TokenStep';
import { Logo } from '@/components/layout/Logo';
import { useSessionStore } from '@/stores/sessionStore';
import { logout } from '@/api/endpoints';
import { toast } from 'sonner';

export function SettingsPage() {
  const navigate = useNavigate();
  const email = useSessionStore((s) => s.email);
  const clearAuth = useSessionStore((s) => s.clearAuth);

  async function handleLogout() {
    try {
      await logout();
    } catch {
      // 本地仍清除会话，避免 token 失效时无法登出
    }
    clearAuth();
    toast.success('已登出');
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen bg-[#f7f7f5]">
      <header className="border-b border-[#e8e8e4]/80 bg-[#f7f7f5]/90 px-6 py-4 backdrop-blur-sm">
        <Logo />
      </header>
      <div className="mx-auto max-w-md px-6 py-8">
        <button
          type="button"
          onClick={() => navigate('/chat')}
          className="mb-6 text-[12px] text-[#aaa] transition-colors hover:text-[#5c5c58]"
        >
          ← 返回对话
        </button>

        {email && (
          <p className="mb-6 text-[13px] text-[#5c5c58]">
            当前账户：<span className="text-[#1c1c1a]">{email}</span>
          </p>
        )}

        <TokenStep
          submitLabel="保存 API 口令"
          onSuccess={() => {
            toast.success('API 口令已更新');
            navigate('/chat');
          }}
        />

        <Button variant="outline" className="mt-6 w-full" onClick={handleLogout}>
          登出
        </Button>
      </div>
    </div>
  );
}
