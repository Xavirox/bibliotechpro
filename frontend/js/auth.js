/**
 * BiblioTech Pro - Módulo de Autenticación
 * 
 * Gestiona todo el ciclo de vida de la sesión del usuario:
 * - Inicio de sesión con JWT (cookies HttpOnly)
 * - Cierre de sesión seguro
 * - Persistencia del estado del usuario
 * - Carga de usuarios disponibles para el login
 * 
 * **Seguridad implementada (C-02):**
 * - El token JWT se almacena en cookie HttpOnly (no accesible por JavaScript)
 * - Solo los datos del usuario (no sensibles) se guardan en localStorage
 * - Logout invalida la cookie en el servidor
 * 
 * @module auth
 * @author Xavier Aerox
 * @version 2.1.0
 */

import { API_URL } from './config.js';
import { showToast } from './utils.js';
import { fetchWithAuth } from './api.js';

// ============================================
// ESTADO GLOBAL DE AUTENTICACIÓN
// ============================================

/**
 * Usuario actualmente autenticado.
 * Exportado para acceso desde otros módulos.
 * 
 * @type {{id: number, username: string, rol: string}|null}
 */
export let currentUser = null;

/**
 * Inicializa el estado de autenticación al cargar.
 * Solo recupera datos del usuario, no el token (está en cookie HttpOnly).
 */
export function initAuth() {
    const userStr = localStorage.getItem('user');

    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            // Verificar que el usuario tenga los campos requeridos
            if (user && user.username && user.rol) {
                currentUser = user;
                return true; // User data exists, but session validity depends on cookie
            }
        } catch (e) {
            console.warn('Error parsing user data from localStorage:', e);
        }
    }

    // Si llegamos aquí, limpiar cualquier dato corrupto
    clearAuthData();
    return false; // Not logged in
}

/**
 * Limpia los datos de autenticación del localStorage.
 * NOTA: El token está en una HttpOnly cookie, no podemos borrarlo desde JS.
 * El logout debe llamar al backend para invalidar la cookie.
 */
function clearAuthData() {
    // SEGURIDAD C-02: Ya no almacenamos token en localStorage
    localStorage.removeItem('user');
    currentUser = null;
}

/**
 * Maneja el inicio de sesión.
 */
export async function handleLogin(e, onSuccess) {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const errorDiv = document.getElementById('login-error');

    if (!username) {
        showToast('Por favor selecciona un usuario', 'warning');
        return;
    }

    try {
        errorDiv.textContent = '';
        // Usamos fetch normal aquí porque es el login y maneja su propio error visual en el errorDiv
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
            credentials: 'include'
        });

        if (response.ok) {
            const data = await response.json();
            currentUser = {
                id: data.id,
                username: data.username,
                rol: data.rol
            };
            localStorage.setItem('user', JSON.stringify(currentUser));

            showToast(`Bienvenido, ${currentUser.username}`, 'success');
            if (onSuccess) onSuccess();
        } else {
            let errorMsg = 'Error de inicio de sesión';
            try {
                const errData = await response.json();
                errorMsg = errData.message || errData.errors || errorMsg;
            } catch (e) { }

            errorDiv.innerHTML = `<i class="fa-solid fa-circle-exclamation"></i> ${errorMsg}`;
            showToast(errorMsg, 'error');
        }
    } catch (err) {
        errorDiv.textContent = 'Error de conexión con el servidor.';
    }
}

/**
 * Maneja el cierre de sesión.
 * SEGURIDAD C-02: Ahora llama al backend para invalidar la cookie HttpOnly.
 */
export async function handleLogout() {
    try {
        // Usamos fetchWithAuth para asegurar que la sesión se invalida correctamente
        await fetchWithAuth(`/auth/logout`, { method: 'POST' });
    } catch (err) {
        console.warn('Logout request failed:', err);
    }

    clearAuthData();
    showToast('Sesión cerrada correctamente', 'info');
    // Reload validation or UI update handled by caller
}

/**
 * Carga la lista de usuarios para el login.
 */
export async function populateUserDropdown() {
    const select = document.getElementById('username');
    if (!select) return;

    select.innerHTML = '<option>Conectando con el servidor...</option>';

    try {
        // Endpoint público para cargar usuarios
        const response = await fetch(`${API_URL}/socios/public`);
        if (!response.ok) throw new Error('Servidor no disponible');

        const users = await response.json();

        select.innerHTML = '<option value="" disabled selected>Selecciona un usuario</option>';
        users.forEach(u => {
            const option = document.createElement('option');
            option.value = u.username;
            option.textContent = u.nombre;
            select.appendChild(option);
        });

    } catch (e) {
        console.error('Error loading users:', e);
        select.innerHTML = '<option value="">Error - Click para reintentar</option>';
        select.addEventListener('click', () => populateUserDropdown(), { once: true });

        const errorDiv = document.getElementById('login-error');
        if (errorDiv) {
            errorDiv.innerHTML = `
                <i class="fa-solid fa-network-wired"></i> Error de conexión.<br>
                <small>Backend: ${API_URL}</small>
            `;
        }
    }
}
