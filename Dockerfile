FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HelpManual"
LABEL description="Help Manual Website - Internal Documentation System"

WORKDIR /app

# Create directories for data persistence
RUN mkdir -p /app/data /app/uploads

# Copy the Spring Boot JAR (frontend is bundled inside)
COPY backend/target/helpmanual.jar app.jar

# Environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    JWT_SECRET="" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/public/categories || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
