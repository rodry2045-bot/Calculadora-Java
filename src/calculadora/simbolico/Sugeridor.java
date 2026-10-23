package calculadora.simbolico;

import calculadora.excepciones.ExpresionInvalidaException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Propone una corrección cuando una expresión no se puede interpretar.
 * <p>
 * Aplica arreglos comunes uno tras otro y devuelve el primer texto que sí se
 * puede interpretar:
 * </p>
 * <ol>
 *   <li>palabras en español: seno → sin, coseno → cos, raiz → √...;</li>
 *   <li>nombres mal escritos: son(x) → sin(x), sqr(x) → sqrt(x), lm(x) → ln(x);</li>
 *   <li>una letra desconocida sola (t, z, a...) se cambia por x;</li>
 *   <li>paréntesis o barras | sin cerrar, y paréntesis vacíos;</li>
 *   <li>un operador suelto al final (x^2 +).</li>
 * </ol>
 */
public final class Sugeridor {

    private static final String[][] PALABRAS = {
        {"seno", "sin"}, {"coseno", "cos"}, {"tangente", "tan"}, {"logaritmo", "ln"}, {"raizcuadrada", "sqrt"},
        {"exponencial", "exp"}, {"arcoseno", "arcsin"}, {"arcocoseno", "arccos"}, {"arcotangente", "arctan"}
    };

    private static final Pattern PALABRA = Pattern.compile("[a-záéíóúñ]+");

    private Sugeridor() {
    }

    /**
     * @param texto expresión escrita por el estudiante
     * @return texto corregido que sí se puede interpretar, o null si no hay sugerencia
     */
    public static String sugerir(String texto) {
        if (texto == null || texto.trim().isEmpty() || seInterpreta(texto)) {
            return null;
        }
        String candidato = AnalizadorSimbolico.quitarPrefijoFuncion(texto).trim();
        String[] pasos = {"palabras", "nombres", "letras", "parentesis", "final"};
        for (int i = 0; i < pasos.length; i++) {
            candidato = aplicar(pasos[i], candidato);
            if (!candidato.equals(texto) && seInterpreta(candidato)) {
                return candidato;
            }
        }
        // Último intento: paréntesis y final después de todos los demás arreglos
        candidato = aplicar("parentesis", aplicar("final", candidato));
        if (!candidato.equals(texto) && seInterpreta(candidato)) {
            return candidato;
        }
        return null;
    }

    private static String aplicar(String paso, String texto) {
        if (paso.equals("palabras")) {
            String resultado = texto.replace("**", "^").replace("raíz", "raiz");
            for (int i = 0; i < PALABRAS.length; i++) {
                resultado = resultado.replaceAll("(?i)" + PALABRAS[i][0], PALABRAS[i][1]);
            }
            return resultado;
        }
        if (paso.equals("nombres")) {
            return corregirNombres(texto, false);
        }
        if (paso.equals("letras")) {
            return corregirNombres(texto, true);
        }
        if (paso.equals("parentesis")) {
            return balancear(texto);
        }
        return quitarOperadorFinal(texto);
    }

    /** Reemplaza cada palabra que el analizador no entiende por el nombre más parecido. */
    private static String corregirNombres(String texto, boolean cambiarLetrasSueltas) {
        String minusculas = texto.toLowerCase();
        Matcher m = PALABRA.matcher(minusculas);
        StringBuilder salida = new StringBuilder();
        int ultimo = 0;
        while (m.find()) {
            String palabra = m.group();
            String reemplazo = palabra;
            if (!seDescompone(palabra)) {
                if (palabra.length() == 1) {
                    if (cambiarLetrasSueltas) {
                        reemplazo = "x";
                    }
                } else {
                    String mejor = masParecido(palabra);
                    if (mejor != null) {
                        reemplazo = mejor;
                    }
                }
            }
            salida.append(texto, ultimo, m.start()).append(reemplazo);
            ultimo = m.end();
        }
        salida.append(texto.substring(ultimo));
        return salida.toString();
    }

    /** @return true si la palabra se puede leer como nombres de funciones, x, y, e y pi seguidos */
    private static boolean seDescompone(String palabra) {
        int i = 0;
        while (i < palabra.length()) {
            boolean avanzo = false;
            String[] nombres = AnalizadorSimbolico.NOMBRES_PUBLICOS;
            int mejor = 0;
            for (int k = 0; k < nombres.length; k++) {
                if (palabra.startsWith(nombres[k], i) && nombres[k].length() > mejor) {
                    mejor = nombres[k].length();
                }
            }
            if (palabra.startsWith("raiz", i) || palabra.startsWith("sqrt", i)) {
                mejor = Math.max(mejor, 4);
            }
            if (mejor > 0) {
                i += mejor;
                avanzo = true;
            } else if (palabra.startsWith("pi", i)) {
                i += 2;
                avanzo = true;
            } else {
                char c = palabra.charAt(i);
                if (c == 'x' || c == 'y' || c == 'e') {
                    i++;
                    avanzo = true;
                }
            }
            if (!avanzo) {
                return false;
            }
        }
        return true;
    }

    private static String masParecido(String palabra) {
        String[] nombres = AnalizadorSimbolico.NOMBRES_PUBLICOS;
        String mejor = null;
        int distanciaMejor = Integer.MAX_VALUE;
        for (int k = 0; k < nombres.length; k++) {
            int d = distancia(palabra, nombres[k]);
            if (d < distanciaMejor) {
                distanciaMejor = d;
                mejor = nombres[k];
            }
        }
        int permitida = palabra.length() <= 3 ? 1 : 2;
        return distanciaMejor <= permitida ? mejor : null;
    }

    /** Distancia de Levenshtein: cuántas letras hay que cambiar, quitar o poner. */
    private static int distancia(String a, String b) {
        int[][] tabla = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            tabla[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            tabla[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int minimo = Math.min(tabla[i - 1][j] + 1, tabla[i][j - 1] + 1);
                tabla[i][j] = Math.min(minimo, tabla[i - 1][j - 1] + costo);
            }
        }
        return tabla[a.length()][b.length()];
    }

    private static String balancear(String texto) {
        String resultado = texto.replace("()", "(x)");
        int abiertos = 0;
        StringBuilder limpio = new StringBuilder();
        for (int i = 0; i < resultado.length(); i++) {
            char c = resultado.charAt(i);
            if (c == '(') {
                abiertos++;
            } else if (c == ')') {
                if (abiertos == 0) {
                    continue; // paréntesis de cierre sobrante
                }
                abiertos--;
            }
            limpio.append(c);
        }
        for (int i = 0; i < abiertos; i++) {
            limpio.append(')');
        }
        int barras = 0;
        for (int i = 0; i < limpio.length(); i++) {
            if (limpio.charAt(i) == '|') {
                barras++;
            }
        }
        if (barras % 2 == 1) {
            limpio.append('|');
        }
        return limpio.toString();
    }

    private static String quitarOperadorFinal(String texto) {
        String resultado = texto.trim();
        while (!resultado.isEmpty() && "+-*/^·".indexOf(resultado.charAt(resultado.length() - 1)) >= 0) {
            resultado = resultado.substring(0, resultado.length() - 1).trim();
        }
        return resultado;
    }

    private static boolean seInterpreta(String texto) {
        try {
            String limpio = AnalizadorSimbolico.quitarPrefijoFuncion(texto);
            int igual = limpio.indexOf('=');
            AnalizadorSimbolico analizador = new AnalizadorSimbolico();
            if (igual >= 0) {
                analizador.analizar(limpio.substring(0, igual));
                analizador.analizar(limpio.substring(igual + 1));
            } else {
                analizador.analizar(limpio);
            }
            return true;
        } catch (ExpresionInvalidaException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
