package calculadora.simbolico;

import calculadora.modelo.Variable;

/**
 * Aplicación de una función elemental a un argumento: sin(u), e^u, ln(u), √(u)...
 */
public class NodoFuncion extends Nodo {

    private final TipoFuncion tipo;
    private final Nodo argumento;

    /**
     * @param tipo      función
     * @param argumento argumento
     */
    public NodoFuncion(TipoFuncion tipo, Nodo argumento) {
        this.tipo = tipo;
        this.argumento = argumento;
    }

    /** @return tipo de función */
    public TipoFuncion getTipo() {
        return tipo;
    }

    /** @return argumento */
    public Nodo getArgumento() {
        return argumento;
    }

    @Override
    public double evaluar(double x, double y) {
        return tipo.aplicar(argumento.evaluar(x, y));
    }

    @Override
    public boolean dependeDe(Variable v) {
        return argumento.dependeDe(v);
    }

    @Override
    public String clave() {
        return "(" + tipo.getNombre() + " " + argumento.clave() + ")";
    }

    @Override
    public int precedencia() {
        if (tipo == TipoFuncion.EXP) {
            return PREC_POTENCIA;
        }
        return PREC_ATOMO;
    }

    @Override
    public Nodo reemplazar(Nodo objetivo, Nodo nuevo) {
        if (igualA(objetivo)) {
            return nuevo;
        }
        return new NodoFuncion(tipo, argumento.reemplazar(objetivo, nuevo));
    }

    @Override
    public String toString() {
        if (tipo == TipoFuncion.EXP) {
            boolean simple = argumento instanceof NodoVariable
                    || (argumento.esNumero() && argumento.precedencia() == PREC_ATOMO);
            if (simple) {
                return "e^" + argumento;
            }
            return "e^(" + argumento + ")";
        }
        if (tipo == TipoFuncion.RAIZ) {
            return "√(" + argumento + ")";
        }
        if (tipo == TipoFuncion.ABS) {
            return "|" + argumento + "|";
        }
        if (tipo == TipoFuncion.LN && argumento instanceof NodoFuncion
                && ((NodoFuncion) argumento).getTipo() == TipoFuncion.ABS) {
            return "ln" + argumento;
        }
        return tipo.getNombre() + "(" + argumento + ")";
    }
}
