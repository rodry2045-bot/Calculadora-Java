package calculadora.vista.componentes;

import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Campo de texto con esquinas redondeadas, borde que se ilumina al tener el
 * foco y un texto de ayuda (placeholder) cuando está vacío.
 */
public class CampoModerno extends JTextField {

    private static final long serialVersionUID = 1L;

    private String textoAyuda;

    /**
     * @param texto   texto inicial
     * @param columnas ancho aproximado en caracteres
     */
    public CampoModerno(String texto, int columnas) {
        super(texto, columnas);
        this.textoAyuda = "";
        setOpaque(false);
        setFont(Tema.texto(14f));
        setForeground(Tema.TEXTO);
        setCaretColor(Tema.PRIMARIO);
        setSelectionColor(new Color(21, 94, 117));
        setDisabledTextColor(Tema.TEXTO_TENUE);
        setSelectedTextColor(Tema.TEXTO);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    /**
     * @param texto texto gris que se muestra cuando el campo está vacío
     */
    public void setTextoAyuda(String texto) {
        this.textoAyuda = texto;
        repaint();
    }

    /**
     * Cambia el relleno interno (útil para campos grandes).
     *
     * @param vertical   relleno arriba y abajo
     * @param horizontal relleno a los lados
     */
    public void setRelleno(int vertical, int horizontal) {
        setBorder(BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        RoundRectangle2D forma = new RoundRectangle2D.Double(1, 1, getWidth() - 2.0, getHeight() - 2.0, 12, 12);

        Color fondo = isEnabled() ? Tema.CAMPO : Tema.SUPERFICIE_ALTERNA;
        g2.setColor(fondo);
        g2.fill(forma);

        if (hasFocus()) {
            // Halo de foco
            g2.setColor(new Color(34, 211, 238, 55));
            g2.setStroke(new BasicStroke(4f));
            g2.draw(forma);
            g2.setColor(Tema.BORDE_FOCO);
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(Tema.BORDE);
            g2.setStroke(new BasicStroke(1f));
        }
        g2.draw(forma);
        g2.dispose();

        super.paintComponent(g);

        // Texto de ayuda cuando está vacío
        if (getText().isEmpty() && textoAyuda != null && !textoAyuda.isEmpty() && !hasFocus()) {
            Graphics2D g3 = (Graphics2D) g.create();
            Tema.suavizar(g3);
            g3.setColor(Tema.TEXTO_TENUE);
            g3.setFont(getFont());
            Insets margen = getInsets();
            FontMetrics metricas = g3.getFontMetrics();
            int y = (getHeight() - metricas.getHeight()) / 2 + metricas.getAscent();
            g3.drawString(textoAyuda, margen.left, y);
            g3.dispose();
        }
    }
}
