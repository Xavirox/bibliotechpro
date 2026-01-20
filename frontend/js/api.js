/**
 * BiblioTech Pro - Cliente API con Autenticación
 * 
 * Proporciona una función wrapper para todas las peticiones HTTP al backend.
 * Gestiona automáticamente las cookies HttpOnly para autenticación JWT segura.
 * 
 * @module api
 * @author Xavier Aerox
 * @version 2.1.0
 */

import { API_URL } from './config.js';

// ============================================
// CLIENTE API AUTENTICADO
// ============================================

/**
 * Realiza una petición HTTP autenticada al backend.
 * 
 * **Características de seguridad:**
 * - Envía automáticamente cookies HttpOnly (JWT)
 * - NO almacena tokens en localStorage (previene XSS)
 * - Dispara evento global en caso de sesión expirada (401/403)
 * 
 * @async
 * @function fetchWithAuth
 * @param {string} endpoint - Ruta del endpoint (sin la URL base). Ej: `/libros`
 * @param {Object} [options={}] - Opciones de fetch (method, body, headers, etc.)
 * @returns {Promise<Response>} Respuesta del fetch
 * @throws {Error} Si la sesión ha expirado o hay error de red
 * 
 * @example
 * // GET request
 * const response = await fetchWithAuth('/libros/paginated?page=0&size=10');
 * const data = await response.json();
 * 
 * @example
 * // POST request
 * const response = await fetchWithAuth('/bloqueos', {
 *     method: 'POST',
 *     body: JSON.stringify({ idEjemplar: 123 })
 * });
 * 
 * @fires window#auth:unauthorized - Cuando el servidor responde 401 o 403
 */
export async function fetchWithAuth(endpoint, options = {}) {
    const headers = options.headers || {};

    // Añadir Content-Type JSON por defecto (excepto para FormData)
    if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    const config = {
        ...options,
        headers,
        // SEGURIDAD: Include credentials envía las cookies HttpOnly automáticamente
        credentials: 'include'
    };

    try {
        const response = await fetch(`${API_URL}${endpoint}`, config);

        // Detectar sesión expirada o acceso denegado
        if (response.status === 401 || response.status === 403) {
            // Disparar evento para que main.js maneje el logout
            window.dispatchEvent(new CustomEvent('auth:unauthorized'));
            throw new Error('Sesión expirada o no autorizada');
        }

        return response;
    } catch (error) {
        // Re-lanzar el error para que el caller lo maneje
        throw error;
    }
}
