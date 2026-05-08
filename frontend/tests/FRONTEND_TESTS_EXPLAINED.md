# Frontend Tests Explained

## 1. 这套前端测试在做什么

本项目前端测试使用 `Jest + jsdom`，目标是验证：

- 页面脚本在正常流程下是否按预期工作。
- 边界输入（空值、缺字段、缺 DOM、取消操作）是否处理正确。
- 异常流程（接口失败、权限失败）是否有正确提示或重定向。
- 输出内容是否安全（如 `escapeHtml` 防注入）。
- 管理端和用户端关键业务流程是否可回归验证。

---

## 2. 当前测试文件结构

```text
frontend/tests/
  module1/module1.test.js
  module2/admin-approval.test.js
  module3/module3.test.js
  module5/review-approval.test.js
  module6/module6.test.js
  module7/admin-dashboard.test.js
  module7/admin-resources.test.js
  module7/classification-management.test.js
  module7/module7.test.js
  module7/tag-management.test.js
  shared/shared.test.js
  test-utils/eval-with-coverage.js
```

---

## 3. 各测试文件具体验证内容

### `tests/shared/shared.test.js`

- 查询参数读取：`getQueryMessage`
- 网络请求包装：`requestJson`（成功、非 JSON、网络异常、业务异常）
- 统一提示：`showToast` / `showMessageFromQuery`
- 退出登录事件绑定：`bindLogoutButtons`
- 公共工具函数：`escapeHtml`、`formatDateTime`、`setText`、`setValue`

### `tests/module1/module1.test.js`（登录/注册/账户）

- 登录后跳转路径净化与白名单校验
- 枚举状态文案与账户状态文案生成
- 注册验证码冷却倒计时
- 管理员待审核列表加载与渲染
- 首页登录状态卡片渲染
- 账户页贡献者状态区域渲染
- 注册页和登录页关键初始化流程（含校验与异常提示）

### `tests/module2/admin-approval.test.js`（管理员贡献者审批）

- 审批页 Tab 切换与事件绑定
- 待审批列表与已批准列表加载
- 详情区域渲染
- 审批通过/拒绝提交流程
- 撤销贡献者资格流程
- 常见异常分支（取消、接口失败、空数据）

### `tests/module3/module3.test.js`（资源编辑工作台）

- 标签、分类、资源类型的标准化与去重
- 下拉选项渲染与兜底逻辑
- 分类/资源类型加载流程
- 元数据保存、自动保存、提交审核流程
- 文件上传触发链路
- 页面会话信息与查询参数解析

### `tests/module5/review-approval.test.js`（资源审核）

- 待审核资源列表加载与卡片渲染
- 资源详情、媒体区、审核历史渲染
- 审核通过/拒绝提交
- 正常与异常分支（空列表、接口失败、取消）

### `tests/module6/module6.test.js`（游客浏览与反馈）

- 游客端筛选项加载与筛选重置
- 已审核资源列表与详情渲染
- 评论提交、评论删除、评论错误展示
- 反馈历史加载与渲染
- 资源标签/媒体预览渲染
- 鉴权与统一错误处理分支

### `tests/module7/module7.test.js`（管理端公共层）

- `bindAdminBasics` 退出登录绑定配置
- `requireAdmin` 鉴权逻辑
- 401 场景跳转登录并携带 `next`
- 非管理员/异常场景的访问提示渲染
- 状态标签、空行模板、错误消息工具函数
- `jsonRequest` 请求头拼装逻辑

### `tests/module7/admin-dashboard.test.js`

- `DOMContentLoaded` 初始化流程
- 管理员欢迎文案渲染
- 无用户时提前返回
- 缺少目标 DOM 节点时不抛错

### `tests/module7/admin-resources.test.js`

- 资源状态标准化与过滤
- 资源列表加载/渲染/空态/错态
- 归档/取消归档动作
- 取消确认、接口失败等异常分支

### `tests/module7/classification-management.test.js`

- 资源类型与分类数据加载
- 概览、使用历史、操作历史渲染
- 新增、编辑、启用/禁用流程
- 空态、取消、失败分支处理

### `tests/module7/tag-management.test.js`

- 标签数据与历史数据加载
- 列表/概览/历史渲染
- 新增、编辑、启用/禁用流程
- 空态与异常分支处理

---

## 4. `test-utils/eval-with-coverage.js` 的作用

很多页面脚本不是模块化导出，而是直接在浏览器环境运行。  
该工具会在 `window.eval` 前做覆盖率插桩，让这些脚本执行路径能被 Jest 覆盖率正确统计。

---

## 5. CI 里如何跑前端测试

在 `.github/workflows/ci-cd.yml` 的 `frontend-check` job 中，前端流程包括：

- `npm ci`
- `npm run lint`
- `npm run format:check`
- `npm run test:coverage -- --runInBand --json --outputFile=jest-results.json`
- 汇总并输出：
  - 指令覆盖率（Statements）
  - 分支覆盖率（Branches）
  - 平均每条用例执行时间
  - 测试通过率
- 上传覆盖率报告与测试日志 artifact

---

## 6. 如何本地运行

```bash
cd frontend
npm ci
npm run lint
npm run format:check
npm run test -- --runInBand
npm run test:coverage -- --runInBand
```

---

## 7. 如果要继续补测，优先顺序建议

- 优先补 `module3.js` 的低覆盖分支（长流程、分支多）
- 再补 `module6.js` 的异常路径和权限分支
- 每次新增功能同时补充：
  - 1 条正常路径
  - 1 条边界路径
  - 1 条异常路径

