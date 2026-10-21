# Calculadora de Cálculo con Asistente de IA y Graficador Integral de Funciones

Proyecto universitario de **Programación II – Ingeniería en Sistemas**.
Aplicación de escritorio en **Java Swing** que deriva, integra y evalúa funciones de una o dos variables, explica cada regla aplicada paso a paso, grafica la función con el área de la integral sombreada y cuenta con un **asistente de IA** que explica (sin calcular) el procedimiento.

![Gráfica de una integral definida](docs/capturas/grafica_integral_definida.png)

## Características

- **Lista de métodos** para elegir la ley o regla: constante, identidad, múltiplo, suma, potencia, producto, cociente, cadena, exponenciales, logarítmicas, trigonométricas, inversas, orden superior, implícita, parciales y **L'Hôpital**; en integrales: propiedades, potencia, logaritmo, exponenciales, trigonométricas, **sustitución, partes, fracciones parciales, sustitución trigonométrica**, TFC, límites iguales, inversión, aditividad e **impropias con ±∞**.
- **Procedimiento paso a paso** con el método elegido resaltado.
- **Motor simbólico** propio (árbol de expresión, simplificador, derivador, integrador con verificación).
- **Límites infinitos** (∞ y −∞) en L'Hôpital y en integrales impropias.
- **Graficador 2D**: derivadas, áreas (también hasta ∞), puntos de límite y curvas implícitas.
- **Interfaz en modo oscuro** con teclado matemático (números, funciones, ∞).
- **Asistente de IA** configurable en el código (Claude, OpenAI u Ollama) con respaldo al Explicador Local.
- **Sin dependencias**: JDK 11+, `java.net.http.HttpClient` y JSON manual.
- **171 pruebas automáticas** sin JUnit.

![Regla del producto](docs/capturas/regla_del_producto.png)

## Inicio rápido (Windows / PowerShell)

```powershell
.\build.bat              # compila y genera dist\CalculadoraCalculo.jar
.\ejecutar.bat           # abre la calculadora
.\ejecutar_pruebas.bat   # ejecuta las pruebas
```

O directamente: `java -jar dist\CalculadoraCalculo.jar`

### Asistente de IA (opcional)

Edite `src/calculadora/ia/ConfiguracionIA.java` (constantes `PROVEEDOR`, `CLAVE_API`, `MODELO`) y recompile con `.\build.bat`. Sin clave se usa el Explicador Local (sin Internet).

## Estructura

```
calculadora java/
├── src/calculadora/
│   ├── Main.java
│   ├── modelo/        Términos, Expresion, ResultadoCalculo, MetodoCalculo, Calculadora (fachada)
│   ├── simbolico/     Árbol de expresión, Simplificador, Derivador, Integrador, Limites
│   ├── analisis/      AnalizadorExpresion (descenso recursivo)
│   ├── excepciones/   CalculadoraException + 4 subclases
│   ├── util/          FormatoNumeros, JsonSencillo
│   ├── ia/            ProveedorIA, ProveedorAnthropic, ProveedorCompatibleOpenAI,
│   │                  ConfiguracionIA, ExplicadorLocal, TutorIA
│   ├── vista/         VentanaCalculadora, PanelMetodos, PanelGrafico2D, componentes y estilo
│   └── pruebas/       PruebasCalculadora
├── docs/
│   ├── Documento_Tecnico.md
│   ├── Manual_de_Usuario.md
│   ├── uml/           diagramas PlantUML y Mermaid
│   └── capturas/
├── build.bat · ejecutar.bat · ejecutar_pruebas.bat
├── config.properties.ejemplo
└── .gitignore
```

## Ejemplos

| Método | Entrada | Resultado |
|---|---|---|
| Regla del producto | `x^2·sin(x)` | `2x·sin(x) + x^2·cos(x)` |
| Regla de L'Hôpital | `sin(x)/x`, x → 0 | `1` |
| Derivación implícita | `x^2 + y^2 = 25` | `dy/dx = -x/y` |
| Por partes | `x·e^x` | `x·e^x - e^x + C` |
| Fracciones parciales | `1/(x^2 - 1)` | `ln\|x - 1\|/2 - ln\|x + 1\|/2 + C` |
| Sustitución trigonométrica | `1/√(4 - x^2)` | `arcsin(x/2) + C` |
| Impropia | `1/x^2` en [1, ∞) | `1` |

## Documentación

- [Documento Técnico](docs/Documento_Tecnico.md): arquitectura, reglas matemáticas, parser, IA y pruebas.
- [Manual de Usuario](docs/Manual_de_Usuario.md): instalación, uso, sintaxis y errores frecuentes.
- [Diagramas UML](docs/uml/): clases y secuencia (PlantUML y Mermaid).
