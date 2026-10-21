package calculadora.modelo;

import calculadora.util.FormatoNumeros;

/**
 * Término con exponencial natural: c · x^a · y^b · e^(k·w).
 * <ul>
 *   <li>Derivada (regla de la cadena): d/dv [c·e^(k·v)] = c·k·e^(k·v)</li>
 *   <li>Integral inmediata: ∫ c·e^(k·v) dv = (c/k)·e^(k·v)</li>
 * </ul>
 */
public class TerminoExponencial extends TerminoFuncional {

    /**
     * @param coeficiente     coeficiente c
     * @param exponenteX      exponente de x
     * @param exponenteY      exponente de y
     * @param variableFuncion variable del exponente
     * @param constanteK      constante k del exponente
     */
    public TerminoExponencial(double coeficiente, double exponenteX, double exponenteY,
                              Variable variableFuncion, double constanteK) {
        super(coeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected double aplicarFuncion(double argumento) {
        return Math.exp(argumento);
    }

    @Override
    public String nombreFuncion() {
        return "Exponencial";
    }

    @Override
    protected String textoFuncion() {
        if (FormatoNumeros.sonIguales(constanteK, 1.0)) {
            return "e^" + variableFuncion.getSimbolo();
        }
        return "e^(" + textoArgumento() + ")";
    }

    /** d/dv [c·e^(k·v)] = c·k·e^(k·v). */
    @Override
    protected Termino derivarFuncion() {
        double nuevoCoeficiente = coeficiente * constanteK;
        return new TerminoExponencial(nuevoCoeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected String formulaDerivada() {
        String v = variableFuncion.getSimbolo();
        return "d/d" + v + "[c·e^(k·" + v + ")] = c·k·e^(k·" + v + ")";
    }

    /** ∫ c·e^(k·v) dv = (c/k)·e^(k·v). */
    @Override
    protected Termino integrarFuncion() {
        double nuevoCoeficiente = coeficiente / constanteK;
        return new TerminoExponencial(nuevoCoeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
    }

    @Override
    protected String formulaIntegral() {
        String v = variableFuncion.getSimbolo();
        return "∫ c·e^(k·" + v + ") d" + v + " = (c/k)·e^(k·" + v + ")";
    }

    @Override
    public Termino conParteAlgebraica(double nuevoCoeficiente, double nuevoExpX, double nuevoExpY) {
        return new TerminoExponencial(nuevoCoeficiente, nuevoExpX, nuevoExpY, variableFuncion, constanteK);
    }
}
