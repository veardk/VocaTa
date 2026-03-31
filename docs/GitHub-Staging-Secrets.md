# GitHub Staging Secrets 清单

当前 staging 目标机：

- `STAGING_HOST=86.53.161.33`
- `STAGING_USER=deploy`
- SSH 端口固定为 `22`（当前 workflow 已写死）

## 必填 Secrets

现在的简化版 staging workflow 只需要 3 个 secrets：

- `STAGING_HOST`
- `STAGING_USER`
- `STAGING_SSH_KEY`

## STAGING_SSH_KEY 来源

当前为 GitHub Actions 生成的 staging deploy key 私钥保存在本机：

- `/home/an/Projects/goodPro/VocaTa/.local/vocata_staging_ed25519`

对应公钥已经安装到服务器 `root` 用户：

- `/root/.ssh/authorized_keys`

同一把公钥也已经安装到服务器部署用户：

- `/home/deploy/.ssh/authorized_keys`

把私钥完整内容复制到 GitHub 仓库 Secret `STAGING_SSH_KEY` 即可。

## 当前服务器登录建议

- 日常部署用户：`deploy`
- 应急用户：`root`
- SSH 密码登录：已禁用
- SSH 公钥登录：已启用

## 服务器配置放哪里

业务配置现在不再放 GitHub Secrets，而是放服务器本地：

- `/home/deploy/deploy/vocata/.env`

兼容旧位置：

- `/home/deploy/deploy/vocata/data/vocata-staging.env`

如果新位置不存在，workflow 会自动从旧位置复制一份。

## 前端 IP 暴露说明

如果服务器本地 `.env` 中写的是：

- `VITE_APP_URL=http://86.53.161.33:9009`

那么前端构建产物里会直接带这个 IP，浏览器里可见。

如果你想避免把后端 IP 直接打进前端，服务器 `.env` 建议改成：

- `VITE_APP_URL=/api`

前提：

- 当前前端镜像内 Nginx 已经把 `/api` 代理到 `vocata-server:9009`

这会让浏览器只请求当前站点的 `/api`，不再把 `9009` 和后端 IP 显式写进前端包里。
