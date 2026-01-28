-- Trigger de validación para nuevos préstamos
-- Implementa las reglas de negocio descritas en los requisitos

CREATE OR REPLACE TRIGGER TRG_VALIDAR_PRESTAMO
BEFORE INSERT ON PRESTAMO
FOR EACH ROW
DECLARE
    v_penalizacion DATE;
    v_num_prestamos NUMBER;
    v_max_prestamos NUMBER;
    v_estado_ejemplar VARCHAR2(20);
    v_libro_id NUMBER;
    v_tiene_copia NUMBER;
    v_usuario_bloqueo NUMBER;
BEGIN
    -- 1. Verificar si el socio está penalizado
    SELECT PENALIZACION_HASTA, MAX_PRESTAMOS_ACTIVOS
    INTO v_penalizacion, v_max_prestamos
    FROM SOCIO
    WHERE ID_SOCIO = :NEW.ID_SOCIO;

    IF v_penalizacion IS NOT NULL AND v_penalizacion > SYSDATE THEN
        RAISE_APPLICATION_ERROR(-20001, 'El socio está penalizado hasta ' || TO_CHAR(v_penalizacion, 'DD/MM/YYYY HH24:MI'));
    END IF;

    -- 2. Verificar límite de préstamos activos
    SELECT COUNT(*)
    INTO v_num_prestamos
    FROM PRESTAMO
    WHERE ID_SOCIO = :NEW.ID_SOCIO AND ESTADO = 'ACTIVO';

    IF v_num_prestamos >= v_max_prestamos THEN
        RAISE_APPLICATION_ERROR(-20002, 'El socio ha alcanzado su límite de préstamos (' || v_max_prestamos || ')');
    END IF;

    -- 3. Verificar estado del ejemplar
    SELECT ESTADO, ID_LIBRO
    INTO v_estado_ejemplar, v_libro_id
    FROM EJEMPLAR
    WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR;

    IF v_estado_ejemplar IN ('PRESTADO', 'BAJA') THEN
        RAISE_APPLICATION_ERROR(-20003, 'El ejemplar no está disponible (Estado: ' || v_estado_ejemplar || ')');
    END IF;

    IF v_estado_ejemplar = 'BLOQUEADO' THEN
         -- Verificar si el bloqueo corresponde al mismo usuario
         SELECT COUNT(*) INTO v_usuario_bloqueo
         FROM BLOQUEO
         WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR 
           AND ID_SOCIO = :NEW.ID_SOCIO 
           AND ESTADO = 'ACTIVO';
         
         IF v_usuario_bloqueo = 0 THEN
             RAISE_APPLICATION_ERROR(-20004, 'El ejemplar está reservado por otro usuario');
         END IF;
    END IF;

    -- 4. Verificar si ya tiene un ejemplar del MISMO libro
    SELECT COUNT(*)
    INTO v_tiene_copia
    FROM PRESTAMO p
    JOIN EJEMPLAR e ON p.ID_EJEMPLAR = e.ID_EJEMPLAR
    WHERE p.ID_SOCIO = :NEW.ID_SOCIO 
      AND p.ESTADO = 'ACTIVO'
      AND e.ID_LIBRO = v_libro_id;

    IF v_tiene_copia > 0 THEN
        RAISE_APPLICATION_ERROR(-20005, 'El socio ya tiene un préstamo activo de este libro');
    END IF;
END;
/
