#!/bin/bash
# Script de Demostración INTERNAL (Running on VPS)

echo '1. Login Admin...'
# Usamos un login simple y extraemos token con grep/cut
TOKEN=$(curl -s -X POST http://localhost:9141/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"password"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo 'Login Failed - Trying biblio'
  TOKEN=$(curl -s -X POST http://localhost:9141/api/auth/login -H 'Content-Type: application/json' -d '{"username":"biblio","password":"password"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  if [ -z "$TOKEN" ]; then echo 'FATAL: Login impossible'; exit 1; fi
fi
echo 'Login OK'

# Buscar ID prestamo
# Asumimos que la API /api/prestamos devuelve algun JSON array. Cogemos el primer ID.
LOAN_ID=$(curl -s -X GET http://localhost:9141/api/prestamos -H "Authorization: Bearer $TOKEN" | grep -o '"idPrestamo":[0-9]*' | head -n1 | cut -d: -f2)

if [ -z "$LOAN_ID" ]; then
  echo 'No loans found via API - forcing via SQL ID=1'
  LOAN_ID=1
fi

echo "Target Loan: $LOAN_ID"

# Hack SQL
echo '3. Time Hacking (Simulating delay)...'
docker exec -i bibliotech-oracle-vps sqlplus -s biblioteca/biblioteca123@//localhost:1521/XEPDB1 <<SQL
UPDATE PRESTAMO SET FECHA_PREVISTA_DEVOLUCION = SYSDATE - 1 WHERE ID_PRESTAMO = $LOAN_ID;
COMMIT;
EXIT;
SQL

# Return
echo '4. Triggering Return...'
curl -s -X POST http://localhost:9141/api/prestamos/devolver/$LOAN_ID -H "Authorization: Bearer $TOKEN"
echo ''

# Check logs
echo '5. Checking Logs...'
sleep 2
docker logs bibliotech-backend-vps --tail 20 | grep 'Enviando webhook'
