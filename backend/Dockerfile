# 后端 Dockerfile
# 使用多阶段构建，减小镜像体积

# 第一阶段：构建阶段
FROM maven:3.8.7-openjdk-8-slim AS build

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 文件（利用 Docker 缓存层）
COPY pom.xml .

# 下载依赖（如果 pom.xml 没有变化，这层会被缓存）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests -B

# 第二阶段：运行阶段
FROM openjdk:8-jre-slim

# 设置工作目录
WORKDIR /app

# 安装必要的工具（包括 curl 用于健康检查）
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# 从构建阶段复制 JAR 文件
COPY --from=build /app/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

## nohup java -jar fund-analysis-1.0.0.jar > /var/log/fund-analysis.log 2>&1 &
