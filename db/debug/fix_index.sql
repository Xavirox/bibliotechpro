CONNECT system/Oracle123@//localhost:1521/XEPDB1;
SET SQLBLANKLINES ON;
SET SERVEROUTPUT ON;
PROMPT Cleaning duplicates...
DELETE FROM biblioteca.bloqueo b1 WHERE estado = 'ACTIVO' AND rowid < (SELECT MAX(rowid) FROM biblioteca.bloqueo b2 WHERE b2.id_socio = b1.id_socio AND b2.estado = 'ACTIVO');
COMMIT;
PROMPT Dropping potential non-unique index...
BEGIN EXECUTE IMMEDIATE 'DROP INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
PROMPT Creating UNIQUE index...
CREATE UNIQUE INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO ON biblioteca.BLOQUEO (CASE WHEN ESTADO = 'ACTIVO' THEN ID_SOCIO ELSE NULL END);
PROMPT Verifying constraint...
DECLARE
  v_socio_id NUMBER;
BEGIN
  SELECT id_socio INTO v_socio_id FROM biblioteca.socio WHERE usuario = 'sofia_l' FETCH FIRST 1 ROWS ONLY;
  BEGIN
    INSERT INTO biblioteca.bloqueo (id_socio, id_ejemplar, fecha_inicio, fecha_fin, estado) VALUES (v_socio_id, 1, SYSDATE, SYSDATE+1, 'ACTIVO');
    DBMS_OUTPUT.PUT_LINE('!!! FAILURE: Constrain BROKEN !!!');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('SUCCESS: Constraint VIOLATED as expected.');
  END;
  ROLLBACK;
END;
/
EXIT;
