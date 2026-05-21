# ─── Stage 1: Frontend build ─────────────────────────────────────────────────
FROM node:20-alpine AS frontend-builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install --legacy-peer-deps
COPY frontend/src ./src
COPY frontend/public ./public
RUN npm run build

# ─── Stage 2: Backend build ──────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app
# Resolve dependencies in a separate layer for better cache utilization
COPY pom.xml .
RUN mvn dependency:go-offline -q
# Copy source and inject the built frontend as Spring Boot static assets
COPY src ./src
COPY --from=frontend-builder /app/build ./src/main/resources/static
RUN mvn package -DskipTests -q

# ─── Stage 3: Runtime image ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Run as a non-root user
RUN groupadd --system --gid 1001 appgroup && \
    useradd --system --uid 1001 --gid appgroup appuser
COPY --from=backend-builder /app/target/library-management-system-1.0.0.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
