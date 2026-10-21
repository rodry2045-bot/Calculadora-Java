package calculadora.analisis;

import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.modelo.Expresion;
import calculadora.modelo.Termino;
import calculadora.modelo.TerminoCoseno;
import calculadora.modelo.TerminoExponencial;
import calculadora.modelo.TerminoFuncional;
import calculadora.modelo.TerminoLogaritmico;
import calculadora.modelo.TerminoPolinomico;
import calculadora.modelo.TerminoSeno;
import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

import java.util.List;

/**
 * Analizador sintáctico (parser) de DESCENSO RECURSIVO, escrito a mano y
 * recorriendo el texto carácter por carácter.
 * <p>
 * Gramática reconocida (notación EBNF simplificada):
 * </p>
 * <pre>
 * expresion  ::= termino { ('+' | '-') termino }
 * termino    ::= factor { ('*' | '/' | multiplicación implícita) factor }
 * factor     ::= ('+' | '-') factor | potencia
 * potencia   ::= primario [ '^' exponente ]
 * primario   ::= numero | 'x' | 'y' | 'pi' | 'e' [ '^' argumentoExp ]
 *              | funcion '(' expresion ')' | '(' expresion ')'
 * funcion    ::= 'sin' | 'sen' | 'cos' | 'exp' | 'ln'
 * exponente  ::= ['+' | '-'] ( numero | '(' expresion constante ')' )
 * </pre>
 * <p>
 * Ejemplos válidos: {@code 3x^2 - 2x + 1}, {@code x^2*y}, {@code 2sin(3x)},
 * {@code e^(2x)}, {@code 1/x}, {@code x^(1/2)}, {@code y*cos(x)}, {@code 4ln(x)}.
 * </p>
 * <p>
 * Restricciones (para mantener el motor sencillo y didáctico):
 * </p>
 * <ul>
 *   <li>El argumento de cada función debe ser lineal: k·x o k·y.</li>
 *   <li>No se permite multiplicar dos funciones entre sí (sin(x)·cos(x)).</li>
 *   <li>Un paréntesis que contiene una suma no puede multiplicarse:
 *       (x+1)·x debe escribirse ya desarrollado: x^2 + x.</li>
 * </ul>
 */
public class AnalizadorExpresion {

    /** Texto que se está analizando (ya normalizado). */
    private String texto;

    /** Posición del carácter actual dentro del texto. */
    private int posicion;

    /**
     * Convierte un texto en un objeto {@link Expresion}.
     *
     * @param entrada texto escrito por el usuario
     * @return expresión simplificada (términos semejantes agrupados)
     * @throws ExpresionInvalidaException si el texto no es una expresión válida
     */
    public Expresion analizar(String entrada) throws ExpresionInvalidaException {
        if (entrada == null || entrada.trim().isEmpty()) {
            throw new ExpresionInvalidaException("La expresión está vacía. Escriba, por ejemplo: 3x^2 + 2x");
        }
        this.texto = normalizar(entrada);
        this.posicion = 0;

        Expresion resultado = analizarExpresion();

        saltarEspacios();
        if (posicion < texto.length()) {
            char inesperado = texto.charAt(posicion);
            if (inesperado == ')') {
                throw new ExpresionInvalidaException("Hay un paréntesis de cierre ')' sin su apertura", posicion);
            }
            throw new ExpresionInvalidaException("Carácter inesperado '" + inesperado + "'", posicion);
        }
        return resultado.simplificar();
    }

    /**
     * Reemplaza símbolos alternativos por los que entiende el analizador.
     *
     * @param entrada texto original
     * @return texto normalizado en minúsculas
     */
    private String normalizar(String entrada) {
        String normal = entrada.toLowerCase();
        normal = normal.replace('·', '*');
        normal = normal.replace('×', '*');
        normal = normal.replace('−', '-');
        normal = normal.replace('÷', '/');
        normal = normal.replace(',', '.');
        normal = normal.replace("π", "pi");
        return normal;
    }

    // ------------------------------------------------------------------
    // Reglas de la gramática
    // ------------------------------------------------------------------

    /**
     * expresion ::= termino { ('+' | '-') termino }
     */
    private Expresion analizarExpresion() throws ExpresionInvalidaException {
        Expresion expresion = new Expresion();

        Termino primero = analizarTermino();
        expresion.agregar(primero);

        while (true) {
            saltarEspacios();
            if (posicion >= texto.length()) {
                break;
            }
            char actual = texto.charAt(posicion);
            if (actual == '+') {
                posicion++;
                Termino siguiente = analizarTermino();
                expresion.agregar(siguiente);
            } else if (actual == '-') {
                posicion++;
                Termino siguiente = analizarTermino();
                // Restar equivale a sumar el término con el coeficiente cambiado de signo
                expresion.agregar(cambiarSigno(siguiente));
            } else {
                break;
            }
        }
        return expresion;
    }

    /**
     * termino ::= factor { ('*' | '/' | implícita) factor }
     */
    private Termino analizarTermino() throws ExpresionInvalidaException {
        Termino acumulado = analizarFactor();

        while (true) {
            saltarEspacios();
            if (posicion >= texto.length()) {
                break;
            }
            char actual = texto.charAt(posicion);
            if (actual == '*') {
                posicion++;
                Termino otro = analizarFactor();
                acumulado = multiplicar(acumulado, otro);
            } else if (actual == '/') {
                int posicionDivision = posicion;
                posicion++;
                Termino divisor = analizarFactor();
                acumulado = dividir(acumulado, divisor, posicionDivision);
            } else if (esInicioDeFactor(actual)) {
                // Multiplicación implícita: 2x, 3sin(x), x y, (1/2)x
                Termino otro = analizarFactor();
                acumulado = multiplicar(acumulado, otro);
            } else {
                break;
            }
        }
        return acumulado;
    }

    /**
     * factor ::= ('+' | '-') factor | potencia
     */
    private Termino analizarFactor() throws ExpresionInvalidaException {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw new ExpresionInvalidaException("La expresión termina de forma incompleta", posicion);
        }
        char actual = texto.charAt(posicion);
        if (actual == '-') {
            posicion++;
            return cambiarSigno(analizarFactor());
        }
        if (actual == '+') {
            posicion++;
            return analizarFactor();
        }
        return analizarPotencia();
    }

    /**
     * potencia ::= primario [ '^' exponente ]
     */
    private Termino analizarPotencia() throws ExpresionInvalidaException {
        int inicio = posicion;
        Termino base = analizarPrimario();
        saltarEspacios();
        if (posicion < texto.length() && texto.charAt(posicion) == '^') {
            posicion++;
            double exponente = analizarExponenteNumerico();
            return elevar(base, exponente, inicio);
        }
        return base;
    }

    /**
     * primario ::= numero | variable | constante | funcion(...) | ( expresion )
     */
    private Termino analizarPrimario() throws ExpresionInvalidaException {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw new ExpresionInvalidaException("Falta un número, variable o función", posicion);
        }
        char actual = texto.charAt(posicion);

        // 1) Número
        if (Character.isDigit(actual) || actual == '.') {
            double numero = leerNumero();
            return TerminoPolinomico.constante(numero);
        }

        // 2) Paréntesis de agrupación
        if (actual == '(') {
            return analizarGrupo();
        }

        // 3) Funciones y constantes con nombre (se revisan antes que las variables)
        if (empiezaCon("sin") || empiezaCon("sen")) {
            posicion += 3;
            return analizarFuncion("sin");
        }
        if (empiezaCon("cos")) {
            posicion += 3;
            return analizarFuncion("cos");
        }
        if (empiezaCon("exp")) {
            posicion += 3;
            return analizarFuncion("exp");
        }
        if (empiezaCon("ln")) {
            posicion += 2;
            return analizarFuncion("ln");
        }
        if (empiezaCon("pi")) {
            posicion += 2;
            return TerminoPolinomico.constante(Math.PI);
        }
        if (actual == 'e') {
            posicion++;
            return analizarNumeroE();
        }

        // 4) Variables
        Variable variable = Variable.desdeCaracter(actual);
        if (variable != null) {
            posicion++;
            if (variable == Variable.X) {
                return new TerminoPolinomico(1.0, 1.0, 0.0);
            }
            return new TerminoPolinomico(1.0, 0.0, 1.0);
        }

        if (Character.isLetter(actual)) {
            throw new ExpresionInvalidaException("Función o símbolo desconocido que empieza con '" + actual
                    + "'. Use x, y, sin, cos, e^, exp o ln", posicion);
        }
        throw new ExpresionInvalidaException("Carácter inesperado '" + actual + "'", posicion);
    }

    /**
     * Analiza un paréntesis de agrupación: ( expresion ).
     * Solo se acepta si el contenido es un único término, como (1/2) o (2x).
     */
    private Termino analizarGrupo() throws ExpresionInvalidaException {
        int inicio = posicion;
        posicion++; // consumir '('
        Expresion interior = analizarExpresion();
        esperarCaracter(')', "Falta cerrar el paréntesis abierto", inicio);

        Expresion simplificada = interior.simplificar();
        if (simplificada.estaVacia()) {
            return TerminoPolinomico.constante(0.0);
        }
        if (simplificada.cantidadTerminos() > 1) {
            throw new ExpresionInvalidaException("No se puede multiplicar o elevar una suma entre paréntesis ("
                    + simplificada + "). Escriba la expresión ya desarrollada", inicio);
        }
        return simplificada.getTerminos().get(0);
    }

    /**
     * Analiza el argumento de una función: nombre ( k·v ).
     *
     * @param nombre "sin", "cos", "exp" o "ln"
     */
    private Termino analizarFuncion(String nombre) throws ExpresionInvalidaException {
        saltarEspacios();
        int inicio = posicion;
        if (posicion >= texto.length() || texto.charAt(posicion) != '(') {
            throw new ExpresionInvalidaException("Después de '" + nombre + "' debe ir un paréntesis, por ejemplo "
                    + nombre + "(x)", posicion);
        }
        posicion++; // consumir '('
        Expresion argumento = analizarExpresion();
        esperarCaracter(')', "Falta cerrar el paréntesis de " + nombre + "(", inicio);
        return construirFuncion(nombre, argumento, inicio);
    }

    /**
     * Analiza lo que sigue a la letra 'e': puede ser la constante e o una
     * exponencial e^(k·v).
     */
    private Termino analizarNumeroE() throws ExpresionInvalidaException {
        saltarEspacios();
        if (posicion >= texto.length() || texto.charAt(posicion) != '^') {
            // Constante de Euler sola
            return TerminoPolinomico.constante(Math.E);
        }
        posicion++; // consumir '^'
        saltarEspacios();
        int inicio = posicion;

        if (posicion < texto.length() && texto.charAt(posicion) == '(') {
            posicion++;
            Expresion argumento = analizarExpresion();
            esperarCaracter(')', "Falta cerrar el paréntesis del exponente de e", inicio);
            return construirFuncion("exp", argumento, inicio);
        }

        // Forma corta sin paréntesis: e^x, e^-x, e^2x, e^2
        double signo = 1.0;
        if (posicion < texto.length() && (texto.charAt(posicion) == '-' || texto.charAt(posicion) == '+')) {
            if (texto.charAt(posicion) == '-') {
                signo = -1.0;
            }
            posicion++;
        }
        boolean hayNumero = false;
        double k = 1.0;
        if (posicion < texto.length() && (Character.isDigit(texto.charAt(posicion)) || texto.charAt(posicion) == '.')) {
            k = leerNumero();
            hayNumero = true;
        }
        Variable variable = null;
        if (posicion < texto.length()) {
            variable = Variable.desdeCaracter(texto.charAt(posicion));
            if (variable != null) {
                posicion++;
            }
        }
        if (!hayNumero && variable == null) {
            throw new ExpresionInvalidaException("El exponente de e no es válido. Use e^x, e^(2x) o e^2", inicio);
        }
        k = signo * k;
        if (variable == null) {
            return TerminoPolinomico.constante(Math.exp(k));
        }
        return new TerminoExponencial(1.0, 0.0, 0.0, variable, k);
    }

    /**
     * exponente ::= ['+'|'-'] ( numero | '(' expresion constante ')' )
     *
     * @return valor numérico del exponente
     */
    private double analizarExponenteNumerico() throws ExpresionInvalidaException {
        saltarEspacios();
        int inicio = posicion;
        if (posicion >= texto.length()) {
            throw new ExpresionInvalidaException("Falta el exponente después de '^'", posicion);
        }
        char actual = texto.charAt(posicion);
        if (actual == '-') {
            posicion++;
            return -analizarExponenteNumerico();
        }
        if (actual == '+') {
            posicion++;
            return analizarExponenteNumerico();
        }
        if (Character.isDigit(actual) || actual == '.') {
            return leerNumero();
        }
        if (actual == '(') {
            posicion++;
            Expresion interior = analizarExpresion();
            esperarCaracter(')', "Falta cerrar el paréntesis del exponente", inicio);
            Expresion simplificada = interior.simplificar();
            if (simplificada.dependeDe(Variable.X) || simplificada.dependeDe(Variable.Y)) {
                throw new ExpresionInvalidaException("El exponente debe ser un número constante "
                        + "(las potencias variables como x^x no están soportadas)", inicio);
            }
            return simplificada.evaluar(0.0, 0.0);
        }
        throw new ExpresionInvalidaException("El exponente debe ser un número, por ejemplo x^2 o x^(1/2)", inicio);
    }

    // ------------------------------------------------------------------
    // Construcción de términos
    // ------------------------------------------------------------------

    /**
     * Crea el término de función a partir de su argumento, validando que el
     * argumento sea de la forma k·v o una constante.
     */
    private Termino construirFuncion(String nombre, Expresion argumento, int inicio)
            throws ExpresionInvalidaException {
        Expresion simplificado = argumento.simplificar();

        // Caso A: argumento constante, la función se convierte en un número
        boolean dependeDeX = simplificado.dependeDe(Variable.X);
        boolean dependeDeY = simplificado.dependeDe(Variable.Y);
        if (!dependeDeX && !dependeDeY) {
            double valor = simplificado.evaluar(0.0, 0.0);
            double resultado = aplicarFuncionNumerica(nombre, valor);
            if (Double.isNaN(resultado) || Double.isInfinite(resultado)) {
                throw new ExpresionInvalidaException(nombre + "(" + FormatoNumeros.formatear(valor)
                        + ") no está definido en los números reales", inicio);
            }
            return TerminoPolinomico.constante(resultado);
        }

        // Caso B: argumento lineal k·v
        String mensajeFormato = "El argumento de " + nombre + " debe ser de la forma k·x o k·y, "
                + "por ejemplo " + nombre + "(2x). Se recibió: " + simplificado;
        if (simplificado.cantidadTerminos() != 1) {
            throw new ExpresionInvalidaException(mensajeFormato, inicio);
        }
        Termino unico = simplificado.getTerminos().get(0);
        if (unico instanceof TerminoFuncional) {
            throw new ExpresionInvalidaException("No se admiten funciones anidadas como " + nombre + "("
                    + unico + ")", inicio);
        }
        double exponenteX = unico.getExponenteX();
        double exponenteY = unico.getExponenteY();
        Variable variable;
        if (FormatoNumeros.sonIguales(exponenteX, 1.0) && FormatoNumeros.sonIguales(exponenteY, 0.0)) {
            variable = Variable.X;
        } else if (FormatoNumeros.sonIguales(exponenteY, 1.0) && FormatoNumeros.sonIguales(exponenteX, 0.0)) {
            variable = Variable.Y;
        } else {
            throw new ExpresionInvalidaException(mensajeFormato, inicio);
        }
        double k = unico.getCoeficiente();

        if (nombre.equals("sin")) {
            return new TerminoSeno(1.0, 0.0, 0.0, variable, k);
        }
        if (nombre.equals("cos")) {
            return new TerminoCoseno(1.0, 0.0, 0.0, variable, k);
        }
        if (nombre.equals("exp")) {
            return new TerminoExponencial(1.0, 0.0, 0.0, variable, k);
        }
        return new TerminoLogaritmico(1.0, 0.0, 0.0, variable, k, false);
    }

    /**
     * Evalúa una función con argumento numérico constante.
     */
    private double aplicarFuncionNumerica(String nombre, double valor) {
        if (nombre.equals("sin")) {
            return Math.sin(valor);
        }
        if (nombre.equals("cos")) {
            return Math.cos(valor);
        }
        if (nombre.equals("exp")) {
            return Math.exp(valor);
        }
        return Math.log(valor);
    }

    /**
     * Multiplica dos términos: se multiplican los coeficientes y se suman
     * los exponentes. Solo uno de los dos puede contener una función.
     */
    private Termino multiplicar(Termino a, Termino b) throws ExpresionInvalidaException {
        boolean aEsFuncion = a instanceof TerminoFuncional;
        boolean bEsFuncion = b instanceof TerminoFuncional;
        if (aEsFuncion && bEsFuncion) {
            throw new ExpresionInvalidaException("No se admite el producto de dos funciones (" + a + ")·(" + b
                    + "); requiere la regla del producto, aún no soportada");
        }
        double coeficiente = a.getCoeficiente() * b.getCoeficiente();
        double exponenteX = a.getExponenteX() + b.getExponenteX();
        double exponenteY = a.getExponenteY() + b.getExponenteY();
        if (bEsFuncion) {
            // El resultado conserva la función de b
            return b.conParteAlgebraica(coeficiente, exponenteX, exponenteY);
        }
        return a.conParteAlgebraica(coeficiente, exponenteX, exponenteY);
    }

    /**
     * Divide dos términos. El divisor debe ser polinómico (sin funciones).
     */
    private Termino dividir(Termino dividendo, Termino divisor, int posicionDivision)
            throws ExpresionInvalidaException {
        if (divisor instanceof TerminoFuncional) {
            throw new ExpresionInvalidaException("No se admite dividir entre una función (" + divisor + ")",
                    posicionDivision);
        }
        if (divisor.esCero()) {
            throw new ExpresionInvalidaException("División entre cero", posicionDivision);
        }
        double coeficiente = dividendo.getCoeficiente() / divisor.getCoeficiente();
        double exponenteX = dividendo.getExponenteX() - divisor.getExponenteX();
        double exponenteY = dividendo.getExponenteY() - divisor.getExponenteY();
        return dividendo.conParteAlgebraica(coeficiente, exponenteX, exponenteY);
    }

    /**
     * Eleva un término polinómico a una potencia constante: (c·x^a·y^b)^n = c^n·x^(a·n)·y^(b·n).
     */
    private Termino elevar(Termino base, double exponente, int inicio) throws ExpresionInvalidaException {
        if (base instanceof TerminoFuncional) {
            throw new ExpresionInvalidaException("No se admite elevar una función a una potencia (" + base
                    + ")^" + FormatoNumeros.formatear(exponente), inicio);
        }
        double coeficiente = Math.pow(base.getCoeficiente(), exponente);
        if (Double.isNaN(coeficiente) || Double.isInfinite(coeficiente)) {
            throw new ExpresionInvalidaException("La potencia " + FormatoNumeros.formatear(base.getCoeficiente())
                    + "^" + FormatoNumeros.formatear(exponente) + " no es un número real", inicio);
        }
        return new TerminoPolinomico(coeficiente, base.getExponenteX() * exponente,
                base.getExponenteY() * exponente);
    }

    /**
     * Devuelve el término con el coeficiente multiplicado por -1.
     */
    private Termino cambiarSigno(Termino termino) {
        return termino.conParteAlgebraica(-termino.getCoeficiente(), termino.getExponenteX(),
                termino.getExponenteY());
    }

    // ------------------------------------------------------------------
    // Utilidades de lectura carácter por carácter
    // ------------------------------------------------------------------

    /** Avanza la posición mientras haya espacios en blanco. */
    private void saltarEspacios() {
        while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }

    /**
     * @param palabra texto a buscar
     * @return true si el texto, desde la posición actual, empieza con la palabra
     */
    private boolean empiezaCon(String palabra) {
        return texto.startsWith(palabra, posicion);
    }

    /**
     * @param caracter carácter actual
     * @return true si el carácter puede iniciar un factor (multiplicación implícita)
     */
    private boolean esInicioDeFactor(char caracter) {
        return Character.isLetterOrDigit(caracter) || caracter == '.' || caracter == '(';
    }

    /**
     * Lee un número decimal como 12, 3.5 o .25 carácter por carácter.
     *
     * @return valor del número
     */
    private double leerNumero() throws ExpresionInvalidaException {
        int inicio = posicion;
        boolean hayPunto = false;
        while (posicion < texto.length()) {
            char actual = texto.charAt(posicion);
            if (Character.isDigit(actual)) {
                posicion++;
            } else if (actual == '.' && !hayPunto) {
                hayPunto = true;
                posicion++;
            } else {
                break;
            }
        }
        String numeroTexto = texto.substring(inicio, posicion);
        if (numeroTexto.equals(".")) {
            throw new ExpresionInvalidaException("Número mal escrito", inicio);
        }
        return Double.parseDouble(numeroTexto);
    }

    /**
     * Verifica que el carácter actual sea el esperado y lo consume.
     */
    private void esperarCaracter(char esperado, String mensaje, int posicionApertura)
            throws ExpresionInvalidaException {
        saltarEspacios();
        if (posicion >= texto.length() || texto.charAt(posicion) != esperado) {
            throw new ExpresionInvalidaException(mensaje, posicionApertura);
        }
        posicion++;
    }

    /**
     * Método auxiliar para pruebas: devuelve los términos como texto.
     *
     * @param expresion expresión a describir
     * @return términos separados por " | "
     */
    public static String describirTerminos(Expresion expresion) {
        StringBuilder texto = new StringBuilder();
        List<Termino> terminos = expresion.getTerminos();
        for (int i = 0; i < terminos.size(); i++) {
            if (i > 0) {
                texto.append(" | ");
            }
            Termino termino = terminos.get(i);
            texto.append(termino.getClass().getSimpleName()).append(":").append(termino);
        }
        return texto.toString();
    }
}
