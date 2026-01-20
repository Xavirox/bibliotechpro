/**
 * BiblioTech Pro - Módulo de Efectos Visuales Premium
 * 
 * Proporciona efectos visuales avanzados para una experiencia de usuario premium:
 * - Efecto 3D Tilt en las tarjetas de libros
 * - Toggle de tema claro/oscuro
 * 
 * @module effects
 * @author Xavier Aerox
 * @version 2.1.0
 */

// ============================================
// EFECTO 3D TILT EN TARJETAS
// ============================================

/**
 * Inicializa el efecto 3D tilt en las tarjetas del catálogo.
 * 
 * Las tarjetas se inclinan sutilmente siguiendo el cursor del ratón,
 * creando una sensación de profundidad y interactividad premium.
 * 
 * **Optimizaciones implementadas:**
 * - Event delegation (un solo listener para todo el contenedor)
 * - requestAnimationFrame para throttling (60fps máximo)
 * - Reducción de operaciones DOM
 * 
 * @function initTiltEffect
 * @returns {void}
 * 
 * @example
 * // Llamar después de que el DOM esté listo
 * document.addEventListener('DOMContentLoaded', () => {
 *     initTiltEffect();
 * });
 */
export function initTiltEffect() {
    // Estado del efecto (reutilizado entre frames)
    let ticking = false;    // Flag para throttling con rAF
    let currentCard = null; // Tarjeta actualmente bajo el cursor
    let mouseX = 0;         // Posición X del ratón
    let mouseY = 0;         // Posición Y del ratón

    // Contenedor del catálogo (event delegation)
    const catalogContainer = document.getElementById('catalog-list');
    if (!catalogContainer) return;

    /**
     * Calcula y aplica la transformación 3D.
     * Ejecutado en cada frame de animación cuando hay movimiento.
     * @private
     */
    function updateTilt() {
        if (currentCard) {
            const rect = currentCard.getBoundingClientRect();
            const x = mouseX - rect.left;
            const y = mouseY - rect.top;

            const centerX = rect.width / 2;
            const centerY = rect.height / 2;

            // Calcular rotación (máximo ±5 grados) basada en posición del cursor
            const rotateX = ((y - centerY) / centerY) * -5;
            const rotateY = ((x - centerX) / centerX) * 5;

            // Aplicar transformación con perspectiva y escala
            currentCard.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale(1.02)`;
        }
        ticking = false;
    }

    // Listener de movimiento (delegado al contenedor)
    catalogContainer.addEventListener('mousemove', (e) => {
        const card = e.target.closest('.book-card');
        if (!card) return;

        currentCard = card;
        mouseX = e.clientX;
        mouseY = e.clientY;

        // Throttle con requestAnimationFrame (máximo 60 actualizaciones/segundo)
        if (!ticking) {
            requestAnimationFrame(updateTilt);
            ticking = true;
        }
    });

    // Reset al salir de una tarjeta
    catalogContainer.addEventListener('mouseleave', (e) => {
        const card = e.target.closest('.book-card');
        if (card) {
            card.style.transform = 'perspective(1000px) rotateX(0) rotateY(0) scale(1)';
            currentCard = null;
        }
    }, true); // Capture phase para detectar salida de hijos
}

// ============================================
// TOGGLE DE TEMA (DARK MODE)
// ============================================

/**
 * Inicializa el toggle de tema claro/oscuro.
 * 
 * Gestiona la preferencia del usuario para dark mode, guardándola en localStorage.
 * Busca el botón existente en el header (#dark-mode-toggle).
 * 
 * **Características:**
 * - Persiste la preferencia del usuario entre sesiones
 * - Soporta múltiples keys de localStorage para retrocompatibilidad
 * - Animación suave al cambiar de tema
 * 
 * @function initThemeToggle
 * @returns {void}
 * 
 * @example
 * // Llamar al cargar la aplicación
 * initThemeToggle();
 */
export function initThemeToggle() {
    // Buscar el botón de toggle existente en el HTML
    const btn = document.getElementById('dark-mode-toggle');
    if (!btn) return;

    // Recuperar preferencia guardada (soporta ambas keys para retrocompatibilidad)
    const savedDarkMode = localStorage.getItem('darkMode');
    const savedTheme = localStorage.getItem('theme');

    // Determinar estado inicial: darkMode tiene prioridad por ser la key más usada
    const shouldBeDark = savedDarkMode === 'true' || savedTheme === 'dark';

    if (shouldBeDark) {
        document.body.classList.add('dark-mode');
        btn.innerHTML = '<i class="fa-solid fa-sun"></i>';
        // Migrar a la key unificada
        localStorage.setItem('darkMode', 'true');
    }

    // Handler del click con animación suave
    btn.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        const isDark = document.body.classList.contains('dark-mode');

        // Guardar preferencia
        localStorage.setItem('darkMode', isDark ? 'true' : 'false');

        // Actualizar icono con transición
        btn.innerHTML = isDark
            ? '<i class="fa-solid fa-sun"></i>'
            : '<i class="fa-solid fa-moon"></i>';
    });
}
