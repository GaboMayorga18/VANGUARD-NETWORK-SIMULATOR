/**
 * charts.js — Chart.js real-time charts for EVEA Dashboard
 * 
 * Creates and manages 4 charts:
 * 1. Server Usage Line (historical + prediction)
 * 2. Queue & Rejected Bar chart
 * 3. User State Distribution Doughnut
 * 4. Bandwidth & Storage Gauge-like bars
 */

// Chart.js global defaults for dark mode
Chart.defaults.color = '#94a3b8';
Chart.defaults.borderColor = 'rgba(148, 163, 184, 0.06)';
Chart.defaults.font.family = "'Inter', sans-serif";
Chart.defaults.font.size = 11;
Chart.defaults.plugins.legend.labels.usePointStyle = true;
Chart.defaults.plugins.legend.labels.pointStyleWidth = 8;
Chart.defaults.plugins.legend.labels.padding = 15;
Chart.defaults.animation.duration = 600;
Chart.defaults.animation.easing = 'easeInOutQuart';

// ============ COLOR PALETTE ============
const COLORS = {
    cyan: '#06b6d4',
    cyanFade: 'rgba(6, 182, 212, 0.15)',
    green: '#10b981',
    greenFade: 'rgba(16, 185, 129, 0.15)',
    red: '#ef4444',
    redFade: 'rgba(239, 68, 68, 0.15)',
    amber: '#f59e0b',
    amberFade: 'rgba(245, 158, 11, 0.15)',
    purple: '#a855f7',
    purpleFade: 'rgba(168, 85, 247, 0.15)',
    blue: '#3b82f6',
    blueFade: 'rgba(59, 130, 246, 0.15)',
    gray: '#64748b',
    gridLine: 'rgba(148, 163, 184, 0.06)',
};

// ============ CHART INSTANCES ============
let usageChart = null;
let queueChart = null;
let stateChart = null;
let predictionChart = null;

/**
 * Initialize the Server Usage Line Chart
 */
function initUsageChart() {
    const ctx = document.getElementById('usageChart');
    if (!ctx) return;

    usageChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Uso del Servidor (%)',
                data: [],
                borderColor: COLORS.cyan,
                backgroundColor: createGradient(ctx, COLORS.cyan, COLORS.cyanFade),
                borderWidth: 2.5,
                fill: true,
                tension: 0.4,
                pointRadius: 3,
                pointBackgroundColor: COLORS.cyan,
                pointBorderColor: '#0a0f1e',
                pointBorderWidth: 2,
                pointHoverRadius: 6,
                pointHoverBackgroundColor: COLORS.cyan,
                pointHoverBorderColor: '#fff',
            }, {
                label: 'Conexiones',
                data: [],
                borderColor: COLORS.green,
                backgroundColor: 'transparent',
                borderWidth: 2,
                borderDash: [],
                fill: false,
                tension: 0.4,
                pointRadius: 2,
                pointBackgroundColor: COLORS.green,
                pointBorderColor: '#0a0f1e',
                pointBorderWidth: 1,
                yAxisID: 'y1',
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'top',
                    align: 'end',
                    labels: { boxWidth: 8, boxHeight: 8 }
                },
                tooltip: {
                    backgroundColor: 'rgba(10, 15, 30, 0.95)',
                    titleColor: '#f1f5f9',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(148, 163, 184, 0.1)',
                    borderWidth: 1,
                    padding: 10,
                    cornerRadius: 8,
                    displayColors: true,
                    boxWidth: 8,
                    boxHeight: 8,
                }
            },
            scales: {
                x: {
                    grid: { color: COLORS.gridLine },
                    ticks: { font: { size: 10 } },
                    title: { display: true, text: 'Intervalo', font: { size: 10, weight: '600' } }
                },
                y: {
                    min: 0, max: 100,
                    grid: { color: COLORS.gridLine },
                    ticks: { callback: v => v + '%', font: { size: 10 } },
                    title: { display: true, text: 'Uso (%)', font: { size: 10, weight: '600' } }
                },
                y1: {
                    position: 'right',
                    min: 0,
                    grid: { drawOnChartArea: false },
                    ticks: { font: { size: 10 }, stepSize: 1 },
                    title: { display: true, text: 'Conexiones', font: { size: 10, weight: '600' } }
                }
            }
        }
    });
}

/**
 * Initialize Queue & Rejected Bar Chart
 */
function initQueueChart() {
    const ctx = document.getElementById('queueChart');
    if (!ctx) return;

    queueChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: [],
            datasets: [{
                label: 'Cola',
                data: [],
                backgroundColor: COLORS.amberFade,
                borderColor: COLORS.amber,
                borderWidth: 1.5,
                borderRadius: 4,
                barPercentage: 0.6,
            }, {
                label: 'Rechazados',
                data: [],
                backgroundColor: COLORS.redFade,
                borderColor: COLORS.red,
                borderWidth: 1.5,
                borderRadius: 4,
                barPercentage: 0.6,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'top',
                    align: 'end',
                    labels: { boxWidth: 8, boxHeight: 8 }
                },
                tooltip: {
                    backgroundColor: 'rgba(10, 15, 30, 0.95)',
                    titleColor: '#f1f5f9',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(148, 163, 184, 0.1)',
                    borderWidth: 1,
                    padding: 10,
                    cornerRadius: 8,
                }
            },
            scales: {
                x: {
                    grid: { color: COLORS.gridLine },
                    ticks: { font: { size: 10 } },
                    title: { display: true, text: 'Intervalo', font: { size: 10, weight: '600' } }
                },
                y: {
                    min: 0,
                    grid: { color: COLORS.gridLine },
                    ticks: { font: { size: 10 }, stepSize: 1 },
                }
            }
        }
    });
}

/**
 * Initialize User State Distribution Doughnut
 */
function initStateChart() {
    const ctx = document.getElementById('stateChart');
    if (!ctx) return;

    stateChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Finalizados', 'Subiendo', 'Esperando', 'Rechazados'],
            datasets: [{
                data: [0, 0, 0, 0],
                backgroundColor: [
                    COLORS.greenFade,
                    COLORS.cyanFade,
                    COLORS.amberFade,
                    COLORS.redFade,
                ],
                borderColor: [
                    COLORS.green,
                    COLORS.cyan,
                    COLORS.amber,
                    COLORS.red,
                ],
                borderWidth: 2,
                hoverBorderWidth: 3,
                hoverOffset: 6,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        boxWidth: 8,
                        boxHeight: 8,
                        padding: 12,
                        font: { size: 10 }
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(10, 15, 30, 0.95)',
                    titleColor: '#f1f5f9',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(148, 163, 184, 0.1)',
                    borderWidth: 1,
                    padding: 10,
                    cornerRadius: 8,
                }
            }
        }
    });
}

/**
 * Initialize AI Prediction Chart (Line with dashed future)
 */
function initPredictionChart() {
    const ctx = document.getElementById('predictionChart');
    if (!ctx) return;

    predictionChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Uso Real (%)',
                data: [],
                borderColor: COLORS.cyan,
                backgroundColor: 'transparent',
                borderWidth: 2,
                tension: 0.4,
                pointRadius: 3,
                pointBackgroundColor: COLORS.cyan,
                pointBorderColor: '#0a0f1e',
                pointBorderWidth: 2,
            }, {
                label: 'Predicción IA (%)',
                data: [],
                borderColor: COLORS.purple,
                backgroundColor: createGradient(ctx, COLORS.purple, COLORS.purpleFade),
                borderWidth: 2,
                borderDash: [6, 4],
                fill: true,
                tension: 0.3,
                pointRadius: 3,
                pointBackgroundColor: COLORS.purple,
                pointBorderColor: '#0a0f1e',
                pointBorderWidth: 2,
                pointStyle: 'triangle',
            }, {
                label: 'Umbral Saturación',
                data: [],
                borderColor: 'rgba(239, 68, 68, 0.4)',
                backgroundColor: 'transparent',
                borderWidth: 1.5,
                borderDash: [4, 4],
                fill: false,
                pointRadius: 0,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'top',
                    align: 'end',
                    labels: { boxWidth: 8, boxHeight: 8, font: { size: 10 } }
                },
                tooltip: {
                    backgroundColor: 'rgba(10, 15, 30, 0.95)',
                    titleColor: '#f1f5f9',
                    bodyColor: '#94a3b8',
                    borderColor: 'rgba(148, 163, 184, 0.1)',
                    borderWidth: 1,
                    padding: 10,
                    cornerRadius: 8,
                }
            },
            scales: {
                x: {
                    grid: { color: COLORS.gridLine },
                    ticks: { font: { size: 10 } },
                    title: { display: true, text: 'Intervalo', font: { size: 10, weight: '600' } }
                },
                y: {
                    min: 0, max: 110,
                    grid: { color: COLORS.gridLine },
                    ticks: { callback: v => v + '%', font: { size: 10 } },
                }
            }
        }
    });
}

/**
 * Create a vertical gradient for chart fills
 */
function createGradient(ctx, colorStart, colorEnd) {
    const canvas = ctx.getContext ? ctx : ctx.canvas || ctx;
    const context = canvas.getContext ? canvas.getContext('2d') : null;
    if (!context) return colorEnd;
    const gradient = context.createLinearGradient(0, 0, 0, 250);
    gradient.addColorStop(0, colorStart.replace(')', ', 0.3)').replace('rgb', 'rgba'));
    gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
    return gradient;
}

// ============ UPDATE FUNCTIONS ============

/**
 * Update Usage Chart with history data
 */
function updateUsageChart(historial) {
    if (!usageChart || !historial) return;

    const labels = historial.map(h => '#' + h.intervalo);
    const usoData = historial.map(h => h.pUso);
    const connData = historial.map(h => h.conn);

    usageChart.data.labels = labels;
    usageChart.data.datasets[0].data = usoData;
    usageChart.data.datasets[1].data = connData;

    // Update y1 max
    const maxConn = Math.max(5, ...connData);
    usageChart.options.scales.y1.max = maxConn + 1;

    usageChart.update('none');
}

/**
 * Update Queue Chart with history data
 */
function updateQueueChart(historial) {
    if (!queueChart || !historial) return;

    const labels = historial.map(h => '#' + h.intervalo);
    const colaData = historial.map(h => h.cola);
    const rechData = historial.map(h => h.rechazados);

    queueChart.data.labels = labels;
    queueChart.data.datasets[0].data = colaData;
    queueChart.data.datasets[1].data = rechData;

    queueChart.update('none');
}

/**
 * Update State Doughnut with user data
 */
function updateStateChart(usuarios) {
    if (!stateChart || !usuarios) return;

    let fin = 0, sub = 0, esp = 0, rech = 0;
    usuarios.forEach(u => {
        switch (u.estado) {
            case 'Finalizado': fin++; break;
            case 'Subiendo': sub++; break;
            case 'Esperando': esp++; break;
            case 'Rechazado': rech++; break;
        }
    });

    stateChart.data.datasets[0].data = [fin, sub, esp, rech];
    stateChart.update('none');
}

/**
 * Update Prediction Chart with real + predicted data
 */
function updatePredictionChart(historial, prediccion) {
    if (!predictionChart) return;

    const realLabels = historial.map(h => '#' + h.intervalo);
    const realData = historial.map(h => h.pUso);

    let predLabels = [];
    let predData = [];

    if (prediccion && prediccion.datosPrediccion) {
        predLabels = prediccion.datosPrediccion.map(p => '#' + p.intervalo);
        predData = prediccion.datosPrediccion.map(p => p.pUso);
    }

    // Combined labels
    const allLabels = [...realLabels, ...predLabels];

    // Real data (no values for prediction slots)
    const realSeriesData = [...realData, ...predLabels.map(() => null)];

    // Prediction data: start from last real point
    const predSeriesData = [...realLabels.slice(0, -1).map(() => null)];
    if (realData.length > 0) {
        predSeriesData.push(realData[realData.length - 1]); // connect at last real point
    }
    predSeriesData.push(...predData);

    // Threshold line (80%)
    const thresholdData = allLabels.map(() => 80);

    predictionChart.data.labels = allLabels;
    predictionChart.data.datasets[0].data = realSeriesData;
    predictionChart.data.datasets[1].data = predSeriesData;
    predictionChart.data.datasets[2].data = thresholdData;

    predictionChart.update('none');
}

// ============ INITIALIZATION ============

function initAllCharts() {
    initUsageChart();
    initQueueChart();
    initStateChart();
    initPredictionChart();
}

// Export for use in app.js
window.EVEACharts = {
    init: initAllCharts,
    updateUsage: updateUsageChart,
    updateQueue: updateQueueChart,
    updateState: updateStateChart,
    updatePrediction: updatePredictionChart,
};
