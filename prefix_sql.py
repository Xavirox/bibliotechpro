import re
import sys

# Read the SQL file
with open('db/01_schema.sql', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace CREATE TABLE statements
content = re.sub(r'\bCREATE TABLE (\w+)', r'CREATE TABLE biblioteca.\1', content)

# Replace CREATE INDEX statements
content = re.sub(r'\bCREATE (UNIQUE )?INDEX (\w+)', r'CREATE \1INDEX biblioteca.\2', content)

# Replace ON table_name in index definitions
content = re.sub(r'\bON (\w+)\s*\(', r'ON biblioteca.\1 (', content)

# Replace REFERENCES in foreign keys
content = re.sub(r'\bREFERENCES (\w+)\s*\(', r'REFERENCES biblioteca.\1(', content)

# Write the modified content
with open('db/01_schema_prefixed.sql', 'w', encoding='utf-8') as f:
    f.write(content)

print("Schema file processed successfully")
