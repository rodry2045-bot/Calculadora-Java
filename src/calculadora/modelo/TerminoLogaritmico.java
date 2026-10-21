package calculadora.modelo;

import calculadora.excepciones.OperacionNoSoportadaException;

/**
 * Término con logaritmo natural: c · x^a · y^b · ln(k·w).
 * <p>
 * Puede mostrarse con valor absoluto, ln|k·w|, cuando proviene de la
 * integral de 1/w (exponente -1).
 * </p>
 * Reglas:
 * <ul>
 *   <li>Derivada: d/dv [c·ln(k·v)] = c/v = c·v^(-1). (La k se cancela por la regla de la cadena: k/(k·v) = 1/v.)</li>
 *   <li>Integral: ∫ ln(v) dv requiere integración por partes, aún no soportado.</li>
 * </ul>
 */
public class TerminoLogaritmico extends TerminoFuncional {

    /** Indica si se usa ln|k·w| (valor absoluto) en lugar de ln(k·w). */
    private final boolean valorAbsoluto;

    /**
     * @param coeficiente     coeficiente c
     * @param exponenteX      exponente de x
     * @param exponenteY      exponente de y
     * @param variableFuncion variable dentro del logaritmo
     * @param constanteK      constante k del argumento
     * @param valorAbsoluto   true para ln|k·w|, false para ln(k·w)
     */
    public TerminoLogaritmico(double coeficiente, double exponenteX, double exponenteY,
                              Variable variableFuncion, double constanteK, boolean valorAbsoluto) {
        super(coeficiente, exponenteX, exponenteY, variableFuncion, constanteK);
        this.valorAbsoluto = valorAbsoluto;
    }

    /** @return true si el logaritmo usa valor absoluto */
    public boolean isValorAbsoluto() {
        return valorAbsoluto;
    }

    @Override
    protected double aplicarFuncion(double argumento) {
        if (valorAbsoluto) {
            return Math.log(Math.abs(argumento));
        }
        // Para argumentos negativos Math.log devuelve NaN (fuera del dominio)
        return Math.log(argumento);
    }

    @Override
    public String nombreFuncion() {
        return "Logaritmo Natural";
    }

    @Override
    protected String textoFuncion() {
        if (valorAbsoluto) {
            return "ln|" + textoArgumento() + "|";
        }
        return "ln(" + textoArgumento() + ")";
    }

    /**
     * d/dv [c·ln(k·v)] = c·k/(k·v) = c·v^(-1).
     */
    @Override
    protected Termino derivarFuncion() {
        double nuevoExpX = exponenteX;
        double nuevoExpY = exponenteY;
        if (variableFuncion == Variable.X) {
            nuevoExpX = -1.0;
        } else {
            nuevoExpY = -1.0;
        }
        return new TerminoPolinomico(coeficiente, nuevoExpX, nuevoExpY);
    }

    @Override
    protected String formulaDerivada() {
        String v = variableFuncion.getSimbolo();
        return "d/d" + v + "[c·ln(k·" + v + ")] = c·k/(k·" + v + ") = c/" + v + " = c·" + v + "^(-1)";
    }

    /**
     * La integral de ln(v) requiere integración por partes.
     */
    @Override
    protected Termino integrarFuncion() throws OperacionNoSoportadaException {
        throw new OperacionNoSoportadaException("La integral de " + textoFuncion()
                + " requiere integración por partes, aún no soportado.");
    }

    @Override
    protected String formulaIntegral() {
        return "∫ ln(v) dv requiere integración por partes (no soportado)";
    }

    @Override
    public Termino conParteAlgebraica(double nuevoCoeficiente, double nuevoExpX, double nuevoExpY) {
        return new TerminoLogaritmico(nuevoCoeficiente, nuevoExpX, nuevoExpY,
                variableFuncion, constanteK, valorAbsoluto);
    }

    @Override
    public String firmaSemejanza() {
        String firma = super.firmaSemejanza();
        if (valorAbsoluto) {
            return firma + "|abs";
        }
        return firma;
    }
}
