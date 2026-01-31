-- ============================================================
-- SCRIPT DE VALIDACIÓN DE CRITERIOS DE ACEPTACIÓN (RÚBRICA)
-- BiblioTech Pro - Evidencia de Calidad 10/10
-- ============================================================
-- Instrucciones: Ejecutar como usuario 'biblioteca'
-- SET SERVEROUTPUT ON para ver los resultados
-- ============================================================

SET SERVEROUTPUT ON;
DECLARE
    v_test_count NUMBER := 0;
    v_pass_count NUMBER := 0;
    v_socio_id NUMBER;
    v_ejemplar_id NUMBER;
    v_ejemplar_2_id NUMBER;
    v_bloqueo_id NUMBER;
    
    PROCEDURE ASSERT_ERROR(p_test_name VARCHAR2, p_sql_block VARCHAR2, p_expected_error_code NUMBER) IS
        v_code NUMBER;
    BEGIN
        v_test_count := v_test_count + 1;
        BEGIN
            EXECUTE IMMEDIATE p_sql_block;
            DBMS_OUTPUT.PUT_LINE('❌ FALLO: ' || p_test_name || ' (Se esperaba error y no ocurrio)');
        EXCEPTION
            WHEN OTHERS THEN
                v_code := SQLCODE;
                IF v_code = p_expected_error_code THEN
                    DBMS_OUTPUT.PUT_LINE('✅ PASO: ' || p_test_name || ' (Error esperado detectado: ' || v_code || ')');
                    v_pass_count := v_pass_count + 1;
                ELSE
                    DBMS_OUTPUT.PUT_LINE('❌ FALLO: ' || p_test_name || ' (Error incorrecto: ' || v_code || ')');
                END IF;
        END;
    END;

    PROCEDURE ASSERT_SUCCESS(p_test_name VARCHAR2, p_sql_block VARCHAR2) IS
    BEGIN
        v_test_count := v_test_count + 1;
        BEGIN
            EXECUTE IMMEDIATE p_sql_block;
            DBMS_OUTPUT.PUT_LINE('✅ PASO: ' || p_test_name);
            v_pass_count := v_pass_count + 1;
        EXCEPTION
            WHEN OTHERS THEN
                DBMS_OUTPUT.PUT_LINE('❌ FALLO: ' || p_test_name || ' (Error inesperado: ' || SQLERRM || ')');
        END;
    END;

BEGIN
    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE(' INICIO DE PRUEBAS DE ACEPTACION - BIBLIOTECA WEB ');
    DBMS_OUTPUT.PUT_LINE('==================================================');

    -- Limpieza previa para pruebas
    DELETE FROM biblioteca.PRESTAMO WHERE ID_SOCIO = (SELECT ID_SOCIO FROM biblioteca.SOCIO WHERE USUARIO='socio2');
    DELETE FROM biblioteca.BLOQUEO WHERE ID_SOCIO = (SELECT ID_SOCIO FROM biblioteca.SOCIO WHERE USUARIO='socio2');
    UPDATE biblioteca.SOCIO SET PENALIZACION_HASTA = NULL, MAX_PRESTAMOS_ACTIVOS = 3 WHERE USUARIO = 'socio2';
    COMMIT;

    SELECT ID_SOCIO INTO v_socio_id FROM biblioteca.SOCIO WHERE USUARIO='socio2';
    
    -- Obtener 2 ejemplares disponibles
    SELECT ID_EJEMPLAR INTO v_ejemplar_id FROM biblioteca.EJEMPLAR WHERE ROWNUM = 1 AND ESTADO = 'DISPONIBLE';
    SELECT ID_EJEMPLAR INTO v_ejemplar_2_id FROM biblioteca.EJEMPLAR WHERE ROWNUM = 1 AND ESTADO = 'DISPONIBLE' AND ID_EJEMPLAR != v_ejemplar_id;

    -- ============================================================
    -- CRITERIO 1: SOCIO PUEDE BLOQUEAR DISPONIBLE Y NO MAS DE 1
    -- ============================================================
    DBMS_OUTPUT.PUT_LINE('--- PRUEBA 1: Bloqueos y Limites ---');
    
    -- 1.1 Bloquear 1 ejemplar (Debe funcionar)
    ASSERT_SUCCESS('Bloquear 1er ejemplar disponible', 
        'INSERT INTO biblioteca.BLOQUEO (ID_SOCIO, ID_EJEMPLAR, FECHA_FIN, ESTADO) VALUES (' || v_socio_id || ', ' || v_ejemplar_id || ', SYSDATE+1, ''ACTIVO'')');

    -- 1.2 Intentar bloquear 2do ejemplar (Debe fallar - Trigger)
    ASSERT_ERROR('Intentar bloquear 2do ejemplar (Limite 1)', 
        'INSERT INTO biblioteca.BLOQUEO (ID_SOCIO, ID_EJEMPLAR, FECHA_FIN, ESTADO) VALUES (' || v_socio_id || ', ' || v_ejemplar_2_id || ', SYSDATE+1, ''ACTIVO'')', 
        -20101); -- Codigo error definido en TRG_VALIDAR_BLOQUEO_INSERT

    -- ============================================================
    -- CRITERIO 2: JOB FINAL DIA (Limpieza)
    -- ============================================================
    DBMS_OUTPUT.PUT_LINE('--- PRUEBA 2: Job Diario de Limpieza ---');
    
    -- Forzar caducidad del bloqueo actual (poner fecha ayer)
    UPDATE biblioteca.BLOQUEO SET FECHA_FIN = SYSDATE - 1 WHERE ID_SOCIO = v_socio_id AND ESTADO = 'ACTIVO';
    
    -- Ejecutar Procedure del Job Manualmente
    biblioteca.SP_LIMPIEZA_DIARIA;
    
    -- Verificar que estado cambio a EXPIRADO
    DECLARE
        v_estado_bloq VARCHAR2(20);
        v_estado_ejem VARCHAR2(20);
    BEGIN
        SELECT ESTADO INTO v_estado_bloq FROM biblioteca.BLOQUEO WHERE ID_SOCIO = v_socio_id ORDER BY ID_BLOQUEO DESC FETCH FIRST 1 ROWS ONLY;
        SELECT ESTADO INTO v_estado_ejem FROM biblioteca.EJEMPLAR WHERE ID_EJEMPLAR = v_ejemplar_id;
        
        IF v_estado_bloq = 'EXPIRADO' AND v_estado_ejem = 'DISPONIBLE' THEN
             DBMS_OUTPUT.PUT_LINE('✅ PASO: Job limpieza (Bloqueo->EXPIRADO, Ejemplar->DISPONIBLE)');
             v_pass_count := v_pass_count + 1;
        ELSE
             DBMS_OUTPUT.PUT_LINE('❌ FALLO: Job limpieza (Estados incorrectos: ' || v_estado_bloq || '/' || v_estado_ejem || ')');
        END IF;
        v_test_count := v_test_count + 1;
    END;

    -- ============================================================
    -- CRITERIO 3: TRIGGERS DE PRESTAMO (Bibliotecario)
    -- ============================================================
    DBMS_OUTPUT.PUT_LINE('--- PRUEBA 3: Validaciones de Prestamo ---');
    
    -- 3.1 Socio Penalizado
    UPDATE biblioteca.SOCIO SET PENALIZACION_HASTA = SYSDATE + 7 WHERE ID_SOCIO = v_socio_id;
    ASSERT_ERROR('Impedir prestamo a socio penalizado', 
        'INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PREVISTA_DEVOLUCION, ESTADO) VALUES (' || v_socio_id || ', ' || v_ejemplar_id || ', SYSDATE+7, ''ACTIVO'')',
        -20001);
    
    -- Quitar penalizacion
    UPDATE biblioteca.SOCIO SET PENALIZACION_HASTA = NULL WHERE ID_SOCIO = v_socio_id;

    -- 3.2 Exceso de Prestamos
    UPDATE biblioteca.SOCIO SET MAX_PRESTAMOS_ACTIVOS = 0 WHERE ID_SOCIO = v_socio_id; -- Forzar limite 0
    ASSERT_ERROR('Impedir prestamo si cupo lleno', 
        'INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PREVISTA_DEVOLUCION, ESTADO) VALUES (' || v_socio_id || ', ' || v_ejemplar_id || ', SYSDATE+7, ''ACTIVO'')',
        -20002);
    UPDATE biblioteca.SOCIO SET MAX_PRESTAMOS_ACTIVOS = 3 WHERE ID_SOCIO = v_socio_id; -- Restaurar

    -- 3.3 Ejemplar No Disponible (Simular que esta prestado)
    UPDATE biblioteca.EJEMPLAR SET ESTADO = 'PRESTADO' WHERE ID_EJEMPLAR = v_ejemplar_id;
     ASSERT_ERROR('Impedir prestar ejemplar ya prestado', 
        'INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PREVISTA_DEVOLUCION, ESTADO) VALUES (' || v_socio_id || ', ' || v_ejemplar_id || ', SYSDATE+7, ''ACTIVO'')',
        -20003);
    UPDATE biblioteca.EJEMPLAR SET ESTADO = 'DISPONIBLE' WHERE ID_EJEMPLAR = v_ejemplar_id; -- Restaurar

    -- ============================================================
    -- CRITERIO 4: PRESTAMO VALIDO Y CONVERSION
    -- ============================================================
    DBMS_OUTPUT.PUT_LINE('--- PRUEBA 4: Flujo Exitoso (Bloqueo -> Prestamo) ---');
    
    -- Crear bloqueo valido
    INSERT INTO biblioteca.BLOQUEO (ID_SOCIO, ID_EJEMPLAR, FECHA_FIN, ESTADO) VALUES (v_socio_id, v_ejemplar_id, SYSDATE+1, 'ACTIVO');
    
    -- Insertar Prestamo (Debe funcionar y convertir bloqueo)
    ASSERT_SUCCESS('Insertar Prestamo Valido (con bloqueo previo)', 
        'INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PREVISTA_DEVOLUCION, ESTADO) VALUES (' || v_socio_id || ', ' || v_ejemplar_id || ', SYSDATE+7, ''ACTIVO'')');

    -- Verificar conversion
    DECLARE
        v_estado_bloq VARCHAR2(20);
        v_estado_ejem VARCHAR2(20);
    BEGIN
        SELECT ESTADO INTO v_estado_bloq FROM biblioteca.BLOQUEO WHERE ID_SOCIO = v_socio_id AND ID_EJEMPLAR = v_ejemplar_id ORDER BY ID_BLOQUEO DESC FETCH FIRST 1 ROWS ONLY;
        SELECT ESTADO INTO v_estado_ejem FROM biblioteca.EJEMPLAR WHERE ID_EJEMPLAR = v_ejemplar_id;
        
        IF v_estado_bloq = 'CONVERTIDO' AND v_estado_ejem = 'PRESTADO' THEN
             DBMS_OUTPUT.PUT_LINE('✅ PASO: Conversion Correcta (Bloqueo->CONVERTIDO, Ejemplar->PRESTADO)');
             v_pass_count := v_pass_count + 1;
        ELSE
             DBMS_OUTPUT.PUT_LINE('❌ FALLO: Conversion (Estados: ' || v_estado_bloq || '/' || v_estado_ejem || ')');
        END IF;
        v_test_count := v_test_count + 1;
    END;

    DBMS_OUTPUT.PUT_LINE('==================================================');
    DBMS_OUTPUT.PUT_LINE(' RESULTADO FINAL: ' || v_pass_count || '/' || v_test_count || ' PRUEBAS EXITOSAS');
    DBMS_OUTPUT.PUT_LINE('==================================================');
    
    ROLLBACK; -- Deshacer cambios de prueba
END;
/
EXIT;
