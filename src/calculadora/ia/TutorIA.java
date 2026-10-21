package calculadora.ia;

import calculadora.excepciones.IAException;
import calculadora.modelo.ResultadoCalculo;

/**
 * Tutor que explica el procedimiento de un cálculo.
 * <p>
 * Reglas de diseño:
 * </p>
 * <ul>
 *   <li>La IA NUNCA calcula: recibe los pasos ya calculados por el motor Java
 *       y solo los explica con palabras sencillas.</li>
 *   <li>Respuestas en texto plano (sin Markdown ni LaTeX) y de máximo 250 palabras.</li>
 *   <li>Si no hay clave, no hay red o el proveedor falla, conmuta automáticamente
 *       al {@link ExplicadorLocal}.</li>
 * </ul>
 */
public class TutorIA {

    /** Máximo de palabras permitido en una explicación. */
    public static final int MAXIMO_PALABRAS = 250;

    private final ConfiguracionIA configuracion;
    private final ProveedorIA proveedorRemoto;
    private final ExplicadorLocal explicadorLocal;

    private String ultimaFuente;

    /**
     * @param configuracion configuración leída de config.properties
     */
    public TutorIA(ConfiguracionIA configuracion) {
        this.configuracion = configuracion;
        this.proveedorRemoto = crearProveedor(configuracion);
        this.explicadorLocal = new ExplicadorLocal();
        this.ultimaFuente = "";
    }

    /**
     * Fábrica sencilla: elige el proveedor según la configuración.
     *
     * @param configuracion configuración
     * @return el proveedor remoto o null si se debe usar solo el explicador local
     */
    private static ProveedorIA crearProveedor(ConfiguracionIA configuracion) {
        String proveedor = configuracion.getProveedor();
        if (ConfiguracionIA.PROVEEDOR_ANTHROPIC.equals(proveedor) && configuracion.hayClave()) {
            return new ProveedorAnthropic(configuracion);
        }
        if (ConfiguracionIA.PROVEEDOR_OPENAI.equals(proveedor) && configuracion.hayClave()) {
            return new ProveedorCompatibleOpenAI(configuracion);
        }
        if (ConfiguracionIA.PROVEEDOR_OLLAMA.equals(proveedor)) {
            return new ProveedorCompatibleOpenAI(configuracion);
        }
        return null;
    }

    /**
     * @return true si hay un proveedor remoto configurado
     */
    public boolean tieneProveedorRemoto() {
        return proveedorRemoto != null;
    }

    /**
     * @return descripción del proveedor que se intentará usar
     */
    public String describirProveedor() {
        if (proveedorRemoto == null) {
            return "Explicador Local (sin conexión). Para usar IA en línea, configure config.properties.";
        }
        return proveedorRemoto.getNombre() + " con respaldo en el Explicador Local";
    }

    /**
     * @return nombre de la fuente que generó la última explicación
     */
    public String getUltimaFuente() {
        return ultimaFuente;
    }

    /**
     * Explica un resultado. ESTE MÉTODO PUEDE TARDAR (llamada de red): en la
     * interfaz gráfica debe ejecutarse dentro de un SwingWorker.
     *
     * @param resultado resultado calculado por el motor Java
     * @param pregunta  pregunta opcional del estudiante
     * @return explicación en texto plano de máximo 250 palabras
     */
    public String explicar(ResultadoCalculo resultado, String pregunta) {
        if (proveedorRemoto == null) {
            ultimaFuente = "Explicador Local";
            return explicadorLocal.explicar(resultado, pregunta);
        }
        try {
            String respuesta = proveedorRemoto.generarRespuesta(construirInstruccionSistema(),
                    construirMensaje(resultado, pregunta));
            ultimaFuente = proveedorRemoto.getNombre();
            return limitarPalabras(limpiarFormato(respuesta), MAXIMO_PALABRAS);
        } catch (IAException e) {
            ultimaFuente = "Explicador Local (respaldo: " + e.getMessage() + ")";
            return explicadorLocal.explicar(resultado, pregunta);
        }
    }

    /**
     * Instrucciones de sistema: definen el rol y las reglas para el modelo.
     *
     * @return texto de la instrucción de sistema
     */
    public String construirInstruccionSistema() {
        return "Eres un tutor de Cálculo para estudiantes universitarios de Ingeniería en Sistemas. "
                + "Responde siempre en español. "
                + "REGLAS: "
                + "1) NO calcules ni cambies ningún resultado: el motor Java ya hizo los cálculos y son correctos. "
                + "Tu tarea es SOLO explicar, paso a paso y con palabras sencillas, el procedimiento que se te entrega. "
                + "2) Escribe en texto plano: sin Markdown (sin asteriscos, almohadillas ni viñetas con guiones) "
                + "y sin LaTeX (sin signos de dólar ni barras invertidas). Usa notación como x^2, sin(2x), e^(3x). "
                + "3) Máximo 250 palabras. "
                + "4) Menciona el nombre de cada regla aplicada y por qué se usa. "
                + "5) Si hay una pregunta del estudiante, respóndela basándote en el procedimiento.";
    }

    /**
     * Mensaje con el procedimiento registrado por el motor.
     *
     * @param resultado resultado del motor
     * @param pregunta  pregunta opcional
     * @return mensaje para el modelo
     */
    public String construirMensaje(ResultadoCalculo resultado, String pregunta) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Explica el siguiente procedimiento calculado por el motor Java:\n\n");
        mensaje.append(resultado.generarInformePasos());
        if (pregunta != null && !pregunta.trim().isEmpty()) {
            mensaje.append("\nPregunta del estudiante: ").append(pregunta.trim()).append('\n');
        }
        return mensaje.toString();
    }

    /**
     * Elimina marcas de Markdown y LaTeX que el modelo pudiera incluir.
     *
     * @param texto respuesta original
     * @return texto plano
     */
    public static String limpiarFormato(String texto) {
        String limpio = texto;
        limpio = limpio.replace("**", "");
        limpio = limpio.replace("__", "");
        limpio = limpio.replace("`", "");
        limpio = limpio.replace("$", "");
        limpio = limpio.replace("\\(", "");
        limpio = limpio.replace("\\)", "");
        limpio = limpio.replace("\\[", "");
        limpio = limpio.replace("\\]", "");
        limpio = limpio.replace("\\cdot", "·");
        limpio = limpio.replace("\\int", "∫");

        // Quitar almohadillas de títulos y asteriscos de viñetas al inicio de cada línea
        StringBuilder resultado = new StringBuilder();
        String[] lineas = limpio.split("\n", -1);
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i];
            String sinEspacios = linea.trim();
            while (sinEspacios.startsWith("#")) {
                sinEspacios = sinEspacios.substring(1).trim();
                linea = sinEspacios;
            }
            if (sinEspacios.startsWith("* ")) {
                linea = sinEspacios.substring(2);
            }
            resultado.append(linea);
            if (i < lineas.length - 1) {
                resultado.append('\n');
            }
        }
        return resultado.toString().trim();
    }

    /**
     * Recorta un texto a un número máximo de palabras conservando los saltos de línea.
     *
     * @param texto   texto original
     * @param maximo  número máximo de palabras
     * @return texto recortado (con "..." si se cortó)
     */
    public static String limitarPalabras(String texto, int maximo) {
        int contador = 0;
        boolean dentroDePalabra = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (Character.isWhitespace(c)) {
                dentroDePalabra = false;
            } else if (!dentroDePalabra) {
                dentroDePalabra = true;
                contador++;
                if (contador > maximo) {
                    return texto.substring(0, i).trim() + " ...";
                }
            }
        }
        return texto;
    }

    /**
     * Cuenta las palabras de un texto.
     *
     * @param texto texto a analizar
     * @return cantidad de palabras
     */
    public static int contarPalabras(String texto) {
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            return 0;
        }
        return limpio.split("\\s+").length;
    }
}
