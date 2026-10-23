package calculadora.ia;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Configuración del Asistente de IA.
 * <p>
 * <b>Forma recomendada:</b> copiar {@code config.properties.ejemplo} con el
 * nombre {@code config.properties} y completar los valores. Ese archivo está
 * en .gitignore, así que la clave de API nunca se sube al repositorio.
 * </p>
 * <p>
 * Orden de prioridad:
 * </p>
 * <ol>
 *   <li>Archivo {@code config.properties}, si existe (más la variable de
 *       entorno {@code CALCULADORA_IA_API_KEY}, que tiene prioridad sobre la
 *       clave del archivo).</li>
 *   <li>Constantes de esta clase. Úselas solo para proveedores SIN clave
 *       (Ollama o "local"): una clave escrita en el código termina subida a
 *       GitHub y cualquiera podría usarla.</li>
 *   <li>Solo la variable de entorno {@code CALCULADORA_IA_API_KEY}.</li>
 * </ol>
 * <p>
 * Si no hay clave o no hay Internet, se usa el Explicador Local.
 * </p>
 */
public class ConfiguracionIA {

    // ==================================================================
    // CONFIGURACIÓN EN EL CÓDIGO (solo para proveedores sin clave).
    // Para Claude u OpenAI use config.properties: NO escriba la clave aquí.
    // ==================================================================

    /** Proveedor: "anthropic" (Claude), "openai", "ollama" (local, gratis) o "local" (sin IA). */
    public static final String PROVEEDOR = "ollama";

    /** Clave de API. Déjela VACÍA: la clave va en config.properties o en CALCULADORA_IA_API_KEY. */
    public static final String CLAVE_API = "";

    /** Modelo. Vacío = valor por defecto (claude-haiku-5-5, gpt-4o-mini o llama3.2). */
    public static final String MODELO = "llama3:latest";

    /** URL base. Vacía = valor por defecto del proveedor. */
    public static final String URL_BASE = "";

    /** Tiempo máximo de espera de la respuesta, en segundos. */
    public static final int TIEMPO_ESPERA_SEGUNDOS = 120;

    // ==================================================================

    /** Nombre del archivo de configuración. */
    public static final String NOMBRE_ARCHIVO = "config.properties";

    /** Nombre de la variable de entorno con la clave de API. */
    public static final String VARIABLE_ENTORNO_CLAVE = "CALCULADORA_IA_API_KEY";

    /** Proveedores reconocidos. */
    public static final String PROVEEDOR_ANTHROPIC = "anthropic";
    /** Proveedor OpenAI. */
    public static final String PROVEEDOR_OPENAI = "openai";
    /** Proveedor Ollama (modelo local, compatible con OpenAI). */
    public static final String PROVEEDOR_OLLAMA = "ollama";
    /** Sin IA remota: solo explicador local. */
    public static final String PROVEEDOR_LOCAL = "local";

    private String proveedor;
    private String apiKey;
    private String modelo;
    private String urlBase;
    private int tiempoEsperaSegundos;
    private String origen;

    /**
     * Crea una configuración con valores explícitos (útil para pruebas).
     *
     * @param proveedor            anthropic, openai, ollama o local
     * @param apiKey               clave de API (puede ser vacía)
     * @param modelo               modelo (vacío = valor por defecto)
     * @param urlBase              URL base (vacía = valor por defecto)
     * @param tiempoEsperaSegundos tiempo máximo de espera
     */
    public ConfiguracionIA(String proveedor, String apiKey, String modelo, String urlBase,
                           int tiempoEsperaSegundos) {
        this.proveedor = limpiar(proveedor).toLowerCase();
        if (this.proveedor.isEmpty()) {
            this.proveedor = PROVEEDOR_LOCAL;
        }
        this.apiKey = limpiar(apiKey);
        this.modelo = limpiar(modelo);
        this.urlBase = limpiar(urlBase);
        this.tiempoEsperaSegundos = tiempoEsperaSegundos;
        this.origen = "valores explícitos";
    }

    /**
     * Carga la configuración desde config.properties y la variable de entorno.
     * Si el archivo no existe se usa el proveedor "local".
     *
     * @return configuración cargada (nunca null)
     */
    public static ConfiguracionIA cargar() {
        // 1) Si existe config.properties, manda el archivo; si no, la configuración del código
        boolean codigoCompleto = !CLAVE_API.trim().isEmpty() || PROVEEDOR_OLLAMA.equalsIgnoreCase(PROVEEDOR.trim())
                || PROVEEDOR_LOCAL.equalsIgnoreCase(PROVEEDOR.trim());
        String claveEntornoPrevia = System.getenv(VARIABLE_ENTORNO_CLAVE);
        boolean hayEntorno = claveEntornoPrevia != null && !claveEntornoPrevia.trim().isEmpty();
        boolean hayArchivo = new File(NOMBRE_ARCHIVO).isFile();
        if (codigoCompleto && !hayArchivo && !(PROVEEDOR_LOCAL.equalsIgnoreCase(PROVEEDOR.trim()) && hayEntorno)) {
            ConfiguracionIA desdeCodigo = new ConfiguracionIA(PROVEEDOR, CLAVE_API, MODELO, URL_BASE,
                    TIEMPO_ESPERA_SEGUNDOS);
            desdeCodigo.origen = "código (calculadora.ia.ConfiguracionIA)";
            return desdeCodigo;
        }

        // 2) y 3) Variable de entorno o archivo opcional
        Properties propiedades = new Properties();
        String origenLectura = "valores por defecto (no se encontró " + NOMBRE_ARCHIVO + ")";

        File archivo = new File(NOMBRE_ARCHIVO);
        if (archivo.isFile()) {
            InputStream entrada = null;
            try {
                entrada = new FileInputStream(archivo);
                Reader lector = new InputStreamReader(entrada, StandardCharsets.UTF_8);
                propiedades.load(lector);
                origenLectura = "archivo " + archivo.getAbsolutePath();
            } catch (IOException e) {
                origenLectura = "valores por defecto (error al leer " + NOMBRE_ARCHIVO + ": " + e.getMessage() + ")";
            } finally {
                if (entrada != null) {
                    try {
                        entrada.close();
                    } catch (IOException e) {
                        // Se ignora: el archivo ya fue leído o no se pudo abrir
                    }
                }
            }
        }

        String proveedor = propiedades.getProperty("proveedor", PROVEEDOR);
        String clave = propiedades.getProperty("api_key", "");
        String modelo = propiedades.getProperty("modelo", MODELO);
        String url = propiedades.getProperty("url_base", URL_BASE);
        int tiempo = convertirEntero(propiedades.getProperty("tiempo_espera_segundos",
                String.valueOf(TIEMPO_ESPERA_SEGUNDOS)), TIEMPO_ESPERA_SEGUNDOS);

        // La variable de entorno tiene prioridad sobre el archivo
        String claveEntorno = System.getenv(VARIABLE_ENTORNO_CLAVE);
        if (claveEntorno != null && !claveEntorno.trim().isEmpty()) {
            clave = claveEntorno;
            origenLectura = origenLectura + " + variable de entorno " + VARIABLE_ENTORNO_CLAVE;
            // Si hay clave pero el proveedor quedó en "local", se asume Anthropic
            if (limpiar(proveedor).isEmpty() || limpiar(proveedor).equalsIgnoreCase(PROVEEDOR_LOCAL)) {
                proveedor = PROVEEDOR_ANTHROPIC;
            }
        }

        ConfiguracionIA configuracion = new ConfiguracionIA(proveedor, clave, modelo, url, tiempo);
        configuracion.origen = origenLectura;
        return configuracion;
    }

    /**
     * @return true si la variable de entorno CALCULADORA_IA_API_KEY está definida
     *         (en ese caso tiene prioridad sobre la clave del archivo)
     */
    public static boolean hayClaveEnEntorno() {
        String valor = System.getenv(VARIABLE_ENTORNO_CLAVE);
        return valor != null && !valor.trim().isEmpty();
    }

    /** @return modelo escrito por el usuario (puede ser vacío) */
    public String getModelo() {
        return modelo;
    }

    /** @return URL base escrita por el usuario (puede ser vacía) */
    public String getUrlBase() {
        return urlBase;
    }

    /** @return true si hay una clave de API configurada */
    public boolean hayClave() {
        return !apiKey.isEmpty();
    }

    /**
     * @return modelo configurado o el valor por defecto del proveedor
     */
    public String getModeloEfectivo() {
        if (!modelo.isEmpty()) {
            return modelo;
        }
        if (proveedor.equals(PROVEEDOR_ANTHROPIC)) {
            return "claude-haiku-5-5";
        }
        if (proveedor.equals(PROVEEDOR_OPENAI)) {
            return "gpt-4o-mini";
        }
        if (proveedor.equals(PROVEEDOR_OLLAMA)) {
            return "llama3.2";
        }
        return "";
    }

    /**
     * @return URL base configurada o la URL por defecto del proveedor
     */
    public String getUrlBaseEfectiva() {
        if (!urlBase.isEmpty()) {
            return urlBase;
        }
        if (proveedor.equals(PROVEEDOR_ANTHROPIC)) {
            return "https://api.anthropic.com/v1";
        }
        if (proveedor.equals(PROVEEDOR_OPENAI)) {
            return "https://api.openai.com/v1";
        }
        if (proveedor.equals(PROVEEDOR_OLLAMA)) {
            return "http://localhost:11434/v1";
        }
        return "";
    }

    /** @return proveedor configurado en minúsculas */
    public String getProveedor() {
        return proveedor;
    }

    /** @return clave de API (no se debe mostrar ni registrar en pantalla) */
    public String getApiKey() {
        return apiKey;
    }

    /** @return tiempo máximo de espera en segundos */
    public int getTiempoEsperaSegundos() {
        return tiempoEsperaSegundos;
    }

    /** @return de dónde se leyó la configuración */
    public String getOrigen() {
        return origen;
    }

    /**
     * Descripción segura (sin mostrar la clave) para la interfaz.
     *
     * @return texto descriptivo
     */
    public String describir() {
        String estadoClave;
        if (hayClave()) {
            estadoClave = "configurada";
        } else {
            estadoClave = "no configurada";
        }
        return "Proveedor: " + proveedor + " | Modelo: " + getModeloEfectivo() + " | Clave: " + estadoClave;
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }

    private static int convertirEntero(String texto, int valorPorDefecto) {
        try {
            int valor = Integer.parseInt(texto.trim());
            if (valor <= 0) {
                return valorPorDefecto;
            }
            return valor;
        } catch (NumberFormatException e) {
            return valorPorDefecto;
        }
    }
}
