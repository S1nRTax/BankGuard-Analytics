# Multi-stage build for Stream Processor
FROM openjdk:17-jdk-slim as build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
COPY .mvn .mvn

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:resolve

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Create final runtime image
FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/stream-processor-*.jar app.jar

# Create non-root user for security
RUN useradd -r -s /bin/false appuser && \
    chown -R appuser:appuser /app

# Create directory for Kafka Streams state store
RUN mkdir -p /tmp/kafka-streams && chown -R appuser:appuser /tmp/kafka-streams

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8082/api/health || exit 1

# JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:G1HeapRegionSize=16m -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]