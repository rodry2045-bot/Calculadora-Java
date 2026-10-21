package calculadora.ia;

import calculadora.excepciones.IAException;
import calculadora.util.JsonSencillo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Proveedor de IA que usa la API de Mensajes de Anthropic (modelos Claude).
 * <p>
 * Petición (POST {urlBase}/messages):
 * </p>
 * <pre>
 * Encabezados: x-api-key, anthropic-version: 2023-06-01, content-type: application/json
 * Cuerpo: {"model": "...", "max_tokens": 700, "system": "...",
 *          "messages": [{"role": "user", "content": "..."}]}
 * </pre>
 * Respuesta: el texto está en {@code content[0].text}.
 */
public class ProveedorAnthropic implements ProveedorIA {

    /** Versión de la API de Anthropic que se utiliza. */
    private static final String VERSION_API = "2023-06-01";

    /** Máximo de tokens de la respuesta (≈ 250 palabras con margen). */
    private static final int MAXIMO_TOKENS = 700;

    private final ConfiguracionIA configuracion;
    private final HttpClient clienteHttp;

    /**
     * @param configuracion configuración con la clave, el modelo y el tiempo de espera
     */
    public ProveedorAnthropic(ConfiguracionIA configuracion) {
        this.configuracion = configuracion;

        // Construcción paso a paso del cliente HTTP
        HttpClient.Builder constructor = HttpClient.newBuilder();
        constructor.connectTimeout(Duration.ofSeconds(10));
        constructor.version(HttpClient.Version.HTTP_1_1);
        this.clienteHttp = constructor.build();
    }

    @Override
    public String getNombre() {
        return "Anthropic (" + configuracion.getModeloEfectivo() + ")";
    }

    @Override
    public String generarRespuesta(String instruccionSistema, String mensajeUsuario) throws IAException {
        if (!configuracion.hayClave()) {
            throw new IAException("No hay clave de API configurada para Anthropic");
        }

        // 1) Construir el cuerpo JSON de forma explícita
        String mensaje = JsonSencillo.objeto(
                "role", JsonSencillo.cadena("user"),
                "content", JsonSencillo.cadena(mensajeUsuario));
        String cuerpo = JsonSencillo.objeto(
                "model", JsonSencillo.cadena(configuracion.getModeloEfectivo()),
                "max_tokens", String.valueOf(MAXIMO_TOKENS),
                "system", JsonSencillo.cadena(instruccionSistema),
                "messages", JsonSencillo.arreglo(mensaje));

        // 2) Construir la petición HTTP
        String url = configuracion.getUrlBaseEfectiva() + "/messages";
        HttpRequest.Builder constructor = HttpRequest.newBuilder();
        constructor.uri(URI.create(url));
        constructor.timeout(Duration.ofSeconds(configuracion.getTiempoEsperaSegundos()));
        constructor.header("content-type", "application/json");
        constructor.header("x-api-key", configuracion.getApiKey());
        constructor.header("anthropic-version", VERSION_API);
        constructor.POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        HttpRequest peticion = constructor.build();

        // 3) Enviar y recibir
        HttpResponse<String> respuesta;
        try {
            respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IAException("No se pudo conectar con Anthropic (¿hay conexión a Internet?): "
                    + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IAException("La consulta a Anthropic fue interrumpida", e);
        }

        // 4) Interpretar la respuesta
        return extraerTexto(respuesta.statusCode(), respuesta.body());
    }

    /**
     * Interpreta el JSON de la respuesta. Es público y estático para poder
     * probarlo sin conexión a Internet.
     *
     * @param codigoHttp código de estado HTTP
     * @param cuerpo     cuerpo de la respuesta (JSON)
     * @return texto generado por el modelo
     * @throws IAException si hubo error o el formato no es el esperado
     */
    @SuppressWarnings("unchecked")
    public static String extraerTexto(int codigoHttp, String cuerpo) throws IAException {
        Object raiz;
        try {
            raiz = JsonSencillo.parsear(cuerpo);
        } catch (IllegalArgumentException e) {
            throw new IAException("Respuesta de Anthropic no válida (código " + codigoHttp + ")", e);
        }

        if (codigoHttp != 200) {
            Object mensajeError = JsonSencillo.obtenerRuta(raiz, "error", "message");
            throw new IAException(describirCodigo(codigoHttp) + (mensajeError != null ? ": " + mensajeError : ""));
        }

        // El contenido es una lista de bloques; se concatenan los de tipo "text"
        Object contenido = JsonSencillo.obtenerRuta(raiz, "content");
        if (!(contenido instanceof List)) {
            throw new IAException("La respuesta de Anthropic no contiene el campo 'content'");
        }
        List<Object> bloques = (List<Object>) contenido;
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < bloques.size(); i++) {
            Object bloque = bloques.get(i);
            if (bloque instanceof Map) {
                Object tipo = ((Map<String, Object>) bloque).get("type");
                Object valor = ((Map<String, Object>) bloque).get("text");
                if ("text".equals(tipo) && valor != null) {
                    texto.append(valor);
                }
            }
        }
        if (texto.length() == 0) {
            throw new IAException("Anthropic devolvió una respuesta vacía");
        }
        return texto.toString();
    }

    private static String describirCodigo(int codigo) {
        if (codigo == 401) {
            return "Clave de API de Anthropic inválida (401)";
        }
        if (codigo == 403) {
            return "Acceso denegado por Anthropic (403)";
        }
        if (codigo == 404) {
            return "Modelo o URL no encontrados en Anthropic (404)";
        }
        if (codigo == 429) {
            return "Se superó el límite de consultas de Anthropic (429)";
        }
        if (codigo >= 500) {
            return "Error temporal del servidor de Anthropic (" + codigo + ")";
        }
        return "Anthropic respondió con el código " + codigo;
    }
}
