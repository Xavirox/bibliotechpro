CONNECT system/Oracle123@//localhost:1521/XEPDB1;
SET LINESIZE 200 PAGESIZE 50;

PROMPT --- INDICES ON BLOQUEO TABLE ---
SELECT index_name, uniqueness, status FROM all_indexes WHERE table_owner = 'BIBLIOTECA' AND table_name = 'BLOQUEO';

PROMPT --- INDEX COLUMNS ---
SELECT index_name, column_name, column_position FROM all_ind_columns WHERE index_owner = 'BIBLIOTECA' AND table_name = 'BLOQUEO';

PROMPT --- ACTIVE RESERVATIONS FOR SOCIO1 ---
SELECT s.usuario, b.id_bloqueo, b.estado, e.codigo_barras FROM biblioteca.bloqueo b JOIN biblioteca.socio s ON b.id_socio = s.id_socio JOIN biblioteca.ejemplar e ON b.id_ejemplar = e.id_ejemplar WHERE s.usuario = 'socio1' AND b.estado = 'ACTIVO';

PROMPT --- ATTEMPTING DUPLICATE INSERT (SHOULD FAIL) ---
INSERT INTO biblioteca.bloqueo (id_socio, id_ejemplar, fecha_inicio, fecha_fin, estado) SELECT id_socio, 1, SYSDATE, SYSDATE+1, 'ACTIVO' FROM biblioteca.socio WHERE usuario = 'socio1';
COMMIT;

EXIT;
