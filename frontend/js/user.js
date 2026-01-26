/**
 * BiblioTech Pro - Módulo de Usuario (Socio)
 * 
 * Gestiona las funcionalidades exclusivas para usuarios con rol SOCIO:
 * - Visualización de reservas activas (bloqueos)
 * - Gestión de préstamos activos y historial
 * - Widget de lectura actual en el sidebar
 * - Generación de recomendaciones con IA (Gemini)
 * 
 * @module user
 * @author Xavier Aerox
 * @version 2.1.0
 */

import { fetchWithAuth } from './api.js';
import { getGradient, showToast } from './utils.js';
import { LOAN_STATUS } from './constants.js';

// ============================================
// GESTIÓN DE RESERVAS (BLOQUEOS)
// ============================================

/**
 * Carga y renderiza las reservas activas del usuario actual.
 * Muestra tarjetas con información del ejemplar reservado y opción de cancelar.
 * 
 * @async
 * @function loadMyBlocks
 * @returns {Promise<void>}
 */
export async function loadMyBlocks() {
    const blocksContainer = document.getElementById('my-blocks-list');
    if (!blocksContainer) return;

    blocksContainer.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Cargando...';

    try {
        const bloqueos = await fetchWithAuth(`/bloqueos/mios`);

        blocksContainer.innerHTML = '';
        if (bloqueos.length === 0) {
            blocksContainer.innerHTML = `
                <div style="text-align:center; padding:1.5rem; color:var(--text-muted); border: 2px dashed var(--border); border-radius:1rem;">
                    <i class="fa-solid fa-calendar-plus" style="font-size:2rem; margin-bottom:0.5rem; opacity:0.3;"></i>
                    <p>No tienes reservas activas.</p>
                    <button class="btn btn-sm btn-outline" onclick="document.querySelector('[data-target=\'catalog-section\']').click()" style="margin-top:0.5rem; font-size:0.75rem;">Ir al catálogo</button>
                </div>
            `;
            return;
        }

        bloqueos.forEach(b => {
            const card = document.createElement('div');
            card.className = 'book-card';
            const gradient = getGradient('Reserva');

            card.innerHTML = `
                <div class="book-cover-placeholder" style="background: ${gradient}; height: 100px; font-size: 2rem;">
                    <i class="fa-solid fa-clock"></i>
                </div>
                <div class="book-info">
                    <div class="book-title">Reserva activa</div>
                    <p style="font-size:0.9rem;"><strong>Ejemplar:</strong> ${b.ejemplar.codigoBarras}</p>
                    <p style="font-size:0.9rem; margin-bottom:1rem;"><strong>Vence:</strong> ${new Date(b.fechaFin).toLocaleTimeString()}</p>
                    <button class="btn btn-danger btn-cancel-block" style="width:100%">Cancelar reserva</button>
                </div>
            `;
            containerBlockCancel(card, b.idBloqueo);
            blocksContainer.appendChild(card);
        });
    } catch (err) {
        blocksContainer.innerHTML = 'Error al cargar reservas';
    }
}

function containerBlockCancel(card, idBloqueo) {
    const btn = card.querySelector('.btn-cancel-block');
    if (btn) btn.addEventListener('click', () => cancelarBloqueo(idBloqueo));
}

async function cancelarBloqueo(idBloqueo) {
    if (!confirm('¿Cancelar reserva?')) return;

    try {
        await fetchWithAuth(`/bloqueos/${idBloqueo}/cancelar`, {
            method: 'POST'
        });

        showToast('Reserva cancelada.', 'info');
        loadMyBlocks();
        window.dispatchEvent(new CustomEvent('catalog:refresh'));
    } catch (err) {
        // El error ya fue notificado por fetchWithAuth
    }
}

export async function loadMyLoansData() {
    const historyContainer = document.getElementById('my-history-list');
    const activeContainer = document.getElementById('my-active-loans-list');
    if (!historyContainer || !activeContainer) return;

    try {
        const loans = await fetchWithAuth(`/prestamos/mis-prestamos`);

        updateActiveReadingWidget(loans);

        const activeLoans = loans.filter(l => l.estado && l.estado.toUpperCase() === LOAN_STATUS.ACTIVO);
        const historyLoans = loans.filter(l => !l.estado || l.estado.toUpperCase() !== LOAN_STATUS.ACTIVO);

        // --- Render Active Loans ---
        activeContainer.innerHTML = '';
        if (activeLoans.length === 0) {
            activeContainer.innerHTML = `
                <div style="text-align:center; padding:1.5rem; color:var(--text-muted); border: 2px dashed var(--border); border-radius:1rem; grid-column:1/-1;">
                    <i class="fa-solid fa-mug-hot" style="font-size:2rem; margin-bottom:0.5rem; opacity:0.3;"></i>
                    <p>No tienes libros prestados en este momento.</p>
                    <p style="font-size:0.8rem;">¡Es un buen momento para empezar una nueva lectura!</p>
                </div>
            `;
        } else {
            activeLoans.forEach(l => {
                const card = document.createElement('div');
                card.className = 'book-card';
                card.style.borderLeft = '4px solid var(--primary)';

                card.innerHTML = `
                     <div class="book-info">
                        <div class="book-title">${l.tituloLibro}</div>
                        <p style="font-size:0.9rem; color:var(--text-muted); margin-bottom:0.5rem;">
                            <i class="fa-solid fa-calendar-check"></i> Iniciado: ${new Date(l.fechaPrestamo).toLocaleDateString()}
                        </p>
                         <p style="font-weight:600; color: ${l.estaVencido ? 'var(--danger)' : 'var(--success)'}">
                            ${l.estaVencido ? `<i class="fa-solid fa-circle-exclamation"></i> Vencido hace ${Math.abs(l.diasRestantes)} días` : `<i class="fa-solid fa-clock"></i> Vence en ${l.diasRestantes} días`}
                        </p>
                    </div>
                `;
                activeContainer.appendChild(card);
            });
        }

        // --- Render History Table ---
        historyContainer.innerHTML = '';
        if (historyLoans.length === 0) {
            historyContainer.innerHTML = '<tr><td colspan="4" style="padding:1.5rem; text-align:center; color:var(--text-muted);">Sin historial de lectura.</td></tr>';
        } else {
            historyLoans.forEach(l => {
                const tr = document.createElement('tr');
                tr.style.borderBottom = '1px solid var(--border)';
                tr.innerHTML = `
                    <td style="padding:1rem; font-weight:500;">${l.tituloLibro}</td>
                    <td style="padding:1rem;">${new Date(l.fechaPrestamo).toLocaleDateString()}</td>
                    <td style="padding:1rem;">${l.fechaDevolucionReal ? new Date(l.fechaDevolucionReal).toLocaleDateString() : '-'}</td>
                    <td style="padding:1rem; text-align:center;">
                        <span class="badge ${l.badgeClass}">${l.estado}</span>
                    </td>
                `;
                historyContainer.appendChild(tr);
            });
        }

    } catch (err) {
        console.error('Error loading loans:', err);
    }
}

// ============================================
// WIDGET DE LECTURA ACTIVA
// ============================================

/**
 * Actualiza el widget del sidebar que muestra el libro actualmente en préstamo.
 * Incluye barra de progreso visual basada en el tiempo transcurrido.
 * 
 * @private
 * @param {Array} loans - Lista de préstamos del usuario
 */
function updateActiveReadingWidget(loans) {
    const widget = document.getElementById('active-reading-widget');
    if (!widget) return;

    try {
        const activeLoan = loans.find(l => l.estado && l.estado.toUpperCase() === LOAN_STATUS.ACTIVO);

        if (!activeLoan) {
            widget.classList.add('hidden');
            return;
        }

        widget.classList.remove('hidden');

        let progress = 0;

        // Calcular progreso basado en tiempo de préstamo (15 días máximo)
        const start = new Date(activeLoan.fechaPrestamo).getTime();
        const now = new Date().getTime();
        const totalDuration = 15 * 24 * 60 * 60 * 1000; // 15 días en ms
        progress = Math.min(100, Math.max(0, ((now - start) / totalDuration) * 100));
        if (isNaN(progress)) progress = 0;

        widget.innerHTML = `
            <div style="padding: 1.25rem; background: rgba(255,255,255,0.05); border-radius: 12px; margin: 1rem 0; border: 1px solid rgba(255,255,255,0.1);">
                <h4 style="font-size: 0.75rem; text-transform: uppercase; color: #94A3B8; margin-bottom: 0.75rem; letter-spacing: 0.05em; font-weight: 700;">
                    <i class="fa-solid fa-book-open" style="margin-right: 0.5rem;"></i> Leyendo ahora
                </h4>
                <div style="font-weight: 600; font-size: 1rem; margin-bottom: 0.5rem; color: white;">
                    ${activeLoan.tituloLibro || 'Libro Desconocido'}
                </div>
                
                <!-- Progress Bar -->
                <div style="width: 100%; height: 6px; background: rgba(255,255,255,0.2); border-radius: 3px; overflow: hidden; margin-top: 0.75rem;">
                    <div style="width: ${progress}%; height: 100%; background: var(--accent); border-radius: 3px;"></div>
                </div>
                <div style="text-align: right; font-size: 0.75rem; color: #94A3B8; margin-top: 0.5rem;">
                    ${Math.floor(progress)}% completado
                </div>
            </div>
        `;
    } catch (err) {
        console.error('Error updating active reading widget:', err);
        widget.classList.add('hidden');
    }
}

// --- Recommendations ---
export async function loadRecommendations() {
    const container = document.getElementById('recommendations-content');
    const loading = document.getElementById('gemini-loading');
    const btn = document.getElementById('get-recs-btn');

    if (!container) return;

    container.innerHTML = '';
    loading.classList.remove('hidden');
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Generando...';

    try {
        const recommendations = await fetchWithAuth(`/recomendaciones/mias`);
        loading.classList.add('hidden');

        if (Array.isArray(recommendations) && recommendations.length > 0) {
            recommendations.forEach((rec, index) => {
                const card = document.createElement('div');
                card.className = 'book-card fade-in';
                card.style.animationDelay = `${index * 100}ms`;
                const gradient = getGradient(rec.titulo);

                card.innerHTML = `
                    <div class="book-cover-placeholder" style="background: ${gradient}; height:140px; font-size:2rem;">
                         <i class="fa-solid fa-lightbulb"></i>
                    </div>
                    <div class="book-info">
                        <div class="book-title">${rec.titulo}</div>
                        <p style="color:var(--text-muted); font-size:0.9rem; line-height:1.6;">${rec.razon}</p>
                    </div>
                `;
                container.appendChild(card);
            });
        } else {
            container.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:2rem; color:var(--text-muted)">No hay recomendaciones disponibles en este momento.</div>';
        }

    } catch (err) {
        loading.classList.add('hidden');
        showToast('Error al obtener recomendaciones', 'error');
        container.innerHTML = '<div style="grid-column:1/-1; text-align:center; padding:2rem; color:var(--danger)">Error al conectar con el servicio de IA.</div>';
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-wand-magic-sparkles"></i> Generar Nuevas';
    }
}
