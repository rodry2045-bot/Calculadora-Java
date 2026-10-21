package calculadora.vista.componentes;

import calculadora.vista.estilo.Iconos;
import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Barra superior con degradado, logotipo, título de la aplicación y una
 * zona a la derecha para el estado de la IA y botones.
 */
public class Encabezado extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel zonaDerecha;

    /**
     * @param titulo    título principal
     * @param subtitulo texto descriptivo bajo el título
     */
    public Encabezado(String titulo, String subtitulo) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));

        // Logotipo + textos
        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        izquierda.setOpaque(false);
        JLabel logo = new JLabel(new ImageIcon(Iconos.logotipo(46)));
        izquierda.add(logo);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel etiquetaTitulo = new JLabel(titulo);
        etiquetaTitulo.setFont(Tema.negrita(21f));
        etiquetaTitulo.setForeground(Color.WHITE);
        JLabel etiquetaSubtitulo = new JLabel(subtitulo);
        etiquetaSubtitulo.setFont(Tema.texto(13f));
        etiquetaSubtitulo.setForeground(new Color(224, 231, 255));
        textos.add(etiquetaTitulo);
        textos.add(Box.createVerticalStrut(2));
        textos.add(etiquetaSubtitulo);
        izquierda.add(textos);
        add(izquierda, BorderLayout.WEST);

        zonaDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        zonaDerecha.setOpaque(false);
        add(zonaDerecha, BorderLayout.EAST);
    }

    /**
     * Agrega un componente a la zona derecha del encabezado.
     *
     * @param componente componente a agregar
     */
    public void agregarDerecha(JComponent componente) {
        zonaDerecha.add(componente);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        g2.setPaint(new GradientPaint(0, 0, Tema.PRIMARIO, getWidth(), getHeight(), Tema.SECUNDARIO));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Círculos decorativos translúcidos
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fill(new Ellipse2D.Double(getWidth() - 260, -120, 260, 260));
        g2.fill(new Ellipse2D.Double(getWidth() * 0.55, getHeight() - 30, 180, 180));
        g2.setColor(new Color(255, 255, 255, 10));
        g2.fill(new Ellipse2D.Double(getWidth() - 420, 10, 140, 140));
        g2.dispose();
    }

    /**
     * Pastilla de estado (punto de color + texto) pensada para el encabezado.
     */
    public static class Insignia extends JLabel {

        private static final long serialVersionUID = 1L;
        private Color colorPunto;

        /** Crea la insignia vacía. */
        public Insignia() {
            super("");
            this.colorPunto = Tema.EXITO;
            setFont(Tema.negrita(12.5f));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(7, 26, 7, 14));
        }

        /**
         * @param texto texto de la insignia
         * @param punto color del punto indicador
         */
        public void actualizar(String texto, Color punto) {
            setText(texto);
            this.colorPunto = punto;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension tamano = super.getPreferredSize();
            tamano.height = Math.max(tamano.height, 34);
            return tamano;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Tema.suavizar(g2);
            g2.setColor(new Color(15, 23, 42, 55));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            int diametro = 9;
            int y = (getHeight() - diametro) / 2;
            g2.setColor(new Color(colorPunto.getRed(), colorPunto.getGreen(), colorPunto.getBlue(), 90));
            g2.fillOval(9, y - 3, diametro + 6, diametro + 6);
            g2.setColor(colorPunto);
            g2.fillOval(12, y, diametro, diametro);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
