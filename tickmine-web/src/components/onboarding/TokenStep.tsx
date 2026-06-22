import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { bindTickTickToken } from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';

interface Props {
  submitLabel?: string;
  onSuccess: () => void;
}

export function TokenStep({ submitLabel = '连接并进入', onSuccess }: Props) {
  const [token, setToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [showHelp, setShowHelp] = useState(false);

  async function handleSubmit() {
    if (!token.trim()) return;
    setLoading(true);
    try {
      await bindTickTickToken(token.trim());
      onSuccess();
    } catch (e) {
      const msg = e instanceof ApiError ? e.message : '连接失败，请重试';
      toast.error(msg);
      if (e instanceof ApiError && e.code === 'TICKTICK_TOKEN_INVALID') {
        setToken('');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card className="w-full max-w-md">
      <CardHeader>
        <CardTitle>绑定滴答 API 口令</CardTitle>
        <CardDescription>绑定 API 口令后才能写入滴答清单</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-1.5">
          <Label htmlFor="token">API 口令</Label>
          <Input
            id="token"
            className="font-mono"
            placeholder="dp_..."
            value={token}
            onChange={(e) => setToken(e.target.value)}
            type="password"
          />
        </div>
        <button
          type="button"
          className="text-[13px] text-[#5c5c58] underline underline-offset-2 transition-colors hover:text-[#1c1c1a]"
          onClick={() => setShowHelp(!showHelp)}
        >
          如何获取 API 口令？
        </button>
        {showHelp && (
          <div className="border border-[#e8dfc8] bg-[#faf6ed] p-4">
            <p className="mb-2 text-[13px] font-medium text-[#5c4a2a]">获取步骤</p>
            <ol className="list-decimal space-y-1 pl-4 text-[13px] leading-relaxed text-[#8a7a5a]">
              <li>打开滴答清单网页版或 App</li>
              <li>进入「设置 → 账户与安全 → API 口令管理」</li>
              <li>新建或复制 API 口令（格式 dp_...）粘贴到上方</li>
              <li>若提示 Token 无效，请删除旧口令并重新生成</li>
            </ol>
          </div>
        )}
        <Button className="w-full" onClick={handleSubmit} disabled={loading || !token.trim()}>
          {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {submitLabel}
        </Button>
      </CardContent>
    </Card>
  );
}
