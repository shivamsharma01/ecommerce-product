# Multi-stage Spring Boot 3 (Java 17) — small runtime image, layered JAR for cache-friendly layers.
# Requires: bootJar { layered { enabled = true } } in build.gradle

FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

# Dependency layer (cached when only sources change)
RUN ./gradlew dependencies --no-daemon --quiet || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test --quiet

FROM eclipse-temurin:17-jre-jammy AS extractor
WORKDIR /workspace
COPY --from=builder /workspace/build/libs/app.jar /workspace/application.jar
RUN java -Djarmode=layertools -jar /workspace/application.jar extract --destination /layers

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring spring

COPY --from=extractor /layers/dependencies/ ./
COPY --from=extractor /layers/spring-boot-loader/ ./
COPY --from=extractor /layers/snapshot-dependencies/ ./
COPY --from=extractor /layers/application/ ./

USER spring:spring

ENV SERVER_PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
