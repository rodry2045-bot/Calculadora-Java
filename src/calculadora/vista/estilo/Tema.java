package calculadora.vista.estilo;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Graphics2D;

/**
 * Paleta de colores y tipografías de la aplicación.
 * <p>
 * Centralizar el estilo en una sola clase permite cambiar la apariencia
 * completa de la calculadora modificando únicamente estas constantes.
 * </p>
 * Las fuentes se eligen según lo que esté instalado: en Windows se usan
 * "Segoe UI" (textos) y "Consolas" (expresiones matemáticas).
 */
public final class Tema {

    // ------------------------------------------------------------------
    // Colores principales
    // ------------------------------------------------------------------

    /** Color de marca (índigo). */
    public static final Color PRIMARIO = new Color(79, 70, 229);
    /** Primario al pasar el ratón. */
    public static final Color PRIMARIO_HOVER = new Color(67, 56, 202);
    /** Primario al presionar. */
    public static final Color PRIMARIO_PRESIONADO = new Color(55, 48, 163);
    /** Fondo suave del color primario. */
    public static final Color PRIMARIO_SUAVE = new Color(238, 240, 255);
    /** Segundo color del degradado del encabezado (violeta). */
    public static final Color SECUNDARIO = new Color(124, 58, 237);
    /** Color de acento (verde azulado). */
    public static final Color ACENTO = new Color(13, 148, 136);
    /** Fondo suave del acento. */
    public static final Color ACENTO_SUAVE = new Color(220, 245, 241);

    /** Fondo general de la ventana. */
    public static final Color FONDO = new Color(243, 245, 250);
    /** Fondo de tarjetas. */
    public static final Color SUPERFICIE = Color.WHITE;
    /** Fondo de elementos secundarios (teclas, campos deshabilitados). */
    public static final Color SUPERFICIE_ALTERNA = new Color(247, 248, 252);
    /** Bordes finos. */
    public static final Color BORDE = new Color(225, 229, 238);
    /** Borde de un campo con el foco. */
    public static final Color BORDE_FOCO = new Color(129, 140, 248);

    /** Texto principal. */
    public static final Color TEXTO = new Color(30, 41, 59);
    /** Texto secundario. */
    public static final Color TEXTO_SECUNDARIO = new Color(100, 116, 139);
    /** Texto deshabilitado o de ayuda. */
    public static final Color TEXTO_TENUE = new Color(160, 170, 186);

    /** Éxito. */
    public static final Color EXITO = new Color(22, 163, 74);
    /** Advertencia. */
    public static final Color ADVERTENCIA = new Color(217, 119, 6);
    /** Error. */
    public static final Color ERROR = new Color(220, 38, 38);

    // Colores de la gráfica
    /** Curva principal f. */
    public static final Color GRAFICA_FUNCION = new Color(79, 70, 229);
    /** Curva secundaria (derivada o antiderivada). */
    public static final Color GRAFICA_SECUNDARIA = new Color(236, 72, 153);
    /** Área sombreada de la integral. */
    public static final Color GRAFICA_AREA = new Color(20, 184, 166);

    /** Radio de las esquinas redondeadas. */
    public static final int RADIO = 14;

    // ------------------------------------------------------------------
    // Tipografías
    // ------------------------------------------------------------------

    private static final String FAMILIA_TEXTO = elegirFamilia(
            new String[]{"Segoe UI", "Inter", "Helvetica Neue", "DejaVu Sans"}, Font.SANS_SERIF);
    private static final String FAMILIA_MONO = elegirFamilia(
            new String[]{"Consolas", "Cascadia Mono", "JetBrains Mono", "DejaVu Sans Mono"}, Font.MONOSPACED);

    /** Constructor privado: clase de constantes. */
    private Tema() {
    }

    /**
     * Devuelve la primera familia de fuente instalada de la lista.
     *
     * @param candidatas familias en orden de preferencia
     * @param respaldo   familia lógica a usar si no hay ninguna
     * @return nombre de la familia elegida
     */
    private static String elegirFamilia(String[] candidatas, String respaldo) {
        String[] instaladas;
        try {
            instaladas = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        } catch (RuntimeException e) {
            return respaldo;
        }
        for (int i = 0; i < candidatas.length; i++) {
            for (int j = 0; j < instaladas.length; j++) {
                if (instaladas[j].equalsIgnoreCase(candidatas[i])) {
                    return instaladas[j];
                }
            }
        }
        return respaldo;
    }

    /**
     * @param tamano tamaño en puntos
     * @return fuente normal para textos
     */
    public static Font texto(float tamano) {
        return new Font(FAMILIA_TEXTO, Font.PLAIN, 12).deriveFont(tamano);
    }

    /**
     * @param tamano tamaño en puntos
     * @return fuente en negrita para títulos y botones
     */
    public static Font negrita(float tamano) {
        return new Font(FAMILIA_TEXTO, Font.BOLD, 12).deriveFont(tamano);
    }

    /**
     * @param tamano tamaño en puntos
     * @return fuente monoespaciada para expresiones
     */
    public static Font mono(float tamano) {
        return new Font(FAMILIA_MONO, Font.PLAIN, 12).deriveFont(tamano);
    }

    /** @return nombre de la familia de texto (útil para HTML) */
    public static String familiaTexto() {
        return FAMILIA_TEXTO;
    }

    /** @return nombre de la familia monoespaciada (útil para HTML) */
    public static String familiaMono() {
        return FAMILIA_MONO;
    }

    /**
     * Activa el suavizado de bordes y de texto en un Graphics2D.
     *
     * @param g2 contexto gráfico
     */
    public static void suavizar(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    /**
     * Convierte un color a formato hexadecimal para usarlo en HTML.
     *
     * @param color color
     * @return texto como "#4f46e5"
     */
    public static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
