FROM docker.1ms.run/library/maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

RUN mkdir -p /root/.m2 && printf '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"><mirrors><mirror><id>aliyunmaven</id><mirrorOf>*</mirrorOf><name>aliyun</name><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests clean package

FROM docker.1ms.run/library/eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/health-system-1.0.0.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
