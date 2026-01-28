-- ==========================================
-- SCRIPT: FIX UNIQUE RESERVATION INDEX
-- Purpose: Reinforce the 1 active reservation per user rule
-- ==========================================

ALTER SESSION SET CONTAINER=XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = biblioteca;

-- 1. Drop existing index if it exists (to avoid conflicts or stale definitions)
BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 THEN -- ORA-01418: specified index does not exist
            RAISE;
        END IF;
END;
/

-- 2. Create the unique index
-- This ensures that for a given ID_SOCIO, there can only be one row where ESTADO is 'ACTIVO'.
-- Rows where ESTADO is not 'ACTIVO' will have NULL for this calculated column, which Oracle ignores for uniqueness.
CREATE UNIQUE INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO 
ON biblioteca.BLOQUEO (CASE WHEN ESTADO = 'ACTIVO' THEN ID_SOCIO ELSE NULL END);

-- 3. Verification
SELECT index_name, status, uniqueness 
FROM all_indexes 
WHERE owner = 'BIBLIOTECA' AND index_name = 'IDX_UN_BLOQUEO_ACTIVO';

EXIT;
