/**
 * BiblioTech Pro - Configuración de la API
 * 
 * Centraliza la URL base del backend para facilitar cambios entre entornos.
 * 
 * @module config
 * @author Xavier Aerox
 * @version 2.2.0
 */

/**
 * Determina la URL base de la API dinámicamente.
 * 
 * Lógica:
 * - Si estamos en localhost, asumimos desarrollo local (Puerto 9091)
 * - Si estamos en cualquier otro host (VPS), asumimos puerto 9141 (según docker-compose-vps.yml)
 */
const getApiUrl = () => {
    return '/api';
};

/**
 * URL base de la API REST del backend.
 */
export const API_URL = getApiUrl();

console.log('BiblioTech Pro - API URL:', API_URL);
