# Design

## Theme

**Light mode.** 高校科研管理人员白天在办公室使用，自然光 + 室内照明环境。需要长时间阅读的舒适度，因此整体明亮但不刺眼，暖色调底灰取代冷灰。

色调方向：**暖灰底 + 靛蓝点缀**。靛蓝（indigo）比标准 SaaS 蓝更内敛、更有学术感，同时保持专业可信。

## Color Palette

### Core
- **Primary (靛蓝)**: `#4C51BF` — 主按钮、链接、活跃状态、选中行
- **Primary light**: `#EEF2FF` — 悬停背景、选中行底色
- **Primary dark**: `#3730A3` — 按钮按下态

### Backgrounds
- **Page background**: `#F5F3F0` — 暖灰色底，比冷灰更舒适
- **Surface / Card**: `#FDFDFC` — 接近纯白，带极微暖色调
- **Nav / Header**: `#FDFDFC` — 与卡片统一

### Text
- **Primary text**: `#1E1B4B` — 深靛灰，不是纯黑
- **Regular text**: `#4B5563` — 正文阅读色
- **Secondary text**: `#9CA3AF` — 辅助信息、占位文字
- **Placeholder**: `#D1D5DB` — 输入框占位

### Borders
- **Default border**: `#E8E4DF` — 暖灰边框
- **Light border**: `#F0EDE9` — 分割线

### Semantic
- **Success**: `#059669` — 翠绿，通过、已归档
- **Warning**: `#D97706` — 琥珀，待审核
- **Danger**: `#DC2626` — 红，驳回、删除
- **Info**: `#6B7280` — 灰，草稿、中性信息

### Shadows
- `shadow-sm`: `0 1px 2px rgba(30, 27, 75, 0.04)` — 卡片默认
- `shadow-md`: `0 4px 12px rgba(30, 27, 75, 0.06)` — 卡片悬停
- `shadow-lg`: `0 8px 24px rgba(30, 27, 75, 0.08)` — 下拉、弹窗

## Typography

- **Font family**: `-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif`
- **Scale** (Major Third, 1.25 ratio):
  - `text-xs`: 0.75rem (12px) — 标签、辅助
  - `text-sm`: 0.875rem (14px) — 表格、表单
  - `text-base`: 1rem (16px) — 正文
  - `text-lg`: 1.125rem (18px) — 卡片标题
  - `text-xl`: 1.25rem (20px) — 页面标题
  - `text-2xl`: 1.5rem (24px) — 区块标题
  - `text-3xl`: 1.875rem (30px) — 统计数据
- **Font weights**: 400 (regular), 500 (medium), 600 (semibold), 700 (bold)
- **Line height**: 1.6 for body, 1.3 for headings, 1.5 for table cells

## Layout

- **Max content width**: 1280px（比原 1200px 略宽，给表格更多空间）
- **Page padding**: 28px（比原 24px 略大，更透气）
- **Card padding**: 24px（比原 20px 略大）
- **Card radius**: 8px（比原 12px 更克制）
- **Nav height**: 56px

## Spacing Scale (4px base)

- `xs`: 4px, `sm`: 8px, `md`: 16px, `lg`: 24px, `xl`: 32px, `2xl`: 48px

## Components

- **卡片**：无顶边彩色条，用标题颜色和内容区分层级。悬停时微提阴影。
- **表格**：去掉纵向分割线，用横向浅线分隔行。表头用 medium 字重。悬停行淡蓝底。
- **按钮**：主按钮用靛蓝实底白字。次按钮用白底 + 靛蓝边框。链接按钮用靛蓝文字。
- **标签 (Tag)**：圆角 pill 形状，浅底色 + 深色文字，不用高饱和色。
- **输入框**：圆角 6px，聚焦时靛蓝边框 + 淡蓝阴影。
- **导航**：顶部水平导航，logo 文字（不用 emoji），活跃项有一抹底线指示。
- **分页**：右侧对齐，简洁数字按钮。

## Motion

- `transition-duration`: 150ms (micro), 250ms (standard), 350ms (entrance)
- `transition-timing`: `cubic-bezier(0.4, 0, 0.2, 1)` (ease-out)
- 页面切换用淡入淡出，不位移
- 不编排页面加载序列动画
