-- SCRIPT DE DATOS (SOLO INSERTs)
ALTER SESSION SET CURRENT_SCHEMA = biblioteca;

-- From db/05_seed.sql.bak
-- Datos de prueba

-- Usuarios
-- Password hash para 'password' (ejemplo BCrypt, aunque aquí pondremos un placeholder si no lo generamos desde Java)
-- En producción usar hashes reales. Aquí usaremos texto plano temporalmente o un hash fijo conocido.
-- Supongamos que $2a$10$Xavi... es el hash de '1234'
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES ('admin', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'ADMIN', 'Administrador', 'admin@biblio.com', 3);
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES ('biblio', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'BIBLIOTECARIO', 'Bibliotecario Jefe', 'biblio@biblio.com', 3);
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES ('socio1', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'SOCIO', 'Juan Socio', 'juan@email.com', 3);

-- Libros
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0134685991', 'Effective Java', 'Joshua Bloch', 'Tecnología', 2018);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0321125217', 'Domain-Driven Design', 'Eric Evans', 'Tecnología', 2003);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-8401020414', 'Dune', 'Frank Herbert', 'Ciencia Ficción', 1965);

-- Ejemplares
-- Effective Java (2 copias)
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES (1, 'EJ-001', 'DISPONIBLE', 'Estantería A1');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES (1, 'EJ-002', 'DISPONIBLE', 'Estantería A1');

-- DDD (1 copia)
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES (2, 'EJ-003', 'DISPONIBLE', 'Estantería B2');

-- Dune (1 copia)
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES (3, 'EJ-004', 'DISPONIBLE', 'Estantería C3');

COMMIT;


-- From db/06_more_seed.sql.bak
-- Extended Seed Data

-- 1. More Books
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0451524935', '1984', 'George Orwell', 'Ciencia Ficción', 1949);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0061120084', 'To Kill a Mockingbird', 'Harper Lee', 'Clásico', 1960);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0743273565', 'The Great Gatsby', 'F. Scott Fitzgerald', 'Clásico', 1925);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0553380163', 'A Brief History of Time', 'Stephen Hawking', 'Ciencia', 1988);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0132350884', 'Clean Code', 'Robert C. Martin', 'Tecnología', 2008);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0201633610', 'Design Patterns', 'Erich Gamma', 'Tecnología', 1994);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-1491950357', 'Building Microservices', 'Sam Newman', 'Tecnología', 2015);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0307474278', 'The Da Vinci Code', 'Dan Brown', 'Misterio', 2003);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0547928227', 'The Hobbit', 'J.R.R. Tolkien', 'Fantasía', 1937);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0439023481', 'The Hunger Games', 'Suzanne Collins', 'Fantasía', 2008);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0385504201', 'The Da Vinci Code', 'Dan Brown', 'Thriller', 2003);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0140449136', 'Crime and Punishment', 'Fyodor Dostoevsky', 'Clásico', 1866);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0679783268', 'Pride and Prejudice', 'Jane Austen', 'Romance', 1813);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0747532743', 'Harry Potter and the Philosophers Stone', 'J.K. Rowling', 'Fantasía', 1997);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0062315007', 'The Alchemist', 'Paulo Coelho', 'Ficción', 1988);

-- 2. Copies (Ejemplares)
-- Assuming IDs continue from previous seed (1, 2, 3 were used). New books start at 4.
-- We'll just insert blindly assuming auto-increment works or we can use subqueries if needed, but simple inserts are fine for this demo.

-- 1984
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM LIBRO WHERE TITULO='1984'), 'EJ-1984-1', 'DISPONIBLE', 'Estantería F1');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM LIBRO WHERE TITULO='1984'), 'EJ-1984-2', 'DISPONIBLE', 'Estantería F1');

-- Clean Code
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM LIBRO WHERE TITULO='Clean Code'), 'EJ-CLEAN-1', 'DISPONIBLE', 'Estantería T1');

-- Design Patterns
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM LIBRO WHERE TITULO='Design Patterns'), 'EJ-DP-1', 'DISPONIBLE', 'Estantería T2');

-- Hobbit
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM LIBRO WHERE TITULO='The Hobbit'), 'EJ-HOB-1', 'DISPONIBLE', 'Estantería F2');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM LIBRO WHERE TITULO='The Hobbit'), 'EJ-HOB-2', 'PRESTADO', 'Estantería F2'); -- One borrowed

-- 3. History for Socio1 (ID usually 3, but let's look it up)
-- We want socio1 to have read Sci-Fi and Tech so Gemini recommends similar.
-- We need to insert into PRESTAMO with ESTADO='DEVUELTO'

-- Prestamo 1: Dune (ID 3)
INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PRESTAMO, FECHA_PREVISTA_DEVOLUCION, FECHA_DEVOLUCION_REAL, ESTADO)
VALUES (
    (SELECT ID_SOCIO FROM SOCIO WHERE USUARIO='socio1'),
    (SELECT ID_EJEMPLAR FROM EJEMPLAR WHERE CODIGO_BARRAS='EJ-004'), -- Dune
    SYSDATE - 60,
    SYSDATE - 45,
    SYSDATE - 50,
    'DEVUELTO'
);

-- Prestamo 2: Effective Java (ID 1)
INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PRESTAMO, FECHA_PREVISTA_DEVOLUCION, FECHA_DEVOLUCION_REAL, ESTADO)
VALUES (
    (SELECT ID_SOCIO FROM SOCIO WHERE USUARIO='socio1'),
    (SELECT ID_EJEMPLAR FROM EJEMPLAR WHERE CODIGO_BARRAS='EJ-001'), -- Effective Java
    SYSDATE - 30,
    SYSDATE - 15,
    SYSDATE - 20,
    'DEVUELTO'
);

COMMIT;


-- From db/04_update_passwords.sql.bak
-- Update passwords to 'password' (hash: $2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW)
UPDATE biblioteca.SOCIO SET PASSWORD_HASH = '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW';
COMMIT;


COMMIT;
