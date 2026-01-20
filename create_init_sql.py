import re

# List of SQL files to process in order
sql_files = [
    'db/01_schema.sql',
    'db/02_triggers.sql',
    'db/04_update_passwords.sql',
    'db/05_seed.sql',
    'db/06_more_seed.sql',
    'db/07_massive_seed.sql',
    'db/08_jobs.sql',
    'db/09_cleanup_socio1.sql',
    'db/10_fix_version_column.sql'
]

# Start with header
output = """-- ==========================================
-- SCRIPT MAESTRO DE INICIALIZACIÓN
-- Todos los objetos se crean en el esquema biblioteca
-- ==========================================

"""

for sql_file in sql_files:
    try:
        with open(sql_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Add section header
        output += f"\n-- ==========================================\n"
        output += f"-- {sql_file}\n"
        output += f"-- ==========================================\n\n"
        
        # Replace CREATE TABLE statements
        content = re.sub(r'\bCREATE TABLE (\w+)', r'CREATE TABLE biblioteca.\1', content)
        
        # Replace CREATE INDEX statements  
        content = re.sub(r'\bCREATE (UNIQUE )?INDEX (\w+)', r'CREATE \1INDEX biblioteca.\2', content)
        
        # Replace CREATE TRIGGER statements
        content = re.sub(r'\bCREATE OR REPLACE TRIGGER (\w+)', r'CREATE OR REPLACE TRIGGER biblioteca.\1', content)
        
        # Replace CREATE PROCEDURE statements
        content = re.sub(r'\bCREATE OR REPLACE PROCEDURE (\w+)', r'CREATE OR REPLACE PROCEDURE biblioteca.\1', content)
        
        # Replace ON table_name in various contexts (indexes, triggers)
        content = re.sub(r'\bON (\w+)\s*\(', r'ON biblioteca.\1 (', content)
        content = re.sub(r'\bON (\w+)\s+FOR EACH ROW', r'ON biblioteca.\1 FOR EACH ROW', content)
        content = re.sub(r'\bBEFORE INSERT ON (\w+)', r'BEFORE INSERT ON biblioteca.\1', content)
        content = re.sub(r'\bBEFORE UPDATE ON (\w+)', r'BEFORE UPDATE ON biblioteca.\1', content)
        content = re.sub(r'\bAFTER INSERT ON (\w+)', r'AFTER INSERT ON biblioteca.\1', content)
        content = re.sub(r'\bAFTER UPDATE ON (\w+)', r'AFTER UPDATE ON biblioteca.\1', content)
        
        # Replace REFERENCES in foreign keys
        content = re.sub(r'\bREFERENCES (\w+)\s*\(', r'REFERENCES biblioteca.\1(', content)
        
        # Replace FROM table_name
        content = re.sub(r'\bFROM (\w+)(\s|$|,|\))', r'FROM biblioteca.\1\2', content)
        
        # Replace UPDATE table_name
        content = re.sub(r'\bUPDATE (\w+)\s+SET', r'UPDATE biblioteca.\1 SET', content)
        
        # Replace INSERT INTO table_name
        content = re.sub(r'\bINSERT INTO (\w+)', r'INSERT INTO biblioteca.\1', content)
        
        # Replace ALTER TABLE table_name
        content = re.sub(r'\bALTER TABLE (\w+)', r'ALTER TABLE biblioteca.\1', content)
        
        # Replace WHERE clauses with table references
        content = re.sub(r'\bWHERE (\w+)\.', r'WHERE biblioteca.\1.', content)
        
        # Replace JOIN clauses
        content = re.sub(r'\bJOIN (\w+)\s', r'JOIN biblioteca.\1 ', content)
        
        output += content + "\n\n"
        
    except Exception as e:
        print(f"Error processing {sql_file}: {e}")

# Write the consolidated file
with open('db/00_init.sql', 'w', encoding='utf-8') as f:
    f.write(output)

print("Consolidated SQL file created successfully with biblioteca schema prefixes")
