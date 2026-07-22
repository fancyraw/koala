# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /build
# 先拷 pom 预热依赖缓存，命中缓存时改代码无需重下依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:8-jre
WORKDIR /app
# 时区与非 root 运行
ENV TZ=Asia/Shanghai
RUN apt-get update && apt-get install -y --no-install-recommends tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*
RUN groupadd -r koala && useradd -r -g koala koala \
    && mkdir -p /app/logs && chown -R koala:koala /app
COPY --from=build /build/target/*.jar app.jar
RUN chown koala:koala app.jar
USER koala

EXPOSE 8080
# 生产 profile；JVM 堆按容器内存自适应
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
