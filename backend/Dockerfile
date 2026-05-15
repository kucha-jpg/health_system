ARG IMAGE_PREFIX=docker.m.daocloud.io/library/

FROM ${IMAGE_PREFIX}maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Use Maven Central directly (aliyun mirror was unreliable with TLS handshake)
RUN mkdir -p /root/.m2

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

FROM ${IMAGE_PREFIX}eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/health-system-1.0.0.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
