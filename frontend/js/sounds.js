/**
 * BiblioTech Pro - Módulo de Efectos Sonoros y Visuales
 * 
 * Proporciona feedback audiovisual premium para mejorar la experiencia del usuario.
 * Los sonidos se generan mediante Web Audio API (sin archivos externos).
 * Funciona incluso si el audio no está disponible (fallback a efectos visuales).
 * 
 * @module sounds
 * @author Xavier Aerox
 * @version 2.1.0
 */

// ============================================
// ESTADO DEL MÓDULO
// ============================================

/** 
 * Contexto de audio para generación de sonidos.
 * Se inicializa lazy en la primera interacción del usuario.
 * @type {AudioContext|null}
 */
let audioContext = null;

// ============================================
// INICIALIZACIÓN
// ============================================

/**
 * Inicializa el AudioContext.
 * 
 * Debe llamarse en respuesta a una interacción del usuario (click, tap)
 * debido a las políticas de autoplay de los navegadores modernos.
 * 
 * @function initAudio
 * @returns {void}
 */
export function initAudio() {
    if (audioContext) return;

    try {
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
    } catch (e) {
        console.log('[Sounds] Audio no disponible, usando solo feedback visual');
    }
}

// ============================================
// FUNCIONES DE SONIDO
// ============================================

/**
 * Reproduce un sonido de éxito (acorde mayor ascendente).
 * Usado para confirmar acciones completadas correctamente.
 * 
 * @function playSuccessSound
 * @returns {void}
 * 
 * @example
 * // Después de guardar exitosamente
 * playSuccessSound();
 */
export function playSuccessSound() {
    playTone([440, 554, 659], 0.08); // A4, C#5, E5 (acorde mayor)
    showVisualFeedback('success');
}

/**
 * Reproduce un sonido de error (tono descendente).
 * Usado para indicar que algo salió mal.
 * 
 * @function playErrorSound
 * @returns {void}
 */
export function playErrorSound() {
    playTone([440, 349], 0.12); // A4, F4 (intervalo descendente)
    showVisualFeedback('error');
}

/**
 * Reproduce un sonido de click corto.
 * Proporciona feedback táctil para botones e interacciones.
 * 
 * @function playClickSound
 * @returns {void}
 */
export function playClickSound() {
    playTone([800], 0.03);
    showVisualFeedback('click');
}

// ============================================
// FUNCIONES INTERNAS
// ============================================

/**
 * Genera y reproduce tonos usando Web Audio API.
 * 
 * @private
 * @param {number[]} frequencies - Array de frecuencias en Hz a reproducir
 * @param {number} duration - Duración de cada tono en segundos
 * @returns {void}
 */
function playTone(frequencies, duration) {
    if (!audioContext) return;

    try {
        frequencies.forEach((freq, index) => {
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();

            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);

            oscillator.frequency.value = freq;
            oscillator.type = 'sine'; // Onda sinusoidal suave

            // Envelope ADSR simplificado para sonido limpio
            const now = audioContext.currentTime + (index * duration);
            gainNode.gain.setValueAtTime(0, now);
            gainNode.gain.linearRampToValueAtTime(0.1, now + 0.01);      // Attack
            gainNode.gain.exponentialRampToValueAtTime(0.01, now + duration); // Decay

            oscillator.start(now);
            oscillator.stop(now + duration);
        });
    } catch (e) {
        // Silenciar errores de audio - no son críticos
    }
}

/**
 * Muestra un efecto visual de feedback en el centro de la pantalla.
 * 
 * @private
 * @param {'success'|'error'|'click'} type - Tipo de feedback para determinar el color
 * @returns {void}
 */
function showVisualFeedback(type) {
    const colors = {
        success: '#10B981',  // Verde esmeralda
        error: '#EF4444',    // Rojo
        click: '#6366F1'     // Indigo (primary)
    };

    const feedback = document.createElement('div');
    feedback.className = `visual-feedback visual-feedback-${type}`;
    feedback.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: ${colors[type] || colors.click};
        opacity: 0.3;
        pointer-events: none;
        z-index: 99999;
        animation: visual-pulse 0.4s ease-out forwards;
    `;

    document.body.appendChild(feedback);
    setTimeout(() => feedback.remove(), 500);
}

// ============================================
// EFECTOS DE INTERACCIÓN
// ============================================

/**
 * Añade efecto ripple (Material Design) a un elemento.
 * 
 * @function addRippleEffect
 * @param {HTMLElement} element - Elemento al que añadir el efecto
 * @returns {void}
 * 
 * @example
 * const button = document.querySelector('.my-button');
 * addRippleEffect(button);
 */
export function addRippleEffect(element) {
    element.addEventListener('click', function (e) {
        const rect = this.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        const ripple = document.createElement('span');
        ripple.className = 'ripple-effect';
        ripple.style.cssText = `
            position: absolute;
            width: 100px;
            height: 100px;
            background: rgba(255, 255, 255, 0.4);
            border-radius: 50%;
            transform: translate(-50%, -50%) scale(0);
            animation: ripple-expand 0.6s ease-out;
            pointer-events: none;
            left: ${x}px;
            top: ${y}px;
        `;

        this.style.position = 'relative';
        this.style.overflow = 'hidden';
        this.appendChild(ripple);

        playClickSound();

        setTimeout(() => ripple.remove(), 600);
    });
}

// ============================================
// INICIALIZACIÓN GLOBAL
// ============================================

/**
 * Inicializa todos los efectos sonoros y visuales de la aplicación.
 * 
 * - Crea el AudioContext en el primer click
 * - Inyecta los estilos CSS de animación necesarios
 * - Añade efecto ripple a todos los botones .btn
 * 
 * @function initSoundEffects
 * @returns {void}
 * 
 * @example
 * // Llamar una vez al cargar la app
 * document.addEventListener('DOMContentLoaded', () => {
 *     initSoundEffects();
 * });
 */
export function initSoundEffects() {
    // Inicializar audio en primer click (requerido por navegadores)
    document.addEventListener('click', () => initAudio(), { once: true });

    // CSS de animaciones movido a visuals.css para limpieza y separación de intereses.

    // Añadir ripple a todos los botones
    document.querySelectorAll('.btn').forEach(btn => {
        if (!btn.dataset.rippleInit) {
            addRippleEffect(btn);
            btn.dataset.rippleInit = 'true';
        }
    });
}
