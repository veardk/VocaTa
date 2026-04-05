# Refactor Preparation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a minimal-risk preparation baseline for the upcoming refactor by first stabilizing configuration, validation, workflow rules, and structural boundaries across `vocata-server`, `vocata-web`, and `vocata-admin`.

**Architecture:** This plan avoids business-feature rewrites. It treats refactor preparation as a documentation-and-guardrail project: freeze the current baseline, normalize configuration entry points, lock validation commands, narrow CI behavior, and define structure boundaries before any module-level refactor starts.

**Tech Stack:** Spring Boot 3.1.4, Java 17, Maven, Vue 3, TypeScript, Vite, ESLint, vue-tsc, Docker Compose, GitHub Actions

---

## File Map

**Create:**
- `docs/开发环境说明.md` — single source of truth for local startup, required config, optional config, and common startup failures.
- `docs/部署环境说明.md` — server layout, `.env` location, staging deployment, rollback path.
- `docs/开发工作流.md` — personal branch flow, merge discipline, pre-push checks, forbidden mixed commits.
- `docs/提交规范.md` — conventional commit usage for this repo with examples.
- `docs/验证清单.md` — exact validation commands for server, web, admin, Docker, and staging checks.
- `docs/重构边界清单.md` — per-module allowed changes, forbidden changes, and directory ownership rules for phase 1.

**Modify:**
- `README.md` — replace outdated startup/deployment guidance with links to the new docs.
- `.env.example` — align naming and comments with the actual development baseline.
- `docs/Docker开发环境.md` — narrow to Docker-specific concerns and link to the main environment doc.
- `docs/部署文档.md` — either fold into the new deployment doc or reduce to a redirect note.
- `.github/workflows/ci.yml` — keep CI scoped to relevant modules and ensure validation steps reflect the agreed baseline.
- `.github/workflows/cd-staging.yml` — confirm staging deploy assumptions match the new docs-only deployment model.

**Inspect Only:**
- `.env`
- `docker-compose.yml`
- `vocata-web/package.json`
- `vocata-admin/package.json`
- `vocata-server/pom.xml`
- `CLAUDE.md`

---

### Task 1: Freeze The Current Baseline

**Files:**
- Create: `docs/验证清单.md`
- Modify: `README.md`
- Inspect: `.env`, `docker-compose.yml`, `.github/workflows/ci.yml`, `.github/workflows/cd-staging.yml`

- [ ] **Step 1: Capture the current runnable baseline**

Run:

```bash
git status --short --branch
docker compose ps
curl -fsS http://127.0.0.1:9009/api/health
curl -fsS "http://127.0.0.1:9009/api/open/character/list?pageNum=1&pageSize=2"
```

Expected:
- Git branch is `develop`
- Docker services for postgres, redis, server, web, and admin are visible if local stack is up
- Health endpoint returns success
- Character list endpoint returns success

- [ ] **Step 2: Write the validation checklist with exact commands**

Create `docs/验证清单.md` with this initial structure:

~~~markdown
# 验证清单

## 后端

```bash
cd vocata-server
mvn -Dmaven.repo.local=/tmp/juhao_m2repo -Dmaven.test.skip=true package
```

## 用户端

```bash
cd vocata-web
npm run lint
npm run type-check
npm run build
```

## 管理端

```bash
cd vocata-admin
npm run lint
npm run type-check
npm run build
```

## Docker 开发环境

```bash
docker compose config -q
docker compose ps
```

## Staging 部署后检查

```bash
curl -fsS http://127.0.0.1:9009/api/health
```
~~~

- [ ] **Step 3: Add README pointers instead of duplicating startup knowledge**

Update `README.md` to link to:

```markdown
## 开发与部署入口

- 开发环境说明：`docs/开发环境说明.md`
- 部署环境说明：`docs/部署环境说明.md`
- 验证清单：`docs/验证清单.md`
- 开发工作流：`docs/开发工作流.md`
- 提交规范：`docs/提交规范.md`
- 重构边界清单：`docs/重构边界清单.md`
```

- [ ] **Step 4: Verify the docs-only baseline changes**

Run:

```bash
git diff --check
```

Expected: no output

- [ ] **Step 5: Commit**

```bash
git add README.md docs/验证清单.md
git commit -m "docs: record validation baseline"
```

---

### Task 2: Normalize Development Configuration Documentation

**Files:**
- Create: `docs/开发环境说明.md`
- Modify: `.env.example`, `docs/Docker开发环境.md`
- Inspect: `.env`, `docker-compose.yml`, `vocata-server/src/main/resources/application.yml`

- [ ] **Step 1: Diff the real `.env` against `.env.example`**

Run:

```bash
diff -u .env.example .env || true
```

Expected:
- Differences identify missing comments, stale keys, or naming drift
- No direct secret values should be copied from `.env` into `.env.example`

- [ ] **Step 2: Write the development environment guide**

Create `docs/开发环境说明.md` with this structure:

```markdown
# 开发环境说明

## 启动入口

- 根目录 `.env`
- `docker-compose.yml`
- 后端 `application.yml`
- 前端各自 `package.json` 脚本

## 启动必需配置

- PostgreSQL
- Redis
- 七牛存储
- 当前启用的 AI/TTS provider

## 功能可选配置

- 邮件
- 非默认 AI provider

## 启动顺序

1. `docker compose up -d postgres redis`
2. 启动后端
3. 启动 `vocata-web`
4. 启动 `vocata-admin`

## 常见问题

- 端口冲突
- `.env` 未同步
- 第三方 key 缺失
- 数据库备份未恢复
```

- [ ] **Step 3: Align `.env.example` comments with actual runtime behavior**

Update `.env.example` so that each section follows this pattern:

```env
# PostgreSQL development container port
POSTGRES_PORT=55433

# Redis development container port
REDIS_EXPOSE_PORT=6380

# Frontend API base used at build time
VITE_APP_URL=/api
```

Rule:
- keep placeholder values non-sensitive
- keep comments operational, not marketing
- remove deprecated key names

- [ ] **Step 4: Reduce Docker doc duplication**

Update `docs/Docker开发环境.md` so the top section becomes:

```markdown
> 本文只说明 Docker 开发编排本身。
> 环境变量、启动顺序、第三方配置说明统一见 `docs/开发环境说明.md`。
```

- [ ] **Step 5: Verify the config documentation changes**

Run:

```bash
git diff --check
docker compose config -q
```

Expected:
- no diff formatting issues
- Docker config validates successfully

- [ ] **Step 6: Commit**

```bash
git add .env.example docs/开发环境说明.md docs/Docker开发环境.md
git commit -m "docs: normalize development environment guidance"
```

---

### Task 3: Document Deployment And Personal Workflow

**Files:**
- Create: `docs/部署环境说明.md`, `docs/开发工作流.md`, `docs/提交规范.md`
- Modify: `docs/部署文档.md`
- Inspect: `.github/workflows/cd-staging.yml`, `.github/workflows/ci.yml`

- [ ] **Step 1: Write the deployment environment guide**

Create `docs/部署环境说明.md` with this structure:

```markdown
# 部署环境说明

## 服务器目录约定

- `/home/deploy/deploy/vocata/repo`
- `/home/deploy/deploy/vocata/.env`

## Staging 部署方式

- push `develop`
- GitHub Actions 通过 SSH 登录
- 服务器本地 `git pull`
- `docker compose up -d --build`

## 回滚方式

- 回退到上一个 commit
- 重新执行 staging workflow

## 故障定位入口

- `docker compose ps`
- `docker compose logs`
- `curl http://127.0.0.1:9009/api/health`
```

- [ ] **Step 2: Write the personal workflow guide**

Create `docs/开发工作流.md` with this structure:

```markdown
# 开发工作流

## 分支规则

- `develop` 只接收已验证改动
- 功能和治理任务从 `feature/*` 分支开发

## 提交规则

- 一个提交只解决一类问题
- 禁止环境、结构、业务逻辑混改

## 合并前检查

- 后端验证
- 用户端验证
- 管理端验证
- 需要时验证 Docker 和 staging
```

- [ ] **Step 3: Write the commit conventions guide**

Create `docs/提交规范.md` with this structure:

```markdown
# 提交规范

## 类型

- `docs:`
- `chore:`
- `fix:`
- `refactor:`
- `test:`

## 示例

- `docs: add deployment environment guide`
- `chore: normalize environment variable naming`
- `refactor: remove duplicate frontend utility types`
```

- [ ] **Step 4: Turn old deployment doc into a redirect or trimmed legacy note**

Update `docs/部署文档.md` top section to:

```markdown
# 部署文档

> 当前有效部署说明统一见 `docs/部署环境说明.md`。
> 本文仅保留历史背景或补充说明，不再作为执行入口。
```

- [ ] **Step 5: Verify link consistency**

Run:

```bash
rg -n "开发环境说明|部署环境说明|开发工作流|提交规范" README.md docs
git diff --check
```

Expected:
- new docs are referenced
- no formatting errors

- [ ] **Step 6: Commit**

```bash
git add docs/部署环境说明.md docs/开发工作流.md docs/提交规范.md docs/部署文档.md
git commit -m "docs: add personal development workflow guides"
```

---

### Task 4: Lock Validation Commands Into CI Expectations

**Files:**
- Modify: `.github/workflows/ci.yml`, `docs/验证清单.md`
- Inspect: `vocata-web/package.json`, `vocata-admin/package.json`, `vocata-server/pom.xml`

- [ ] **Step 1: Confirm current runnable validation commands**

Run:

```bash
cd vocata-web && npm run lint && npm run type-check && npm run build
cd ../vocata-admin && npm run lint && npm run type-check && npm run build
cd ../vocata-server && mvn -Dmaven.repo.local=/tmp/juhao_m2repo test
```

Expected:
- frontends complete lint/type-check/build
- backend test command either passes or exposes the exact minimum gap to document

- [ ] **Step 2: Update `docs/验证清单.md` if backend command needs a staged baseline**

If full `mvn test` is too unstable, change backend section to:

~~~markdown
## 后端当前基线

```bash
cd vocata-server
mvn -Dmaven.repo.local=/tmp/juhao_m2repo -Dmaven.test.skip=true package
```

## 后端下一阶段目标

```bash
cd vocata-server
mvn -Dmaven.repo.local=/tmp/juhao_m2repo test
```
~~~

- [ ] **Step 3: Align CI job descriptions with the agreed baseline**

In `.github/workflows/ci.yml`, ensure:
- server job explains whether it is running compile/package vs. test baseline
- web/admin jobs explicitly run `lint`, `type-check`, `build`
- path filters do not trigger all modules for unrelated doc-only changes

Use this edit pattern in comments or step names:

```yaml
- name: 运行代码检查
  run: npm run lint

- name: 运行类型检查
  run: npm run type-check

- name: 构建应用
  run: npm run build
```

- [ ] **Step 4: Verify the CI baseline changes**

Run:

```bash
git diff --check
```

Expected: no output

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/ci.yml docs/验证清单.md
git commit -m "ci: align validation baseline with local workflow"
```

---

### Task 5: Define Refactor Boundaries Before Any File Moves

**Files:**
- Create: `docs/重构边界清单.md`
- Inspect: `vocata-server/src`, `vocata-web/src`, `vocata-admin/src`

- [ ] **Step 1: Map each module to directory responsibilities**

Create `docs/重构边界清单.md` with this table skeleton:

```markdown
# 重构边界清单

## vocata-server

| 目录 | 职责 | 第一阶段是否允许改动 |
| --- | --- | --- |
| `controller` | API 入口 | 仅小修 |
| `service` | 业务编排 | 仅小修 |
| `config` | 配置与装配 | 允许 |
| `common` | 通用能力 | 允许 |

## vocata-web

| 目录 | 职责 | 第一阶段是否允许改动 |
| --- | --- | --- |
| `src/api` | 接口调用 | 允许 |
| `src/views` | 页面 | 仅小修 |
| `src/types` | 类型 | 允许 |
| `src/utils` | 工具 | 允许 |

## vocata-admin

| 目录 | 职责 | 第一阶段是否允许改动 |
| --- | --- | --- |
| `src/api` | 接口调用 | 允许 |
| `src/views` | 页面 | 仅小修 |
| `src/types` | 类型 | 允许 |
| `src/utils` | 工具 | 允许 |
```

- [ ] **Step 2: Add explicit first-phase constraints**

Append this section:

```markdown
## 第一阶段允许改动

- 配置命名统一
- 文档收口
- 验证命令固定
- 死代码删除
- 明显重复类型和工具抽取

## 第一阶段禁止改动

- 大规模搬迁目录
- 一次性重写页面
- 一次性替换 AI provider 结构
- 数据库大规模重设计
```

- [ ] **Step 3: Verify boundaries doc is concrete enough**

Run:

```bash
rg -n "允许改动|禁止改动|vocata-server|vocata-web|vocata-admin" docs/重构边界清单.md
git diff --check
```

Expected:
- all three modules are covered
- allowed/forbidden sections exist
- no formatting errors

- [ ] **Step 4: Commit**

```bash
git add docs/重构边界清单.md
git commit -m "docs: define phase-one refactor boundaries"
```

---

### Task 6: Prepare The First Week Execution Queue

**Files:**
- Modify: `docs/开发工作流.md`, `docs/验证清单.md`, `docs/重构边界清单.md`
- Create: optional `docs/第一周执行清单.md`

- [ ] **Step 1: Convert the approved design into a one-week checklist**

Create `docs/第一周执行清单.md` with this structure:

```markdown
# 第一周执行清单

## Day 1
- 冻结基线
- 记录命令

## Day 2
- 收口环境说明
- 校正 `.env.example`

## Day 3
- 固定验证清单
- 收窄 CI 范围

## Day 4
- 完成开发工作流和提交规范

## Day 5
- 完成重构边界清单
```

- [ ] **Step 2: Cross-link the preparation docs**

Add a short “相关文档” section to:
- `docs/开发工作流.md`
- `docs/验证清单.md`
- `docs/重构边界清单.md`

Pattern:

```markdown
## 相关文档

- `docs/开发环境说明.md`
- `docs/部署环境说明.md`
- `docs/提交规范.md`
```

- [ ] **Step 3: Final verification before execution starts**

Run:

```bash
git diff --check
git status --short
```

Expected:
- only the intended docs/CI files are changed
- no accidental edits to business code

- [ ] **Step 4: Commit**

```bash
git add docs/第一周执行清单.md docs/开发工作流.md docs/验证清单.md docs/重构边界清单.md
git commit -m "docs: add first-week refactor preparation checklist"
```

---

## Spec Coverage Check

- 配置与环境治理：Task 2, Task 3
- 测试与验证基线：Task 1, Task 4
- 结构边界定义：Task 5
- 个人开发流程固定：Task 3, Task 6
- 最小改动提交：all tasks use docs/ci/config-only commits and avoid mixed business changes
- 三端统一治理、分端落地：Task 5 documents boundaries; subsequent refactor entry starts only after these tasks complete

## Placeholder Scan

No placeholder markers or undefined follow-up wording are left in the plan. Each task names exact files, commands, expected results, and commit messages.

## Type / Name Consistency Check

- Uses `STAGING_SSH_KEY`, `STAGING_HOST`, `STAGING_USER` consistently with the current staging workflow
- Uses `lint`, `type-check`, `build` consistently with current frontend `package.json`
- Uses `mvn -Dmaven.repo.local=/tmp/juhao_m2repo` consistently with the current local Maven setup
