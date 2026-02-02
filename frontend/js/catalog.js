/**
 * BiblioTech Pro - Módulo del Catálogo de Libros
 * 
 * Gestiona la visualización y interacción con el catálogo de libros:
 * - Carga paginada de libros con filtros y búsqueda
 * - Visualización de disponibilidad de ejemplares
 * - Sistema de reservas (bloqueos de 24 horas)
 * 
 * **Optimizaciones:**
 * - Paginación server-side (12 libros por página)
 * - Lazy loading de portadas
 * - Cache de disponibilidad en lote
 * 
 * @module catalog
 * @author Xavier Aerox
 * @version 2.1.0
 */

import { fetchWithAuth } from './api.js';
import { currentUser } from './auth.js';
import { getGradient, getIcon, showToast, escapeHtml } from './utils.js';
import { BOOK_STATUS, CATEGORY_EMOJIS } from './constants.js';

// ============================================
// ESTADO DE PAGINACIÓN
// ============================================

/** @type {number} Página actual del catálogo (0-indexed) */
let currentPage = 0;

/** @type {number} Total de páginas disponibles */
let totalPages = 1;

/** @type {boolean} Flag para evitar cargas simultáneas */
let isLoading = false;

/** @type {string} Parámetros de query actuales (para cache) */
let currentQueryParams = '';

// ============================================
// FUNCIONES PÚBLICAS
// ============================================

/**
 * Reinicia el estado de paginación del catálogo.
 * Debe llamarse cuando cambian los filtros o la búsqueda.
 * 
 * @function resetCatalogState
 * @returns {void}
 */
export function resetCatalogState() {
    currentPage = 0;
    totalPages = 1;
    currentQueryParams = '';
    const container = document.getElementById('catalog-list');
    if (container) container.innerHTML = '';
}

export async function loadCatalog(append = false) {
    if (isLoading) return;
    if (!append) resetCatalogState();

    // Safety check for end of list
    if (append && currentPage >= totalPages) return;

    isLoading = true;
    const container = document.getElementById('catalog-list');
    const loadMoreBtnId = 'load-more-btn-container';

    // Remove old load more button if exists
    const oldBtn = document.getElementById(loadMoreBtnId);
    if (oldBtn) oldBtn.remove();

    if (!append) {
        container.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:2rem;"><i class="fa-solid fa-circle-notch fa-spin fa-2x" style="color:var(--primary)"></i></div>';
    } else {
        // Show small spinner at bottom?
    }

    // Build Query
    const categoryElement = document.getElementById('filter-category');
    const category = categoryElement ? categoryElement.value : 'Todas';
    const excludeReadElement = document.getElementById('filter-unread');
    const excludeRead = excludeReadElement ? excludeReadElement.checked : false;
    const searchInput = document.getElementById('search-input');
    const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';

    let queryParams = [`page=${currentPage}`, `size=12`]; // 12 fits nicely in 2, 3, 4 columns

    if (category !== 'Todas') {
        // Note: The backend filter 'Leídos' is now handled via excludeRead=false + onlyRead=true ? 
        // Actually the backend has `onlyRead`. The dropdown logic in original code was complex.
        // Simplified:
        if (category === 'Leídos') queryParams.push('onlyRead=true');
        else queryParams.push(`categoria=${encodeURIComponent(category)}`);
    }

    if (currentUser) {
        queryParams.push(`username=${encodeURIComponent(currentUser.username)}`);
        if (excludeRead) queryParams.push(`excludeRead=true`);
    }

    if (searchTerm) {
        queryParams.push(`search=${encodeURIComponent(searchTerm)}`);
    }

    const endpoint = `/libros/paginated?${queryParams.join('&')}`;

    try {
        const data = await fetchWithAuth(endpoint);
        const libros = data.content;
        totalPages = data.totalPages;

        // Update Counter
        const counterEl = document.getElementById('book-counter');
        if (counterEl) {
            counterEl.innerHTML = `<i class="fa-solid fa-book"></i> Mostrando <strong>${data.totalElements}</strong> libros`;
        }

        if (!append) container.innerHTML = '';

        if (libros.length === 0 && !append) {
            container.innerHTML = '<div style="grid-column:1/-1; text-align:center; color:var(--text-muted); padding: 3rem;">No se encontraron libros con estos filtros.</div>';
            isLoading = false;
            return;
        }

        // Render Items with DocumentFragment (Performance Optimization)
        const fragment = document.createDocumentFragment();

        libros.forEach((libro, index) => {
            const card = createBookCard(libro, index, libro.estaDisponible);
            fragment.appendChild(card);
        });

        container.appendChild(fragment);

        // "Load More" Button
        if (currentPage < totalPages - 1) {
            const btnContainer = document.createElement('div');
            btnContainer.id = loadMoreBtnId;
            btnContainer.className = "load-more-container";

            const btn = document.createElement('button');
            btn.className = "load-more-btn";
            btn.innerHTML = 'Cargar más libros <i class="fa-solid fa-chevron-down"></i>';

            btn.addEventListener('click', () => {
                currentPage++;
                loadCatalog(true);
            });

            btnContainer.appendChild(btn);
            container.appendChild(btnContainer);
        }

    } catch (err) {
        if (!append) container.innerHTML = `<div style="color:var(--danger)">Error al cargar el catálogo: ${err.message}</div>`;
        else showToast('Error al cargar más libros', 'error');
        console.error(err);
    } finally {
        isLoading = false;
    }
}

export async function loadEjemplares(idLibro) {
    const container = document.getElementById(`ejemplares-${idLibro}`);
    if (!container) return false;

    container.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Buscando...';

    try {
        const ejemplares = await fetchWithAuth(`/ejemplares?idLibro=${idLibro}`);
        const available = ejemplares.some(ej => ej.estado === BOOK_STATUS.DISPONIBLE);

        container.innerHTML = '';

        if (ejemplares.length === 0) {
            container.innerHTML = '<span style="color:var(--text-muted)">Agotado temporalmente</span>';
            return false;
        }

        ejemplares.forEach(ej => {
            const div = document.createElement('div');
            div.style.marginBottom = '0.5rem';
            div.style.display = 'flex';
            div.style.justifyContent = 'space-between';
            div.style.alignItems = 'center';

            let badgeClass = 'badge-success';
            let statusText = ej.estado;

            if (ej.estado === BOOK_STATUS.BLOQUEADO) { badgeClass = 'badge-danger'; statusText = 'Reservado'; }
            if (ej.estado === BOOK_STATUS.PRESTADO) { badgeClass = 'badge-warning'; statusText = 'Prestado'; }

            div.innerHTML = `
                <span class="badge ${badgeClass}">${statusText}</span>
            `;

            // Add Reserve button if eligible
            if (ej.estado === BOOK_STATUS.DISPONIBLE && currentUser && currentUser.rol === 'SOCIO') {
                const btn = document.createElement('button');
                btn.className = 'btn-reserve-inline';
                btn.textContent = "Reservar";
                btn.title = "Reservar por 24 horas";
                btn.addEventListener('click', (e) => bloquearEjemplar(ej.idEjemplar, idLibro, e.currentTarget));
                div.appendChild(btn);
            }

            container.appendChild(div);
        });

        return available;
    } catch (err) {
        container.innerHTML = '<span style="color:var(--danger)">No disponible</span>';
        return false;
    }
}

async function bloquearEjemplar(idEjemplar, idLibro, btn) {
    if (!confirm('¿Reservar este ejemplar por 24h?')) return;

    // UI Safety
    const originalContent = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';

    try {
        await fetchWithAuth(`/bloqueos`, {
            method: 'POST',
            body: JSON.stringify({ idEjemplar })
        });

        showToast('¡Reserva confirmada! Pasa por la biblioteca en las próximas 24h.', 'success');

        // Trigger Sidebar update without full page reload
        window.dispatchEvent(new CustomEvent('user:refresh-data'));

        // Refresh specific book availability
        const isAvailable = await loadEjemplares(idLibro);

        // Update the card style if no longer available
        if (!isAvailable) {
            const btn = document.querySelector(`button[data-id="${idLibro}"]`);
            if (btn) {
                const card = btn.closest('.book-card');
                if (card) card.classList.add('unavailable');
            }
        }
    } catch (err) {
        btn.disabled = false;
        btn.innerHTML = originalContent;
        // El error ya fue notificado por fetchWithAuth
    }
}

/**
 * Crea el elemento DOM para una tarjeta de libro.
 * Separa la lógica de presentación de la lógica de negocio.
 * 
 * @param {Object} libro - Datos del libro
 * @param {number} index - Índice para animación escalonada
 * @param {boolean} isAvailable - Disponibilidad calculada
 * @returns {HTMLElement} Nodo DOM de la tarjeta
 */
function createBookCard(libro, index, isAvailable) {
    const card = document.createElement('div');
    card.className = 'book-card fade-in';
    card.style.animationDelay = `${index * 50}ms`;

    const gradient = getGradient(libro.titulo);
    const icon = getIcon(libro.categoria);
    if (!isAvailable) card.classList.add('unavailable');

    // Badge de categoría con emoji
    const categoryEmoji = CATEGORY_EMOJIS[libro.categoria] || '📚';

    // SEGURIDAD: Escapar todos los datos del servidor para prevenir XSS
    const safeTitle = escapeHtml(libro.titulo);
    const safeAuthor = escapeHtml(libro.autor);
    const safeCategory = escapeHtml(libro.categoria);
    const safeIsbn = escapeHtml(libro.isbn);

    card.innerHTML = `
        <div class="book-cover-wrapper" style="position: relative; overflow: hidden; height: 320px;">
            <div class="book-cover-placeholder" style="background: ${gradient}; position: absolute; top:0; left:0; width:100%; height:100%; display:flex; flex-direction:column; align-items:center; justify-content:center; gap: 0.5rem;">
                ${icon}
                <span class="cover-fallback-text" style="font-size: 0.75rem; color: rgba(255,255,255,0.6); text-transform: uppercase; letter-spacing: 0.1em; margin-top: 0.5rem;">Portada no disponible</span>
            </div>
            <img 
                src="https://covers.openlibrary.org/b/isbn/${safeIsbn}-L.jpg?default=false" 
                alt="${safeTitle}"
                class="book-cover-real"
                loading="lazy"
                style="width: 100%; height: 100%; object-fit: cover; position: absolute; top:0; left:0; opacity: 0; transition: opacity 0.3s;"
                onload="this.style.opacity=1; this.previousElementSibling.style.display='none'"
                onerror="this.style.display='none';"
            />
            <div class="book-category-badge" style="position: absolute; top: 12px; left: 12px; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); color: white; padding: 4px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 600;">
                ${categoryEmoji} ${safeCategory}
            </div>
        </div>
        <div class="book-info">
            <div class="book-title" title="${safeTitle}">${safeTitle}</div>
            <div class="book-meta">
                <div><i class="fa-solid fa-user-pen"></i> ${safeAuthor}</div>
                ${libro.anio ? `<div><i class="fa-solid fa-calendar"></i> ${libro.anio}</div>` : ''}
            </div>
            <button data-id="${libro.id}" class="btn btn-primary btn-ver-dispo" style="width:100%; font-size:0.875rem;">
                <i class="fa-solid fa-eye"></i> Ver Disponibilidad
            </button>
            <div id="ejemplares-${libro.id}" style="margin-top:1rem; font-size:0.875rem;"></div>
        </div>
    `;

    const btn = card.querySelector('.btn-ver-dispo');
    if (btn) btn.addEventListener('click', () => loadEjemplares(libro.id));

    return card;
}
