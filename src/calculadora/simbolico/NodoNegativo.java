package calculadora.simbolico;

import calculadora.modelo.Variable;

/**
 * Cambio de signo: −a.
 */
public class NodoNegativo extends Nodo {

    private final Nodo argumento;

    /**
     * @param argumento expresión a negar
     */
    public NodoNegativo(Nodo argumento) {
        this.argumento = argumento;
    }

    /** @return expresión negada */
    public Nodo getArgumento() {
        return argumento;
    }

    @Override
    public double evaluar(double x, double y) {
        return -argumento.evaluar(x, y);
    }

    @Override
    public boolean dependeDe(Variable v) {
        return argumento.dependeDe(v);
    }

    @Override
    public String clave() {
        return "(neg " + argumento.clave() + ")";
    }

    @Override
    public int precedencia() {
        return PREC_PRODUCTO;
    }

    @Override
    public Nodo reemplazar(Nodo objetivo, Nodo nuevo) {
        if (igualA(objetivo)) {
            return nuevo;
        }
        return new NodoNegativo(argumento.reemplazar(objetivo, nuevo));
    }

    @Override
    public String toString() {
        String texto = argumento.textoCon(PREC_PRODUCTO);
        if (texto.startsWith("-")) {
            return "-(" + texto + ")";
        }
        return "-" + texto;
    }
}
