package calculadora.simbolico;

/**
 * Suma de dos expresiones: a + b.
 */
public class NodoSuma extends NodoBinario {

    /**
     * @param a primer sumando
     * @param b segundo sumando
     */
    public NodoSuma(Nodo a, Nodo b) {
        super(a, b);
    }

    @Override
    public double evaluar(double x, double y) {
        return izquierdo.evaluar(x, y) + derecho.evaluar(x, y);
    }

    @Override
    protected String operador() {
        return "+";
    }

    @Override
    protected Nodo crear(Nodo a, Nodo b) {
        return new NodoSuma(a, b);
    }

    @Override
    public int precedencia() {
        return PREC_SUMA;
    }

    @Override
    public String toString() {
        String textoDerecho = derecho.toString();
        if (textoDerecho.startsWith("-")) {
            return izquierdo + " - " + textoDerecho.substring(1);
        }
        return izquierdo + " + " + textoDerecho;
    }
}
