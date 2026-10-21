# Diagramas en Mermaid

Estos diagramas se visualizan directamente en GitHub, GitLab o en VS Code (extensión *Markdown Preview Mermaid Support*).

## Jerarquía de términos

```mermaid
classDiagram
    class Termino {
        <<abstract>>
        #double coeficiente
        #double exponenteX
        #double exponenteY
        +evaluar(x, y) double
        +derivar(v) Termino
        +describirDerivada(v) String
        +integrar(v) Termino
        +describirIntegral(v) String
    }
    class TerminoPolinomico
    class TerminoFuncional {
        <<abstract>>
        #Variable variableFuncion
        #double constanteK
        #aplicarFuncion(arg) double
        #derivarFuncion() Termino
        #integrarFuncion() Termino
    }
    class TerminoSeno
    class TerminoCoseno
    class TerminoExponencial
    class TerminoLogaritmico
    class Expresion {
        -List~Termino~ terminos
        +derivar(v) Expresion
        +integrar(v) Expresion
        +simplificar() Expresion
    }
    Termino <|-- TerminoPolinomico
    Termino <|-- TerminoFuncional
    TerminoFuncional <|-- TerminoSeno
    TerminoFuncional <|-- TerminoCoseno
    TerminoFuncional <|-- TerminoExponencial
    TerminoFuncional <|-- TerminoLogaritmico
    Expresion o-- Termino
```

## Módulo de IA

```mermaid
classDiagram
    class ProveedorIA {
        <<interface>>
        +getNombre() String
        +generarRespuesta(sistema, mensaje) String
    }
    class ProveedorAnthropic
    class ProveedorCompatibleOpenAI
    class TutorIA {
        +explicar(resultado, pregunta) String
    }
    class ExplicadorLocal
    class ConfiguracionIA
    ProveedorIA <|.. ProveedorAnthropic
    ProveedorIA <|.. ProveedorCompatibleOpenAI
    TutorIA --> ProveedorIA
    TutorIA --> ExplicadorLocal : respaldo
    TutorIA --> ConfiguracionIA
```

## Flujo de una operación

```mermaid
flowchart TD
    A[Texto del usuario] --> B[AnalizadorExpresion]
    B -->|ExpresionInvalidaException| X[Mensaje amable en la GUI]
    B --> C[Expresion = suma de Terminos]
    C --> D{Operación}
    D -->|Derivada| E[derivar + describirDerivada por término]
    D -->|Integral indefinida| F[integrar + describirIntegral + C]
    D -->|Integral definida| G[validar dominio → F b − F a]
    D -->|Evaluar| H[sustituir x, y]
    E & F & G & H --> R[ResultadoCalculo con pasos]
    R --> P[Pestaña Resultado y pasos]
    R --> Q[PanelGrafico2D]
    R --> T[TutorIA en SwingWorker]
    T -->|sin red o sin clave| L[ExplicadorLocal]
    T -->|con proveedor| M[Anthropic / OpenAI / Ollama]
```

## Secuencia: explicación con IA y respaldo automático

```mermaid
sequenceDiagram
    actor E as Estudiante
    participant V as VentanaCalculadora
    participant W as SwingWorker
    participant T as TutorIA
    participant P as ProveedorIA
    participant L as ExplicadorLocal
    E->>V: Explicar con IA
    V->>W: execute()
    W->>T: explicar(resultado, pregunta)
    alt proveedor configurado
        T->>P: generarRespuesta(instrucción, pasos del motor)
        P-->>T: texto (o IAException)
    end
    opt sin proveedor o IAException
        T->>L: explicar(resultado, pregunta)
        L-->>T: texto local
    end
    T-->>W: texto plano ≤ 250 palabras
    W-->>V: done()
    V-->>E: muestra la explicación
```
