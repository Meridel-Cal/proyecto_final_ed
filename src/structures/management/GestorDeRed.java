package structures.management;

import model.LecturaSensor;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GestorDeRed - Módulo Integrador de Estructuras de Datos.
 * Gestiona alertas, consultas, topología y buffer mediante el Java Collections Framework (JCF).
 */
public class GestorDeRed {

    // =========================================================================
    // ATRIBUTOS Y ESTRUCTURAS DE DATOS
    // =========================================================================

    // Historial lineal de eventos (cronológico)
    private final List<String> historialEventos;

    // Navegador bidireccional de alertas (permite avanzar y retroceder en O(1))
    private final Deque<String> navegadorAlertas;
    private String alertaActual;

    // Cola de turnos FIFO para la consulta rotativa de sensores
    private final Queue<String> colaMonitoreo;

    // Grafo representado como Lista de Adyacencia: sensor -> lista de vecinos
    private final Map<String, List<String>> topologiaRed;

    // Buffer FIFO para encolar y procesar lecturas entrantes
    private final Queue<LecturaSensor> bufferIngesta;

    public GestorDeRed() {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Se mantuvieron las instancias concretas elegidas (ArrayList, ArrayDeque, HashMap)
        // pero se aseguraron como 'final' para evitar reasignaciones accidentales de las colecciones.
        this.historialEventos = new ArrayList<>();
        this.navegadorAlertas = new ArrayDeque<>();
        this.colaMonitoreo = new ArrayDeque<>();
        this.topologiaRed = new HashMap<>();
        this.bufferIngesta = new ArrayDeque<>();
    }

    // =========================================================================
    // SECCIÓN 1: HISTORIAL Y EVENTOS
    // =========================================================================

    /**
     * Registra un nuevo evento anteponiendo la marca temporal actual.
     */
    public void agregarEvento(String evento) {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // El método estaba vacío (TODO 1).
        // Se implementó el formateo de fecha con SimpleDateFormat ("yyyy-MM-dd HH:mm")
        // y se añade la cadena compuesta "[fecha] evento" al final de la lista.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String fecha = sdf.format(new Date());
        historialEventos.add("[" + fecha + "] " + evento);
    }

    /**
     * Retorna los últimos 'limite' eventos del historial.
     */
    public List<String> obtenerEventosRecientes(int limite) {
        // Método ya provisto en la plantilla: retorna copia o sublista segura.
        if (historialEventos.size() <= limite) return new ArrayList<>(historialEventos);
        return historialEventos.subList(historialEventos.size() - limite, historialEventos.size());
    }

    /**
     * Poda el historial para conservar únicamente los últimos 'mantener' eventos.
     */
    public void limpiarHistorialAntiguo(int mantener) {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // El método estaba vacío (TODO 10).
        // Se añadió validación para no procesar números negativos y un bucle 'while'
        // que elimina repetidamente el elemento en el índice 0 hasta alcanzar el tamaño deseado.
        if (mantener < 0) return;
        while (historialEventos.size() > mantener) {
            historialEventos.remove(0);
        }
    }

    // =========================================================================
    // SECCIÓN 2: NAVEGACIÓN DE ALERTAS
    // =========================================================================

    /**
     * Agrega una alerta al final del deque y la establece como la actual.
     */
    public void registrarAlerta(String alerta) {
        navegadorAlertas.addLast(alerta);
        alertaActual = alerta;
    }

    /**
     * Avanza en el carrusel de alertas: extrae la del frente y la reinserta al final.
     */
    public String navegarAlSiguiente() {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Retornaba null por defecto (TODO 2).
        // Se implementó la rotación hacia adelante con pollFirst() + addLast(),
        // actualizando el campo 'alertaActual' y manejando el caso de colección vacía.
        if (navegadorAlertas.isEmpty()) {
            alertaActual = null;
            return null;
        }
        alertaActual = navegadorAlertas.pollFirst();
        navegadorAlertas.addLast(alertaActual);
        return alertaActual;
    }

    /**
     * Retrocede en el carrusel de alertas: extrae la última y la coloca al frente.
     */
    public String navegarAlAnterior() {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Retornaba null por defecto (TODO 3).
        // Se implementó la rotación inversa con pollLast() + addFirst(),
        // sincronizando 'alertaActual' con el nuevo elemento visible.
        if (navegadorAlertas.isEmpty()) {
            alertaActual = null;
            return null;
        }
        alertaActual = navegadorAlertas.pollLast();
        navegadorAlertas.addFirst(alertaActual);
        return alertaActual;
    }

    public String getAlertaActual() {
        return alertaActual;
    }

    // =========================================================================
    // SECCIÓN 3: CICLO DE MONITOREO
    // =========================================================================

    /**
     * Añade un sensor a la cola de consulta si no ha sido programado previamente.
     */
    public void programarConsulta(String idSensor) {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // El método estaba vacío (TODO 4).
        // Se agregó la verificación con contains() para impedir duplicados en la cola
        // y se usa offer() para encolar el elemento de forma segura.
        if (idSensor != null && !colaMonitoreo.contains(idSensor)) {
            colaMonitoreo.offer(idSensor);
        }
    }

    /**
     * Obtiene el siguiente sensor a consultar y lo reencola al final (Round-Robin).
     */
    public String obtenerSiguienteSensor() {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Retornaba null por defecto (TODO 5).
        // Se implementó la rotación FIFO continua: poll() extrae el sensor al frente
        // y offer() lo devuelve al final de la cola para que nunca se pierda el turno.
        if (colaMonitoreo.isEmpty()) return null;
        String siguiente = colaMonitoreo.poll();
        colaMonitoreo.offer(siguiente);
        return siguiente;
    }

    // =========================================================================
    // SECCIÓN 4: TOPOLOGÍA DE RED
    // =========================================================================

    /**
     * Crea un enlace bidireccional no dirigido entre dos estaciones de sensores.
     */
    public void registrarConexion(String sensorA, String sensorB) {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // El método estaba vacío (TODO 6).
        // Se usó computeIfAbsent para inicializar las listas de adyacencia de ambos nodos si no existían,
        // y se validó con contains() antes de añadir para evitar aristas duplicadas en A->B y B->A.
        topologiaRed.computeIfAbsent(sensorA, k -> new ArrayList<>());
        topologiaRed.computeIfAbsent(sensorB, k -> new ArrayList<>());

        if (!topologiaRed.get(sensorA).contains(sensorB)) {
            topologiaRed.get(sensorA).add(sensorB);
        }
        if (!topologiaRed.get(sensorB).contains(sensorA)) {
            topologiaRed.get(sensorB).add(sensorA);
        }
    }

    /**
     * Retorna la lista de sensores conectados al sensor consultado.
     */
    public List<String> obtenerVecinos(String idSensor) {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Retornaba null por defecto (TODO 7).
        // Se sustituyó por getOrDefault(), devolviendo una lista vacía inmutable
        // (Collections.emptyList()) si el sensor no está registrado en el mapa.
        return topologiaRed.getOrDefault(idSensor, Collections.emptyList());
    }

    /**
     * Identifica los sensores registrados que no tienen ninguna conexión (grado 0).
     */
    public List<String> detectarEstacionesAisladas() {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Retornaba null por defecto (TODO 9).
        // Se implementó la iteración sobre entrySet() del mapa, filtrando las claves
        // cuya lista de adyacencia asociada sea nula o esté vacía (isEmpty()).
        List<String> aisladas = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : topologiaRed.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                aisladas.add(entry.getKey());
            }
        }
        return aisladas;
    }

    // =========================================================================
    // SECCIÓN 5: BUFFER DE INGESTA
    // =========================================================================

    /**
     * Recibe una lectura de la red y la pone en la cola de espera.
     */
    public void recibirLectura(LecturaSensor lectura) {
        bufferIngesta.add(lectura);
    }

    /**
     * Extrae y procesa la lectura más antigua en espera (FIFO).
     */
    public LecturaSensor procesarSiguienteLectura() {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Retornaba null por defecto (TODO 8).
        // Se implementó bufferIngesta.poll(), que remueve la cabeza de la cola
        // retornando el elemento o null si está vacía sin lanzar excepciones.
        return bufferIngesta.poll();
    }

    /**
     * Filtra mediante Streams las lecturas del buffer cuyo PM2.5 supere un límite.
     */
    public List<LecturaSensor> filtrarAlertasCriticas(double limitePm25) {
        // [CAMBIO RESPECTO AL ORIGINAL]:
        // Retornaba null por defecto (TODO EXTRA).
        // Se implementó el pipeline funcional con .stream(), .filter(...) y .collect(Collectors.toList()).
        return bufferIngesta.stream()
                .filter(l -> l.getPm25() > limitePm25)
                .collect(Collectors.toList());
    }
}