package calculadora.simbolico;

import calculadora.modelo.Graficable;
import calculadora.modelo.Variable;

/**
 * Nodo de un ÁRBOL DE EXPRESIÓN (patrón de diseño Composite).
 * <p>
 * Cualquier fórmula se representa como un árbol. Por ejemplo,
 * {@code x^2·sin(3x)} es:
 * </p>
 * <pre>
 *            Producto
 *           /        \
 *      Potencia      Función sin
 *      /     \             |
 *     x       2         Producto
 *                        /    \
 *                       3      x
 * </pre>
 * <p>
 * A diferencia del motor por términos (paquete modelo), el árbol permite
 * representar CUALQUIER combinación: productos de funciones, cocientes,
 * funciones compuestas, etc. Por eso es la base de la regla del producto,
 * del cociente, de la cadena, de L'Hôpital y de los métodos de integración.
 * </p>
 * Los nodos son inmutables.
 */
public abstract class Nodo implements Graficable {

    /** Precedencia de suma y resta (la más baja). */
    protected static final int PREC_SUMA = 1;
    /** Precedencia de producto, cociente y negación. */
    protected static final int PREC_PRODUCTO = 2;
    /** Precedencia de la potencia. */
    protected static final int PREC_POTENCIA = 4;
    /** Precedencia de números, variables y funciones (la más alta). */
    protected static final int PREC_ATOMO = 5;

    /**
     * Evalúa numéricamente el árbol.
     *
     * @param x valor de x
     * @param y valor de y
     * @return valor numérico (NaN fuera del dominio)
     */
    public abstract double evaluar(double x, double y);

    /**
     * @param v variable consultada
     * @return true si v aparece en el árbol
     */
    @Override
    public abstract boolean dependeDe(Variable v);

    /**
     * Clave estructural única: dos árboles con la misma clave son iguales.
     *
     * @return clave en notación prefija
     */
    public abstract String clave();

    /** @return precedencia del nodo para decidir dónde poner paréntesis */
    public abstract int precedencia();

    /**
     * Reemplaza cada aparición del subárbol {@code objetivo} por {@code nuevo}.
     *
     * @param objetivo subárbol a buscar
     * @param nuevo    subárbol que lo sustituye
     * @return árbol nuevo con los reemplazos
     */
    public abstract Nodo reemplazar(Nodo objetivo, Nodo nuevo);

    /** @return texto legible de la expresión */
    @Override
    public abstract String toString();

    // ------------------------------------------------------------------
    // Métodos comunes
    // ------------------------------------------------------------------

    @Override
    public double evaluarEn(Variable variable, double valor, double valorOtra) {
        if (variable == Variable.Y) {
            return evaluar(valorOtra, valor);
        }
        return evaluar(valor, valorOtra);
    }

    /**
     * @param otro otro nodo
     * @return true si ambos árboles son estructuralmente iguales
     */
    public boolean igualA(Nodo otro) {
        return clave().equals(otro.clave());
    }

    /** @return true si es un número (sin símbolo como π o e) */
    public boolean esNumero() {
        return this instanceof NodoConstante && ((NodoConstante) this).getSimbolo() == null;
    }

    /**
     * @param valor número a comparar
     * @return true si este nodo es exactamente ese número
     */
    public boolean esNumero(double valor) {
        return esNumero() && Math.abs(((NodoConstante) this).getValor() - valor) < 1e-12;
    }

    /** @return true si no depende de ninguna variable */
    public boolean esConstante() {
        Variable[] todas = Variable.values();
        for (int i = 0; i < todas.length; i++) {
            if (dependeDe(todas[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Texto del nodo, con paréntesis si su precedencia es menor que la mínima.
     *
     * @param minima precedencia mínima que no necesita paréntesis
     * @return texto con o sin paréntesis
     */
    protected String textoCon(int minima) {
        if (precedencia() < minima) {
            return "(" + this + ")";
        }
        return toString();
    }

    // ------------------------------------------------------------------
    // Fábricas cortas (hacen el código del motor más legible)
    // ------------------------------------------------------------------

    /**
     * @param valor número
     * @return nodo constante
     */
    public static Nodo num(double valor) {
        return new NodoConstante(valor);
    }

    /**
     * @param v variable
     * @return nodo variable
     */
    public static Nodo var(Variable v) {
        return new NodoVariable(v);
    }

    /**
     * @param a sumando
     * @param b sumando
     * @return a + b
     */
    public static Nodo suma(Nodo a, Nodo b) {
        return new NodoSuma(a, b);
    }

    /**
     * @param a minuendo
     * @param b sustraendo
     * @return a − b
     */
    public static Nodo resta(Nodo a, Nodo b) {
        return new NodoResta(a, b);
    }

    /**
     * @param a factor
     * @param b factor
     * @return a · b
     */
    public static Nodo mul(Nodo a, Nodo b) {
        return new NodoProducto(a, b);
    }

    /**
     * @param a numerador
     * @param b denominador
     * @return a / b
     */
    public static Nodo div(Nodo a, Nodo b) {
        return new NodoCociente(a, b);
    }

    /**
     * @param base      base
     * @param exponente exponente
     * @return base ^ exponente
     */
    public static Nodo pot(Nodo base, Nodo exponente) {
        return new NodoPotencia(base, exponente);
    }

    /**
     * @param base      base
     * @param exponente exponente numérico
     * @return base ^ exponente
     */
    public static Nodo pot(Nodo base, double exponente) {
        return new NodoPotencia(base, new NodoConstante(exponente));
    }

    /**
     * @param a expresión
     * @return −a
     */
    public static Nodo neg(Nodo a) {
        return new NodoNegativo(a);
    }

    /**
     * @param tipo      función
     * @param argumento argumento
     * @return tipo(argumento)
     */
    public static Nodo fn(TipoFuncion tipo, Nodo argumento) {
        return new NodoFuncion(tipo, argumento);
    }
}
