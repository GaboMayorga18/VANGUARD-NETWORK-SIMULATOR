# 🖥️ EVEA Server Dashboard

**Dashboard web interactivo** para monitorear en tiempo real la saturación del servidor EVEA, con análisis predictivo basado en IA.

## 📋 Requisitos

- **Java JDK 8+** instalado y en el PATH del sistema
- Navegador web moderno (Chrome, Edge, Firefox)
- Conexión a internet (para cargar Chart.js desde CDN la primera vez)

## 🚀 Inicio Rápido

### Opción 1: Doble clic (recomendado)
Ejecute el archivo `compilar_y_ejecutar.bat` con doble clic.

### Opción 2: Desde CMD
```cmd
cd C:\Users\Gabo\Documents\Antigravity\SimuladorRed
javac -encoding UTF-8 ServidorWeb.java
java ServidorWeb
```
Luego abra http://localhost:8080 en su navegador.

## 🎯 Funcionalidades

### Dashboard
- **6 tarjetas KPI** con métricas en tiempo real (conexiones, cola, banda, almacenamiento, atendidos, rechazados)
- **Indicadores visuales** de saturación con animaciones de alerta
- **Reloj en vivo** y contador de intervalos

### Gráficos (Chart.js)
- 📈 **Uso del servidor** — Línea temporal con doble eje Y
- 📊 **Cola y Rechazados** — Barras por intervalo
- 🍩 **Distribución de estados** — Dona interactiva
- 🔮 **Proyección de saturación** — Línea con predicción IA superpuesta

### Análisis Predictivo IA
- **Regresión lineal** sobre historial de uso
- **Indicador de riesgo** (0-100%) con clasificación visual
- **Predicción de saturación** (cuándo ocurrirá)
- **Recomendaciones automáticas** basadas en tendencias

### Simulación
- ⚡ Simular paso de tiempo manualmente
- ▶ Auto-simulación con velocidad configurable (1s, 2s, 3s, 5s)
- ➕ Registrar nuevos usuarios con modal interactivo
- ↺ Reiniciar simulación

### Extras
- 🔀 **Tabla de verdad** con resaltado del estado actual (P∧Q)
- 📐 **Reporte matemático** en tiempo real
- 📜 **Log de eventos** con scroll automático
- ⌨️ **Atajos de teclado**: Espacio = simular, Esc = cerrar modal

## 📁 Estructura del Proyecto

```
SimuladorRed/
├── SimuladorServidor.java      ← Simulador original (consola)
├── ServidorWeb.java            ← Backend HTTP + API REST
├── compilar_y_ejecutar.bat     ← Script de inicio
├── README.md                   ← Este archivo
└── web/
    ├── index.html              ← Dashboard principal
    ├── css/
    │   └── dashboard.css       ← Estilos dark mode
    └── js/
        ├── app.js              ← Lógica frontend
        ├── charts.js           ← Configuración Chart.js
        └── ai-predictor.js     ← Motor de predicción IA
```

## 🔌 API REST

| Endpoint | Método | Descripción |
|---|---|---|
| `/api/estado` | GET | Estado actual del servidor |
| `/api/usuarios` | GET | Lista de usuarios |
| `/api/historial` | GET | Historial de intervalos |
| `/api/reporte` | GET | Reporte estadístico |
| `/api/prediccion` | GET | Análisis predictivo IA |
| `/api/log` | GET | Log de eventos |
| `/api/registrar` | POST | Registrar usuario `{nombre, tamano}` |
| `/api/simular` | POST | Simular paso de tiempo |
| `/api/reset` | POST | Reiniciar simulación |

## 🛠️ Tecnologías

- **Backend**: Java (com.sun.net.httpserver — embebido en el JDK)
- **Frontend**: HTML5 + CSS3 + JavaScript vanilla
- **Gráficos**: Chart.js 4.x (CDN)
- **Diseño**: Dark mode con glassmorphism, Inter + JetBrains Mono
