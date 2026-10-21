package calculadora.modelo;

import calculadora.analisis.AnalizadorExpresion;
import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.DominioInvalidoException;
import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.simbolico.MotorSimbolico;
import calculadora.util.FormatoNumeros;

import java.util.List;

/**
 * FACHADA del motor matemático (patrón de diseño Fachada / Facade).
 * <p>
 * Ofrece dos motores: el motor por términos (métodos derivar, integrarIndefinida,
 * integrarDefinida y evaluar), y el motor simbólico ({@link #resolver}), que
 * trabaja con árboles de expresión y aplica todos los métodos de la lista.
 * </p>
 * <p>
 * La interfaz gráfica y las pruebas solo necesitan conocer esta clase:
 * ella se encarga de interpretar el texto, aplicar las reglas de cada
 * término y registrar el procedimiento en un {@link ResultadoCalculo}.
 * </p>
 */
public class Calculadora {

    /** Analizador que convierte texto en expresiones (motor por términos). */
    private final AnalizadorExpresion analizador;

    /** Motor simbólico (árbol de expresión) para los métodos avanzados. */
    private final MotorSimbolico motorSimbolico;

    /** Crea la calculadora con su propio analizador. */
    public Calculadora() {
        this.analizador = new AnalizadorExpresion();
        this.motorSimbolico = new MotorSimbolico();
    }

    /**
     * Resuelve un ejercicio con el método elegido en la lista (derivadas,
     * L'Hôpital, integrales y sus propiedades). Usa el motor simbólico.
     *
     * @param metodo  método elegido
     * @param entrada datos escritos por el estudiante
     * @return resultado con el procedimiento
     * @throws CalculadoraException si la expresión no es válida o el método no se puede aplicar
     */
    public ResultadoCalculo resolver(MetodoCalculo metodo, EntradaCalculo entrada) throws CalculadoraException {
        return motorSimbolico.resolver(metodo, entrada);
    }

    /**
     * Interpreta un texto y devuelve la expresión correspondiente.
     *
     * @param texto expresión escrita por el usuario
     * @return expresión interpretada y simplificada
     * @throws ExpresionInvalidaException si el texto no es válido
     */
    public Expresion interpretar(String texto) throws ExpresionInvalidaException {
        return analizador.analizar(texto);
    }

    // ==================================================================
    // DERIVADA
    // ==================================================================

    /**
     * Deriva una expresión respecto a v registrando cada paso.
     *
     * @param texto expresión escrita por el usuario
     * @param v     variable de derivación
     * @return resultado con procedimiento
     * @throws CalculadoraException si la expresión no es válida o no se puede derivar
     */
    public ResultadoCalculo derivar(String texto, Variable v) throws CalculadoraException {
        Expresion expresion = interpretar(texto);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.DERIVADA,
                texto, expresion, v);

        String operador = operadorDerivada(expresion, v);
        resultado.agregarPaso("Expresión interpretada: f = " + expresion + ". Se deriva respecto a "
                + v.getSimbolo() + textoParcial(expresion, v));

        List<Termino> terminos = expresion.getTerminos();
        if (terminos.size() > 1) {
            resultado.agregarPaso("Regla de la Suma/Resta: d/d" + v.getSimbolo() + "[f ± g] = f' ± g'. "
                    + "Derivar cada término de forma independiente y conservar los signos.");
        }

        for (int i = 0; i < terminos.size(); i++) {
            Termino termino = terminos.get(i);
            termino.derivar(v); // si no está soportado, lanza la excepción con un mensaje claro
            resultado.agregarPaso("Término " + (i + 1) + " [" + termino + "]: " + termino.describirDerivada(v));
        }

        Expresion derivada = expresion.derivar(v);
        resultado.agregarPaso("Unir los resultados y agrupar términos semejantes: " + derivada + ".");

        resultado.setExpresionResultado(derivada);
        resultado.setTextoResultado(operador + "(" + expresion + ") = " + derivada);
        return resultado;
    }

    // ==================================================================
    // INTEGRAL INDEFINIDA
    // ==================================================================

    /**
     * Calcula la integral indefinida (antiderivada + C) registrando los pasos.
     *
     * @param texto expresión escrita por el usuario
     * @param v     variable de integración
     * @return resultado con procedimiento
     * @throws CalculadoraException si la expresión no es válida o no se puede integrar
     */
    public ResultadoCalculo integrarIndefinida(String texto, Variable v) throws CalculadoraException {
        Expresion expresion = interpretar(texto);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.INTEGRAL_INDEFINIDA,
                texto, expresion, v);

        Expresion antiderivada = registrarPasosIntegracion(resultado, expresion, v);

        resultado.agregarPaso("Constante de Integración: toda integral indefinida concatena al final la "
                + "constante + C, porque la derivada de cualquier constante es 0.");

        String textoAntiderivada = textoConConstante(antiderivada);
        resultado.setExpresionResultado(antiderivada);
        resultado.setTextoResultado("∫ (" + expresion + ") d" + v.getSimbolo() + " = " + textoAntiderivada);
        return resultado;
    }

    // ==================================================================
    // INTEGRAL DEFINIDA (Teorema Fundamental del Cálculo)
    // ==================================================================

    /**
     * Calcula la integral definida en [a, b] aplicando el Teorema Fundamental del Cálculo.
     *
     * @param texto     expresión escrita por el usuario
     * @param v         variable de integración
     * @param a         límite inferior
     * @param b         límite superior
     * @param valorOtra valor fijo de la otra variable (si aparece en la expresión)
     * @return resultado con procedimiento y valor numérico
     * @throws CalculadoraException si hay errores de sintaxis, de dominio o de soporte
     */
    public ResultadoCalculo integrarDefinida(String texto, Variable v, double a, double b, double valorOtra)
            throws CalculadoraException {
        Expresion expresion = interpretar(texto);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA,
                texto, expresion, v);
        resultado.setLimites(a, b);
        resultado.setValorOtraVariable(valorOtra);

        validarDominioIntegral(expresion, v, a, b, valorOtra);

        // Paso 1 del TFC: obtener la antiderivada
        Expresion antiderivada = registrarPasosIntegracion(resultado, expresion, v);
        String s = v.getSimbolo();
        resultado.agregarPaso("Teorema Fundamental del Cálculo, paso 1: la antiderivada es F(" + s + ") = "
                + antiderivada + ". En la integral definida no se agrega + C porque se cancela al restar.");

        Variable otra = v.otra();
        if (expresion.dependeDe(otra)) {
            resultado.agregarPaso("La otra variable " + otra.getSimbolo() + " se trata como constante y se "
                    + "sustituye por su valor fijo " + otra.getSimbolo() + " = "
                    + FormatoNumeros.formatear(valorOtra) + ".");
        }

        // Paso 2: evaluar el límite superior
        double valorB = antiderivada.evaluarEn(v, b, valorOtra);
        resultado.agregarPaso("Teorema Fundamental del Cálculo, paso 2: evaluar el límite superior: F(b) = F("
                + s + ")|" + s + "=" + FormatoNumeros.formatear(b) + " = " + FormatoNumeros.formatear(valorB) + ".");

        // Paso 3: evaluar el límite inferior
        double valorA = antiderivada.evaluarEn(v, a, valorOtra);
        resultado.agregarPaso("Teorema Fundamental del Cálculo, paso 3: evaluar el límite inferior: F(a) = F("
                + s + ")|" + s + "=" + FormatoNumeros.formatear(a) + " = " + FormatoNumeros.formatear(valorA) + ".");

        // Paso 4: resultado neto
        double neto = valorB - valorA;
        if (Double.isNaN(neto) || Double.isInfinite(neto)) {
            throw new DominioInvalidoException("Integral impropia fuera del alcance: la antiderivada no está "
                    + "definida en alguno de los límites del intervalo [" + FormatoNumeros.formatear(a) + ", "
                    + FormatoNumeros.formatear(b) + "].");
        }
        resultado.agregarPaso("Teorema Fundamental del Cálculo, paso 4: resultado neto R = F(b) - F(a) = "
                + FormatoNumeros.formatear(valorB) + " - (" + FormatoNumeros.formatear(valorA) + ") = "
                + FormatoNumeros.formatear(neto) + ".");

        resultado.setExpresionResultado(antiderivada);
        resultado.setValorNumerico(Double.valueOf(neto));
        resultado.setTextoResultado("∫ de " + FormatoNumeros.formatear(a) + " a " + FormatoNumeros.formatear(b)
                + " de (" + expresion + ") d" + s + " = " + textoNumeroConAproximacion(neto));
        return resultado;
    }

    // ==================================================================
    // EVALUACIÓN
    // ==================================================================

    /**
     * Evalúa la expresión en un punto (x, y).
     *
     * @param texto expresión escrita por el usuario
     * @param x     valor de x
     * @param y     valor de y
     * @return resultado con procedimiento
     * @throws CalculadoraException si la expresión no es válida o el punto está fuera del dominio
     */
    public ResultadoCalculo evaluar(String texto, double x, double y) throws CalculadoraException {
        Expresion expresion = interpretar(texto);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.EVALUACION,
                texto, expresion, Variable.X);
        resultado.setValorOtraVariable(y);

        resultado.agregarPaso("Sustituir x = " + FormatoNumeros.formatear(x) + " y y = "
                + FormatoNumeros.formatear(y) + " en f = " + expresion + ".");

        List<Termino> terminos = expresion.getTerminos();
        double suma = 0.0;
        for (int i = 0; i < terminos.size(); i++) {
            Termino termino = terminos.get(i);
            double valor = termino.evaluar(x, y);
            resultado.agregarPaso("Término " + (i + 1) + " [" + termino + "] vale " + FormatoNumeros.formatear(valor) + ".");
            suma = suma + valor;
        }
        if (Double.isNaN(suma) || Double.isInfinite(suma)) {
            throw new DominioInvalidoException("La expresión no está definida en el punto (x = "
                    + FormatoNumeros.formatear(x) + ", y = " + FormatoNumeros.formatear(y) + ").");
        }
        resultado.agregarPaso("Sumar los valores de todos los términos: " + FormatoNumeros.formatear(suma) + ".");

        resultado.setValorNumerico(Double.valueOf(suma));
        resultado.setTextoResultado("f(" + FormatoNumeros.formatear(x) + ", " + FormatoNumeros.formatear(y)
                + ") = " + textoNumeroConAproximacion(suma));
        return resultado;
    }

    // ==================================================================
    // Métodos auxiliares
    // ==================================================================

    /**
     * Registra los pasos comunes de la integración término a término.
     */
    private Expresion registrarPasosIntegracion(ResultadoCalculo resultado, Expresion expresion, Variable v)
            throws CalculadoraException {
        resultado.agregarPaso("Expresión interpretada: f = " + expresion + ". Se integra respecto a "
                + v.getSimbolo() + textoParcial(expresion, v));

        List<Termino> terminos = expresion.getTerminos();
        if (terminos.size() > 1) {
            resultado.agregarPaso("Linealidad de la integral: ∫ [f ± g] d" + v.getSimbolo()
                    + " = ∫ f d" + v.getSimbolo() + " ± ∫ g d" + v.getSimbolo()
                    + ". Integrar cada término por separado y conservar los signos.");
        }
        for (int i = 0; i < terminos.size(); i++) {
            Termino termino = terminos.get(i);
            termino.integrar(v); // lanza la excepción si la técnica no está soportada
            resultado.agregarPaso("Término " + (i + 1) + " [" + termino + "]: " + termino.describirIntegral(v));
        }
        Expresion antiderivada = expresion.integrar(v);
        resultado.agregarPaso("Unir las antiderivadas y agrupar términos semejantes: " + antiderivada + ".");
        return antiderivada;
    }

    /**
     * Validación de dominio antes de una integral definida.
     * <ul>
     *   <li>Si un término tiene exponente negativo en v y el 0 pertenece a [a, b],
     *       la integral es impropia (la función no está definida en 0).</li>
     *   <li>Si un exponente no es entero (raíz) y el intervalo contiene números
     *       negativos, aparecerían raíces no reales.</li>
     *   <li>Un logaritmo ln(k·v) necesita k·v &gt; 0 en todo el intervalo.</li>
     * </ul>
     */
    private void validarDominioIntegral(Expresion expresion, Variable v, double a, double b, double valorOtra)
            throws DominioInvalidoException {
        double minimo = Math.min(a, b);
        double maximo = Math.max(a, b);
        String intervalo = "[" + FormatoNumeros.formatear(a) + ", " + FormatoNumeros.formatear(b) + "]";

        List<Termino> terminos = expresion.getTerminos();
        for (int i = 0; i < terminos.size(); i++) {
            Termino termino = terminos.get(i);
            double n = termino.getExponente(v);

            if (n < 0 && minimo <= 0.0 && maximo >= 0.0) {
                throw new DominioInvalidoException("Integral impropia fuera del alcance: el término " + termino
                        + " no está definido en " + v.getSimbolo() + " = 0, que pertenece al intervalo "
                        + intervalo + ".");
            }
            if (!FormatoNumeros.esEntero(n) && minimo < 0.0) {
                throw new DominioInvalidoException("Integral impropia fuera del alcance: el término " + termino
                        + " tiene una raíz (exponente " + FormatoNumeros.formatear(n)
                        + ") que produce valores no reales para " + v.getSimbolo() + " < 0 dentro de "
                        + intervalo + ".");
            }
            if (termino instanceof TerminoLogaritmico) {
                TerminoLogaritmico logaritmo = (TerminoLogaritmico) termino;
                if (logaritmo.getVariableFuncion() == v && !logaritmo.isValorAbsoluto()) {
                    double extremo1 = logaritmo.getConstanteK() * minimo;
                    double extremo2 = logaritmo.getConstanteK() * maximo;
                    if (extremo1 <= 0.0 || extremo2 <= 0.0) {
                        throw new DominioInvalidoException("Fuera del dominio: " + termino
                                + " requiere un argumento positivo en todo el intervalo " + intervalo + ".");
                    }
                }
            }
            // La otra variable fija también debe estar en el dominio
            double exponenteOtra = termino.getExponente(v.otra());
            if (exponenteOtra < 0 && FormatoNumeros.sonIguales(valorOtra, 0.0)) {
                throw new DominioInvalidoException("El término " + termino + " no está definido para "
                        + v.otra().getSimbolo() + " = 0. Cambie el valor fijo de " + v.otra().getSimbolo() + ".");
            }
        }
    }

    /**
     * @return "d/dx" o "∂/∂x" según la expresión tenga una o dos variables
     */
    private String operadorDerivada(Expresion expresion, Variable v) {
        if (expresion.dependeDe(v.otra())) {
            return "∂/∂" + v.getSimbolo() + " ";
        }
        return "d/d" + v.getSimbolo() + " ";
    }

    /**
     * Frase que explica que la otra variable se trata como constante (si aparece).
     */
    private String textoParcial(Expresion expresion, Variable v) {
        Variable otra = v.otra();
        if (expresion.dependeDe(otra)) {
            return ". Es una operación PARCIAL: la variable " + otra.getSimbolo()
                    + " se trata como una CONSTANTE.";
        }
        return ".";
    }

    /**
     * @return la antiderivada seguida de "+ C"
     */
    private String textoConConstante(Expresion antiderivada) {
        if (antiderivada.estaVacia()) {
            return "C";
        }
        return antiderivada + " + C";
    }

    /**
     * Muestra un número exacto y, si es fracción o irracional, su aproximación decimal.
     */
    private String textoNumeroConAproximacion(double valor) {
        String exacto = FormatoNumeros.formatear(valor);
        String decimal = FormatoNumeros.formatearDecimal(valor, 6);
        if (exacto.equals(decimal)) {
            return exacto;
        }
        return exacto + " ≈ " + decimal;
    }
}
