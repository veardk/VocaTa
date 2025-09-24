# VocaTa后端服务 - 多阶段构建Dockerfile
# 基于OpenJDK 17的精简镜像

# 构建阶段 - 用于Maven构建（可选，CI/CD中已构建）
FROM maven:3.9.4-eclipse-temurin-17-alpine AS build

WORKDIR /build

# 复制Maven配置文件和Wrapper
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（利用Docker层缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用（支持多环境）
ARG SPRING_PROFILES_ACTIVE=prod
RUN mvn clean package -B -DskipTests -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}

# 运行阶段 - 精简的生产镜像
FROM eclipse-temurin:17-jre-alpine

# 安装必要工具和dumb-init
RUN apk add --no-cache \
    curl \
    tzdata \
    font-noto-cjk \
    dumb-init

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用用户（安全最佳实践）
RUN addgroup -g 1001 -S vocata && \
    adduser -u 1001 -S vocata -G vocata

# 设置工作目录
WORKDIR /app

# 复制JAR文件（从构建阶段或本地）
COPY --from=build /build/target/vocata-server-*.jar app.jar

# 创建必要目录并设置权限
RUN mkdir -p /app/logs /app/uploads /var/log/vocata && \
    chown -R vocata:vocata /app /var/log/vocata && \
    chmod +x /app/app.jar

# 切换到非root用户
USER vocata

# 暴露端口
EXPOSE 9010

# JVM优化参数
ENV JAVA_OPTS="-Xms512m -Xmx2048m \
-XX:+UseG1GC \
-XX:G1HeapRegionSize=16m \
-XX:+DisableExplicitGC \
-XX:+UseStringDeduplication \
-XX:+OptimizeStringConcat \
-Djava.security.egd=file:/dev/./urandom \
-Dfile.encoding=UTF-8 \
-Duser.timezone=Asia/Shanghai"

# 应用配置环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=9010

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT}/api/actuator/health || exit 1

# 启动应用 - 使用 dumb-init 处理信号
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 构建参数和标签信息
ARG BUILD_DATE
ARG VERSION="1.0.0"
LABEL maintainer="VocaTa Team <dev@vocata.com>" \
      version="${VERSION}" \
      build-date="${BUILD_DATE}" \
      description="VocaTa AI角色扮演平台后端服务" \
      org.opencontainers.image.title="vocata-server" \
      org.opencontainers.image.description="VocaTa AI Role Playing Platform Backend Service" \
      org.opencontainers.image.url="https://github.com/leivik/vocata" \
      org.opencontainers.image.vendor="VocaTa Team" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}"