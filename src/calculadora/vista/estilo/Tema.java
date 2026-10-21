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
    // Colores principales (MODO OSCURO con acentos cian / turquesa)
    // ------------------------------------------------------------------

    /** Color de marca (cian). */
    public static final Color PRIMARIO = new Color(34, 211, 238);
    /** Primario al pasar el ratón. */
    public static final Color PRIMARIO_HOVER = new Color(103, 232, 249);
    /** Primario al presionar. */
    public static final Color PRIMARIO_PRESIONADO = new Color(6, 182, 212);
    /** Fondo suave del color primario. */
    public static final Color PRIMARIO_SUAVE = new Color(16, 46, 56);
    /** Fondo suave más intenso (hover y selección). */
    public static final Color PRIMARIO_SUAVE_FUERTE = new Color(21, 66, 79);
    /** Texto sobre botones de color primario. */
    public static final Color TEXTO_SOBRE_PRIMARIO = new Color(6, 24, 32);
    /** Segundo color de acento (turquesa). */
    public static final Color SECUNDARIO = new Color(45, 212, 191);
    /** Color de acento para notas (violeta suave). */
    public static final Color ACENTO = new Color(167, 139, 250);
    /** Fondo suave del acento. */
    public static final Color ACENTO_SUAVE = new Color(36, 30, 64);

    /** Fondo general de la ventana. */
    public static final Color FONDO = new Color(13, 17, 23);
    /** Fondo de tarjetas. */
    public static final Color SUPERFICIE = new Color(22, 27, 34);
    /** Fondo de elementos secundarios (teclas, rieles). */
    public static final Color SUPERFICIE_ALTERNA = new Color(30, 37, 47);
    /** Fondo de los campos de texto. */
    public static final Color CAMPO = new Color(11, 15, 20);
    /** Bordes finos. */
    public static final Color BORDE = new Color(42, 50, 63);
    /** Borde de un campo con el foco. */
    public static final Color BORDE_FOCO = new Color(34, 211, 238);

    /** Texto principal. */
    public static final Color TEXTO = new Color(230, 237, 243);
    /** Texto secundario. */
    public static final Color TEXTO_SECUNDARIO = new Color(148, 163, 184);
    /** Texto deshabilitado o de ayuda. */
    public static final Color TEXTO_TENUE = new Color(92, 104, 120);

    /** Éxito. */
    public static final Color EXITO = new Color(74, 222, 128);
    /** Fondo suave de éxito. */
    public static final Color EXITO_SUAVE = new Color(16, 44, 30);
    /** Advertencia. */
    public static final Color ADVERTENCIA = new Color(251, 191, 36);
    /** Fondo suave de advertencia. */
    public static final Color ADVERTENCIA_SUAVE = new Color(51, 38, 10);
    /** Error. */
    public static final Color ERROR = new Color(248, 113, 113);
    /** Fondo suave de error. */
    public static final Color ERROR_SUAVE = new Color(55, 20, 24);

    /** Inicio del degradado del encabezado. */
    public static final Color ENCABEZADO_INICIO = new Color(15, 23, 34);
    /** Fin del degradado del encabezado. */
    public static final Color ENCABEZADO_FIN = new Color(8, 51, 61);

    // Colores de la gráfica
    /** Fondo de la gráfica. */
    public static final Color GRAFICA_FONDO = new Color(11, 15, 20);
    /** Cuadrícula de la gráfica. */
    public static final Color GRAFICA_CUADRICULA = new Color(30, 37, 47);
    /** Ejes de la gráfica. */
    public static final Color GRAFICA_EJES = new Color(110, 124, 142);
    /** Curva principal f. */
    public static final Color GRAFICA_FUNCION = new Color(34, 211, 238);
    /** Curva secundaria (derivada o antiderivada). */
    public static final Color GRAFICA_SECUNDARIA = new Color(244, 114, 182);
    /** Área sombreada de la integral. */
    public static final Color GRAFICA_AREA = new Color(45, 212, 191);

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
