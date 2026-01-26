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
 * - Maneja errores 401/403 redirigiendo (vía evento) al login
 * - Proporciona feedback visual (Toasts) en caso de error
 * 
 * @async
 * @function fetchWithAuth
 * @param {string} endpoint - Ruta del endpoint (sin la URL base). Ej: `/libros`
 * @param {Object} [options={}] - Opciones de fetch (method, body, headers, etc.)
 * @returns {Promise<any>} Datos de la respuesta parseados (si es JSON) o el objeto Response
 */
export async function fetchWithAuth(endpoint, options = {}) {
    const headers = options.headers || {};

    // Opcional: Si el sistema usara tokens en localStorage en lugar de solo cookies
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    if (user.token && !headers['Authorization']) {
        headers['Authorization'] = `Bearer ${user.token}`;
    }

    // Importamos dinámicamente para evitar dependencias circulares complejas si fuera necesario,
    // pero como utils.js no importa api.js, podemos importarlo normalmente arriba.
    // Usaremos import dinámico aquí para asegurar que siempre tenemos la última referencia si se recarga.
    const { showToast } = await import('./utils.js');

    if (!headers['Content-Type'] && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }

    const config = {
        ...options,
        headers,
        credentials: 'include'
    };

    try {
        const response = await fetch(`${API_URL}${endpoint}`, config);

        // 1. Manejo de Sesión Expirada (401/403)
        if (response.status === 401 || response.status === 403) {
            window.dispatchEvent(new CustomEvent('auth:unauthorized'));
            showToast('Tu sesión ha expirado. Por favor, vuelve a entrar.', 'warning');
            throw new Error('Sesión no autorizada');
        }

        // 2. Manejo de errores de negocio (4xx, 5xx)
        if (!response.ok) {
            let errorMessage = 'Error en la petición al servidor';
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorData.errors || errorMessage;
            } catch (e) {
                // Si no es JSON, usamos el statusText
                errorMessage = `Error ${response.status}: ${response.statusText}`;
            }

            showToast(errorMessage, 'error');
            throw new Error(errorMessage);
        }

        // 3. Retornar datos parseados si es posible
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }

        return response;
    } catch (error) {
        if (error.message !== 'Sesión no autorizada' && !error.message.includes('Error ')) {
            showToast('Error de conexión con el servidor', 'error');
        }
        throw error;
    }
}
