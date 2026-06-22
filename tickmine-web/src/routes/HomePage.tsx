import { Link } from 'react-router-dom';
import {
  BookOpen,
  Briefcase,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  ListChecks,
  MessageSquare,
  Sparkles,
  Zap,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Logo } from '@/components/layout/Logo';

const features = [
  {
    icon: MessageSquare,
    title: '自然语言澄清',
    description: '不用填表单。说出模糊想法，Agent 通过多轮对话帮你补全目标、期限和优先级。',
  },
  {
    icon: ListChecks,
    title: '结构化计划预览',
    description: '计划以里程碑 + 任务卡片呈现，含预计时长、截止日期和检查项，确认后再执行。',
  },
  {
    icon: Zap,
    title: '一键写入滴答清单',
    description: '确认计划后自动创建 TickTick 项目与任务，从「想做什么」到「清单里有了」只需几分钟。',
  },
];

const steps = [
  {
    step: '01',
    title: '注册并绑定 Token',
    description: '创建账户后，在 onboarding 中填入 TickTick Token，建立与清单的安全连接。',
  },
  {
    step: '02',
    title: '用对话描述目标',
    description: '像和同事聊天一样说明你想完成什么。Agent 会追问细节，直到目标足够清晰。',
  },
  {
    step: '03',
    title: '预览计划并执行',
    description: '查看生成的计划卡片，满意后一键执行，任务自动写入你的滴答清单。',
  },
];

const useCases = [
  {
    icon: Briefcase,
    title: '工作项目拆解',
    example: '「下季度要上线一个新功能，帮我拆成可执行的待办。」',
  },
  {
    icon: BookOpen,
    title: '学习计划制定',
    example: '「我想在 8 周内系统学完 React，每周大概 5 小时。」',
  },
  {
    icon: CalendarDays,
    title: '生活事务安排',
    example: '「下周要搬家，帮我列一份按时间排序的清单。」',
  },
];

const faqs = [
  {
    q: 'TickMine 和普通 AI 聊天有什么区别？',
    a: 'TickMine 专注「目标 → 计划 → 清单」闭环。对话不是为了闲聊，而是澄清目标、生成可执行计划，并直接写入 TickTick。',
  },
  {
    q: '需要安装滴答清单吗？',
    a: '需要你有 TickTick 账户。TickMine 通过 API Token 连接你的账户，执行后任务会出现在手机或桌面端的滴答清单里。',
  },
  {
    q: '计划可以修改吗？',
    a: '可以。在确认执行前，你可以继续对话调整计划；执行后如需改动，可在 TickTick 中直接编辑任务。',
  },
  {
    q: '对话和计划会保存吗？',
    a: '会。登录后你的对话与目标会保存在账户中，刷新页面或下次访问仍可继续。',
  },
];

const examplePrompts = ['上线新功能的项目计划', '8 周 React 学习路线', '下周搬家待办清单'];

function ProductPreview() {
  return (
    <div className="overflow-hidden rounded-2xl border border-[#e8e8e4] bg-white shadow-[0_24px_48px_-12px_rgba(28,28,26,0.08)]">
      <div className="flex items-center gap-2 border-b border-[#e8e8e4] bg-[#fafaf8] px-4 py-3">
        <div className="flex gap-1.5" aria-hidden>
          <span className="h-2.5 w-2.5 rounded-full bg-[#e8e8e4]" />
          <span className="h-2.5 w-2.5 rounded-full bg-[#e8e8e4]" />
          <span className="h-2.5 w-2.5 rounded-full bg-[#e8e8e4]" />
        </div>
        <span className="ml-2 text-[12px] text-[#aaa]">TickMine 对话预览</span>
      </div>

      <div className="space-y-6 p-5 sm:p-6">
        <div className="flex flex-col items-end">
          <span className="mb-1 text-[11px] text-[#aaa]">你</span>
          <p className="max-w-[85%] text-right text-[14px] leading-relaxed text-[#1c1c1a]">
            我想在 6 周内上线一个用户反馈功能，团队就我和一个后端，帮我拆成可执行的待办。
          </p>
        </div>

        <div className="flex flex-col items-start">
          <span className="mb-1 text-[11px] text-[#aaa]">回复</span>
          <div className="max-w-full border-l-2 border-[#dcdcd8] pl-4 text-[14px] leading-relaxed text-[#2d2d2a]">
            <p>
              好的，我先确认两点：反馈功能是嵌入现有 App，还是独立页面？另外 6
              周里是否包含测试与上线窗口？
            </p>

            <div className="mt-4 border border-[#e8e8e4] bg-[#fafaf8] p-4">
              <p className="text-[15px] font-medium text-[#1c1c1a]">用户反馈功能上线</p>
              <ul className="mt-3 space-y-3">
                <li>
                  <p className="text-[11px] font-medium tracking-[0.18em] text-[#8a8a84] uppercase">
                    需求与设计
                  </p>
                  <ul className="mt-1.5 space-y-1">
                    <li className="text-[14px] text-[#2d2d2a]">
                      <span className="mr-1 text-[12px] text-[#8a8a84]">1.</span>
                      整理反馈字段与入口方案
                      <span className="ml-2 text-[12px] text-[#8a8a84]">高 预计 2 天</span>
                    </li>
                    <li className="text-[14px] text-[#2d2d2a]">
                      <span className="mr-1 text-[12px] text-[#8a8a84]">2.</span>
                      完成 UI 原型评审
                      <span className="ml-2 text-[12px] text-[#8a8a84]">中 预计 1 天</span>
                    </li>
                  </ul>
                </li>
                <li>
                  <p className="text-[11px] font-medium tracking-[0.18em] text-[#8a8a84] uppercase">
                    开发与上线
                  </p>
                  <ul className="mt-1.5 space-y-1">
                    <li className="text-[14px] text-[#2d2d2a]">
                      <span className="mr-1 text-[12px] text-[#8a8a84]">3.</span>
                      前后端联调与灰度发布
                      <span className="ml-2 text-[12px] text-[#8a8a84]">高 预计 5 天</span>
                    </li>
                  </ul>
                </li>
              </ul>
              <div className="mt-4 flex items-center gap-2">
                <span className="inline-flex items-center gap-1.5 bg-[#1c1c1a] px-4 py-2 text-[13px] font-medium text-[#f7f7f5]">
                  <CheckCircle2 className="h-3.5 w-3.5" aria-hidden />
                  确认并写入清单
                </span>
                <span className="text-[12px] text-[#8a8a84]">预览示意，非真实交互</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function SectionHeading({
  eyebrow,
  title,
  description,
  centered = true,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  centered?: boolean;
}) {
  return (
    <div className={centered ? 'mx-auto max-w-2xl text-center' : 'max-w-2xl'}>
      <p className="mb-3 text-[12px] font-medium tracking-[0.14em] text-[#888] uppercase">
        {eyebrow}
      </p>
      <h2 className="text-[1.75rem] leading-tight font-semibold tracking-tight text-[#1c1c1a] sm:text-3xl">
        {title}
      </h2>
      {description && (
        <p className="mt-3 text-[15px] leading-relaxed text-[#5c5c58]">{description}</p>
      )}
    </div>
  );
}

export function HomePage() {
  return (
    <div className="flex min-h-screen flex-col bg-[#f7f7f5]">
      <header className="sticky top-0 z-10 border-b border-[#e8e8e4]/80 bg-[#f7f7f5]/90 backdrop-blur-sm">
        <div className="mx-auto flex w-full max-w-5xl items-center justify-between px-6 py-4">
          <Link to="/" className="transition-opacity hover:opacity-80">
            <Logo />
          </Link>
          <nav className="hidden items-center gap-6 text-[13px] text-[#5c5c58] sm:flex">
            <a href="#features" className="transition-colors hover:text-[#1c1c1a]">
              功能
            </a>
            <a href="#how-it-works" className="transition-colors hover:text-[#1c1c1a]">
              如何使用
            </a>
            <a href="#faq" className="transition-colors hover:text-[#1c1c1a]">
              常见问题
            </a>
          </nav>
          <div className="flex items-center gap-2">
            <Button size="sm" asChild>
              <Link to="/login">登录</Link>
            </Button>
          </div>
        </div>
      </header>

      <main>
        {/* Hero */}
        <section className="mx-auto w-full max-w-5xl px-6 pb-16 pt-14 sm:pt-20">
          <div className="grid items-center gap-12 lg:grid-cols-2 lg:gap-10">
            <div>
              <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-[#e8e8e4] bg-white/70 px-3 py-1 text-[12px] text-[#5c5c58]">
                <Sparkles className="h-3.5 w-3.5 text-[#1c1c1a]" aria-hidden />
                AI 目标规划助手 · 对接滴答清单
              </div>
              <h1 className="text-[2.5rem] leading-[1.12] font-semibold tracking-tight text-[#1c1c1a] sm:text-[3rem]">
                用说的，
                <br />
                管清单
              </h1>
              <p className="mt-5 max-w-md text-[16px] leading-relaxed text-[#5c5c58]">
                告别空白待办列表。告诉 TickMine 你想完成什么，它会帮你澄清目标、生成可执行计划，并一键同步到
                TickTick。
              </p>
              <div className="mt-8 flex flex-wrap items-center gap-3">
                <Button size="lg" asChild>
                  <Link to="/login">登录</Link>
                </Button>
              </div>
              <div className="mt-8">
                <p className="mb-2.5 text-[12px] text-[#aaa]">可以这样开始：</p>
                <div className="flex flex-wrap gap-2">
                  {examplePrompts.map((prompt) => (
                    <span
                      key={prompt}
                      className="rounded-full border border-[#e8e8e4] bg-white/60 px-3 py-1.5 text-[12px] text-[#5c5c58]"
                    >
                      {prompt}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            <ProductPreview />
          </div>
        </section>

        {/* Features */}
        <section id="features" className="border-t border-[#e8e8e4] bg-[#fafaf8]/60 py-16 sm:py-20">
          <div className="mx-auto w-full max-w-5xl px-6">
            <SectionHeading
              eyebrow="核心能力"
              title="从模糊想法到清晰行动"
              description="TickMine 把「想清楚」和「做进去」连成一条线，减少你在计划上花费的时间。"
            />
            <div className="mx-auto mt-12 grid max-w-4xl gap-4 sm:grid-cols-3">
              {features.map(({ icon: Icon, title, description }) => (
                <article
                  key={title}
                  className="rounded-xl border border-[#e8e8e4] bg-white p-5 transition-shadow hover:shadow-[0_8px_24px_-8px_rgba(28,28,26,0.06)]"
                >
                  <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-[#1c1c1a] text-[#f7f7f5]">
                    <Icon className="h-4 w-4" aria-hidden />
                  </div>
                  <h3 className="text-[15px] font-semibold text-[#1c1c1a]">{title}</h3>
                  <p className="mt-2 text-[13px] leading-relaxed text-[#5c5c58]">{description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* How it works */}
        <section id="how-it-works" className="py-16 sm:py-20">
          <div className="mx-auto w-full max-w-5xl px-6">
            <SectionHeading
              eyebrow="如何使用"
              title="三步开始你的第一个目标"
              description="注册后即可使用。首次绑定 TickTick Token 后，即可进入对话界面。"
            />
            <ol className="mx-auto mt-12 grid max-w-4xl gap-6 sm:grid-cols-3">
              {steps.map(({ step, title, description }) => (
                <li key={step} className="relative">
                  <span className="text-[2.5rem] font-semibold leading-none text-[#e8e8e4]">{step}</span>
                  <h3 className="mt-3 text-[16px] font-semibold text-[#1c1c1a]">{title}</h3>
                  <p className="mt-2 text-[13px] leading-relaxed text-[#5c5c58]">{description}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        {/* Use cases */}
        <section className="border-t border-[#e8e8e4] bg-[#fafaf8]/60 py-16 sm:py-20">
          <div className="mx-auto w-full max-w-5xl px-6">
            <SectionHeading
              eyebrow="适用场景"
              title="无论工作还是生活，都能帮上忙"
              description="只要是你想系统化推进的目标，都可以用对话的方式交给 TickMine 来规划。"
            />
            <div className="mx-auto mt-12 grid max-w-4xl gap-4 sm:grid-cols-3">
              {useCases.map(({ icon: Icon, title, example }) => (
                <article
                  key={title}
                  className="flex flex-col rounded-xl border border-[#e8e8e4] bg-white p-5"
                >
                  <div className="mb-3 flex h-9 w-9 items-center justify-center rounded-lg border border-[#e8e8e4] bg-[#fafaf8] text-[#1c1c1a]">
                    <Icon className="h-4 w-4" aria-hidden />
                  </div>
                  <h3 className="text-[15px] font-semibold text-[#1c1c1a]">{title}</h3>
                  <p className="mt-3 flex-1 text-[13px] leading-relaxed text-[#5c5c58] italic">
                    「{example}」
                  </p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* FAQ */}
        <section id="faq" className="py-16 sm:py-20">
          <div className="mx-auto w-full max-w-5xl px-6">
            <SectionHeading eyebrow="常见问题" title="你可能想了解的" />
            <dl className="mx-auto mt-12 max-w-2xl divide-y divide-[#e8e8e4]">
              {faqs.map(({ q, a }) => (
                <div key={q} className="py-5 first:pt-0 last:pb-0">
                  <dt className="flex items-start gap-2 text-[15px] font-medium text-[#1c1c1a]">
                    <ChevronRight className="mt-0.5 h-4 w-4 shrink-0 text-[#aaa]" aria-hidden />
                    {q}
                  </dt>
                  <dd className="mt-2 pl-6 text-[14px] leading-relaxed text-[#5c5c58]">{a}</dd>
                </div>
              ))}
            </dl>
          </div>
        </section>

        {/* Bottom CTA */}
        <section className="border-t border-[#e8e8e4] bg-[#1c1c1a] py-16 text-[#f7f7f5] sm:py-20">
          <div className="mx-auto w-full max-w-5xl px-6 text-center">
            <h2 className="text-[1.75rem] font-semibold tracking-tight sm:text-3xl">
              准备好把目标写进清单了吗？
            </h2>
            <p className="mx-auto mt-4 max-w-md text-[15px] leading-relaxed text-[#aaa]">
              注册账户，绑定 TickTick，用第一次对话体验从想法到待办的完整流程。
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <Button
                size="lg"
                className="bg-[#f7f7f5] text-[#1c1c1a] hover:bg-white"
                asChild
              >
                <Link to="/login">登录已有账户</Link>
              </Button>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-[#e8e8e4] px-6 py-8">
        <div className="mx-auto flex w-full max-w-5xl flex-col items-center justify-between gap-4 sm:flex-row">
          <Logo />
          <p className="text-[12px] text-[#aaa]">TickMine — 对话式目标规划，写入滴答清单</p>
          <div className="flex items-center gap-4 text-[12px] text-[#aaa]">
            <Link to="/login" className="transition-colors hover:text-[#5c5c58]">
              登录
            </Link>
            <Link to="/register" className="transition-colors hover:text-[#5c5c58]">
              注册
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
