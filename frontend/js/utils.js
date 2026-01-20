/**
 * BiblioTech Pro - Módulo de Utilidades Compartidas
 * 
 * Proporciona funciones utilitarias usadas en toda la aplicación:
 * - Sistema de notificaciones toast premium
 * - Generación de gradientes y iconos para portadas
 * - Formateo de fechas
 * - Utilidades de seguridad (escape XSS)
 * 
 * @module utils
 * @author Xavier Aerox
 * @version 2.1.0
 */

// ============================================
// SISTEMA DE NOTIFICACIONES TOAST
// ============================================

/**
 * Muestra una notificación toast premium con animación y barra de progreso.
 * @param {string} message - Mensaje a mostrar
 * @param {string} type - Tipo: 'info', 'success', 'error', 'warning'
 * @param {number} duration - Duración en ms (default: 4000)
 */
export function showToast(message, type = 'info', duration = 4000) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    // Configuración visual según tipo
    const config = {
        success: {
            icon: 'fa-circle-check',
            title: '¡Éxito!',
            color: 'var(--success)'
        },
        error: {
            icon: 'fa-circle-xmark',
            title: 'Error',
            color: 'var(--danger)'
        },
        warning: {
            icon: 'fa-triangle-exclamation',
            title: 'Atención',
            color: 'var(--warning)'
        },
        info: {
            icon: 'fa-circle-info',
            title: 'Información',
            color: 'var(--info, #3B82F6)'
        }
    };

    const { icon, title, color } = config[type] || config.info;

    toast.innerHTML = `
        <div class="toast-icon" style="color: ${color}">
            <i class="fa-solid ${icon}"></i>
        </div>
        <div class="toast-content">
            <div class="toast-title" style="font-weight: 600; font-size: 0.9rem; color: var(--text-primary, #1F2937);">${title}</div>
            <div class="toast-message" style="font-size: 0.85rem; color: var(--text-secondary, #6B7280);">${message}</div>
        </div>
        <button class="toast-close" aria-label="Cerrar notificación">
            <i class="fa-solid fa-xmark"></i>
        </button>
        <div class="toast-progress" style="position: absolute; bottom: 0; left: 0; height: 3px; background: ${color}; width: 100%; border-radius: 0 0 8px 8px;"></div>
    `;

    // Estilos del toast
    toast.style.cssText = `
        position: relative;
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 16px 16px 18px;
        background: var(--bg-card, white);
        border-radius: 12px;
        box-shadow: 0 10px 40px rgba(0,0,0,0.15), 0 2px 8px rgba(0,0,0,0.08);
        min-width: 320px;
        max-width: 420px;
        border-left: 4px solid ${color};
        opacity: 0;
        transform: translateX(100%) scale(0.8);
        transition: all 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
        overflow: hidden;
    `;

    container.appendChild(toast);

    // Botón cerrar
    const closeBtn = toast.querySelector('.toast-close');
    if (closeBtn) {
        closeBtn.style.cssText = `
            position: absolute;
            top: 8px;
            right: 8px;
            background: none;
            border: none;
            color: var(--text-muted, #9CA3AF);
            cursor: pointer;
            padding: 4px;
            line-height: 1;
            font-size: 0.9rem;
            border-radius: 4px;
            transition: all 0.2s;
        `;
        closeBtn.addEventListener('click', () => dismissToast(toast));
    }

    // Animación de entrada
    requestAnimationFrame(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateX(0) scale(1)';
    });

    // Barra de progreso
    const progressBar = toast.querySelector('.toast-progress');
    if (progressBar) {
        progressBar.style.transition = `width ${duration}ms linear`;
        requestAnimationFrame(() => {
            progressBar.style.width = '0%';
        });
    }

    // Auto-dismiss
    const timeoutId = setTimeout(() => dismissToast(toast), duration);
    toast.dataset.timeoutId = timeoutId;

    // Pausar al hover
    toast.addEventListener('mouseenter', () => {
        clearTimeout(parseInt(toast.dataset.timeoutId));
        if (progressBar) progressBar.style.animationPlayState = 'paused';
    });

    toast.addEventListener('mouseleave', () => {
        const newTimeoutId = setTimeout(() => dismissToast(toast), 2000);
        toast.dataset.timeoutId = newTimeoutId;
    });
}

function dismissToast(toast) {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%) scale(0.8)';
    setTimeout(() => toast.remove(), 400);
}


/**
 * Genera un gradiente determinista basado en una cadena de texto.
 * Produce colores más vibrantes y modernos.
 */
export function getGradient(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }

    // Paleta de gradientes premium predefinidos
    const premiumGradients = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
        'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
        'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #6a11cb 0%, #2575fc 100%)',
        'linear-gradient(135deg, #ff0844 0%, #ffb199 100%)',
        'linear-gradient(135deg, #00c6fb 0%, #005bea 100%)',
        'linear-gradient(135deg, #7f7fd5 30%, #86a8e7 50%, #91eae4 100%)',
        'linear-gradient(135deg, #654ea3 0%, #eaafc8 100%)',
        'linear-gradient(135deg, #1a2980 0%, #26d0ce 100%)'
    ];

    const index = Math.abs(hash) % premiumGradients.length;
    return premiumGradients[index];
}

/**
 * Retorna el icono FA según la categoría del libro.
 * Iconos más grandes y estilizados para las portadas.
 */
export function getIcon(category) {
    const iconMap = {
        'Novela': 'fa-book-open',
        'Ciencia Ficción': 'fa-rocket',
        'Tecnología': 'fa-microchip',
        'Historia': 'fa-landmark',
        'Biografía': 'fa-user-pen',
        'Fantasía': 'fa-dragon',
        'Terror': 'fa-ghost',
        'Romance': 'fa-heart',
        'Aventura': 'fa-mountain',
        'Misterio': 'fa-magnifying-glass',
        'Poesía': 'fa-feather',
        'Drama': 'fa-masks-theater',
        'Comedia': 'fa-face-laugh-beam',
        'Infantil': 'fa-child'
    };

    const iconClass = iconMap[category] || 'fa-book';

    return `<i class="fa-solid ${iconClass}" style="font-size: 4rem; opacity: 0.9; text-shadow: 0 4px 20px rgba(0,0,0,0.3);"></i>`;
}

/**
 * Formatea una fecha en formato legible español
 */
export function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('es-ES', {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
    });
}



/**
 * SEGURIDAD: Escapa caracteres HTML para prevenir XSS
 * Usar siempre que se inserte texto del servidor en innerHTML
 * @param {string} str - Texto a escapar
 * @returns {string} - Texto sanitizado
 */
export function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}
