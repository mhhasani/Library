# Use a lightweight JRE image
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the pre-built JAR file
COPY target/library-management-system-1.0.0.jar app.jar

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
