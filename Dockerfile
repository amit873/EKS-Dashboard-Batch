FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy entrypoint script and config
COPY advanced-entrypoint.sh /app/entrypoint.sh
COPY batch-config.properties /app/batch-config.properties
RUN chmod +x /app/entrypoint.sh

# Copy all JAR files from modules
COPY Dashboard/target/dashboard-1.0.0.jar dashboard.jar
COPY Batch1/target/batch1-1.0.0.jar batch1.jar
COPY Batch2/target/batch2-1.0.0.jar batch2.jar

EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]