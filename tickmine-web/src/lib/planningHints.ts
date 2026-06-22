export const PLANNING_HINT_EXAMPLES = [
  '帮我把今天的事理一理',
  '帮我策划一场婚礼',
  '帮我规划一场毕业典礼',
  '帮我拆分需求文档进行工作安排',
  '帮我规划一次周末短途旅行',
  '下午三点去大润发买菜',
  '帮我制定两周的备考计划',
  '帮我安排明天的工作重点',
] as const;

export function pickRandomPlanningHints(count: number): string[] {
  const pool = [...PLANNING_HINT_EXAMPLES];
  for (let i = pool.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, Math.min(count, pool.length));
}
