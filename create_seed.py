import re

# List of seed files
seed_files = [
    'db/05_seed.sql.bak',
    'db/06_more_seed.sql.bak',
    'db/04_update_passwords.sql.bak'
]

output = "-- SCRIPT DE DATOS (SOLO INSERTs)\n"
output += "ALTER SESSION SET CURRENT_SCHEMA = biblioteca;\n\n"

for fpath in seed_files:
    try:
        with open(fpath, 'r', encoding='utf-8') as f:
            content = f.read()
            # Prefix INSERT INTO
            content = re.sub(r'INSERT INTO (\w+)', r'INSERT INTO biblioteca.\1', content)
            
            # Fix SOCIO inserts to include MAX_PRESTAMOS_ACTIVOS (Hibernate might have missed the default)
            content = re.sub(
                r'INSERT INTO biblioteca.SOCIO \(USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL\) VALUES \((.*?)\);',
                r'INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES (\1, 3);',
                content
            )
            
            # Prefix UPDATE
            content = re.sub(r'UPDATE (\w+)', r'UPDATE biblioteca.\1', content)
            
            output += f"-- From {fpath}\n"
            output += content + "\n\n"
    except Exception as e:
        print(f"Skipping {fpath}: {e}")

output += "COMMIT;\n"

with open('db/clean_seed.sql', 'w', encoding='utf-8') as f:
    f.write(output)

print("Created db/clean_seed.sql")
