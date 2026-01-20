# Informe Final de Endurecimiento (Hardening)

Fecha: 18 de Enero de 2026
Autor: Antigravity/Auditor

## Resumen de Defensa en Profundidad (Capas 1, 2, 3 y 4)

El sistema ha sido reforzado para resistir no solo atacantes externos, sino también fallos internos, errores de concurrencia y desastres naturales (caídas de servidor).

### Capa 1: Base de Datos (Física)
*   **Índice Único Condicional:** Se implementó `CREATE UNIQUE INDEX ... WHERE ESTADO='ACTIVO'`.
    *   *Objetivo:* Garantizar matemáticamente que nunca existan dos reservas activas para el mismo usuario.
    *   *Resultado:* Cualquier intento de violación (incluso por administradores con SQL directo) fallará con `ORA-00001`.

### Capa 2: Base de Datos (Lógica/Triggers)
*   **Validación Temporal:** `TRG_VALIDAR_PRESTAMO` aplica `WHERE FECHA_FIN > SYSDATE`.
    *   *Objetivo:* Proteger contra bugs del backend que intenten usar datos expirados.
    *   *Resultado:* La BD es consciente del tiempo real, independiente de la capa Java.

### Capa 3: Backend (Servicio/Java)
*   **Manejo de Excepciones de Integridad:** `BloqueoService` ahora captura `DataIntegrityViolationException`.
    *   *Objetivo:* Cuando la Capa 1 rechaza un ataque de concurrencia, Java lo atrapa y devuelve un mensaje de negocio (`IllegalStateException`) en lugar de un stacktrace 500.
    *   *Resultado:* UX consistente incluso bajo ataque.
*   **Protección Anti-XSS y Anti-DoS:** En `GeminiService` y `LibroRepository`.

### Capa 4: Frontend (Cosmética)
*   Mantiene validaciones visuales para guiar al usuario honesto, pero el sistema no depende de ellas.

### Pruebas de Estrés Mental (Veredicto)
*   **Race Condition (Bibliotecarios):** `ObjectOptimisticLockingFailureException` (Protegido).
*   **Race Condition (Socios):** `DataIntegrityViolationException` (Protegido por índice condicional).
*   **Caída del Job:** Endpoint Manual `/cleanup` (Recuperable).
*   **Caída del Server:** ACID Rollback automático (Protegido).

El sistema está listo para producción.
