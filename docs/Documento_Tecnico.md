# Documento Técnico

**Proyecto:** Calculadora de Cálculo con Asistente de IA y Graficador Integral de Funciones
**Curso:** Programación II – Ingeniería en Sistemas
**Lenguaje:** Java 11 o superior (sin Maven, sin Gradle, sin librerías externas)

---

## 1. Objetivo

Construir una aplicación de escritorio que derive, calcule límites (directos, laterales y con L'Hôpital), integre (indefinida, definida e impropiamente) funciones de una o dos variables y resuelva aplicaciones típicas del curso (recta tangente, máximos y mínimos, área entre curvas), mostrando **el procedimiento paso a paso** con la regla aplicada. El estudiante elige el método de una **lista de leyes y reglas** y los pasos de ese método se resaltan. La aplicación grafica la función, sombrea áreas y ofrece un **asistente de IA que explica (pero nunca calcula)** el procedimiento.

## 2. Decisiones de diseño

| Decisión | Motivo |
|---|---|
| JDK estándar, sin dependencias | Se compila con `javac` en cualquier laboratorio; el estudiante ve todo el código. |
| Un solo motor: árbol de expresión | Representa cualquier fórmula (productos, cocientes, funciones compuestas). Antes había un segundo motor "por términos" con otra sintaxis; se eliminó para que la calculadora entienda siempre lo mismo. |
| `java.net.http.HttpClient` + `JsonSencillo` | Se practica HTTP y el parseo manual de JSON (descenso recursivo). |
| Nodos inmutables | Derivar o integrar devuelve un árbol nuevo; evita efectos secundarios. |
| Excepciones comprobadas propias | El compilador obliga a manejar los errores y la GUI muestra mensajes amables. |
| Todo resultado se comprueba | Las antiderivadas se verifican derivándolas y las integrales definidas se comparan con una integración numérica independiente. |
| La IA solo explica | El resultado siempre proviene del motor Java, que es verificable con pruebas. |
| Bucles `for`/`while` explícitos | Estilo didáctico: se evita ocultar la lógica en lambdas o streams. |

## 3. Arquitectura por paquetes

```
calculadora
├── Main                         Punto de entrada (Swing en el EDT)
├── modelo
│   ├── Variable                 Enum X, Y (y auxiliares U, θ para cambios de variable)
│   ├── Graficable               Interfaz común para graficar
│   ├── ResultadoCalculo         Operación, pasos, notas y datos para graficar
│   ├── PasoDetallado            Paso con regla, expresión, fórmula y resultado
│   ├── ReglaCalculo             Catálogo de reglas (para resaltar pasos)
│   ├── MetodoCalculo            Lista de métodos (Derivadas, Integrales, Aplicaciones)
│   ├── TecnicaIntegracion       Técnica exigida (sustitución, partes, ...)
│   ├── EntradaCalculo           Datos escritos por el estudiante
│   └── Calculadora              Fachada del motor
├── simbolico                    Motor simbólico (árbol de expresión)
│   ├── Nodo (+ NodoConstante, NodoVariable, NodoSuma, NodoResta, NodoProducto,
│   │   NodoCociente, NodoPotencia, NodoNegativo, NodoFuncion), TipoFuncion
│   ├── AnalizadorSimbolico      Parser de descenso recursivo, avisos de lectura y límites ±∞
│   ├── Sugeridor                Propone correcciones cuando el texto no se entiende
│   ├── Simplificador            Forma canónica: términos semejantes y factores
│   ├── Embellecedor             Retoque final: cot, tan, sec, sin²+cos² = 1, signos
│   ├── Polinomio                Desarrollo, división y factorización
│   ├── Derivador                Todas las reglas de derivación con pasos
│   ├── Limites                  Límites numéricos (finitos, laterales y en ±∞)
│   ├── Integrador               Tabla, reducción, sustitución, partes, verificación
│   ├── IntegradorRacional       Fracciones parciales
│   ├── IntegradorTrigonometrico Sustitución trigonométrica
│   ├── IntegracionNumerica      Gauss-Kronrod adaptativo (también con límites ±∞)
│   ├── BuscadorRaices           Ceros de una expresión en un intervalo
│   ├── RegistroPasos            Bitácora con deshacer (intentos fallidos)
│   └── MotorSimbolico           Fachada: derivadas, límites, integrales y aplicaciones
├── excepciones                  CalculadoraException y sus 4 subclases
├── util
│   ├── FormatoNumeros           Enteros, fracciones, múltiplos de π y e, raíces, ∞
│   └── JsonSencillo             Lectura y escritura de JSON
├── ia                           ProveedorIA, ProveedorAnthropic, ProveedorCompatibleOpenAI,
│                                ConfiguracionIA, ExplicadorLocal, TutorIA
├── vista
│   ├── VentanaCalculadora       JFrame de tres columnas
│   ├── PanelMetodos             Lista de métodos con fórmula y ejemplo
│   ├── VistaFormula             Dibuja la fórmula escrita (fracciones, exponentes, raíces)
│   ├── PanelGrafico2D           Graficador (áreas, área entre curvas, puntos, implícitas)
│   ├── Historial                Últimos 25 cálculos (se guardan en la carpeta del usuario)
│   ├── ExportadorProcedimiento  Procedimiento en HTML o a la impresora (PDF)
│   ├── RenderizadorHtml         Procedimiento con formato
│   ├── estilo (Tema, Iconos)    Modo oscuro y logotipo
│   └── componentes              BotonModerno, PanelTarjeta, GrupoSegmentado,
│                                CampoModerno, Encabezado, BarraDesplazamientoModerna
└── pruebas
    └── PruebasCalculadora       271 pruebas sin JUnit
```

Los diagramas UML están en `docs/uml/` (PlantUML: `diagrama_clases.puml`, `secuencia_integral_definida.puml`; Mermaid: `diagramas_mermaid.md`).

### Patrones de diseño aplicados

- **Fachada:** `Calculadora` (y por dentro `MotorSimbolico`) es la única puerta de entrada al motor para la GUI y las pruebas.
- **Composite:** el árbol de expresión; cada `Nodo` sabe evaluarse, compararse y reemplazar subárboles.
- **Estrategia / Polimorfismo:** `ProveedorIA` permite cambiar de proveedor sin modificar `TutorIA`.
- **Fábrica sencilla:** `TutorIA.crearProveedor()` elige el proveedor según la configuración.

## 4. Analizador de expresiones

Parser de **descenso recursivo** (`AnalizadorSimbolico`) que avanza carácter por carácter:

```
expresion ::= termino { ('+' | '-') termino }
termino   ::= unario { ('*' | '/' | multiplicación implícita) unario }
unario    ::= ('+' | '-') unario | potencia
potencia  ::= primario { '!' } [ '^' unario ]          (asociativa a la derecha)
primario  ::= numero | x | y | pi | e | funcion [ '^' n ] ( '(' expresion ')' | argumento simple )
            | '√' primario | '(' expresion ')' | '|' expresion '|'
funcion   ::= sin | sen | cos | tan | tg | sec | csc | cot | ln | log | exp | sqrt | raiz
            | arcsin | arcsen | arccos | arctan | arctg | abs | sinh | senh | cosh | tanh
```

- **Funciones sin paréntesis:** `sinx`, `sin x`, `sin 2x`, `ln x`. El argumento simple es un número opcional seguido de x, y, pi o e (con su potencia): termina en un operador, un espacio o el nombre de otra función (`sinxcosx` = sin(x)·cos(x)).
- Multiplicación implícita: `2x`, `3sin(x)`, `x^2y`, `(x+1)(x-1)`.
- Factorial de enteros (`5!`), `x**2` como `x^2`, y se ignora un prefijo `f(x) =` o `y =`.
- Normaliza `·`, `×`, `−`, `÷`, `π`, `²`, `³` y la coma decimal.
- **Avisos de lectura ambigua** (`getAvisos()`): `e^2x` se lee e²·x, `1/2x` se lee x/2, `√2x` se lee √2·x y `cos x^2` se lee cos(x²). El cálculo continúa, pero el aviso aparece en la vista previa y como nota del procedimiento, con la forma correcta de escribir la otra lectura.
- Los errores indican la posición del carácter (`ExpresionInvalidaException`). Si el texto no se entiende, `Sugeridor` prueba arreglos comunes (palabras en español, nombres mal escritos con distancia de Levenshtein, letras desconocidas, paréntesis sin cerrar, operador final) y propone el primero que sí se puede leer.

## 5. Motor simbólico (árbol de expresión)

**Simplificador (forma canónica).** Los productos se aplanan en *coeficiente × Π base^exponente* (así x·x² = x³, x/x = 1, e^x·e^(2x) = e^(3x)); las sumas se aplanan en términos y se combinan los semejantes. También se extrae el factor común de una suma ((2x + 2)/4 = (x + 1)/2) y se evalúan funciones con valor exacto (arctan(1) = π/4). Al final, `Embellecedor` escribe el resultado como en un libro (cos/sin → cot, 1/cos² → sec², sin² + cos² → 1, −(a − b) → b − a); solo acepta el cambio si el valor no cambia.

**Derivador.** Recursivo, una regla por tipo de nodo: constante, identidad, variable tratada como constante, múltiplo constante, suma/resta, potencia (con cadena), producto, cociente, exponencial (e^u, a^u), logarítmica (ln, log, ln|u|), trigonométricas, inversas, hiperbólicas (sinh, cosh, tanh) y derivación logarítmica para f^g. Cada aplicación queda como `PasoDetallado` con profundidad (los pasos de los factores aparecen anidados).

**Derivación implícita.** Para F(x, y) = 0 se derivan ambos lados con y = y(x), se agrupan los términos con y' y se despeja: dy/dx = −F_x/F_y.

**Límites y L'Hôpital.** `Limites` evalúa la función en puntos cada vez más cercanos (o en ±10^k) y clasifica: finito, +∞, −∞ o no existe (detecta funciones definidas solo de un lado, como ln x). Admite límites laterales escribiendo el punto con `+` o `-` al final (`0+`, `0-`). L'Hôpital se repite mientras la forma sea 0/0 o ∞/∞; la forma 0·∞ se reescribe como cociente.

**Integrador.** Orden automático: constante → suma/resta → múltiplo constante → tabla inmediata con argumento lineal (incluye hiperbólicas y |ax + b|) → identidades trigonométricas → **fórmula de reducción** para sinⁿ y cosⁿ → **producto a suma** para sin(ax)·cos(bx) → desarrollo de polinomios → **sustitución** (se prueba cada subexpresión u, se divide entre u' y se comprueba que no quede x) → **fracciones parciales** (división, factorización con raíz racional y Durand–Kerner, sistema lineal con Gauss) → **sustitución u = e^(kx)** para funciones racionales de e^x → distribución → **sustitución trigonométrica** → **partes** (LIATE, con integrales cíclicas I = A + k·I). Los intentos fallidos se deshacen en la bitácora. **Todo resultado se verifica** derivándolo numéricamente; si no coincide, se rechaza.

**Integrales definidas.** TFC con propiedades (límites iguales, inversión, aditividad), respecto a x o a y. Si hay límites infinitos o puntos donde la función se va a infinito, se trata como **impropia**: se divide el intervalo y cada extremo se evalúa con límites laterales. Se distingue entre **diverge** (algún límite es infinito) y **no existe** (F oscila, como en ∫₀^∞ sin x dx).

- **Forma exacta:** los límites se sustituyen como árbol (π/2, e, ln 2) para mostrar, por ejemplo, `e − 1 ≈ 1.718282` en lugar de solo el decimal.
- **Otra variable:** si la función contiene la otra variable, el resultado es una expresión en ella (∫₀² x·y dx = 2y).
- **Comprobación numérica:** cada resultado finito se compara con `IntegracionNumerica`; si no coinciden se agrega una nota de advertencia.
- **Sin antiderivada elemental** (e^(−x²), sin(x)/x, x^x): en el modo automático la integral definida se aproxima con **Gauss-Kronrod 7/15 adaptativo**. Los nodos nunca tocan los extremos (sirve con singularidades en a o b) y los límites infinitos se transforman a un intervalo finito (x = t/(1 − t²) o x = a + t/(1 − t)). El resultado se marca como aproximado.

**Aplicaciones.**

- *Recta tangente y normal:* y = f(a) + f'(a)(x − a); la normal tiene pendiente −1/f'(a) (o es vertical si la tangente es horizontal).
- *Máximos, mínimos e inflexión:* `BuscadorRaices` encuentra los ceros de f' en [a, b] (exactos si f' es polinomio; si no, recorrido con bisección y búsqueda ternaria para raíces dobles) y los puntos donde f' no existe. Se clasifican con f'' (o con el signo de f' si f'' = 0). Los puntos de inflexión son los cambios de signo de f''. Los extremos absolutos se obtienen comparando f en los puntos críticos y en a y b.
- *Área entre curvas:* A = ∫|f − g|. Se buscan los cruces dentro de [a, b], se integra cada tramo y se suman los valores absolutos. Si a y b se dejan vacíos se usan los puntos de corte (para curvas periódicas, la región entre dos cortes seguidos desde x = 0).

**Resaltado del método.** Cada método de la lista declara las reglas que le corresponden (`MetodoCalculo.getReglas()`); los pasos con esas reglas se marcan como destacados y, si ninguna se usó, se agrega una nota.

**Formato de números.** `FormatoNumeros.formatearExacto()` reconoce enteros, fracciones (denominador ≤ 100), múltiplos de π, raíces cuadradas (√3/3, 2√2), múltiplos de e y n/e; si no, muestra un decimal de 6 cifras.

## 6. Asistente de IA

- **Configuración:** archivo `config.properties` (copia de `config.properties.ejemplo`, excluido de Git) o la variable de entorno `CALCULADORA_IA_API_KEY`. Las constantes de `ConfiguracionIA` sirven solo para proveedores sin clave, como Ollama; **nunca** se escribe una clave de API en el código.
- **Proveedores:** `anthropic` (API de Mensajes, encabezados `x-api-key` y `anthropic-version`), `openai` y `ollama` (Chat Completions, `Authorization: Bearer`).
- **Instrucción de sistema:** prohíbe calcular, exige texto plano sin Markdown ni LaTeX y máximo 250 palabras.
- **Mensaje:** contiene el informe de pasos de `ResultadoCalculo` y la pregunta opcional.
- **Post-proceso:** `TutorIA.limpiarFormato()` quita marcas de Markdown/LaTeX y `limitarPalabras()` recorta a 250 palabras.
- **Respaldo:** si no hay proveedor o se lanza `IAException` (sin red, 401, 429, tiempo agotado, JSON inválido), se usa `ExplicadorLocal`, que resume los pasos dentro del presupuesto de 250 palabras.
- **Concurrencia:** la GUI llama al tutor dentro de un `SwingWorker` para no congelar la ventana.

## 7. Interfaz gráfica

Diseño visual propio, dibujado con `Graphics2D` (sin librerías externas), para que se vea igual en cualquier equipo:

| Clase | Función |
|---|---|
| `vista.estilo.Tema` | Paleta en modo oscuro (cian/turquesa), tipografías (Segoe UI y Consolas en Windows) y suavizado. |
| `vista.estilo.Iconos` | Logotipo e íconos de la ventana generados por código. |
| `vista.componentes.*` | Botones, tarjetas, selector segmentado, campos, encabezado y barras de desplazamiento. |
| `vista.RenderizadorHtml` | Convierte los pasos y la explicación en HTML con formato. |
| `vista.PanelMetodos` | Lista de métodos (Derivadas / Integrales / Aplicaciones) con fórmula, descripción y ejemplo. |
| `vista.VistaFormula` | Vista previa de la fórmula con cajas tipográficas: fracciones con raya, exponentes elevados, raíces y paréntesis que crecen. |
| `vista.Historial` | Últimos 25 cálculos en `%USERPROFILE%\.calculadora_calculo_historial.txt`. |
| `vista.ExportadorProcedimiento` | Guarda el procedimiento como página HTML clara o lo imprime («Microsoft Print to PDF» genera un PDF). |

`VentanaCalculadora` (tres columnas):

- `crearEncabezado()`: título, insignia de la IA y Ayuda.
- `PanelMetodos`: lista de métodos; al cambiar de método `actualizarCamposSegunMetodo()` muestra solo los campos necesarios (variable, orden, punto, tipo de integral, a, b, c, g(x)).
- `crearTarjetaEntrada()`: debajo de la función, la **vista previa** se actualiza mientras se escribe (con un `Timer` de 220 ms): dibuja la fórmula tal como se entendió, muestra los avisos de lectura y, si hay un error, la sugerencia con el botón «Usar sugerencia».
- `crearTarjetaTeclado()`: las teclas no toman el foco y escriben en el último campo con cursor (`campoActivo`), incluidos los límites (∞ y −∞) y el valor absoluto.
- `crearTarjetaResultado()`: resultado destacado con los botones **Historial**, **Exportar** y **Copiar**; los errores se muestran ahí en rojo.
- `crearZonaSecciones()`: **Procedimiento**, **Gráfica 2D** y **Asistente IA** con `CardLayout`.

`PanelGrafico2D`: antialiasing, cuadrícula con paso "1-2-5", curva principal, curva secundaria discontinua (derivada, antiderivada, tangente o g(x)), área en `[a, b]` hasta el eje o **entre dos curvas**, puntos destacados con etiqueta (máximos, mínimos, inflexión, tangencia), zoom con rueda, arrastre y doble clic para restablecer. Dibuja curvas implícitas F(x, y) = 0 con *marching squares*.

## 8. Pruebas

`calculadora.pruebas.PruebasCalculadora` (271 casos, sin JUnit). Las derivadas y las integrales no se comprueban comparando texto, sino **numéricamente**:

- cada derivada se compara con el cociente (f(x + h) − f(x − h))/2h en varios puntos;
- cada antiderivada se deriva numéricamente y debe coincidir con el integrando;
- las integrales definidas se comparan con valores conocidos (incluidas las aproximaciones numéricas de e^(−x²) y sin(x)/x).

Además: formato de números, analizador (sin paréntesis, español, avisos y sugerencias), aplicaciones (límites laterales, tangente, extremos, área entre curvas), manejo de errores, JSON, módulo de IA (incluida la conmutación al explicador local) y una prueba que resuelve el ejemplo de **cada método de la lista**. Termina con código 1 si alguna falla.

```
.\ejecutar_pruebas.bat
...
Pruebas exitosas: 271
Pruebas fallidas: 0
RESULTADO: TODAS LAS PRUEBAS PASARON
```

## 9. Compilación

`build.bat` usa `javac --release 11 -sourcepath src` partiendo de `Main` y `PruebasCalculadora` (compila todo el proyecto) y empaqueta con `jar --main-class calculadora.Main` en `dist/CalculadoraCalculo.jar`.

## 10. Historial de etapas (Git)

| Etapa | Contenido |
|---|---|
| 0 | Estructura, `.gitignore`, repositorio |
| 1 | Modelo, excepciones, `FormatoNumeros`, pruebas base |
| 2 | Analizador de expresiones y fachada `Calculadora` |
| 3 | Seno, coseno, exponencial y logaritmo |
| 4 | Módulo de IA, `JsonSencillo`, `ExplicadorLocal` |
| 5 | GUI Swing, `PanelGrafico2D`, `SwingWorker` |
| 6 | Scripts `.bat` |
| 7 | Documentación |
| 8 | Motor simbólico, lista de métodos y modo oscuro |
| 9 | Lectura flexible (sinx, español, avisos y sugerencias), vista previa, integración numérica, integrales respecto a y, aplicaciones (tangente, extremos, área entre curvas, límites laterales), historial, exportación y eliminación del motor por términos |

## 11. Limitaciones y trabajo futuro

- Productos generales de potencias trigonométricas (sin³x·cos²x) y fracciones parciales con factores cuadráticos repetidos.
- Formas indeterminadas ∞ − ∞, 1^∞ y 0^0 en L'Hôpital (el límite numérico sí las resuelve de forma aproximada).
- Integrales impropias cuando la función contiene la otra variable.
- Gráficas 3D de funciones f(x, y).
