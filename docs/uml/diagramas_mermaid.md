# Diagramas en Mermaid

Estos diagramas se visualizan directamente en GitHub, GitLab o en VS Code (extensión *Markdown Preview Mermaid Support*).

## Árbol de expresión (patrón Composite)

```mermaid
classDiagram
    class Nodo {
        <<abstract>>
        +evaluar(x, y) double
        +clave() String
        +reemplazar(objetivo, nuevo) Nodo
    }
    class NodoConstante
    class NodoVariable
    class NodoBinario {
        <<abstract>>
    }
    class NodoSuma
    class NodoResta
    class NodoProducto
    class NodoCociente
    class NodoPotencia
    class NodoNegativo
    class NodoFuncion {
        -TipoFuncion tipo
        -Nodo argumento
    }
    Nodo <|-- NodoConstante
    Nodo <|-- NodoVariable
    Nodo <|-- NodoBinario
    Nodo <|-- NodoNegativo
    Nodo <|-- NodoFuncion
    NodoBinario <|-- NodoSuma
    NodoBinario <|-- NodoResta
    NodoBinario <|-- NodoProducto
    NodoBinario <|-- NodoCociente
    NodoBinario <|-- NodoPotencia
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
    A[Texto del usuario] --> V[Vista previa: fórmula dibujada y avisos]
    A --> B[AnalizadorSimbolico]
    B -->|ExpresionInvalidaException| X[Mensaje amable + sugerencia del Sugeridor]
    B --> C[Árbol de expresión Nodo]
    C --> D{Método elegido}
    D -->|Derivadas| E[Derivador con pasos]
    D -->|Límites| K[Limites + L'Hôpital]
    D -->|Integral indefinida| F[Integrador + verificación + C]
    D -->|Integral definida| G[F b − F a, impropias, comprobación numérica]
    G -->|sin antiderivada elemental| N[IntegracionNumerica Gauss-Kronrod]
    D -->|Aplicaciones| H[Tangente, extremos, área entre curvas]
    E & K & F & G & N & H --> R[ResultadoCalculo con pasos y notas]
    R --> P[Procedimiento, Historial y Exportar]
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
