-- =========================================================================
-- SCRIPT DE CORRECCIÓN DE DATOS
-- Elimina préstamos que violan las reglas de negocio (Exceso de límite o Duplicados)
-- =========================================================================

ALTER SESSION SET CURRENT_SCHEMA = biblioteca;

DECLARE
    v_closed_count NUMBER := 0;
BEGIN
    DBMS_OUTPUT.ENABLE;
    
    -- 1. Cerrar préstamos excedentes (dejar solo los N más recientes por usuario)
    FOR r IN (
        SELECT p.ID_SOCIO, s.MAX_PRESTAMOS_ACTIVOS
        FROM PRESTAMO p
        JOIN SOCIO s ON p.ID_SOCIO = s.ID_SOCIO
        WHERE p.ESTADO = 'ACTIVO'
        GROUP BY p.ID_SOCIO, s.MAX_PRESTAMOS_ACTIVOS
        HAVING COUNT(*) > s.MAX_PRESTAMOS_ACTIVOS
    ) LOOP
        -- Para cada socio con exceso, cerramos los más antiguos (mantenemos los recientes)
        FOR p_borrar IN (
            SELECT ID_PRESTAMO, ID_EJEMPLAR
            FROM (
                SELECT p.ID_PRESTAMO, p.ID_EJEMPLAR,
                       ROW_NUMBER() OVER (ORDER BY p.FECHA_PRESTAMO DESC) as rn
                FROM PRESTAMO p
                WHERE p.ID_SOCIO = r.ID_SOCIO AND p.ESTADO = 'ACTIVO'
            )
            WHERE rn > r.MAX_PRESTAMOS_ACTIVOS
        ) LOOP
            -- Cerrar préstamo
            UPDATE PRESTAMO 
            SET ESTADO = 'DEVUELTO', 
                FECHA_DEVOLUCION_REAL = SYSDATE, 
                FECHA_PREVISTA_DEVOLUCION = LEAST(FECHA_PREVISTA_DEVOLUCION, SYSDATE)
            WHERE ID_PRESTAMO = p_borrar.ID_PRESTAMO;
            
            -- Liberar ejemplar
            UPDATE EJEMPLAR 
            SET ESTADO = 'DISPONIBLE'
            WHERE ID_EJEMPLAR = p_borrar.ID_EJEMPLAR;
            
            v_closed_count := v_closed_count + 1;
        END LOOP;
    END LOOP;

    -- 2. Cerrar préstamos duplicados del mismo libro (mismo usuario, mismo libro)
    -- Se mantiene el préstamo más antiguo (el primero que hizo), se cierran los posteriores duplicados
    FOR dup IN (
        SELECT p1.ID_PRESTAMO, p1.ID_EJEMPLAR
        FROM PRESTAMO p1
        JOIN EJEMPLAR e1 ON p1.ID_EJEMPLAR = e1.ID_EJEMPLAR
        WHERE p1.ESTADO = 'ACTIVO'
        AND EXISTS (
            SELECT 1
            FROM PRESTAMO p2
            JOIN EJEMPLAR e2 ON p2.ID_EJEMPLAR = e2.ID_EJEMPLAR
            WHERE p2.ID_SOCIO = p1.ID_SOCIO
              AND p2.ESTADO = 'ACTIVO'
              AND e2.ID_LIBRO = e1.ID_LIBRO
              AND p2.ID_PRESTAMO < p1.ID_PRESTAMO -- Existe uno más antiguo activo
        )
    ) LOOP
         UPDATE PRESTAMO 
         SET ESTADO = 'DEVUELTO', 
             FECHA_DEVOLUCION_REAL = SYSDATE
         WHERE ID_PRESTAMO = dup.ID_PRESTAMO;

         UPDATE EJEMPLAR 
         SET ESTADO = 'DISPONIBLE'
         WHERE ID_EJEMPLAR = dup.ID_EJEMPLAR;
         
         v_closed_count := v_closed_count + 1;
    END LOOP;

    COMMIT;
    -- DBMS_OUTPUT.PUT_LINE('Se han corregido ' || v_closed_count || ' préstamos inválidos.');
END;
/
