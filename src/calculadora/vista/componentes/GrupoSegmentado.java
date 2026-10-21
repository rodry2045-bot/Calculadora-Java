package calculadora.vista.componentes;

import calculadora.vista.estilo.Tema;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Control "segmentado": una fila de opciones donde solo una está activa
 * (como los selectores de iOS o de las aplicaciones web modernas).
 * Reemplaza a los JComboBox para que todas las opciones estén a la vista.
 */
public class GrupoSegmentado extends JComponent {

    private static final long serialVersionUID = 1L;

    private final String[] opciones;
    private final transient List<ActionListener> oyentes;
    private int seleccionado;
    private int encima;
    private boolean ocuparAncho;

    /**
     * @param opciones textos de las opciones
     */
    public GrupoSegmentado(String... opciones) {
        this.opciones = opciones;
        this.oyentes = new ArrayList<ActionListener>();
        this.seleccionado = 0;
        this.encima = -1;
        this.ocuparAncho = true;
        setFont(Tema.negrita(13f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        configurarRaton();
    }

    /**
     * @param ocupar true para repartir todo el ancho disponible entre las opciones
     */
    public void setOcuparAncho(boolean ocupar) {
        this.ocuparAncho = ocupar;
    }

    /** @return índice de la opción activa */
    public int getSeleccionado() {
        return seleccionado;
    }

    /** @return texto de la opción activa */
    public String getTextoSeleccionado() {
        return opciones[seleccionado];
    }

    /**
     * Cambia la opción activa y avisa a los oyentes.
     *
     * @param indice nueva opción
     */
    public void setSeleccionado(int indice) {
        if (indice < 0 || indice >= opciones.length) {
            return;
        }
        boolean cambio = (indice != seleccionado);
        this.seleccionado = indice;
        repaint();
        if (cambio) {
            ActionEvent evento = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, opciones[indice]);
            for (int i = 0; i < oyentes.size(); i++) {
                oyentes.get(i).actionPerformed(evento);
            }
        }
    }

    /**
     * @param oyente se ejecuta cada vez que cambia la opción activa
     */
    public void addActionListener(ActionListener oyente) {
        oyentes.add(oyente);
    }

    private void configurarRaton() {
        MouseAdapter adaptador = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int indice = indiceEn(e.getX());
                if (indice >= 0 && isEnabled()) {
                    setSeleccionado(indice);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int indice = indiceEn(e.getX());
                if (indice != encima) {
                    encima = indice;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                encima = -1;
                repaint();
            }
        };
        addMouseListener(adaptador);
        addMouseMotionListener(adaptador);
    }

    /** Ancho de cada segmento en píxeles. */
    private double[] calcularAnchos() {
        double[] anchos = new double[opciones.length];
        FontMetrics metricas = getFontMetrics(getFont());
        double total = 0;
        for (int i = 0; i < opciones.length; i++) {
            anchos[i] = metricas.stringWidth(opciones[i]) + 28;
            total = total + anchos[i];
        }
        double disponible = getWidth() - 8;
        if (ocuparAncho && total > 0) {
            for (int i = 0; i < anchos.length; i++) {
                anchos[i] = anchos[i] * disponible / total;
            }
        }
        return anchos;
    }

    private int indiceEn(int x) {
        double[] anchos = calcularAnchos();
        double inicio = 4;
        for (int i = 0; i < anchos.length; i++) {
            if (x >= inicio && x < inicio + anchos[i]) {
                return i;
            }
            inicio = inicio + anchos[i];
        }
        return -1;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metricas = getFontMetrics(getFont());
        int ancho = 8;
        for (int i = 0; i < opciones.length; i++) {
            ancho = ancho + metricas.stringWidth(opciones[i]) + 28;
        }
        return new Dimension(ancho, metricas.getHeight() + 18);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(80, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        int alto = getHeight();
        double[] anchos = calcularAnchos();
        double anchoTotal = 8;
        for (int i = 0; i < anchos.length; i++) {
            anchoTotal = anchoTotal + anchos[i];
        }

        // Fondo del riel
        g2.setColor(new Color(236, 239, 246));
        g2.fill(new RoundRectangle2D.Double(0, 0, anchoTotal, alto, 12, 12));

        Font fuente = getFont();
        g2.setFont(fuente);
        FontMetrics metricas = g2.getFontMetrics();
        double x = 4;
        for (int i = 0; i < opciones.length; i++) {
            if (i == seleccionado) {
                // Pastilla blanca con sombra para la opción activa
                g2.setColor(new Color(15, 23, 42, 22));
                g2.fill(new RoundRectangle2D.Double(x, 5, anchos[i], alto - 8, 9, 9));
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(x, 4, anchos[i], alto - 8, 9, 9));
                g2.setColor(isEnabled() ? Tema.PRIMARIO : Tema.TEXTO_TENUE);
            } else if (i == encima && isEnabled()) {
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fill(new RoundRectangle2D.Double(x, 4, anchos[i], alto - 8, 9, 9));
                g2.setColor(Tema.TEXTO);
            } else {
                g2.setColor(isEnabled() ? Tema.TEXTO_SECUNDARIO : Tema.TEXTO_TENUE);
            }
            int anchoTexto = metricas.stringWidth(opciones[i]);
            int tx = (int) Math.round(x + (anchos[i] - anchoTexto) / 2.0);
            int ty = (alto - metricas.getHeight()) / 2 + metricas.getAscent();
            g2.drawString(opciones[i], tx, ty);
            x = x + anchos[i];
        }
        g2.dispose();
    }
}
