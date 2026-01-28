CONNECT system/Oracle123@//localhost:1521/XEPDB1;
SET PAGESIZE 0 FEEDBACK OFF VERIFY OFF HEADING OFF ECHO OFF;
SELECT 'USER_FOUND: ' || usuario || ' ROLE: ' || rol FROM biblioteca.socio WHERE usuario='admin';
EXIT;
