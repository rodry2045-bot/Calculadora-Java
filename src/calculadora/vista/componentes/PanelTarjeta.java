package calculadora.vista.componentes;

import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

/**
 * Panel blanco con esquinas redondeadas y una sombra suave, al estilo de
 * las "tarjetas" de las aplicaciones modernas.
 */
public class PanelTarjeta extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Espacio reservado alrededor para dibujar la sombra. */
    private static final int SOMBRA = 4;

    private Color colorFondo;

    /**
     * @param distribucion administrador de diseño del contenido
     */
    public PanelTarjeta(LayoutManager distribucion) {
        super(distribucion);
        this.colorFondo = Tema.SUPERFICIE;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16 + 0, 18, 16 + SOMBRA, 18 + SOMBRA / 2));
    }

    /**
     * Crea una tarjeta con un título en la parte superior.
     *
     * @param titulo    texto del título
     * @param contenido componente principal
     * @return la tarjeta construida
     */
    public static PanelTarjeta conTitulo(String titulo, java.awt.Component contenido) {
        PanelTarjeta tarjeta = new PanelTarjeta(new BorderLayout(0, 10));
        JLabel etiqueta = crearTitulo(titulo);
        tarjeta.add(etiqueta, BorderLayout.NORTH);
        tarjeta.add(contenido, BorderLayout.CENTER);
        return tarjeta;
    }

    /**
     * Crea una etiqueta con el estilo de título de sección (mayúsculas pequeñas).
     *
     * @param texto texto del título
     * @return etiqueta con estilo
     */
    public static JLabel crearTitulo(String texto) {
        JLabel etiqueta = new JLabel(texto.toUpperCase());
        etiqueta.setFont(Tema.negrita(11.5f));
        etiqueta.setForeground(Tema.TEXTO_SECUNDARIO);
        return etiqueta;
    }

    /**
     * Cambia el color de fondo de la tarjeta.
     *
     * @param color nuevo color
     */
    public void setColorFondo(Color color) {
        this.colorFondo = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        int ancho = getWidth() - SOMBRA / 2;
        int alto = getHeight() - SOMBRA;

        // Sombra: varias capas con transparencia creciente
        for (int i = SOMBRA; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 22));
            g2.fill(new RoundRectangle2D.Double(0, i, ancho, alto, Tema.RADIO + i, Tema.RADIO + i));
        }

        RoundRectangle2D forma = new RoundRectangle2D.Double(0, 0, ancho - 1.0, alto - 1.0, Tema.RADIO * 2.0,
                Tema.RADIO * 2.0);
        g2.setColor(colorFondo);
        g2.fill(forma);
        g2.setColor(Tema.BORDE);
        g2.draw(forma);
        g2.dispose();
        super.paintComponent(g);
    }
}
