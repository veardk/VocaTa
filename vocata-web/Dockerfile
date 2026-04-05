FROM node:20-alpine AS build

RUN apk add --no-cache python3 make g++ git

WORKDIR /app

COPY package*.json ./
RUN npm ci && npm cache clean --force

COPY . .

ARG BUILD_MODE=production
ARG VITE_APP_URL
ENV VITE_APP_URL=${VITE_APP_URL}
RUN npm run build:${BUILD_MODE}

FROM nginx:1.25-alpine

RUN apk add --no-cache \
    curl \
    dumb-init \
    tzdata

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN addgroup -g 1001 -S vocata && \
    adduser -u 1001 -S vocata -G vocata && \
    mkdir -p /var/cache/nginx /var/log/nginx /var/lib/nginx && \
    chown -R vocata:vocata /var/cache/nginx /var/log/nginx /var/lib/nginx /etc/nginx

COPY --from=build --chown=vocata:vocata /app/dist /usr/share/nginx/html

RUN cat > /etc/nginx/nginx.conf << 'EOF'
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /tmp/nginx.pid;

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

    # 服务器配置
    server {
        listen 8080;
        server_name localhost;
        root /usr/share/nginx/html;
        index index.html index.htm;

        # 安全头
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-XSS-Protection "1; mode=block" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "no-referrer-when-downgrade" always;

        # SPA路由支持
        location / {
            try_files $uri $uri/ /index.html;
        }

        # 静态资源缓存
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }

        location /api {
            proxy_pass http://vocata-server:9009;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # 健康检查端点
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
EOF

RUN chown -R vocata:vocata /usr/share/nginx/html /etc/nginx

USER vocata

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

ARG BUILD_DATE
ARG VERSION="1.0.0"
LABEL maintainer="VocaTa Team <dev@vocata.com>" \
      version="${VERSION}" \
      build-date="${BUILD_DATE}" \
      description="VocaTa AI角色扮演平台前端客户端" \
      org.opencontainers.image.title="vocata-web" \
      org.opencontainers.image.description="VocaTa AI Role Playing Platform Frontend Client" \
      org.opencontainers.image.url="https://github.com/leivik/vocata" \
      org.opencontainers.image.vendor="VocaTa Team" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}"

ENTRYPOINT ["dumb-init", "--"]
CMD ["nginx", "-g", "daemon off;"]
