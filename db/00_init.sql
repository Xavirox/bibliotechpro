-- ==========================================
-- SCRIPT MAESTRO DE INICIALIZACIÓN Y DATOS (GOLD MASTER v3 - RÚBRICA 100%)
-- BiblioTech Pro - VPS Edition 
-- ==========================================
-- INCLUYE:
-- 1. Estructura Limpia (Tablas, Secuencias)
-- 2. Lógica PL/SQL Avanzada (Procedures de Negocio)
-- 3. Triggers Obligatorios (Préstamo) y Recomendados (Bloqueo)
-- 4. Job de Limpieza Automática (DBMS_SCHEDULER)
-- 5. Datos Semilla (Seed Data)
-- ==========================================

ALTER SESSION SET CONTAINER=XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = biblioteca;
GRANT UNLIMITED TABLESPACE TO biblioteca;
GRANT CREATE JOB TO biblioteca;

-- 1. LIMPIEZA TOTAL
-- ==========================================
BEGIN
    FOR r IN (SELECT table_name FROM all_tables WHERE owner = 'BIBLIOTECA') LOOP
        EXECUTE IMMEDIATE 'DROP TABLE BIBLIOTECA.' || r.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
    FOR s IN (SELECT sequence_name FROM all_sequences WHERE sequence_owner = 'BIBLIOTECA') LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE BIBLIOTECA.' || s.sequence_name;
    END LOOP;
    -- Limpiar Job si existe
    BEGIN
        DBMS_SCHEDULER.DROP_JOB('JOB_LIMPIEZA_BLOQUEOS');
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
END;
/

-- 2. ESTRUCTURA (DDL)
-- ==========================================

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
    FECHA_FIN TIMESTAMP NOT NULL, -- 1 día
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
    ID_BLOQUEO NUMBER, -- Opcional, para traza de conversión
    CONSTRAINT FK_PRESTAMO_SOCIO FOREIGN KEY (ID_SOCIO) REFERENCES biblioteca.SOCIO(ID_SOCIO),
    CONSTRAINT FK_PRESTAMO_EJEMPLAR FOREIGN KEY (ID_EJEMPLAR) REFERENCES biblioteca.EJEMPLAR(ID_EJEMPLAR),
    CONSTRAINT FK_PRESTAMO_BLOQUEO FOREIGN KEY (ID_BLOQUEO) REFERENCES biblioteca.BLOQUEO(ID_BLOQUEO)
);

CREATE UNIQUE INDEX biblioteca.IDX_UN_BLOQUEO_ACTIVO ON biblioteca.BLOQUEO (CASE WHEN ESTADO = 'ACTIVO' THEN ID_SOCIO ELSE NULL END);

-- 3. LOGICA PL/SQL (PROCEDURES)
-- ==========================================

-- SP: Limpieza Diaria (Se ejecutará por Job)
CREATE OR REPLACE PROCEDURE biblioteca.SP_LIMPIEZA_DIARIA IS
BEGIN
    -- 1. Marcar como EXPIRADO bloqueos vencidos
    -- 2. Liberar ejemplares asociados
    FOR r IN (
        SELECT b.ID_BLOQUEO, b.ID_EJEMPLAR
        FROM biblioteca.BLOQUEO b
        WHERE b.ESTADO = 'ACTIVO' AND b.FECHA_FIN < SYSDATE
    ) LOOP
        UPDATE biblioteca.BLOQUEO SET ESTADO = 'EXPIRADO' WHERE ID_BLOQUEO = r.ID_BLOQUEO;
        UPDATE biblioteca.EJEMPLAR SET ESTADO = 'DISPONIBLE' WHERE ID_EJEMPLAR = r.ID_EJEMPLAR;
    END LOOP;
    COMMIT;
END;
/

-- SP: Convertir Bloqueo a Prestamo (Helper)
CREATE OR REPLACE PROCEDURE biblioteca.SP_CONVERTIR_BLOQUEO (
    p_id_bloqueo IN NUMBER,
    p_id_socio IN NUMBER
) IS
BEGIN
    UPDATE biblioteca.BLOQUEO 
    SET ESTADO = 'CONVERTIDO' 
    WHERE ID_BLOQUEO = p_id_bloqueo AND ID_SOCIO = p_id_socio AND ESTADO = 'ACTIVO';
END;
/

-- 4. TRIGGERS (REGLAS DE NEGOCIO OBLIGATORIAS)
-- ==========================================

-- TRIGGER BLOQUEO: 1 Activo Máximo y Ejemplar Disponible
CREATE OR REPLACE TRIGGER biblioteca.TRG_VALIDAR_BLOQUEO_INSERT
BEFORE INSERT ON biblioteca.BLOQUEO
FOR EACH ROW
DECLARE
    v_bloqueos_activos NUMBER;
    v_estado_ejemplar VARCHAR2(20);
BEGIN
    -- 1. Un socio no debe tener más de 1 bloqueo ACTIVO
    SELECT COUNT(*) INTO v_bloqueos_activos 
    FROM biblioteca.BLOQUEO 
    WHERE ID_SOCIO = :NEW.ID_SOCIO AND ESTADO = 'ACTIVO';
    
    IF v_bloqueos_activos > 0 THEN
        RAISE_APPLICATION_ERROR(-20101, 'El socio ya tiene un bloqueo activo. Maximo 1 permitido.');
    END IF;

    -- 2. Solo se puede bloquear si está DISPONIBLE
    SELECT ESTADO INTO v_estado_ejemplar FROM biblioteca.EJEMPLAR WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR;
    
    IF v_estado_ejemplar != 'DISPONIBLE' THEN
        RAISE_APPLICATION_ERROR(-20102, 'El ejemplar no esta disponible para bloqueo (Estado: ' || v_estado_ejemplar || ')');
    END IF;
    
    -- Si pasa, actualizamos ejemplar a BLOQUEADO (esto no se puede hacer en BEFORE INSERT directo sobre otra tabla si hay mutación,
    -- pero en Oracle sí en AFTER, o aquí en lógica de aplicación.
    -- IMPORTANTE: Oracle no permite UPDATE de otra tabla en TRIGGER si hay integridad referencial a veces.
    -- Lo ideal es hacer el UPDATE del ejemplar en el Procedure de AppJava/SP, pero el requisito dice Trigger controla TODO.
    -- Para evitar "Mutating Table", el trigger solo valida. El cambio de estado debe ser atómico en la transacción principal
    -- O usar un Trigger AFTER INSERT (que sí permite update mas fácil).
END;
/

CREATE OR REPLACE TRIGGER biblioteca.TRG_ACTUALIZAR_ESTADO_BLOQUEO
AFTER INSERT ON biblioteca.BLOQUEO
FOR EACH ROW
BEGIN
    UPDATE biblioteca.EJEMPLAR SET ESTADO = 'BLOQUEADO' WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR;
END;
/


-- TRIGGER PRESTAMO (EL MAS IMPORTANTE)
CREATE OR REPLACE TRIGGER biblioteca.TRG_VALIDAR_PRESTAMO
BEFORE INSERT ON biblioteca.PRESTAMO
FOR EACH ROW
DECLARE
    v_penalizacion DATE;
    v_num_prestamos NUMBER;
    v_max_prestamos NUMBER;
    v_estado_ejemplar VARCHAR2(20);
    v_libro_id NUMBER;
    v_usuario_bloqueo NUMBER;
    v_id_bloqueo NUMBER;
BEGIN
    -- 1. Socio Penalizado
    SELECT PENALIZACION_HASTA, MAX_PRESTAMOS_ACTIVOS INTO v_penalizacion, v_max_prestamos 
    FROM biblioteca.SOCIO WHERE ID_SOCIO = :NEW.ID_SOCIO;
    
    IF v_penalizacion IS NOT NULL AND v_penalizacion > SYSDATE THEN
        RAISE_APPLICATION_ERROR(-20001, 'Socio penalizado hasta ' || TO_CHAR(v_penalizacion));
    END IF;

    -- 2. Limite Prestamos
    SELECT COUNT(*) INTO v_num_prestamos FROM biblioteca.PRESTAMO WHERE ID_SOCIO = :NEW.ID_SOCIO AND ESTADO = 'ACTIVO';
    IF v_num_prestamos >= v_max_prestamos THEN
        RAISE_APPLICATION_ERROR(-20002, 'Limite de prestamos superado (' || v_max_prestamos || ')');
    END IF;

    -- 3. Estado Ejemplar
    SELECT ESTADO, ID_LIBRO INTO v_estado_ejemplar, v_libro_id 
    FROM biblioteca.EJEMPLAR WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR;

    IF v_estado_ejemplar IN ('PRESTADO', 'BAJA') THEN
        RAISE_APPLICATION_ERROR(-20003, 'Ejemplar no disponible (Estado: ' || v_estado_ejemplar || ')');
    END IF;

    -- 4. Gestion de Bloqueos (Reserva)
    IF v_estado_ejemplar = 'BLOQUEADO' THEN
         -- Verificar si es MI bloqueo
         BEGIN
             SELECT ID_BLOQUEO INTO v_id_bloqueo
             FROM biblioteca.BLOQUEO 
             WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR 
               AND ID_SOCIO = :NEW.ID_SOCIO 
               AND ESTADO = 'ACTIVO';
         EXCEPTION WHEN NO_DATA_FOUND THEN
             RAISE_APPLICATION_ERROR(-20004, 'Ejemplar bloqueado por OTRO usuario');
         END;
         
         -- Si llegamos aqui, es mi bloqueo. Lo asignamos al prestamo para referencia (si ID_BLOQUEO es null en INSERT)
         IF :NEW.ID_BLOQUEO IS NULL THEN
             :NEW.ID_BLOQUEO := v_id_bloqueo;
         END IF;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER biblioteca.TRG_PRESTAMO_POST_INSERT
AFTER INSERT ON biblioteca.PRESTAMO
FOR EACH ROW
BEGIN
    -- 1. Marcar Ejemplar como PRESTADO
    UPDATE biblioteca.EJEMPLAR SET ESTADO = 'PRESTADO' WHERE ID_EJEMPLAR = :NEW.ID_EJEMPLAR;
    
    -- 2. Si viene de bloqueo, marcar Bloqueo como CONVERTIDO
    IF :NEW.ID_BLOQUEO IS NOT NULL THEN
        UPDATE biblioteca.BLOQUEO SET ESTADO = 'CONVERTIDO' WHERE ID_BLOQUEO = :NEW.ID_BLOQUEO;
    END IF;
END;
/


-- 5. JOB AUTOMATICO (DBMS_SCHEDULER)
-- ==========================================
BEGIN
    DBMS_SCHEDULER.CREATE_JOB (
        job_name        => 'BIBLIOTECA.JOB_LIMPIEZA_BLOQUEOS',
        job_type        => 'PLSQL_BLOCK',
        job_action      => 'BEGIN biblioteca.SP_LIMPIEZA_DIARIA; END;',
        start_date      => SYSTIMESTAMP,
        repeat_interval => 'FREQ=DAILY; BYHOUR=23; BYMINUTE=59; BYSECOND=0',
        enabled         => TRUE,
        comments        => 'Job diario para expirar bloqueos vencidos y liberar ejemplares'
    );
END;
/


-- 6. SEED DATA
-- ==========================================
-- Contraseña global 'password'
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES ('admin', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'ADMIN', 'Administrador', 'admin@bibliotech.com', 10);
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES ('biblio', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'BIBLIOTECARIO', 'Bibliotecario Jefe', 'biblio@bibliotech.com', 10);
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES ('socio1', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'SOCIO', 'Juan Socio', 'juan@bibliotech.com', 3);
INSERT INTO biblioteca.SOCIO (USUARIO, PASSWORD_HASH, ROL, NOMBRE, EMAIL, MAX_PRESTAMOS_ACTIVOS) VALUES ('ana_p', '$2a$10$BA0QvPo6W1sgf2kx7g9C/ulfV.CI8lmDJ/HJczwPrEtOPPAwsePdW', 'SOCIO', 'Ana Perez', 'ana@bibliotech.com', 3);

INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('9781', 'Effective Java', 'Joshua Bloch', 'Tecnologia', 2018);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('9782', '1984', 'George Orwell', 'Novela', 1949);
INSERT INTO biblioteca.LIBRO (ISBN, TITULO, AUTOR, CATEGORIA, ANIO) VALUES ('9783', 'The Hobbit', 'Tolkien', 'Fantasia', 1937);

INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM biblioteca.LIBRO WHERE ISBN='9781'), 'EJ-JAVA-1', 'DISPONIBLE', 'Estanteria T1');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM biblioteca.LIBRO WHERE ISBN='9781'), 'EJ-JAVA-2', 'DISPONIBLE', 'Estanteria T1');
INSERT INTO biblioteca.EJEMPLAR (ID_LIBRO, CODIGO_BARRAS, ESTADO, UBICACION) VALUES ((SELECT ID_LIBRO FROM biblioteca.LIBRO WHERE ISBN='9782'), 'EJ-1984-1', 'DISPONIBLE', 'Estanteria N1');

-- PRESTAMO EJEMPLO (Sin trigger mutation issues, usando los triggers que acabamos de definir)
INSERT INTO biblioteca.PRESTAMO (ID_SOCIO, ID_EJEMPLAR, FECHA_PRESTAMO, FECHA_PREVISTA_DEVOLUCION, ESTADO)
VALUES (
    (SELECT ID_SOCIO FROM biblioteca.SOCIO WHERE USUARIO='socio1'),
    (SELECT ID_EJEMPLAR FROM biblioteca.EJEMPLAR WHERE CODIGO_BARRAS='EJ-1984-1'),
    SYSDATE - 5, SYSDATE + 10, 'ACTIVO'
);
-- Nota: El trigger TRG_PRESTAMO_POST_INSERT se encargará de poner el Ejemplar en PRESTADO.

COMMIT;
EXIT;
