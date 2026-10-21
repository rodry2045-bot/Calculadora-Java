package calculadora.modelo;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.OperacionNoSoportadaException;
import calculadora.util.FormatoNumeros;

/**
 * Término que contiene una función trascendental de argumento lineal:
 * <pre>
 *     c · x^a · y^b · f(k · w)
 * </pre>
 * donde f puede ser seno, coseno, exponencial o logaritmo natural,
 * k es una constante distinta de cero y w es la variable de la función.
 * <p>
 * Esta clase implementa el "esqueleto" común de la derivación y la
 * integración (patrón de diseño Método Plantilla). Cada subclase solo
 * aporta lo específico de su función: la regla de la cadena para derivar
 * y la integral inmediata.
 * </p>
 * <p>
 * Casos que se resuelven aquí:
 * </p>
 * <ul>
 *   <li>Si w NO es la variable de trabajo v, la función completa se trata como
 *       constante y solo se aplica la regla de la potencia a la parte en v.
 *       Ejemplo: d/dy [y·sin(x)] = sin(x).</li>
 *   <li>Si w ES la variable de trabajo y la parte algebraica no contiene a v,
 *       se aplica la regla de la cadena o la integral inmediata.
 *       Ejemplo: d/dx [y·sin(x)] = y·cos(x).</li>
 *   <li>Si w es la variable de trabajo y además hay una potencia de v
 *       (por ejemplo x·sin(x)), se necesitaría la regla del producto o la
 *       integración por partes, que aún no están soportadas.</li>
 * </ul>
 */
public abstract class TerminoFuncional extends Termino {

    /** Variable que aparece dentro de la función (w). */
    protected final Variable variableFuncion;

    /** Constante k que multiplica a la variable dentro de la función. */
    protected final double constanteK;

    /**
     * @param coeficiente     coeficiente c
     * @param exponenteX      exponente de x en la parte algebraica
     * @param exponenteY      exponente de y en la parte algebraica
     * @param variableFuncion variable w dentro de la función
     * @param constanteK      constante k del argumento k·w (distinta de cero)
     */
    protected TerminoFuncional(double coeficiente, double exponenteX, double exponenteY,
                               Variable variableFuncion, double constanteK) {
        super(coeficiente, exponenteX, exponenteY);
        this.variableFuncion = variableFuncion;
        this.constanteK = constanteK;
    }

    // ------------------------------------------------------------------
    // Métodos específicos que implementa cada función
    // ------------------------------------------------------------------

    /**
     * Aplica la función matemática a un argumento ya calculado (k·w).
     *
     * @param argumento valor de k·w
     * @return f(argumento)
     */
    protected abstract double aplicarFuncion(double argumento);

    /** @return nombre de la función en español, por ejemplo "Seno" */
    public abstract String nombreFuncion();

    /** @return texto de la función, por ejemplo "sin(2x)" */
    protected abstract String textoFuncion();

    /**
     * Regla de la cadena propia de la función. Se llama solo cuando la
     * parte algebraica NO contiene a la variable de la función.
     *
     * @return derivada respecto a la variable de la función
     */
    protected abstract Termino derivarFuncion();

    /** @return fórmula de la regla de derivación de esta función */
    protected abstract String formulaDerivada();

    /**
     * Integral inmediata propia de la función (cambio de variable u = k·w).
     *
     * @return antiderivada respecto a la variable de la función
     * @throws CalculadoraException si la integral no está soportada
     */
    protected abstract Termino integrarFuncion() throws CalculadoraException;

    /** @return fórmula de la regla de integración de esta función */
    protected abstract String formulaIntegral();

    // ------------------------------------------------------------------
    // Métodos comunes
    // ------------------------------------------------------------------

    /** @return la variable que aparece dentro de la función */
    public Variable getVariableFuncion() {
        return variableFuncion;
    }

    /** @return la constante k del argumento k·w */
    public double getConstanteK() {
        return constanteK;
    }

    @Override
    public double evaluar(double x, double y) {
        double valorVariable = x;
        if (variableFuncion == Variable.Y) {
            valorVariable = y;
        }
        double argumento = constanteK * valorVariable;
        return evaluarParteAlgebraica(x, y) * aplicarFuncion(argumento);
    }

    @Override
    public boolean dependeDe(Variable v) {
        if (variableFuncion == v) {
            return true;
        }
        return super.dependeDe(v);
    }

    /**
     * Texto del argumento k·w, por ejemplo "2x", "x", "-y" o "(1/2)x".
     *
     * @return argumento en texto
     */
    protected String textoArgumento() {
        return FormatoNumeros.formatearCoeficiente(constanteK) + variableFuncion.getSimbolo();
    }

    /**
     * @param v variable de trabajo
     * @return true si la parte algebraica contiene a v y además la función depende de v
     */
    private boolean requiereReglaDelProducto(Variable v) {
        boolean funcionDependeDeV = (variableFuncion == v);
        boolean parteAlgebraicaDependeDeV = !FormatoNumeros.sonIguales(getExponente(v), 0.0);
        return funcionDependeDeV && parteAlgebraicaDependeDeV;
    }

    // ------------------------------------------------------------------
    // DERIVACIÓN (método plantilla)
    // ------------------------------------------------------------------

    @Override
    public Termino derivar(Variable v) throws CalculadoraException {
        if (variableFuncion != v) {
            // La función no depende de v: es un coeficiente constante.
            return derivarParteAlgebraica(v);
        }
        if (requiereReglaDelProducto(v)) {
            throw new OperacionNoSoportadaException("El término " + this
                    + " es un producto de dos funciones de " + v.getSimbolo()
                    + ". Requiere la regla del producto, aún no soportado.");
        }
        return derivarFuncion();
    }

    @Override
    public String describirDerivada(Variable v) {
        String simbolo = v.getSimbolo();
        if (variableFuncion != v) {
            double n = getExponente(v);
            if (FormatoNumeros.sonIguales(n, 0.0)) {
                return "Variable tratada como Constante: el término " + this + " no contiene a "
                        + simbolo + " (la función " + textoFuncion() + " depende de "
                        + variableFuncion.getSimbolo() + "). Todo el término es constante y su derivada es 0.";
            }
            return "Variable tratada como Constante + Regla de la Potencia: la función " + textoFuncion()
                    + " depende de " + variableFuncion.getSimbolo() + " y actúa como coeficiente constante. "
                    + "Se multiplica el coeficiente por el exponente n = " + FormatoNumeros.formatear(n)
                    + " y se resta 1 al exponente. Resultado: " + textoDerivadaSegura(v) + ".";
        }
        if (requiereReglaDelProducto(v)) {
            return "El término " + this + " requiere la Regla del Producto (aún no soportada).";
        }
        return "Regla de la Cadena (" + nombreFuncion() + "): " + formulaDerivada()
                + ". Aquí c = " + FormatoNumeros.formatear(coeficiente) + " y k = "
                + FormatoNumeros.formatear(constanteK) + ". Resultado: " + textoDerivadaSegura(v) + "."
                + notaOtraVariable(v);
    }

    /**
     * @param v variable de derivación
     * @return texto de la derivada o un aviso si no es posible calcularla
     */
    private String textoDerivadaSegura(Variable v) {
        try {
            Termino derivada = derivar(v);
            if (derivada.esCero()) {
                return "0";
            }
            return derivada.toString();
        } catch (CalculadoraException e) {
            return "(no disponible)";
        }
    }

    // ------------------------------------------------------------------
    // INTEGRACIÓN (método plantilla)
    // ------------------------------------------------------------------

    @Override
    public Termino integrar(Variable v) throws CalculadoraException {
        if (variableFuncion != v) {
            double n = getExponente(v);
            if (FormatoNumeros.sonIguales(n, -1.0)) {
                throw new OperacionNoSoportadaException("La integral de " + this
                        + " produciría el producto ln|" + v.getSimbolo() + "|·" + textoFuncion()
                        + ", que aún no se puede representar como un solo término.");
            }
            // La función es constante respecto a v: regla de la potencia sobre la parte en v.
            return integrarParteAlgebraica(v);
        }
        if (requiereReglaDelProducto(v)) {
            throw new OperacionNoSoportadaException("La integral de " + this
                    + " requiere integración por partes, aún no soportado.");
        }
        return integrarFuncion();
    }

    @Override
    public String describirIntegral(Variable v) {
        String simbolo = v.getSimbolo();
        String resultado;
        try {
            resultado = integrar(v).toString();
        } catch (CalculadoraException e) {
            return "No se puede integrar " + this + ": " + e.getMessage();
        }
        if (variableFuncion != v) {
            return "Variable tratada como Constante + Potencia: la función " + textoFuncion()
                    + " no depende de " + simbolo + " y actúa como coeficiente. Se suma 1 al exponente de "
                    + simbolo + " y se divide el coeficiente entre el nuevo exponente. Resultado: "
                    + resultado + ".";
        }
        return "Integral inmediata de " + nombreFuncion() + " (cambio de variable implícito u = "
                + textoArgumento() + ", du = " + FormatoNumeros.formatear(constanteK) + " d" + simbolo
                + "): " + formulaIntegral() + ". Aquí c = " + FormatoNumeros.formatear(coeficiente)
                + " y k = " + FormatoNumeros.formatear(constanteK) + ". Resultado: " + resultado + "."
                + notaOtraVariable(v);
    }

    // ------------------------------------------------------------------
    // Otros
    // ------------------------------------------------------------------

    @Override
    public String firmaSemejanza() {
        return getClass().getSimpleName() + "|" + firmaParteAlgebraica() + "|"
                + variableFuncion.getSimbolo() + "|k" + FormatoNumeros.formatearDecimal(constanteK, 9);
    }

    @Override
    public String toString() {
        return construirTexto(textoFuncion());
    }
}
