FROM eclipse-temurin:17-jdk-alpine

# Install necessary packages
RUN apk add --no-cache bash curl

# Create app directory
WORKDIR /app

# Copy the application
COPY target/universal/stage /app

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV CONFIG_FORCE_akka_remote_artery_canonical_hostname=""
ENV CONFIG_FORCE_akka_remote_artery_canonical_port="2551"
ENV CONFIG_FORCE_akka_management_http_hostname=""
ENV CONFIG_FORCE_akka_management_http_port="8558"
ENV CONFIG_FORCE_akka_management_http_bind_hostname="0.0.0.0"
ENV CONFIG_FORCE_akka_management_http_bind_port="8558"

# Expose ports
EXPOSE 2551 8558 8080

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8558/health || exit 1

# Run the application
ENTRYPOINT ["/app/bin/arktrio-mesh"] 