package calculadora.vista;

import calculadora.modelo.ResultadoCalculo;
import calculadora.vista.estilo.Tema;

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
                + "<br><br><font size='6' color='" + Tema.hex(Tema.BORDE_FOCO) + "'>&#8747;</font><br>"
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

        // Datos generales
        html.append("<table cellpadding='0' cellspacing='0'><tr>");
        html.append(pastilla(resultado.getTipo().getNombre(), Tema.PRIMARIO_SUAVE, Tema.PRIMARIO));
        if (resultado.getVariable() != null && resultado.getTipo() != ResultadoCalculo.TipoOperacion.EVALUACION) {
            html.append("<td width='8'></td>");
            html.append(pastilla("respecto a " + resultado.getVariable().getSimbolo(), Tema.ACENTO_SUAVE, Tema.ACENTO));
        }
        html.append("</tr></table>");
        html.append("<p style='margin-top:10px; color:").append(Tema.hex(Tema.TEXTO_SECUNDARIO)).append("'>")
                .append("f = <span style='font-family:").append(Tema.familiaMono()).append("; font-size:13pt; color:")
                .append(Tema.hex(Tema.TEXTO)).append("'>").append(escapar(resultado.getExpresion().toString()))
                .append("</span></p>");

        // Pasos
        html.append("<table width='100%' cellpadding='0' cellspacing='0'>");
        List<String> pasos = resultado.getPasos();
        for (int i = 0; i < pasos.size(); i++) {
            html.append(filaPaso(i + 1, pasos.get(i)));
        }
        html.append("</table>");

        // Resultado final
        html.append("<table width='100%' cellpadding='12' cellspacing='0' style='margin-top:8px'>")
                .append("<tr><td bgcolor='").append(Tema.hex(new java.awt.Color(236, 253, 245))).append("'>")
                .append("<font color='").append(Tema.hex(Tema.EXITO)).append("'><b>RESULTADO</b></font><br>")
                .append("<span style='font-family:").append(Tema.familiaMono()).append("; font-size:14pt; color:")
                .append(Tema.hex(Tema.TEXTO)).append("'>").append(escapar(resultado.getTextoResultado()))
                .append("</span></td></tr></table>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Genera una fila con el número del paso, el nombre de la regla, la
     * explicación y el resultado parcial resaltado.
     */
    private static String filaPaso(int numero, String paso) {
        String termino = "";
        String cuerpo = paso;

        // "Término 2 [3x^2]: Regla ..." -> separar el término
        int finEncabezado = paso.indexOf("]: ");
        if (paso.startsWith("Término") && finEncabezado > 0) {
            int inicioCorchete = paso.indexOf('[');
            termino = paso.substring(inicioCorchete + 1, finEncabezado);
            cuerpo = paso.substring(finEncabezado + 3);
        }

        // Nombre de la regla: texto antes del primer ": "
        String regla = "";
        String explicacion = cuerpo;
        int dosPuntos = cuerpo.indexOf(": ");
        if (dosPuntos > 0 && dosPuntos < 120) {
            regla = cuerpo.substring(0, dosPuntos);
            explicacion = cuerpo.substring(dosPuntos + 2);
        }

        // Resultado parcial
        String parcial = "";
        String nota = "";
        int indiceResultado = explicacion.indexOf("Resultado: ");
        if (indiceResultado >= 0) {
            String resto = explicacion.substring(indiceResultado + "Resultado: ".length());
            explicacion = explicacion.substring(0, indiceResultado).trim();
            int punto = resto.indexOf(". ");
            if (punto > 0) {
                parcial = resto.substring(0, punto);
                nota = resto.substring(punto + 2).trim();
            } else {
                parcial = resto;
            }
            if (parcial.endsWith(".")) {
                parcial = parcial.substring(0, parcial.length() - 1);
            }
        }

        StringBuilder fila = new StringBuilder();
        fila.append("<tr>");
        // Número
        fila.append("<td width='34' valign='top' style='padding-top:12px'>")
                .append("<table cellpadding='0' cellspacing='0'><tr><td width='26' height='26' align='center' bgcolor='")
                .append(Tema.hex(Tema.PRIMARIO_SUAVE)).append("'><font color='").append(Tema.hex(Tema.PRIMARIO))
                .append("'><b>").append(numero).append("</b></font></td></tr></table></td>");
        // Contenido
        fila.append("<td valign='top' style='padding-top:10px; padding-bottom:10px; border-bottom:1px solid ")
                .append(Tema.hex(Tema.BORDE)).append("'>");
        if (!termino.isEmpty()) {
            fila.append("<span style='font-family:").append(Tema.familiaMono()).append("; color:")
                    .append(Tema.hex(Tema.TEXTO_SECUNDARIO)).append("'>Término  ").append(escapar(termino))
                    .append("</span><br>");
        }
        if (!regla.isEmpty()) {
            fila.append("<b style='color:").append(Tema.hex(Tema.TEXTO)).append("'>").append(escapar(regla))
                    .append("</b><br>");
        }
        fila.append("<span style='color:").append(Tema.hex(Tema.TEXTO_SECUNDARIO)).append("'>")
                .append(escapar(explicacion)).append("</span>");
        if (!parcial.isEmpty()) {
            fila.append("<br><span style='color:").append(Tema.hex(Tema.TEXTO_SECUNDARIO)).append("'>Resultado: </span>")
                    .append("<span style='font-family:").append(Tema.familiaMono()).append("; color:")
                    .append(Tema.hex(Tema.PRIMARIO)).append("; font-size:13pt'><b>").append(escapar(parcial))
                    .append("</b></span>");
        }
        if (!nota.isEmpty()) {
            fila.append("<br><i style='color:").append(Tema.hex(Tema.ACENTO)).append("'>").append(escapar(nota))
                    .append("</i>");
        }
        fila.append("</td></tr>");
        return fila.toString();
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
            // Resaltar el prefijo "Paso n:", "Resultado final:", "Consejo:", etc.
            int dosPuntos = linea.indexOf(':');
            if (dosPuntos > 0 && dosPuntos <= 22) {
                String prefijo = escapar(linea.substring(0, dosPuntos + 1));
                String resto = escapar(linea.substring(dosPuntos + 1));
                contenido = "<b style='color:" + Tema.hex(Tema.PRIMARIO) + "'>" + prefijo + "</b>" + resto;
            }
            html.append("<p style='margin-top:0; margin-bottom:9px; line-height:150%'>").append(contenido).append("</p>");
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

    private static String pastilla(String texto, java.awt.Color fondo, java.awt.Color color) {
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
