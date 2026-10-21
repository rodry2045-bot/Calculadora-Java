package calculadora.vista.componentes;

import calculadora.vista.estilo.Tema;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Apariencia delgada y redondeada para las barras de desplazamiento,
 * sin las flechas de los extremos.
 */
public class BarraDesplazamientoModerna extends BasicScrollBarUI {

    /**
     * Aplica el estilo moderno a ambas barras de un JScrollPane y quita su borde.
     *
     * @param panel panel con desplazamiento
     */
    public static void aplicar(JScrollPane panel) {
        panel.getVerticalScrollBar().setUI(new BarraDesplazamientoModerna());
        panel.getHorizontalScrollBar().setUI(new BarraDesplazamientoModerna());
        panel.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        panel.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
        panel.getVerticalScrollBar().setUnitIncrement(16);
        panel.setBorder(null);
        panel.setViewportBorder(null);
        panel.setOpaque(false);
        panel.getViewport().setOpaque(false);
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = new Color(203, 210, 222);
        trackColor = new Color(0, 0, 0, 0);
    }

    @Override
    protected JButton createDecreaseButton(int orientacion) {
        return botonInvisible();
    }

    @Override
    protected JButton createIncreaseButton(int orientacion) {
        return botonInvisible();
    }

    private JButton botonInvisible() {
        JButton boton = new JButton();
        boton.setPreferredSize(new Dimension(0, 0));
        boton.setMinimumSize(new Dimension(0, 0));
        boton.setMaximumSize(new Dimension(0, 0));
        return boton;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle limites) {
        // Riel transparente
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle limites) {
        if (limites.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        g2.setColor(isThumbRollover() ? new Color(160, 170, 190) : thumbColor);
        g2.fillRoundRect(limites.x + 2, limites.y + 2, limites.width - 4, limites.height - 4, 8, 8);
        g2.dispose();
    }
}
