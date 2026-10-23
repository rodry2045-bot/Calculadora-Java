package calculadora.modelo;

/**
 * Catálogo de métodos, leyes y reglas que el estudiante puede elegir en la
 * lista de la interfaz. Cada método indica:
 * <ul>
 *   <li>la operación que se realiza (derivada, límite, integral...);</li>
 *   <li>las reglas que se resaltan en el procedimiento;</li>
 *   <li>la técnica de integración que se exige (si aplica);</li>
 *   <li>su fórmula, una descripción y un ejemplo listo para cargar.</li>
 * </ul>
 */
public enum MetodoCalculo {

    // ================================ DERIVADAS ================================
    D_AUTOMATICO(Categoria.DERIVADAS, Operacion.DERIVADA, "Automático (todas las reglas)",
            "Se aplican todas las reglas necesarias",
            "El motor identifica la estructura de la función y aplica cada regla en el orden correcto.",
            "x^2·sin(x) + e^(3x)", null),
    D_CONSTANTE(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla de la constante", "d/dx[c] = 0",
            "La derivada de un número es cero: una constante no cambia.", "7 + x^2", null, ReglaCalculo.CONSTANTE),
    D_IDENTIDAD(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla de la identidad", "d/dx[x] = 1",
            "La derivada de la variable respecto a sí misma es 1.", "x + 5", null, ReglaCalculo.IDENTIDAD),
    D_MULTIPLO(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla del múltiplo constante", "d/dx[c·f] = c·f'",
            "Una constante que multiplica se conserva y se deriva solo la función.", "5x^3", null,
            ReglaCalculo.MULTIPLO_CONSTANTE),
    D_SUMA(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla de la suma y la resta", "d/dx[f ± g] = f' ± g'",
            "Se deriva cada término por separado y se conservan los signos.", "x^3 - 4x + 2", null,
            ReglaCalculo.SUMA_RESTA),
    D_POTENCIA(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla de la potencia", "d/dx[x^n] = n·x^(n-1)",
            "Se multiplica por el exponente y se le resta 1. Sirve para raíces (x^(1/2)) y 1/x^n.",
            "x^5 + √(x) + 1/x^2", null, ReglaCalculo.POTENCIA),
    D_PRODUCTO(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla del producto", "(f·g)' = f'·g + f·g'",
            "Derivada del primero por el segundo, más el primero por la derivada del segundo.", "x^2·sin(x)", null,
            ReglaCalculo.PRODUCTO),
    D_COCIENTE(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla del cociente", "(f/g)' = (f'·g - f·g')/g²",
            "Derivada del numerador por el denominador, menos el numerador por la derivada del denominador, "
                    + "todo entre el denominador al cuadrado.", "sin(x)/x", null, ReglaCalculo.COCIENTE),
    D_CADENA(Categoria.DERIVADAS, Operacion.DERIVADA, "Regla de la cadena", "[f(g(x))]' = f'(g(x))·g'(x)",
            "Para funciones compuestas: se deriva la función de afuera evaluada en la de adentro y se multiplica "
                    + "por la derivada de la de adentro.", "sin(x^2 + 1)", null, ReglaCalculo.CADENA),
    D_EXPONENCIAL(Categoria.DERIVADAS, Operacion.DERIVADA, "Derivadas exponenciales",
            "(e^u)' = e^u·u'     (a^u)' = a^u·ln(a)·u'",
            "La exponencial natural es su propia derivada; con otra base aparece ln(a).", "e^(3x) + 2^x", null,
            ReglaCalculo.EXPONENCIAL),
    D_LOGARITMICA(Categoria.DERIVADAS, Operacion.DERIVADA, "Derivadas logarítmicas",
            "(ln u)' = u'/u     (log u)' = u'/(u·ln 10)",
            "La derivada del logaritmo es la derivada del argumento entre el argumento.", "ln(x^2 + 1)", null,
            ReglaCalculo.LOGARITMICA),
    D_TRIGONOMETRICA(Categoria.DERIVADAS, Operacion.DERIVADA, "Derivadas trigonométricas",
            "(sin u)' = cos u·u'   (cos u)' = -sin u·u'   (tan u)' = sec²u·u'",
            "También: (sec u)' = sec u·tan u·u',  (csc u)' = -csc u·cot u·u',  (cot u)' = -csc²u·u'.",
            "tan(2x) + sec(x)", null, ReglaCalculo.TRIGONOMETRICA),
    D_INVERSAS(Categoria.DERIVADAS, Operacion.DERIVADA, "Derivadas de funciones inversas",
            "(arcsin u)' = u'/√(1-u²)   (arctan u)' = u'/(1+u²)",
            "También: (arccos u)' = -u'/√(1-u²).", "arctan(x^2) + arcsin(x)", null, ReglaCalculo.INVERSA),
    D_ORDEN_SUPERIOR(Categoria.DERIVADAS, Operacion.ORDEN_SUPERIOR, "Derivadas de orden superior",
            "f'' = (f')'     f''' = (f'')'",
            "Se deriva varias veces seguidas. Escriba el orden n (2 a 6).", "x^4 - 3x^2 + sin(x)", null),
    D_IMPLICITA(Categoria.DERIVADAS, Operacion.IMPLICITA, "Derivación implícita", "dy/dx = -F_x / F_y",
            "Para ecuaciones con x e y (use el signo =). Se deriva respecto a x considerando y = y(x) y se despeja y'.",
            "x^2 + y^2 = 25", null),
    D_PARCIAL(Categoria.DERIVADAS, Operacion.DERIVADA, "Derivada parcial", "∂f/∂y: x se trata como constante",
            "Para funciones de x e y: se deriva respecto a la variable elegida y la otra se trata como número.",
            "x^2·y^3 + sin(x·y)", null, ReglaCalculo.VARIABLE_CONSTANTE),
    D_LHOPITAL(Categoria.DERIVADAS, Operacion.LIMITE, "Regla de L'Hôpital", "lim f/g = lim f'/g'   (0/0 o ∞/∞)",
            "Si al sustituir aparece 0/0 o ∞/∞, se derivan numerador y denominador por separado. "
                    + "El punto puede ser un número, ∞ o -∞.", "sin(x)/x", null, ReglaCalculo.LHOPITAL),

    // ================================ INTEGRALES ================================
    I_AUTOMATICO(Categoria.INTEGRALES, Operacion.INTEGRAL, "Automático (todos los métodos)",
            "Se prueba cada método hasta encontrar la antiderivada",
            "Orden: tabla, sustitución, fracciones parciales, sustitución trigonométrica y por partes.",
            "x·e^x + 1/(x^2 + 1)", TecnicaIntegracion.AUTOMATICO),
    I_MULTIPLO(Categoria.INTEGRALES, Operacion.INTEGRAL, "Propiedad del múltiplo constante",
            "∫c·f dx = c·∫f dx", "La constante sale de la integral.", "7x^3", TecnicaIntegracion.AUTOMATICO,
            ReglaCalculo.INT_MULTIPLO),
    I_SUMA(Categoria.INTEGRALES, Operacion.INTEGRAL, "Propiedad de la suma y la resta",
            "∫(f ± g) dx = ∫f dx ± ∫g dx", "Se integra término a término.", "x^2 + cos(x) - 3",
            TecnicaIntegracion.AUTOMATICO, ReglaCalculo.INT_SUMA),
    I_CONSTANTE(Categoria.INTEGRALES, Operacion.INTEGRAL, "Integral de una constante", "∫k dx = k·x + C",
            "La integral de un número es el número por x.", "5", TecnicaIntegracion.AUTOMATICO,
            ReglaCalculo.INT_CONSTANTE),
    I_POTENCIA(Categoria.INTEGRALES, Operacion.INTEGRAL, "Regla de la potencia", "∫x^n dx = x^(n+1)/(n+1) + C,  n ≠ -1",
            "Se suma 1 al exponente y se divide entre el nuevo exponente.", "x^4 + √(x)", TecnicaIntegracion.AUTOMATICO,
            ReglaCalculo.INT_POTENCIA),
    I_LOGARITMO(Categoria.INTEGRALES, Operacion.INTEGRAL, "Integral del logaritmo natural", "∫1/x dx = ln|x| + C",
            "Es el caso n = -1 de la potencia.", "3/x + 1/(2x + 1)", TecnicaIntegracion.AUTOMATICO,
            ReglaCalculo.INT_LOGARITMO),
    I_EXPONENCIAL(Categoria.INTEGRALES, Operacion.INTEGRAL, "Integral de funciones exponenciales",
            "∫e^(kx) dx = e^(kx)/k + C     ∫a^x dx = a^x/ln(a) + C", "La exponencial se conserva y se divide entre k.",
            "e^(2x) + 3^x", TecnicaIntegracion.AUTOMATICO, ReglaCalculo.INT_EXPONENCIAL),
    I_TRIGONOMETRICAS(Categoria.INTEGRALES, Operacion.INTEGRAL, "Integrales trigonométricas básicas",
            "∫sin x dx = -cos x   ∫cos x dx = sin x   ∫sec²x dx = tan x",
            "También tan, cot, sec, csc, sec·tan y csc·cot.", "sin(3x) + sec(x)^2", TecnicaIntegracion.AUTOMATICO,
            ReglaCalculo.INT_TRIGONOMETRICA),
    I_SUSTITUCION(Categoria.INTEGRALES, Operacion.INTEGRAL, "Sustitución (cambio de variable)",
            "∫f(g(x))·g'(x) dx = ∫f(u) du,  u = g(x)",
            "Se elige u para que su derivada aparezca en el integrando.", "2x·cos(x^2)", TecnicaIntegracion.SUSTITUCION,
            ReglaCalculo.SUSTITUCION),
    I_PARTES(Categoria.INTEGRALES, Operacion.INTEGRAL, "Integración por partes", "∫u dv = u·v - ∫v du",
            "u se elige con LIATE: Logarítmica, Inversa, Algebraica, Trigonométrica, Exponencial.", "x·e^x",
            TecnicaIntegracion.PARTES, ReglaCalculo.PARTES),
    I_FRACCIONES(Categoria.INTEGRALES, Operacion.INTEGRAL, "Fracciones parciales",
            "P/Q = A/(x - r) + B/(x - s) + ...", "Para cocientes de polinomios: se factoriza Q y se separa en "
                    + "fracciones simples.", "(3x + 5)/(x^2 + x - 2)", TecnicaIntegracion.FRACCIONES,
            ReglaCalculo.FRACCIONES_PARCIALES),
    I_SUSTITUCION_TRIG(Categoria.INTEGRALES, Operacion.INTEGRAL, "Sustitución trigonométrica",
            "√(a²-x²): x = a·sinθ   √(a²+x²): x = a·tanθ   √(x²-a²): x = a·secθ",
            "Se usa una identidad pitagórica para eliminar la raíz.", "√(9 - x^2)", TecnicaIntegracion.SUSTITUCION_TRIG,
            ReglaCalculo.SUSTITUCION_TRIG),
    I_TFC(Categoria.INTEGRALES, Operacion.INTEGRAL_DEFINIDA, "Teorema Fundamental del Cálculo",
            "∫[a,b] f dx = F(b) - F(a)", "Se obtiene la antiderivada F y se evalúa en los límites.", "x^2",
            TecnicaIntegracion.AUTOMATICO, ReglaCalculo.TFC),
    I_LIMITES_IGUALES(Categoria.INTEGRALES, Operacion.LIMITES_IGUALES, "Propiedad de límites iguales",
            "∫[a,a] f dx = 0", "Si los dos límites son iguales no hay área.", "x^3 + 1", TecnicaIntegracion.AUTOMATICO,
            ReglaCalculo.LIMITES_IGUALES),
    I_INVERSION(Categoria.INTEGRALES, Operacion.INVERSION, "Propiedad de inversión de límites",
            "∫[a,b] f dx = -∫[b,a] f dx", "Al intercambiar los límites la integral cambia de signo.", "x^2",
            TecnicaIntegracion.AUTOMATICO, ReglaCalculo.INVERSION_LIMITES),
    I_ADITIVIDAD(Categoria.INTEGRALES, Operacion.ADITIVIDAD, "Propiedad de aditividad del intervalo",
            "∫[a,b] f dx = ∫[a,c] f dx + ∫[c,b] f dx", "El intervalo se parte en un punto c intermedio.", "x^2",
            TecnicaIntegracion.AUTOMATICO, ReglaCalculo.ADITIVIDAD),
    I_IMPROPIA(Categoria.INTEGRALES, Operacion.INTEGRAL_DEFINIDA, "Integrales impropias (límites ∞)",
            "∫[a,∞) f dx = lim(t→∞) ∫[a,t] f dx",
            "Límites infinitos o funciones que se van a infinito en el intervalo. Use ∞ o -∞ del teclado.", "1/x^2",
            TecnicaIntegracion.AUTOMATICO, ReglaCalculo.IMPROPIA),

    // ================================ APLICACIONES ================================
    A_LIMITE(Categoria.APLICACIONES, Operacion.LIMITE, "Límite de una función", "lim(x→a) f(x)",
            "Primero se sustituye directamente; si aparece 0/0 o ∞/∞ se usa L'Hôpital. Para límites laterales "
                    + "escriba el punto con + o - al final, por ejemplo 0+ o 0-.", "(x^2 - 1)/(x - 1)", null),
    A_TANGENTE(Categoria.APLICACIONES, Operacion.TANGENTE, "Recta tangente y normal",
            "y = f(a) + f'(a)·(x - a)",
            "La pendiente de la tangente es la derivada en el punto. La recta normal es perpendicular: su pendiente "
                    + "es -1/f'(a).", "x^2 - 2x + 3", null, ReglaCalculo.RECTA_TANGENTE),
    A_EXTREMOS(Categoria.APLICACIONES, Operacion.EXTREMOS, "Máximos, mínimos e inflexión",
            "f'(c) = 0  →  f''(c) > 0 mínimo,  f''(c) < 0 máximo",
            "Busca los puntos críticos en [a, b], los clasifica con la segunda derivada y encuentra los puntos de "
                    + "inflexión y los extremos absolutos del intervalo.", "x^3 - 3x", null,
            ReglaCalculo.PUNTOS_CRITICOS, ReglaCalculo.CRITERIO_SEGUNDA_DERIVADA),
    A_AREA_CURVAS(Categoria.APLICACIONES, Operacion.AREA_CURVAS, "Área entre dos curvas",
            "A = ∫[a,b] |f(x) - g(x)| dx",
            "Se resta la curva de abajo a la de arriba en cada tramo. Si deja a y b vacíos se usan los puntos donde "
                    + "se cortan las curvas.", "x", null, ReglaCalculo.AREA_ENTRE_CURVAS);

    /** Grupo de la lista. */
    public enum Categoria {
        /** Métodos de derivación (y límites). */
        DERIVADAS,
        /** Métodos y propiedades de integración. */
        INTEGRALES,
        /** Límites, recta tangente, extremos y área entre curvas. */
        APLICACIONES
    }

    /** Operación que realiza el método. */
    public enum Operacion {
        /** Derivada (parcial) de primer orden. */
        DERIVADA,
        /** Derivada de orden n. */
        ORDEN_SUPERIOR,
        /** Derivación implícita. */
        IMPLICITA,
        /** Límite con L'Hôpital. */
        LIMITE,
        /** Integral indefinida o definida (el usuario elige). */
        INTEGRAL,
        /** Integral definida obligatoria. */
        INTEGRAL_DEFINIDA,
        /** Propiedad ∫[a,a] = 0. */
        LIMITES_IGUALES,
        /** Propiedad de inversión de límites. */
        INVERSION,
        /** Propiedad de aditividad. */
        ADITIVIDAD,
        /** Recta tangente y normal. */
        TANGENTE,
        /** Máximos, mínimos e inflexión. */
        EXTREMOS,
        /** Área entre dos curvas. */
        AREA_CURVAS
    }

    private final Categoria categoria;
    private final Operacion operacion;
    private final String nombre;
    private final String formula;
    private final String descripcion;
    private final String ejemplo;
    private final TecnicaIntegracion tecnica;
    private final ReglaCalculo[] reglas;

    MetodoCalculo(Categoria categoria, Operacion operacion, String nombre, String formula, String descripcion,
                  String ejemplo, TecnicaIntegracion tecnica, ReglaCalculo... reglas) {
        this.categoria = categoria;
        this.operacion = operacion;
        this.nombre = nombre;
        this.formula = formula;
        this.descripcion = descripcion;
        this.ejemplo = ejemplo;
        this.tecnica = tecnica == null ? TecnicaIntegracion.AUTOMATICO : tecnica;
        this.reglas = reglas;
    }

    /** @return grupo */
    public Categoria getCategoria() {
        return categoria;
    }

    /** @return operación */
    public Operacion getOperacion() {
        return operacion;
    }

    /** @return nombre visible */
    public String getNombre() {
        return nombre;
    }

    /** @return fórmula */
    public String getFormula() {
        return formula;
    }

    /** @return descripción */
    public String getDescripcion() {
        return descripcion;
    }

    /** @return expresión de ejemplo */
    public String getEjemplo() {
        return ejemplo;
    }

    /** @return técnica de integración exigida */
    public TecnicaIntegracion getTecnica() {
        return tecnica;
    }

    /** @return reglas que se resaltan */
    public ReglaCalculo[] getReglas() {
        return reglas.clone();
    }

    /** @return true si se puede elegir la variable x/y */
    public boolean usaVariable() {
        return operacion == Operacion.DERIVADA || operacion == Operacion.ORDEN_SUPERIOR
                || categoria == Categoria.INTEGRALES;
    }

    /** @return true si pide la segunda función g(x) */
    public boolean usaFuncionG() {
        return operacion == Operacion.AREA_CURVAS;
    }

    /** @return true si pide el intervalo [a, b] (aunque no sea una integral) */
    public boolean usaIntervalo() {
        return operacion == Operacion.EXTREMOS || operacion == Operacion.AREA_CURVAS;
    }

    /** @return ejemplo de la segunda función g(x) */
    public String ejemploG() {
        return this == A_AREA_CURVAS ? "x^2" : "";
    }

    /** @return ejemplo del punto (límite o recta tangente) */
    public String ejemploPunto() {
        if (this == A_LIMITE) {
            return "1";
        }
        if (this == A_TANGENTE) {
            return "2";
        }
        return "0";
    }

    /** @return true si pide el orden n */
    public boolean usaOrden() {
        return operacion == Operacion.ORDEN_SUPERIOR;
    }

    /** @return true si pide el punto al que tiende x */
    public boolean usaPunto() {
        return operacion == Operacion.LIMITE || operacion == Operacion.TANGENTE;
    }

    /** @return true si el usuario elige entre integral indefinida y definida */
    public boolean usaTipoIntegral() {
        return operacion == Operacion.INTEGRAL;
    }

    /** @return true si siempre es integral definida */
    public boolean esDefinidaObligatoria() {
        return operacion == Operacion.INTEGRAL_DEFINIDA || operacion == Operacion.LIMITES_IGUALES
                || operacion == Operacion.INVERSION || operacion == Operacion.ADITIVIDAD;
    }

    /** @return true si pide el punto intermedio c */
    public boolean usaPuntoIntermedio() {
        return operacion == Operacion.ADITIVIDAD;
    }

    /** @return límite inferior del ejemplo */
    public String ejemploA() {
        if (this == A_AREA_CURVAS) {
            return "";
        }
        if (this == A_EXTREMOS) {
            return "-3";
        }
        if (this == I_IMPROPIA) {
            return "1";
        }
        if (this == I_LIMITES_IGUALES) {
            return "2";
        }
        if (this == I_INVERSION) {
            return "3";
        }
        return "0";
    }

    /** @return límite superior del ejemplo */
    public String ejemploB() {
        if (this == A_AREA_CURVAS) {
            return "";
        }
        if (this == A_EXTREMOS) {
            return "3";
        }
        if (this == I_IMPROPIA) {
            return "∞";
        }
        if (this == I_LIMITES_IGUALES) {
            return "2";
        }
        if (this == I_INVERSION) {
            return "0";
        }
        return "3";
    }

    @Override
    public String toString() {
        return nombre;
    }
}
