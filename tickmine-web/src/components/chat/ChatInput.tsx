import { useCallback, useState } from 'react';
import { Button } from '@/components/ui/button';

interface Props {
  disabled: boolean;
  onSend: (text: string) => void;
}

export function ChatInput({ disabled, onSend }: Props) {
  const [text, setText] = useState('');

  const handleSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      const trimmed = text.trim();
      if (!trimmed || disabled) return;
      onSend(trimmed);
      setText('');
    },
    [text, disabled, onSend],
  );

  return (
    <form
      onSubmit={handleSubmit}
      className="sticky bottom-0 border-t border-[#e8e8e4] bg-[#fafaf8] py-4"
    >
      <div className="flex gap-3">
        <div className="flex flex-1 border border-[#dcdcd8] bg-white focus-within:border-[#aaa]">
          <textarea
            value={text}
            onChange={(e) => setText(e.target.value)}
            disabled={disabled}
            placeholder="说说你想完成的事…"
            rows={1}
            className="min-h-[40px] max-h-32 flex-1 resize-none bg-transparent px-3 py-2.5 text-[14px] leading-relaxed outline-none placeholder:text-[#aaa] disabled:opacity-50"
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSubmit(e);
              }
            }}
          />
        </div>
        <Button type="submit" size="sm" disabled={disabled || !text.trim()}>
          发送
        </Button>
      </div>
    </form>
  );
}
