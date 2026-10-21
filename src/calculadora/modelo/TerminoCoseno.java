package calculadora.modelo;

/**
 * Término con coseno: c · x^a · y^b · cos(k·w).
 * <ul>
 *   <li>Derivada (regla de la cadena): d/dv [c·cos(k·v)] = -c·k·sin(k·v)</li>
 *   <li>Integral inmediata: ∫ c·cos(k·v) dv = (c/k)·sin(k·v)</li>
 * </ul>
 */
public class TerminoCoseno extends TerminoFuncional {

    /**
     * @param coeficiente     coeficiente c
     * @param exponenteX      exponente de x
     * @param exponenteY      exponente de y
     * @param variableFuncion variable dentro del coseno
     * @param constanteK      constante k del argumento
     */
    public TerminoCoseno(double coeficiente, double exponenteX, double exponenteY,
                         Variable variableFuncion, double constanteK) {
        super(coeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected double aplicarFuncion(double argumento) {
        return Math.cos(argumento);
    }

    @Override
    public String nombreFuncion() {
        return "Coseno";
    }

    @Override
    protected String textoFuncion() {
        return "cos(" + textoArgumento() + ")";
    }

    /** d/dv [c·cos(k·v)] = -c·k·sin(k·v). */
    @Override
    protected Termino derivarFuncion() {
        double nuevoCoeficiente = -coeficiente * constanteK;
        return new TerminoSeno(nuevoCoeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected String formulaDerivada() {
        String v = variableFuncion.getSimbolo();
        return "d/d" + v + "[c·cos(k·" + v + ")] = -c·k·sin(k·" + v + ")";
    }

    /** ∫ c·cos(k·v) dv = (c/k)·sin(k·v). */
    @Override
    protected Termino integrarFuncion() {
        double nuevoCoeficiente = coeficiente / constanteK;
        return new TerminoSeno(nuevoCoeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected String formulaIntegral() {
        String v = variableFuncion.getSimbolo();
        return "∫ c·cos(k·" + v + ") d" + v + " = (c/k)·sin(k·" + v + ")";
    }

    @Override
    public Termino conParteAlgebraica(double nuevoCoeficiente, double nuevoExpX, double nuevoExpY) {
        return new TerminoCoseno(nuevoCoeficiente, nuevoExpX, nuevoExpY, variableFuncion, constanteK);
    }
}
