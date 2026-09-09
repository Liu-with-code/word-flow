# =====================================================================
# WordFlow 后端镜像（多阶段构建）
#   阶段 1：Maven 编译打包
#   阶段 2：仅 JRE 运行
# =====================================================================

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 先拷贝 pom 预拉依赖，利用 Docker 层缓存加速重复构建
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline

COPY backend/src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_TOOL_OPTIONS="-Xms256m -Xmx512m"

COPY --from=build /workspace/target/wordflow-backend-0.1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
