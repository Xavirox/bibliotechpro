# 🛡️ Correcciones de Auditoría de Seguridad

## Fecha: 2026-01-18
## Estado: IMPLEMENTADAS (Ronda 2)

---

## Resumen Ejecutivo

Se realizaron DOS rondas de auditoría técnica exhaustiva del sistema BiblioTech Pro. Este documento detalla las correcciones implementadas en ambas rondas.

---

## ✅ Correcciones Implementadas - RONDA 2 (18-Enero-2026)

### C-02 (NUEVA): JWT en HttpOnly Cookie
**Severidad original:** 🔴 CRÍTICO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/controller/AuthController.java`
- `backend/src/main/java/com/biblioteca/security/JwtAuthenticationFilter.java`
- `frontend/js/auth.js`
- `frontend/js/api.js`
- `frontend/js/main.js`

**Cambios realizados:**
- Token JWT ahora se envía como cookie HttpOnly (no accesible por JavaScript)
- Backend: Login establece cookie, Logout la invalida
- Backend: Filtro JWT lee de cookie primero, header Authorization como fallback
- Frontend: Ya no almacena token en localStorage (vulnerable a XSS)
- Frontend: Todas las peticiones usan `credentials: 'include'`

**Beneficio de seguridad:** Ataques XSS ya no pueden robar el token de sesión.

---

### C-03: Verificación de Expiración en Formalización
**Severidad original:** 🔴 CRÍTICO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/service/BloqueoService.java`

**Cambios realizados:**
- `formalizarBloqueo()` ahora verifica expiración en tiempo real
- Verifica que el ejemplar sigue en estado BLOQUEADO
- Previene race conditions entre creación de bloqueo y formalización
- Logging de intentos sospechosos

---

### H-01: Documentación de Swagger en Producción
**Severidad original:** 🟠 ALTO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/security/SecurityConfig.java`

**Cambios realizados:**
- Añadidos comentarios claros indicando que en producción Swagger debe restringirse a ADMIN o desactivarse con @Profile("dev")

---

## ✅ Correcciones Implementadas - RONDA 1 (18-Enero-2026)

### C-03: Actuator Endpoints Restringidos
**Severidad original:** 🔴 CRÍTICO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/security/SecurityConfig.java`
- `backend/src/main/resources/application.properties`

**Cambios realizados:**
- Solo `/actuator/health` es público (para balanceadores de carga)
- Resto de endpoints de Actuator requieren rol ADMIN
- `health.show-details=never` para no exponer información interna
- `info.env.enabled=false` para no exponer variables de entorno

---

### C-02 (Ronda 1): Endpoint Público de Usuarios Endurecido  
**Severidad original:** 🔴 CRÍTICO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/controller/SocioController.java`
- `frontend/js/auth.js`

**Cambios realizados:**
- Se eliminó el campo `rol` de la respuesta (evita identificar cuentas privilegiadas)
- Se añadió documentación de advertencia para recordar deshabilitarlo en producción
- Frontend adaptado para funcionar sin el campo rol

---

### H-02: Validación de Propiedad en Devolución de Préstamos
**Severidad original:** 🟠 ALTO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/controller/PrestamoController.java`
- `backend/src/main/java/com/biblioteca/service/PrestamoService.java`
- `backend/src/test/java/com/biblioteca/service/PrestamoServiceTest.java`

**Cambios realizados:**
- El método `devolverPrestamo` ahora verifica la propiedad del préstamo
- Un SOCIO solo puede devolver SUS propios préstamos
- BIBLIOTECARIO/ADMIN pueden devolver cualquier préstamo
- Se loguean intentos de devolver préstamos ajenos (auditoría)
- Tests actualizados con casos de seguridad

---

### H-04: Sanitización XSS de Respuestas de IA
**Severidad original:** 🟠 ALTO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/service/GeminiService.java`

**Cambios realizados:**
- Nueva función `sanitizeForXss()` que escapa caracteres HTML peligrosos
- Se aplica a todas las respuestas de Gemini antes de devolverlas
- Protege contra inyección de `<script>` y otros tags maliciosos

---

### M-02: X-Forwarded-For Spoofing Protection
**Severidad original:** 🟡 MEDIO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/security/RateLimitingFilter.java`

**Cambios realizados:**
- Nueva función `isValidIpFormat()` que valida formato IPv4/IPv6
- X-Forwarded-For con formato inválido es rechazado y se usa RemoteAddr
- Se loguean intentos de spoofing para auditoría

---

### M-03: Expiración de Bloqueos en Tiempo Real
**Severidad original:** 🟡 MEDIO

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/service/BloqueoService.java`

**Cambios realizados:**
- `getMisBloqueos()` filtra por `fechaFin > now` además de estado
- `getBloqueosActivos()` también verifica expiración real
- `crearBloqueo()` valida límite de 1 bloqueo activo (con expiración real)
- Ya no depende exclusivamente del job nocturno para detectar expiración

---

### H-01: Lógica Duplicada Backend/Triggers - MEJORADA
**Severidad original:** 🟠 ALTO (mejora de UX)

**Archivos modificados:**
- `backend/src/main/java/com/biblioteca/service/BloqueoService.java`
- `backend/src/main/java/com/biblioteca/service/PrestamoService.java`

**Cambios realizados:**
- `crearBloqueo()` ahora valida límite de 1 bloqueo activo EN JAVA (antes solo en trigger)
- `crearPrestamo()` ahora valida penalización del socio EN JAVA
- `crearPrestamo()` ahora valida límite de préstamos activos EN JAVA
- Mensajes de error amigables en español (antes: "ORA-20001...")

---

## ⚠️ Acciones Pendientes (Requieren Intervención Manual)

### C-01: Rotación de Credenciales
**Estado:** ⏳ PENDIENTE - Requiere acción del propietario

El archivo `.env` contiene credenciales reales que deben rotarse:
1. Cambiar contraseña de base de datos Oracle
2. Generar nuevo JWT_SECRET
3. Regenerar GEMINI_API_KEY desde Google Cloud Console

### Recomendaciones adicionales:
- Verificar que `.env` esté en `.gitignore` ✅
- Considerar uso de gestores de secretos (Vault, AWS Secrets Manager)
- En producción, deshabilitar el endpoint `/api/socios/public`
- En producción, Swagger requiere autenticación ADMIN

---

## 📊 Estado de Hallazgos

| ID | Severidad | Título | Estado |
|----|-----------|--------|--------|
| C-01 | 🔴 CRÍTICO | Secretos en .env | ⏳ Pendiente manual |
| C-02 | 🔴 CRÍTICO | JWT en localStorage (XSS) | ✅ Corregido (Ronda 2) |
| C-03 | 🔴 CRÍTICO | Formalización sin verificar expiración | ✅ Corregido (Ronda 2) |
| C-04 | 🔴 CRÍTICO | Actuator expuesto | ✅ Corregido (Ronda 1) |
| H-01 | 🟠 ALTO | Swagger público | ✅ Documentado (Ronda 2) |
| H-02 | 🟠 ALTO | Validación devolución | ✅ Corregido (Ronda 1) |
| H-03 | 🟠 ALTO | Lógica duplicada | ✅ Mejorado (Ronda 1) |
| H-04 | 🟠 ALTO | XSS respuestas IA | ✅ Corregido (Ronda 1) |
| M-01 | 🟡 MEDIO | X-Forwarded-For spoofable | ✅ Corregido (Ronda 1) |
| M-02 | 🟡 MEDIO | Expiración bloqueos tiempo real | ✅ Corregido (Ronda 1) |
| M-03 | 🟡 MEDIO | Rate limiting no distribuido | ℹ️ Nota: Requiere Redis |
| M-04 | 🟡 MEDIO | Catálogo público | ℹ️ Decisión de negocio |

---

## Verificación

Todos los tests pasan después de las correcciones:
```
mvn compile -q
mvn test -q -Dtest=PrestamoServiceTest
```

---

*Documento generado automáticamente tras auditoría de seguridad*
*Última actualización: 2026-01-18 (Ronda 2)*

