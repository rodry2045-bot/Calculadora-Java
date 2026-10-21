package calculadora.simbolico;

import calculadora.util.FormatoNumeros;

/**
 * Potencia: base ^ exponente.
 */
public class NodoPotencia extends NodoBinario {

    /**
     * @param base      base
     * @param exponente exponente
     */
    public NodoPotencia(Nodo base, Nodo exponente) {
        super(base, exponente);
    }

    /** @return base */
    public Nodo getBase() {
        return izquierdo;
    }

    /** @return exponente */
    public Nodo getExponente() {
        return derecho;
    }

    @Override
    public double evaluar(double x, double y) {
        double b = izquierdo.evaluar(x, y);
        double e = derecho.evaluar(x, y);
        // Raíces impares de números negativos: (-8)^(1/3) = -2
        if (b < 0 && !FormatoNumeros.esEntero(e)) {
            long[] fraccion = FormatoNumeros.comoFraccion(e);
            if (fraccion != null && fraccion[1] % 2 == 1) {
                double magnitud = Math.pow(-b, e);
                if (fraccion[0] % 2 == 0) {
                    return magnitud;
                }
                return -magnitud;
            }
        }
        return Math.pow(b, e);
    }

    @Override
    protected String operador() {
        return "^";
    }

    @Override
    protected Nodo crear(Nodo a, Nodo b) {
        return new NodoPotencia(a, b);
    }

    @Override
    public int precedencia() {
        return PREC_POTENCIA;
    }

    @Override
    public String toString() {
        String textoBase = izquierdo.textoCon(PREC_ATOMO);
        String textoExponente = derecho.toString();
        boolean simple = derecho instanceof NodoVariable
                || (derecho.esNumero() && derecho.precedencia() == PREC_ATOMO);
        if (!simple) {
            textoExponente = "(" + textoExponente + ")";
        }
        return textoBase + "^" + textoExponente;
    }
}
