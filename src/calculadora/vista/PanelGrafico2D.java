package calculadora.vista;

import calculadora.modelo.Expresion;
import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;
import calculadora.vista.estilo.Tema;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * Panel que dibuja funciones en un plano cartesiano usando Graphics2D.
 * <p>
 * Características:
 * </p>
 * <ul>
 *   <li>Cuadrícula, ejes y marcas numéricas que se adaptan al nivel de zoom.</li>
 *   <li>Trazado suavizado (antialiasing) de la función y de una curva secundaria
 *       (su derivada o su antiderivada).</li>
 *   <li>Sombreado semitransparente (AlphaComposite) del área bajo la curva
 *       en las integrales definidas.</li>
 *   <li>Zoom con la rueda del ratón (centrado en el cursor), arrastre para
 *       desplazar la vista y doble clic para restablecerla.</li>
 * </ul>
 * <p>
 * Conversión de coordenadas: el punto del "mundo" (centroX, centroY) se
 * dibuja en el centro del panel, y cada unidad mide {@code escala} píxeles.
 * </p>
 */
public class PanelGrafico2D extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final double ESCALA_INICIAL = 50.0;
    private static final double ESCALA_MINIMA = 0.5;
    private static final double ESCALA_MAXIMA = 20000.0;

    private static final Color COLOR_FONDO = Color.WHITE;
    private static final Color COLOR_CUADRICULA = new Color(236, 239, 245);
    private static final Color COLOR_EJES = new Color(100, 116, 139);
    private static final Color COLOR_FUNCION = Tema.GRAFICA_FUNCION;
    private static final Color COLOR_SECUNDARIA = Tema.GRAFICA_SECUNDARIA;
    private static final Color COLOR_AREA = Tema.GRAFICA_AREA;
    private static final Color COLOR_TEXTO = Tema.TEXTO_SECUNDARIO;

    // Datos que se grafican
    private transient Expresion funcion;
    private String etiquetaFuncion;
    private transient Expresion curvaSecundaria;
    private String etiquetaSecundaria;
    private Variable variable;
    private double valorOtraVariable;
    private Double limiteA;
    private Double limiteB;
    private Double puntoX;
    private Double puntoY;

    // Estado de la vista
    private double centroX;
    private double centroY;
    private double escala;
    private Point ultimoPuntoArrastre;

    /** Crea el panel vacío con la vista inicial y los oyentes del ratón. */
    public PanelGrafico2D() {
        this.variable = Variable.X;
        this.centroX = 0.0;
        this.centroY = 0.0;
        this.escala = ESCALA_INICIAL;
        setBackground(COLOR_FONDO);
        setPreferredSize(new Dimension(700, 450));
        setToolTipText("Rueda: zoom | Arrastrar: mover | Doble clic: restablecer vista");
        configurarRaton();
    }

    // ==================================================================
    // API pública
    // ==================================================================

    /**
     * Define las curvas a graficar.
     *
     * @param funcion            función principal f
     * @param etiquetaFuncion    texto de la leyenda para f
     * @param curvaSecundaria    derivada o antiderivada (puede ser null)
     * @param etiquetaSecundaria texto de la leyenda de la curva secundaria
     * @param variable           variable del eje horizontal
     * @param valorOtraVariable  valor fijo de la otra variable
     */
    public void graficar(Expresion funcion, String etiquetaFuncion, Expresion curvaSecundaria,
                         String etiquetaSecundaria, Variable variable, double valorOtraVariable) {
        this.funcion = funcion;
        this.etiquetaFuncion = etiquetaFuncion;
        this.curvaSecundaria = curvaSecundaria;
        this.etiquetaSecundaria = etiquetaSecundaria;
        this.variable = variable;
        this.valorOtraVariable = valorOtraVariable;
        this.limiteA = null;
        this.limiteB = null;
        this.puntoX = null;
        this.puntoY = null;
        // Cada gráfica nueva comienza con la vista inicial
        this.centroX = 0.0;
        this.centroY = 0.0;
        this.escala = ESCALA_INICIAL;
        repaint();
    }

    /**
     * Activa el sombreado del área entre a y b y centra la vista en el intervalo.
     *
     * @param a límite inferior
     * @param b límite superior
     */
    public void sombrearArea(double a, double b) {
        this.limiteA = Double.valueOf(Math.min(a, b));
        this.limiteB = Double.valueOf(Math.max(a, b));
        centrarEnIntervalo(limiteA.doubleValue(), limiteB.doubleValue());
        repaint();
    }

    /**
     * Marca un punto (por ejemplo, el resultado de una evaluación).
     *
     * @param x coordenada horizontal
     * @param y coordenada vertical
     */
    public void marcarPunto(double x, double y) {
        this.puntoX = Double.valueOf(x);
        this.puntoY = Double.valueOf(y);
        repaint();
    }

    /** Borra todas las curvas. */
    public void limpiar() {
        this.funcion = null;
        this.curvaSecundaria = null;
        this.limiteA = null;
        this.limiteB = null;
        this.puntoX = null;
        this.puntoY = null;
        repaint();
    }

    /** Vuelve al zoom y centro iniciales. */
    public void restablecerVista() {
        this.centroX = 0.0;
        this.centroY = 0.0;
        this.escala = ESCALA_INICIAL;
        if (limiteA != null && limiteB != null) {
            centrarEnIntervalo(limiteA.doubleValue(), limiteB.doubleValue());
        }
        repaint();
    }

    // ==================================================================
    // Conversión de coordenadas mundo <-> pantalla
    // ==================================================================

    private double aPantallaX(double xMundo) {
        return getWidth() / 2.0 + (xMundo - centroX) * escala;
    }

    private double aPantallaY(double yMundo) {
        return getHeight() / 2.0 - (yMundo - centroY) * escala;
    }

    private double aMundoX(double xPantalla) {
        return centroX + (xPantalla - getWidth() / 2.0) / escala;
    }

    private double aMundoY(double yPantalla) {
        return centroY - (yPantalla - getHeight() / 2.0) / escala;
    }

    /**
     * Ajusta la vista para que el intervalo [a, b] ocupe gran parte del ancho.
     */
    private void centrarEnIntervalo(double a, double b) {
        double ancho = Math.max(getWidth(), 600);
        double longitud = Math.max(b - a, 0.5);
        double nuevaEscala = (ancho * 0.6) / longitud;
        nuevaEscala = Math.max(ESCALA_MINIMA, Math.min(ESCALA_MAXIMA, nuevaEscala));
        this.escala = nuevaEscala;
        this.centroX = (a + b) / 2.0;
        // Centrar verticalmente en el valor medio de la función dentro del intervalo
        if (funcion != null) {
            double minimo = Double.POSITIVE_INFINITY;
            double maximo = Double.NEGATIVE_INFINITY;
            int muestras = 50;
            for (int i = 0; i <= muestras; i++) {
                double x = a + (b - a) * i / muestras;
                double y = funcion.evaluarEn(variable, x, valorOtraVariable);
                if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                    minimo = Math.min(minimo, Math.min(y, 0.0));
                    maximo = Math.max(maximo, Math.max(y, 0.0));
                }
            }
            if (minimo <= maximo) {
                this.centroY = (minimo + maximo) / 2.0;
                double alto = Math.max(getHeight(), 400);
                double rango = Math.max(maximo - minimo, 0.5);
                double escalaVertical = (alto * 0.7) / rango;
                this.escala = Math.max(ESCALA_MINIMA, Math.min(this.escala, escalaVertical));
            } else {
                this.centroY = 0.0;
            }
        }
    }

    // ==================================================================
    // Dibujo
    // ==================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        dibujarCuadricula(g2);
        if (funcion != null && limiteA != null && limiteB != null) {
            dibujarArea(g2);
        }
        dibujarEjes(g2);
        if (curvaSecundaria != null) {
            dibujarCurva(g2, curvaSecundaria, COLOR_SECUNDARIA, new BasicStroke(2.0f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{8.0f, 6.0f}, 0.0f));
        }
        if (funcion != null) {
            dibujarCurva(g2, funcion, COLOR_FUNCION, new BasicStroke(2.5f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            dibujarPunto(g2);
            dibujarLeyenda(g2);
        } else {
            dibujarMensajeVacio(g2);
        }
        g2.dispose();
    }

    /**
     * Calcula un paso "agradable" (1, 2 o 5 por una potencia de 10) para que
     * las líneas de la cuadrícula queden separadas al menos 60 píxeles.
     */
    private double calcularPasoCuadricula() {
        double pasoMinimo = 60.0 / escala;
        double potencia = Math.pow(10, Math.floor(Math.log10(pasoMinimo)));
        double[] multiplicadores = {1.0, 2.0, 5.0, 10.0};
        for (int i = 0; i < multiplicadores.length; i++) {
            double paso = multiplicadores[i] * potencia;
            if (paso >= pasoMinimo) {
                return paso;
            }
        }
        return 10.0 * potencia;
    }

    private void dibujarCuadricula(Graphics2D g2) {
        double paso = calcularPasoCuadricula();
        g2.setColor(COLOR_CUADRICULA);
        g2.setStroke(new BasicStroke(1.0f));

        double xInicio = Math.floor(aMundoX(0) / paso) * paso;
        double xFin = aMundoX(getWidth());
        for (double x = xInicio; x <= xFin; x = x + paso) {
            int px = (int) Math.round(aPantallaX(x));
            g2.drawLine(px, 0, px, getHeight());
        }
        double yInicio = Math.floor(aMundoY(getHeight()) / paso) * paso;
        double yFin = aMundoY(0);
        for (double y = yInicio; y <= yFin; y = y + paso) {
            int py = (int) Math.round(aPantallaY(y));
            g2.drawLine(0, py, getWidth(), py);
        }
    }

    private void dibujarEjes(Graphics2D g2) {
        double paso = calcularPasoCuadricula();
        int ejeX = (int) Math.round(aPantallaY(0));   // posición vertical del eje horizontal
        int ejeY = (int) Math.round(aPantallaX(0));   // posición horizontal del eje vertical

        // Si el eje queda fuera de la pantalla, las marcas se dibujan en el borde
        int ejeXVisible = Math.max(0, Math.min(getHeight() - 1, ejeX));
        int ejeYVisible = Math.max(0, Math.min(getWidth() - 1, ejeY));

        g2.setColor(COLOR_EJES);
        g2.setStroke(new BasicStroke(1.6f));
        if (ejeX >= 0 && ejeX < getHeight()) {
            g2.drawLine(0, ejeX, getWidth(), ejeX);
        }
        if (ejeY >= 0 && ejeY < getWidth()) {
            g2.drawLine(ejeY, 0, ejeY, getHeight());
        }

        // Marcas numéricas
        g2.setFont(Tema.texto(11.5f));
        FontMetrics metricas = g2.getFontMetrics();
        int decimales = Math.max(0, (int) Math.ceil(-Math.log10(paso)) + 1);

        double xInicio = Math.floor(aMundoX(0) / paso) * paso;
        double xFin = aMundoX(getWidth());
        for (double x = xInicio; x <= xFin; x = x + paso) {
            if (Math.abs(x) < paso / 2.0) {
                continue; // el cero se escribe una sola vez
            }
            int px = (int) Math.round(aPantallaX(x));
            g2.setColor(COLOR_EJES);
            g2.drawLine(px, ejeXVisible - 4, px, ejeXVisible + 4);
            String texto = FormatoNumeros.formatearDecimal(x, decimales);
            int anchoTexto = metricas.stringWidth(texto);
            int yTexto = ejeXVisible + 16;
            if (yTexto > getHeight() - 2) {
                yTexto = ejeXVisible - 8;
            }
            g2.setColor(COLOR_TEXTO);
            g2.drawString(texto, px - anchoTexto / 2, yTexto);
        }

        double yInicio = Math.floor(aMundoY(getHeight()) / paso) * paso;
        double yFin = aMundoY(0);
        for (double y = yInicio; y <= yFin; y = y + paso) {
            if (Math.abs(y) < paso / 2.0) {
                continue;
            }
            int py = (int) Math.round(aPantallaY(y));
            g2.setColor(COLOR_EJES);
            g2.drawLine(ejeYVisible - 4, py, ejeYVisible + 4, py);
            String texto = FormatoNumeros.formatearDecimal(y, decimales);
            int xTexto = ejeYVisible + 7;
            if (xTexto + metricas.stringWidth(texto) > getWidth()) {
                xTexto = ejeYVisible - 7 - metricas.stringWidth(texto);
            }
            g2.setColor(COLOR_TEXTO);
            g2.drawString(texto, xTexto, py + 4);
        }

        // Origen y nombres de los ejes
        g2.setColor(COLOR_TEXTO);
        if (ejeX >= 0 && ejeX < getHeight() && ejeY >= 0 && ejeY < getWidth()) {
            g2.drawString("0", ejeY + 5, ejeX + 15);
        }
        g2.setFont(Tema.negrita(13f));
        g2.drawString(variable.getSimbolo(), getWidth() - 16, ejeXVisible - 8);
        g2.drawString("f", ejeYVisible + 8, 16);
    }

    /**
     * Dibuja una curva evaluando la expresión en cada columna de píxeles.
     * La línea se interrumpe donde la función no está definida (NaN o infinito)
     * o donde hay un salto vertical enorme (asíntotas).
     */
    private void dibujarCurva(Graphics2D g2, Expresion expresion, Color color, BasicStroke trazo) {
        Path2D.Double camino = new Path2D.Double();
        boolean dibujando = false;
        double yAnteriorPantalla = 0.0;
        double limiteSalto = getHeight() * 3.0;

        for (int px = 0; px <= getWidth(); px++) {
            double xMundo = aMundoX(px);
            double yMundo = expresion.evaluarEn(variable, xMundo, valorOtraVariable);
            if (Double.isNaN(yMundo) || Double.isInfinite(yMundo)) {
                dibujando = false;
                continue;
            }
            double py = aPantallaY(yMundo);
            // Limitar valores enormes para evitar errores de dibujo
            py = Math.max(-limiteSalto, Math.min(getHeight() + limiteSalto, py));

            if (dibujando && Math.abs(py - yAnteriorPantalla) > getHeight()) {
                dibujando = false; // probable asíntota vertical
            }
            if (dibujando) {
                camino.lineTo(px, py);
            } else {
                camino.moveTo(px, py);
                dibujando = true;
            }
            yAnteriorPantalla = py;
        }
        g2.setColor(color);
        g2.setStroke(trazo);
        g2.draw(camino);
    }

    /**
     * Sombrea el área entre la curva y el eje horizontal en [a, b] con
     * transparencia (AlphaComposite).
     */
    private void dibujarArea(Graphics2D g2) {
        double a = limiteA.doubleValue();
        double b = limiteB.doubleValue();
        int pxInicio = (int) Math.floor(aPantallaX(a));
        int pxFin = (int) Math.ceil(aPantallaX(b));
        double yCero = aPantallaY(0);
        double limite = getHeight() * 3.0;

        Path2D.Double region = new Path2D.Double();
        region.moveTo(aPantallaX(a), yCero);
        for (int px = pxInicio; px <= pxFin; px++) {
            double xMundo = aMundoX(px);
            if (xMundo < a) {
                xMundo = a;
            }
            if (xMundo > b) {
                xMundo = b;
            }
            double yMundo = funcion.evaluarEn(variable, xMundo, valorOtraVariable);
            if (Double.isNaN(yMundo) || Double.isInfinite(yMundo)) {
                continue;
            }
            double py = Math.max(-limite, Math.min(getHeight() + limite, aPantallaY(yMundo)));
            region.lineTo(aPantallaX(xMundo), py);
        }
        region.lineTo(aPantallaX(b), yCero);
        region.closePath();

        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        g2.setColor(COLOR_AREA);
        g2.fill(region);
        g2.setComposite(original);

        // Líneas verticales en los límites a y b
        g2.setColor(COLOR_AREA.darker());
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
                new float[]{4.0f, 4.0f}, 0.0f));
        int xa = (int) Math.round(aPantallaX(a));
        int xb = (int) Math.round(aPantallaX(b));
        g2.drawLine(xa, 0, xa, getHeight());
        g2.drawLine(xb, 0, xb, getHeight());
        g2.setFont(Tema.negrita(12f));
        g2.drawString("a = " + FormatoNumeros.formatearDecimal(a, 4), xa + 4, getHeight() - 8);
        g2.drawString("b = " + FormatoNumeros.formatearDecimal(b, 4), xb + 4, getHeight() - 24);
    }

    private void dibujarPunto(Graphics2D g2) {
        if (puntoX == null || puntoY == null) {
            return;
        }
        double px = aPantallaX(puntoX.doubleValue());
        double py = aPantallaY(puntoY.doubleValue());
        g2.setColor(COLOR_SECUNDARIA);
        g2.fill(new Ellipse2D.Double(px - 5, py - 5, 10, 10));
        g2.setFont(Tema.negrita(12f));
        g2.drawString("(" + FormatoNumeros.formatearDecimal(puntoX.doubleValue(), 4) + ", "
                + FormatoNumeros.formatearDecimal(puntoY.doubleValue(), 4) + ")", (int) px + 8, (int) py - 8);
    }

    private void dibujarLeyenda(Graphics2D g2) {
        g2.setFont(Tema.texto(12.5f));
        FontMetrics metricas = g2.getFontMetrics();
        String linea1 = "f = " + etiquetaFuncion;
        String linea2 = null;
        if (curvaSecundaria != null && etiquetaSecundaria != null) {
            linea2 = etiquetaSecundaria;
        }
        String linea3 = null;
        if (funcion != null && (funcion.dependeDe(variable.otra())
                || (curvaSecundaria != null && curvaSecundaria.dependeDe(variable.otra())))) {
            linea3 = "(" + variable.otra().getSimbolo() + " fijo = " + FormatoNumeros.formatear(valorOtraVariable) + ")";
        }

        int ancho = metricas.stringWidth(linea1);
        int lineas = 1;
        if (linea2 != null) {
            ancho = Math.max(ancho, metricas.stringWidth(linea2));
            lineas++;
        }
        if (linea3 != null) {
            ancho = Math.max(ancho, metricas.stringWidth(linea3));
            lineas++;
        }
        int alto = lineas * 18 + 8;

        Composite original = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(10, 10, ancho + 44, alto, 10, 10);
        g2.setComposite(original);
        g2.setColor(COLOR_CUADRICULA.darker());
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(10, 10, ancho + 44, alto, 10, 10);

        int y = 28;
        g2.setColor(COLOR_FUNCION);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(18, y - 4, 38, y - 4);
        g2.setColor(COLOR_TEXTO);
        g2.drawString(linea1, 46, y);
        if (linea2 != null) {
            y = y + 18;
            g2.setColor(COLOR_SECUNDARIA);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
                    new float[]{5.0f, 3.0f}, 0.0f));
            g2.drawLine(18, y - 4, 38, y - 4);
            g2.setColor(COLOR_TEXTO);
            g2.drawString(linea2, 46, y);
        }
        if (linea3 != null) {
            y = y + 18;
            g2.setColor(COLOR_TEXTO);
            g2.drawString(linea3, 46, y);
        }
    }

    private void dibujarMensajeVacio(Graphics2D g2) {
        String mensaje = "Calcula algo para ver aquí la gráfica de la función";
        g2.setFont(Tema.texto(15f));
        FontMetrics metricas = g2.getFontMetrics();
        int x = (getWidth() - metricas.stringWidth(mensaje)) / 2;
        g2.setColor(COLOR_TEXTO);
        g2.drawString(mensaje, x, 30);
    }

    // ==================================================================
    // Interacción con el ratón
    // ==================================================================

    private void configurarRaton() {
        MouseAdapter oyente = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evento) {
                ultimoPuntoArrastre = evento.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent evento) {
                if (ultimoPuntoArrastre == null) {
                    ultimoPuntoArrastre = evento.getPoint();
                    return;
                }
                int dx = evento.getX() - ultimoPuntoArrastre.x;
                int dy = evento.getY() - ultimoPuntoArrastre.y;
                // Mover la vista en sentido contrario al arrastre
                centroX = centroX - dx / escala;
                centroY = centroY + dy / escala;
                ultimoPuntoArrastre = evento.getPoint();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent evento) {
                ultimoPuntoArrastre = null;
            }

            @Override
            public void mouseClicked(MouseEvent evento) {
                if (evento.getClickCount() == 2) {
                    restablecerVista();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent evento) {
                // Punto del mundo bajo el cursor ANTES del zoom
                double xMundo = aMundoX(evento.getX());
                double yMundo = aMundoY(evento.getY());

                double factor = 1.15;
                if (evento.getPreciseWheelRotation() > 0) {
                    escala = escala / factor; // alejar
                } else {
                    escala = escala * factor; // acercar
                }
                escala = Math.max(ESCALA_MINIMA, Math.min(ESCALA_MAXIMA, escala));

                // Recalcular el centro para que el punto bajo el cursor no se mueva
                centroX = xMundo - (evento.getX() - getWidth() / 2.0) / escala;
                centroY = yMundo + (evento.getY() - getHeight() / 2.0) / escala;
                repaint();
            }
        };
        addMouseListener(oyente);
        addMouseMotionListener(oyente);
        addMouseWheelListener(oyente);
    }
}
