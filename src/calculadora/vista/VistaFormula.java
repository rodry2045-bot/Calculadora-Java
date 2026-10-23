package calculadora.vista;

import calculadora.simbolico.Nodo;
import calculadora.simbolico.NodoCociente;
import calculadora.simbolico.NodoConstante;
import calculadora.simbolico.NodoFuncion;
import calculadora.simbolico.NodoNegativo;
import calculadora.simbolico.NodoPotencia;
import calculadora.simbolico.NodoProducto;
import calculadora.simbolico.NodoResta;
import calculadora.simbolico.NodoSuma;
import calculadora.simbolico.NodoVariable;
import calculadora.simbolico.TipoFuncion;
import calculadora.vista.estilo.Tema;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja una fórmula "como en el cuaderno": fracciones con raya horizontal,
 * exponentes pequeños y elevados, raíces con su signo y paréntesis que crecen
 * según el contenido.
 * <p>
 * Funciona con CAJAS: cada parte de la fórmula se convierte en una caja que
 * sabe su ancho, cuánto sube sobre la línea base (ascenso) y cuánto baja
 * (descenso). Las cajas se combinan: una fila pone cajas una al lado de otra,
 * una fracción pone una caja encima de otra, etc.
 * </p>
 */
public class VistaFormula extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final float TAMANO = 21f;

    private transient Nodo izquierda;
    private transient Nodo derecha;
    private String mensaje = "";

    /** Crea la vista vacía. */
    public VistaFormula() {
        setOpaque(false);
        setPreferredSize(new Dimension(200, 64));
    }

    /**
     * @param formula fórmula a dibujar
     */
    public void setFormula(Nodo formula) {
        this.izquierda = formula;
        this.derecha = null;
        this.mensaje = "";
        repaint();
    }

    /**
     * Dibuja una ecuación izquierda = derecha.
     *
     * @param izquierda lado izquierdo
     * @param derecha   lado derecho
     */
    public void setEcuacion(Nodo izquierda, Nodo derecha) {
        this.izquierda = izquierda;
        this.derecha = derecha;
        this.mensaje = "";
        repaint();
    }

    /**
     * @param mensaje texto tenue que se muestra cuando no hay fórmula
     */
    public void setMensaje(String mensaje) {
        this.izquierda = null;
        this.derecha = null;
        this.mensaje = mensaje;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.suavizar(g2);
        if (izquierda == null) {
            g2.setFont(Tema.texto(13f));
            g2.setColor(Tema.TEXTO_TENUE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(mensaje, 4, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
            return;
        }
        Caja caja = construirCompleta(g2);
        double alto = caja.ascenso + caja.descenso;
        double escala = Math.min(1.0, Math.min((getWidth() - 8) / Math.max(1, caja.ancho), (getHeight() - 6) / Math.max(1, alto)));
        g2.translate(4, (getHeight() - alto * escala) / 2 + caja.ascenso * escala);
        g2.scale(escala, escala);
        caja.dibujar(g2, 0, 0);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        if (izquierda == null) {
            return new Dimension(200, 40);
        }
        BufferedImage imagen = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagen.createGraphics();
        Caja caja = construirCompleta(g2);
        g2.dispose();
        int alto = (int) Math.ceil(caja.ascenso + caja.descenso) + 12;
        return new Dimension(200, Math.max(44, Math.min(alto, 130)));
    }

    private Caja construirCompleta(Graphics2D g2) {
        Constructor c = new Constructor(g2);
        if (derecha == null) {
            return c.caja(izquierda, TAMANO);
        }
        List<Caja> partes = new ArrayList<Caja>();
        partes.add(c.caja(izquierda, TAMANO));
        partes.add(c.texto(" = ", TAMANO, false));
        partes.add(c.caja(derecha, TAMANO));
        return new Fila(partes);
    }

    // ==================================================================
    // Cajas
    // ==================================================================

    /** Rectángulo con línea base: la unidad de la composición. */
    private abstract static class Caja {
        double ancho;
        double ascenso;
        double descenso;

        abstract void dibujar(Graphics2D g2, double x, double base);
    }

    /** Texto simple (número, variable, operador o nombre de función). */
    private static final class Texto extends Caja {
        private final String texto;
        private final Font fuente;
        private final Color color;

        Texto(String texto, Font fuente, Color color, FontMetrics fm) {
            this.texto = texto;
            this.fuente = fuente;
            this.color = color;
            this.ancho = fm.stringWidth(texto);
            this.ascenso = fm.getAscent() * 0.78;
            this.descenso = fm.getDescent() * 0.6;
        }

        @Override
        void dibujar(Graphics2D g2, double x, double base) {
            g2.setFont(fuente);
            g2.setColor(color);
            g2.drawString(texto, (float) x, (float) base);
        }
    }

    /** Cajas una al lado de otra. */
    private static final class Fila extends Caja {
        private final List<Caja> partes;

        Fila(List<Caja> partes) {
            this.partes = partes;
            for (int i = 0; i < partes.size(); i++) {
                Caja p = partes.get(i);
                ancho += p.ancho;
                ascenso = Math.max(ascenso, p.ascenso);
                descenso = Math.max(descenso, p.descenso);
            }
        }

        @Override
        void dibujar(Graphics2D g2, double x, double base) {
            double actual = x;
            for (int i = 0; i < partes.size(); i++) {
                partes.get(i).dibujar(g2, actual, base);
                actual += partes.get(i).ancho;
            }
        }
    }

    /** Numerador sobre denominador con raya. */
    private static final class Fraccion extends Caja {
        private final Caja numerador;
        private final Caja denominador;
        private final double eje;
        private final double margen = 4;

        Fraccion(Caja numerador, Caja denominador, double eje) {
            this.numerador = numerador;
            this.denominador = denominador;
            this.eje = eje;
            this.ancho = Math.max(numerador.ancho, denominador.ancho) + 2 * margen;
            this.ascenso = eje + numerador.descenso + numerador.ascenso + 3;
            this.descenso = -eje + denominador.ascenso + denominador.descenso + 3;
        }

        @Override
        void dibujar(Graphics2D g2, double x, double base) {
            double raya = base - eje;
            numerador.dibujar(g2, x + (ancho - numerador.ancho) / 2, raya - 3 - numerador.descenso);
            denominador.dibujar(g2, x + (ancho - denominador.ancho) / 2, raya + 3 + denominador.ascenso);
            g2.setColor(Tema.TEXTO);
            g2.setStroke(new BasicStroke(1.3f));
            g2.draw(new java.awt.geom.Line2D.Double(x + 1, raya, x + ancho - 1, raya));
        }
    }

    /** Base con exponente pequeño elevado. */
    private static final class Potencia extends Caja {
        private final Caja base;
        private final Caja exponente;
        private final double subida;

        Potencia(Caja base, Caja exponente) {
            this.base = base;
            this.exponente = exponente;
            this.subida = Math.max(base.ascenso * 0.62, base.ascenso - exponente.ascenso * 0.5);
            this.ancho = base.ancho + exponente.ancho + 1;
            this.ascenso = Math.max(base.ascenso, subida + exponente.ascenso);
            this.descenso = base.descenso;
        }

        @Override
        void dibujar(Graphics2D g2, double x, double linea) {
            base.dibujar(g2, x, linea);
            exponente.dibujar(g2, x + base.ancho + 1, linea - subida);
        }
    }

    /** Signo de raíz con raya encima del radicando. */
    private static final class Raiz extends Caja {
        private final Caja radicando;
        private final double signo;

        Raiz(Caja radicando) {
            this.radicando = radicando;
            this.signo = 10 + (radicando.ascenso + radicando.descenso) * 0.15;
            this.ancho = signo + radicando.ancho + 3;
            this.ascenso = radicando.ascenso + 4;
            this.descenso = radicando.descenso + 1;
        }

        @Override
        void dibujar(Graphics2D g2, double x, double base) {
            double arriba = base - ascenso + 1;
            double abajo = base + descenso;
            Path2D.Double trazo = new Path2D.Double();
            trazo.moveTo(x + 1, base - (ascenso + descenso) * 0.35 + descenso);
            trazo.lineTo(x + signo * 0.35, abajo);
            trazo.lineTo(x + signo - 1, arriba);
            trazo.lineTo(x + ancho, arriba);
            g2.setColor(Tema.TEXTO);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(trazo);
            radicando.dibujar(g2, x + signo + 1, base);
        }
    }

    /** Contenido entre paréntesis o barras que crecen con su altura. */
    private static final class Delimitada extends Caja {
        private final Caja interior;
        private final boolean barras;
        private final double grosor;

        Delimitada(Caja interior, boolean barras) {
            this.interior = interior;
            this.barras = barras;
            this.grosor = barras ? 5 : 3 + (interior.ascenso + interior.descenso) * 0.12;
            this.ancho = interior.ancho + 2 * grosor + 2;
            this.ascenso = interior.ascenso + 2;
            this.descenso = interior.descenso + 2;
        }

        @Override
        void dibujar(Graphics2D g2, double x, double base) {
            double arriba = base - ascenso;
            double abajo = base + descenso;
            double medio = (arriba + abajo) / 2;
            g2.setColor(Tema.TEXTO_SECUNDARIO);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (barras) {
                g2.draw(new java.awt.geom.Line2D.Double(x + 2, arriba, x + 2, abajo));
                g2.draw(new java.awt.geom.Line2D.Double(x + ancho - 2, arriba, x + ancho - 2, abajo));
            } else {
                Path2D.Double izquierdo = new Path2D.Double();
                izquierdo.moveTo(x + grosor, arriba);
                izquierdo.quadTo(x - grosor * 0.2, medio, x + grosor, abajo);
                Path2D.Double derecho = new Path2D.Double();
                derecho.moveTo(x + ancho - grosor, arriba);
                derecho.quadTo(x + ancho + grosor * 0.2, medio, x + ancho - grosor, abajo);
                g2.draw(izquierdo);
                g2.draw(derecho);
            }
            interior.dibujar(g2, x + grosor + 1, base);
        }
    }

    // ==================================================================
    // Conversión del árbol de expresión a cajas
    // ==================================================================

    private static final class Constructor {
        private final Graphics2D g2;

        Constructor(Graphics2D g2) {
            this.g2 = g2;
        }

        Caja texto(String s, float tamano, boolean cursiva) {
            Font fuente = new Font(Font.SERIF, cursiva ? Font.ITALIC : Font.PLAIN, Math.round(tamano));
            FontMetrics fm = g2.getFontMetrics(fuente);
            Color color = cursiva ? Tema.PRIMARIO : Tema.TEXTO;
            return new Texto(s, fuente, color, fm);
        }

        Caja caja(Nodo n, float t) {
            if (n instanceof NodoConstante) {
                return texto(n.toString(), t, false);
            }
            if (n instanceof NodoVariable) {
                return texto(n.toString(), t, true);
            }
            if (n instanceof NodoSuma || n instanceof NodoResta) {
                Nodo izq = n instanceof NodoSuma ? ((NodoSuma) n).getIzquierdo() : ((NodoResta) n).getIzquierdo();
                Nodo der = n instanceof NodoSuma ? ((NodoSuma) n).getDerecho() : ((NodoResta) n).getDerecho();
                List<Caja> partes = new ArrayList<Caja>();
                partes.add(caja(izq, t));
                boolean resta = n instanceof NodoResta;
                Nodo derecho = der;
                if (!resta && der instanceof NodoNegativo) {
                    resta = true;
                    derecho = ((NodoNegativo) der).getArgumento();
                }
                partes.add(texto(resta ? " − " : " + ", t, false));
                boolean parentesis = resta && (derecho instanceof NodoSuma || derecho instanceof NodoResta);
                partes.add(parentesis ? new Delimitada(caja(derecho, t), false) : caja(derecho, t));
                return new Fila(partes);
            }
            if (n instanceof NodoNegativo) {
                Nodo arg = ((NodoNegativo) n).getArgumento();
                List<Caja> partes = new ArrayList<Caja>();
                partes.add(texto("−", t, false));
                boolean parentesis = arg instanceof NodoSuma || arg instanceof NodoResta;
                partes.add(parentesis ? new Delimitada(caja(arg, t), false) : caja(arg, t));
                return new Fila(partes);
            }
            if (n instanceof NodoProducto) {
                List<Nodo> factores = new ArrayList<Nodo>();
                aplanar(n, factores);
                List<Caja> partes = new ArrayList<Caja>();
                for (int i = 0; i < factores.size(); i++) {
                    Nodo f = factores.get(i);
                    if (i == 0 && f.esNumero(-1.0) && factores.size() > 1) {
                        partes.add(texto("−", t, false));
                        continue;
                    }
                    if (i > 0 && !(i == 1 && factores.get(0).esNumero(-1.0))) {
                        boolean pegado = factores.get(i - 1) instanceof NodoConstante && !(f instanceof NodoConstante);
                        partes.add(texto(pegado ? "" : "·", t, false));
                    }
                    boolean parentesis = f instanceof NodoSuma || f instanceof NodoResta || f instanceof NodoNegativo;
                    partes.add(parentesis ? new Delimitada(caja(f, t), false) : caja(f, t));
                }
                return new Fila(partes);
            }
            if (n instanceof NodoCociente) {
                float menor = Math.max(13f, t * 0.9f);
                Caja num = caja(((NodoCociente) n).getNumerador(), menor);
                Caja den = caja(((NodoCociente) n).getDenominador(), menor);
                return new Fraccion(num, den, t * 0.3);
            }
            if (n instanceof NodoPotencia) {
                Nodo base = ((NodoPotencia) n).getBase();
                Nodo exponente = ((NodoPotencia) n).getExponente();
                // x^(1/2) se dibuja como raíz cuadrada
                if (exponente.esNumero(0.5)) {
                    return new Raiz(caja(base, t));
                }
                Caja cajaBase = caja(base, t);
                boolean parentesis = !(base instanceof NodoVariable || (base instanceof NodoConstante
                        && base.toString().indexOf('/') < 0 && !base.toString().startsWith("-"))
                        || base instanceof NodoFuncion);
                if (parentesis) {
                    cajaBase = new Delimitada(cajaBase, false);
                }
                return new Potencia(cajaBase, caja(exponente, Math.max(12f, t * 0.68f)));
            }
            if (n instanceof NodoFuncion) {
                NodoFuncion f = (NodoFuncion) n;
                Nodo arg = f.getArgumento();
                if (f.getTipo() == TipoFuncion.EXP) {
                    return new Potencia(texto("e", t, false), caja(arg, Math.max(12f, t * 0.68f)));
                }
                if (f.getTipo() == TipoFuncion.RAIZ) {
                    return new Raiz(caja(arg, t));
                }
                if (f.getTipo() == TipoFuncion.ABS) {
                    return new Delimitada(caja(arg, t), true);
                }
                List<Caja> partes = new ArrayList<Caja>();
                partes.add(texto(f.getTipo().getNombre(), t, false));
                if (f.getTipo() == TipoFuncion.LN && arg instanceof NodoFuncion
                        && ((NodoFuncion) arg).getTipo() == TipoFuncion.ABS) {
                    partes.add(caja(arg, t));
                } else {
                    partes.add(new Delimitada(caja(arg, t), false));
                }
                return new Fila(partes);
            }
            return texto(n.toString(), t, false);
        }

        private static void aplanar(Nodo n, List<Nodo> factores) {
            if (n instanceof NodoProducto) {
                aplanar(((NodoProducto) n).getIzquierdo(), factores);
                aplanar(((NodoProducto) n).getDerecho(), factores);
            } else {
                factores.add(n);
            }
        }
    }
}
