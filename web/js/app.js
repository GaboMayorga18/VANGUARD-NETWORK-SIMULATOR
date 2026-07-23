/**
 * app.js — Main application logic for EVEA Dashboard
 * 
 * Handles: API polling, DOM updates, user interactions,
 * auto-simulation mode, and toast notifications.
 */

// ============ STATE ============
const App = {
    autoMode: false,
    autoInterval: null,
    autoSpeed: 3000,
    pollInterval: null,
    lastLogCount: 0,
};

// ============ API HELPERS ============

async function api(endpoint, method = 'GET', body = null) {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    try {
        const res = await fetch('/api/' + endpoint, opts);
        return await res.json();
    } catch (e) {
        console.error('API Error:', endpoint, e);
        return null;
    }
}

// ============ DATA FETCHING ============

async function fetchAllData() {
    const [estado, usuarios, historial, prediccion, reporte, log] = await Promise.all([
        api('estado'),
        api('usuarios'),
        api('historial'),
        api('prediccion'),
        api('reporte'),
        api('log'),
    ]);

    if (estado) updateKPIs(estado);
    if (estado) updateStatusBadge(estado);
    if (estado) updateTruthTable(estado);
    if (estado) updateIntervalCounter(estado);
    if (usuarios) updateUsersTable(usuarios);
    if (historial) window.EVEACharts.updateUsage(historial);
    if (historial) window.EVEACharts.updateQueue(historial);
    if (usuarios) window.EVEACharts.updateState(usuarios);
    if (historial && prediccion) window.EVEACharts.updatePrediction(historial, prediccion);
    if (prediccion) window.AIPredictor.renderPanel(prediccion, document.getElementById('aiPanelBody'));
    if (log) updateEventLog(log);
    if (estado && reporte) updateMathReport(estado, reporte);
    if (usuarios) {
        const countBadge = document.getElementById('usersCount');
        if (countBadge) countBadge.textContent = usuarios.length;
    }
}

// ============ DOM UPDATES ============

function updateKPIs(est) {
    // Connections
    setKPI('kpi-conn', est.connAct, `${est.connAct} / ${est.maxConn}`, est.pConn);
    toggleCritical('kpi-conn', est.pConn >= 80);

    // Queue
    setKPI('kpi-cola', est.tamCola, `${est.tamCola} / ${est.maxCola}`, (est.tamCola / est.maxCola) * 100);
    toggleCritical('kpi-cola', est.tamCola >= est.maxCola);

    // Bandwidth
    setKPI('kpi-banda', `${Math.round(est.bandaUsd)}`, `${est.bandaUsd} / ${est.maxBanda} Mbps`, est.pBanda);
    toggleCritical('kpi-banda', est.pBanda >= 80);

    // Storage
    setKPI('kpi-alm', `${est.almGB}`, `${est.almGB} / ${est.maxAlGB} GB`, est.pAlm);

    // Served
    setKPI('kpi-atend', est.atendidos, `de ${est.totalUsr} totales`, (est.totalUsr > 0 ? (est.atendidos / est.totalUsr) * 100 : 0));

    // Rejected
    setKPI('kpi-rech', est.rechazados, `${est.alertas} alertas`, (est.totalUsr > 0 ? (est.rechazados / est.totalUsr) * 100 : 0));
    toggleCritical('kpi-rech', est.rechazados > 2);
}

function setKPI(id, value, sub, barPercent) {
    const card = document.getElementById(id);
    if (!card) return;
    const valEl = card.querySelector('.kpi-value');
    const subEl = card.querySelector('.kpi-sub');
    const barEl = card.querySelector('.kpi-bar-fill');
    if (valEl) valEl.textContent = value;
    if (subEl) subEl.textContent = sub;
    if (barEl) barEl.style.width = Math.min(100, Math.max(0, barPercent)) + '%';
}

function toggleCritical(id, isCrit) {
    const el = document.getElementById(id);
    if (!el) return;
    if (isCrit) el.classList.add('critical');
    else el.classList.remove('critical');
}

function updateStatusBadge(est) {
    const badge = document.getElementById('statusBadge');
    if (!badge) return;
    if (est.saturado) {
        badge.className = 'status-badge saturado';
        badge.innerHTML = '<span class="status-dot"></span> SATURADO';
    } else {
        badge.className = 'status-badge normal';
        badge.innerHTML = '<span class="status-dot"></span> OPERATIVO';
    }
}

function updateIntervalCounter(est) {
    const el = document.getElementById('intervalCounter');
    if (el) el.textContent = `Intervalo #${est.numInt}`;
}

function updateUsersTable(usuarios) {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    tbody.innerHTML = usuarios.map(u => {
        const estadoClass = u.estado.toLowerCase();
        const stateIcon = { 'Finalizado': '✓', 'Subiendo': '↑', 'Esperando': '⏳', 'Rechazado': '✕' }[u.estado] || '?';
        return `
            <tr>
                <td class="mono">${u.id}</td>
                <td>${u.nombre}</td>
                <td class="mono">${u.tamano} MB</td>
                <td><span class="state-badge ${estadoClass}">${stateIcon} ${u.estado}</span></td>
                <td class="mono">${u.tEsp}s</td>
                <td class="mono">${u.tCarga}s</td>
            </tr>
        `;
    }).join('');
}

function updateTruthTable(est) {
    const rows = document.querySelectorAll('.truth-table tbody tr');
    if (!rows || rows.length < 4) return;

    // Determine which row is active based on P and Q
    const p = est.p;
    const q = est.q;
    let activeIdx = 0;
    if (!p && !q) activeIdx = 0;
    else if (!p && q) activeIdx = 1;
    else if (p && !q) activeIdx = 2;
    else activeIdx = 3;

    rows.forEach((row, i) => {
        if (i === activeIdx) row.classList.add('active-row');
        else row.classList.remove('active-row');
    });

    // Update current state
    const stateEl = document.getElementById('truthCurrentState');
    if (stateEl) {
        let decision, decClass;
        if (!p) { decision = 'ADMITIR'; decClass = 'decision-admitir'; }
        else if (p && !q) { decision = 'EN COLA'; decClass = 'decision-cola'; }
        else { decision = 'RECHAZAR'; decClass = 'decision-rechazar'; }
        stateEl.innerHTML = `Estado: P=<span class="${p ? 'val-true' : 'val-false'}">${p}</span> Q=<span class="${q ? 'val-true' : 'val-false'}">${q}</span> → <span class="${decClass}">${decision}</span>`;
    }
}

function updateMathReport(est, rep) {
    const el = document.getElementById('mathReport');
    if (!el) return;
    const pR = rep.pRechazo || 0;
    const promEspMin = rep.promEspera ? (rep.promEspera / 60).toFixed(4) : '0';
    const bandaGbps = (est.bandaUsd / 1000).toFixed(3);
    const critClass = est.pConn >= 80 ? 'color: var(--red); font-weight: 700;' : 'color: var(--green);';
    el.innerHTML = `
        <div>1) Utilización: (${est.connAct}/${est.maxConn}) ×100 = <span style="${critClass}">${est.pConn}%</span></div>
        <div style="padding-left:1rem; font-size:0.65rem; color:var(--text-dim);">→ ${est.pConn >= 80 ? 'NIVEL CRÍTICO (≥80%)' : 'NIVEL NORMAL (<80%)'}</div>
        <div>2) Prom. espera: ${est.sEspera.toFixed(1)}/${rep.atendidos || est.atendidos} = <span style="color:var(--cyan)">${rep.promEspera}s</span> = ${promEspMin} min</div>
        <div>3) % Rechazados: (${est.rechazados}/${est.totalUsr}) ×100 = <span style="color:${pR > 15 ? 'var(--red)' : 'var(--green)'}">${pR}%</span></div>
        <div>4) Uso banda: (${est.bandaUsd}/${est.maxBanda}) ×100 = <span style="color:var(--purple)">${est.pBanda}%</span></div>
        <div>5) Conversiones: ${est.bandaUsd} Mbps = ${bandaGbps} Gbps</div>
    `;
}

function updateEventLog(logEntries) {
    const container = document.getElementById('eventLog');
    if (!container) return;

    // Only update if changed
    if (logEntries.length === App.lastLogCount) return;
    App.lastLogCount = logEntries.length;

    container.innerHTML = logEntries.map(entry => {
        let cls = 'log-entry';
        if (entry.includes('ALERTA') || entry.includes('SATURADO') || entry.includes('RECHAZADO')) cls += ' alert';
        else if (entry.includes('>>') || entry.includes('ADMITIDO') || entry.includes('EN COLA')) cls += ' action';
        else if (entry.includes('finalizó') || entry.includes('salió')) cls += ' success';
        return `<div class="${cls}">${escapeHtml(entry)}</div>`;
    }).join('');

    // Auto-scroll to bottom
    container.scrollTop = container.scrollHeight;
}

// ============ ACTIONS ============

async function simularPaso() {
    const result = await api('simular', 'POST');
    if (result && result.ok) {
        if (result.saturado) {
            showToast('⚠️ ¡Servidor SATURADO!', 'warning');
        } else {
            showToast(`Intervalo #${result.intervalo} simulado`, 'info');
        }
        await fetchAllData();
    }
}

async function registrarUsuario() {
    const nombre = document.getElementById('regNombre').value.trim();
    const tamano = parseFloat(document.getElementById('regTamano').value);

    if (!nombre) { showToast('Ingrese un nombre', 'error'); return; }
    if (isNaN(tamano) || tamano <= 0 || tamano > 100) {
        showToast('Tamaño inválido (0.1 a 100 MB)', 'error');
        return;
    }

    const result = await api('registrar', 'POST', { nombre, tamano });
    if (result) {
        if (result.ok) {
            showToast(`${nombre}: ${result.decision} (ID: ${result.id})`, result.decision === 'RECHAZADO' ? 'error' : 'success');
            closeModal();
            await fetchAllData();
        } else {
            showToast(result.msg, 'error');
        }
    }
}

async function resetSimulacion() {
    if (!confirm('¿Reiniciar la simulación? Se perderán todos los datos actuales.')) return;
    const result = await api('reset', 'POST');
    if (result && result.ok) {
        showToast('Simulación reiniciada', 'info');
        App.lastLogCount = 0;
        await fetchAllData();
    }
}

function toggleAutoMode() {
    App.autoMode = !App.autoMode;
    const btn = document.getElementById('btnAuto');

    if (App.autoMode) {
        btn.classList.add('active');
        btn.innerHTML = '⏸ Detener Auto';
        App.autoInterval = setInterval(() => simularPaso(), App.autoSpeed);
        showToast('Auto-simulación activada', 'info');
    } else {
        btn.classList.remove('active');
        btn.innerHTML = '▶ Auto-Simular';
        if (App.autoInterval) {
            clearInterval(App.autoInterval);
            App.autoInterval = null;
        }
        showToast('Auto-simulación detenida', 'info');
    }
}

function changeAutoSpeed() {
    const sel = document.getElementById('autoSpeed');
    App.autoSpeed = parseInt(sel.value);
    if (App.autoMode) {
        clearInterval(App.autoInterval);
        App.autoInterval = setInterval(() => simularPaso(), App.autoSpeed);
    }
}

// ============ MODAL ============

function openModal() {
    document.getElementById('registerModal').classList.add('active');
    document.getElementById('regNombre').value = '';
    document.getElementById('regTamano').value = '';
    setTimeout(() => document.getElementById('regNombre').focus(), 200);
}

function closeModal() {
    document.getElementById('registerModal').classList.remove('active');
}

// ============ TOAST NOTIFICATIONS ============

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const icons = { success: '✓', error: '✕', warning: '⚠', info: 'ℹ' };
    toast.innerHTML = `<span>${icons[type] || 'ℹ'}</span><span>${message}</span>`;

    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('removing');
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// ============ CLOCK ============

function updateClock() {
    const el = document.getElementById('clock');
    if (el) {
        const now = new Date();
        el.textContent = now.toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }
}

// ============ UTILITIES ============

function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

// ============ KEYBOARD SHORTCUTS ============

document.addEventListener('keydown', (e) => {
    // Escape closes modal
    if (e.key === 'Escape') closeModal();
    // Space simulates step (when modal not open)
    if (e.key === ' ' && !document.getElementById('registerModal').classList.contains('active')) {
        e.preventDefault();
        simularPaso();
    }
});

// ============ INITIALIZATION ============

document.addEventListener('DOMContentLoaded', () => {
    // Init charts
    window.EVEACharts.init();

    // Initial data fetch
    fetchAllData();

    // Start polling every 2s
    App.pollInterval = setInterval(fetchAllData, 2000);

    // Start clock
    updateClock();
    setInterval(updateClock, 1000);

    // Wire up buttons
    document.getElementById('btnSimular').addEventListener('click', simularPaso);
    document.getElementById('btnRegistrar').addEventListener('click', openModal);
    document.getElementById('btnReset').addEventListener('click', resetSimulacion);
    document.getElementById('btnAuto').addEventListener('click', toggleAutoMode);
    document.getElementById('autoSpeed').addEventListener('change', changeAutoSpeed);
    document.getElementById('btnRegSubmit').addEventListener('click', registrarUsuario);
    document.getElementById('btnRegCancel').addEventListener('click', closeModal);

    // Modal overlay close
    document.getElementById('registerModal').addEventListener('click', (e) => {
        if (e.target === e.currentTarget) closeModal();
    });

    // Enter key in modal submits
    document.getElementById('regTamano').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') registrarUsuario();
    });

    console.log('%c EVEA Dashboard Ready ', 'background: #06b6d4; color: #0a0f1e; font-weight: bold; padding: 4px 8px; border-radius: 4px;');
});
