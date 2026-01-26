# ============================================================
# BiblioTech Pro - Backend Spring Boot (VPS Optimized)
# ============================================================
# Esta imagen asume que el JAR ya fue compilado externamente
# para ahorrar recursos en el VPS (evita correr Maven).
# ============================================================

FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Xavier Aerox <xavier@bibliotech.pro>"
LABEL description="BiblioTech Pro Backend API (VPS Optimized)"

WORKDIR /app

# Instalar curl para healthcheck
RUN apk add --no-cache curl

# Crear usuario no-root
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# Copiar JAR ya compilado desde el contexto de build (carpeta target)
# Nota: El script de despliegue debe asegurar que el JAR existe en target/
COPY target/*.jar app.jar

# Cambiar propietario
RUN chown -R appuser:appgroup /app

# Usar usuario no-root
USER appuser

# Puerto de la aplicación
EXPOSE 9091

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9091/actuator/health || exit 1

# Entrypoint
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
