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
 * Orden de lectura:
 * </p>
 * <ol>
 *   <li>Archivo {@code config.properties} en la carpeta desde donde se ejecuta el programa.</li>
 *   <li>Variable de entorno {@code CALCULADORA_IA_API_KEY}: si existe, reemplaza la clave del archivo.</li>
 * </ol>
 * <p>
 * Por seguridad, la clave NUNCA está escrita en el código fuente y el
 * archivo config.properties está excluido del repositorio (.gitignore).
 * </p>
 */
public class ConfiguracionIA {

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

        String proveedor = propiedades.getProperty("proveedor", PROVEEDOR_LOCAL);
        String clave = propiedades.getProperty("api_key", "");
        String modelo = propiedades.getProperty("modelo", "");
        String url = propiedades.getProperty("url_base", "");
        int tiempo = convertirEntero(propiedades.getProperty("tiempo_espera_segundos", "30"), 30);

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
