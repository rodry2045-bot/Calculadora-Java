package calculadora.util;

/**
 * Utilidades para mostrar números de forma legible para un estudiante.
 * <p>
 * Reglas de presentación:
 * </p>
 * <ol>
 *   <li>Si el número es (casi) entero, se muestra sin decimales: 6.0 se ve como "6".</li>
 *   <li>Si el número equivale a una fracción N/D con denominador menor o igual
 *       a 100, se muestra como fracción: 0.3333333 se ve como "1/3".</li>
 *   <li>En cualquier otro caso se muestra con hasta 6 decimales: 3.1415926 se ve como "3.141593".</li>
 * </ol>
 * Esta clase no se instancia: todos sus métodos son estáticos.
 */
public final class FormatoNumeros {

    /** Tolerancia para decidir si dos números reales son "iguales". */
    public static final double TOLERANCIA = 1e-9;

    /** Denominador máximo permitido al convertir a fracción. */
    public static final int DENOMINADOR_MAXIMO = 100;

    /** Constructor privado: clase utilitaria, no se instancia. */
    private FormatoNumeros() {
    }

    /**
     * Indica si un número es entero (dentro de la tolerancia).
     *
     * @param valor número a revisar
     * @return true si el número es entero
     */
    public static boolean esEntero(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return false;
        }
        double redondeado = Math.rint(valor);
        return Math.abs(valor - redondeado) < TOLERANCIA;
    }

    /**
     * Compara dos números reales usando la tolerancia.
     *
     * @param a primer número
     * @param b segundo número
     * @return true si |a - b| es menor que la tolerancia
     */
    public static boolean sonIguales(double a, double b) {
        return Math.abs(a - b) < TOLERANCIA;
    }

    /**
     * Intenta expresar un número como fracción N/D con D entre 2 y 100.
     * <p>
     * Algoritmo explícito: se prueba cada denominador D = 2, 3, ..., 100.
     * Para cada uno se calcula N = redondear(valor * D) y se verifica
     * si N / D reproduce el valor original. El primer D que funcione
     * es el más pequeño, por lo que la fracción ya queda simplificada.
     * </p>
     *
     * @param valor número a convertir
     * @return arreglo {numerador, denominador} o null si no hay fracción adecuada
     */
    public static long[] comoFraccion(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return null;
        }
        for (int denominador = 2; denominador <= DENOMINADOR_MAXIMO; denominador++) {
            double producto = valor * denominador;
            long numerador = Math.round(producto);
            double reconstruido = (double) numerador / denominador;
            if (Math.abs(reconstruido - valor) < TOLERANCIA) {
                long[] fraccion = new long[2];
                fraccion[0] = numerador;
                fraccion[1] = denominador;
                return fraccion;
            }
        }
        return null;
    }

    /**
     * Convierte un número en texto siguiendo las reglas de la clase
     * (entero, fracción o decimal).
     *
     * @param valor número a formatear
     * @return representación legible del número
     */
    public static String formatear(double valor) {
        if (Double.isNaN(valor)) {
            return "indefinido";
        }
        if (Double.isInfinite(valor)) {
            if (valor > 0) {
                return "infinito";
            }
            return "-infinito";
        }
        // 1) ¿Es entero?
        if (esEntero(valor)) {
            long entero = Math.round(valor);
            return String.valueOf(entero);
        }
        // 2) ¿Es una fracción con denominador pequeño?
        long[] fraccion = comoFraccion(valor);
        if (fraccion != null) {
            return fraccion[0] + "/" + fraccion[1];
        }
        // 3) Decimal con hasta 6 cifras
        return formatearDecimal(valor, 6);
    }

    /**
     * Como {@link #formatear(double)}, pero además reconoce los infinitos
     * ("∞", "-∞") y los múltiplos racionales de π ("π/2", "3π/4", "2π").
     * Se usa para mostrar resultados finales de límites e integrales.
     *
     * @param valor número
     * @return texto exacto si es posible; si no, decimal
     */
    public static String formatearExacto(double valor) {
        if (Double.isInfinite(valor)) {
            return valor > 0 ? "∞" : "-∞";
        }
        if (Double.isNaN(valor)) {
            return "indefinido";
        }
        if (esEntero(valor) || comoFraccion(valor) != null) {
            return formatear(valor);
        }
        double multiploPi = valor / Math.PI;
        for (int d = 1; d <= 12; d++) {
            double n = Math.rint(multiploPi * d);
            if (n != 0 && Math.abs(n / d - multiploPi) < 1e-9) {
                long numerador = (long) n;
                String texto;
                if (numerador == 1) {
                    texto = "π";
                } else if (numerador == -1) {
                    texto = "-π";
                } else {
                    texto = numerador + "π";
                }
                if (d > 1) {
                    texto = texto + "/" + d;
                }
                return texto;
            }
        }
        return formatearDecimal(valor, 6);
    }

    /**
     * Formatea un número siempre en notación decimal, sin ceros sobrantes.
     * Se usa el punto como separador decimal, independientemente del idioma
     * del sistema operativo, para que el texto sea consistente.
     *
     * @param valor     número a formatear
     * @param decimales cantidad máxima de decimales
     * @return texto con el número en notación decimal
     */
    public static String formatearDecimal(double valor, int decimales) {
        if (Double.isNaN(valor)) {
            return "indefinido";
        }
        if (Double.isInfinite(valor)) {
            return formatear(valor);
        }
        String patron = "%." + decimales + "f";
        String texto = String.format(java.util.Locale.ROOT, patron, valor);
        // Quitar ceros finales y el punto si queda solo
        if (texto.indexOf('.') >= 0) {
            while (texto.endsWith("0")) {
                texto = texto.substring(0, texto.length() - 1);
            }
            if (texto.endsWith(".")) {
                texto = texto.substring(0, texto.length() - 1);
            }
        }
        if (texto.equals("-0")) {
            texto = "0";
        }
        return texto;
    }

    /**
     * Formatea un coeficiente que va delante de una variable o función.
     * <ul>
     *   <li>1 no se escribe: 1·x se ve como "x".</li>
     *   <li>-1 se escribe solo como signo: -1·x se ve como "-x".</li>
     *   <li>Las fracciones se encierran en paréntesis: (3/4)x.</li>
     * </ul>
     *
     * @param coeficiente valor del coeficiente
     * @return texto del coeficiente listo para anteponerse
     */
    public static String formatearCoeficiente(double coeficiente) {
        if (sonIguales(coeficiente, 1.0)) {
            return "";
        }
        if (sonIguales(coeficiente, -1.0)) {
            return "-";
        }
        String texto = formatear(coeficiente);
        if (texto.indexOf('/') >= 0) {
            if (texto.startsWith("-")) {
                return "-(" + texto.substring(1) + ")";
            }
            return "(" + texto + ")";
        }
        return texto;
    }

    /**
     * Formatea un exponente para escribirlo después del símbolo ^.
     * <ul>
     *   <li>Exponente 1: no se escribe nada.</li>
     *   <li>Entero positivo: ^2</li>
     *   <li>Negativo o fraccionario: entre paréntesis, ^(-1) o ^(1/2)</li>
     * </ul>
     *
     * @param exponente valor del exponente
     * @return texto del exponente (incluyendo el ^) o cadena vacía
     */
    public static String formatearExponente(double exponente) {
        if (sonIguales(exponente, 1.0)) {
            return "";
        }
        String texto = formatear(exponente);
        boolean esEnteroPositivo = esEntero(exponente) && exponente > 0;
        if (esEnteroPositivo) {
            return "^" + texto;
        }
        return "^(" + texto + ")";
    }
}
