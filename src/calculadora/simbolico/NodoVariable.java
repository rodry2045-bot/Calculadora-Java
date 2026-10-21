package calculadora.simbolico;

import calculadora.modelo.Variable;

/**
 * Hoja del árbol que representa una variable (x, y, o las auxiliares u y θ).
 */
public class NodoVariable extends Nodo {

    private final Variable variable;

    /**
     * @param variable variable representada
     */
    public NodoVariable(Variable variable) {
        this.variable = variable;
    }

    /** @return la variable representada */
    public Variable getVariable() {
        return variable;
    }

    @Override
    public double evaluar(double x, double y) {
        if (variable == Variable.X) {
            return x;
        }
        if (variable == Variable.Y) {
            return y;
        }
        // u y θ solo aparecen durante un cambio de variable, cuando x ya no
        // está en la expresión: se evalúan en la misma posición que x
        return x;
    }

    @Override
    public boolean dependeDe(Variable v) {
        return variable == v;
    }

    @Override
    public String clave() {
        return "V:" + variable.getSimbolo();
    }

    @Override
    public int precedencia() {
        return PREC_ATOMO;
    }

    @Override
    public Nodo reemplazar(Nodo objetivo, Nodo nuevo) {
        if (igualA(objetivo)) {
            return nuevo;
        }
        return this;
    }

    @Override
    public String toString() {
        return variable.getSimbolo();
    }
}
