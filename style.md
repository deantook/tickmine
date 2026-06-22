# Toka 前端 UI 风格指南

本文档总结 Toka 项目（`apps/web` 官网 + `apps/desktop` 桌面应用）的前端视觉与交互规范，供后续开发保持一致性。

---

## 1. 设计理念

Toka 的 UI 追求 **克制、干净、工具感**：

- **暖灰中性色**：避免纯白/纯黑，整体偏纸质感（off-white + charcoal）
- **几乎无圆角**：按钮、卡片、输入框以直角为主，呈现编辑/工具类产品的严肃感
- **细边框分层**：用 1px 边框和背景色阶区分层级，而非大阴影或渐变
- **小字号高密度**：桌面端对话区以 12–14px 为主，信息紧凑但不拥挤
- **中文优先**：文案、日期格式、locale 均为 `zh-CN`

技术选型文档中注明样式与 Rick 项目一致：**Tailwind CSS v4**（`@tailwindcss/vite`），无独立 `tailwind.config`，颜色与尺寸直接在 className 中以任意值（`text-[#1c1c1a]`）声明。

---

## 2. 技术栈

| 项目 | 路径 | 框架 | 样式 |
|------|------|------|------|
| 官网 | `apps/web` | React 19 + Vite + React Router | Tailwind CSS 4 |
| 桌面 | `apps/desktop` | React 19 + Vite + Tauri 2 | Tailwind CSS 4 |

共享约定：

- 全局样式入口：`src/index.css`，通过 `@import "tailwindcss"` 引入
- **不使用** CSS Modules、styled-components 或 shadcn 组件库（desktop 的 package.json 虽含 Radix/lucide 依赖，当前 UI 未引用）
- 桌面端表单输入使用 CSS 类 `.field-input`（定义于 `apps/desktop/src/index.css`）
- 桌面端 Markdown 渲染使用 `react-markdown` + `remark-gfm`，样式由 `.markdown` 全局规则 + 组件内联 class 共同控制

---

## 3. 色彩系统

所有颜色以 **硬编码 hex** 使用，未抽象为 design token 变量。以下为语义分组。

### 3.1 背景色

| 色值 | 用途 |
|------|------|
| `#f7f7f5` | 页面主背景（`:root`、layout 根节点） |
| `#fafaf8` | 抬升表面：卡片、输入区底栏、模态内容、聊天 mockup |
| `#f0f0ec` | 侧栏、页脚、区块背景、调试面板、代码块底色 |
| `#ffffff` / `white` | 输入框、卡片内嵌块、代码/pre 背景 |
| `white/60` | 次要按钮半透明底 |

### 3.2 文字色

| 色值 | 层级 | 典型用途 |
|------|------|----------|
| `#1c1c1a` | 主色 | 标题、正文、主按钮文字、链接默认色 |
| `#2d2d2a` | — | 助手回复正文 |
| `#3d3d3a` | — | 侧栏非激活项、调试日志正文 |
| `#5c5c58` | 次要 | 说明段落、导航默认态、表单标签辅助 |
| `#8a8a84` / `#8a8a86` |  tertiary | 元信息、脚注、SectionLabel、时间戳标签 |
| `#aaa` | 弱化 | 占位提示、时间戳、侧栏操作、空状态 |
| `#bbb` / `#ccc` / `#ddd` | 禁用 | 空对话提示、禁用按钮、删除按钮默认态 |
| `#555` | 交互 | 链接 hover |

### 3.3 边框与分隔

| 色值 | 用途 |
|------|------|
| `#e8e8e4` | 主分隔线：section 边框、卡片边框、列表分隔 |
| `#dcdcd8` | 输入框边框、次级容器边框、引用块左边线 |
| `#e0e0dc` | 侧栏内部细分隔 |
| `#ccc` | 卡片 hover 边框强调 |
| `#aaa` | 输入 focus 边框、次要按钮 hover 边框 |

Header 使用 `border-[#e8e8e4]/80` 半透明边框；网格背景线同样为 `#e8e8e4`。

### 3.4 品牌 / 交互色

| 色值 | 用途 |
|------|------|
| `#1c1c1a` | 主按钮背景、Logo 方块、用户消息气泡（官网 mockup） |
| `#333` / `#3d3d3a` | 主按钮 hover、Logo hover |
| `#f7f7f5` / `#fafaf8` | 主按钮文字、Logo 字母 |

### 3.5 语义色（警告 / 错误 / 调试）

**配置提示（桌面 ChatPanel）**

| 色值 | 用途 |
|------|------|
| `#faf6ed` | 警告背景 |
| `#e8dfc8` | 警告边框 |
| `#5c4a2a` | 警告标题 |
| `#8a7a5a` | 警告正文 |

**手册重要提示（ManualPage）**

- 背景 `#febc2e/10`，边框 `#febc2e/40`

**调试 / 失败状态（DebugLogPanel、ToolTrace）**

| 色值 | 用途 |
|------|------|
| `#8b4513` | 错误文字 |
| `#faf0e8` | 失败 badge 背景 |
| `#e8d4c4` | 失败 badge 边框 |
| `#8a6d3b` / `#f5f0e1` / `#e8dfc8` | DEBUG 标签 |

**macOS 窗口装饰（官网 Hero mockup 专用）**

- 红 `#ff5f57`、黄 `#febc2e`、绿 `#28c840`（各带半透明描边）

### 3.6 Markdown 内联代码（助手消息）

助手 variant 的 code/pre 暂用 Tailwind slate 系（`bg-slate-200/80`、`text-slate-800`），与主体暖灰 palette 略有偏差，属已知例外。

---

## 4. 字体与排版

### 4.1 字体栈

**桌面（`apps/desktop/src/index.css`）**

```css
"SF Pro Text", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif
```

**官网（`apps/web/src/index.css`）** — 在桌面栈基础上追加中文字体：

```css
"SF Pro Text", -apple-system, BlinkMacSystemFont, "Segoe UI",
"PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif
```

**等宽字体**：Tailwind `font-mono` — 用于步骤编号、API 配置值、代码块、调试日志。

### 4.2 字号阶梯

| 尺寸 | 用途 |
|------|------|
| `2.5rem` / `3.25rem`（sm+） | 官网 Hero H1 |
| `text-3xl` / `text-4xl` | 手册页 H1 |
| `text-2xl` / `text-3xl` | Section H2 |
| `text-[17px]` / `text-[18px]` | Hero 副标题 |
| `text-[16px]` | 手册导语 |
| `text-[15px]` | 卡片标题、CTA 按钮（大）、配置区 H3 |
| `text-[14px]` | 正文、按钮默认、用户/助手消息 |
| `text-[13px]` | 导航、表单输入、卡片描述、侧栏会话标题 |
| `text-[12px]` | 脚注、侧栏操作、快捷操作、调试摘要 |
| `text-[11px]` | SectionLabel、角色标签、消息角色标注 |
| `text-[10px]` | Badge、工具追踪标签 |

### 4.3 字重与行高

- 标题：`font-semibold`（600）或 `font-medium`（500，设置页标题）
- 正文：`leading-relaxed`（约 1.625）
- 标题字距：`tracking-tight`（大标题）、`tracking-[0.18em]`（SectionLabel 大写）
- 全局 `line-height: 1.5`（`:root`）

### 4.4 SectionLabel 模式

官网区块标签统一组件 `SectionLabel`：

```
text-[11px] uppercase tracking-[0.18em] text-[#8a8a84] font-medium mb-3
```

使用场景标签变体（使用场景卡片）：`tracking-[0.12em]`，非 uppercase 时仍保持小字距。

---

## 5. 布局与间距

### 5.1 内容宽度

| 容器 | 值 | 场景 |
|------|-----|------|
| `max-w-5xl` | 1024px | 官网 header/footer/各 section |
| `max-w-3xl` | 768px | 使用手册正文 |
| `max-w-2xl` | 672px | 桌面聊天消息区、输入区 |
| `max-w-md` | 448px | 设置模态框 |

水平内边距：官网/手册统一 `px-6`。

### 5.2 垂直节奏

- Section 上下：`py-20 sm:py-24`（大区块）、Hero `pt-16 pb-20 sm:pt-24 sm:pb-28`
- 卡片内边距：`p-5` / `p-6`
- 组件间距：`space-y-6`（消息列表）、`gap-5`（卡片网格）、`gap-3`（按钮组）

### 5.3 网格

- 特性卡片：`sm:grid-cols-2`
- 使用场景：`sm:grid-cols-2 lg:grid-cols-3`
- Hero：`lg:grid-cols-[1fr_380px]`
- 配置说明：`md:grid-cols-2`
- 角色条：`grid-cols-2 sm:grid-cols-4`

### 5.4 桌面应用布局

```
┌─────────────┬──────────────────────────┬─────────────┐
│ 侧栏 w-52   │      聊天主区域           │ Debug w-80  │
│ (可折叠 w-7)│      max-w-2xl 居中       │ (xl+ 显示)  │
└─────────────┴──────────────────────────┴─────────────┘
```

- 侧栏：`bg-[#f0f0ec]`，`border-r border-[#e8e8e4]`
- 折叠动画：`transition-[width] duration-200 ease-out`
- 调试面板：小屏底部 `h-48`，`xl` 及以上右侧固定宽

### 5.5 官网装饰

- **网格背景**：固定全屏、48×48px 网格线、`opacity-[0.35]`，自顶向下 `maskImage` 渐隐
- **Sticky Header**：`h-14`，`backdrop-blur-sm`，`bg-[#f7f7f5]/90`

---

## 6. 圆角、边框与阴影

### 6.1 圆角策略

**默认直角** — 按钮、卡片、输入框、模态框均无 `rounded-*`。

例外：

| 元素 | 圆角 |
|------|------|
| Logo 方块 | `rounded-lg`（8px） |
| Favicon | SVG `rx='8'` |
| macOS 窗口圆点 | `rounded-full` |
| Checkbox | `rounded` |
| Markdown 行内 code / pre | `rounded` / `rounded-lg` |
| 流式指示点 | `rounded-full` |

整体美学：**偏平面、编辑风**，非消费级圆角卡片风。

### 6.2 边框

- 标准容器：`border border-[#e8e8e4]`
- 输入/次级：`border border-[#dcdcd8]`
- 激活/选中：左侧 `border-l-2 border-[#1c1c1a]`（侧栏当前会话）
- 引用/助手消息：左侧 `border-l-2 border-[#dcdcd8] pl-4`
- Hover：`hover:border-[#ccc]`

### 6.3 阴影

极少使用。仅官网 Hero 聊天 mockup：

```
shadow-[0_24px_48px_-12px_rgba(28,28,26,0.08)]
```

设置模态：`shadow-lg`（Tailwind 默认）。

---

## 7. 组件模式

### 7.1 按钮

**主按钮（Primary）**

```
px-5 py-2.5（或 px-6 py-3 大 CTA）
bg-[#1c1c1a] text-[#f7f7f5] text-[14px] font-medium
hover:bg-[#333] transition-colors
```

**次按钮（Secondary）**

```
border border-[#dcdcd8] bg-white/60 text-[14px]
hover:border-[#aaa] transition-colors
```

**文字按钮（Ghost）**

```
text-[12px] text-[#aaa] hover:text-[#5c5c58] transition-colors
disabled:opacity-40
```

**设置页保存**

```
px-4 py-1.5 text-[13px] bg-[#1c1c1a] text-[#fafaf8] hover:bg-[#3d3d3a]
```

**设置页测试连接**

```
px-4 py-1.5 text-[13px] border border-[#dcdcd8] text-[#5c5c58] hover:bg-[#f0f0ec]
```

### 7.2 链接

```
text-[#1c1c1a] underline underline-offset-2 hover:text-[#555] transition-colors
```

导航链接（无下划线）：`text-[#5c5c58] hover:text-[#1c1c1a]`。

手册目录链接：`text-[#5c5c58] hover:text-[#1c1c1a]`。

### 7.3 卡片

**标准内容卡片**

```
border border-[#e8e8e4] bg-[#fafaf8] p-6
hover:border-[#ccc] transition-colors
```

**使用场景卡片**（背景略深一层 `#f7f7f5`，内含白底引用块）。

**手册步骤卡片** `ManualStep`：`border border-[#e8e8e4] bg-[#fafaf8] p-6`。

### 7.4 表单输入

`.field-input`（桌面）：

```css
width: 100%;
border: 1px solid #dcdcd8;
background: white;
padding: 0.5rem 0.75rem;  /* py-2 px-3 */
font-size: 13px;
transition: border-color 0.15s;
/* focus: border-color #aaa, outline none */
```

聊天输入区：外层 `border border-[#dcdcd8] bg-white focus-within:border-[#aaa]`，textarea 透明底、`text-[14px]`。

敏感字段（API Key、Token）：追加 `font-mono`。

字段标签：`text-[12px] text-[#8a8a86]`。

### 7.5 模态框

```
overlay: fixed inset-0 z-30 bg-black/20
panel:   max-w-md mx-4 max-h-[85vh] overflow-y-auto
         bg-[#fafaf8] border border-[#dcdcd8] shadow-lg
```

点击 overlay 关闭，panel 内 `stopPropagation`。

### 7.6 可折叠面板（`<details>`）

用于 ToolTrace、DebugLogPanel、MCP 调用详情：

```
border border-[#e8e8e4] bg-[#fafaf8]
open 态背景可变为 white 或 border 加深为 #dcdcd8
summary: cursor-pointer, text-[12px], flex items-center gap-2
```

**Badge 标签**（工具/MCP/HTTP method）：

```
px-1 py-0.5 text-[10px] font-medium
text-[#5c5c58] bg-[#f0f0ec] border border-[#dcdcd8]
```

### 7.7 对话 UI（桌面）

与官网 mockup **有意区分**：

| 元素 | 桌面实际 | 官网 mockup |
|------|----------|-------------|
| 用户消息 | 右对齐纯文字，无气泡底色 | 深色 `#1c1c1a` 气泡 |
| 助手消息 | 左对齐 + 左边线引用风格 | 白底边框卡片 |
| 角色标注 | `text-[11px] text-[#aaa]`「你 / 回复」 | 无 |
| 空状态 | 简短文字提示 + 快捷操作 | — |

快捷操作：底部 `text-[12px]` 文字链，非 pill 按钮。

### 7.8 代码与配置展示

- 行内 code：`bg-[#f0f0ec] px-1.5 py-0.5`（手册）
- 配置表：`ConfigRow` — `dt` 灰色固定宽，`dd` 等宽字体
- 调试 CodeBlock：`text-[11px] font-mono bg-white border border-[#e8e8e4] px-2 py-1.5`

---

## 8. 动效

| 名称 | 定义 | 用途 |
|------|------|------|
| `fade-up` | opacity 0→1, translateY 12px→0, 0.6s ease-out | 官网 Hero 入场 |
| 延迟变体 | `0.1s` / `0.2s` delay | 分层动画 |
| `transition-colors` | 默认 150ms 级 | 按钮、链接、边框 hover |
| `transition-[width] duration-200 ease-out` | — | 侧栏折叠 |
| `transition-opacity` | — | 侧栏删除按钮 hover 显示 |
| `animate-pulse` | Tailwind 内置 | 流式状态指示点 |
| `scroll-behavior: smooth` | 官网 `:root` | 锚点跳转 |

**不做** page transition、复杂 spring 动画或大面积 motion。

---

## 9. 响应式断点

遵循 Tailwind 默认：

| 前缀 | 宽度 | 项目中的用法 |
|------|------|--------------|
| `sm:` | 640px | 导航显示、网格列数、手册缩进 |
| `md:` | 768px | 配置双列 |
| `lg:` | 1024px | Hero 双列、场景三列 |
| `xl:` | 1280px | 调试面板从底部移至右侧 |

移动端：主导航 `hidden sm:flex`（小屏仅 Logo + 隐式缺失汉堡菜单——当前未实现移动导航 drawer）。

---

## 10. 图标与品牌

- **Logo**：28×28（`w-7 h-7`）深色圆角方块 + 字母「T」`text-sm font-semibold`
- **Favicon**：内联 SVG，与 Logo 一致（`#1c1c1a` 底 + `#f7f7f5` 字）
- **图标库**：未使用 lucide/heroicons；折叠侧栏用 ASCII `«` / `»`，关闭用 `×`
- **语言**：`<html lang="zh-CN">`，日期 `toLocaleDateString("zh-CN")`

---

## 11. 无障碍与语义

- 装饰性网格：`aria-hidden`
- macOS 圆点：`aria-hidden`
- 侧栏按钮：`aria-label`（设置、展开/折叠）
- 外链：`target="_blank"` + `rel="noreferrer"` / `noopener noreferrer`
- 表单：Settings 使用 `<label>` 包裹字段；checkbox 原生 `<input type="checkbox">`

---

## 12. 文件对照

| 文件 | 职责 |
|------|------|
| `apps/web/src/index.css` | 官网全局字体、fade-up 动画、smooth scroll |
| `apps/desktop/src/index.css` | 桌面全局字体、`.field-input`、`.markdown` 排版 |
| `apps/web/src/components/SiteLayout.tsx` | 官网壳层（header/footer/网格背景） |
| `apps/web/src/components/SectionLabel.tsx` | 区块标签 |
| `apps/web/src/pages/HomePage.tsx` | 营销页各 section 样式范本 |
| `apps/web/src/pages/ManualPage.tsx` | 长文文档页样式范本 |
| `apps/desktop/src/App.tsx` | 桌面布局壳层 |
| `apps/desktop/src/components/Chat/ChatPanel.tsx` | 对话区交互范式 |
| `apps/desktop/src/components/Settings/SettingsPanel.tsx` | 表单与按钮范式 |

---

## 13. 扩展建议

新增 UI 时请遵循：

1. **优先复用已有 hex 值**，不要引入新的色相（尤其避免纯 blue/purple 系 accent）
2. **保持直角**；若需圆角，仅用于 badge/avatar 等小元素
3. **边框优先于阴影** 表达层级
4. **transition-colors** 作为默认 hover 反馈，避免 scale/translate hover
5. 新页面容器宽度对齐 `max-w-5xl`（营销）或 `max-w-2xl`（应用内容区）
6. 中文正文用 `leading-relaxed`，元信息用 `#8a8a84` 及以下
7. 若需 design token 化，建议从本文档 §3 色板提取为 CSS 变量，但 **web 与 desktop 须同步**

---

## 14. 色板速查（复制用）

```
背景:     #f7f7f5  #fafaf8  #f0f0ec  #ffffff
文字:     #1c1c1a  #2d2d2a  #3d3d3a  #5c5c58  #8a8a84  #aaa  #bbb  #ccc  #ddd
边框:     #e8e8e4  #dcdcd8  #e0e0dc  #ccc  #aaa
品牌:     #1c1c1a → hover #333 / #3d3d3a
警告:     #faf6ed  #e8dfc8  #5c4a2a  #8a7a5a
错误:     #8b4513  #faf0e8  #e8d4c4
```
