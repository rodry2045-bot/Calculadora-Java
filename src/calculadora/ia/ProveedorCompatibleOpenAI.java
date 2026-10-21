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

/**
 * Proveedor para cualquier API compatible con "Chat Completions" de OpenAI.
 * <p>
 * Sirve tanto para OpenAI como para Ollama (modelos que se ejecutan en la
 * propia computadora, en http://localhost:11434/v1, sin clave).
 * </p>
 * <pre>
 * POST {urlBase}/chat/completions
 * Encabezado: Authorization: Bearer CLAVE (solo si hay clave)
 * Cuerpo: {"model": "...", "max_tokens": 700,
 *          "messages": [{"role":"system","content":"..."},{"role":"user","content":"..."}]}
 * </pre>
 * Respuesta: el texto está en {@code choices[0].message.content}.
 */
public class ProveedorCompatibleOpenAI implements ProveedorIA {

    private static final int MAXIMO_TOKENS = 700;

    private final ConfiguracionIA configuracion;
    private final HttpClient clienteHttp;

    /**
     * @param configuracion configuración (proveedor openai u ollama)
     */
    public ProveedorCompatibleOpenAI(ConfiguracionIA configuracion) {
        this.configuracion = configuracion;

        HttpClient.Builder constructor = HttpClient.newBuilder();
        constructor.connectTimeout(Duration.ofSeconds(10));
        constructor.version(HttpClient.Version.HTTP_1_1);
        this.clienteHttp = constructor.build();
    }

    @Override
    public String getNombre() {
        String nombreProveedor = "OpenAI";
        if (ConfiguracionIA.PROVEEDOR_OLLAMA.equals(configuracion.getProveedor())) {
            nombreProveedor = "Ollama local";
        }
        return nombreProveedor + " (" + configuracion.getModeloEfectivo() + ")";
    }

    @Override
    public String generarRespuesta(String instruccionSistema, String mensajeUsuario) throws IAException {
        boolean esOpenAI = ConfiguracionIA.PROVEEDOR_OPENAI.equals(configuracion.getProveedor());
        if (esOpenAI && !configuracion.hayClave()) {
            throw new IAException("No hay clave de API configurada para OpenAI");
        }

        // 1) Cuerpo JSON
        String mensajeSistema = JsonSencillo.objeto(
                "role", JsonSencillo.cadena("system"),
                "content", JsonSencillo.cadena(instruccionSistema));
        String mensajeDelUsuario = JsonSencillo.objeto(
                "role", JsonSencillo.cadena("user"),
                "content", JsonSencillo.cadena(mensajeUsuario));
        String cuerpo = JsonSencillo.objeto(
                "model", JsonSencillo.cadena(configuracion.getModeloEfectivo()),
                "max_tokens", String.valueOf(MAXIMO_TOKENS),
                "messages", JsonSencillo.arreglo(mensajeSistema, mensajeDelUsuario));

        // 2) Petición HTTP
        String url = configuracion.getUrlBaseEfectiva() + "/chat/completions";
        HttpRequest.Builder constructor = HttpRequest.newBuilder();
        constructor.uri(URI.create(url));
        constructor.timeout(Duration.ofSeconds(configuracion.getTiempoEsperaSegundos()));
        constructor.header("Content-Type", "application/json");
        if (configuracion.hayClave()) {
            constructor.header("Authorization", "Bearer " + configuracion.getApiKey());
        }
        constructor.POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        HttpRequest peticion = constructor.build();

        // 3) Envío
        HttpResponse<String> respuesta;
        try {
            respuesta = clienteHttp.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IAException("No se pudo conectar con " + getNombre() + " en " + url + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IAException("La consulta fue interrumpida", e);
        }

        // 4) Interpretación
        return extraerTexto(respuesta.statusCode(), respuesta.body());
    }

    /**
     * Interpreta el JSON de respuesta (público y estático para probarlo sin red).
     *
     * @param codigoHttp código de estado HTTP
     * @param cuerpo     JSON recibido
     * @return texto generado
     * @throws IAException si hubo error o falta el contenido
     */
    public static String extraerTexto(int codigoHttp, String cuerpo) throws IAException {
        Object raiz;
        try {
            raiz = JsonSencillo.parsear(cuerpo);
        } catch (IllegalArgumentException e) {
            throw new IAException("Respuesta no válida del servidor (código " + codigoHttp + ")", e);
        }
        if (codigoHttp != 200) {
            Object mensajeError = JsonSencillo.obtenerRuta(raiz, "error", "message");
            String detalle = "";
            if (mensajeError != null) {
                detalle = ": " + mensajeError;
            }
            if (codigoHttp == 401) {
                throw new IAException("Clave de API inválida (401)" + detalle);
            }
            if (codigoHttp == 429) {
                throw new IAException("Se superó el límite de consultas (429)" + detalle);
            }
            throw new IAException("El servidor respondió con el código " + codigoHttp + detalle);
        }
        Object contenido = JsonSencillo.obtenerRuta(raiz, "choices", 0, "message", "content");
        if (contenido == null || contenido.toString().trim().isEmpty()) {
            throw new IAException("La respuesta no contiene texto en choices[0].message.content");
        }
        return contenido.toString();
    }
}
