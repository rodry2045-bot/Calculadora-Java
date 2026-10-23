package calculadora.simbolico;

import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

/**
 * Hoja del árbol que representa un número. Puede llevar un símbolo
 * ("π" o "e") para mostrarse de forma exacta.
 */
public class NodoConstante extends Nodo {

    /** Constante π. */
    public static final NodoConstante PI = new NodoConstante(Math.PI, "π");
    /** Constante de Euler e. */
    public static final NodoConstante E = new NodoConstante(Math.E, "e");

    private final double valor;
    private final String simbolo;

    /**
     * @param valor número
     */
    public NodoConstante(double valor) {
        this(valor, null);
    }

    /**
     * @param valor   número
     * @param simbolo símbolo a mostrar (o null)
     */
    public NodoConstante(double valor, String simbolo) {
        // Se evita el "cero negativo" para que las claves coincidan
        this.valor = (valor == 0.0) ? 0.0 : valor;
        this.simbolo = simbolo;
    }

    /** @return valor numérico */
    public double getValor() {
        return valor;
    }

    /** @return símbolo ("π", "e") o null si es un número común */
    public String getSimbolo() {
        return simbolo;
    }

    @Override
    public double evaluar(double x, double y) {
        return valor;
    }

    @Override
    public boolean dependeDe(Variable v) {
        return false;
    }

    @Override
    public String clave() {
        if (simbolo != null) {
            return "S:" + simbolo;
        }
        return "N:" + FormatoNumeros.formatearDecimal(valor, 10);
    }

    @Override
    public int precedencia() {
        String texto = toString();
        if (texto.startsWith("-") || texto.indexOf('/') >= 0) {
            return PREC_PRODUCTO;
        }
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
        if (simbolo != null) {
            return simbolo;
        }
        return FormatoNumeros.formatearExacto(valor);
    }
}
