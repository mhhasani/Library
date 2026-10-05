FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# skip BOTH test compilation and execution: the deploy image must not depend on
# test-code health (tests run separately via `mvn test` / CI). -DskipTests would
# still compile tests and break the image when a refactor outpaces the tests.
RUN mvn package -Dmaven.test.skip=true --no-transfer-progress

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup appuser
COPY --from=builder /app/target/library-management-system-1.0.0.jar app.jar
# Create the file-storage path owned by appuser so a fresh named volume mounted here
# inherits writable ownership (otherwise uploads fail with AccessDeniedException).
# Log directory is owned by appuser as well (rolling application/audit logs).
RUN mkdir -p /data/library-files /var/log/library \
    && chown -R appuser:appgroup /data /var/log/library app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-Djava.awt.headless=true", "-jar", "app.jar"]
