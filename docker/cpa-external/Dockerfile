# ============================================
# cpa-external 应用镜像（服务器端构建）
#
# 构建上下文只需本目录下的 Dockerfile 和 app.jar：
#   jar 由本地 bin/package 打包后上传至此
#   服务器执行 docker compose build 或 deploy-server.sh update
# ============================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="cpa-external"
LABEL description="CLIProxyAPI 外部管理扩展后端服务"

WORKDIR /app

# 时区与字体（验证码功能依赖）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone \
    && apk add --no-cache fontconfig ttf-dejavu && fc-cache -fv

# jar 由发版流程上传到构建上下文
COPY app.jar /app/app.jar

# 上传文件目录（宿主机挂载点）
RUN mkdir -p /home/ruoyi/uploadPath

EXPOSE 8080

# JVM 参数通过 compose 注入 JAVA_TOOL_OPTIONS，便于按机器规格调整
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
