-- ==========================================
-- SCRIPT MAESTRO DE INICIALIZACIÓN Y DATOS (GOLD MASTER v2)
-- BiblioTech Pro - VPS Edition 
-- ==========================================

-- Configuración de sesión
ALTER SESSION SET CONTAINER=XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = biblioteca;
GRANT UNLIMITED TABLESPACE TO biblioteca;

-- 1. LIMPIEZA TOTAL
-- ==========================================
BEGIN
    FOR r IN (SELECT table_name FROM all_tables WHERE owner = 'BIBLIOTECA') LOOP
        EXECUTE IMMEDIATE 'DROP TABLE BIBLIOTECA.' || r.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
    
    FOR s IN (SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'BIBLIOTECA') LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE BIBLIOTECA.' || s.sequence_name;
    END LOOP;
END;
/

-- 2. ESTRUCTURA (DDL)
-- ==========================================

-- Secuencias Explícitas
CREATE SEQUENCE biblioteca.SEQ_SOCIO START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE biblioteca.SEQ_LIBRO START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE biblioteca.SEQ_EJEMPLAR START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE biblioteca.SEQ_PRESTAMO START WITH 100 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE biblioteca.SEQ_BLOQUEO START WITH 100 INCREMENT BY 1 NOCACHE;

CREATE TABLE biblioteca.SOCIO (
    ID_SOCIO NUMBER DEFAULT biblioteca.SEQ_SOCIO.NEXTVAL PRIMARY KEY,
    USUARIO VARCHAR2(50) NOT NULL UNIQUE,
    PASSWORD_HASH VARCHAR2(255) NOT NULL,
    ROL VARCHAR2(20) CHECK (ROL IN ('SOCIO', 'BIBLIOTECARIO', 'ADMIN')) NOT NULL,
    PENALIZACION_HASTA DATE,
    MAX_PRESTAMOS_ACTIVOS NUMBER DEFAULT 1 NOT NULL,
    NOMBRE VARCHAR2(100),
    EMAIL VARCHAR2(100)
);

CREATE TABLE biblioteca.LIBRO (
    ID_LIBRO NUMBER DEFAULT biblioteca.SEQ_LIBRO.NEXTVAL PRIMARY KEY,
    ISBN VARCHAR2(20) UNIQUE NOT NULL,
    TITULO VARCHAR2(200) NOT NULL,
    AUTOR VARCHAR2(100) NOT NULL,
    CATEGORIA VARCHAR2(50),
    ANIO NUMBER(4)
);

CREATE TABLE biblioteca.EJEMPLAR (
    ID_EJEMPLAR NUMBER DEFAULT biblioteca.SEQ_EJEMPLAR.NEXTVAL PRIMARY KEY,
    ID_LIBRO NUMBER NOT NULL,
    CODIGO_BARRAS VARCHAR2(50) UNIQUE NOT NULL,
    ESTADO VARCHAR2(20) CHECK (ESTADO IN ('DISPONIBLE', 'BLOQUEADO', 'PRESTADO', 'BAJA')) NOT NULL,
    UBICACION VARCHAR2(100),
    VERSION NUMBER DEFAULT 0,
    CONSTRAINT FK_EJEMPLAR_LIBRO FOREIGN KEY (ID_LIBRO) REFERENCES biblioteca.LIBRO(ID_LIBRO)
);

CREATE TABLE biblioteca.BLOQUEO (
    ID_BLOQUEO NUMBER DEFAULT biblioteca.SEQ_BLOQUEO.NEXTVAL PRIMARY KEY,
    ID_SOCIO NUMBER NOT NULL,
    ID_EJEMPLAR NUMBER NOT NULL,
    FECHA_INICIO TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FECHA_FIN TIMESTAMP NOT NULL,
    ESTADO VARCHAR2(20) CHECK (ESTADO IN ('ACTIVO', 'CANCELADO', 'EXPIRADO', 'CONVERTIDO')) NOT NULL,
    CONSTRAINT FK_BLOQUEO_SOCIO FOREIGN KEY (ID_SOCIO) REFERENCES biblioteca.SOCIO(ID_SOCIO),
    CONSTRAINT FK_BLOQUEO_EJEMPLAR FOREIGN KEY (ID_EJEMPLAR) REFERENCES biblioteca.EJEMPLAR(ID_EJEMPLAR)
);

CREATE TABLE biblioteca.PRESTAMO (
    ID_PRESTAMO NUMBER DEFAULT biblioteca.SEQ_PRESTAMO.NEXTVAL PRIMARY KEY,
    ID_SOCIO NUMBER NOT NULL,
    ID_EJEMPLAR NUMBER NOT NULL,
    FECHA_PRESTAMO DATE DEFAULT SYSDATE NOT NULL,
    FECHA_PREVISTA_DEVOLUCION DATE NOT NULL,
    FECHA_DEVOLUCION_REAL DATE,
    ESTADO VARCHAR2(20) CHECK (ESTADO IN ('ACTIVO', 'DEVUELTO')) NOT NULL,
    ID_BLOQUEO NUMBER,
    CONSTRAINT FK_PRESTAMO_SOCIO FOREIGN KEY (ID_SOCIO) REFERENCES biblioteca.SOCIO(ID_SOCIO),
    CONSTRAINT FK_PRESTAMO_EJEMPLAR FOREIGN KEY (ID_EJEMPLAR) REFERENCES biblioteca.EJEMPLAR(ID_EJEMPLAR),
    CONSTRAINT FK_PRESTAMO_BLOQUEO FOREIGN KEY (ID_BLOQUEO) REFERENCES biblioteca.BLOQUEO(ID_BLOQUEO)
);

-- Índices
CREATE INDEX biblioteca.IDX_EJEMPLAR_LIBRO ON biblioteca.EJEMPLAR (ID_LIBRO);
CREATE INDEX biblioteca.IDX_BLOQUEO_SOCIO ON biblioteca.BLOQUEO (ID_SOCIO);
CREATE INDEX biblioteca.IDX_PRESTAMO_SOCIO ON biblioteca.PRESTAMO (ID_SOCIO);
CREATE UNIQUE INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO ON biblioteca.BLOQUEO (CASE WHEN ESTADO = 'ACTIVO' THEN ID_SOCIO ELSE NULL END);

-- 3. LOGICA PL/SQL AVANZADA (PROCEDIMIENTOS ALMACENADOS)
-- ==========================================

CREATE OR REPLACE PROCEDURE biblioteca.SP_REGISTRAR_PRESTAMO (
    p_usuario IN VARCHAR2,
    p_codigo_barras IN VARCHAR2,
    p_dias IN NUMBER,
    o_resultado OUT VARCHAR2
) IS
    v_id_socio NUMBER;
    v_id_ejemplar NUMBER;
    v_estado_ejemplar VARCHAR2(20);
    v_max_prestamos NUMBER;
    v_prestamos_actuales NUMBER;
    v_penalizacion DATE;
BEGIN
    -- 1. Obtener ID Socio y validaciones básicas
    BEGIN
        SELECT ID_SOCIO, MAX_PRESTAMOS_ACTIVOS, PENALIZACION_HASTA 
        INTO v_id_socio, v_max_prestamos, v_penalizacion
        FROM biblioteca.SOCIO WHERE USUARIO = p_usuario;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_resultado := 'ERROR: Usuario no encontrado';
            RETURN;
    END;

    -- Validar Penalización
    IF v_penalizacion IS NOT NULL AND v_penalizacion > SYSDATE THEN
        o_resultado := 'ERROR: Usuario penalizado hasta ' || TO_CHAR(v_penalizacion, 'DD/MM/YYYY');
        RETURN;
    END IF;

    -- Validar cupo de préstamos
    SELECT COUNT(*) INTO v_prestamos_actuales FROM biblioteca.PRESTAMO WHERE ID_SOCIO = v_id_socio AND ESTADO = 'ACTIVO';
    IF v_prestamos_actuales >= v_max_prestamos THEN
        o_resultado := 'ERROR: Cupo de prestamos superado';
        RETURN;
    END IF;

    -- 2. Obtener Ejemplar
    BEGIN
        SELECT ID_EJEMPLAR, ESTADO INTO v_id_ejemplar, v_estado_ejemplar 
        FROM biblioteca.EJEMPLAR WHERE CODIGO_BARRAS = p_codigo_barras;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_resultado := 'ERROR: Ejemplar no encontrado';
            RETURN;
    END;

    IF v_estado_ejemplar != 'DISPONIBLE' THEN
        o_resultado := 'ERROR: Ejemplar no disponible (' || v_estado_ejemplar || ')';
        RETURN;
    END IF;

    -- 3. Ejecutar Transacción
    INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PREVISTA_DEVOLUCION, ESTADO)
    VALUES (v_id_socio, v_id_ejemplar, SYSDATE + p_dias, 'ACTIVO');

    UPDATE biblioteca.EJEMPLAR SET ESTADO = 'PRESTADO' WHERE ID_EJEMPLAR = v_id_ejemplar;

    COMMIT;
    o_resultado := 'OK: Prestamo registrado correctamente';

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        o_resultado := 'ERROR SYSTEM: ' || SQLERRM;
END;
/

CREATE OR REPLACE PROCEDURE biblioteca.SP_DEVOLVER_EJEMPLAR (
    p_codigo_barras IN VARCHAR2,
    o_resultado OUT VARCHAR2
) IS
    v_id_prestamo NUMBER;
    v_id_ejemplar NUMBER;
    v_fecha_prevista DATE;
BEGIN
    -- Obtener préstamo activo asociado al ejemplar
    BEGIN
        SELECT p.ID_PRESTAMO, p.ID_EJEMPLAR, p.FECHA_PREVISTA_DEVOLUCION
        INTO v_id_prestamo, v_id_ejemplar, v_fecha_prevista
        FROM biblioteca.PRESTAMO p
        JOIN biblioteca.EJEMPLAR e ON p.ID_EJEMPLAR = e.ID_EJEMPLAR
        WHERE e.CODIGO_BARRAS = p_codigo_barras AND p.ESTADO = 'ACTIVO';
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_resultado := 'ERROR: No hay prestamo activo para este ejemplar';
            RETURN;
    END;

    -- Actualizar préstamo
    UPDATE biblioteca.PRESTAMO 
    SET ESTADO = 'DEVUELTO', FECHA_DEVOLUCION_REAL = SYSDATE 
    WHERE ID_PRESTAMO = v_id_prestamo;

    -- Liberar ejemplar
    UPDATE biblioteca.EJEMPLAR SET ESTADO = 'DISPONIBLE' WHERE ID_EJEMPLAR = v_id_ejemplar;

    COMMIT;
    
    IF SYSDATE > v_fecha_prevista THEN
        o_resultado := 'OK: Devuelto con RETRASO';
    ELSE
        o_resultado := 'OK: Devuelto a tiempo';
    END IF;

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        o_resultado := 'ERROR SYSTEM: ' || SQLERRM;
END;
/

-- 4. TRIGGERS (Red de seguridad adicional)
-- ==========================================
CREATE OR REPLACE TRIGGER biblioteca.TRG_VALIDAR_PRESTAMO_INSERT
BEFORE INSERT ON biblioteca.PRESTAMO
FOR EACH ROW
DECLARE
    v_estado_ejemplar VARCHAR2(20);
BEGIN
    -- Solo validación crítica de estado por si se inserta directo sin usar el SP
    SELECT ESTADO INTO v_estado_ejemplar FROM biblioteca.EJEMPLAR WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR;
    IF v_estado_ejemplar IN ('PRESTADO', 'BAJA') THEN
        RAISE_APPLICATION_ERROR(-20003, 'Ejemplar no disponible (Trigger Guard)');
    END IF;
END;
/

-- 5. SEED DATA
-- ==========================================
-- Contraseña global 'password': $2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW

-- Administradores
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) 
VALUES ('admin', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'ADMIN', 'Administrador', 'admin@bibliotech.com', 10);
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) 
VALUES ('superdev', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'ADMIN', 'Super Developer', 'dev@bibliotech.com', 10);

-- Personal
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) 
VALUES ('biblio', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'BIBLIOTECARIO', 'Bibliotecario Jefe', 'biblio@bibliotech.com', 10);

-- Socios
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) 
VALUES ('socio1', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'SOCIO', 'Juan Socio', 'juan@bibliotech.com', 3);
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) 
VALUES ('ana_p', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'SOCIO', 'Ana Perez', 'ana@bibliotech.com', 3);

-- Libros
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0134685991', 'Effective Java', 'Joshua Bloch', 'Tecnologia', 2018);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0451524935', '1984', 'George Orwell', 'Novela', 1949);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-0547928227', 'The Hobbit', 'J.R.R. Tolkien', 'Fantasia', 1937);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('978-8401015104', 'Dune', 'Frank Herbert', 'Ciencia Ficcion', 1965);

-- Ejemplares
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM biblioteca.LIBRO WHERE ISBN='978-0134685991'), 'EJ-JAVA-1', 'DISPONIBLE', 'Estanteria T1-1');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM biblioteca.LIBRO WHERE ISBN='978-0134685991'), 'EJ-JAVA-2', 'DISPONIBLE', 'Estanteria T1-2');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM biblioteca.LIBRO WHERE ISBN='978-0451524935'), 'EJ-1984-1', 'DISPONIBLE', 'Estanteria N1-1');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM biblioteca.LIBRO WHERE ISBN='978-8401015104'), 'EJ-DUNE-1', 'DISPONIBLE', 'Estanteria C1-1');

-- PRESTAMO DEMOSTRACION (Usando SP si fuera posible, pero aqui insertamos directo para semilla)
INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PRESTAMO, FECHA_PREVISTA_DEVOLUCION, ESTADO)
VALUES (
    (SELECT ID_SOCIO FROM biblioteca.SOCIO WHERE USUARIO='socio1'),
    (SELECT ID_EJEMPLAR FROM biblioteca.EJEMPLAR WHERE CODIGO_BARRAS='EJ-1984-1'),
    SYSDATE - 5, SYSDATE + 10, 'ACTIVO'
);
-- Actualizar estado del ejemplar prestado manualmente en semilla
UPDATE biblioteca.EJEMPLAR SET ESTADO = 'PRESTADO' WHERE CODIGO_BARRAS='EJ-1984-1';

COMMIT;
EXIT;
