package calculadora.simbolico;

import calculadora.util.FormatoNumeros;

/**
 * Producto de dos expresiones: a · b.
 * <p>
 * Si el primer factor es un número se escribe pegado (3x, 2sin(x));
 * en otro caso se usa el punto medio (x·sin(x)).
 * </p>
 */
public class NodoProducto extends NodoBinario {

    /**
     * @param a primer factor
     * @param b segundo factor
     */
    public NodoProducto(Nodo a, Nodo b) {
        super(a, b);
    }

    @Override
    public double evaluar(double x, double y) {
        return izquierdo.evaluar(x, y) * derecho.evaluar(x, y);
    }

    @Override
    protected String operador() {
        return "*";
    }

    @Override
    protected Nodo crear(Nodo a, Nodo b) {
        return new NodoProducto(a, b);
    }

    @Override
    public int precedencia() {
        return PREC_PRODUCTO;
    }

    @Override
    public String toString() {
        String textoDerecho = derecho.textoCon(PREC_PRODUCTO);
        if (textoDerecho.startsWith("-")) {
            textoDerecho = "(" + textoDerecho + ")";
        }
        if (izquierdo.esNumero()) {
            double c = ((NodoConstante) izquierdo).getValor();
            if (FormatoNumeros.sonIguales(c, 1.0)) {
                return textoDerecho;
            }
            if (FormatoNumeros.sonIguales(c, -1.0)) {
                return "-" + textoDerecho;
            }
            String coeficiente = FormatoNumeros.formatearCoeficiente(c);
            char primero = textoDerecho.charAt(0);
            if (Character.isDigit(primero) || primero == '.') {
                return coeficiente + "·" + textoDerecho;
            }
            return coeficiente + textoDerecho;
        }
        return izquierdo.textoCon(PREC_PRODUCTO) + "·" + textoDerecho;
    }
}
