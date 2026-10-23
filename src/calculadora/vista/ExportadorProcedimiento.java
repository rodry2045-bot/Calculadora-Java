package calculadora.vista;

import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ResultadoCalculo;

import javax.swing.JEditorPane;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exporta el procedimiento de un cálculo para entregarlo como tarea:
 * <ul>
 *   <li>como página HTML (se abre en cualquier navegador y se puede imprimir);</li>
 *   <li>o directamente a la impresora. En Windows, eligiendo la impresora
 *       «Microsoft Print to PDF» se obtiene un archivo PDF.</li>
 * </ul>
 * Usa colores claros (fondo blanco) para que se lea bien en papel.
 */
public final class ExportadorProcedimiento {

    private ExportadorProcedimiento() {
    }

    /**
     * Guarda el procedimiento como página HTML.
     *
     * @param resultado cálculo a exportar
     * @param archivo   archivo de destino
     * @throws IOException si no se puede escribir el archivo
     */
    public static void guardarHtml(ResultadoCalculo resultado, File archivo) throws IOException {
        try (Writer escritor = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
            escritor.write(html(resultado, true));
        }
    }

    /**
     * Abre el diálogo de impresión del sistema con el procedimiento.
     *
     * @param resultado cálculo a imprimir
     * @return true si se imprimió; false si el usuario canceló
     * @throws PrinterException si la impresora falla
     */
    public static boolean imprimir(ResultadoCalculo resultado) throws PrinterException {
        JEditorPane hoja = new JEditorPane();
        hoja.setContentType("text/html");
        hoja.setText(html(resultado, false));
        hoja.setSize(700, 10);
        return hoja.print();
    }

    /**
     * @param resultado cálculo
     * @param completo  true para un documento HTML completo (con estilos y codificación)
     * @return procedimiento en HTML con colores claros
     */
    static String html(ResultadoCalculo resultado, boolean completo) {
        StringBuilder h = new StringBuilder();
        if (completo) {
            h.append("<!DOCTYPE html><html lang='es'><head><meta charset='UTF-8'>")
                    .append("<title>Procedimiento - Calculadora de Cálculo</title>")
                    .append("<style>body{font-family:Segoe UI,Arial,sans-serif;max-width:820px;margin:24px auto;")
                    .append("padding:0 16px;color:#1f2937;line-height:1.45}h1{font-size:22px;margin-bottom:4px}")
                    .append(".sub{color:#6b7280;margin-top:0}.mono{font-family:Consolas,monospace}")
                    .append(".paso{border-left:3px solid #d1d5db;padding:6px 12px;margin:8px 0}")
                    .append(".destacado{border-left-color:#0891b2;background:#ecfeff}")
                    .append(".nota{background:#fef3c7;padding:8px 12px;margin:8px 0}")
                    .append(".final{background:#dcfce7;padding:12px;margin-top:16px;font-size:17px}")
                    .append(".res{color:#0e7490;font-weight:bold}</style></head><body>");
        } else {
            h.append("<html><body style='font-family:Arial; font-size:11pt; color:#1f2937'>");
        }
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        h.append("<h1>").append(e(resultado.getTipo().getNombre()));
        if (!resultado.getNombreMetodo().isEmpty()) {
            h.append(" — ").append(e(resultado.getNombreMetodo()));
        }
        h.append("</h1><p class='sub' style='color:#6b7280'>Calculadora de Cálculo · ").append(fecha).append("</p>");
        String expresion = resultado.getTipo() == ResultadoCalculo.TipoOperacion.DERIVADA_IMPLICITA
                ? resultado.getTextoOriginal() : String.valueOf(resultado.getExpresion());
        h.append("<p><b>Función:</b> <span class='mono' style='font-family:monospace'>").append(e(expresion))
                .append("</span></p>");

        List<String> notas = resultado.getNotas();
        for (int i = 0; i < notas.size(); i++) {
            h.append(completo ? "<div class='nota'>" : "<p style='background:#fef3c7'>")
                    .append("<b>Nota:</b> ").append(e(notas.get(i))).append(completo ? "</div>" : "</p>");
        }

        h.append("<h2 style='font-size:17px'>Procedimiento</h2>");
        List<PasoDetallado> pasos = resultado.getPasosDetallados();
        for (int i = 0; i < pasos.size(); i++) {
            PasoDetallado p = pasos.get(i);
            int sangria = Math.min(p.getProfundidad(), 6) * 18;
            if (completo) {
                h.append("<div class='paso").append(p.isDestacado() ? " destacado" : "").append("' style='margin-left:")
                        .append(sangria).append("px'>");
            } else {
                h.append("<p style='margin-left:").append(sangria).append("px'>");
            }
            h.append("<b>").append(i + 1).append(". ").append(e(p.getTitulo())).append("</b>");
            if (p.isDestacado()) {
                h.append(" <i>(método elegido)</i>");
            }
            if (!p.getExpresion().isEmpty()) {
                h.append("<br><span class='mono' style='font-family:monospace'>").append(e(p.getExpresion()))
                        .append("</span>");
            }
            if (!p.getDetalle().isEmpty()) {
                h.append("<br>").append(e(p.getDetalle()));
            }
            if (!p.getResultado().isEmpty()) {
                h.append("<br>⇒ <span class='res mono' style='font-family:monospace; color:#0e7490'><b>")
                        .append(e(p.getResultado())).append("</b></span>");
            }
            h.append(completo ? "</div>" : "</p>");
        }
        h.append(completo ? "<div class='final'>" : "<p style='background:#dcfce7'>").append("<b>Resultado:</b> ")
                .append("<span class='mono' style='font-family:monospace'>")
                .append(e(resultado.getTextoResultado()).replace("\n", "<br>")).append("</span>")
                .append(completo ? "</div>" : "</p>");
        h.append("</body></html>");
        return h.toString();
    }

    private static String e(String texto) {
        return RenderizadorHtml.escapar(texto);
    }
}
