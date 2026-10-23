package calculadora.simbolico;

/**
 * Funciones elementales que reconoce el motor simbólico.
 */
public enum TipoFuncion {
    /** Seno. */
    SIN("sin"),
    /** Coseno. */
    COS("cos"),
    /** Tangente. */
    TAN("tan"),
    /** Secante. */
    SEC("sec"),
    /** Cosecante. */
    CSC("csc"),
    /** Cotangente. */
    COT("cot"),
    /** Exponencial natural e^u. */
    EXP("exp"),
    /** Logaritmo natural. */
    LN("ln"),
    /** Logaritmo en base 10. */
    LOG("log"),
    /** Raíz cuadrada. */
    RAIZ("√"),
    /** Arcoseno. */
    ARCSIN("arcsin"),
    /** Arcocoseno. */
    ARCCOS("arccos"),
    /** Arcotangente. */
    ARCTAN("arctan"),
    /** Valor absoluto. */
    ABS("abs"),
    /** Seno hiperbólico. */
    SINH("sinh"),
    /** Coseno hiperbólico. */
    COSH("cosh"),
    /** Tangente hiperbólica. */
    TANH("tanh");

    private final String nombre;

    TipoFuncion(String nombre) {
        this.nombre = nombre;
    }

    /** @return nombre que se escribe en las expresiones */
    public String getNombre() {
        return nombre;
    }

    /** @return true para sin, cos, tan, sec, csc y cot */
    public boolean esTrigonometrica() {
        return this == SIN || this == COS || this == TAN || this == SEC || this == CSC || this == COT;
    }

    /** @return true para sinh, cosh y tanh */
    public boolean esHiperbolica() {
        return this == SINH || this == COSH || this == TANH;
    }

    /** @return true para arcsin, arccos y arctan */
    public boolean esInversa() {
        return this == ARCSIN || this == ARCCOS || this == ARCTAN;
    }

    /**
     * Evalúa la función.
     *
     * @param u argumento
     * @return valor de la función (NaN fuera del dominio)
     */
    public double aplicar(double u) {
        switch (this) {
            case SIN:
                return Math.sin(u);
            case COS:
                return Math.cos(u);
            case TAN:
                return Math.tan(u);
            case SEC:
                return 1.0 / Math.cos(u);
            case CSC:
                return 1.0 / Math.sin(u);
            case COT:
                return Math.cos(u) / Math.sin(u);
            case EXP:
                return Math.exp(u);
            case LN:
                return Math.log(u);
            case LOG:
                return Math.log10(u);
            case RAIZ:
                return Math.sqrt(u);
            case ARCSIN:
                return Math.asin(u);
            case ARCCOS:
                return Math.acos(u);
            case ARCTAN:
                return Math.atan(u);
            case SINH:
                return Math.sinh(u);
            case COSH:
                return Math.cosh(u);
            case TANH:
                return Math.tanh(u);
            default:
                return Math.abs(u);
        }
    }
}
