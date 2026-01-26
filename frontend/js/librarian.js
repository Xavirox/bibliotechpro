import { fetchWithAuth } from './api.js';
import { showToast } from './utils.js';
import { LOAN_STATUS } from './constants.js';

export async function loadLibrarianView() {
    const section = document.getElementById('librarian-section');
    section.innerHTML = `
        <!-- Stats Dashboard -->
        <div class="stats-dashboard" style="display:grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap:1.5rem; margin-bottom:2rem;">
            <div class="stat-card hover-glow gradient-border" id="stat-books">
                <div class="stat-icon-wrapper" style="width:60px; height:60px; border-radius:16px; background: linear-gradient(135deg, #6366F1 0%, #8B5CF6 100%); display:flex; align-items:center; justify-content:center;">
                    <i class="fa-solid fa-book" style="font-size:1.5rem; color:white;"></i>
                </div>
                <div style="flex:1;">
                    <div class="stat-value count-animate" id="stat-books-value">--</div>
                    <div class="stat-label">Total libros</div>
                </div>
            </div>
            <div class="stat-card hover-glow gradient-border" id="stat-loans">
                <div class="stat-icon-wrapper" style="width:60px; height:60px; border-radius:16px; background: linear-gradient(135deg, #10B981 0%, #059669 100%); display:flex; align-items:center; justify-content:center;">
                    <i class="fa-solid fa-hand-holding-heart" style="font-size:1.5rem; color:white;"></i>
                </div>
                <div style="flex:1;">
                    <div class="stat-value count-animate" id="stat-loans-value">--</div>
                    <div class="stat-label">Préstamos activos</div>
                </div>
            </div>
            <div class="stat-card hover-glow gradient-border" id="stat-reservations">
                <div class="stat-icon-wrapper" style="width:60px; height:60px; border-radius:16px; background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%); display:flex; align-items:center; justify-content:center;">
                    <i class="fa-solid fa-clock" style="font-size:1.5rem; color:white;"></i>
                </div>
                <div style="flex:1;">
                    <div class="stat-value count-animate" id="stat-reservations-value">--</div>
                    <div class="stat-label">Reservas pendientes</div>
                </div>
            </div>
            <div class="stat-card hover-glow gradient-border" id="stat-users">
                <div class="stat-icon-wrapper" style="width:60px; height:60px; border-radius:16px; background: linear-gradient(135deg, #EC4899 0%, #DB2777 100%); display:flex; align-items:center; justify-content:center;">
                    <i class="fa-solid fa-users" style="font-size:1.5rem; color:white;"></i>
                </div>
                <div style="flex:1;">
                    <div class="stat-value count-animate" id="stat-users-value">--</div>
                    <div class="stat-label">Usuarios activos</div>
                </div>
            </div>
        </div>

        <!-- Charts Section -->
        <div class="charts-section" style="display:grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap:1.5rem; margin-bottom:2rem;">
            <div class="panel-card">
                <h3 style="font-weight:700; margin-bottom:1rem; display:flex; align-items:center; gap:0.5rem;">
                    <i class="fa-solid fa-chart-pie" style="color:var(--primary);"></i>
                    Distribución por categorías
                </h3>
                <div id="category-chart" style="height:200px; display:flex; align-items:center; justify-content:center;">
                    <i class="fa-solid fa-circle-notch fa-spin" style="color:var(--primary);"></i>
                </div>
            </div>
            <div class="panel-card">
                <h3 style="font-weight:700; margin-bottom:1rem; display:flex; align-items:center; gap:0.5rem;">
                    <i class="fa-solid fa-chart-line" style="color:var(--success);"></i>
                    Actividad reciente
                </h3>
                <div id="activity-chart" style="height:200px; display:flex; align-items:center; justify-content:center;">
                    <i class="fa-solid fa-circle-notch fa-spin" style="color:var(--primary);"></i>
                </div>
            </div>
        </div>

        <!-- Main Panels -->
        <div style="display:grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap:2rem;">
            <div class="panel-card stagger-list">
                <h3 style="border-bottom:2px solid var(--primary); padding-bottom:0.5rem; margin-bottom:1rem; font-weight:700; display:flex; align-items:center; gap:0.5rem;">
                    <i class="fa-solid fa-clock" style="color:var(--warning);"></i> 
                    Reservas pendientes
                    <span class="badge badge-warning" id="reservation-count" style="margin-left:auto;">0</span>
                </h3>
                <div id="pending-reservations-list" class="card-grid" style="grid-template-columns: 1fr; max-height: 400px; overflow-y: auto;">
                    <i class="fa-solid fa-circle-notch fa-spin"></i> Cargando...
                </div>
            </div>
            <div class="panel-card stagger-list">
                 <h3 style="border-bottom:2px solid var(--success); padding-bottom:0.5rem; margin-bottom:1rem; font-weight:700; display:flex; align-items:center; gap:0.5rem;">
                    <i class="fa-solid fa-book-open" style="color:var(--success);"></i> 
                    Préstamos activos
                    <span class="badge badge-success" id="loan-count" style="margin-left:auto;">0</span>
                </h3>
                 <div id="all-loans-list" class="card-grid" style="grid-template-columns: 1fr; max-height: 400px; overflow-y: auto;">
                    <i class="fa-solid fa-circle-notch fa-spin"></i> Cargando...
                 </div>
            </div>
        </div>
    `;

    // Load all data
    loadStats();
    loadCategoryChart();
    loadActivityChart();
    loadReservationsList();
    loadAllLoansList();
}

async function loadStats() {
    try {

        // Fetch books
        const books = await fetchWithAuth('/libros');
        animateValue('stat-books-value', 0, books.length, 1000);

        // Fetch loans
        const loans = await fetchWithAuth('/prestamos');
        const activeLoans = loans.filter(l => l.estado === LOAN_STATUS.ACTIVO);
        animateValue('stat-loans-value', 0, activeLoans.length, 1000);

        // Fetch reservations
        const reservations = await fetchWithAuth('/bloqueos/activos');
        animateValue('stat-reservations-value', 0, reservations.length, 1000);

        // Fetch users (usando fetch directo ya que es endpoint público)
        const usersRes = await fetch('/api/socios/public');
        const users = await usersRes.json();
        animateValue('stat-users-value', 0, users.length, 1000);

    } catch (e) {
        console.error('Error loading stats:', e);
    }
}

function animateValue(elementId, start, end, duration) {
    const element = document.getElementById(elementId);
    if (!element) return;

    const range = end - start;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);

        // Easing function (ease-out-cubic)
        const easeOut = 1 - Math.pow(1 - progress, 3);
        const current = Math.floor(start + (range * easeOut));

        element.textContent = current;

        if (progress < 1) {
            requestAnimationFrame(update);
        } else {
            element.textContent = end;
        }
    }

    requestAnimationFrame(update);
}

async function loadCategoryChart() {
    const container = document.getElementById('category-chart');
    if (!container) return;

    try {
        const books = await fetchWithAuth('/libros');

        // Count by category
        const categories = {};
        books.forEach(book => {
            categories[book.categoria] = (categories[book.categoria] || 0) + 1;
        });

        // Create visual bar chart
        const total = books.length;
        const colors = [
            '#6366F1', '#8B5CF6', '#EC4899', '#F59E0B', '#10B981',
            '#3B82F6', '#EF4444', '#14B8A6', '#F97316', '#84CC16'
        ];

        let html = '<div style="display:flex; flex-direction:column; gap:0.75rem; width:100%;">';

        Object.entries(categories)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 6)
            .forEach(([cat, count], index) => {
                const percentage = ((count / total) * 100).toFixed(1);
                const color = colors[index % colors.length];

                html += `
                    <div style="display:flex; align-items:center; gap:0.75rem;">
                        <div style="width:100px; font-size:0.8rem; color:var(--text-secondary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${cat}</div>
                        <div style="flex:1; height:24px; background:var(--bg-secondary); border-radius:12px; overflow:hidden; position:relative;">
                            <div style="height:100%; width:${percentage}%; background:${color}; border-radius:12px; transition: width 1s ease-out;" class="progress-bar-fill"></div>
                        </div>
                        <div style="width:50px; font-size:0.8rem; font-weight:600; text-align:right;">${count}</div>
                    </div>
                `;
            });

        html += '</div>';
        container.innerHTML = html;

        // Animate bars
        setTimeout(() => {
            container.querySelectorAll('.progress-bar-fill').forEach(bar => {
                bar.style.width = bar.style.width;
            });
        }, 100);

    } catch (e) {
        console.error('Error loading category chart:', e);
        container.innerHTML = '<div style="color:var(--text-muted);">Error al cargar</div>';
    }
}

async function loadActivityChart() {
    const container = document.getElementById('activity-chart');
    if (!container) return;

    try {
        const loans = await fetchWithAuth('/prestamos');

        // Last 7 days activity
        const days = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
        const activityData = new Array(7).fill(0);
        const today = new Date();

        loans.forEach(loan => {
            const loanDate = new Date(loan.fechaPrestamo);
            const diffDays = Math.floor((today - loanDate) / (1000 * 60 * 60 * 24));
            if (diffDays >= 0 && diffDays < 7) {
                activityData[6 - diffDays]++;
            }
        });

        const maxValue = Math.max(...activityData, 1);

        let html = `
            <div style="display:flex; align-items:flex-end; justify-content:space-between; height:160px; width:100%; gap:8px; padding-bottom:24px; position:relative;">
        `;

        activityData.forEach((value, index) => {
            const height = (value / maxValue) * 120;
            const dayIndex = (today.getDay() - 6 + index + 7) % 7;

            html += `
                <div style="display:flex; flex-direction:column; align-items:center; flex:1;">
                    <div style="font-size:0.7rem; font-weight:600; margin-bottom:4px; color:var(--primary);">${value}</div>
                    <div style="width:100%; max-width:40px; height:${height}px; min-height:4px; background: linear-gradient(180deg, var(--primary) 0%, var(--accent) 100%); border-radius:6px 6px 0 0; transition: height 0.5s ease-out;" class="bar-animated"></div>
                    <div style="font-size:0.65rem; color:var(--text-muted); margin-top:4px;">${days[dayIndex]}</div>
                </div>
            `;
        });

        html += `
            </div>
            <div style="text-align:center; font-size:0.75rem; color:var(--text-muted);">
                📈 Préstamos últimos 7 días
            </div>
        `;

        container.innerHTML = html;

    } catch (e) {
        console.error('Error loading activity chart:', e);
        container.innerHTML = '<div style="color:var(--text-muted);">Error al cargar</div>';
    }
}

async function loadReservationsList() {
    const container = document.getElementById('pending-reservations-list');
    const countBadge = document.getElementById('reservation-count');
    if (!container) return;

    try {
        const reservas = await fetchWithAuth(`/bloqueos/activos`);

        if (countBadge) countBadge.textContent = reservas.length;

        container.innerHTML = '';
        if (reservas.length === 0) {
            container.innerHTML = `
                <div style="text-align:center; padding:2rem; color:var(--text-muted);">
                    <i class="fa-solid fa-check-circle" style="font-size:3rem; margin-bottom:1rem; color:var(--success); opacity:0.5;"></i>
                    <div style="font-weight:600; color:var(--text-primary);">¡Todo al día!</div>
                    <div style="font-size:0.9rem;">No hay reservas esperando ser formalizadas.</div>
                </div>
            `;
            return;
        }

        const fragment = document.createDocumentFragment();

        reservas.forEach((r, index) => {
            const div = document.createElement('div');
            div.className = 'book-card elastic-enter';
            // Optimización: Animación escalonada
            div.style.animationDelay = (index * 50) + 'ms';
            div.style.display = 'flex';
            div.style.alignItems = 'center';
            div.style.justifyContent = 'space-between';

            const expiresAt = new Date(r.fechaFin);
            const now = new Date();
            const hoursLeft = Math.max(0, Math.round((expiresAt - now) / (1000 * 60 * 60)));

            div.innerHTML = `
                <div style="flex:1;">
                   <div style="font-weight:700; font-size:1rem;">${r.ejemplar.libro.titulo}</div>
                   <div style="font-size:0.85rem; color:var(--text-muted); display:flex; gap:1rem; flex-wrap:wrap;">
                        <span><i class="fa-solid fa-user"></i> ${r.socio.usuario}</span>
                        <span><i class="fa-solid fa-barcode"></i> ${r.ejemplar.codigoBarras}</span>
                   </div>
                   <div style="font-size:0.8rem; margin-top:0.25rem; display:flex; align-items:center; gap:0.5rem;">
                        <i class="fa-solid fa-hourglass-half" style="color:${hoursLeft < 6 ? 'var(--danger)' : 'var(--warning)'};"></i>
                        <span style="color:${hoursLeft < 6 ? 'var(--danger)' : 'var(--text-secondary)'};">
                            ${hoursLeft < 1 ? 'Expira pronto' : `${hoursLeft}h restantes`}
                        </span>
                   </div>
                </div>
                <button class="btn btn-primary btn-loan btn-ripple" style="padding:0.6rem 1.2rem; font-size:0.85rem;">
                    <i class="fa-solid fa-check"></i> Prestar
                </button>
             `;
            const btnLoan = div.querySelector('.btn-loan');
            btnLoan.addEventListener('click', (e) => prestarLibro(r.idBloqueo, e.currentTarget));
            fragment.appendChild(div);
        });
        container.appendChild(fragment);

    } catch (e) {
        console.error(e);
        container.innerHTML = '<div style="color:var(--danger)">Error al cargar reservas</div>';
    }
}

async function loadAllLoansList() {
    const container = document.getElementById('all-loans-list');
    const countBadge = document.getElementById('loan-count');
    if (!container) return;

    try {
        const loans = await fetchWithAuth(`/prestamos`);

        container.innerHTML = '';
        const activeLoans = loans.filter(l => l.estado === LOAN_STATUS.ACTIVO);

        if (countBadge) countBadge.textContent = activeLoans.length;

        if (activeLoans.length === 0) {
            container.innerHTML = `
                <div style="text-align:center; padding:2rem; color:var(--text-muted);">
                    <i class="fa-solid fa-box-open" style="font-size:3rem; margin-bottom:1rem; opacity:0.5;"></i>
                    <div style="font-weight:600; color:var(--text-primary);">Sin préstamos activos</div>
                    <div style="font-size:0.9rem;">Los libros devueltos aparecerán aquí.</div>
                </div>
            `;
            return;
        }

        const fragment = document.createDocumentFragment();

        activeLoans.forEach((l, index) => {
            const div = document.createElement('div');
            div.className = 'book-card elastic-enter';
            div.style.animationDelay = (index * 100) + 'ms';
            div.style.display = 'flex';
            div.style.alignItems = 'center';
            div.style.justifyContent = 'space-between';

            div.innerHTML = `
                <div style="flex:1;">
                   <div style="font-weight:700; font-size:1rem;">${l.tituloLibro}</div>
                   <div style="font-size:0.85rem; color:var(--text-muted);">
                        <i class="fa-solid fa-user"></i> ${l.usuario}
                   </div>
                   <div style="font-size:0.8rem; margin-top:0.25rem; display:flex; align-items:center; gap:0.5rem;">
                        <span class="badge ${l.badgeClass}" style="font-size:0.7rem;">
                            ${l.estaVencido ? `⚠️ Vencido (${Math.abs(l.diasRestantes)}d)` : `✓ ${l.diasRestantes} días`}
                        </span>
                   </div>
                </div>
                <button class="btn btn-return btn-ripple" style="padding:0.6rem 1.2rem; font-size:0.85rem; background:var(--bg-secondary); border:1px solid var(--border); color:var(--text-primary);">
                    <i class="fa-solid fa-arrow-rotate-left"></i> Devolver
                </button>
             `;
            const btnReturn = div.querySelector('.btn-return');
            btnReturn.addEventListener('click', (e) => devolverPrestamo(l.idPrestamo, e.currentTarget));
            fragment.appendChild(div);
        });
        container.appendChild(fragment);
    } catch (e) {
        console.error(e);
        container.innerHTML = '<div style="color:var(--danger)">Error al cargar préstamos</div>';
    }
}

async function prestarLibro(idBloqueo, btn) {
    if (!confirm('¿Formalizar préstamo para este usuario?')) return;

    // UI Safety: Deshabilitar y mostrar carga
    const originalContent = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Procesando...';

    try {
        await fetchWithAuth(`/bloqueos/${idBloqueo}/formalizar`, { method: 'POST' });
        showToast('¡Préstamo formalizado! El libro está ahora en manos del socio.', 'success');
        triggerConfetti();
        loadLibrarianView(); // Reload lists
    } catch (e) {
        btn.disabled = false;
        btn.innerHTML = originalContent;
        // El error ya fue notificado por fetchWithAuth
    }
}

async function devolverPrestamo(idPrestamo, btn) {
    if (!confirm('¿Confirmar devolución del libro?')) return;

    // UI Safety: Deshabilitar y mostrar carga
    const originalContent = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i> Verificando...';

    try {
        await fetchWithAuth(`/prestamos/${idPrestamo}/devolver`, { method: 'POST' });
        showToast('Libro devuelto con éxito. Ya está disponible en el catálogo.', 'success');
        loadLibrarianView(); // Reload lists
    } catch (e) {
        btn.disabled = false;
        btn.innerHTML = originalContent;
        // El error ya fue notificado por fetchWithAuth
    }
}

// Celebration effect!
function triggerConfetti() {
    const container = document.createElement('div');
    container.className = 'confetti-container';
    document.body.appendChild(container);

    const colors = ['#6366F1', '#EC4899', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6'];

    for (let i = 0; i < 50; i++) {
        const confetti = document.createElement('div');
        confetti.className = 'confetti';
        confetti.style.cssText = `
            left: ${Math.random() * 100}%;
            background: ${colors[Math.floor(Math.random() * colors.length)]};
            animation-delay: ${Math.random() * 0.5}s;
            animation-duration: ${2 + Math.random() * 2}s;
        `;
        container.appendChild(confetti);
    }

    setTimeout(() => container.remove(), 4000);
}
