# Bitacora individual - Semana 07

## 1. Datos de la actividad

- **Estudiante:** Meridel Calderon
- **Equipo:** Plataforma de Monitoreo Ambiental Urbano
- **Semana:** 07
- **Fecha del laboratorio:** 2026-11-5
- **Fecha del taller:** 2026-11-17
- **Tema principal:** Java Collections Framework (JCF) e Integración del Gestor de Red IoT
- **Pregunta de la semana:** ¿Cómo seleccionar y justificar técnicamente las estructuras de datos de la biblioteca estándar (JCF) frente a implementaciones manuales basadas en nodos para garantizar equidad, eficiencia y escalabilidad en un sistema IoT?

## 2. Prediccion antes de ejecutar

1. **Que creo que va a ocurrir?**
   Espero que al implementar `GestorDeRed` utilizando `ArrayList`, `ArrayDeque` y `HashMap`, los tests unitarios de `TestGestorDeRed` pasen exitosamente, demostrando que la rotación en la cola de turnos es justa (Round-Robin) y que la navegación en el Deque funciona de forma bidireccional en tiempo constante O(1).

2. **Que parte del programa o del algoritmo puede fallar?**
   Sospecho que pueden fallar:
   - La navegación en `navegadorAlertas` si no se sincroniza adecuadamente `alertaActual` al rotar con `pollFirst()`/`addLast()`.
   - La programación de turnos si se permiten IDs de sensores duplicados en la cola de monitoreo.
   - El formateo de la fecha o errores de sintaxis/llaves al completar los métodos vacíos.

3. **Como comprobare mi prediccion?**
   Ejecutando la suite de pruebas unitarias `TestGestorDeRed` mediante JUnit y verificando que los 7 casos de prueba pasen en verde, especialmente `testCicloMonitoreo_DebeRotarSensores` y `testNavegacionAlertas_DebeMoverseAdelanteYAtraves`.

## 3. Evidencia del laboratorio

### Resultado observado
Al ejecutar `TestGestorDeRed`, todas las aserciones pasaron satisfactoriamente (7/7 tests en verde):
- El historial registra marcas de tiempo `[yyyy-MM-dd HH:mm]` y permite podas correctas.
- El carrusel de alertas navega hacia adelante y hacia atrás sin lanzar excepciones ni perder referencias.
- El ciclo de turnos devuelve `S1 -> S2 -> S1` garantizando equidad.
- El grafo bidireccional registra correctamente las conexiones mutuas (`S1 <-> S2`) y detecta estaciones aisladas.
- El buffer extrae lecturas en orden FIFO estricto.

### Diferencia entre la prediccion y el resultado
Inicialmente ocurrió un error de compilación (`illegal start of type`, `reached end of file while parsing`) debido a un desbalance de llaves `{}` al reemplazar los bloques `TODO`. Una vez estructurada la clase con todas sus llaves cerradas y sus métodos completos, el comportamiento coincidió exactamente con la predicción teórica.

### Error o comportamiento inesperado
- **Que ocurrio?** Error de análisis del compilador (`parsing error` en línea 41 y 207) durante el build en el IDE.
- **Por que ocurrio?** Al pegar los cambios dentro de `GestorDeRed.java`, quedaron llaves sin cerrar en uno de los métodos superiores, lo que causó que el compilador interpretara las declaraciones de métodos subsiguientes como código fuera de contexto.
- **Como lo corregimos o que falta corregir?** Se reescribió la clase completa asegurando el cierre formal de cada método y de la clase externa, verificando la compilación limpia previa a la ejecución de las pruebas.

## 4. Explicacion en lenguaje llano

Imagina una oficina de correos inteligente: hay un libro donde se anota cada paquete que entra con su hora exacta; una fila donde las cartas se atienden en el orden en que llegaron sin que nadie se cuele; una pantalla donde puedes pasar noticias hacia adelante y hacia atrás como fotos en un celular; y un mapa que muestra qué ciudades están unidas por carreteras para saber si alguna quedó aislada. Java ya nos da cajas construidas y probadas para hacer todo esto sin tener que inventar los tornillos desde cero.

### Ejemplo o analogia
Es como un carrusel de maletas en el aeropuerto (el `Deque` de alertas): las maletas dan vueltas en círculo. Si quieres ver la siguiente maleta, la tomas del frente y la vuelves a colocar al final para no perderla; si quieres retroceder, tomas la de atrás y la pasas al frente. La analogía deja de ser exacta porque en la computadora este movimiento no gasta energía física ni mueve todas las demás maletas, solo cambia los punteros de los extremos.

## 5. El vacio que encontre

- **Mi duda concreta es:** ¿Por qué un `ArrayList` es lento para borrar el elemento más antiguo si es tan rápido para leer cualquier posición?
- **Lo que ya puedo explicar es:** Que `ArrayList` permite acceso inmediato O(1) con un índice porque calcula la dirección de memoria multiplicando la posición por el tamaño del dato.
- **Para resolver la duda consulte:** El documento de la guía `Explicacion_Estructuras_JCF.html` y la sección de costo Big-O.
- **Ahora lo entiendo asi:** Como los datos en un `ArrayList` están pegados uno al lado del otro en memoria como vagones de tren, si sacas el primero, estás obligado a empujar todos los demás vagones hacia adelante uno por uno (costo O(n)). En cambio, en una lista enlazada solo desenganchas el primer vagón (costo O(1)).

## 6. Trazado de la solucion

Caso: Ejecución del ciclo de monitoreo rotativo (`programarConsulta` y `obtenerSiguienteSensor`).

| Paso | Estado de los datos o estructura | Decision o resultado |
|---|---|---|
| 1 | `colaMonitoreo = []` (vacía) | Se programa `S1`. `contains("S1")` es false -> se encola `S1`. Cola: `[S1]` |
| 2 | `colaMonitoreo = [S1]` | Se programa `S2`. `contains("S2")` es false -> se encola `S2`. Cola: `[S1, S2]` |
| 3 | `colaMonitoreo = [S1, S2]` | Intento duplicado `S1`. `contains("S1")` es true -> se rechaza. Cola: `[S1, S2]` |
| 4 | Consulta 1: `poll()` -> extrae `S1` | Se devuelve `S1` y se reencola con `offer("S1")`. Cola: `[S2, S1]` |
| 5 | Consulta 2: `poll()` -> extrae `S2` | Se devuelve `S2` y se reencola con `offer("S2")`. Cola: `[S1, S2]` |
| 6 | Consulta 3: `poll()` -> extrae `S1` | Se devuelve `S1` de nuevo. El ciclo se repite equitativamente. |

## 7. Decision de diseño

- **Problema que debiamos resolver:** Representar la topología física de conexiones entre las estaciones de la red urbana, donde cada estación solo tiene 2 o 3 vecinas.
- **Estructura, algoritmo o estrategia elegida:** Grafo representado como Lista de Adyacencia usando `Map<String, List<String>>` (`HashMap`).
- **Alternativa descartada:** Matriz de adyacencia bidimensional `boolean[V][V]`.
- **Por que elegimos la primera:** En una red con pocos enlaces (grafo disperso), una matriz gasta memoria cuadrática O(V²) llena de ceros. El `HashMap` con listas usa solo O(V + E), almacenando únicamente las conexiones reales.
- **Que evidencia respalda la decision:** La prueba `testTopologia_DebeRegistrarConexiones` y la consulta directa de vecinos en tiempo O(1) promedio mediante `topologiaRed.getOrDefault()`.

## 8. Aporte al proyecto

- **Archivo(s) o modulo(s) trabajado(s):**
   - `src/structures/management/GestorDeRed.java`
   - `docs/Preguntas_Criterio.md`
   - `docs/decisiones.md`
- **Cambio realizado:** Implementación completa de los 10 TODOs de gestión de red (historial con timestamp, carrusel bidireccional con `Deque`, turnos FIFO con `Queue`, grafo de adyacencia con `Map`, poda de memoria y filtrado funcional con Streams) y documentación de criterios técnicos.
- **Como se conecta con la capa anterior:** Integra el modelo `LecturaSensor` (Semana 1) con los conceptos de listas enlazadas, colas y montículos (Semanas 5 y 6), delegando el almacenamiento en las colecciones estándar y certificadas del Java Collections Framework.
- **Que queda pendiente para la siguiente semana:** Preparar la sustentación final y empaquetar el entregable bajo la etiqueta Git requerida (`hito-final` o `hito-S07`).

## 9. Commits realizados

| Commit | Mensaje | Que demuestra |
|---|---|---|
| `[hash-1]` | `feat(gestor): completar retos 1-10 y filtro de alertas en GestorDeRed` | Implementación de las estructuras JCF en `GestorDeRed.java` y paso de tests unitarios |
| `[hash-2]` | `docs(decisiones): consolidar decisiones de diseno del proyecto (S01-S07)` | Actualización arquitectónica del documento `decisiones.md` |
| `[hash-3]` | `docs(criterio): responder preguntas de seleccion y analisis de estructuras JCF` | Justificación teórica de complejidades Big-O y selección de colecciones |

## 10. Reexplicacion final

Programar contra las interfaces del Java Collections Framework (`List`, `Deque`, `Queue`, `Map`) desacopla la lógica del sistema del almacenamiento físico. La selección de cada estructura no es arbitraria sino dependiente del patrón de acceso: `ArrayList` para secuencias leídas por índice, `ArrayDeque` para extremos dinámicos O(1) sin sobrecarga de nodos, y `HashMap` para grafos dispersos con acceso por clave O(1).

## 11. Reflexion individual

1. **Lo que ahora puedo hacer y antes no podia:** Puedo justificar la elección de una colección en Java según su complejidad Big-O y su comportamiento de memoria en lugar de elegirla por comodidad.
2. **El error o supuesto que mas me enseno:** Asumir que `ArrayList` servía para cualquier lista; comprobar que eliminar al inicio tiene costo O(n) me enseñó cuándo preferir `LinkedList` o `Deque`.
3. **La pregunta que llevaria a la proxima clase:** ¿Bajo qué volumen de peticiones concurrentes el costo de sincronización de `ConcurrentLinkedQueue` supera el rendimiento de una cola simple protegida con bloqueos?
4. **Que parte del trabajo fue realmente mia:** La resolución lógica de los retos en `GestorDeRed.java`, la depuración de errores de compilación y la redacción de las justificaciones de ingeniería en los documentos de criterios y bitácora.

## Lista de verificacion antes de entregar

- [x] Escribi la prediccion antes de consultar el resultado.
- [x] Inclui evidencia concreta del laboratorio.
- [x] Explique un concepto sin depender de jerga.
- [x] Registre un vacio, una duda o un error real.
- [x] Trace al menos un caso paso a paso.
- [x] Justifique una decision del proyecto y una alternativa descartada.
- [x] Registre mis commits y mi aporte individual.
- [x] Deje claro que queda pendiente.
- [x] Renombre el archivo con el formato `sXX-nombre.md`.