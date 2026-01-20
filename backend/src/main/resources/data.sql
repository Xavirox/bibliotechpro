-- Usuarios iniciales
INSERT INTO socio (usuario, password_hash, nombre, rol, email, max_prestamos_activos) VALUES ('admin', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'Administrador', 'ADMIN', 'admin@biblioteca.com', 5);
INSERT INTO socio (usuario, password_hash, nombre, rol, email, max_prestamos_activos) VALUES ('bibliotecario', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'Bibliotecario Jefe', 'BIBLIOTECARIO', 'biblio@biblioteca.com', 3);
INSERT INTO socio (usuario, password_hash, nombre, rol, email, max_prestamos_activos) VALUES ('user1', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'Juan Lector', 'SOCIO', 'juan@email.com', 3);
INSERT INTO socio (usuario, password_hash, nombre, rol, email, max_prestamos_activos) VALUES ('user2', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'Ana Libro', 'SOCIO', 'ana@email.com', 3);

-- Libros de ejemplo
INSERT INTO libro (titulo, autor, isbn, categoria, anio) VALUES 
('Cien Años de Soledad', 'Gabriel García Márquez', '978-84-376-0494-7', 'Novela', 1967),
('El Señor de los Anillos', 'J.R.R. Tolkien', '978-0-261-10238-5', 'Fantasía', 1954),
('1984', 'George Orwell', '978-0-452-28423-4', 'Ciencia Ficción', 1949),
('Clean Code', 'Robert C. Martin', '978-0-13-235088-4', 'Tecnología', 2008),
('Steve Jobs', 'Walter Isaacson', '978-1-4516-4853-9', 'Biografía', 2011);

-- Ejemplares (2 copias de cada uno)
INSERT INTO ejemplar (estado, id_libro, codigo_barras) VALUES ('DISPONIBLE', 1, 'L001-C01');
INSERT INTO ejemplar (estado, id_libro, codigo_barras) VALUES ('DISPONIBLE', 1, 'L001-C02');
INSERT INTO ejemplar (estado, id_libro, codigo_barras) VALUES ('DISPONIBLE', 2, 'L002-C01');
INSERT INTO ejemplar (estado, id_libro, codigo_barras) VALUES ('DISPONIBLE', 2, 'L002-C02');
INSERT INTO ejemplar (estado, id_libro, codigo_barras) VALUES ('DISPONIBLE', 3, 'L003-C01');
INSERT INTO ejemplar (estado, id_libro, codigo_barras) VALUES ('DISPONIBLE', 4, 'L004-C01');
INSERT INTO ejemplar (estado, id_libro, codigo_barras) VALUES ('DISPONIBLE', 5, 'L005-C01');
