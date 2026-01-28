#!/bin/bash
# Script to reset remote database
# Copy SQL files to container
docker cp db/00_init.sql bibliotech-oracle-vps:/tmp/00_init.sql
docker cp db/01_demo_data.sql bibliotech-oracle-vps:/tmp/01_demo_data.sql

# Execute with SQLPlus
echo "Ejecutando 00_init.sql..."
docker exec bibliotech-oracle-vps sqlplus system/Oracle123@//localhost:1521/XEPDB1 @/tmp/00_init.sql

echo "Ejecutando 01_demo_data.sql..."
docker exec bibliotech-oracle-vps sqlplus system/Oracle123@//localhost:1521/XEPDB1 @/tmp/01_demo_data.sql

echo "Database reset complete."
