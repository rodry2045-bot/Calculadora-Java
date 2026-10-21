package calculadora.vista.estilo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera por código el ícono de la aplicación (un cuadrado redondeado con
 * degradado y el símbolo de integral). Así no se necesitan archivos de imagen.
 */
public final class Iconos {

    /** Constructor privado: clase utilitaria. */
    private Iconos() {
    }

    /**
     * Dibuja el logotipo en el tamaño pedido.
     *
     * @param tamano ancho y alto en píxeles
     * @return imagen del logotipo
     */
    public static BufferedImage logotipo(int tamano) {
        BufferedImage imagen = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagen.createGraphics();
        Tema.suavizar(g2);

        // Fondo con degradado diagonal
        g2.setPaint(new GradientPaint(0, 0, Tema.PRIMARIO, tamano, tamano, new Color(20, 184, 166)));
        int radio = Math.max(4, tamano / 3);
        g2.fillRoundRect(0, 0, tamano, tamano, radio, radio);

        // Brillo superior
        g2.setColor(new Color(255, 255, 255, 35));
        g2.fillRoundRect(0, 0, tamano, tamano / 2, radio, radio);

        // Símbolo de integral
        Font fuente = new Font(Font.SERIF, Font.BOLD | Font.ITALIC, Math.round(tamano * 0.78f));
        g2.setFont(fuente);
        FontMetrics metricas = g2.getFontMetrics();
        String simbolo = "∫";
        int x = (tamano - metricas.stringWidth(simbolo)) / 2;
        int y = (tamano - metricas.getHeight()) / 2 + metricas.getAscent();
        g2.setColor(Tema.TEXTO_SOBRE_PRIMARIO);
        g2.drawString(simbolo, x, y);
        g2.dispose();
        return imagen;
    }

    /**
     * @return el logotipo en varios tamaños para la barra de tareas y la ventana
     */
    public static List<Image> iconosVentana() {
        List<Image> lista = new ArrayList<Image>();
        int[] tamanos = {16, 24, 32, 48, 64, 128};
        for (int i = 0; i < tamanos.length; i++) {
            lista.add(logotipo(tamanos[i]));
        }
        return lista;
    }
}
