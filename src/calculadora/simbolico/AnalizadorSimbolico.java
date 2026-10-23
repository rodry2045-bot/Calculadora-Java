package calculadora.simbolico;

import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.modelo.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * potencia  ::= primario { '!' } [ '^' unario ]   (asociativa a la derecha)
 * primario  ::= numero | x | y | pi | e | funcion [ '^' n ] '(' expresion ')'
 *             | '√' primario | '(' expresion ')' | '|' expresion '|'
 * funcion   ::= sin | sen | cos | tan | tg | sec | csc | cot | ln | log | exp
 *             | sqrt | raiz | arcsin | arcsen | arccos | arctan | arctg | abs
 *             | sinh | senh | cosh | tanh
 * (el argumento puede ir sin paréntesis: sinx, sin 2x, ln x)
 * </pre>
 * Ejemplos válidos: {@code x^2·sin(x)}, {@code sin(x)/x}, {@code e^(x^2)},
 * {@code (3x+5)/(x^2+x-2)}, {@code 1/√(4 - x^2)}, {@code sin^2(x)}.
 */
public class AnalizadorSimbolico {

    /** Nombres de funciones reconocidos (los más largos primero). */
    private static final String[] NOMBRES = {
        "arcsen", "arcsin", "arccos", "arctan", "cosec", "arctg", "asin", "acos", "atan", "sqrt", "raiz", "raíz",
        "sinh", "senh", "cosh", "tanh", "tgh",
        "sin", "sen", "cos", "tan", "sec", "csc", "cot", "ctg", "exp", "log", "abs", "tg", "ln"
    };

    private static final TipoFuncion[] TIPOS = {
        TipoFuncion.ARCSIN, TipoFuncion.ARCSIN, TipoFuncion.ARCCOS, TipoFuncion.ARCTAN, TipoFuncion.CSC,
        TipoFuncion.ARCTAN, TipoFuncion.ARCSIN, TipoFuncion.ARCCOS, TipoFuncion.ARCTAN, TipoFuncion.RAIZ,
        TipoFuncion.RAIZ, TipoFuncion.RAIZ,
        TipoFuncion.SINH, TipoFuncion.SINH, TipoFuncion.COSH, TipoFuncion.TANH, TipoFuncion.TANH,
        TipoFuncion.SIN, TipoFuncion.SIN, TipoFuncion.COS, TipoFuncion.TAN, TipoFuncion.SEC, TipoFuncion.CSC,
        TipoFuncion.COT, TipoFuncion.COT, TipoFuncion.EXP, TipoFuncion.LOG, TipoFuncion.ABS, TipoFuncion.TAN,
        TipoFuncion.LN
    };

    /** Nombres válidos que puede usar el estudiante (para las sugerencias). */
    public static final String[] NOMBRES_PUBLICOS = {
        "sin", "cos", "tan", "sec", "csc", "cot", "sen", "ln", "log", "exp", "sqrt", "abs",
        "arcsin", "arccos", "arctan", "arcsen", "sinh", "cosh", "tanh"
    };

    /**
     * Prefijo "f(x) =" o "y =" que algunos escriben antes de la función.
     * Se quita porque solo hace falta la función.
     */
    private static final Pattern PREFIJO_FUNCION =
            Pattern.compile("^\\s*(?:[fgh]\\s*\\(\\s*[xy]\\s*\\)|[yf])\\s*=(?!=)");

    private String texto;
    private int posicion;

    /** Avisos sobre lecturas que podrían no ser las que el estudiante quería. */
    private final List<String> avisos = new ArrayList<String>();

    /**
     * Avisos del último análisis: por ejemplo, que {@code e^2x} se leyó como e²·x.
     *
     * @return copia de la lista de avisos
     */
    public List<String> getAvisos() {
        return new ArrayList<String>(avisos);
    }

    /**
     * Quita un prefijo como "f(x) =" o "y =" al inicio del texto.
     *
     * @param entrada texto escrito por el estudiante
     * @return el texto sin el prefijo (o el mismo texto si no lo tiene)
     */
    public static String quitarPrefijoFuncion(String entrada) {
        if (entrada == null) {
            return null;
        }
        Matcher m = PREFIJO_FUNCION.matcher(entrada);
        if (m.find()) {
            String resto = entrada.substring(m.end());
            if (resto.indexOf('=') < 0) {
                return resto;
            }
        }
        return entrada;
    }

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
        avisos.clear();
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

    /**
     * Interpreta un límite finito y lo devuelve como árbol, para conservar la
     * forma exacta (π/2, e, ln(2)...) al sustituirlo en una antiderivada.
     *
     * @param entrada texto del límite
     * @return árbol constante simplificado
     * @throws ExpresionInvalidaException si no es un número válido
     */
    public Nodo analizarLimiteNodo(String entrada) throws ExpresionInvalidaException {
        Nodo nodo = analizar(entrada);
        if (!nodo.esConstante()) {
            throw new ExpresionInvalidaException("El límite debe ser un número (o ∞), no puede contener variables");
        }
        return Simplificador.simplificar(nodo);
    }

    private String normalizar(String entrada) {
        String normal = entrada.toLowerCase();
        normal = normal.replace('·', '*').replace('×', '*').replace('−', '-').replace('÷', '/');
        normal = normal.replace(',', '.').replace("π", "pi").replace('[', '(').replace(']', ')');
        normal = normal.replace("²", "^2").replace("³", "^3").replace("**", "^");
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
                saltarEspacios();
                int inicioDivisor = posicion;
                Nodo divisor = unario();
                if (divisor.esNumero(0.0)) {
                    throw new ExpresionInvalidaException("División entre cero", posicionDivision);
                }
                acumulado = Nodo.div(acumulado, divisor);
                if (divisor.esNumero() && texto.charAt(inicioDivisor) != '(' && sigueFactorPegado()) {
                    String numero = texto.substring(inicioDivisor, posicion).trim();
                    String siguiente = leerFragmentoSiguiente();
                    avisos.add("«/" + numero + siguiente + "» se leyó como «/" + numero + "·" + siguiente
                            + "»: solo " + numero + " queda en el denominador. Si querías dividir entre "
                            + numero + siguiente + ", escribe /(" + numero + siguiente + ")");
                }
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
        saltarEspacios();
        int inicio = posicion;
        Nodo base = primario();
        while (posicion < texto.length() && texto.charAt(posicion) == '!') {
            base = factorial(base, posicion);
            posicion++;
        }
        saltarEspacios();
        if (posicion < texto.length() && texto.charAt(posicion) == '^') {
            int posicionCircunflejo = posicion;
            posicion++;
            saltarEspacios();
            int inicioExponente = posicion;
            Nodo exponente = unario(); // asociativa a la derecha y admite x^-1
            boolean exponenteEntreParentesis = inicioExponente < texto.length() && texto.charAt(inicioExponente) == '(';
            if (!exponenteEntreParentesis && base.esConstante() && sigueFactorPegado()) {
                String baseTexto = texto.substring(inicio, posicionCircunflejo).trim();
                String exponenteTexto = texto.substring(inicioExponente, posicion).trim();
                String siguiente = leerFragmentoSiguiente();
                avisos.add("«" + baseTexto + "^" + exponenteTexto + siguiente + "» se leyó como " + baseTexto + "^"
                        + exponenteTexto + "·" + siguiente + " (el exponente es solo " + exponenteTexto
                        + "). Si querías " + baseTexto + "^(" + exponenteTexto + siguiente
                        + "), escribe el exponente entre paréntesis");
            }
            if (base instanceof NodoConstante && "e".equals(((NodoConstante) base).getSimbolo())) {
                return Nodo.fn(TipoFuncion.EXP, exponente);
            }
            return Nodo.pot(base, exponente);
        }
        return base;
    }

    /** Calcula n! para un número entero entre 0 y 170. */
    private Nodo factorial(Nodo base, int posicionSigno) throws ExpresionInvalidaException {
        if (!base.esNumero()) {
            throw new ExpresionInvalidaException("El factorial (!) solo se puede usar con números enteros, por ejemplo 5!",
                    posicionSigno);
        }
        double n = ((NodoConstante) base).getValor();
        if (n < 0 || n > 170 || Math.abs(n - Math.rint(n)) > 1e-9) {
            throw new ExpresionInvalidaException("El factorial (!) solo existe para enteros de 0 a 170", posicionSigno);
        }
        double producto = 1;
        for (int k = 2; k <= (int) Math.rint(n); k++) {
            producto = producto * k;
        }
        return Nodo.num(producto);
    }

    /**
     * @return true si justo después de la posición actual (sin espacio) empieza
     *         otro factor, como la x de {@code e^2x}
     */
    private boolean sigueFactorPegado() {
        if (posicion >= texto.length() || posicion == 0) {
            return false;
        }
        if (Character.isWhitespace(texto.charAt(posicion - 1))) {
            return false;
        }
        char c = texto.charAt(posicion);
        return Character.isLetter(c) || c == '(' || c == '√';
    }

    /** Lee (sin consumir) el factor que sigue: "x", "sin(x)", "(x+1)"... */
    private String leerFragmentoSiguiente() {
        int fin = posicion;
        int profundidad = 0;
        while (fin < texto.length()) {
            char c = texto.charAt(fin);
            if (c == '(') {
                profundidad++;
            } else if (c == ')') {
                if (profundidad == 0) {
                    break;
                }
                profundidad--;
                if (profundidad == 0) {
                    fin++;
                    break;
                }
            } else if (profundidad == 0 && !Character.isLetterOrDigit(c) && c != '.' && c != '^') {
                break;
            }
            fin++;
        }
        return texto.substring(posicion, fin);
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
            saltarEspacios();
            boolean conParentesis = posicion < texto.length() && texto.charAt(posicion) == '(';
            int inicioRadicando = posicion;
            Nodo radicando = potencia();
            if (!conParentesis && sigueFactorPegado()) {
                String radicandoTexto = texto.substring(inicioRadicando, posicion).trim();
                String siguiente = leerFragmentoSiguiente();
                avisos.add("«√" + radicandoTexto + siguiente + "» se leyó como √(" + radicandoTexto + ")·" + siguiente
                        + ". Si querías √(" + radicandoTexto + siguiente + "), usa paréntesis");
            }
            return Nodo.fn(TipoFuncion.RAIZ, radicando);
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
            // Forma sin paréntesis: sinx, sin x, sin 2x, ln x, cos x^2
            Nodo argumento = argumentoSinParentesis(nombre);
            Nodo resultado = Nodo.fn(tipo, argumento);
            if (exponente != null) {
                resultado = Nodo.pot(resultado, exponente);
            }
            return resultado;
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

    /**
     * Lee el argumento de una función escrita sin paréntesis. El argumento es
     * un número opcional seguido de x, y, pi o e (cada uno con su potencia):
     * {@code sinx → sin(x)}, {@code sin 2x → sin(2x)}, {@code cos x^2 → cos(x^2)}.
     * Termina al llegar a un operador, un espacio o el nombre de otra función.
     */
    private Nodo argumentoSinParentesis(String nombre) throws ExpresionInvalidaException {
        saltarEspacios();
        int inicio = posicion;
        if (posicion >= texto.length() || !esInicioArgumentoSimple(true)) {
            throw new ExpresionInvalidaException("Después de '" + nombre + "' debe ir el argumento, por ejemplo "
                    + nombre + "(x)", posicion);
        }
        Nodo argumento = factorSimple();
        while (posicion < texto.length() && esInicioArgumentoSimple(false)) {
            argumento = Nodo.mul(argumento, factorSimple());
        }
        String fragmento = texto.substring(inicio, posicion);
        int circunflejo = fragmento.indexOf('^');
        if (circunflejo >= 0) {
            String sinPotencia = fragmento.substring(0, circunflejo);
            String potencia = fragmento.substring(circunflejo);
            avisos.add("«" + nombre + " " + fragmento + "» se leyó como " + nombre + "(" + fragmento + "). Si querías "
                    + "elevar la función, escribe " + nombre + "(" + sinPotencia + ")" + potencia);
        }
        return argumento;
    }

    /** @return true si en la posición actual empieza un número, x, y, pi o e */
    private boolean esInicioArgumentoSimple(boolean admiteNumero) {
        char c = texto.charAt(posicion);
        if (Character.isDigit(c) || c == '.') {
            return admiteNumero;
        }
        if (c == 'x' || c == 'y') {
            return true;
        }
        if (texto.startsWith("pi", posicion)) {
            return true;
        }
        return c == 'e' && !texto.startsWith("exp", posicion);
    }

    /** Un número, x, y, pi o e, con una potencia opcional. */
    private Nodo factorSimple() throws ExpresionInvalidaException {
        Nodo base;
        char c = texto.charAt(posicion);
        if (Character.isDigit(c) || c == '.') {
            base = Nodo.num(leerNumero());
        } else if (c == 'x') {
            posicion++;
            base = Nodo.var(Variable.X);
        } else if (c == 'y') {
            posicion++;
            base = Nodo.var(Variable.Y);
        } else if (c == 'e') {
            posicion++;
            base = NodoConstante.E;
        } else {
            posicion += 2;
            base = NodoConstante.PI;
        }
        if (posicion < texto.length() && texto.charAt(posicion) == '^') {
            posicion++;
            Nodo exponente = unario();
            if (base instanceof NodoConstante && "e".equals(((NodoConstante) base).getSimbolo())) {
                return Nodo.fn(TipoFuncion.EXP, exponente);
            }
            return Nodo.pot(base, exponente);
        }
        return base;
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
