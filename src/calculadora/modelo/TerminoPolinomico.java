package calculadora.modelo;

import calculadora.excepciones.CalculadoraException;
import calculadora.util.FormatoNumeros;

/**
 * Término polinómico (monomio) de la forma c · x^a · y^b.
 * <p>
 * Ejemplos: 5 (constante), 3x^2, -x^(-1), 4x^2y.
 * </p>
 * Implementa la Regla de la Constante, la Regla de la Potencia y el caso
 * especial n = -1 de la integral, cuyo resultado es un logaritmo natural.
 */
public class TerminoPolinomico extends Termino {

    /**
     * Crea un monomio c · x^a · y^b.
     *
     * @param coeficiente coeficiente c
     * @param exponenteX  exponente de x
     * @param exponenteY  exponente de y
     */
    public TerminoPolinomico(double coeficiente, double exponenteX, double exponenteY) {
        super(coeficiente, exponenteX, exponenteY);
    }

    /**
     * Crea un término constante (sin variables).
     *
     * @param constante valor de la constante
     * @return término constante
     */
    public static TerminoPolinomico constante(double constante) {
        return new TerminoPolinomico(constante, 0.0, 0.0);
    }

    @Override
    public double evaluar(double x, double y) {
        return evaluarParteAlgebraica(x, y);
    }

    // ------------------------------------------------------------------
    // DERIVACIÓN
    // ------------------------------------------------------------------

    /**
     * Regla de la constante y regla de la potencia.
     */
    @Override
    public Termino derivar(Variable v) {
        return derivarParteAlgebraica(v);
    }

    @Override
    public String describirDerivada(Variable v) {
        double n = getExponente(v);
        String simbolo = v.getSimbolo();

        // Caso 1: el término no contiene a v
        if (FormatoNumeros.sonIguales(n, 0.0)) {
            boolean dependeDeOtra = dependeDe(v.otra());
            if (!dependeDeOtra) {
                return "Regla de la Constante: d/d" + simbolo + "(" + this + ") = 0. "
                        + "La derivada de una constante pura es 0.";
            }
            return "Variable tratada como Constante: el término " + this + " solo depende de "
                    + v.otra().getSimbolo() + ", que se considera constante al derivar respecto a "
                    + simbolo + "; por eso su derivada es 0.";
        }

        // Caso 2: regla de la potencia
        Termino derivada = derivar(v);
        return "Regla de la Potencia: d/d" + simbolo + "(c·" + simbolo + "^n) = c·n·" + simbolo
                + "^(n-1). Multiplicar el coeficiente c = " + FormatoNumeros.formatear(coeficiente)
                + " por el exponente n = " + FormatoNumeros.formatear(n)
                + " y restar 1 al exponente (" + FormatoNumeros.formatear(n) + " - 1 = "
                + FormatoNumeros.formatear(n - 1.0) + "). Resultado: " + textoSeguro(derivada) + "."
                + notaOtraVariable(v);
    }

    // ------------------------------------------------------------------
    // INTEGRACIÓN
    // ------------------------------------------------------------------

    /**
     * Regla de la potencia para integrales; si n = -1 el resultado es c·ln|v|.
     */
    @Override
    public Termino integrar(Variable v) throws CalculadoraException {
        double n = getExponente(v);
        if (FormatoNumeros.sonIguales(n, -1.0)) {
            // ∫ c · v^(-1) dv = c · ln|v|
            // La parte en v desaparece (se transforma en logaritmo) y la otra variable se conserva.
            double nuevoExpX = exponenteX;
            double nuevoExpY = exponenteY;
            if (v == Variable.X) {
                nuevoExpX = 0.0;
            } else {
                nuevoExpY = 0.0;
            }
            return new TerminoLogaritmico(coeficiente, nuevoExpX, nuevoExpY, v, 1.0, true);
        }
        return integrarParteAlgebraica(v);
    }

    @Override
    public String describirIntegral(Variable v) {
        double n = getExponente(v);
        String simbolo = v.getSimbolo();
        String resultado;
        try {
            resultado = integrar(v).toString();
        } catch (CalculadoraException e) {
            resultado = "(no disponible: " + e.getMessage() + ")";
        }

        if (FormatoNumeros.sonIguales(n, -1.0)) {
            return "Integral del Logaritmo Natural (exponente n = -1): ∫ c/" + simbolo + " d" + simbolo
                    + " = c·ln|" + simbolo + "|. Para exponente -1 (1/" + simbolo
                    + "), la antiderivada es el logaritmo natural del valor absoluto. Resultado: "
                    + resultado + "." + notaOtraVariable(v);
        }
        if (FormatoNumeros.sonIguales(n, 0.0)) {
            return "Integral de una constante (potencia con n = 0): ∫ c d" + simbolo + " = c·" + simbolo
                    + ". Sumar 1 al exponente (0 + 1 = 1) y dividir el coeficiente entre 1. Resultado: "
                    + resultado + "." + notaOtraVariable(v);
        }
        return "Integral Indefinida de la Potencia: ∫ c·" + simbolo + "^n d" + simbolo + " = c/(n+1)·"
                + simbolo + "^(n+1). Sumar 1 al exponente (" + FormatoNumeros.formatear(n) + " + 1 = "
                + FormatoNumeros.formatear(n + 1.0) + ") y dividir el coeficiente "
                + FormatoNumeros.formatear(coeficiente) + " entre el nuevo exponente. Resultado: "
                + resultado + "." + notaOtraVariable(v);
    }

    // ------------------------------------------------------------------
    // Otros
    // ------------------------------------------------------------------

    @Override
    public Termino conParteAlgebraica(double nuevoCoeficiente, double nuevoExpX, double nuevoExpY) {
        return new TerminoPolinomico(nuevoCoeficiente, nuevoExpX, nuevoExpY);
    }

    @Override
    public String firmaSemejanza() {
        return "POLINOMICO|" + firmaParteAlgebraica();
    }

    @Override
    public String toString() {
        return construirTexto("");
    }

    /**
     * Convierte un término a texto mostrando "0" si es nulo.
     *
     * @param termino término a mostrar
     * @return texto del término
     */
    private static String textoSeguro(Termino termino) {
        if (termino.esCero()) {
            return "0";
        }
        return termino.toString();
    }
}
