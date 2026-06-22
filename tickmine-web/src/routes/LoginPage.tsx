import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Logo } from '@/components/layout/Logo';
import { login, getTokenStatus } from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { parseSubscriptionTier } from '@/lib/subscriptionTier';
import { useSessionStore } from '@/stores/sessionStore';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';

export function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useSessionStore((s) => s.setAuth);
  const completeOnboarding = useSessionStore((s) => s.completeOnboarding);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim() || !password) return;
    setLoading(true);
    try {
      const res = await login({ email: email.trim(), password });
      setAuth(
        res.accessToken,
        res.userId,
        res.email,
        parseSubscriptionTier(res.subscriptionTier) ?? 'FREE',
      );
      try {
        const status = await getTokenStatus();
        if (status.connected) {
          completeOnboarding();
          navigate('/chat', { replace: true });
        } else {
          navigate('/onboarding', { replace: true });
        }
      } catch {
        navigate('/onboarding', { replace: true });
      }
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : '登录失败，请重试';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-[#f7f7f5] px-6">
      <div className="mb-8 text-center">
        <Logo className="mb-4 justify-center" />
        <p className="text-[14px] text-[#5c5c58]">用说的，管清单</p>
      </div>
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>登录</CardTitle>
          <CardDescription>使用邮箱登录你的账户</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email">邮箱</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="password">密码</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <Button className="w-full" type="submit" disabled={loading || !email.trim() || !password}>
              {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              登录
            </Button>
            <p className="text-center text-[13px] text-[#5c5c58]">
              还没有账户？{' '}
              <Link
                to="/register"
                className="text-[#1c1c1a] underline underline-offset-2 transition-colors hover:text-[#555]"
              >
                注册
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
