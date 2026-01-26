# ============================================================
# BiblioTech Pro - Backend Spring Boot
# Dockerfile Multistage Optimizado
# ============================================================
# Características:
#   - Build con Maven (caché de dependencias)
#   - Imagen final Alpine ultraligera (~200MB)
#   - Usuario no-root para seguridad
#   - Healthcheck integrado
# ============================================================

# ===========================================
# STAGE 1: BUILD
# ===========================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 1. Copiar solo pom.xml primero (cache de dependencias)
COPY pom.xml .

# 2. Descargar dependencias (esta capa se cachea si pom.xml no cambia)
RUN mvn dependency:go-offline -B

# 3. Copiar código fuente
COPY src ./src

# 4. Compilar y empaquetar
RUN mvn clean package -DskipTests -B

# ===========================================
# STAGE 2: RUNTIME (Imagen ligera)
# ===========================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Xavier Aerox <xavier@bibliotech.pro>"
LABEL description="BiblioTech Pro Backend API"
LABEL version="1.0.0"

WORKDIR /app

# Instalar curl para healthcheck
RUN apk add --no-cache curl

# Crear usuario no-root
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# Copiar JAR desde el stage de build
COPY --from=build /app/target/*.jar app.jar

# Cambiar propietario
RUN chown -R appuser:appgroup /app

# Usar usuario no-root
USER appuser

# Puerto de la aplicación
EXPOSE 9091

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9091/actuator/health || exit 1

# Variables de entorno por defecto (pueden sobreescribirse)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Punto de entrada con opciones de JVM configurables
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
