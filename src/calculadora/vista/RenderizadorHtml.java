package calculadora.vista;

import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ResultadoCalculo;
import calculadora.vista.estilo.Tema;

import java.awt.Color;
import java.util.List;

/**
 * Convierte el procedimiento del motor y las explicaciones de la IA en
 * HTML sencillo para mostrarlos con formato dentro de un JEditorPane.
 * <p>
 * Swing solo entiende HTML 3.2 y un subconjunto pequeño de CSS, por eso se
 * usan tablas y atributos como bgcolor en lugar de estilos modernos.
 * </p>
 */
public final class RenderizadorHtml {

    /** Constructor privado: clase utilitaria. */
    private RenderizadorHtml() {
    }

    /**
     * Mensaje centrado para cuando todavía no hay contenido.
     *
     * @param titulo  texto principal
     * @param detalle texto secundario
     * @return documento HTML
     */
    public static String estadoVacio(String titulo, String detalle) {
        return encabezadoDocumento()
                + "<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center'>"
                + "<br><br><font size='7' color='" + Tema.hex(Tema.PRIMARIO_SUAVE_FUERTE) + "'>&#8747;</font><br>"
                + "<p style='font-size:15pt; color:" + Tema.hex(Tema.TEXTO) + "'><b>" + escapar(titulo) + "</b></p>"
                + "<p style='font-size:12pt; color:" + Tema.hex(Tema.TEXTO_SECUNDARIO) + "'>" + escapar(detalle) + "</p>"
                + "</td></tr></table></body></html>";
    }

    /**
     * Genera el HTML del procedimiento paso a paso.
     *
     * @param resultado resultado del motor
     * @return documento HTML
     */
    public static String pasos(ResultadoCalculo resultado) {
        StringBuilder html = new StringBuilder();
        html.append(encabezadoDocumento());

        // Pastillas con la operación y el método
        html.append("<table cellpadding='0' cellspacing='0'><tr>");
        html.append(pastilla(resultado.getTipo().getNombre(), Tema.PRIMARIO_SUAVE, Tema.PRIMARIO));
        if (!resultado.getNombreMetodo().isEmpty()) {
            html.append("<td width='8'></td>");
            html.append(pastilla(resultado.getNombreMetodo(), Tema.ACENTO_SUAVE, Tema.ACENTO));
        }
        html.append("</tr></table>");

        String etiqueta = resultado.getTipo() == ResultadoCalculo.TipoOperacion.DERIVADA_IMPLICITA ? "Ecuación: "
                : "f = ";
        String expresion = resultado.getTipo() == ResultadoCalculo.TipoOperacion.DERIVADA_IMPLICITA
                ? resultado.getTextoOriginal() : String.valueOf(resultado.getExpresion());
        html.append("<p style='margin-top:10px; color:").append(Tema.hex(Tema.TEXTO_SECUNDARIO)).append("'>")
                .append(etiqueta).append("<span style='font-family:").append(Tema.familiaMono())
                .append("; font-size:14pt; color:").append(Tema.hex(Tema.TEXTO)).append("'>")
                .append(escapar(expresion)).append("</span></p>");

        // Notas (por ejemplo: "esta función no necesita la regla elegida")
        List<String> notas = resultado.getNotas();
        for (int i = 0; i < notas.size(); i++) {
            html.append("<table width='100%' cellpadding='9' cellspacing='0'><tr><td bgcolor='")
                    .append(Tema.hex(Tema.ADVERTENCIA_SUAVE)).append("'><font color='").append(Tema.hex(Tema.ADVERTENCIA))
                    .append("'><b>Nota: </b></font><font color='").append(Tema.hex(Tema.TEXTO)).append("'>")
                    .append(escapar(notas.get(i))).append("</font></td></tr></table><br>");
        }

        // Pasos
        html.append("<table width='100%' cellpadding='0' cellspacing='0'>");
        List<PasoDetallado> detallados = resultado.getPasosDetallados();
        if (!detallados.isEmpty()) {
            int numero = 1;
            for (int i = 0; i < detallados.size(); i++) {
                html.append(filaDetallada(numero, detallados.get(i)));
                numero++;
            }
        } else {
            List<String> pasos = resultado.getPasos();
            for (int i = 0; i < pasos.size(); i++) {
                html.append(filaSimple(i + 1, pasos.get(i)));
            }
        }
        html.append("</table>");

        // Resultado final
        html.append("<table width='100%' cellpadding='12' cellspacing='0' style='margin-top:10px'>")
                .append("<tr><td bgcolor='").append(Tema.hex(Tema.EXITO_SUAVE)).append("'>")
                .append("<font color='").append(Tema.hex(Tema.EXITO)).append("'><b>RESULTADO</b></font><br>")
                .append("<span style='font-family:").append(Tema.familiaMono()).append("; font-size:15pt; color:")
                .append(Tema.hex(Tema.TEXTO)).append("'>").append(escapar(resultado.getTextoResultado()).replace("\n", "<br>"))
                .append("</span></td></tr></table>");

        html.append("</body></html>");
        return html.toString();
    }

    /** Fila de un paso del motor simbólico (con sangría según la profundidad). */
    private static String filaDetallada(int numero, PasoDetallado paso) {
        int sangria = Math.min(paso.getProfundidad(), 6) * 22;
        boolean destacado = paso.isDestacado();
        Color fondo = destacado ? Tema.PRIMARIO_SUAVE : Tema.SUPERFICIE;
        Color colorNumero = destacado ? Tema.PRIMARIO : Tema.TEXTO_SECUNDARIO;
        Color fondoNumero = destacado ? Tema.PRIMARIO_SUAVE_FUERTE : Tema.SUPERFICIE_ALTERNA;

        StringBuilder fila = new StringBuilder();
        fila.append("<tr><td>");
        fila.append("<table width='100%' cellpadding='0' cellspacing='0'><tr>");
        if (sangria > 0) {
            fila.append("<td width='").append(sangria).append("'></td>");
        }
        fila.append("<td bgcolor='").append(Tema.hex(fondo)).append("'>");
        fila.append("<table width='100%' cellpadding='7' cellspacing='0'><tr>");
        fila.append("<td width='30' valign='top'><table cellpadding='3' cellspacing='0'><tr><td align='center' bgcolor='")
                .append(Tema.hex(fondoNumero)).append("'><font color='").append(Tema.hex(colorNumero)).append("'><b>&nbsp;")
                .append(numero).append("&nbsp;</b></font></td></tr></table></td>");
        fila.append("<td valign='top'>");
        fila.append("<b style='color:").append(Tema.hex(destacado ? Tema.PRIMARIO : Tema.TEXTO)).append("'>")
                .append(escapar(paso.getTitulo())).append("</b>");
        if (destacado) {
            fila.append("&nbsp;&nbsp;<font color='").append(Tema.hex(Tema.SECUNDARIO)).append("'>(método elegido)</font>");
        }
        if (!paso.getExpresion().isEmpty()) {
            fila.append("<br><span style='font-family:").append(Tema.familiaMono()).append("; color:")
                    .append(Tema.hex(Tema.TEXTO_SECUNDARIO)).append("'>").append(escapar(paso.getExpresion()))
                    .append("</span>");
        }
        if (!paso.getDetalle().isEmpty()) {
            fila.append("<br><span style='color:").append(Tema.hex(Tema.TEXTO_SECUNDARIO)).append("'>")
                    .append(escapar(paso.getDetalle())).append("</span>");
        }
        if (!paso.getResultado().isEmpty()) {
            fila.append("<br><span style='font-family:").append(Tema.familiaMono()).append("; color:")
                    .append(Tema.hex(Tema.TEXTO_TENUE)).append("'>→ </span>")
                    .append("<span style='font-family:").append(Tema.familiaMono()).append("; color:")
                    .append(Tema.hex(Tema.PRIMARIO)).append("; font-size:13pt'><b>").append(escapar(paso.getResultado()))
                    .append("</b></span>");
        }
        fila.append("</td></tr></table>");
        fila.append("</td></tr></table>");
        fila.append("</td></tr><tr><td height='5'></td></tr>");
        return fila.toString();
    }

    /** Fila de un paso en texto simple (motor por términos). */
    private static String filaSimple(int numero, String paso) {
        return "<tr><td><table width='100%' cellpadding='7' cellspacing='0'><tr><td bgcolor='" + Tema.hex(Tema.SUPERFICIE)
                + "'><font color='" + Tema.hex(Tema.PRIMARIO) + "'><b>" + numero + ".</b></font> <span style='color:"
                + Tema.hex(Tema.TEXTO_SECUNDARIO) + "'>" + escapar(paso) + "</span></td></tr></table></td></tr>";
    }

    /**
     * Da formato a la explicación en texto plano de la IA: párrafos y
     * "Paso n:" en negrita.
     *
     * @param texto  explicación en texto plano
     * @param fuente quién generó la explicación
     * @return documento HTML
     */
    public static String explicacion(String texto, String fuente) {
        StringBuilder html = new StringBuilder();
        html.append(encabezadoDocumento());
        html.append("<table cellpadding='0' cellspacing='0'><tr>")
                .append(pastilla("Explicado por: " + fuente, Tema.ACENTO_SUAVE, Tema.ACENTO))
                .append("</tr></table><br>");

        String[] lineas = texto.split("\n");
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.isEmpty()) {
                continue;
            }
            String contenido = escapar(linea);
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos > 0 && dosPuntos <= 22) {
                String prefijo = escapar(linea.substring(0, dosPuntos + 1));
                String resto = escapar(linea.substring(dosPuntos + 1));
                contenido = "<b style='color:" + Tema.hex(Tema.PRIMARIO) + "'>" + prefijo + "</b>" + resto;
            }
            html.append("<p style='margin-top:0; margin-bottom:9px'>").append(contenido).append("</p>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * @param mensaje texto mientras se espera la respuesta
     * @return documento HTML de "cargando"
     */
    public static String cargando(String mensaje) {
        return estadoVacio(mensaje, "Esto puede tardar unos segundos si se usa un proveedor en línea.");
    }

    // ------------------------------------------------------------------

    private static String encabezadoDocumento() {
        return "<html><body style='font-family:" + Tema.familiaTexto() + "; font-size:13pt; color:"
                + Tema.hex(Tema.TEXTO) + "; margin:4px 6px 4px 4px'>";
    }

    private static String pastilla(String texto, Color fondo, Color color) {
        return "<td bgcolor='" + Tema.hex(fondo) + "' style='padding:3px 10px 3px 10px'><font color='"
                + Tema.hex(color) + "'><b>" + escapar(texto) + "</b></font></td>";
    }

    /**
     * Escapa los caracteres especiales de HTML.
     *
     * @param texto texto original
     * @return texto seguro para insertar en HTML
     */
    public static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
