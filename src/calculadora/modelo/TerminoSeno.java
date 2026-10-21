package calculadora.modelo;

/**
 * Término con seno: c · x^a · y^b · sin(k·w).
 * <ul>
 *   <li>Derivada (regla de la cadena): d/dv [c·sin(k·v)] = c·k·cos(k·v)</li>
 *   <li>Integral inmediata: ∫ c·sin(k·v) dv = -(c/k)·cos(k·v)</li>
 * </ul>
 */
public class TerminoSeno extends TerminoFuncional {

    /**
     * @param coeficiente     coeficiente c
     * @param exponenteX      exponente de x
     * @param exponenteY      exponente de y
     * @param variableFuncion variable dentro del seno
     * @param constanteK      constante k del argumento
     */
    public TerminoSeno(double coeficiente, double exponenteX, double exponenteY,
                       Variable variableFuncion, double constanteK) {
        super(coeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected double aplicarFuncion(double argumento) {
        return Math.sin(argumento);
    }

    @Override
    public String nombreFuncion() {
        return "Seno";
    }

    @Override
    protected String textoFuncion() {
        return "sin(" + textoArgumento() + ")";
    }

    /** d/dv [c·sin(k·v)] = c·k·cos(k·v). */
    @Override
    protected Termino derivarFuncion() {
        double nuevoCoeficiente = coeficiente * constanteK;
        return new TerminoCoseno(nuevoCoeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected String formulaDerivada() {
        String v = variableFuncion.getSimbolo();
        return "d/d" + v + "[c·sin(k·" + v + ")] = c·k·cos(k·" + v + ")";
    }

    /** ∫ c·sin(k·v) dv = -(c/k)·cos(k·v). */
    @Override
    protected Termino integrarFuncion() {
        double nuevoCoeficiente = -coeficiente / constanteK;
        return new TerminoCoseno(nuevoCoeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected String formulaIntegral() {
        String v = variableFuncion.getSimbolo();
        return "∫ c·sin(k·" + v + ") d" + v + " = -(c/k)·cos(k·" + v + ")";
    }

    @Override
    public Termino conParteAlgebraica(double nuevoCoeficiente, double nuevoExpX, double nuevoExpY) {
        return new TerminoSeno(nuevoCoeficiente, nuevoExpX, nuevoExpY, variableFuncion, constanteK);
    }
}
