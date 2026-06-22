import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Logo } from '@/components/layout/Logo';
import { register } from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { useSessionStore } from '@/stores/sessionStore';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';

export function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useSessionStore((s) => s.setAuth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim() || !password) return;
    if (password !== confirmPassword) {
      toast.error('两次输入的密码不一致');
      return;
    }
    if (password.length < 8) {
      toast.error('密码至少 8 位');
      return;
    }
    setLoading(true);
    try {
      const res = await register({ email: email.trim(), password });
      setAuth(res.accessToken, res.userId, res.email);
      toast.success('注册成功');
      navigate('/onboarding', { replace: true });
    } catch (err) {
      const msg = err instanceof ApiError ? err.message : '注册失败，请重试';
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
          <CardTitle>注册</CardTitle>
          <CardDescription>创建账户以保存你的目标与对话</CardDescription>
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
                autoComplete="new-password"
                placeholder="至少 8 位"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="confirmPassword">确认密码</Label>
              <Input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
            <Button
              className="w-full"
              type="submit"
              disabled={loading || !email.trim() || !password || !confirmPassword}
            >
              {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              注册
            </Button>
            <p className="text-center text-[13px] text-[#5c5c58]">
              已有账户？{' '}
              <Link
                to="/login"
                className="text-[#1c1c1a] underline underline-offset-2 transition-colors hover:text-[#555]"
              >
                登录
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
