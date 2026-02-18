# Memoria Técnica del Proyecto: Sistema de Gestión Bibliotecaria

## 1. Introducción y Alcance
El presente documento detalla las decisiones arquitectónicas, de diseño y de seguridad adoptadas para el desarrollo del sistema de gestión bibliotecaria. El objetivo principal ha sido construir una solución robusta, segura y escalable que cumpla estrictamente con los requisitos funcionales establecidos, integrando tecnologías modernas como Inteligencia Artificial Generativa bajo un marco de "Defensa en Profundidad".

## 2. Arquitectura del Sistema

### 2.1. Patrón Arquitectónico
Se ha implementado una arquitectura en capas sobre el framework **Spring Boot**, separando claramente las responsabilidades para facilitar la mantenibilidad y el testing:

*   **Capa de Presentación (Frontend):** SPA (Single Page Application) desarrollada en JavaScript vainilla (ES6+), HTML5 y CSS3. Se ha evitado el uso de frameworks pesados para demostrar dominio de los estándares web y optimizar los tiempos de carga (Performance Budget).
*   **Capa de Controladores (API REST):** Exposición de endpoints RESTful documentados mediante OpenAPI/Swagger. Manejo centralizado de excepciones y validación de entrada (DTOs).
*   **Capa de Servicio (Lógica de Negocio):** Implementación de las reglas de negocio, orquestación de transacciones (`@Transactional`) e integración con la IA.
*   **Capa de Persistencia (Acceso a Datos):** Uso de Spring Data JPA / Hibernate para la interacción con la base de datos Oracle, complementado con consultas nativas optimizadas.

### 2.2. Diagrama Lógico de Componentes
El flujo de información sigue un modelo estricto donde el Frontend nunca accede directamente a la Base de Datos ni a la API de IA.

`[Cliente Web] <--(HTTPS + Secure Cookie)--> [Nginx/API Gateway] <--> [Backend Spring Boot] <--> [Oracle DB / Gemini API]`

## 3. Modelo de Datos e Integridad

### 3.1. Diseño del Esquema Relacional
El modelo de datos se ha diseñado sobre **Oracle Database 21c XE**, respetando la Tercera Forma Normal (3NF) y los requisitos específicos del dominio académico.

*   **Separación Lógica Libro/Ejemplar:** Permite la gestión eficiente de múltiples copias físicas de una misma obra intelectual.
*   **Dualidad Bloqueo/Préstamo:** Se mantienen tablas separadas para `BLOQUEO` (reserva temporal) y `PRESTAMO` (cesión física), segregando estados transitorios de estados persistentes.

### 3.2. Estrategia de Integridad y Concurrencia (Defensa en Profundidad)
Para garantizar la consistencia de los datos ante peticiones concurrentes (Race Conditions), se ha implementado un mecanismo de seguridad en múltiples niveles:

1.  **Nivel Base de Datos (Hard Constraint):**
    *   **Índice Único Condicional:** Se creó el índice `IDX_UN_BLOQUEO_ACTIVO` sobre la tabla `BLOQUEO`.
    *   *Justificación:* Este objeto de base de datos hace físicamente imposible que existan dos filas con estado 'ACTIVO' para el mismo usuario simultáneamente, delegando la autoridad final de la integridad al motor de base de datos.
2.  **Nivel Aplicación (Manejo de Excepciones):**
    *   El servicio captura específicamente `DataIntegrityViolationException` para traducir errores técnicos de base de datos en mensajes de negocio comprensibles ("Ya posee una reserva activa").
3.  **Triggers de Validación:**
    *   Disparadores `BEFORE INSERT` que validan reglas complejas (penalizaciones, cuotas máximas) que no pueden ser cubiertas solo por restricciones `CHECK` o `UNIQUE`.

## 4. Diseño de Seguridad

### 4.1. Autenticación y Gestión de Sesiones
Se ha descartado el almacenamiento de tokens JWT en `localStorage` o `sessionStorage` debido a su vulnerabilidad ante ataques de tipo Cross-Site Scripting (XSS).

*   **Implementación:** Los tokens JWT se entregan y reciben exclusivamente a través de Cookies con las banderas `HttpOnly`, `Secure` y `SameSite=Strict`.
*   **Justificación:** Esto hace que el token sea inaccesible para cualquier script malicioso ejecutándose en el navegador, mitigando efectivamente el robo de sesión.

### 4.2. Control de Acceso (RBAC)
Se utiliza Spring Security para imponer restricciones basadas en roles (`ADMIN`, `BIBLIOTECARIO`, `SOCIO`) tanto a nivel de endpoint (`SecurityFilterChain`) como a nivel de método. Se aplica el principio de mínimo privilegio (ej. los Socios no tienen acceso a endpoints de devolución).

## 5. Integración de Inteligencia Artificial

### 5.1. Arquitectura de Integración
La comunicación con el modelo generativo (Google Gemini) se realiza de forma **stateless** desde el backend.

*   **Privacidad por Diseño:** Se anonimizan los datos antes de enviarlos al proveedor de IA. No se envía información personal (PII) del usuario, únicamente metadatos de libros (categorías, títulos).
*   **Resiliencia:** El sistema implementa un parser tolerante a fallos para interpretar las respuestas de la IA (JSON) y sanitiza cualquier contenido HTML/Markdown recibido antes de enviarlo al frontend.

## 6. Automatización y Mantenimiento

### 6.1. Jobs Programados
Se utiliza `DBMS_SCHEDULER` de Oracle para ejecutar procedimientos almacenados de limpieza nocturna. Esto garantiza que la liberación de recursos (ejemplares reservados no recogidos) ocurra independientemente del estado del servidor de aplicaciones.

## 7. Conclusión
El sistema resultante no solo cumple con los requisitos funcionales de gestión bibliotecaria, sino que excede las expectativas en términos de robustez técnica. La combinación de restricciones físicas en base de datos, seguridad a nivel de transporte (HttpOnly Cookies) y una arquitectura limpia asegura una solución defendible, auditable y preparada para entornos productivos.
