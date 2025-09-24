# VocaTa管理后台 - 多阶段构建Dockerfile
# 基于Node.js官方镜像

# 构建阶段
FROM node:20-alpine AS build

# 安装必要的构建工具
RUN apk add --no-cache python3 make g++ git

# 设置工作目录
WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production && npm cache clean --force

# 复制源代码
COPY . .

# 构建应用
ARG BUILD_MODE=production
RUN npm run build:${BUILD_MODE}

# 生产阶段 - 使用Nginx托管静态文件
FROM nginx:1.25-alpine

# 安装必要工具
RUN apk add --no-cache \
    curl \
    tzdata \
    dumb-init

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建nginx用户和目录
RUN addgroup -g 1001 -S vocata && \
    adduser -u 1001 -S vocata -G vocata && \
    mkdir -p /var/cache/nginx /var/log/nginx /var/lib/nginx && \
    chown -R vocata:vocata /var/cache/nginx /var/log/nginx /var/lib/nginx /etc/nginx

# 复制构建产物
COPY --from=build --chown=vocata:vocata /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY --chown=vocata:vocata nginx.conf /etc/nginx/nginx.conf

# 创建默认的Nginx配置文件
RUN cat > /etc/nginx/nginx.conf << 'EOF'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # 日志格式
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    # 性能优化
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/js
        text/xml
        text/javascript
        application/javascript
        application/xml+rss
        application/json;

    # 管理后台服务器配置
    server {
        listen 80;
        server_name localhost;
        root /usr/share/nginx/html;
        index index.html index.htm;

        # 安全头 - 管理后台加强安全
        add_header X-Frame-Options "DENY" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self' https://api.vocata.com https://test-api.vocata.com;" always;

        # 管理后台访问控制（可选）
        # allow 192.168.1.0/24;
        # deny all;

        # SPA路由支持
        location / {
            try_files $uri $uri/ /index.html;
        }

        # 静态资源缓存
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        # 管理API代理
        location /api {
            proxy_pass http://vocata-server:9010;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;

            # 管理后台专用头部
            proxy_set_header X-Admin-Request "true";
        }

        # 健康检查端点
        location /health {
            access_log off;
            return 200 "admin-healthy\n";
            add_header Content-Type text/plain;
        }

        # 管理后台特殊路径保护
        location /admin {
            try_files $uri $uri/ /index.html;
        }

        # 禁止访问敏感文件
        location ~ /\. {
            deny all;
            access_log off;
            log_not_found off;
        }

        location ~ \.(env|config)$ {
            deny all;
            access_log off;
            log_not_found off;
        }
    }
}
EOF

# 设置权限
RUN chown -R vocata:vocata /usr/share/nginx/html /etc/nginx

# 切换到非root用户
USER vocata

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# 构建参数和标签
ARG BUILD_DATE
ARG VERSION="1.0.0"
LABEL maintainer="VocaTa Team <dev@vocata.com>" \
      version="${VERSION}" \
      build-date="${BUILD_DATE}" \
      description="VocaTa AI角色扮演平台管理后台" \
      org.opencontainers.image.title="vocata-admin" \
      org.opencontainers.image.description="VocaTa AI Role Playing Platform Admin Dashboard" \
      org.opencontainers.image.url="https://github.com/leivik/vocata" \
      org.opencontainers.image.vendor="VocaTa Team" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}"

# 启动Nginx
ENTRYPOINT ["dumb-init", "--"]
CMD ["nginx", "-g", "daemon off;"]