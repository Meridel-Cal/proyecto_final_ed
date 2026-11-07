# Decisiones de Diseño — Plataforma de Monitoreo Ambiental Urbano

Este documento consolida y justifica las decisiones arquitectónicas, algorítmicas y de estructuras de datos adoptadas a lo largo de las siete semanas de desarrollo del proyecto integrador.

## 1. Arquitectura General y Punto de Entrada

- Punto de entrada único: El sistema mantiene como único punto de ejecución de la aplicación IngestaSensores.main(). Las nuevas capacidades agregadas en cada semana amplían la plataforma en capas sucesivas, sin crear aplicaciones independientes.
- Aislamiento por contrato (Interfaz Repositorio): La lógica de negocio e ingesta interactúa exclusivamente contra la interfaz Repositorio. Esto permite intercambiar la capa de persistencia interna (arreglos dinámicos, listas enlazadas manuales o colecciones estándar del JCF) sin modificar el flujo de procesamiento.
- Instrumentación y métricas: Para evaluar el costo algorítmico real, la comparación entre soluciones prioriza el conteo exacto de operaciones elementales (comparaciones, intercambios, desplazamientos y saltos entre nodos), manteniendo el tiempo en milisegundos como evidencia experimental complementaria.

## 2. Ingesta, Validación y Modelo (Semanas 1 y 2)

- Inmutabilidad y encapsulación: La clase LecturaSensor encapsula las cinco variables de medición en un único objeto inmutable con métodos de validación de rangos físicos (esValida()), evitando el paso de variables dispersas y filtrando datos corruptos antes de almacenarlos.
- Comparación de cadenas por contenido: Los identificadores de estación y marcas temporales se evalúan estrictamente mediante equals() o compareTo() para garantizar la comparación por contenido y no por identidad de referencia en memoria (==).
- Arreglo dinámico (RepositorioArreglo / RepositorioLecturas):
    - Se implementó una estrategia de duplicación de capacidad (2x) amortizando el costo de inserción al final a O(1).
    - En las eliminaciones se garantiza la anulación de la última posición (lecturas[cantidad - 1] = null) para prevenir fugas de memoria y referencias fantasmas.

## 3. Búsqueda y Análisis Matricial (Semana 3)

- Precondición de Búsqueda Binaria por Timestamp: Los datos sintéticos se generan de forma estrictamente cronológica ascendente, cumpliendo la precondición de ordenamiento que reduce las comparaciones de búsqueda de O(n) a O(log n).
- Búsqueda por PM2.5: Se preserva la búsqueda binaria sobre arreglos no ordenados por PM2.5 como experimento para evidenciar las fallas cuando no se respeta la precondición del algoritmo.
- Manejo de vacíos en matriz (AnalizadorMatriz): Para calcular estadísticas horarias por estación sin sesgar los promedios con ceros de lecturas inexistentes, se introdujo una matriz booleana paralela hayDato[estacion][hora] que computa únicamente las estaciones que efectivamente reportaron.

## 4. Algoritmos de Ordenamiento (Semana 4)

- Corte temprano en Bubble-Sort: Se agregó una bandera booleana (swapped) que detiene el algoritmo si en una pasada no se producen intercambios, reduciendo el costo en arreglos ya ordenados de O(n^2) a O(n) (n - 1 comparaciones).
- QuickSort con pivote aleatorio: QuickSort con selección de pivote fijo (primer elemento) degrada a O(n^2) y provoca StackOverflowError cuando procesa datos que ya vienen ordenados cronológicamente desde la red. Se implementó la selección de pivote aleatorio con intercambio previo para garantizar una partición balanceada y un tiempo promedio de O(n log n).
- HeapSort y MergeSort: Se incorporaron como alternativas eficientes para ordenar grandes volúmenes de datos con costo garantizado O(n log n) en el peor caso.

## 5. Listas Enlazadas y Navegación (Semana 5)

- Puntero ultimo en Lista Simple (RepositorioEnlazado): Se introdujo una referencia directa al nodo de cola para permitir inserciones al final (agregarAlFinal) en tiempo constante O(1), evitando el recorrido lineal O(n) de toda la cadena.
- Navegación bidireccional (HistorialNavegable): Para soportar la funcionalidad de cursor adelante/atrás en la consola del operador, se implementó una lista doblemente enlazada (NodoDoble) donde cada nodo mantiene punteros hacia siguiente y anterior, garantizando navegación en O(1).

## 6. Control de Flujo: Pilas, Colas y Montículos (Semana 6)

- Pila de Acciones (PilaDeAcciones): Estructura LIFO utilizada para registrar las operaciones del operador y permitir la reversión de comandos (Deshacer / Ctrl+Z).
- Cola Circular de Ingesta (ColaDeIngesta): Implementada como un buffer circular con aritmética modular ((fin + 1) % CAPACIDAD) sobre un arreglo fijo. Esto recicla espacio continuamente y evita que la cola se declare llena falsamente cuando se procesa al ritmo que se ingesta.
- Cola de Prioridad con Montículo Binario (ColaDeAlertas):
    - Aritmética de índices: Se implementó un Heap sobre un arreglo (padre = (i - 1) / 2, hijos = 2i + 1, 2i + 2), eliminando la sobrecarga de memoria de nodos y referencias.
    - Desempate justo: Cuando dos alertas tienen el mismo nivel de severidad, el criterio de desempate prioriza el menor ordenDeLlegada, asegurando equidad FIFO dentro de una misma prioridad.

## 7. Java Collections Framework (JCF) y Gestor de Red (Semana 7)

- Programación contra interfaces: Toda la capa de integración de la Semana 7 (GestorDeRed, RepositorioColeccion) programa contra las interfaces estándar (List, Deque, Queue, Map) en lugar de acoplarse a implementaciones concretas.
- Elección de estructuras en GestorDeRed:
    - historialEventos (ArrayList): Optimizado para agregar al final en O(1) amortizado y lecturas por rango o sublistas.
    - navegadorAlertas (ArrayDeque): Proporciona un carrusel bidireccional eficiente con inserciones y extracciones en ambos extremos (pollFirst, addLast, pollLast, addFirst) en O(1).
    - colaMonitoreo (Queue / ArrayDeque): Modelo FIFO estricto con rotación continua (poll() y re-encolado con offer()), evitando duplicados con contains().
    - topologiaRed (Map<String, List<String>> / HashMap): Representación de grafo disperso mediante listas de adyacencia. Ocupa memoria proporcional a O(V + E) frente al costo cuadrático O(V^2) de una matriz de adyacencia.
    - bufferIngesta (Queue<LecturaSensor>): Buffer seguro de extracción FIFO mediante poll() y filtrado funcional con Streams.