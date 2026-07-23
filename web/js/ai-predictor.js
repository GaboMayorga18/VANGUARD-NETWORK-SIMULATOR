/**
 * ai-predictor.js — Client-side AI prediction analysis for EVEA Dashboard
 * 
 * Processes prediction data from the server and generates
 * visual risk indicators, recommendations, and trend analysis.
 */

const AIPredictor = {
    /**
     * Get CSS class for risk level
     */
    getRiskClass(riesgo) {
        if (riesgo >= 65) return 'high';
        if (riesgo >= 35) return 'medium';
        return 'low';
    },

    /**
     * Get human-readable risk label
     */
    getRiskLabel(riesgo) {
        if (riesgo >= 80) return 'MUY ALTO';
        if (riesgo >= 65) return 'ALTO';
        if (riesgo >= 45) return 'MODERADO';
        if (riesgo >= 25) return 'BAJO';
        return 'MÍNIMO';
    },

    /**
     * Get tendency icon and label
     */
    getTendencyInfo(tendencia) {
        const map = {
            'critica': { icon: '🔴', label: 'Tendencia Crítica ↑↑', color: 'var(--red)' },
            'ascendente': { icon: '🟠', label: 'Tendencia Ascendente ↑', color: 'var(--amber)' },
            'estable': { icon: '🟢', label: 'Tendencia Estable →', color: 'var(--green)' },
            'descendente': { icon: '🔵', label: 'Tendencia Descendente ↓', color: 'var(--cyan)' },
            'neutral': { icon: '⚪', label: 'Sin datos suficientes', color: 'var(--text-muted)' },
        };
        return map[tendencia] || map['neutral'];
    },

    /**
     * Classify a message for styling
     */
    classifyMessage(msg) {
        const lower = msg.toLowerCase();
        if (lower.includes('critico') || lower.includes('saturacion muy alto')) return 'critical';
        if (lower.includes('alerta') || lower.includes('creciente') || lower.includes('alta')) return 'warning';
        if (lower.includes('estable') || lower.includes('no se detectan')) return 'ok';
        return 'info';
    },

    /**
     * Render the full AI prediction panel
     */
    renderPanel(prediccion, containerEl) {
        if (!containerEl) return;

        if (!prediccion || prediccion.riesgo === undefined) {
            containerEl.innerHTML = `
                <div class="ai-msg info">
                    <span>⏳</span> Simulando... Se necesitan más datos para generar predicciones.
                </div>
            `;
            return;
        }

        const riskClass = this.getRiskClass(prediccion.riesgo);
        const riskLabel = this.getRiskLabel(prediccion.riesgo);
        const tendency = this.getTendencyInfo(prediccion.tendencia);

        let predText = '';
        if (prediccion.prediccionSaturacion > 0) {
            predText = `Saturación estimada en intervalo #${prediccion.prediccionSaturacion}`;
        } else if (prediccion.tendencia === 'descendente' || prediccion.tendencia === 'estable') {
            predText = 'No se prevé saturación en el corto plazo';
        } else {
            predText = 'Datos insuficientes para estimar saturación';
        }

        let html = `
            <div class="ai-risk-meter">
                <div class="risk-circle ${riskClass}">
                    <div class="risk-value">${Math.round(prediccion.riesgo)}%</div>
                    <div class="risk-label">Riesgo</div>
                </div>
                <div class="risk-info">
                    <div class="risk-tendency" style="color: ${tendency.color}">
                        ${tendency.icon} ${tendency.label}
                    </div>
                    <div class="risk-prediction">${predText}</div>
                    ${prediccion.pendiente !== undefined ? `
                        <div class="risk-prediction" style="margin-top: 4px; font-family: 'JetBrains Mono', monospace; font-size: 0.65rem;">
                            m=${prediccion.pendiente} | b=${prediccion.intercepto}
                        </div>
                    ` : ''}
                </div>
            </div>
            <div class="ai-messages">
        `;

        if (prediccion.mensajes && prediccion.mensajes.length > 0) {
            prediccion.mensajes.forEach(msg => {
                const cls = this.classifyMessage(msg);
                const icon = cls === 'critical' ? '🚨' : cls === 'warning' ? '⚠️' : cls === 'ok' ? '✅' : 'ℹ️';
                html += `<div class="ai-msg ${cls}">${icon} ${msg}</div>`;
            });
        }

        html += '</div>';
        containerEl.innerHTML = html;
    }
};

window.AIPredictor = AIPredictor;
