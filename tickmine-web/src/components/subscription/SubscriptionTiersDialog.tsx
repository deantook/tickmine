import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Check, X } from 'lucide-react';
import type { SubscriptionTier } from '@/api/types';
import { formatSubscriptionTierLabel } from '@/lib/subscriptionTier';
import { cn } from '@/lib/utils';

interface TierFeature {
  text: string;
  included: boolean;
}

interface TierInfo {
  tier: SubscriptionTier;
  headline: string;
  model: string;
  chatQuota: string;
  features: TierFeature[];
}

const TIERS: TierInfo[] = [
  {
    tier: 'FREE',
    headline: '每日有限对话次数',
    model: 'DeepSeek Chat',
    chatQuota: '每日 10 次',
    features: [
      { text: '自然语言澄清目标与生成计划', included: true },
      { text: '确认后一键写入滴答清单', included: true },
      { text: '无限对话次数', included: false },
      { text: '上传图片与文件', included: false },
    ],
  },
  {
    tier: 'VIP',
    headline: 'DeepSeek 无限对话次数',
    model: 'DeepSeek Chat',
    chatQuota: '无限次',
    features: [
      { text: '自然语言澄清目标与生成计划', included: true },
      { text: '确认后一键写入滴答清单', included: true },
      { text: '无限对话次数', included: true },
      { text: '上传图片与文件', included: false },
    ],
  },
  {
    tier: 'SVIP',
    headline: '高级模型，支持上传图片与文件',
    model: '通义千问 Plus',
    chatQuota: '无限次',
    features: [
      { text: '自然语言澄清目标与生成计划', included: true },
      { text: '确认后一键写入滴答清单', included: true },
      { text: '无限对话次数', included: true },
      { text: '上传图片与文件辅助规划', included: true },
    ],
  },
];

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentTier: SubscriptionTier | null;
}

export function SubscriptionTiersDialog({ open, onOpenChange, currentTier }: Props) {
  useEffect(() => {
    if (!open) return;
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onOpenChange(false);
    }
    document.addEventListener('keydown', onKeyDown);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = prevOverflow;
    };
  }, [open, onOpenChange]);

  if (!open) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center p-6 sm:p-8"
      onClick={() => onOpenChange(false)}
    >
      <div className="absolute inset-0 bg-[#1c1c1a]/25 backdrop-blur-sm" aria-hidden />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="subscription-tiers-title"
        className="relative flex w-full max-w-[min(92vw,960px)] flex-col border border-[#e8e8e4] bg-[#fafaf8] text-[#1c1c1a] shadow-2xl max-h-[min(90vh,760px)]"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex shrink-0 items-start justify-between gap-4 border-b border-[#e8e8e4] px-6 py-5 sm:px-8">
          <div>
            <h2 id="subscription-tiers-title" className="text-[17px] font-semibold">
              付费档位
            </h2>
            <p className="mt-1.5 text-[13px] leading-relaxed text-[#5c5c58]">
              选择适合你的方案。对话次数、模型能力与附件支持因档位而异。
            </p>
          </div>
          <button
            type="button"
            className="shrink-0 rounded p-1 text-[#aaa] transition-colors hover:bg-[#f0f0ec] hover:text-[#1c1c1a]"
            onClick={() => onOpenChange(false)}
            aria-label="关闭"
          >
            <X className="size-5" />
          </button>
        </div>

        <div className="overflow-y-auto px-6 py-5 sm:px-8 sm:py-6">
          <ul className="grid gap-4 sm:grid-cols-3 sm:gap-5">
            {TIERS.map(({ tier, headline, model, chatQuota, features }) => {
              const isCurrent = tier === currentTier;
              return (
                <li
                  key={tier}
                  className={cn(
                    'flex min-w-0 flex-col rounded border p-4 sm:p-5',
                    isCurrent
                      ? 'border-[#1c1c1a] bg-[#f0f0ec] ring-1 ring-[#1c1c1a]/10'
                      : 'border-[#e8e8e4] bg-white/70',
                  )}
                >
                  <div className="mb-3 flex items-center justify-between gap-2">
                    <span className="text-[15px] font-semibold">
                      {formatSubscriptionTierLabel(tier)}
                    </span>
                    {isCurrent && (
                      <span className="rounded bg-[#1c1c1a] px-1.5 py-0.5 text-[10px] text-[#f7f7f5]">
                        当前
                      </span>
                    )}
                  </div>
                  <p className="text-[13px] font-medium leading-snug text-[#1c1c1a]">
                    {headline}
                  </p>
                  <dl className="mt-4 space-y-2 border-t border-[#e8e8e4] pt-4 text-[12px]">
                    <div className="flex justify-between gap-3">
                      <dt className="text-[#aaa]">模型</dt>
                      <dd className="text-right font-medium text-[#5c5c58]">{model}</dd>
                    </div>
                    <div className="flex justify-between gap-3">
                      <dt className="text-[#aaa]">对话次数</dt>
                      <dd className="text-right font-medium text-[#5c5c58]">{chatQuota}</dd>
                    </div>
                  </dl>
                  <ul className="mt-4 space-y-2.5">
                    {features.map(({ text, included }) => (
                      <li
                        key={text}
                        className={cn(
                          'flex items-start gap-2 text-[12px] leading-relaxed',
                          included ? 'text-[#5c5c58]' : 'text-[#bbb]',
                        )}
                      >
                        {included ? (
                          <Check className="mt-0.5 size-3.5 shrink-0 text-[#1c1c1a]" />
                        ) : (
                          <X className="mt-0.5 size-3.5 shrink-0 text-[#ccc]" />
                        )}
                        <span>{text}</span>
                      </li>
                    ))}
                  </ul>
                </li>
              );
            })}
          </ul>
          <p className="mt-5 text-center text-[11px] text-[#aaa]">
            升级或变更档位请联系管理员。免费版每日对话次数于北京时间 0 点重置。
          </p>
        </div>
      </div>
    </div>,
    document.body,
  );
}
