package calculadora.vista.componentes;

import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Botón con esquinas redondeadas y colores que cambian al pasar el ratón
 * y al presionar. Se dibuja a mano con Graphics2D, por lo que se ve igual
 * en cualquier sistema operativo.
 */
public class BotonModerno extends JButton {

    private static final long serialVersionUID = 1L;

    /** Estilos visuales disponibles. */
    public enum Estilo {
        /** Fondo de color primario y texto blanco: acción principal. */
        PRIMARIO,
        /** Fondo blanco con borde: acciones secundarias. */
        SECUNDARIO,
        /** Sin fondo hasta que se pasa el ratón: acciones discretas. */
        FANTASMA,
        /** Pastilla pequeña con fondo suave: ejemplos rápidos. */
        CHIP,
        /** Tecla del teclado matemático. */
        TECLA,
        /** Botón sobre el encabezado de color (fondo blanco translúcido). */
        SOBRE_COLOR
    }

    private final Estilo estilo;
    private int radio;

    /**
     * @param texto  texto del botón
     * @param estilo estilo visual
     */
    public BotonModerno(String texto, Estilo estilo) {
        super(texto);
        this.estilo = estilo;
        this.radio = Tema.RADIO - 4;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        configurarSegunEstilo();
    }

    private void configurarSegunEstilo() {
        if (estilo == Estilo.PRIMARIO) {
            setForeground(Tema.TEXTO_SOBRE_PRIMARIO);
            setFont(Tema.negrita(14f));
            setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        } else if (estilo == Estilo.CHIP) {
            setForeground(Tema.PRIMARIO);
            setFont(Tema.mono(13f));
            setBorder(BorderFactory.createEmptyBorder(5, 11, 5, 11));
            radio = 999; // forma de píldora
        } else if (estilo == Estilo.TECLA) {
            setForeground(Tema.TEXTO);
            setFont(Tema.mono(15f));
            setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            radio = 10;
        } else if (estilo == Estilo.SOBRE_COLOR) {
            setForeground(Tema.TEXTO);
            setFont(Tema.negrita(13f));
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            radio = 999;
        } else {
            setForeground(Tema.TEXTO);
            setFont(Tema.negrita(13f));
            setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        }
    }

    /**
     * Cambia el radio de las esquinas.
     *
     * @param radio radio en píxeles
     */
    public void setRadio(int radio) {
        this.radio = radio;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension tamano = super.getPreferredSize();
        // Altura mínima cómoda para hacer clic
        if (estilo == Estilo.PRIMARIO || estilo == Estilo.SECUNDARIO) {
            tamano.height = Math.max(tamano.height, 40);
        }
        return tamano;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);

        ButtonModel modelo = getModel();
        boolean presionado = modelo.isArmed() && modelo.isPressed();
        boolean encima = modelo.isRollover();
        boolean activo = isEnabled();

        int ancho = getWidth();
        int alto = getHeight();
        int r = Math.min(radio, alto);
        RoundRectangle2D forma = new RoundRectangle2D.Double(0.5, 0.5, ancho - 1.0, alto - 1.0, r, r);

        Color fondo = null;
        Color borde = null;
        Color texto = getForeground();

        if (estilo == Estilo.PRIMARIO) {
            fondo = Tema.PRIMARIO;
            if (encima) {
                fondo = Tema.PRIMARIO_HOVER;
            }
            if (presionado) {
                fondo = Tema.PRIMARIO_PRESIONADO;
            }
            if (!activo) {
                fondo = Tema.PRIMARIO_SUAVE_FUERTE;
            }
            texto = activo ? Tema.TEXTO_SOBRE_PRIMARIO : Tema.TEXTO_SECUNDARIO;
        } else if (estilo == Estilo.SECUNDARIO) {
            fondo = Tema.SUPERFICIE_ALTERNA;
            borde = Tema.BORDE;
            if (encima) {
                fondo = Tema.PRIMARIO_SUAVE;
                borde = Tema.PRIMARIO_PRESIONADO;
            }
            if (presionado) {
                fondo = Tema.PRIMARIO_SUAVE_FUERTE;
            }
            texto = activo ? Tema.TEXTO : Tema.TEXTO_TENUE;
        } else if (estilo == Estilo.FANTASMA) {
            if (encima) {
                fondo = Tema.PRIMARIO_SUAVE;
            }
            if (presionado) {
                fondo = Tema.PRIMARIO_SUAVE_FUERTE;
            }
            texto = activo ? Tema.PRIMARIO : Tema.TEXTO_TENUE;
        } else if (estilo == Estilo.CHIP) {
            fondo = Tema.PRIMARIO_SUAVE;
            texto = Tema.PRIMARIO;
            if (encima) {
                fondo = Tema.PRIMARIO_SUAVE_FUERTE;
                borde = Tema.PRIMARIO_PRESIONADO;
            }
            if (presionado) {
                fondo = new Color(28, 86, 102);
            }
        } else if (estilo == Estilo.TECLA) {
            fondo = Tema.SUPERFICIE_ALTERNA;
            borde = Tema.BORDE;
            if (encima) {
                fondo = Tema.PRIMARIO_SUAVE;
                borde = Tema.PRIMARIO_PRESIONADO;
            }
            if (presionado) {
                fondo = Tema.PRIMARIO_SUAVE_FUERTE;
            }
        } else if (estilo == Estilo.SOBRE_COLOR) {
            fondo = new Color(255, 255, 255, encima ? 30 : 14);
            if (presionado) {
                fondo = new Color(255, 255, 255, 45);
            }
            borde = new Color(34, 211, 238, encima ? 160 : 90);
            texto = Tema.TEXTO;
        }

        // Sombra sutil para el botón principal
        if (estilo == Estilo.PRIMARIO && activo && !presionado) {
            g2.setColor(new Color(34, 211, 238, 50));
            g2.fill(new RoundRectangle2D.Double(1, 3, ancho - 2.0, alto - 2.0, r, r));
            forma = new RoundRectangle2D.Double(0.5, 0.5, ancho - 1.0, alto - 3.0, r, r);
        }

        if (fondo != null) {
            g2.setColor(fondo);
            g2.fill(forma);
        }
        if (borde != null) {
            g2.setColor(borde);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(forma);
        }
        g2.dispose();

        // El texto lo dibuja Swing con el color elegido
        setForeground(texto);
        super.paintComponent(g);
    }
}
