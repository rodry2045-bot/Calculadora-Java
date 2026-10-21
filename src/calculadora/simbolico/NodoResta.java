package calculadora.simbolico;

/**
 * Resta de dos expresiones: a − b.
 */
public class NodoResta extends NodoBinario {

    /**
     * @param a minuendo
     * @param b sustraendo
     */
    public NodoResta(Nodo a, Nodo b) {
        super(a, b);
    }

    @Override
    public double evaluar(double x, double y) {
        return izquierdo.evaluar(x, y) - derecho.evaluar(x, y);
    }

    @Override
    protected String operador() {
        return "-";
    }

    @Override
    protected Nodo crear(Nodo a, Nodo b) {
        return new NodoResta(a, b);
    }

    @Override
    public int precedencia() {
        return PREC_SUMA;
    }

    @Override
    public String toString() {
        String textoDerecho = derecho.toString();
        if (derecho.precedencia() <= PREC_SUMA || textoDerecho.startsWith("-")) {
            textoDerecho = "(" + textoDerecho + ")";
        }
        return izquierdo + " - " + textoDerecho;
    }
}
