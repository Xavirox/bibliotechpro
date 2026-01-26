#!/bin/bash
# DEMO FINAL
echo '1. Login SuperDev...'
TOKEN=$(curl -s -X POST http://localhost:9141/api/auth/login -H 'Content-Type: application/json' -d '{"username":"superdev","password":"password"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then echo 'FATAL: Login failed even with superdev'; exit 1; fi
echo 'Login OK'

echo '2. Finding Loan...'
LOAN_ID=$(curl -s -X GET http://localhost:9141/api/prestamos -H "Authorization: Bearer $TOKEN" | grep -o '"idPrestamo":[0-9]*' | head -n1 | cut -d: -f2)
if [ -z "$LOAN_ID" ]; then LOAN_ID=1; fi
echo "Loan ID: $LOAN_ID"

echo '3. Time Hack (SQL)...'
docker exec -i bibliotech-oracle-vps sqlplus -s biblioteca/biblioteca123@//localhost:1521/XEPDB1 <<SQL
UPDATE PRESTAMO SET FECHA_PREVISTA_DEVOLUCION = SYSDATE - 1 WHERE ID_PRESTAMO = $LOAN_ID;
COMMIT;
EXIT;
SQL

echo '4. Returning Book...'
curl -s -X POST http://localhost:9141/api/prestamos/devolver/$LOAN_ID -H "Authorization: Bearer $TOKEN"
echo ''

echo '5. LOGS DE EXITO (Java -> n8n):'
sleep 2
docker logs bibliotech-backend-vps --tail 50 | grep 'Enviando webhook'
