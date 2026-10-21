package calculadora.simbolico;

import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.modelo.Variable;

/**
 * Analizador de DESCENSO RECURSIVO que convierte un texto en un árbol de
 * expresión ({@link Nodo}). Recorre el texto carácter por carácter.
 * <p>
 * Gramática (EBNF):
 * </p>
 * <pre>
 * expresion ::= termino { ('+' | '-') termino }
 * termino   ::= unario { ('*' | '/' | multiplicación implícita) unario }
 * unario    ::= ('+' | '-') unario | potencia
 * potencia  ::= primario [ '^' unario ]          (asociativa a la derecha)
 * primario  ::= numero | x | y | pi | e | funcion [ '^' n ] '(' expresion ')'
 *             | '√' primario | '(' expresion ')' | '|' expresion '|'
 * funcion   ::= sin | sen | cos | tan | tg | sec | csc | cot | ln | log | exp
 *             | sqrt | arcsin | arccos | arctan | asin | acos | atan | abs
 * </pre>
 * Ejemplos válidos: {@code x^2·sin(x)}, {@code sin(x)/x}, {@code e^(x^2)},
 * {@code (3x+5)/(x^2+x-2)}, {@code 1/√(4 - x^2)}, {@code sin^2(x)}.
 */
public class AnalizadorSimbolico {

    /** Nombres de funciones reconocidos (los más largos primero). */
    private static final String[] NOMBRES = {
        "arcsin", "arccos", "arctan", "cosec", "asin", "acos", "atan", "sqrt",
        "sin", "sen", "cos", "tan", "sec", "csc", "cot", "ctg", "exp", "log", "abs", "tg", "ln"
    };

    private static final TipoFuncion[] TIPOS = {
        TipoFuncion.ARCSIN, TipoFuncion.ARCCOS, TipoFuncion.ARCTAN, TipoFuncion.CSC, TipoFuncion.ARCSIN,
        TipoFuncion.ARCCOS, TipoFuncion.ARCTAN, TipoFuncion.RAIZ,
        TipoFuncion.SIN, TipoFuncion.SIN, TipoFuncion.COS, TipoFuncion.TAN, TipoFuncion.SEC, TipoFuncion.CSC,
        TipoFuncion.COT, TipoFuncion.COT, TipoFuncion.EXP, TipoFuncion.LOG, TipoFuncion.ABS, TipoFuncion.TAN,
        TipoFuncion.LN
    };

    private String texto;
    private int posicion;

    /**
     * Convierte un texto en árbol (sin simplificar).
     *
     * @param entrada expresión escrita por el usuario
     * @return árbol de la expresión
     * @throws ExpresionInvalidaException si la expresión no es válida
     */
    public Nodo analizar(String entrada) throws ExpresionInvalidaException {
        if (entrada == null || entrada.trim().isEmpty()) {
            throw new ExpresionInvalidaException("La expresión está vacía. Escriba, por ejemplo: x^2·sin(x)");
        }
        texto = normalizar(entrada);
        posicion = 0;
        Nodo resultado = expresion();
        saltarEspacios();
        if (posicion < texto.length()) {
            char c = texto.charAt(posicion);
            if (c == ')') {
                throw new ExpresionInvalidaException("Hay un paréntesis ')' de más", posicion);
            }
            if (c == '=') {
                throw new ExpresionInvalidaException("El signo '=' solo se usa en derivación implícita", posicion);
            }
            throw new ExpresionInvalidaException("Carácter inesperado '" + c + "'", posicion);
        }
        return resultado;
    }

    /**
     * Interpreta un límite numérico que puede ser infinito:
     * "∞", "inf", "infinito", "-∞", "+∞" o cualquier expresión constante (pi/2, 1/3...).
     *
     * @param entrada texto del límite
     * @return valor (Double.POSITIVE_INFINITY o NEGATIVE_INFINITY para ±∞)
     * @throws ExpresionInvalidaException si no es un número válido
     */
    public double analizarLimite(String entrada) throws ExpresionInvalidaException {
        if (entrada == null || entrada.trim().isEmpty()) {
            throw new ExpresionInvalidaException("Falta escribir el límite");
        }
        String limpio = entrada.trim().toLowerCase().replace(" ", "").replace('−', '-');
        String sinSigno = limpio;
        boolean negativo = false;
        if (sinSigno.startsWith("-")) {
            negativo = true;
            sinSigno = sinSigno.substring(1);
        } else if (sinSigno.startsWith("+")) {
            sinSigno = sinSigno.substring(1);
        }
        if (sinSigno.equals("∞") || sinSigno.equals("inf") || sinSigno.equals("infinito")
                || sinSigno.equals("infinity")) {
            if (negativo) {
                return Double.NEGATIVE_INFINITY;
            }
            return Double.POSITIVE_INFINITY;
        }
        Nodo nodo = analizar(entrada);
        if (!nodo.esConstante()) {
            throw new ExpresionInvalidaException("El límite debe ser un número (o ∞), no puede contener variables");
        }
        double valor = nodo.evaluar(0, 0);
        if (Double.isNaN(valor)) {
            throw new ExpresionInvalidaException("El límite '" + entrada + "' no es un número real");
        }
        return valor;
    }

    private String normalizar(String entrada) {
        String normal = entrada.toLowerCase();
        normal = normal.replace('·', '*').replace('×', '*').replace('−', '-').replace('÷', '/');
        normal = normal.replace(',', '.').replace("π", "pi").replace('[', '(').replace(']', ')');
        normal = normal.replace("²", "^2").replace("³", "^3");
        return normal;
    }

    // ------------------------------------------------------------------
    // Reglas de la gramática
    // ------------------------------------------------------------------

    private Nodo expresion() throws ExpresionInvalidaException {
        Nodo acumulado = termino();
        while (true) {
            saltarEspacios();
            if (posicion >= texto.length()) {
                break;
            }
            char c = texto.charAt(posicion);
            if (c == '+') {
                posicion++;
                acumulado = Nodo.suma(acumulado, termino());
            } else if (c == '-') {
                posicion++;
                acumulado = Nodo.resta(acumulado, termino());
            } else {
                break;
            }
        }
        return acumulado;
    }

    private Nodo termino() throws ExpresionInvalidaException {
        Nodo acumulado = unario();
        while (true) {
            saltarEspacios();
            if (posicion >= texto.length()) {
                break;
            }
            char c = texto.charAt(posicion);
            if (c == '*') {
                posicion++;
                acumulado = Nodo.mul(acumulado, unario());
            } else if (c == '/') {
                int posicionDivision = posicion;
                posicion++;
                Nodo divisor = unario();
                if (divisor.esNumero(0.0)) {
                    throw new ExpresionInvalidaException("División entre cero", posicionDivision);
                }
                acumulado = Nodo.div(acumulado, divisor);
            } else if (esInicioDePrimario(c)) {
                // Multiplicación implícita: 2x, 3sin(x), x(x+1), (x+1)(x-1)
                acumulado = Nodo.mul(acumulado, potencia());
            } else {
                break;
            }
        }
        return acumulado;
    }

    private Nodo unario() throws ExpresionInvalidaException {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw new ExpresionInvalidaException("La expresión termina de forma incompleta", posicion);
        }
        char c = texto.charAt(posicion);
        if (c == '-') {
            posicion++;
            Nodo argumento = unario();
            if (argumento.esNumero()) {
                return Nodo.num(-((NodoConstante) argumento).getValor());
            }
            return Nodo.neg(argumento);
        }
        if (c == '+') {
            posicion++;
            return unario();
        }
        return potencia();
    }

    private Nodo potencia() throws ExpresionInvalidaException {
        Nodo base = primario();
        saltarEspacios();
        if (posicion < texto.length() && texto.charAt(posicion) == '^') {
            posicion++;
            Nodo exponente = unario(); // asociativa a la derecha y admite x^-1
            if (base instanceof NodoConstante && "e".equals(((NodoConstante) base).getSimbolo())) {
                return Nodo.fn(TipoFuncion.EXP, exponente);
            }
            return Nodo.pot(base, exponente);
        }
        return base;
    }

    private Nodo primario() throws ExpresionInvalidaException {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw new ExpresionInvalidaException("Falta un número, variable o función", posicion);
        }
        char c = texto.charAt(posicion);

        if (Character.isDigit(c) || c == '.') {
            return Nodo.num(leerNumero());
        }
        if (c == '(') {
            int inicio = posicion;
            posicion++;
            Nodo interior = expresion();
            esperar(')', "Falta cerrar el paréntesis", inicio);
            return interior;
        }
        if (c == '|') {
            int inicio = posicion;
            posicion++;
            Nodo interior = expresion();
            esperar('|', "Falta cerrar el valor absoluto |...|", inicio);
            return Nodo.fn(TipoFuncion.ABS, interior);
        }
        if (c == '√') {
            posicion++;
            return Nodo.fn(TipoFuncion.RAIZ, potencia());
        }
        if (c == '∞') {
            throw new ExpresionInvalidaException("El símbolo ∞ solo se usa en los límites, no dentro de la función",
                    posicion);
        }

        // Funciones con nombre
        for (int i = 0; i < NOMBRES.length; i++) {
            if (texto.startsWith(NOMBRES[i], posicion)) {
                posicion += NOMBRES[i].length();
                return funcion(TIPOS[i], NOMBRES[i]);
            }
        }
        if (texto.startsWith("pi", posicion)) {
            posicion += 2;
            return NodoConstante.PI;
        }
        if (c == 'e') {
            posicion++;
            return NodoConstante.E;
        }
        if (c == 'x') {
            posicion++;
            return Nodo.var(Variable.X);
        }
        if (c == 'y') {
            posicion++;
            return Nodo.var(Variable.Y);
        }
        if (Character.isLetter(c)) {
            throw new ExpresionInvalidaException("Símbolo desconocido '" + c
                    + "'. Use x, y, números, pi, e y funciones como sin, cos, tan, ln, √, arctan", posicion);
        }
        throw new ExpresionInvalidaException("Carácter inesperado '" + c + "'", posicion);
    }

    /**
     * Lee el argumento de una función. Admite la forma sin^2(x) = (sin x)^2.
     */
    private Nodo funcion(TipoFuncion tipo, String nombre) throws ExpresionInvalidaException {
        saltarEspacios();
        Nodo exponente = null;
        if (posicion < texto.length() && texto.charAt(posicion) == '^') {
            posicion++;
            exponente = unario();
            saltarEspacios();
        }
        if (posicion < texto.length() && texto.charAt(posicion) == '|') {
            // Forma ln|x|
            Nodo valorAbsoluto = primario();
            Nodo conBarras = Nodo.fn(tipo, valorAbsoluto);
            if (exponente != null) {
                return Nodo.pot(conBarras, exponente);
            }
            return conBarras;
        }
        if (posicion >= texto.length() || texto.charAt(posicion) != '(') {
            throw new ExpresionInvalidaException("Después de '" + nombre + "' debe ir un paréntesis, por ejemplo "
                    + nombre + "(x)", posicion);
        }
        int inicio = posicion;
        posicion++;
        Nodo argumento = expresion();
        esperar(')', "Falta cerrar el paréntesis de " + nombre + "(", inicio);
        Nodo resultado = Nodo.fn(tipo, argumento);
        if (exponente != null) {
            resultado = Nodo.pot(resultado, exponente);
        }
        return resultado;
    }

    private boolean esInicioDePrimario(char c) {
        return Character.isLetterOrDigit(c) || c == '.' || c == '(' || c == '√';
    }

    private double leerNumero() throws ExpresionInvalidaException {
        int inicio = posicion;
        boolean hayPunto = false;
        while (posicion < texto.length()) {
            char c = texto.charAt(posicion);
            if (Character.isDigit(c)) {
                posicion++;
            } else if (c == '.' && !hayPunto) {
                hayPunto = true;
                posicion++;
            } else {
                break;
            }
        }
        String numero = texto.substring(inicio, posicion);
        if (numero.equals(".")) {
            throw new ExpresionInvalidaException("Número mal escrito", inicio);
        }
        return Double.parseDouble(numero);
    }

    private void esperar(char esperado, String mensaje, int apertura) throws ExpresionInvalidaException {
        saltarEspacios();
        if (posicion >= texto.length() || texto.charAt(posicion) != esperado) {
            throw new ExpresionInvalidaException(mensaje, apertura);
        }
        posicion++;
    }

    private void saltarEspacios() {
        while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }
}
