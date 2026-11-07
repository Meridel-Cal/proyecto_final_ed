# Criterios de Decisión: Estructuras de Datos en la Red de Sensores

Este documento es la parte más importante de tu entrega. No basta con que el código funcione; debes justificar **por qué** elegiste una colección de Java sobre otra.

Responde a las siguientes preguntas basándote en la teoría de nodos y el rendimiento del JCF.

### 1. El dilema del Historial
Para el `historialEventos`, podrías usar un `ArrayList` o un `LinkedList`.
- Si la plataforma registra miles de eventos por segundo y necesitas eliminar los más antiguos frecuentemente para ahorrar memoria, ¿cuál de las dos colecciones es más eficiente y por qué? (Pista: piensa en el costo de eliminar el primer elemento).

**Respuesta:**
Para este escenario específico, `LinkedList` es significativamente más eficiente que `ArrayList`.

**Justificación:**
- **Costo en `ArrayList` ($O(n)$):** Internamente almacena los elementos en un arreglo contiguo en memoria. Al invocar `remove(0)` para descartar el evento más antiguo, la máquina virtual está forzada a desplazar físicamente todos los $n-1$ elementos restantes una posición a la izquierda mediante una copia de memoria en bloque (`System.arraycopy`). Si la colección acumula decenas de miles de eventos y se purga frecuentemente, genera una degradación severa de rendimiento.
- **Costo en `LinkedList` ($O(1)$):** Funciona mediante una cadena de nodos doblemente enlazados en el heap. Eliminar la cabeza (`pollFirst()` o `remove(0)`) solo requiere reasignar la referencia del primer nodo (`first = first.next`) y anular el puntero anterior del nuevo primer nodo. La operación no depende del tamaño de la lista, manteniendo un tiempo constante sin mover ningún otro dato en memoria.

---

### 2. Navegación Bidireccional
En el `navegadorAlertas`, utilizamos un `Deque` (Double Ended Queue).
- ¿Qué ventaja ofrece un `Deque` sobre una `Queue` simple para implementar la funcionalidad de "Atrás" y "Adelante"?
- ¿Cómo se relaciona esto con el concepto de un `NodoDoble`?

**Respuesta:**
- **Ventaja de `Deque` sobre `Queue`:** Una `Queue` estándar sigue una disciplina unidireccional estricta FIFO: solo permite insertar por el extremo final (`add`/`offer`) y extraer por el frente (`poll`/`remove`). Por el contrario, la interfaz `Deque` (Double Ended Queue) expone métodos para insertar y remover en ambos extremos (`pollFirst()`, `addLast()`, `pollLast()`, `addFirst()`). Esto permite simular un carrusel bidireccional continuo en $O(1)$: avanzar removiendo del frente y reinsertando al fondo, y retroceder removiendo del fondo y reinsertando al frente.
- **Relación con `NodoDoble`:** En la teoría de estructuras, un `NodoDoble` posee dos punteros (`siguiente` y `anterior`), lo que habilita el recorrido en dos sentidos sin tener que reiniciar desde el inicio. El `Deque` es la abstracción del Java Collections Framework que provee el comportamiento y las garantías operativas de una estructura doblemente enlazada en ambos extremos en tiempo constante.

---

### 3. El Ciclo de Monitoreo
Para la `colaMonitoreo`, implementaste una rotación (sacar del frente y meter al final).
- ¿Por qué una `Queue` es la estructura natural para este problema?
- ¿Qué pasaría con la equidad del sistema si usaras una `Stack` (Pila) en su lugar?

**Respuesta:**
- **Por qué `Queue` es natural:** Modela directamente una cola de turnos o algoritmo de planificación *Round-Robin* bajo la semántica FIFO (*First-In, First-Out*). El primer sensor que entra al sistema es el primero en ser consultado, y al finalizar su turno, pasa al final de la fila mediante la secuencia `poll()` seguido de `offer()`. Esto garantiza que todos los sensores del ecosistema sean consultados de manera ordenada y periódica.
- **Impacto de usar `Stack` (LIFO):** Rompería la equidad del sistema y causaría inanición (*starvation*). En una pila, las operaciones `pop()` y `push()` ocurren sobre el mismo extremo superior (el tope). Al extraer un sensor para consultarlo y volver a colocarlo inmediatamente, ese mismo sensor quedaría nuevamente en la cima. En consecuencia, el sistema consultaría en bucle infinito al mismo sensor una y otra vez, dejando al resto de las estaciones de la red abandonadas sin monitoreo.

---

### 4. Topología de Red (El Grafo)
Representaste la red de sensores como un `Map<String, List<String>>`.
- ¿Por qué usamos un `Map` y no un arreglo bidimensional (matriz de adyacencia) considerando que la mayoría de los sensores solo están conectados a 2 o 3 vecinos?
- ¿Cómo se traduce esta estructura de `Map` a la idea de un `NodoGrafo` con una lista de adyacencias?

**Respuesta:**
- **Eficiencia espacial en grafos dispersos:** Una matriz de adyacencia requiere un espacio cuadrático estricto $O(V^2)$, donde $V$ es el número total de sensores. Si cada sensor se comunica únicamente con 2 o 3 nodos contiguos, la inmensa mayoría de las celdas de la matriz contendrán ceros o booleanos falsos (grafo disperso o *sparse graph*), desperdiciando memoria innecesariamente. La lista de adyacencia mediante `Map<String, List<String>>` solo consume memoria proporcional a $O(V + E)$ (vértices más aristas reales), almacenando únicamente las conexiones existentes.
- **Traducción conceptual a `NodoGrafo`:**
    - La clave del mapa (`String`) representa el identificador o rótulo del vértice/nodo (`NodoGrafo.id`).
    - El valor asociado (`List<String>`) corresponde a la lista de punteros o referencias a los vecinos conectados (`NodoGrafo.vecinos`). De esta forma, el `Map` funge como la tabla de acceso directo a cada nodo de la red en tiempo promedio $O(1)$.

---

### 5. Buffer de Ingesta y Rendimiento
El `bufferIngesta` procesa datos en orden FIFO.
- Si el volumen de datos aumenta drásticamente y necesitas procesar las lecturas en paralelo usando múltiples hilos, ¿seguiría siendo adecuado un `ArrayDeque` o deberías cambiar a una `ConcurrentLinkedQueue`? Justifica tu respuesta basándote en la seguridad de hilos (Thread-safety).

**Respuesta:**
`ArrayDeque` deja de ser adecuado en un entorno multihilo y es indispensable migrar a una colección concurrente como `ConcurrentLinkedQueue`.

**Justificación:**
- `ArrayDeque` **no es thread-safe**: No cuenta con mecanismos de sincronización interna. Si múltiples hilos productores intentan insertar lecturas (`add`/`offer`) mientras varios hilos consumidores intentan extraerlas (`poll`) en paralelo, las modificaciones concurrentes sobre los punteros internos `head`, `tail` y el arreglo subyacente provocan condiciones de carrera (*race conditions*), inconsistencias de estado, sobrescritura inadvertida de datos o excepciones de tipo `ConcurrentModificationException`.
- `ConcurrentLinkedQueue` **es thread-safe**: Utiliza un algoritmo no bloqueante (*lock-free*) de cola FIFO basado en punteros atómicos y operaciones CAS (*Compare-And-Swap*, según el algoritmo de Michael & Scott). Esto permite que productores y consumidores interactúen con el buffer simultáneamente con alto rendimiento y sin riesgo de corromper la cola ni bloquear el flujo de ingesta.