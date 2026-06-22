import { cn } from '@/lib/utils';

interface Props {
  className?: string;
  showText?: boolean;
}

export function Logo({ className, showText = true }: Props) {
  return (
    <div className={cn('flex items-center gap-2.5', className)}>
      <div
        className="flex h-7 w-7 items-center justify-center rounded-lg bg-[#1c1c1a] text-sm font-semibold text-[#f7f7f5]"
        aria-hidden
      >
        T
      </div>
      {showText && (
        <span className="text-[14px] font-semibold tracking-tight text-[#1c1c1a]">
          TickMine
        </span>
      )}
    </div>
  );
}
