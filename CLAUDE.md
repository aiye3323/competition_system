# 竞赛科研管理系统 - Claude 工作指引

## 重要说明
本文件是 Claude Code 的入口指引文件。  
**所有详细规范、设计文档、执行步骤均存放在 `/docs` 文件夹中**，请优先参考。

---

## 一、标准文件路径索引

| 文档类型 | 路径 | 说明 |
|---------|------|------|
| 需求确认文档 | `/docs/需求确认文档.md` | 用户确认的功能需求清单 |
| 技术选型说明 | `/docs/技术选型说明.md` | 前后端技术栈及选型理由 |
| 数据库设计规范 | `/docs/数据库设计规范.md` | 表结构、字段类型、命名规范 |
| 产品设计策略 | `/PRODUCT.md` | 用户画像、品牌个性、设计原则（Impeccable） |
| 视觉设计系统 | `/DESIGN.md` | 配色/字体/布局/组件/动效规范（Impeccable） |
| 文件命名规范 | `/命名规范.md` | 四种成果类型文件命名规则 |
| 开发步骤计划 | `/开发步骤计划.md` | 7个阶段详细任务分解（项目根目录） |
| 接口设计规范 | `/docs/接口设计规范.md` | RESTful API 设计约定 |
| 开发日志目录 | `/开发日志/` | 每天自动记录完成/待办事项 |
| CLAUDE.md 入口 | `/CLAUDE.md` | 本文件，Claude Code 工作指引 |

---

## 二、开发工作总则


1. **每次回答必须遵循**：
   - 给出完整代码 + 具体操作路径（哪个文件、第几行）
   - 明确告知用户”在哪个软件/按钮/命令下执行”
   - 不解释原理，除非用户主动问

2. **文件组织原则**：
   - 后端代码 → `/backend/`
   - 前端代码 → `/frontend/`
   - 文档 → `/docs/`
   - 日志 → `/开发日志/`

3. **自动记录规则（边做边记，无需确认）**：
   每完成一个子任务（写完一个文件、调通一个接口、修复一个bug、完成一个模块），**立即自动**写入以下文档，**不需要等用户说”验证通过”**：

   | 文档路径 | 更新内容 |
   |---------|------|
   | `/开发日志/YYYY-MM-DD.md` | 追加：文件名、操作、说明、完成时间 |
   | `/docs/开发步骤计划.md` | 更新对应任务状态为”已完成” |
   | `/docs/执行步骤.md` | 更新执行进度 |
   | `/docs/需求确认文档.md` | 需求变更时同步 |
   | `/docs/技术选型说明.md` | 技术调整时同步 |
   | `/docs/数据库设计规范.md` | 表结构变更时同步 |
   | `/docs/接口设计规范.md` | 接口变更时同步 |

   **执行流程**：
   - 写代码前：列出文件清单，等用户确认”开始”
   - 写代码中：每完成一个文件，立即记录到开发日志
   - 完成子任务后：自动更新所有相关文档
   - 回复格式：**”已完成【任务名称】，已自动更新所有相关文档”**

---

## 三、当前项目状态

- 项目名称：竞赛科研管理系统
- 技术栈：Spring Boot 3.2.5 + Vue 3 + MySQL + Element Plus + JWT
- 开发阶段：第7阶段（收尾 + 部署）进行中（7.1~7.6 已完成，7.7 部署待开始）
- 数据库名：competition_db
- MySQL 配置：root / 123456（用户请根据实际情况修改）

### 已实现模块
- 用户认证：注册/登录/JWT/角色权限
- 竞赛成果：提交/列表/详情/编辑/删除 + 文件上传
- 审核流程：科研秘书初审 → 学院领导终审（PENDING→PENDING_LEADER→ARCHIVED）
- 系统通知：审核操作触发通知 + 通知铃铛（60s轮询）+ 通知中心
- 创新项目：提交/列表/详情/编辑/删除 + 审核流程
- 学术论文：提交/列表/详情/编辑/删除 + 审核流程
- 数据概览：Dashboard 仪表盘（ECharts 图表 + 统计卡片 + 最近成果）+ 全院成果公开展示
- Excel 导出：Apache POI 流式生成，支持多条件筛选导出
- 软件著作权：提交/列表/详情/编辑/删除 + 审核流程
- 文件管理：FileInfo 元数据 DTO、原始文件名显示、分类别上传提示
- 全屏预览：FullscreenPreview.vue（图片缩放拖拽 + PDF 内嵌 + 键盘快捷键）
- 附件下载：自定义文件名（类别_项目_作者.pdf）

---

## 四、角色权限定义

- 学生/教师：注册、提交成果、查看审核状态
- 科研秘书：审核成果、填写意见、提交领导
- 学院领导：最终审核、归档
- 系统管理员：用户管理、系统日志

---

## 五、工作流程规范

1. **Plan 模式优先**：非简单修改任务（涉及 3+ 文件或架构决策）先启用 plan 模式
2. **需求确认**：代码改动前与用户确认需求理解无误
3. **文件确认**：修改前先 Read 确认当前文件内容
4. **后写代码**：确认以上步骤后再 Edit/Write
5. **日志同步**：每步完成后自动记录到 `/开发日志/YYYY-MM-DD.md`
6. **文档同步**：重要改动同步更新 `/docs/` 下对应文档
7. **默认中文输出**：所有沟通使用简体中文

### 工作流程总结

```
你说"开始任务"
    ↓
我列出文件清单
    ↓
你说"开始"
    ↓
我写代码 → 自动记录日志 → 自动更新文档
    ↓
我完成任务 → 自动 Git 提交推送
    ↓
我回复"已完成【任务】，已更新文档并推送到仓库"
```

## 六、重要提醒（给 Claude）

- 当用户要求”继续”或”下一步”时，请先查看 `/开发日志/` 中最新日期的文件，确认进度
- 当需要查询设计规范时，优先读取 `/docs/` 下对应文档
- **禁止**在根目录随意创建散落文件，所有代码必须放入 `/backend` 或 `/frontend`

---

## 七、Git 自动提交规则

### 仓库地址
- **HTTPS**: `https://github.com/aiye3323/competition_system`
- **SSH**: `git@github.com:aiye3323/competition_system.git`

### 触发时机
每次完成一个任务模块后，自动执行 Git 提交并推送。

### 提交命令
```bash
git add .
git commit -m "【类型】: 简短描述"
git push origin main
```

### 提交信息格式（中文）

| 类型 | 格式示例 |
|------|---------|
| 新功能 | `【新功能】完成软件著作权模块` |
| 优化 | `【优化】修复文件上传文字遮挡` |
| 修复 | `【修复】解决预览窗口报错` |
| 文档 | `【文档】更新开发日志` |
| 设计 | `【设计】Impeccable 重设计前端全局样式` |

### 首次配置（如未配置）
```bash
git init
git remote add origin https://github.com/aiye3323/competition_system.git
git branch -M main
```

### .gitignore 文件
如不存在，自动创建并包含：
```
node_modules/
.idea/
*.log
.DS_Store
*.class
target/
**/target/
**/application-local.properties
```

### 不提交的情况
- 代码有编译错误
- 测试未通过
- 用户说"先不要提交"

### 提交后回复
**"已提交代码到仓库：https://github.com/aiye3323/competition_system"**

---

## 八、CSS 调试升级规则

**如果 CSS 样式问题（文字截断、溢出、定位）尝试 2 次仍然无效，立即停止纯 CSS 方案，改用以下之一：**

1. **JavaScript 方案**：动态测量元素宽度，用 JS 截断文本
2. **HTML 重构**：改变组件 DOM 结构（如把提示移到父组件外侧）
3. **设计妥协**：与用户确认是否可以接受折中方案（固定宽度 vs 自适应）

禁止重复尝试同一类 CSS 方案的变体（`deep` → `inline` → `global` → `reposition`）。

---

## 九、前后端连通性检查清单

调试前后端通信问题前，**依次验证以下 5 层**：

| 层 | 检查命令/方法 | 常见问题 |
|----|-------------|---------|
| 网络绑定 | `netstat -ano \| grep <port>` | Vite 仅绑定 IPv6、端口被占用 |
| 代理配置 | 读 `vite.config.js` proxy 配置 | 代理路径不匹配后端路由 |
| CORS | `curl -v -H 'Origin: <frontend>' <backend>/api/test` | origin 不在白名单 |
| 数据库 Schema | `SHOW COLUMNS FROM <table>` vs JPA 实体字段 | 残留旧字段如 `f_user_id` |
| 数据值 | `SELECT DISTINCT <col> FROM <table>` vs 前端常量 | 值不匹配如 `A` vs `A类` |

**前置检查脚本**（调试前先跑一遍）：
```bash
netstat -ano | grep 8080 | grep LISTENING  # 后端端口
curl -s http://localhost:5173 | head -1      # 前端可访问
curl -s http://localhost:8080/api/test       # 后端可访问
```

---

## 十、中文路径与编码

涉及含中文文件路径时：
- `application.properties` 中**不要写含中文的绝对路径**，用 `./相对路径` 替代
- Java 代码中用 `Paths.get(...).toAbsolutePath()` 动态解析
- 用 `FileSystemResource` 替代 `UrlResource` 避免 URI 编码问题
- 文件下载时对 `filename` 参数做 `replaceAll("[\"\\n\\r]", "_")` 防注入

---

## 十一、模块化架构 & 扩展指南

### 新增科研成果类型（如专利、获奖等）步骤：

| 步骤 | 文件 | 操作 |
|------|------|------|
| 1 | `backend/.../common/AchievementType.java` | 枚举添加类型常量 |
| 2 | `backend/.../entity/` | 创建 Entity 类（参考 Competition.java） |
| 3 | `backend/.../repository/` | 创建 Repository 接口 |
| 4 | `backend/.../service/` | 创建 Service 类 |
| 5 | `backend/.../controller/` | 创建 Controller 类 |
| 6 | `backend/.../common/AchievementServiceRegistry.java` | 注册新 Repository |
| 7 | `frontend/src/utils/uploadConfig.js` | 添加上传区域配置 |
| 8 | `frontend/src/utils/fileNaming.js` | TYPE_LABEL 添加映射 |
| 9 | `frontend/src/router/index.js` | 添加路由 |
| 10 | `frontend/src/views/` | 创建列表/详情/提交页面 |

### 预留接口

| 接口 | 说明 | 扩展方式 |
|------|------|---------|
| `FileEntity.relatedType` | 文件关联的成果类型 | 新增类型字符串即可 |
| `AchievementType` 枚举 | 统一类型管理 | 添加枚举常量 |
| `AchievementServiceRegistry` | 类型→Repository 映射 | switch 添加分支 |
| `uploadConfig.js` | 前端上传区域配置 | 添加新数组 |
| `fileNaming.js TYPE_LABEL` | 命名类型映射 | 添加键值对 |
| `/api/statistics/dashboard` | 统计接口 | 扩展查询逻辑 |
| JWT `role` 字段 | 角色权限 | 新增角色 + SecurityConfig 配置