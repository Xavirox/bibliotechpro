/**
 * BiblioTech Pro - Constantes de la Aplicación
 * 
 * Define valores constantes usados en toda la aplicación para mantener
 * consistencia y evitar "magic strings" en el código.
 * 
 * @module constants
 * @author Xavier Aerox
 * @version 2.1.0
 */

/**
 * Estados posibles de un libro/ejemplar.
 * Sincronizado con el ENUM de la base de datos Oracle.
 * 
 * @constant {Object}
 * @property {string} DISPONIBLE - Libro disponible para préstamo/reserva
 * @property {string} PRESTADO - Libro actualmente prestado a un socio
 * @property {string} BLOQUEADO - Libro reservado (24h) por un socio
 * @property {string} BAJA - Libro dado de baja del sistema
 */
export const BOOK_STATUS = {
    DISPONIBLE: "DISPONIBLE",
    PRESTADO: "PRESTADO",
    BLOQUEADO: "BLOQUEADO",
    BAJA: "BAJA"
};

/**
 * Estados posibles de un préstamo.
 * Sincronizado con el ENUM de la base de datos Oracle.
 * 
 * @constant {Object}
 * @property {string} ACTIVO - Préstamo en curso (libro con el socio)
 * @property {string} DEVUELTO - Préstamo finalizado (libro devuelto)
 */
export const LOAN_STATUS = {
    ACTIVO: "ACTIVO",
    DEVUELTO: "DEVUELTO"
};

/**
 * Roles de usuario del sistema.
 * Determina los permisos y vistas disponibles.
 * 
 * @constant {Object}
 * @property {string} SOCIO - Usuario regular que puede reservar y tener préstamos
 * @property {string} BIBLIOTECARIO - Puede gestionar préstamos y ver estadísticas
 * @property {string} ADMIN - Acceso completo al sistema
 */
export const USER_ROLES = {
    SOCIO: "SOCIO",
    BIBLIOTECARIO: "BIBLIOTECARIO",
    ADMIN: "ADMIN"
};
