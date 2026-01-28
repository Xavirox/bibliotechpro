CONNECT system/Oracle123@//localhost:1521/XEPDB1;
SET SQLBLANKLINES ON;
SET SERVEROUTPUT ON;

PROMPT --- RE-CREATING UNIQUE INDEX (Safety Check) ---
BEGIN EXECUTE IMMEDIATE 'DROP INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
CREATE UNIQUE INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO ON biblioteca.BLOQUEO (CASE WHEN ESTADO = 'ACTIVO' THEN ID_SOCIO ELSE NULL END);

PROMPT --- TEST: STRICT DOUBLE INSERT ---
DECLARE
    v_socio_id NUMBER;
BEGIN
    SELECT id_socio INTO v_socio_id FROM biblioteca.socio WHERE usuario = 'admin' FETCH FIRST 1 ROWS ONLY;
    
    -- 1. Insert First Active Block (Should Succeed)
    INSERT INTO biblioteca.bloqueo (id_socio, id_ejemplar, fecha_inicio, fecha_fin, estado)
    VALUES (v_socio_id, 1, SYSDATE, SYSDATE+1, 'ACTIVO');
    
    DBMS_OUTPUT.PUT_LINE('1. First insert OK.');

    -- 2. Insert Second Active Block (MUST FAIL)
    BEGIN
        INSERT INTO biblioteca.bloqueo (id_socio, id_ejemplar, fecha_inicio, fecha_fin, estado)
        VALUES (v_socio_id, 1, SYSDATE, SYSDATE+1, 'ACTIVO');
        
        -- If we are here, it failed to block
        DBMS_OUTPUT.PUT_LINE('!!! FAILURE: Second insert SUCCEEDED. Constraint broken. !!!');
    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('SUCCESS: Second insert BLOCKED: ' || SQLERRM);
    END;
    
    ROLLBACK;
END;
/
EXIT;
