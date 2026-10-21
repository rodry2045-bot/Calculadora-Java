package calculadora.modelo;

/**
 * Cualquier función que se puede evaluar y dibujar en el plano.
 * <p>
 * La implementan tanto las expresiones del motor por términos
 * ({@link Expresion}) como los nodos del motor simbólico. Así, el panel de
 * gráficas no necesita saber con qué motor se obtuvo la función
 * (principio de inversión de dependencias).
 * </p>
 */
public interface Graficable {

    /**
     * Evalúa la función cuando una variable toma un valor y la otra un valor fijo.
     *
     * @param variable  variable del eje horizontal
     * @param valor     valor de esa variable
     * @param valorOtra valor fijo de la otra variable
     * @return valor numérico (NaN o infinito fuera del dominio)
     */
    double evaluarEn(Variable variable, double valor, double valorOtra);

    /**
     * @param v variable consultada
     * @return true si la función depende de v
     */
    boolean dependeDe(Variable v);
}
