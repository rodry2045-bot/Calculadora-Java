# Documento Técnico

**Proyecto:** Calculadora de Cálculo con Asistente de IA y Graficador Integral de Funciones
**Curso:** Programación II – Ingeniería en Sistemas
**Lenguaje:** Java 11 o superior (sin Maven, sin Gradle, sin librerías externas)

---

## 1. Objetivo

Construir una aplicación de escritorio que derive, integre (indefinida y definidamente) y evalúe funciones de una o dos variables, mostrando **el procedimiento paso a paso** con la regla aplicada en cada término. La aplicación grafica la función, sombrea el área de las integrales definidas y ofrece un **asistente de IA que explica (pero nunca calcula)** el procedimiento.

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
├── modelo                       Motor matemático
│   ├── Variable                 Enum X, Y
│   ├── Termino                  Abstracta: c · x^a · y^b
│   ├── TerminoPolinomico        Monomios y constantes
│   ├── TerminoFuncional         Abstracta: c · x^a · y^b · f(k·w)
│   ├── TerminoSeno / TerminoCoseno / TerminoExponencial / TerminoLogaritmico
│   ├── Expresion                Suma de términos, simplificación
│   ├── ResultadoCalculo         Operación, pasos y resultado
│   └── Calculadora              Fachada del motor
├── analisis
│   └── AnalizadorExpresion      Parser de descenso recursivo
├── excepciones                  CalculadoraException y sus 4 subclases
├── util
│   ├── FormatoNumeros           Enteros, fracciones N/D (D ≤ 100) y decimales
│   └── JsonSencillo             Lectura y escritura de JSON
├── ia                           Asistente
│   ├── ProveedorIA              Interfaz
│   ├── ProveedorAnthropic       API de Mensajes de Anthropic
│   ├── ProveedorCompatibleOpenAI  OpenAI y Ollama local
│   ├── ConfiguracionIA          config.properties + CALCULADORA_IA_API_KEY
│   ├── ExplicadorLocal          Respaldo sin red
│   └── TutorIA                  Orquestador con conmutación automática
├── vista
│   ├── VentanaCalculadora       JFrame (un método por panel)
│   └── PanelGrafico2D           Graficador con Graphics2D
└── pruebas
    └── PruebasCalculadora       99 pruebas sin JUnit
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

## 6. Asistente de IA

- **Configuración:** `config.properties` (copiar desde `config.properties.ejemplo`) o variable de entorno `CALCULADORA_IA_API_KEY`, que tiene prioridad. El archivo real está en `.gitignore`; no hay claves en el código.
- **Proveedores:** `anthropic` (API de Mensajes, encabezados `x-api-key` y `anthropic-version`), `openai` y `ollama` (Chat Completions, `Authorization: Bearer`).
- **Instrucción de sistema:** prohíbe calcular, exige texto plano sin Markdown ni LaTeX y máximo 250 palabras.
- **Mensaje:** contiene el informe de pasos de `ResultadoCalculo` y la pregunta opcional.
- **Post-proceso:** `TutorIA.limpiarFormato()` quita marcas de Markdown/LaTeX y `limitarPalabras()` recorta a 250 palabras.
- **Respaldo:** si no hay proveedor o se lanza `IAException` (sin red, 401, 429, tiempo agotado, JSON inválido), se usa `ExplicadorLocal`, que resume los pasos dentro del presupuesto de 250 palabras.
- **Concurrencia:** la GUI llama al tutor dentro de un `SwingWorker` para no congelar la ventana.

## 7. Interfaz gráfica

`VentanaCalculadora` (un método por panel):

- `crearPanelEntrada()`: función, ejemplos, operación, variable, límites, valor fijo de la otra variable.
- `crearPanelResultado()`: pestaña **Resultado y pasos**.
- `crearPanelGrafica()`: pestaña **Gráfica 2D**.
- `crearPanelAsistente()`: pestaña **Asistente IA**.
- Errores: cada tipo de excepción muestra un `JOptionPane` con título y sugerencia propios.

`PanelGrafico2D`: antialiasing, cuadrícula con paso "1-2-5", marcas numéricas, curva principal (azul), derivada/antiderivada (roja discontinua), área en `[a, b]` con `AlphaComposite` (verde 35 %), zoom con rueda centrado en el cursor, arrastre y doble clic para restablecer. Corta la curva en puntos no definidos y asíntotas.

## 8. Pruebas

`calculadora.pruebas.PruebasCalculadora` (99 casos, sin JUnit), agrupados en: formato de números, términos polinómicos, analizador, derivadas, integrales indefinidas, integrales definidas, manejo de errores, JSON y módulo de IA (incluida la conmutación automática al explicador local cuando el proveedor no responde). Termina con código 1 si alguna falla.

```
.\ejecutar_pruebas.bat
...
Pruebas exitosas: 99
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

- Regla del producto, del cociente e integración por partes (`x·sin(x)`, `ln(x)`).
- Argumentos no lineales (`sin(x^2)`) y regla de la cadena general.
- Desarrollo de productos de sumas (`(x+1)^2`).
- Integrales impropias y métodos numéricos (Simpson) como alternativa.
- Gráficas 3D de funciones f(x, y).
