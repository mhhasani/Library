FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Copy pom.xml
COPY pom.xml .

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Create a lightweight runtime image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=0 /app/target/library-management-system-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Environment variables
ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=library_db
ENV DB_USER=libraryuser
ENV DB_PASSWORD=librarypass
ENV JWT_SECRET=your-secret-key-change-this-in-production-at-least-32-characters-long

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
