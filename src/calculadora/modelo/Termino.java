package calculadora.modelo;

import calculadora.excepciones.CalculadoraException;
import calculadora.util.FormatoNumeros;

/**
 * Clase abstracta que representa un término de una expresión.
 * <p>
 * Todo término tiene una "parte algebraica" de la forma:
 * </p>
 * <pre>
 *     c · x^a · y^b
 * </pre>
 * <p>
 * donde c es el coeficiente, a el exponente de x y b el exponente de y.
 * Las subclases funcionales (seno, coseno, exponencial y logaritmo)
 * multiplican esa parte algebraica por una función: c · x^a · y^b · f(k·v).
 * </p>
 * <p>
 * Gracias a esta representación, un término como x^2·y se puede derivar
 * respecto a x tratando el factor y como un coeficiente constante
 * (derivación parcial).
 * </p>
 * Los objetos Termino son INMUTABLES: derivar o integrar no cambia el
 * término original, sino que devuelve un término nuevo.
 */
public abstract class Termino {

    /** Coeficiente numérico c. */
    protected final double coeficiente;

    /** Exponente de la variable x en la parte algebraica. */
    protected final double exponenteX;

    /** Exponente de la variable y en la parte algebraica. */
    protected final double exponenteY;

    /**
     * Constructor común para todas las subclases.
     *
     * @param coeficiente coeficiente c
     * @param exponenteX  exponente de x (0 si no aparece x)
     * @param exponenteY  exponente de y (0 si no aparece y)
     */
    protected Termino(double coeficiente, double exponenteX, double exponenteY) {
        this.coeficiente = coeficiente;
        this.exponenteX = exponenteX;
        this.exponenteY = exponenteY;
    }

    // ------------------------------------------------------------------
    // Métodos abstractos que cada subclase debe implementar
    // ------------------------------------------------------------------

    /**
     * Evalúa numéricamente el término.
     *
     * @param x valor de la variable x
     * @param y valor de la variable y
     * @return valor numérico (puede ser NaN o infinito fuera del dominio)
     */
    public abstract double evaluar(double x, double y);

    /**
     * Deriva el término respecto a la variable de trabajo v.
     *
     * @param v variable respecto a la cual se deriva
     * @return un término nuevo con la derivada
     * @throws CalculadoraException si la derivada requiere una regla no soportada
     */
    public abstract Termino derivar(Variable v) throws CalculadoraException;

    /**
     * Describe con palabras qué regla de derivación se aplica a este término.
     *
     * @param v variable respecto a la cual se deriva
     * @return explicación de la regla aplicada
     */
    public abstract String describirDerivada(Variable v);

    /**
     * Calcula una antiderivada del término respecto a v (sin la constante C).
     *
     * @param v variable de integración
     * @return un término nuevo con la antiderivada
     * @throws CalculadoraException si la integral requiere una técnica no soportada
     */
    public abstract Termino integrar(Variable v) throws CalculadoraException;

    /**
     * Describe con palabras qué regla de integración se aplica a este término.
     *
     * @param v variable de integración
     * @return explicación de la regla aplicada
     */
    public abstract String describirIntegral(Variable v);

    /**
     * Crea un término del MISMO tipo (misma función, misma k) pero con otra
     * parte algebraica. Es útil para aplicar la regla de la potencia sin
     * repetir código en cada subclase.
     *
     * @param nuevoCoeficiente nuevo coeficiente
     * @param nuevoExpX        nuevo exponente de x
     * @param nuevoExpY        nuevo exponente de y
     * @return término nuevo del mismo tipo
     */
    public abstract Termino conParteAlgebraica(double nuevoCoeficiente, double nuevoExpX, double nuevoExpY);

    /**
     * Texto que identifica a los términos "semejantes": dos términos con la
     * misma firma solo difieren en el coeficiente y pueden sumarse.
     *
     * @return firma de semejanza
     */
    public abstract String firmaSemejanza();

    /**
     * @return texto del término, por ejemplo "-3x^2y" o "(1/2)sin(2x)"
     */
    @Override
    public abstract String toString();

    // ------------------------------------------------------------------
    // Métodos comunes
    // ------------------------------------------------------------------

    /** @return el coeficiente c */
    public double getCoeficiente() {
        return coeficiente;
    }

    /** @return el exponente de x en la parte algebraica */
    public double getExponenteX() {
        return exponenteX;
    }

    /** @return el exponente de y en la parte algebraica */
    public double getExponenteY() {
        return exponenteY;
    }

    /**
     * Devuelve el exponente de la variable indicada en la parte algebraica.
     *
     * @param v variable consultada
     * @return exponente de v
     */
    public double getExponente(Variable v) {
        if (v == Variable.X) {
            return exponenteX;
        }
        return exponenteY;
    }

    /**
     * @return true si el coeficiente es (prácticamente) cero
     */
    public boolean esCero() {
        return Math.abs(coeficiente) < FormatoNumeros.TOLERANCIA;
    }

    /**
     * Indica si el término depende de la variable v (en cualquier parte).
     * Las subclases funcionales amplían este método.
     *
     * @param v variable consultada
     * @return true si v aparece en el término
     */
    public boolean dependeDe(Variable v) {
        return !FormatoNumeros.sonIguales(getExponente(v), 0.0);
    }

    /**
     * Evalúa solo la parte algebraica c · x^a · y^b.
     *
     * @param x valor de x
     * @param y valor de y
     * @return valor de la parte algebraica
     */
    protected double evaluarParteAlgebraica(double x, double y) {
        double resultado = coeficiente;
        if (!FormatoNumeros.sonIguales(exponenteX, 0.0)) {
            resultado = resultado * Math.pow(x, exponenteX);
        }
        if (!FormatoNumeros.sonIguales(exponenteY, 0.0)) {
            resultado = resultado * Math.pow(y, exponenteY);
        }
        return resultado;
    }

    /**
     * Arma el texto de la parte algebraica seguida del texto de una función.
     *
     * @param textoFuncion texto de la función (por ejemplo "sin(2x)") o cadena vacía
     * @return texto completo del término
     */
    protected String construirTexto(String textoFuncion) {
        StringBuilder variables = new StringBuilder();
        if (!FormatoNumeros.sonIguales(exponenteX, 0.0)) {
            variables.append("x").append(FormatoNumeros.formatearExponente(exponenteX));
        }
        if (!FormatoNumeros.sonIguales(exponenteY, 0.0)) {
            variables.append("y").append(FormatoNumeros.formatearExponente(exponenteY));
        }
        variables.append(textoFuncion);

        // Si no hay variables ni función, el término es una constante pura
        if (variables.length() == 0) {
            return FormatoNumeros.formatear(coeficiente);
        }
        return FormatoNumeros.formatearCoeficiente(coeficiente) + variables.toString();
    }

    /**
     * Describe la parte que se mantiene constante (la otra variable), útil
     * para explicar derivadas e integrales parciales.
     *
     * @param v variable de trabajo
     * @return frase explicativa o cadena vacía si no hay otra variable
     */
    protected String notaOtraVariable(Variable v) {
        Variable otra = v.otra();
        double exponenteOtra = getExponente(otra);
        if (FormatoNumeros.sonIguales(exponenteOtra, 0.0)) {
            return "";
        }
        return " El factor " + otra.getSimbolo() + FormatoNumeros.formatearExponente(exponenteOtra)
                + " se trata como coeficiente constante porque se trabaja respecto a "
                + v.getSimbolo() + ".";
    }

    // ------------------------------------------------------------------
    // Reglas de la potencia aplicadas a la parte algebraica
    // (las reutilizan todas las subclases)
    // ------------------------------------------------------------------

    /**
     * Regla de la potencia: d/dv (c·v^n) = c·n·v^(n-1).
     * Si n = 0, el término es constante respecto a v y la derivada es 0.
     *
     * @param v variable de derivación
     * @return término derivado (del mismo tipo)
     */
    protected Termino derivarParteAlgebraica(Variable v) {
        double n = getExponente(v);
        if (FormatoNumeros.sonIguales(n, 0.0)) {
            // Constante respecto a v: la derivada es cero
            return new TerminoPolinomico(0.0, 0.0, 0.0);
        }
        double nuevoCoeficiente = coeficiente * n;
        double nuevoExponente = n - 1.0;
        if (v == Variable.X) {
            return conParteAlgebraica(nuevoCoeficiente, nuevoExponente, exponenteY);
        }
        return conParteAlgebraica(nuevoCoeficiente, exponenteX, nuevoExponente);
    }

    /**
     * Regla de la potencia para integrales: ∫ c·v^n dv = c/(n+1) · v^(n+1), con n ≠ -1.
     *
     * @param v variable de integración
     * @return término integrado (del mismo tipo)
     */
    protected Termino integrarParteAlgebraica(Variable v) {
        double n = getExponente(v);
        double nuevoExponente = n + 1.0;
        double nuevoCoeficiente = coeficiente / nuevoExponente;
        if (v == Variable.X) {
            return conParteAlgebraica(nuevoCoeficiente, nuevoExponente, exponenteY);
        }
        return conParteAlgebraica(nuevoCoeficiente, exponenteX, nuevoExponente);
    }

    /**
     * Firma de la parte algebraica (exponentes), usada por las subclases
     * para construir su firma de semejanza.
     *
     * @return texto con los exponentes de x e y
     */
    protected String firmaParteAlgebraica() {
        return "x" + FormatoNumeros.formatearDecimal(exponenteX, 9)
                + "|y" + FormatoNumeros.formatearDecimal(exponenteY, 9);
    }
}
