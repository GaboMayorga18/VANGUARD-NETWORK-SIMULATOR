import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ServidorWeb.java — Backend HTTP embebido para el Dashboard EVEA
 * 
 * Sirve archivos estáticos desde /web y expone una API REST JSON
 * para el simulador de saturación del servidor EVEA.
 * 
 * Uso: javac ServidorWeb.java && java ServidorWeb
 * Luego abrir http://localhost:8080
 */
public class ServidorWeb {

    // ======== CONSTANTES DEL SIMULADOR ========
    static final int MAXUSR = 50;
    static final int MAXCONN = 5;
    static final int MAXCOLA = 8;
    static final double MAXBAND = 100.0;
    static final double MAXALMAC = 2048.0;
    static final double UMBRAL = 80.0;
    static final int MAXHIST = 100;

    // ======== ESTADO DEL SIMULADOR ========
    static int[] uID = new int[MAXUSR];
    static String[] uNombre = new String[MAXUSR];
    static double[] uTam = new double[MAXUSR];
    static double[] uTEst = new double[MAXUSR];
    static String[] uEstado = new String[MAXUSR];
    static double[] uTEsp = new double[MAXUSR];

    static int[] cola = new int[MAXCOLA];
    static double[][] hist = new double[MAXHIST][5];

    static int totalUsr = 0;
    static int connAct = 0;
    static int tamCola = 0;
    static int atendidos = 0;
    static int rechazados = 0;
    static int alertas = 0;
    static int numInt = 0;
    static double almacUsd = 0;
    static double bandaUsd = 0;
    static double sEspera = 0;
    static double sCarga = 0;

    // Log de eventos
    static List<String> eventLog = new ArrayList<>();

    // ======== FUNCIONES DEL SIMULADOR (portadas de SimuladorServidor.java) ========

    public static double calcularUtilizacion(double valor, double maximo) {
        return (valor / maximo) * 100.0;
    }

    public static double calcularTiempoCarga(double tamMB, double velMbps) {
        return (tamMB * 8.0) / velMbps;
    }

    public static double calcularPromedio(double suma, int n) {
        if (n > 0) {
            return suma / n;
        }
        return 0;
    }

    public static double convertirMBaGB(double mb) {
        return mb / 1024.0;
    }

    public static boolean detectarSaturacion(int conn, int colaT, double banda) {
        double pB = calcularUtilizacion(banda, MAXBAND);
        return (conn >= MAXCONN) || (colaT >= MAXCOLA) || (pB >= UMBRAL);
    }

    static void addLog(String msg) {
        String ts = java.time.LocalTime.now().toString();
        if (ts.length() > 8) ts = ts.substring(0, 8);
        String entry = "[" + ts + "] " + msg;
        eventLog.add(entry);
        if (eventLog.size() > 200) eventLog.remove(0);
        System.out.println(entry);
    }

    // ======== INICIALIZACIÓN ========

    static void cargarDatosIniciales() {
        uID[0] = 1001; uNombre[0] = "Ana Torres";    uTam[0] = 15.5; uTEst[0] = 1.24; uEstado[0] = "Finalizado"; uTEsp[0] = 0.0;
        uID[1] = 1002; uNombre[1] = "Carlos Lopez";   uTam[1] = 32.0; uTEst[1] = 2.56; uEstado[1] = "Finalizado"; uTEsp[1] = 1.2;
        uID[2] = 1003; uNombre[2] = "Maria Perez";    uTam[2] = 8.5;  uTEst[2] = 0.68; uEstado[2] = "Subiendo";   uTEsp[2] = 0.5;
        uID[3] = 1004; uNombre[3] = "Diego Vega";     uTam[3] = 25.0; uTEst[3] = 2.0;  uEstado[3] = "Subiendo";   uTEsp[3] = 0.8;
        uID[4] = 1005; uNombre[4] = "Pedro Mora";     uTam[4] = 12.0; uTEst[4] = 0.96; uEstado[4] = "Esperando";  uTEsp[4] = 2.0;
        uID[5] = 1006; uNombre[5] = "Andres Gil";     uTam[5] = 60.0; uTEst[5] = 4.8;  uEstado[5] = "Rechazado";  uTEsp[5] = 0.0;

        totalUsr = 6;
        connAct = 2;
        tamCola = 1;
        cola[0] = 4;
        atendidos = 2;
        rechazados = 1;
        bandaUsd = 40.0;
        almacUsd = 47.5;
        sEspera = 1.2;
        sCarga = 3.8;

        for (int i = 1; i < MAXCOLA; i++) cola[i] = 0;

        addLog("Sistema inicializado con 6 estudiantes precargados.");
        addLog("Conexiones activas: " + connAct + "/" + MAXCONN);
        addLog("Cola: " + tamCola + "/" + MAXCOLA);
    }

    static void resetSimulacion() {
        totalUsr = 0; connAct = 0; tamCola = 0; atendidos = 0;
        rechazados = 0; alertas = 0; numInt = 0;
        almacUsd = 0; bandaUsd = 0; sEspera = 0; sCarga = 0;
        for (int i = 0; i < MAXCOLA; i++) cola[i] = 0;
        for (int i = 0; i < MAXHIST; i++) for (int j = 0; j < 5; j++) hist[i][j] = 0;
        eventLog.clear();
        cargarDatosIniciales();
        addLog(">> Simulación reiniciada.");
    }

    // ======== LÓGICA DE SIMULACIÓN ========

    static String registrarUsuario(String nombre, double tamano) {
        if (totalUsr >= MAXUSR) {
            addLog("ERROR: Límite de usuarios alcanzado.");
            return "{\"ok\":false,\"msg\":\"Limite de usuarios alcanzado\"}";
        }
        if (tamano <= 0 || tamano > 100) {
            addLog("ERROR: Tamaño de archivo inválido: " + tamano);
            return "{\"ok\":false,\"msg\":\"Tamanio invalido (0.1 a 100 MB)\"}";
        }

        double vel = MAXBAND / MAXCONN;
        double tEst = calcularTiempoCarga(tamano, vel);

        uID[totalUsr] = 1001 + totalUsr;
        uNombre[totalUsr] = nombre;
        uTam[totalUsr] = tamano;
        uTEst[totalUsr] = tEst;
        uTEsp[totalUsr] = 0.0;

        String decision;
        if (connAct < MAXCONN) {
            uEstado[totalUsr] = "Subiendo";
            connAct++;
            bandaUsd += vel;
            decision = "ADMITIDO";
            addLog(">> " + nombre + " ADMITIDO - conexion directa (ID: " + uID[totalUsr] + ")");
        } else if (tamCola < MAXCOLA) {
            uEstado[totalUsr] = "Esperando";
            cola[tamCola] = totalUsr;
            tamCola++;
            decision = "EN COLA";
            addLog(">> " + nombre + " EN COLA - servidor lleno (ID: " + uID[totalUsr] + ")");
        } else {
            uEstado[totalUsr] = "Rechazado";
            rechazados++;
            decision = "RECHAZADO";
            addLog(">>> " + nombre + " RECHAZADO - servidor Y cola llenos (ID: " + uID[totalUsr] + ")");
        }

        int id = uID[totalUsr];
        totalUsr++;
        return "{\"ok\":true,\"msg\":\"" + decision + "\",\"id\":" + id + ",\"decision\":\"" + decision + "\"}";
    }

    static String simularPaso() {
        int liber = 0;
        StringBuilder sb = new StringBuilder();

        // Procesar usuarios subiendo
        for (int i = 0; i < totalUsr; i++) {
            if (uEstado[i].equals("Subiendo")) {
                uTEst[i] -= 1.0;
                if (uTEst[i] <= 0) {
                    uEstado[i] = "Finalizado";
                    atendidos++;
                    connAct--;
                    bandaUsd -= (MAXBAND / MAXCONN);
                    if (bandaUsd < 0) bandaUsd = 0;
                    almacUsd += uTam[i];
                    sEspera += uTEsp[i];
                    sCarga += calcularTiempoCarga(uTam[i], MAXBAND / MAXCONN);
                    liber++;
                    addLog(">> " + uNombre[i] + " finalizó su carga (" + uTam[i] + " MB)");
                }
            }
            if (uEstado[i].equals("Esperando")) {
                uTEsp[i] += 1.0;
            }
        }

        // Mover usuarios de la cola
        while (liber > 0 && tamCola > 0) {
            int sigU = cola[0];
            for (int j = 0; j < tamCola - 1; j++) {
                cola[j] = cola[j + 1];
            }
            tamCola--;
            cola[tamCola] = 0;
            uEstado[sigU] = "Subiendo";
            connAct++;
            bandaUsd += (MAXBAND / MAXCONN);
            liber--;
            addLog(">> " + uNombre[sigU] + " salió de la cola - ahora subiendo");
        }

        // Detectar saturación
        boolean esSat = detectarSaturacion(connAct, tamCola, bandaUsd);
        if (esSat) {
            alertas++;
            addLog(">>> ALERTA #" + alertas + ": SERVIDOR SATURADO <<<");
        }

        // Registrar en historial
        if (numInt < MAXHIST) {
            hist[numInt][0] = numInt + 1;
            hist[numInt][1] = connAct;
            hist[numInt][2] = tamCola;
            hist[numInt][3] = rechazados;
            hist[numInt][4] = Math.round(calcularUtilizacion(connAct, MAXCONN) * 100.0) / 100.0;
            numInt++;
        }

        addLog("Intervalo #" + numInt + " registrado | Conn:" + connAct + " Cola:" + tamCola + " Sat:" + (esSat ? "SI" : "NO"));
        return "{\"ok\":true,\"intervalo\":" + numInt + ",\"saturado\":" + esSat + "}";
    }

    // ======== GENERADORES JSON ========

    static String estadoJSON() {
        double pConn = Math.round(calcularUtilizacion(connAct, MAXCONN) * 100.0) / 100.0;
        double pBanda = Math.round(calcularUtilizacion(bandaUsd, MAXBAND) * 100.0) / 100.0;
        double almGB = Math.round(convertirMBaGB(almacUsd) * 1000.0) / 1000.0;
        double maxAlGB = Math.round(convertirMBaGB(MAXALMAC) * 1000.0) / 1000.0;
        boolean esSat = detectarSaturacion(connAct, tamCola, bandaUsd);
        double pAlm = Math.round(calcularUtilizacion(almacUsd, MAXALMAC) * 100.0) / 100.0;

        boolean p = connAct >= MAXCONN;
        boolean q = tamCola >= MAXCOLA;

        return "{" +
            "\"connAct\":" + connAct + ",\"maxConn\":" + MAXCONN + ",\"pConn\":" + pConn + "," +
            "\"tamCola\":" + tamCola + ",\"maxCola\":" + MAXCOLA + "," +
            "\"bandaUsd\":" + bandaUsd + ",\"maxBanda\":" + MAXBAND + ",\"pBanda\":" + pBanda + "," +
            "\"almacUsd\":" + almacUsd + ",\"maxAlmac\":" + MAXALMAC + ",\"almGB\":" + almGB + ",\"maxAlGB\":" + maxAlGB + ",\"pAlm\":" + pAlm + "," +
            "\"atendidos\":" + atendidos + ",\"rechazados\":" + rechazados + ",\"alertas\":" + alertas + "," +
            "\"totalUsr\":" + totalUsr + ",\"saturado\":" + esSat + "," +
            "\"numInt\":" + numInt + "," +
            "\"p\":" + p + ",\"q\":" + q + "," +
            "\"sEspera\":" + sEspera + ",\"sCarga\":" + sCarga +
            "}";
    }

    static String usuariosJSON() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < totalUsr; i++) {
            if (i > 0) sb.append(",");
            double vel = MAXBAND / MAXCONN;
            double tCarga = Math.round(calcularTiempoCarga(uTam[i], vel) * 100.0) / 100.0;
            sb.append("{\"id\":").append(uID[i])
              .append(",\"nombre\":\"").append(escapeJSON(uNombre[i])).append("\"")
              .append(",\"tamano\":").append(uTam[i])
              .append(",\"tEst\":").append(Math.round(uTEst[i] * 100.0) / 100.0)
              .append(",\"estado\":\"").append(uEstado[i]).append("\"")
              .append(",\"tEsp\":").append(Math.round(uTEsp[i] * 100.0) / 100.0)
              .append(",\"tCarga\":").append(tCarga)
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    static String historialJSON() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < numInt; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"intervalo\":").append((int) hist[i][0])
              .append(",\"conn\":").append((int) hist[i][1])
              .append(",\"cola\":").append((int) hist[i][2])
              .append(",\"rechazados\":").append((int) hist[i][3])
              .append(",\"pUso\":").append(hist[i][4])
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    static String reporteJSON() {
        double vel = MAXBAND / MAXCONN;
        double pConn = Math.round(calcularUtilizacion(connAct, MAXCONN) * 100.0) / 100.0;
        double pBanda = Math.round(calcularUtilizacion(bandaUsd, MAXBAND) * 100.0) / 100.0;

        double sE = 0, sC = 0;
        int cE = 0, cC = 0;
        for (int i = 0; i < totalUsr; i++) {
            if (!uEstado[i].equals("Rechazado")) { sE += uTEsp[i]; cE++; }
            if (uEstado[i].equals("Finalizado") || uEstado[i].equals("Subiendo")) {
                sC += calcularTiempoCarga(uTam[i], vel); cC++;
            }
        }
        double promEsp = Math.round(calcularPromedio(sE, cE) * 100.0) / 100.0;
        double promCrg = Math.round(calcularPromedio(sC, cC) * 100.0) / 100.0;
        double pR = totalUsr > 0 ? Math.round(((double) rechazados / totalUsr) * 10000.0) / 100.0 : 0;

        return "{\"totalUsr\":" + totalUsr + ",\"atendidos\":" + atendidos + ",\"rechazados\":" + rechazados +
               ",\"pRechazo\":" + pR + ",\"promEspera\":" + promEsp + ",\"promCarga\":" + promCrg +
               ",\"pConn\":" + pConn + ",\"pBanda\":" + pBanda + "}";
    }

    static String prediccionJSON() {
        // Regresión lineal sobre el historial de uso del servidor
        if (numInt < 2) {
            return "{\"riesgo\":0,\"tendencia\":\"neutral\",\"prediccionSaturacion\":-1," +
                   "\"mensajes\":[\"Se necesitan al menos 2 intervalos para generar predicciones.\"]," +
                   "\"datosPrediccion\":[]}";
        }

        // Calcular regresión lineal: y = mx + b sobre pUso
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = numInt;
        for (int i = 0; i < n; i++) {
            double x = hist[i][0];
            double y = hist[i][4];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denom = (n * sumX2 - sumX * sumX);
        double m = denom != 0 ? (n * sumXY - sumX * sumY) / denom : 0;
        double b = (sumY - m * sumX) / n;

        // Tendencia
        String tendencia = m > 2 ? "critica" : m > 0.5 ? "ascendente" : m < -0.5 ? "descendente" : "estable";

        // Predicción: ¿cuándo llega a 100%?
        int predSat = -1;
        if (m > 0) {
            predSat = (int) Math.ceil((100.0 - b) / m);
            if (predSat <= numInt) predSat = numInt + 1;
        }

        // Riesgo (0-100)
        double ultimoUso = hist[numInt - 1][4];
        double riesgo = ultimoUso * 0.5 + (m > 0 ? m * 10 : 0) + (tamCola / (double) MAXCOLA) * 20;
        if (riesgo > 100) riesgo = 100;
        if (riesgo < 0) riesgo = 0;
        riesgo = Math.round(riesgo * 10.0) / 10.0;

        // Datos de predicción futura (5 intervalos)
        StringBuilder predData = new StringBuilder("[");
        for (int i = 1; i <= 5; i++) {
            int futuro = numInt + i;
            double predY = m * futuro + b;
            if (predY > 100) predY = 100;
            if (predY < 0) predY = 0;
            predY = Math.round(predY * 100.0) / 100.0;
            if (i > 1) predData.append(",");
            predData.append("{\"intervalo\":").append(futuro).append(",\"pUso\":").append(predY).append("}");
        }
        predData.append("]");

        // Regresión sobre cola
        double sumYC = 0, sumXYC = 0;
        for (int i = 0; i < n; i++) {
            double x = hist[i][0];
            double yc = hist[i][2];
            sumYC += yc;
            sumXYC += x * yc;
        }
        double mCola = denom != 0 ? (n * sumXYC - sumX * sumYC) / denom : 0;

        // Mensajes de recomendación
        StringBuilder msgs = new StringBuilder("[");
        int mc = 0;
        if (riesgo > 70) { if (mc > 0) msgs.append(","); msgs.append("\"CRITICO: Riesgo de saturacion muy alto (").append(riesgo).append("%). Considere ampliar capacidad del servidor.\""); mc++; }
        if (m > 2) { if (mc > 0) msgs.append(","); msgs.append("\"ALERTA: El uso del servidor crece rapidamente. Tendencia critica.\""); mc++; }
        if (predSat > 0 && predSat <= numInt + 5) { if (mc > 0) msgs.append(","); msgs.append("\"PREDICCION: Saturacion estimada en intervalo #").append(predSat).append(".\""); mc++; }
        if (mCola > 0.3) { if (mc > 0) msgs.append(","); msgs.append("\"La cola de espera muestra tendencia creciente. Posible desbordamiento.\""); mc++; }
        if (rechazados > 0 && totalUsr > 0) {
            double tasaR = ((double) rechazados / totalUsr) * 100;
            if (tasaR > 15) { if (mc > 0) msgs.append(","); msgs.append("\"Tasa de rechazo alta (").append(Math.round(tasaR * 10.0) / 10.0).append("%). Se recomienda aumentar conexiones maximas.\""); mc++; }
        }
        if (riesgo <= 30 && m <= 0) { if (mc > 0) msgs.append(","); msgs.append("\"Sistema estable. No se detectan riesgos inmediatos.\""); mc++; }
        if (mc == 0) { msgs.append("\"Sistema bajo monitoreo. Datos insuficientes para recomendaciones detalladas.\""); }
        msgs.append("]");

        return "{\"riesgo\":" + riesgo + ",\"tendencia\":\"" + tendencia + "\"," +
               "\"prediccionSaturacion\":" + predSat + "," +
               "\"pendiente\":" + (Math.round(m * 1000.0) / 1000.0) + "," +
               "\"intercepto\":" + (Math.round(b * 100.0) / 100.0) + "," +
               "\"pendienteCola\":" + (Math.round(mCola * 1000.0) / 1000.0) + "," +
               "\"mensajes\":" + msgs.toString() + "," +
               "\"datosPrediccion\":" + predData.toString() + "}";
    }

    static String logJSON() {
        StringBuilder sb = new StringBuilder("[");
        int start = Math.max(0, eventLog.size() - 50);
        for (int i = start; i < eventLog.size(); i++) {
            if (i > start) sb.append(",");
            sb.append("\"").append(escapeJSON(eventLog.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    static String escapeJSON(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    // ======== SERVIDOR HTTP ========

    static String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".json")) return "application/json; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    static void sendResponse(HttpExchange ex, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static String readRequestBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
        return bos.toString("UTF-8");
    }

    // Parseo JSON simple (sin dependencias externas)
    static String getJSONValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        idx = json.indexOf(":", idx + search.length());
        if (idx < 0) return null;
        idx++;
        while (idx < json.length() && json.charAt(idx) == ' ') idx++;
        if (idx >= json.length()) return null;

        if (json.charAt(idx) == '"') {
            int end = json.indexOf('"', idx + 1);
            return json.substring(idx + 1, end);
        } else {
            int end = idx;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(idx, end).trim();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException e) { /* default */ }
        }

        cargarDatosIniciales();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API endpoints
        server.createContext("/api/estado", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            sendResponse(ex, 200, "application/json", estadoJSON());
        });

        server.createContext("/api/usuarios", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            sendResponse(ex, 200, "application/json", usuariosJSON());
        });

        server.createContext("/api/historial", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            sendResponse(ex, 200, "application/json", historialJSON());
        });

        server.createContext("/api/reporte", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            sendResponse(ex, 200, "application/json", reporteJSON());
        });

        server.createContext("/api/prediccion", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            sendResponse(ex, 200, "application/json", prediccionJSON());
        });

        server.createContext("/api/log", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            sendResponse(ex, 200, "application/json", logJSON());
        });

        server.createContext("/api/registrar", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            if (!ex.getRequestMethod().equals("POST")) {
                sendResponse(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            String body = readRequestBody(ex);
            String nombre = getJSONValue(body, "nombre");
            String tamStr = getJSONValue(body, "tamano");
            if (nombre == null || tamStr == null) {
                sendResponse(ex, 400, "application/json", "{\"ok\":false,\"msg\":\"Faltan parametros\"}");
                return;
            }
            double tam;
            try { tam = Double.parseDouble(tamStr); } catch (NumberFormatException e) {
                sendResponse(ex, 400, "application/json", "{\"ok\":false,\"msg\":\"Tamano invalido\"}");
                return;
            }
            String result = registrarUsuario(nombre, tam);
            sendResponse(ex, 200, "application/json", result);
        });

        server.createContext("/api/simular", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            if (!ex.getRequestMethod().equals("POST")) {
                sendResponse(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            String result = simularPaso();
            sendResponse(ex, 200, "application/json", result);
        });

        server.createContext("/api/reset", ex -> {
            if (ex.getRequestMethod().equals("OPTIONS")) { sendCORS(ex); return; }
            if (!ex.getRequestMethod().equals("POST")) {
                sendResponse(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }
            resetSimulacion();
            sendResponse(ex, 200, "application/json", "{\"ok\":true,\"msg\":\"Simulacion reiniciada\"}");
        });

        // Servir archivos estáticos
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            // Intentar servir desde la carpeta web/
            File baseDir = new File(System.getProperty("user.dir"), "web");
            File file = new File(baseDir, path);

            // Prevenir path traversal
            if (!file.getCanonicalPath().startsWith(baseDir.getCanonicalPath())) {
                sendResponse(ex, 403, "text/plain", "Forbidden");
                return;
            }

            if (file.exists() && file.isFile()) {
                byte[] data = Files.readAllBytes(file.toPath());
                String mime = getMimeType(path);
                ex.getResponseHeaders().set("Content-Type", mime);
                ex.getResponseHeaders().set("Cache-Control", "no-cache");
                ex.sendResponseHeaders(200, data.length);
                OutputStream os = ex.getResponseBody();
                os.write(data);
                os.close();
            } else {
                sendResponse(ex, 404, "text/plain", "File not found: " + path);
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();

        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║   EVEA Server Dashboard - Simulador de Saturacion    ║");
        System.out.println("║                                                       ║");
        System.out.println("║   Servidor iniciado en: http://localhost:" + port + "        ║");
        System.out.println("║   Presione Ctrl+C para detener                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    static void sendCORS(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        ex.sendResponseHeaders(204, -1);
    }
}
