# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

ARG MAVEN_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"
ENV MAVEN_OPTS=${MAVEN_OPTS}

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 chmod +x mvnw && ./mvnw -B -s .mvn/settings.xml -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN command -v curl > /dev/null \
	&& groupadd --system spring \
	&& useradd --system --gid spring --home-dir /app --shell /usr/sbin/nologin spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/target/backend-*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-XX:MaxMetaspaceSize=160m", "-jar", "/app/app.jar", "--spring.profiles.active=prod"]
