package calculadora.modelo;

/**
 * Catálogo de reglas, leyes y métodos que el motor simbólico puede aplicar.
 * <p>
 * Cada paso del procedimiento queda etiquetado con una de estas reglas. Así
 * la interfaz puede resaltar los pasos del método que eligió el estudiante y
 * avisar si ese método no fue necesario.
 * </p>
 */
public enum ReglaCalculo {

    // ------------------------------- Derivación -------------------------------
    /** d/dx(c) = 0. */
    CONSTANTE("Regla de la constante"),
    /** d/dx(x) = 1. */
    IDENTIDAD("Regla de la identidad"),
    /** La otra variable se trata como constante (derivada parcial). */
    VARIABLE_CONSTANTE("Variable tratada como constante"),
    /** d/dx(c·f) = c·f'. */
    MULTIPLO_CONSTANTE("Regla del múltiplo constante"),
    /** d/dx(f ± g) = f' ± g'. */
    SUMA_RESTA("Regla de la suma y la resta"),
    /** d/dx(xⁿ) = n·x^(n-1). */
    POTENCIA("Regla de la potencia"),
    /** (f·g)' = f'·g + f·g'. */
    PRODUCTO("Regla del producto"),
    /** (f/g)' = (f'·g − f·g')/g². */
    COCIENTE("Regla del cociente"),
    /** [f(g(x))]' = f'(g(x))·g'(x). */
    CADENA("Regla de la cadena"),
    /** (e^u)' y (a^u)'. */
    EXPONENCIAL("Derivada exponencial"),
    /** (ln u)' y (log u)'. */
    LOGARITMICA("Derivada logarítmica"),
    /** Derivadas de sin, cos, tan, sec, csc y cot. */
    TRIGONOMETRICA("Derivada trigonométrica"),
    /** Derivadas de arcsin, arccos y arctan. */
    INVERSA("Derivada de función inversa"),
    /** Derivadas de sinh, cosh y tanh. */
    HIPERBOLICA("Derivada hiperbólica"),
    /** y = f^g, se toma logaritmo natural en ambos lados. */
    DERIVACION_LOGARITMICA("Derivación logarítmica"),
    /** Derivadas sucesivas f'', f''', ... */
    ORDEN_SUPERIOR("Derivada de orden superior"),
    /** dy/dx en una ecuación F(x, y) = 0. */
    IMPLICITA("Derivación implícita"),
    /** lim f/g = lim f'/g' en formas 0/0 e ∞/∞. */
    LHOPITAL("Regla de L'Hôpital"),
    /** Sustitución directa en un límite. */
    SUSTITUCION_DIRECTA("Sustitución directa"),

    // ------------------------------- Integración ------------------------------
    /** ∫k dx = kx + C. */
    INT_CONSTANTE("Integral de una constante"),
    /** ∫c·f dx = c·∫f dx. */
    INT_MULTIPLO("Propiedad del múltiplo constante"),
    /** ∫(f ± g) dx = ∫f dx ± ∫g dx. */
    INT_SUMA("Propiedad de la suma y la resta"),
    /** ∫xⁿ dx = x^(n+1)/(n+1) + C. */
    INT_POTENCIA("Regla de la potencia (integral)"),
    /** ∫1/x dx = ln|x| + C. */
    INT_LOGARITMO("Integral del logaritmo natural"),
    /** ∫e^(kx) dx y ∫aˣ dx. */
    INT_EXPONENCIAL("Integral de funciones exponenciales"),
    /** Integrales trigonométricas básicas. */
    INT_TRIGONOMETRICA("Integral trigonométrica básica"),
    /** Integrales que dan arctan o arcsin. */
    INT_INVERSA_TRIG("Integral que produce una función inversa"),
    /** Identidades como sin²x = (1 − cos 2x)/2. */
    IDENTIDAD_TRIG("Identidad trigonométrica"),
    /** Desarrollo o distribución algebraica antes de integrar. */
    DESARROLLO("Desarrollo algebraico"),
    /** Cambio de variable u = g(x). */
    SUSTITUCION("Sustitución (cambio de variable)"),
    /** ∫u dv = uv − ∫v du. */
    PARTES("Integración por partes"),
    /** División de polinomios antes de fracciones parciales. */
    DIVISION_POLINOMIOS("División de polinomios"),
    /** Descomposición en fracciones parciales. */
    FRACCIONES_PARCIALES("Fracciones parciales"),
    /** x = a·sinθ, a·tanθ o a·secθ. */
    SUSTITUCION_TRIG("Sustitución trigonométrica"),
    /** ∫[a,b] f dx = F(b) − F(a). */
    TFC("Teorema Fundamental del Cálculo"),
    /** ∫[a,a] f dx = 0. */
    LIMITES_IGUALES("Propiedad de límites iguales"),
    /** ∫[a,b] f dx = −∫[b,a] f dx. */
    INVERSION_LIMITES("Propiedad de inversión de límites"),
    /** ∫[a,b] = ∫[a,c] + ∫[c,b]. */
    ADITIVIDAD("Propiedad de aditividad del intervalo"),
    /** Integral con límites infinitos o con discontinuidad infinita. */
    IMPROPIA("Integral impropia"),
    /** Aproximación numérica de una integral definida (Gauss-Kronrod). */
    INT_NUMERICA("Integración numérica"),
    /** Integrales de funciones hiperbólicas. */
    INT_HIPERBOLICA("Integral hiperbólica"),
    /** Fórmula de reducción para potencias de seno y coseno. */
    REDUCCION("Fórmula de reducción"),
    /** Integral del valor absoluto de una expresión lineal. */
    INT_VALOR_ABSOLUTO("Integral del valor absoluto"),

    // ------------------------------- Aplicaciones -------------------------------
    /** Recta tangente y = f(a) + f'(a)(x - a). */
    RECTA_TANGENTE("Recta tangente"),
    /** Puntos críticos: f'(x) = 0. */
    PUNTOS_CRITICOS("Puntos críticos"),
    /** Criterio de la segunda derivada y puntos de inflexión. */
    CRITERIO_SEGUNDA_DERIVADA("Criterio de la segunda derivada"),
    /** Área entre dos curvas. */
    AREA_ENTRE_CURVAS("Área entre curvas"),

    // --------------------------------- Comunes --------------------------------
    /** Se simplifica el resultado. */
    SIMPLIFICACION("Simplificación"),
    /** Se comprueba el resultado derivando. */
    VERIFICACION("Verificación"),
    /** Información general del procedimiento. */
    INFORMACION("Planteamiento");

    private final String nombre;

    ReglaCalculo(String nombre) {
        this.nombre = nombre;
    }

    /** @return nombre de la regla en español */
    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
