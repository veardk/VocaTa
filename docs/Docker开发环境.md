# Docker 开发环境

当前仓库先以开发环境为中心整理 Docker 结构。

## 目标

- `docker-compose.yml` 只服务本地开发和联调
- `docker-compose.test.yml`、`docker-compose.prod.yml` 先保留为后续测试/生产预留文件
- 前端容器走接近发布态的静态构建，不提供热更新

## 使用方式

1. 复制环境变量模板

```bash
cp .env.example .env
```

2. 按需补充 `.env`

- 基础开发默认值已经可用：PostgreSQL、Redis、端口映射
- 开发数据库默认使用 `pgvector/pgvector:pg17`，便于恢复现有 PostgreSQL 17 + pgvector 备份
- 若要完整启动后端，仍需补第三方配置，例如七牛和 AI 相关密钥

3. 启动核心开发环境

```bash
docker compose up -d --build
```

4. 启动可选工具

```bash
docker compose --profile tools up -d
```

## 当前开发环境包含

- `postgres`：开发数据库
- `redis`：开发缓存
- `vocata-server`：后端服务
- `vocata-web`：用户端静态前端
- `vocata-admin`：管理端静态前端
- `pgadmin`：可选数据库管理工具
- `mailhog`：可选邮件调试工具

## 约定

- 后端健康检查统一为 `/api/health`
- 前端容器内部统一监听 `8080`
- 前端 API 地址通过构建期变量 `VITE_APP_URL` 注入
- 七牛对象前缀可通过 `QINIU_KEY_PREFIX` 配置，例如 `Vocata`

## 说明

- 如果你要做前端页面开发，建议仍然优先本地运行 `npm run dev`
- 当前 Docker 开发环境更适合联调整体链路、验证容器化行为、准备后续服务器部署
