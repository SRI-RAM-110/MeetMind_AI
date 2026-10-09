# Multi-stage Dockerfile for MeetMind AI Spring Boot Backend
# Stage 1: Build JAR using Maven and OpenJDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Secure OpenJDK 21 runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create non-root system user for security
RUN groupadd -r meetmind && useradd -r -g meetmind meetmind

# Copy the compiled JAR artifact from builder stage
COPY --from=builder /build/target/*.jar /app/app.jar

RUN chown -R meetmind:meetmind /app

USER meetmind

# Render sets PORT dynamically (defaults to 8085 if run locally)
ENV PORT=8085
ENV JAVA_OPTS="-Djava.awt.headless=true -Xmx400m -Xms200m -XX:+UseG1GC"

EXPOSE 8085

# Execute Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
