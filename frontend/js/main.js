/**
 * BiblioTech Pro - Módulo Principal
 * 
 * Punto de entrada de la aplicación. Coordina la inicialización de todos los módulos,
 * gestiona la navegación entre vistas y maneja eventos globales de la aplicación.
 * 
 * @module main
 * @author Xavier Aerox
 * @version 2.1.0
 */

import { initAuth, handleLogin, handleLogout, currentUser, populateUserDropdown } from './auth.js';
import { loadCatalog, resetCatalogState } from './catalog.js';
import { loadMyBlocks, loadMyLoansData, loadRecommendations } from './user.js';
import { loadLibrarianView } from './librarian.js';
import { initTiltEffect, initThemeToggle } from './effects.js';
import { initSoundEffects } from './sounds.js';

// ============================================
// MANEJO GLOBAL DE ERRORES
// ============================================

/**
 * Captura errores JavaScript no controlados para logging centralizado.
 * Previene que excepciones silenciosas pasen desapercibidas.
 */
window.addEventListener('error', (event) => {
    console.error('[BiblioTech] Error global capturado:', event.error);
});

/**
 * Captura promesas rechazadas sin handler.
 * Útil para detectar problemas en llamadas async/await.
 */
window.addEventListener('unhandledrejection', (event) => {
    console.error('[BiblioTech] Promesa rechazada sin manejar:', event.reason);
});

// ============================================
// INICIALIZACIÓN DE LA APLICACIÓN
// ============================================

/**
 * Punto de entrada principal.
 * Se ejecuta cuando el DOM está completamente cargado.
 */
document.addEventListener('DOMContentLoaded', () => {
    try {
        // 1. Inicializar estado de autenticación (verifica cookies/localStorage)
        const isLoggedIn = initAuth();

        // 2. Inicializar efectos visuales básicos (Solo tema, sin tilt/sonidos para versión Pro simple)
        initThemeToggle();   // Toggle de tema claro/oscuro
        // initTiltEffect();    // Efecto 3D deshabilitado por simplicidad
        // initSoundEffects();  // Sonidos deshabilitados por simplicidad

        // 3. Mostrar vista apropiada según estado de autenticación
        if (isLoggedIn) {
            showDashboard();
        } else {
            showLogin();
        }

        // 4. Configurar todos los event listeners de la UI
        setupEventListeners();

        // 5. Cargar lista de usuarios disponibles para login
        populateUserDropdown();
    } catch (error) {
        console.error('[BiblioTech] Error durante inicialización:', error);
    }
});

// ============================================
// EVENTOS GLOBALES ENTRE MÓDULOS
// ============================================

/**
 * Escucha eventos de sesión expirada/no autorizada.
 * Dispara logout automático cuando el backend rechaza el token.
 */
window.addEventListener('auth:unauthorized', async () => {
    await handleLogout();
    showLogin();
});

/**
 * Evento para refrescar el catálogo desde otros módulos.
 * Evita dependencias circulares entre módulos.
 */
window.addEventListener('catalog:refresh', () => loadCatalog());

/**
 * Evento para actualizar datos del usuario (reservas, préstamos).
 * Usado después de acciones que modifican el estado del usuario.
 */
window.addEventListener('user:refresh-data', () => {
    if (currentUser && currentUser.rol === 'SOCIO') {
        loadMyBlocks();
        loadMyLoansData();
    }
});

// ============================================
// CONFIGURACIÓN DE EVENT LISTENERS
// ============================================

/**
 * Configura todos los listeners de la interfaz de usuario.
 * Centraliza la lógica de interacción para facilitar mantenimiento.
 */
function setupEventListeners() {
    // --- Formulario de Login ---
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => handleLogin(e, showDashboard));
    }

    // --- Botón de Logout ---
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            await handleLogout();
            showLogin();
        });
    }

    // --- Click en Logo -> Ir a Catálogo ---
    document.querySelectorAll('.sidebar-brand').forEach(brand => {
        brand.style.cursor = 'pointer';
        brand.addEventListener('click', () => {
            navigateTo('catalog-section');
            updateActiveNav('catalog-section');
        });
    });

    // --- Navegación del Sidebar ---
    document.querySelectorAll('.nav-item').forEach(btn => {
        if (btn.dataset.target) {
            btn.addEventListener('click', (e) => {
                const target = e.currentTarget.dataset.target;
                navigateTo(target);
                updateActiveNav(null, btn);
            });
        }
    });

    // --- Botón de Recomendaciones IA ---
    const recsBtn = document.getElementById('get-recs-btn');
    if (recsBtn) recsBtn.addEventListener('click', loadRecommendations);

    // --- Botón Aplicar Filtros ---
    const applyFiltersBtn = document.getElementById('apply-filters-btn');
    if (applyFiltersBtn) {
        applyFiltersBtn.addEventListener('click', () => {
            resetCatalogState();
            loadCatalog();
        });
    }

    // --- Búsqueda con Debounce ---
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        // Búsqueda al presionar Enter
        searchInput.addEventListener('keyup', (e) => {
            if (e.key === 'Enter') {
                resetCatalogState();
                loadCatalog();
            }
        });

        // Búsqueda automática con delay de 500ms (debounce)
        let searchTimeout;
        searchInput.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                resetCatalogState();
                loadCatalog();
            }, 500);
        });
    }
}

// ============================================
// GESTIÓN DE NAVEGACIÓN
// ============================================

/**
 * Actualiza el estado visual del ítem de navegación activo.
 * 
 * @private
 * @param {string|null} targetId - ID de la sección destino, o null si se pasa btnElement
 * @param {HTMLElement|null} btnElement - Elemento del botón a marcar como activo
 */
function updateActiveNav(targetId, btnElement) {
    document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
    if (btnElement) {
        btnElement.classList.add('active');
    } else if (targetId) {
        const btn = document.querySelector(`.nav-item[data-target="${targetId}"]`);
        if (btn) btn.classList.add('active');
    }
}

// ============================================
// GESTIÓN DE VISTAS
// ============================================

/**
 * Muestra la vista de login y oculta el dashboard.
 * @private
 */
function showLogin() {
    const loginView = document.getElementById('login-view');
    const dbView = document.getElementById('dashboard-view');
    if (loginView) loginView.classList.remove('hidden');
    if (dbView) dbView.classList.add('hidden');
}

function showDashboard() {
    const loginView = document.getElementById('login-view');
    const dbView = document.getElementById('dashboard-view');
    if (loginView) loginView.classList.add('hidden');
    if (dbView) dbView.classList.remove('hidden');

    if (currentUser) {
        // Update user display
        const ud = document.getElementById('user-display');
        const ur = document.getElementById('user-role');
        const ua = document.getElementById('user-avatar');
        if (ud) ud.textContent = currentUser.username;
        if (ur) ur.textContent = getRoleName(currentUser.rol);
        if (ua) ua.textContent = currentUser.username.charAt(0).toUpperCase();

        // Show/Hide nav items based on role
        const adminSection = document.getElementById('admin-section');
        const libNav = document.getElementById('nav-librarian');
        const loansNav = document.getElementById('nav-loans');
        const recsNav = document.getElementById('nav-recs');

        if (currentUser.rol === 'BIBLIOTECARIO' || currentUser.rol === 'ADMIN') {
            if (adminSection) adminSection.style.display = 'block';
            if (libNav) libNav.style.display = 'flex';
            if (loansNav) loansNav.style.display = 'none';
            if (recsNav) recsNav.style.display = 'none';
        } else {
            if (adminSection) adminSection.style.display = 'none';
            if (libNav) libNav.style.display = 'none';
            if (loansNav) loansNav.style.display = 'flex';
            if (recsNav) recsNav.style.display = 'flex';
        }

        // Update page subtitle with user greeting
        const subtitle = document.getElementById('page-subtitle');
        if (subtitle) {
            const hour = new Date().getHours();
            let greeting = 'Bienvenido';
            if (hour < 12) greeting = 'Buenos días';
            else if (hour < 19) greeting = 'Buenas tardes';
            else greeting = 'Buenas noches';
            subtitle.textContent = `${greeting}, ${currentUser.username}`;
        }

        // Initial Data
        loadCatalog();
        if (currentUser.rol === 'SOCIO') {
            loadMyBlocks();
            loadMyLoansData();
        }
    }
}

// Helper function to get role display name
function getRoleName(rol) {
    const roles = {
        'SOCIO': 'Socio',
        'BIBLIOTECARIO': 'Bibliotecario',
        'ADMIN': 'Administrador'
    };
    return roles[rol] || rol;
}

function navigateTo(sectionId) {
    document.querySelectorAll('.section').forEach(s => s.classList.add('hidden'));
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        targetSection.classList.remove('hidden');
        targetSection.classList.remove('fade-in');
        void targetSection.offsetWidth; // trigger reflow
        targetSection.classList.add('fade-in');
    }

    const titles = {
        'catalog-section': 'Explorar Catálogo',
        'my-loans-section': 'Mis Reservas y Lecturas',
        'recommendations-section': 'Recomendaciones IA',
        'librarian-section': 'Panel de Administración'
    };
    const titleEl = document.getElementById('page-title');
    if (titleEl) titleEl.textContent = titles[sectionId] || 'Biblioteca';

    if (sectionId === 'catalog-section') loadCatalog();
    if (sectionId === 'my-loans-section') {
        loadMyBlocks();
        loadMyLoansData();
    }
    if (sectionId === 'librarian-section') loadLibrarianView();
}
