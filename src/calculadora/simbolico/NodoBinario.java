package calculadora.simbolico;

import calculadora.modelo.Variable;

/**
 * Base de los nodos con dos hijos (suma, resta, producto, cociente y potencia).
 */
public abstract class NodoBinario extends Nodo {

    /** Hijo izquierdo. */
    protected final Nodo izquierdo;
    /** Hijo derecho. */
    protected final Nodo derecho;

    /**
     * @param izquierdo hijo izquierdo
     * @param derecho   hijo derecho
     */
    protected NodoBinario(Nodo izquierdo, Nodo derecho) {
        this.izquierdo = izquierdo;
        this.derecho = derecho;
    }

    /** @return hijo izquierdo */
    public Nodo getIzquierdo() {
        return izquierdo;
    }

    /** @return hijo derecho */
    public Nodo getDerecho() {
        return derecho;
    }

    /** @return símbolo de la operación para la clave */
    protected abstract String operador();

    /**
     * Crea un nodo del mismo tipo con otros hijos.
     *
     * @param a nuevo hijo izquierdo
     * @param b nuevo hijo derecho
     * @return nodo nuevo
     */
    protected abstract Nodo crear(Nodo a, Nodo b);

    @Override
    public boolean dependeDe(Variable v) {
        return izquierdo.dependeDe(v) || derecho.dependeDe(v);
    }

    @Override
    public String clave() {
        return "(" + operador() + " " + izquierdo.clave() + " " + derecho.clave() + ")";
    }

    @Override
    public Nodo reemplazar(Nodo objetivo, Nodo nuevo) {
        if (igualA(objetivo)) {
            return nuevo;
        }
        return crear(izquierdo.reemplazar(objetivo, nuevo), derecho.reemplazar(objetivo, nuevo));
    }
}
