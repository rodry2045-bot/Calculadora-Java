# Documento Técnico

**Proyecto:** Calculadora de Cálculo con Asistente de IA y Graficador Integral de Funciones
**Curso:** Programación II – Ingeniería en Sistemas
**Lenguaje:** Java 11 o superior (sin Maven, sin Gradle, sin librerías externas)

---

## 1. Objetivo

Construir una aplicación de escritorio que derive, calcule límites con L'Hôpital, integre (indefinida, definida e impropiamente) funciones de una o dos variables, mostrando **el procedimiento paso a paso** con la regla aplicada. El estudiante elige el método de una **lista de leyes y reglas** y los pasos de ese método se resaltan. La aplicación grafica la función, sombrea el área de las integrales definidas y ofrece un **asistente de IA que explica (pero nunca calcula)** el procedimiento.

## 2. Decisiones de diseño

| Decisión | Motivo |
|---|---|
| JDK estándar, sin dependencias | Se compila con `javac` en cualquier laboratorio; el estudiante ve todo el código. |
| `java.net.http.HttpClient` + `JsonSencillo` | Se practica HTTP y el parseo manual de JSON (descenso recursivo). |
| Términos inmutables | Derivar o integrar devuelve un objeto nuevo; evita efectos secundarios. |
| Excepciones comprobadas propias | El compilador obliga a manejar los errores y la GUI muestra mensajes amables. |
| La IA solo explica | El resultado siempre proviene del motor Java, que es verificable con pruebas. |
| Bucles `for`/`while` explícitos | Estilo didáctico: se evita ocultar la lógica en lambdas o streams. |

## 3. Arquitectura por paquetes

```
calculadora
├── Main                         Punto de entrada (Swing en el EDT)
├── modelo                       Modelo y motor por términos
│   ├── Variable                 Enum X, Y (y auxiliares U, θ)
│   ├── Termino, TerminoPolinomico, TerminoFuncional, TerminoSeno, TerminoCoseno,
│   │   TerminoExponencial, TerminoLogaritmico, Expresion   (motor por términos)
│   ├── Graficable               Interfaz común para graficar
│   ├── ResultadoCalculo         Operación, pasos, notas y datos para graficar
│   ├── PasoDetallado            Paso con regla, expresión, fórmula y resultado
│   ├── ReglaCalculo             Catálogo de reglas (para resaltar pasos)
│   ├── MetodoCalculo            Lista de métodos de la interfaz
│   ├── TecnicaIntegracion       Técnica exigida (sustitución, partes, ...)
│   ├── EntradaCalculo           Datos escritos por el estudiante
│   └── Calculadora              Fachada de ambos motores
├── simbolico                    Motor simbólico (árbol de expresión)
│   ├── Nodo (+ NodoConstante, NodoVariable, NodoSuma, NodoResta, NodoProducto,
│   │   NodoCociente, NodoPotencia, NodoNegativo, NodoFuncion), TipoFuncion
│   ├── AnalizadorSimbolico      Parser de descenso recursivo (y límites ±∞)
│   ├── Simplificador            Forma canónica: términos semejantes y factores
│   ├── Polinomio                Desarrollo, división y factorización
│   ├── Derivador                Todas las reglas de derivación con pasos
│   ├── Limites                  Límites numéricos (finitos y en ±∞)
│   ├── Integrador               Tabla, sustitución, partes, verificación
│   ├── IntegradorRacional       Fracciones parciales
│   ├── IntegradorTrigonometrico Sustitución trigonométrica
│   ├── RegistroPasos            Bitácora con deshacer (intentos fallidos)
│   └── MotorSimbolico           Fachada: derivadas, implícita, L'Hôpital, integrales
├── analisis
│   └── AnalizadorExpresion      Parser del motor por términos
├── excepciones                  CalculadoraException y sus 4 subclases
├── util
│   ├── FormatoNumeros           Enteros, fracciones, múltiplos de π, ∞
│   └── JsonSencillo             Lectura y escritura de JSON
├── ia                           ProveedorIA, ProveedorAnthropic, ProveedorCompatibleOpenAI,
│                                ConfiguracionIA (se edita en el código), ExplicadorLocal, TutorIA
├── vista
│   ├── VentanaCalculadora       JFrame de tres columnas (un método por zona)
│   ├── PanelMetodos             Lista de métodos con fórmula y ejemplo
│   ├── PanelGrafico2D           Graficador (incluye curvas implícitas)
│   ├── RenderizadorHtml         Procedimiento con formato
│   ├── estilo (Tema, Iconos)    Modo oscuro y logotipo
│   └── componentes              BotonModerno, PanelTarjeta, GrupoSegmentado,
│                                CampoModerno, Encabezado, BarraDesplazamientoModerna
└── pruebas
    └── PruebasCalculadora       171 pruebas sin JUnit
```

Los diagramas UML están en `docs/uml/` (PlantUML: `diagrama_clases.puml`, `secuencia_integral_definida.puml`; Mermaid: `diagramas_mermaid.md`).

### Patrones de diseño aplicados

- **Fachada:** `Calculadora` es la única puerta de entrada al motor para la GUI y las pruebas.
- **Método Plantilla:** `TerminoFuncional.derivar()`/`integrar()` definen el algoritmo general (¿la función depende de v?, ¿hace falta la regla del producto?) y delegan en `derivarFuncion()`/`integrarFuncion()` de cada subclase.
- **Estrategia / Polimorfismo:** `ProveedorIA` permite cambiar de proveedor sin modificar `TutorIA`.
- **Fábrica sencilla:** `TutorIA.crearProveedor()` elige el proveedor según la configuración.

## 4. Modelo matemático

### 4.1 Representación de un término

Todo término tiene una parte algebraica `c · x^a · y^b`. Los términos funcionales la multiplican por `f(k·w)`, donde `w ∈ {x, y}` y `k ≠ 0`:

| Texto | Clase | c | a | b | f | k |
|---|---|---|---|---|---|---|
| `3x^2` | TerminoPolinomico | 3 | 2 | 0 | — | — |
| `x^2*y` | TerminoPolinomico | 1 | 2 | 1 | — | — |
| `y*sin(x)` | TerminoSeno | 1 | 0 | 1 | sin, w = x | 1 |
| `-4e^(2x)` | TerminoExponencial | -4 | 0 | 0 | exp, w = x | 2 |

### 4.2 Derivación parcial e integración parcial

Se elige una variable de trabajo `v`. La otra variable se trata como **constante**:

- Si la función `f(k·w)` no depende de `v`, actúa como coeficiente y solo se aplica la regla de la potencia a `v^n`. Ejemplo: `∂/∂y [y·sin(x)] = sin(x)`.
- Si la función depende de `v` y la parte algebraica no contiene `v`, se aplica la regla de la cadena o la integral inmediata. Ejemplo: `∂/∂x [y·sin(x)] = y·cos(x)`.
- Si ambas dependen de `v` (por ejemplo `x·sin(x)`), se lanza `OperacionNoSoportadaException` (regla del producto / integración por partes).

### 4.3 Reglas de derivación (`describirDerivada`)

| Regla | Fórmula | Clase |
|---|---|---|
| Constante | d/dv (c) = 0 | TerminoPolinomico |
| Potencia | d/dv (c·vⁿ) = c·n·vⁿ⁻¹ | Termino (`derivarParteAlgebraica`) |
| Suma/Resta | d/dv [f ± g] = f' ± g' | Expresion / Calculadora |
| Seno | d/dv [c·sin(kv)] = c·k·cos(kv) | TerminoSeno |
| Coseno | d/dv [c·cos(kv)] = −c·k·sin(kv) | TerminoCoseno |
| Exponencial | d/dv [c·e^(kv)] = c·k·e^(kv) | TerminoExponencial |
| Logaritmo | d/dv [c·ln(kv)] = c/v = c·v⁻¹ | TerminoLogaritmico |
| Variable constante | ∂/∂x [y·sin(x)] = y·cos(x) | TerminoFuncional |

### 4.4 Reglas de integración (`describirIntegral`)

| Regla | Fórmula | Clase |
|---|---|---|
| Potencia (n ≠ −1) | ∫ c·vⁿ dv = c/(n+1)·vⁿ⁺¹ | Termino (`integrarParteAlgebraica`) |
| Logaritmo (n = −1) | ∫ c·v⁻¹ dv = c·ln\|v\| | TerminoPolinomico → TerminoLogaritmico |
| Seno | ∫ c·sin(kv) dv = −(c/k)·cos(kv) | TerminoSeno |
| Coseno | ∫ c·cos(kv) dv = (c/k)·sin(kv) | TerminoCoseno |
| Exponencial | ∫ c·e^(kv) dv = (c/k)·e^(kv) | TerminoExponencial |
| ln(v) solo | requiere integración por partes → `OperacionNoSoportadaException` | TerminoLogaritmico |
| Constante de integración | se agrega `+ C` a toda integral indefinida | Calculadora |

### 4.5 Integral definida (Teorema Fundamental del Cálculo)

1. Obtener la antiderivada `F(v)`.
2. Evaluar el límite superior `F(b)`.
3. Evaluar el límite inferior `F(a)`.
4. Calcular `R = F(b) − F(a)`.

**Validación de dominio** (`DominioInvalidoException`, "Integral impropia fuera del alcance"):

- Un término con exponente `n < 0` y `0 ∈ [a, b]`.
- Un exponente no entero (raíz) con valores negativos en el intervalo (raíces no reales).
- `ln(kv)` con argumento no positivo en el intervalo.
- La otra variable con exponente negativo y valor fijo 0.
- Cualquier `F(a)` o `F(b)` que resulte NaN o infinito.

### 4.6 Simplificación

`Expresion.simplificar()` agrupa términos con la misma **firma de semejanza** (tipo, exponentes, variable de la función y `k`), suma sus coeficientes y elimina los nulos.

### 4.7 Formato de números

`FormatoNumeros.formatear()` decide: entero (`6`), fracción con denominador ≤ 100 (`1/3`, buscando D = 2…100) o decimal con 6 cifras (`3.141593`).

## 5. Analizador de expresiones

Parser de **descenso recursivo** que avanza carácter por carácter:

```
expresion  ::= termino { ('+' | '-') termino }
termino    ::= factor { ('*' | '/' | implícita) factor }
factor     ::= ('+' | '-') factor | potencia
potencia   ::= primario [ '^' exponente ]
primario   ::= numero | x | y | pi | e ['^' arg] | funcion '(' expresion ')' | '(' expresion ')'
funcion    ::= sin | sen | cos | exp | ln
```

- Multiplicación implícita: `2x`, `3sin(x)`, `x^2y`.
- `-x^2` se interpreta como `-(x^2)`.
- Normaliza `·`, `×`, `−`, `÷`, `π` y la coma decimal.
- Los errores indican la posición del carácter (`ExpresionInvalidaException`).

## 5 bis. Motor simbólico (árbol de expresión)

El motor por términos (sección 4) solo representa sumas de monomios por una función. Para la regla del producto, del cociente, de la cadena general, L'Hôpital y los métodos de integración se construyó un **árbol de expresión** (patrón Composite): cada nodo es un número, una variable, una operación binaria o una función.

**Simplificador (forma canónica).** Los productos se aplanan en *coeficiente × Π base^exponente* (así x·x² = x³, x/x = 1, e^x·e^(2x) = e^(3x)); las sumas se aplanan en términos y se combinan los semejantes. También se extrae el factor común de una suma ((2x + 2)/4 = (x + 1)/2) y se evalúan funciones con valor exacto (arctan(1) = π/4).

**Derivador.** Recursivo, una regla por tipo de nodo: constante, identidad, variable tratada como constante, múltiplo constante, suma/resta, potencia (con cadena), producto, cociente, exponencial (e^u, a^u), logarítmica (ln, log, ln|u|), trigonométricas, inversas, y derivación logarítmica para f^g. Cada aplicación queda como `PasoDetallado` con profundidad (los pasos de los factores aparecen anidados).

**Derivación implícita.** Para F(x, y) = 0 se derivan ambos lados con y = y(x), se agrupan los términos con y' y se despeja: dy/dx = −F_x/F_y.

**Límites y L'Hôpital.** `Limites` evalúa la función en puntos cada vez más cercanos (o en ±10^k) y clasifica: finito, +∞, −∞ o no existe (detecta funciones definidas solo de un lado, como ln x). L'Hôpital se repite mientras la forma sea 0/0 o ∞/∞; la forma 0·∞ se reescribe como cociente.

**Integrador.** Orden automático: constante → suma/resta → múltiplo constante → tabla inmediata (argumento lineal) → identidades trigonométricas → desarrollo de polinomios → **sustitución** (se prueba cada subexpresión u, se divide entre u' y se comprueba que no quede x) → **fracciones parciales** (división, factorización con raíz racional y Durand–Kerner, sistema lineal con Gauss) → distribución → **sustitución trigonométrica** (√(a²−x²), √(a²+x²), √(x²−a²) con potencias ±1/2, −3/2) → **partes** (LIATE, con integrales cíclicas I = A + k·I). Los intentos fallidos se deshacen en la bitácora. **Todo resultado se verifica** derivándolo numéricamente; si no coincide, se rechaza.

**Integrales definidas.** TFC con propiedades (límites iguales, inversión, aditividad). Si hay límites infinitos o puntos donde la función se va a infinito (raíces del denominador o extremos), se trata como **impropia**: se divide el intervalo y cada extremo se evalúa con límites laterales; si alguno no es finito, la integral **diverge**.

**Resaltado del método.** Cada método de la lista declara las reglas que le corresponden (`MetodoCalculo.getReglas()`); los pasos con esas reglas se marcan como destacados y, si ninguna se usó, se agrega una nota.

## 6. Asistente de IA

- **Configuración en el código:** constantes `PROVEEDOR`, `CLAVE_API`, `MODELO`, `URL_BASE` y `TIEMPO_ESPERA_SEGUNDOS` de `ConfiguracionIA`. Si la clave está vacía se usa la variable de entorno `CALCULADORA_IA_API_KEY` o, opcionalmente, `config.properties`. Advertencia: una clave escrita en el código no debe subirse a un repositorio público.
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
| `vista.componentes.BotonModerno` | Botones redondeados con estados hover/presionado (primario, secundario, fantasma, chip, tecla). |
| `vista.componentes.PanelTarjeta` | Tarjetas blancas con esquinas redondeadas y sombra. |
| `vista.componentes.GrupoSegmentado` | Selector segmentado (operación, variable, navegación). |
| `vista.componentes.CampoModerno` | Campos redondeados con halo de foco y texto de ayuda. |
| `vista.componentes.Encabezado` | Barra con degradado, logotipo e insignia del estado de la IA. |
| `vista.componentes.BarraDesplazamientoModerna` | Barras de desplazamiento delgadas. |
| `vista.RenderizadorHtml` | Convierte los pasos y la explicación en HTML con formato. |
| `vista.PanelMetodos` | Lista de métodos (Derivadas/Integrales) con fórmula, descripción y ejemplo. |

`VentanaCalculadora` (tres columnas, un método por zona):

- `crearEncabezado()`: título, insignia de la IA y Ayuda.
- `PanelMetodos`: lista de métodos; al cambiar de método `actualizarCamposSegunMetodo()` muestra solo los campos necesarios (variable, orden, punto del límite, tipo de integral, a, b, c).
- `crearTarjetaEntrada()` y `crearTarjetaTeclado()`: datos y teclado. Las teclas no toman el foco y escriben en el último campo con cursor (`campoActivo`), incluidos los límites (∞ y −∞).
- `crearTarjetaResultado()`: resultado destacado; los errores se muestran ahí en rojo.
- `crearZonaSecciones()`: **Procedimiento**, **Gráfica 2D** y **Asistente IA** con `CardLayout`.

`PanelGrafico2D`: antialiasing, cuadrícula con paso "1-2-5", marcas numéricas, curva principal (índigo), derivada/antiderivada (rosa discontinua), área en `[a, b]` con `AlphaComposite` (turquesa 35 %), zoom con rueda centrado en el cursor, arrastre y doble clic para restablecer. Corta la curva en puntos no definidos y asíntotas. Dibuja curvas implícitas F(x, y) = 0 con *marching squares* y sombrea áreas con límites infinitos hasta el borde visible.

## 8. Pruebas

`calculadora.pruebas.PruebasCalculadora` (171 casos, sin JUnit): formato de números, motor por términos, analizador, JSON, módulo de IA (incluida la conmutación al explicador local), motor simbólico (reglas de derivación, implícita, orden superior, parciales, L'Hôpital, sustitución, partes, fracciones parciales, sustitución trigonométrica, integrales impropias y divergentes) y una prueba que resuelve el ejemplo de **cada método de la lista**. Termina con código 1 si alguna falla.

```
.\ejecutar_pruebas.bat
...
Pruebas exitosas: 171
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
| 2 | `AnalizadorExpresion` y fachada `Calculadora` |
| 3 | Seno, coseno, exponencial y logaritmo |
| 4 | Módulo de IA, `JsonSencillo`, `ExplicadorLocal` |
| 5 | GUI Swing, `PanelGrafico2D`, `SwingWorker` |
| 6 | Scripts `.bat` |
| 7 | Documentación |

## 11. Limitaciones y trabajo futuro

- Potencias trigonométricas generales (sin³x·cos²x) y fracciones parciales con factores cuadráticos repetidos.
- Formas indeterminadas ∞ − ∞, 1^∞ y 0^0 en L'Hôpital.
- Integración numérica (Simpson) para funciones sin antiderivada elemental, como e^(x²).
- Gráficas 3D de funciones f(x, y).
