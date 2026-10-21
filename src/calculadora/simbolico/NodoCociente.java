package calculadora.simbolico;

/**
 * Cociente de dos expresiones: a / b.
 */
public class NodoCociente extends NodoBinario {

    /**
     * @param a numerador
     * @param b denominador
     */
    public NodoCociente(Nodo a, Nodo b) {
        super(a, b);
    }

    /** @return numerador */
    public Nodo getNumerador() {
        return izquierdo;
    }

    /** @return denominador */
    public Nodo getDenominador() {
        return derecho;
    }

    @Override
    public double evaluar(double x, double y) {
        return izquierdo.evaluar(x, y) / derecho.evaluar(x, y);
    }

    @Override
    protected String operador() {
        return "/";
    }

    @Override
    protected Nodo crear(Nodo a, Nodo b) {
        return new NodoCociente(a, b);
    }

    @Override
    public int precedencia() {
        return PREC_PRODUCTO;
    }

    @Override
    public String toString() {
        return izquierdo.textoCon(PREC_PRODUCTO) + "/" + derecho.textoCon(PREC_POTENCIA);
    }
}
