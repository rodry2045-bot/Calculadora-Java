package calculadora.vista.componentes;

import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.JPasswordField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Campo para contraseñas o claves de API con el mismo aspecto que
 * {@link CampoModerno}. Oculta los caracteres por defecto.
 */
public class CampoClave extends JPasswordField {

    private static final long serialVersionUID = 1L;

    /** Carácter que se muestra en lugar de cada letra de la clave. */
    private static final char CARACTER_OCULTO = '•';

    /**
     * @param columnas ancho aproximado en caracteres
     */
    public CampoClave(int columnas) {
        super(columnas);
        setOpaque(false);
        setFont(Tema.mono(14f));
        setForeground(Tema.TEXTO);
        setCaretColor(Tema.PRIMARIO);
        setEchoChar(CARACTER_OCULTO);
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
     * @param visible true para mostrar la clave en texto claro
     */
    public void setClaveVisible(boolean visible) {
        if (visible) {
            setEchoChar((char) 0);
        } else {
            setEchoChar(CARACTER_OCULTO);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        RoundRectangle2D forma = new RoundRectangle2D.Double(1, 1, getWidth() - 2.0, getHeight() - 2.0, 12, 12);
        g2.setColor(isEnabled() ? Tema.SUPERFICIE : Tema.SUPERFICIE_ALTERNA);
        g2.fill(forma);
        if (hasFocus()) {
            g2.setColor(new Color(129, 140, 248, 60));
            g2.setStroke(new BasicStroke(4f));
            g2.draw(forma);
            g2.setColor(Tema.BORDE_FOCO);
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(Tema.BORDE);
        }
        g2.draw(forma);
        g2.dispose();
        super.paintComponent(g);
    }
}
