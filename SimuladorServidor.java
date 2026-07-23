import java.util.Scanner;

public class SimuladorServidor {

    public static double calcularUtilizacion(double valor, double maximo) {
        double porc;
        porc = (valor / maximo) * 100;
        return porc;
    }

    public static double calcularTiempoCarga(double tamMB, double velMbps) {
        double seg;
        seg = (tamMB * 8) / velMbps;
        return seg;
    }

    public static double calcularPromedio(double suma, int n) {
        double prom;
        if (n > 0) {
            prom = suma / n;
        } else {
            prom = 0;
        }
        return prom;
    }

    public static double convertirMBaGB(double mb) {
        double gb;
        gb = mb / 1024;
        return gb;
    }

    public static boolean detectarSaturacion(int conn, int cola, double banda, int maxConn, int maxCola, double maxBanda, double UMBRAL) {
        boolean sat;
        double pB;
        pB = calcularUtilizacion(banda, maxBanda);
        if ((conn >= maxConn) || (cola >= maxCola) || (pB >= UMBRAL)) {
            sat = true;
        } else {
            sat = false;
        }
        return sat;
    }

    public static void cargarDatos(int[] id, String[] nombre, double[] tamano, double[] tEst, String[] estado, double[] tEsp) {
        id[0] = 1001;
        nombre[0] = "Ana Torres";
        tamano[0] = 15.5;
        tEst[0] = 1.24;
        estado[0] = "Finalizado";
        tEsp[0] = 0.0;
        
        id[1] = 1002;
        nombre[1] = "Carlos Lopez";
        tamano[1] = 32.0;
        tEst[1] = 2.56;
        estado[1] = "Finalizado";
        tEsp[1] = 1.2;
        
        id[2] = 1003;
        nombre[2] = "Maria Perez";
        tamano[2] = 8.5;
        tEst[2] = 0.68;
        estado[2] = "Subiendo";
        tEsp[2] = 0.5;
        
        id[3] = 1004;
        nombre[3] = "Diego Vega";
        tamano[3] = 25.0;
        tEst[3] = 2.0;
        estado[3] = "Subiendo";
        tEsp[3] = 0.8;
        
        id[4] = 1005;
        nombre[4] = "Pedro Mora";
        tamano[4] = 12.0;
        tEst[4] = 0.96;
        estado[4] = "Esperando";
        tEsp[4] = 2.0;
        
        id[5] = 1006;
        nombre[5] = "Andres Gil";
        tamano[5] = 60.0;
        tEst[5] = 4.8;
        estado[5] = "Rechazado";
        tEsp[5] = 0.0;
        
        System.out.println(">> 6 estudiantes precargados.");
    }

    public static void mostrarEstado(int conn, int maxConn, double pConn, int cola, int maxCola, double banda, double pBanda, double almGB, double maxAlGB, int atend, int rechaz, int alertas, boolean esSat) {
        System.out.println("=== ESTADO DEL SERVIDOR ===");
        System.out.println("Conexiones: " + conn + "/" + maxConn + " (" + pConn + "%) | Cola: " + cola + "/" + maxCola);
        System.out.println("Banda: " + banda + " Mbps (" + pBanda + "%) | Almac: " + almGB + "/" + maxAlGB + " GB");
        System.out.println("Atendidos: " + atend + " | Rechazados: " + rechaz + " | Alertas: " + alertas);
        if (esSat) {
            System.out.println(">>> SATURADO <<<");
        } else {
            System.out.println("Estado: NORMAL");
        }
    }

    public static void generarReporte(int[] id, String[] nombre, String[] estado, double[] tEsp, double[] tamano, int total, int atend, int rechaz, double pUso, double pBanda, double vel) {
        int i;
        double sE = 0;
        double sC = 0;
        int cE = 0;
        int cC = 0;
        double tC;
        double pR;
        
        System.out.println("=== REPORTE ESTADISTICO ===");
        System.out.println("ID | Nombre | Estado | T.Esp(s) | T.Carga(s)");
        for (i = 0; i <= total - 1; i++) {
            tC = calcularTiempoCarga(tamano[i], vel);
            System.out.println(id[i] + " | " + nombre[i] + " | " + estado[i] + " | " + tEsp[i] + " | " + tC);
            if (!estado[i].equals("Rechazado")) {
                sE = sE + tEsp[i];
                cE = cE + 1;
            }
            if (estado[i].equals("Finalizado") || estado[i].equals("Subiendo")) {
                sC = sC + tC;
                cC = cC + 1;
            }
        }
        
        if (total > 0) {
            pR = ((double) rechaz / total) * 100;
        } else {
            pR = 0;
        }
        
        System.out.println("Total: " + total + " | Atendidos: " + atend + " | Rechazados: " + rechaz + " (" + pR + "%)");
        System.out.println("Prom.espera: " + calcularPromedio(sE, cE) + " s | Prom.carga: " + calcularPromedio(sC, cC) + " s");
        System.out.println("Uso servidor: " + pUso + "% | Uso banda: " + pBanda + "%");
    }

    public static void mostrarHistorial(double[][] hist, int numInt) {
        int i;
        System.out.println("=== HISTORIAL [Interv | Conn | Cola | Rechaz | %Uso] ===");
        if (numInt == 0) {
            System.out.println("(Sin intervalos registrados)");
        } else {
            for (i = 0; i <= numInt - 1; i++) {
                System.out.println(hist[i][0] + " | " + hist[i][1] + " | " + hist[i][2] + " | " + hist[i][3] + " | " + hist[i][4] + "%");
            }
        }
    }

    public static void mostrarTablasVerdad(boolean p, boolean q) {
        System.out.println("=== TABLAS DE VERDAD ===");
        System.out.println("P: Conexiones al maximo | Q: Cola llena");
        System.out.println("P     | Q     | P Y Q | P Y(NOQ) | Decision");
        System.out.println("Falso | Falso | Falso | Falso    | ADMITIR");
        System.out.println("Falso | Verd  | Falso | Falso    | ADMITIR");
        System.out.println("Verd  | Falso | Falso | Verdad   | EN COLA");
        System.out.println("Verd  | Verd  | Verd  | Falso    | RECHAZAR");
        System.out.println("Estado actual: P=" + p + " | Q=" + q);
        if (!p) {
            System.out.println("Decision: ADMITIR (conexiones disponibles)");
        } else {
            if (p && (!q)) {
                System.out.println("Decision: EN COLA (servidor lleno, cola disponible)");
            } else {
                System.out.println("Decision: RECHAZAR (servidor lleno Y cola llena)");
            }
        }
    }

    public static void mostrarReporteMatematico(int total, int atend, int rechaz, int conn, int maxConn, double banda, double maxBanda, double sEsp, double sCrg) {
        double util;
        double pB;
        double pR;
        double promE;
        double promC;
        double espMin;
        double bandaG;
        
        util = calcularUtilizacion(conn, maxConn);
        pB = calcularUtilizacion(banda, maxBanda);
        promE = calcularPromedio(sEsp, atend);
        promC = calcularPromedio(sCrg, atend);
        espMin = promE / 60;
        bandaG = banda / 1000;
        
        if (total > 0) {
            pR = ((double) rechaz / total) * 100;
        } else {
            pR = 0;
        }
        
        System.out.println("=== REPORTE MATEMATICO ===");
        System.out.println("1) Utilizacion: (" + conn + "/" + maxConn + ") x100 = " + util + "%");
        if (util >= 80) {
            System.out.println("   >> NIVEL CRITICO (>=80%)");
        } else {
            System.out.println("   >> NIVEL NORMAL (<80%)");
        }
        System.out.println("2) Prom.espera: " + sEsp + "/" + atend + " = " + promE + " s = " + espMin + " min");
        System.out.println("3) %Rechazados: (" + rechaz + "/" + total + ") x100 = " + pR + "%");
        System.out.println("4) Uso banda:   (" + banda + "/" + maxBanda + ") x100 = " + pB + "%");
        System.out.println("5) Conversiones: " + banda + " Mbps = " + bandaG + " Gbps | " + promE + " s = " + espMin + " min");
    }
    
    public static void buscarUsuario(int idBuscado, int[] id, String[] nombre, double[] tamano, double[] tEst, String[] estado, double[] tEsp, int total) {
    boolean encontrado = false;
    
    System.out.println("=== RASTREO DE USUARIO ===");
    for (int i = 0; i < total; i++) {
        if (id[i] == idBuscado) {
            System.out.println("ID: " + id[i]);
            System.out.println("Estudiante: " + nombre[i]);
            System.out.println("Archivo: " + tamano[i] + " MB");
            System.out.println("Estado actual: [" + estado[i].toUpperCase() + "]");
            
            // Lógica condicional según el estado del estudiante
            if (estado[i].equals("Esperando")) {
                System.out.println(">> Tiempo esperando en cola: " + tEsp[i] + " s");
            } else if (estado[i].equals("Subiendo")) {
                System.out.println(">> Tiempo restante para finalizar: " + tEst[i] + " s");
            } else if (estado[i].equals("Finalizado")) {
                System.out.println(">> El archivo ya se encuentra alojado en el servidor.");
            } else if (estado[i].equals("Rechazado")) {
                System.out.println(">> Carga denegada. El servidor y la cola estaban saturados.");
            }
            
            encontrado = true;
            break; // Detiene el ciclo porque ya encontró al usuario
        }
    }
    
    if (!encontrado) {
        System.out.println("Error: No se encontró ningún estudiante con el ID " + idBuscado);
    }
}

    public static void main(String[] args) {
        Scanner leer = new Scanner(System.in);
        
        int MAXUSR = 20;
        int MAXCONN = 5;
        int MAXCOLA = 8;
        double MAXBAND = 100.0;
        double MAXALMAC = 2048.0;
        double UMBRAL = 80.0;
        
        int[] uID = new int[20];
        String[] uNombre = new String[20];
        double[] uTam = new double[20];
        double[] uTEst = new double[20];
        String[] uEstado = new String[20];
        double[] uTEsp = new double[20];
        
        int[] cola = new int[8];
        double[][] hist = new double[10][5];
        
        int totalUsr = 0;
        int connAct = 0;
        int tamCola = 0;
        int atendidos = 0;
        int rechazados = 0;
        int alertas = 0;
        int numInt = 0;
        double almacUsd = 0;
        double bandaUsd = 0;
        double sEspera = 0;
        double sCarga = 0;
        
        int op;
        int i, j, liber, sigU;
        double vel, nuevoT, tEst;
        String nuevoN = "";
        boolean esSat, p, q;
        double pConn, pBanda, almGB, maxAlGB;
        
        for (i = 0; i <= 7; i++) {
            cola[i] = 0;
        }
        
        cargarDatos(uID, uNombre, uTam, uTEst, uEstado, uTEsp);
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
        
        do {
            System.out.println("");
            System.out.println("=== SIMULADOR DE SATURACION DE SERVIDOR ===");
            System.out.println("  1. Registrar usuario     2. Simular paso");
            System.out.println("  3. Estado servidor       4. Reporte stats");
            System.out.println("  5. Historial (matriz)    6. Tablas verdad");
            System.out.println("  7. Reporte matematico    8. Buscar usuario");
            System.out.println("  9. Salir");
            System.out.println("===========================================");
            System.out.print("Opcion (1-9): ");

            op = leer.nextInt();
            
            if ((op < 1) || (op > 9)) {
                System.out.println("Opcion invalida.");
            } else {
                switch (op) {
                    case 1:
                        System.out.println("--- REGISTRO DE USUARIO ---");
                        if (totalUsr >= MAXUSR) {
                            System.out.println("Error: limite de usuarios alcanzado.");
                        } else {
                            leer.nextLine(); // Limpiar el buffer
                            System.out.print("Nombre: ");
                            nuevoN = leer.nextLine();
                            System.out.print("Tamanio del archivo en MB (0.1 a 100): ");
                            nuevoT = leer.nextDouble();
                            if ((nuevoT <= 0) || (nuevoT > 100)) {
                                System.out.println("Error: tamanio invalido (0.1 a 100 MB).");
                            } else {
                                vel = MAXBAND / MAXCONN;
                                tEst = calcularTiempoCarga(nuevoT, vel);
                                uID[totalUsr] = 1001 + totalUsr;
                                uNombre[totalUsr] = nuevoN;
                                uTam[totalUsr] = nuevoT;
                                uTEst[totalUsr] = tEst;
                                uTEsp[totalUsr] = 0.0;
                                
                                if (!(connAct >= MAXCONN)) {
                                    uEstado[totalUsr] = "Subiendo";
                                    connAct = connAct + 1;
                                    bandaUsd = bandaUsd + vel;
                                    System.out.println("Decision: ADMITIDO - hay conexiones disponibles.");
                                } else {
                                    if ((connAct >= MAXCONN) && (tamCola < MAXCOLA)) {
                                        uEstado[totalUsr] = "Esperando";
                                        cola[tamCola] = totalUsr;
                                        tamCola = tamCola + 1;
                                        System.out.println("Decision: EN COLA - servidor lleno, cola disponible.");
                                    } else {
                                        uEstado[totalUsr] = "Rechazado";
                                        rechazados = rechazados + 1;
                                        System.out.println("Decision: RECHAZADO - servidor Y cola llenos.");
                                    }
                                }
                                totalUsr = totalUsr + 1;
                            }
                        }
                        break;
                    case 2:
                        System.out.println("--- SIMULANDO PASO DE TIEMPO ---");
                        liber = 0;
                        for (i = 0; i <= totalUsr - 1; i++) {
                            if (uEstado[i].equals("Subiendo")) {
                                uTEst[i] = uTEst[i] - 1.0;
                                if (uTEst[i] <= 0) {
                                    uEstado[i] = "Finalizado";
                                    atendidos = atendidos + 1;
                                    connAct = connAct - 1;
                                    bandaUsd = bandaUsd - (MAXBAND / MAXCONN);
                                    if (bandaUsd < 0) {
                                        bandaUsd = 0;
                                    }
                                    almacUsd = almacUsd + uTam[i];
                                    sEspera = sEspera + uTEsp[i];
                                    sCarga = sCarga + calcularTiempoCarga(uTam[i], MAXBAND / MAXCONN);
                                    liber = liber + 1;
                                    System.out.println(">> " + uNombre[i] + " finalizo su carga.");
                                }
                            }
                            if (uEstado[i].equals("Esperando")) {
                                uTEsp[i] = uTEsp[i] + 1.0;
                            }
                        }
                        while ((liber > 0) && (tamCola > 0)) {
                            sigU = cola[0];
                            for (j = 0; j <= tamCola - 2; j++) {
                                cola[j] = cola[j + 1];
                            }
                            tamCola = tamCola - 1;
                            cola[tamCola] = 0;
                            uEstado[sigU] = "Subiendo";
                            connAct = connAct + 1;
                            bandaUsd = bandaUsd + (MAXBAND / MAXCONN);
                            liber = liber - 1;
                            System.out.println(">> " + uNombre[sigU] + " salio de la cola.");
                        }
                        esSat = detectarSaturacion(connAct, tamCola, bandaUsd, MAXCONN, MAXCOLA, MAXBAND, UMBRAL);
                        if (esSat) {
                            alertas = alertas + 1;
                            System.out.println(">>> ALERTA #" + alertas + ": SERVIDOR SATURADO <<<");
                        }
                        if (numInt < 10) {
                            hist[numInt][0] = numInt + 1;
                            hist[numInt][1] = connAct;
                            hist[numInt][2] = tamCola;
                            hist[numInt][3] = rechazados;
                            hist[numInt][4] = calcularUtilizacion(connAct, MAXCONN);
                            numInt = numInt + 1;
                        }
                        System.out.println("Intervalo #" + numInt + " registrado.");
                        break;
                    case 3:
                        pConn = calcularUtilizacion(connAct, MAXCONN);
                        pBanda = calcularUtilizacion(bandaUsd, MAXBAND);
                        almGB = convertirMBaGB(almacUsd);
                        maxAlGB = convertirMBaGB(MAXALMAC);
                        esSat = detectarSaturacion(connAct, tamCola, bandaUsd, MAXCONN, MAXCOLA, MAXBAND, UMBRAL);
                        mostrarEstado(connAct, MAXCONN, pConn, tamCola, MAXCOLA, bandaUsd, pBanda, almGB, maxAlGB, atendidos, rechazados, alertas, esSat);
                        break;
                    case 4:
                        vel = MAXBAND / MAXCONN;
                        pConn = calcularUtilizacion(connAct, MAXCONN);
                        pBanda = calcularUtilizacion(bandaUsd, MAXBAND);
                        generarReporte(uID, uNombre, uEstado, uTEsp, uTam, totalUsr, atendidos, rechazados, pConn, pBanda, vel);
                        break;
                    case 5:
                        mostrarHistorial(hist, numInt);
                        break;
                    case 6:
                        p = connAct >= MAXCONN;
                        q = tamCola >= MAXCOLA;
                        mostrarTablasVerdad(p, q);
                        break;
                    case 7:
                        mostrarReporteMatematico(totalUsr, atendidos, rechazados, connAct, MAXCONN, bandaUsd, MAXBAND, sEspera, sCarga);
                        break;
                    case 8:
                        System.out.print("Ingrese el ID del estudiante a buscar (ej. 1001, 1002...): ");
                        int idB = leer.nextInt();
                        buscarUsuario(idB, uID, uNombre, uTam, uTEst, uEstado, uTEsp, totalUsr);
                        break;
                    case 9:
                        System.out.println("Sistema cerrado. Hasta luego.");
                        break;
                    default:
                        System.out.println("Opcion no reconocida.");
                        break;
                }
            }
        } while (op != 9);
    }
}
