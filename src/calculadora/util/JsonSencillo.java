package calculadora.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector y escritor de JSON escrito a mano, SIN librerías externas.
 * <p>
 * <b>Lectura (parseo):</b> convierte un texto JSON en objetos de Java:
 * </p>
 * <ul>
 *   <li>objeto JSON  → {@code Map<String, Object>}</li>
 *   <li>arreglo JSON → {@code List<Object>}</li>
 *   <li>texto        → {@code String}</li>
 *   <li>número       → {@code Double}</li>
 *   <li>true/false   → {@code Boolean}</li>
 *   <li>null         → {@code null}</li>
 * </ul>
 * <p>
 * <b>Escritura:</b> ofrece métodos para escapar textos y construir objetos
 * y arreglos JSON de forma explícita.
 * </p>
 */
public final class JsonSencillo {

    /** Texto que se está leyendo. */
    private final String texto;

    /** Posición actual de lectura. */
    private int posicion;

    /**
     * Constructor privado: se usa a través del método estático {@link #parsear(String)}.
     *
     * @param texto JSON a leer
     */
    private JsonSencillo(String texto) {
        this.texto = texto;
        this.posicion = 0;
    }

    // ==================================================================
    // ESCRITURA
    // ==================================================================

    /**
     * Convierte un texto de Java en un texto JSON entre comillas, escapando
     * los caracteres especiales (comillas, barras, saltos de línea, etc.).
     *
     * @param valor texto original
     * @return texto JSON, por ejemplo "hola \"mundo\""
     */
    public static String cadena(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder resultado = new StringBuilder();
        resultado.append('"');
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"':
                    resultado.append("\\\"");
                    break;
                case '\\':
                    resultado.append("\\\\");
                    break;
                case '\n':
                    resultado.append("\\n");
                    break;
                case '\r':
                    resultado.append("\\r");
                    break;
                case '\t':
                    resultado.append("\\t");
                    break;
                case '\b':
                    resultado.append("\\b");
                    break;
                case '\f':
                    resultado.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        // Caracteres de control: se escriben como \\uXXXX
                        resultado.append(String.format("\\u%04x", (int) c));
                    } else {
                        resultado.append(c);
                    }
            }
        }
        resultado.append('"');
        return resultado.toString();
    }

    /**
     * Construye un objeto JSON a partir de pares clave/valor ya convertidos a JSON.
     * <p>
     * Ejemplo: {@code objeto("role", cadena("user"), "content", cadena("hola"))}
     * produce {@code {"role":"user","content":"hola"}}.
     * </p>
     *
     * @param paresClaveValor clave1, valorJson1, clave2, valorJson2, ...
     * @return texto del objeto JSON
     */
    public static String objeto(String... paresClaveValor) {
        if (paresClaveValor.length % 2 != 0) {
            throw new IllegalArgumentException("Se requiere un número par de argumentos (clave, valor)");
        }
        StringBuilder resultado = new StringBuilder();
        resultado.append('{');
        for (int i = 0; i < paresClaveValor.length; i = i + 2) {
            if (i > 0) {
                resultado.append(',');
            }
            resultado.append(cadena(paresClaveValor[i]));
            resultado.append(':');
            resultado.append(paresClaveValor[i + 1]);
        }
        resultado.append('}');
        return resultado.toString();
    }

    /**
     * Construye un arreglo JSON a partir de elementos ya convertidos a JSON.
     *
     * @param elementosJson elementos del arreglo
     * @return texto del arreglo JSON
     */
    public static String arreglo(String... elementosJson) {
        StringBuilder resultado = new StringBuilder();
        resultado.append('[');
        for (int i = 0; i < elementosJson.length; i++) {
            if (i > 0) {
                resultado.append(',');
            }
            resultado.append(elementosJson[i]);
        }
        resultado.append(']');
        return resultado.toString();
    }

    // ==================================================================
    // LECTURA
    // ==================================================================

    /**
     * Convierte un texto JSON en objetos de Java.
     *
     * @param json texto JSON
     * @return Map, List, String, Double, Boolean o null
     * @throws IllegalArgumentException si el JSON está mal formado
     */
    public static Object parsear(String json) {
        if (json == null) {
            throw new IllegalArgumentException("El texto JSON es nulo");
        }
        JsonSencillo lector = new JsonSencillo(json);
        lector.saltarEspacios();
        Object valor = lector.leerValor();
        lector.saltarEspacios();
        if (lector.posicion < lector.texto.length()) {
            throw lector.error("Hay texto sobrante después del JSON");
        }
        return valor;
    }

    /**
     * Navega dentro de una estructura JSON ya parseada.
     * <p>
     * Ejemplo: {@code obtenerRuta(raiz, "choices", 0, "message", "content")}
     * equivale a {@code raiz.choices[0].message.content}.
     * </p>
     *
     * @param raiz  estructura obtenida con {@link #parsear(String)}
     * @param ruta  claves (String) o índices (Integer)
     * @return el valor encontrado o null si la ruta no existe
     */
    @SuppressWarnings("unchecked")
    public static Object obtenerRuta(Object raiz, Object... ruta) {
        Object actual = raiz;
        for (int i = 0; i < ruta.length; i++) {
            Object paso = ruta[i];
            if (actual == null) {
                return null;
            }
            if (paso instanceof String && actual instanceof Map) {
                Map<String, Object> mapa = (Map<String, Object>) actual;
                actual = mapa.get((String) paso);
            } else if (paso instanceof Integer && actual instanceof List) {
                List<Object> lista = (List<Object>) actual;
                int indice = ((Integer) paso).intValue();
                if (indice < 0 || indice >= lista.size()) {
                    return null;
                }
                actual = lista.get(indice);
            } else {
                return null;
            }
        }
        return actual;
    }

    /**
     * Lee cualquier valor JSON según su primer carácter.
     */
    private Object leerValor() {
        saltarEspacios();
        if (posicion >= texto.length()) {
            throw error("Fin inesperado del JSON");
        }
        char actual = texto.charAt(posicion);
        if (actual == '{') {
            return leerObjeto();
        }
        if (actual == '[') {
            return leerArreglo();
        }
        if (actual == '"') {
            return leerCadena();
        }
        if (actual == '-' || Character.isDigit(actual)) {
            return leerNumero();
        }
        if (texto.startsWith("true", posicion)) {
            posicion += 4;
            return Boolean.TRUE;
        }
        if (texto.startsWith("false", posicion)) {
            posicion += 5;
            return Boolean.FALSE;
        }
        if (texto.startsWith("null", posicion)) {
            posicion += 4;
            return null;
        }
        throw error("Carácter inesperado '" + actual + "'");
    }

    /**
     * Lee un objeto: { "clave": valor, ... }
     */
    private Map<String, Object> leerObjeto() {
        Map<String, Object> mapa = new LinkedHashMap<String, Object>();
        posicion++; // consumir '{'
        saltarEspacios();
        if (posicion < texto.length() && texto.charAt(posicion) == '}') {
            posicion++;
            return mapa;
        }
        while (true) {
            saltarEspacios();
            if (posicion >= texto.length() || texto.charAt(posicion) != '"') {
                throw error("Se esperaba una clave entre comillas");
            }
            String clave = leerCadena();
            saltarEspacios();
            consumir(':');
            Object valor = leerValor();
            mapa.put(clave, valor);
            saltarEspacios();
            if (posicion >= texto.length()) {
                throw error("Objeto sin cerrar");
            }
            char separador = texto.charAt(posicion);
            if (separador == ',') {
                posicion++;
            } else if (separador == '}') {
                posicion++;
                return mapa;
            } else {
                throw error("Se esperaba ',' o '}'");
            }
        }
    }

    /**
     * Lee un arreglo: [ valor, valor, ... ]
     */
    private List<Object> leerArreglo() {
        List<Object> lista = new ArrayList<Object>();
        posicion++; // consumir '['
        saltarEspacios();
        if (posicion < texto.length() && texto.charAt(posicion) == ']') {
            posicion++;
            return lista;
        }
        while (true) {
            Object valor = leerValor();
            lista.add(valor);
            saltarEspacios();
            if (posicion >= texto.length()) {
                throw error("Arreglo sin cerrar");
            }
            char separador = texto.charAt(posicion);
            if (separador == ',') {
                posicion++;
            } else if (separador == ']') {
                posicion++;
                return lista;
            } else {
                throw error("Se esperaba ',' o ']'");
            }
        }
    }

    /**
     * Lee un texto entre comillas procesando las secuencias de escape.
     */
    private String leerCadena() {
        posicion++; // consumir la comilla inicial
        StringBuilder resultado = new StringBuilder();
        while (posicion < texto.length()) {
            char c = texto.charAt(posicion);
            posicion++;
            if (c == '"') {
                return resultado.toString();
            }
            if (c != '\\') {
                resultado.append(c);
                continue;
            }
            if (posicion >= texto.length()) {
                throw error("Secuencia de escape incompleta");
            }
            char escape = texto.charAt(posicion);
            posicion++;
            switch (escape) {
                case '"':
                    resultado.append('"');
                    break;
                case '\\':
                    resultado.append('\\');
                    break;
                case '/':
                    resultado.append('/');
                    break;
                case 'n':
                    resultado.append('\n');
                    break;
                case 'r':
                    resultado.append('\r');
                    break;
                case 't':
                    resultado.append('\t');
                    break;
                case 'b':
                    resultado.append('\b');
                    break;
                case 'f':
                    resultado.append('\f');
                    break;
                case 'u':
                    if (posicion + 4 > texto.length()) {
                        throw error("Secuencia \\u incompleta");
                    }
                    String hexadecimal = texto.substring(posicion, posicion + 4);
                    resultado.append((char) Integer.parseInt(hexadecimal, 16));
                    posicion += 4;
                    break;
                default:
                    throw error("Secuencia de escape desconocida \\" + escape);
            }
        }
        throw error("Texto sin comilla de cierre");
    }

    /**
     * Lee un número JSON (entero, decimal o con exponente).
     */
    private Double leerNumero() {
        int inicio = posicion;
        while (posicion < texto.length()) {
            char c = texto.charAt(posicion);
            boolean esParteDeNumero = Character.isDigit(c) || c == '-' || c == '+' || c == '.'
                    || c == 'e' || c == 'E';
            if (!esParteDeNumero) {
                break;
            }
            posicion++;
        }
        String numero = texto.substring(inicio, posicion);
        try {
            return Double.valueOf(numero);
        } catch (NumberFormatException e) {
            throw error("Número inválido: " + numero);
        }
    }

    private void consumir(char esperado) {
        saltarEspacios();
        if (posicion >= texto.length() || texto.charAt(posicion) != esperado) {
            throw error("Se esperaba '" + esperado + "'");
        }
        posicion++;
    }

    private void saltarEspacios() {
        while (posicion < texto.length() && Character.isWhitespace(texto.charAt(posicion))) {
            posicion++;
        }
    }

    private IllegalArgumentException error(String mensaje) {
        return new IllegalArgumentException("JSON inválido: " + mensaje + " (posición " + posicion + ")");
    }
}
