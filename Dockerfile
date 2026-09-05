# ==========================================
# Stage 1: Build the application package
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Optimize Maven memory usage for Render 512MB free tier
ENV MAVEN_OPTS="-Xmx384m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Copy pom.xml and dependencies
COPY pom.xml .
RUN mvn -B dependency:resolve || true

# Compile and package application (skip tests as CI runs them)
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Hardened, production-ready runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for cloud container health checks
RUN apk --no-cache add curl

# Create non-root system group and user for security compliance
RUN addgroup -S spring && adduser -S springuser -G spring

# Copy compiled JAR artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R springuser:spring /app

# Switch to non-root user
USER springuser:spring

# Default port (overridden dynamically by Render / Railway via $PORT)
ENV PORT=8282
EXPOSE 8282

# JVM container memory tuning: strict limits for 512MB free tier containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx350m -Djava.security.egd=file:/dev/./urandom"

# Built-in container health check probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT}/actuator/health || exit 1

# Execute Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
