package calculadora.vista;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.DominioInvalidoException;
import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.excepciones.OperacionNoSoportadaException;
import calculadora.ia.ConfiguracionIA;
import calculadora.ia.TutorIA;
import calculadora.modelo.Calculadora;
import calculadora.modelo.EntradaCalculo;
import calculadora.modelo.Graficable;
import calculadora.modelo.MetodoCalculo;
import calculadora.modelo.ResultadoCalculo;
import calculadora.modelo.Variable;
import calculadora.vista.componentes.BarraDesplazamientoModerna;
import calculadora.vista.componentes.BotonModerno;
import calculadora.vista.componentes.CampoModerno;
import calculadora.vista.componentes.Encabezado;
import calculadora.vista.componentes.GrupoSegmentado;
import calculadora.vista.componentes.PanelTarjeta;
import calculadora.vista.estilo.Iconos;
import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal de la calculadora (modo oscuro).
 * <pre>
 * +----------------------------------- Encabezado ------------------------------------+
 * | Lista de métodos  | Datos de entrada      | Tarjeta de resultado                    |
 * | (Derivadas /      | (campos que cambian   |-----------------------------------------|
 * |  Integrales)      |  según el método)     | [Procedimiento] [Gráfica 2D] [IA]       |
 * | Fórmula + ejemplo | Teclado matemático    | Contenido de la sección                 |
 * +-----------------------------------------------------------------------------------+
 * </pre>
 * Cada zona se construye en su propio método. La IA se consulta en un
 * {@link SwingWorker} para no congelar la ventana.
 */
public class VentanaCalculadora extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String SECCION_PASOS = "pasos";
    private static final String SECCION_GRAFICA = "grafica";
    private static final String SECCION_IA = "ia";

    /**
     * Teclas del teclado: {texto visible, texto a insertar, retroceso del cursor}.
     * El retroceso deja el cursor dentro de los paréntesis: sin(|).
     */
    private static final String[][] TECLAS = {
        {"sin", "sin()", "1"}, {"cos", "cos()", "1"}, {"tan", "tan()", "1"}, {"7", "7", "0"}, {"8", "8", "0"}, {"9", "9", "0"}, {"÷", "/", "0"},
        {"ln", "ln()", "1"}, {"e^", "e^()", "1"}, {"√", "√()", "1"}, {"4", "4", "0"}, {"5", "5", "0"}, {"6", "6", "0"}, {"×", "*", "0"},
        {"x²", "^2", "0"}, {"xⁿ", "^", "0"}, {"π", "pi", "0"}, {"1", "1", "0"}, {"2", "2", "0"}, {"3", "3", "0"}, {"−", "-", "0"},
        {"x", "x", "0"}, {"y", "y", "0"}, {"( )", "()", "1"}, {"0", "0", "0"}, {".", ".", "0"}, {"=", " = ", "0"}, {"+", "+", "0"},
        {"asin", "arcsin()", "1"}, {"atan", "arctan()", "1"}, {"1/x", "1/x", "0"}, {"∞", "∞", "0"}, {"−∞", "-∞", "0"},
        {"←", "", "0"}, {"C", "", "0"}
    };

    // Motor y tutor
    private final transient Calculadora calculadora;
    private transient TutorIA tutor;
    private transient ResultadoCalculo ultimoResultado;

    // Métodos
    private PanelMetodos panelMetodos;

    // Entrada
    private JLabel etiquetaFuncion;
    private CampoModerno campoExpresion;
    private JComponent filaVariable;
    private GrupoSegmentado selectorVariable;
    private JComponent filaOrden;
    private CampoModerno campoOrden;
    private JComponent filaPunto;
    private CampoModerno campoPunto;
    private JComponent filaTipoIntegral;
    private GrupoSegmentado selectorTipoIntegral;
    private JComponent filaLimites;
    private CampoModerno campoA;
    private CampoModerno campoB;
    private JComponent filaC;
    private CampoModerno campoC;
    private BotonModerno botonCalcular;
    private JTextComponent campoActivo;

    // Resultado
    private PanelTarjeta tarjetaResultado;
    private JLabel etiquetaTipoResultado;
    private JTextArea textoResultado;
    private BotonModerno botonCopiar;

    // Secciones
    private GrupoSegmentado navegacion;
    private CardLayout tarjetasSecciones;
    private JPanel contenedorSecciones;
    private JEditorPane vistaPasos;
    private PanelGrafico2D panelGrafico;
    private JEditorPane vistaExplicacion;
    private CampoModerno campoPregunta;
    private BotonModerno botonExplicar;
    private JLabel etiquetaProveedor;
    private Timer animacionCarga;

    // Estado
    private Encabezado.Insignia insigniaIA;
    private JLabel barraEstado;

    /** Construye y organiza la ventana. */
    public VentanaCalculadora() {
        super("Calculadora de Cálculo con Asistente de IA");
        this.calculadora = new Calculadora();
        this.tutor = new TutorIA(ConfiguracionIA.cargar());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setIconImages(Iconos.iconosVentana());

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.add(crearEncabezado(), BorderLayout.NORTH);

        JPanel cuerpo = new JPanel(new BorderLayout(16, 0));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));
        panelMetodos = new PanelMetodos();
        cuerpo.add(panelMetodos, BorderLayout.WEST);

        JPanel derecha = new JPanel(new BorderLayout(16, 0));
        derecha.setOpaque(false);
        derecha.add(crearColumnaEntrada(), BorderLayout.WEST);
        derecha.add(crearColumnaResultados(), BorderLayout.CENTER);
        cuerpo.add(derecha, BorderLayout.CENTER);

        raiz.add(cuerpo, BorderLayout.CENTER);
        raiz.add(crearBarraEstado(), BorderLayout.SOUTH);
        setContentPane(raiz);

        panelMetodos.alCambiarMetodo(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarCamposSegunMetodo();
            }
        });
        panelMetodos.alCargarEjemplo(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarEjemplo();
            }
        });

        actualizarCamposSegunMetodo();
        actualizarInsigniaIA();
        mostrarEstadoInicial();
        campoActivo = campoExpresion;

        setMinimumSize(new Dimension(1180, 720));
        ajustarTamanoInicial();
        getRootPane().setDefaultButton(botonCalcular);
    }

    /** Abre la ventana con un tamaño cómodo (maximizada en pantallas pequeñas). */
    private void ajustarTamanoInicial() {
        Rectangle pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int ancho = Math.min(1500, pantalla.width);
        int alto = Math.min(920, pantalla.height);
        setSize(ancho, alto);
        setLocationRelativeTo(null);
        if (pantalla.width < 1500 || pantalla.height < 900) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    // ==================================================================
    // ENCABEZADO
    // ==================================================================

    private JComponent crearEncabezado() {
        Encabezado encabezado = new Encabezado("Calculadora de Cálculo",
                "Derivadas, límites e integrales paso a paso  ·  Programación II");
        insigniaIA = new Encabezado.Insignia();
        encabezado.agregarDerecha(insigniaIA);
        BotonModerno botonAyuda = new BotonModerno("Ayuda", BotonModerno.Estilo.SOBRE_COLOR);
        botonAyuda.setFocusable(false);
        botonAyuda.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarAyuda();
            }
        });
        encabezado.agregarDerecha(botonAyuda);
        return encabezado;
    }

    // ==================================================================
    // COLUMNA DE ENTRADA: datos y teclado
    // ==================================================================

    private JComponent crearColumnaEntrada() {
        JPanel columna = new PanelQueSigueAncho();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.add(crearTarjetaEntrada());
        columna.add(Box.createVerticalStrut(12));
        columna.add(crearTarjetaTeclado());
        columna.add(Box.createVerticalGlue());

        JScrollPane desplazamiento = new JScrollPane(columna,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        BarraDesplazamientoModerna.aplicar(desplazamiento);
        desplazamiento.setPreferredSize(new Dimension(410, 400));
        return desplazamiento;
    }

    /**
     * Tarjeta con los datos. Las filas se muestran u ocultan según el método:
     * variable (derivadas), orden n, punto del límite, tipo de integral,
     * límites a y b, y punto intermedio c.
     */
    private JComponent crearTarjetaEntrada() {
        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));

        etiquetaFuncion = PanelTarjeta.crearTitulo("Función f(x)");
        contenido.add(alinear(etiquetaFuncion));
        contenido.add(Box.createVerticalStrut(6));
        campoExpresion = new CampoModerno("x^2·sin(x)", 20);
        campoExpresion.setFont(Tema.mono(20f));
        campoExpresion.setRelleno(11, 14);
        campoExpresion.setTextoAyuda("Ej.: x^2·sin(x)");
        registrarCampo(campoExpresion);
        contenido.add(alinear(campoExpresion));
        contenido.add(Box.createVerticalStrut(14));

        // Variable
        selectorVariable = new GrupoSegmentado("x", "y");
        selectorVariable.setFont(Tema.mono(15f));
        selectorVariable.setOcuparAncho(false);
        filaVariable = fila("Derivar respecto a", selectorVariable,
                "La otra variable se trata como constante (derivada parcial)");
        contenido.add(filaVariable);

        // Orden
        campoOrden = crearCampoNumero("2");
        filaOrden = fila("Orden de la derivada (n)", campoOrden, "Número entero de 1 a 6");
        contenido.add(filaOrden);

        // Punto del límite
        campoPunto = crearCampoNumero("0");
        filaPunto = fila("x tiende a", campoPunto, "Un número, pi, ∞ o -∞ (use el teclado)");
        contenido.add(filaPunto);

        // Tipo de integral
        selectorTipoIntegral = new GrupoSegmentado("Indefinida", "Definida [a, b]");
        selectorTipoIntegral.setFont(Tema.negrita(13f));
        selectorTipoIntegral.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarCamposSegunMetodo();
            }
        });
        filaTipoIntegral = fila("Tipo de integral", selectorTipoIntegral, null);
        contenido.add(filaTipoIntegral);

        // Límites a y b
        JPanel limites = new JPanel(new GridLayout(2, 2, 10, 6));
        limites.setOpaque(false);
        limites.add(PanelTarjeta.crearTitulo("Límite inferior a"));
        limites.add(PanelTarjeta.crearTitulo("Límite superior b"));
        campoA = crearCampoNumero("0");
        campoB = crearCampoNumero("1");
        limites.add(campoA);
        limites.add(campoB);
        filaLimites = envolverFila(limites, "Admiten números, fracciones, pi, ∞ y -∞");
        contenido.add(filaLimites);

        // Punto intermedio c
        campoC = crearCampoNumero("1");
        filaC = fila("Punto intermedio c", campoC, "a < c < b");
        contenido.add(filaC);

        // Botones
        JPanel botones = new JPanel(new BorderLayout(10, 0));
        botones.setOpaque(false);
        botonCalcular = new BotonModerno("Calcular", BotonModerno.Estilo.PRIMARIO);
        botonCalcular.setFont(Tema.negrita(15f));
        botonCalcular.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcular();
            }
        });
        BotonModerno botonLimpiar = new BotonModerno("Limpiar", BotonModerno.Estilo.SECUNDARIO);
        botonLimpiar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                limpiar();
            }
        });
        botones.add(botonCalcular, BorderLayout.CENTER);
        botones.add(botonLimpiar, BorderLayout.EAST);
        contenido.add(Box.createVerticalStrut(4));
        contenido.add(alinear(botones));

        PanelTarjeta tarjeta = new PanelTarjeta(new BorderLayout());
        tarjeta.add(contenido, BorderLayout.CENTER);
        return limitarAltura(tarjeta);
    }

    /** Fila con título, componente y ayuda opcional. */
    private JComponent fila(String titulo, JComponent componente, String ayuda) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.add(PanelTarjeta.crearTitulo(titulo), BorderLayout.NORTH);
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.setOpaque(false);
        if (componente instanceof GrupoSegmentado && !(componente == selectorTipoIntegral)) {
            envoltura.add(componente, BorderLayout.WEST);
        } else {
            envoltura.add(componente, BorderLayout.CENTER);
        }
        panel.add(envoltura, BorderLayout.CENTER);
        return envolverFila(panel, ayuda);
    }

    private JComponent envolverFila(JComponent panel, String ayuda) {
        JPanel fila = new JPanel(new BorderLayout(0, 4)) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        fila.setOpaque(false);
        fila.setAlignmentX(0.0f);
        fila.add(panel, BorderLayout.CENTER);
        if (ayuda != null) {
            JLabel etiqueta = new JLabel(ayuda);
            etiqueta.setFont(Tema.texto(11.5f));
            etiqueta.setForeground(Tema.TEXTO_TENUE);
            fila.add(etiqueta, BorderLayout.SOUTH);
        }
        fila.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        return fila;
    }

    private JComponent alinear(JComponent componente) {
        componente.setAlignmentX(0.0f);
        if (componente instanceof JPanel || componente instanceof CampoModerno) {
            final JComponent c = componente;
            JPanel envoltura = new JPanel(new BorderLayout()) {
                private static final long serialVersionUID = 1L;

                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height);
                }
            };
            envoltura.setOpaque(false);
            envoltura.setAlignmentX(0.0f);
            envoltura.add(componente, BorderLayout.CENTER);
            return envoltura;
        }
        return componente;
    }

    private CampoModerno crearCampoNumero(String valor) {
        CampoModerno campo = new CampoModerno(valor, 6);
        campo.setFont(Tema.mono(15f));
        registrarCampo(campo);
        return campo;
    }

    /** Recuerda el último campo con el cursor, para que el teclado escriba en él. */
    private void registrarCampo(final JTextComponent campo) {
        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                campoActivo = campo;
            }
        });
    }

    /**
     * Teclado matemático: números, operaciones, funciones e infinito.
     * Las teclas NO toman el foco, así el cursor se queda en el campo activo.
     */
    private JComponent crearTarjetaTeclado() {
        JPanel rejilla = new JPanel(new GridLayout(5, 7, 6, 6));
        rejilla.setOpaque(false);
        for (int i = 0; i < TECLAS.length; i++) {
            final String visible = TECLAS[i][0];
            final String insertar = TECLAS[i][1];
            final int retroceso = Integer.parseInt(TECLAS[i][2]);
            BotonModerno tecla = new BotonModerno(visible, BotonModerno.Estilo.TECLA);
            tecla.setFocusable(false);
            if (visible.length() > 2) {
                tecla.setFont(Tema.mono(visible.length() > 4 ? 11.5f : 13.5f));
            }
            if (Character.isDigit(visible.charAt(0)) || visible.equals(".")) {
                tecla.setFont(Tema.negrita(15f));
            }
            if (visible.equals("C") || visible.equals("←")) {
                tecla.setForeground(Tema.ERROR);
            } else if (visible.contains("∞")) {
                tecla.setForeground(Tema.SECUNDARIO);
                tecla.setFont(Tema.negrita(15f));
            } else if (!Character.isDigit(visible.charAt(0)) && !visible.equals(".")) {
                tecla.setForeground(Tema.PRIMARIO);
            }
            tecla.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pulsarTecla(visible, insertar, retroceso);
                }
            });
            rejilla.add(tecla);
        }
        JLabel ayuda = new JLabel("Escribe en el campo que tenga el cursor (función o límites)");
        ayuda.setFont(Tema.texto(11.5f));
        ayuda.setForeground(Tema.TEXTO_TENUE);
        JPanel contenido = new JPanel(new BorderLayout(0, 8));
        contenido.setOpaque(false);
        contenido.add(rejilla, BorderLayout.CENTER);
        contenido.add(ayuda, BorderLayout.SOUTH);
        return limitarAltura(PanelTarjeta.conTitulo("Teclado matemático", contenido));
    }

    /** Evita que BoxLayout estire verticalmente las tarjetas. */
    private JComponent limitarAltura(final JComponent componente) {
        componente.setAlignmentX(0.0f);
        JPanel envoltura = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        envoltura.setOpaque(false);
        envoltura.setAlignmentX(0.0f);
        envoltura.add(componente, BorderLayout.CENTER);
        return envoltura;
    }

    // ==================================================================
    // COLUMNA DE RESULTADOS
    // ==================================================================

    private JComponent crearColumnaResultados() {
        JPanel columna = new JPanel(new BorderLayout(0, 12));
        columna.setOpaque(false);
        columna.add(crearTarjetaResultado(), BorderLayout.NORTH);
        columna.add(crearZonaSecciones(), BorderLayout.CENTER);
        return columna;
    }

    private JComponent crearTarjetaResultado() {
        tarjetaResultado = new PanelTarjeta(new BorderLayout(0, 6));
        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);
        etiquetaTipoResultado = PanelTarjeta.crearTitulo("Resultado");
        superior.add(etiquetaTipoResultado, BorderLayout.WEST);
        botonCopiar = new BotonModerno("Copiar", BotonModerno.Estilo.FANTASMA);
        botonCopiar.setFont(Tema.negrita(12f));
        botonCopiar.setFocusable(false);
        botonCopiar.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        botonCopiar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copiarResultado();
            }
        });
        superior.add(botonCopiar, BorderLayout.EAST);
        tarjetaResultado.add(superior, BorderLayout.NORTH);

        textoResultado = new JTextArea();
        textoResultado.setEditable(false);
        textoResultado.setLineWrap(true);
        textoResultado.setWrapStyleWord(true);
        textoResultado.setOpaque(false);
        textoResultado.setBorder(null);
        textoResultado.setFont(Tema.mono(21f));
        textoResultado.setForeground(Tema.TEXTO);
        textoResultado.setSelectionColor(new Color(21, 94, 117));
        tarjetaResultado.add(textoResultado, BorderLayout.CENTER);
        return tarjetaResultado;
    }

    private JComponent crearZonaSecciones() {
        PanelTarjeta tarjeta = new PanelTarjeta(new BorderLayout(0, 12));
        navegacion = new GrupoSegmentado("Procedimiento", "Gráfica 2D", "Asistente IA");
        navegacion.setOcuparAncho(false);
        navegacion.setFont(Tema.negrita(13.5f));
        navegacion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarSeccion(navegacion.getSeleccionado());
            }
        });
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barra.setOpaque(false);
        barra.add(navegacion);
        tarjeta.add(barra, BorderLayout.NORTH);

        tarjetasSecciones = new CardLayout();
        contenedorSecciones = new JPanel(tarjetasSecciones);
        contenedorSecciones.setOpaque(false);
        contenedorSecciones.add(crearSeccionPasos(), SECCION_PASOS);
        contenedorSecciones.add(crearSeccionGrafica(), SECCION_GRAFICA);
        contenedorSecciones.add(crearSeccionAsistente(), SECCION_IA);
        tarjeta.add(contenedorSecciones, BorderLayout.CENTER);
        return tarjeta;
    }

    private JComponent crearSeccionPasos() {
        vistaPasos = crearVisorHtml();
        JScrollPane desplazamiento = new JScrollPane(vistaPasos);
        BarraDesplazamientoModerna.aplicar(desplazamiento);
        return desplazamiento;
    }

    private JComponent crearSeccionGrafica() {
        JPanel seccion = new JPanel(new BorderLayout(0, 8));
        seccion.setOpaque(false);
        panelGrafico = new PanelGrafico2D();
        JPanel marco = new JPanel(new BorderLayout());
        marco.setBackground(Tema.GRAFICA_FONDO);
        marco.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        marco.add(panelGrafico, BorderLayout.CENTER);
        seccion.add(marco, BorderLayout.CENTER);

        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        JLabel ayuda = new JLabel("Rueda: zoom   ·   Arrastrar: mover   ·   Doble clic: restablecer");
        ayuda.setFont(Tema.texto(12.5f));
        ayuda.setForeground(Tema.TEXTO_SECUNDARIO);
        barra.add(ayuda, BorderLayout.WEST);
        BotonModerno botonRestablecer = new BotonModerno("Restablecer vista", BotonModerno.Estilo.FANTASMA);
        botonRestablecer.setFocusable(false);
        botonRestablecer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panelGrafico.restablecerVista();
            }
        });
        barra.add(botonRestablecer, BorderLayout.EAST);
        seccion.add(barra, BorderLayout.SOUTH);
        return seccion;
    }

    private JComponent crearSeccionAsistente() {
        JPanel seccion = new JPanel(new BorderLayout(0, 10));
        seccion.setOpaque(false);
        etiquetaProveedor = new JLabel();
        etiquetaProveedor.setFont(Tema.texto(13f));
        etiquetaProveedor.setForeground(Tema.TEXTO_SECUNDARIO);
        seccion.add(etiquetaProveedor, BorderLayout.NORTH);

        vistaExplicacion = crearVisorHtml();
        JScrollPane desplazamiento = new JScrollPane(vistaExplicacion);
        BarraDesplazamientoModerna.aplicar(desplazamiento);
        seccion.add(desplazamiento, BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout(10, 0));
        inferior.setOpaque(false);
        campoPregunta = new CampoModerno("", 20);
        campoPregunta.setTextoAyuda("Pregunta algo sobre el procedimiento (opcional)...");
        campoPregunta.setRelleno(10, 14);
        campoPregunta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                explicarConIA();
            }
        });
        inferior.add(campoPregunta, BorderLayout.CENTER);
        botonExplicar = new BotonModerno("Explicar con IA", BotonModerno.Estilo.PRIMARIO);
        botonExplicar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                explicarConIA();
            }
        });
        inferior.add(botonExplicar, BorderLayout.EAST);
        seccion.add(inferior, BorderLayout.SOUTH);
        return seccion;
    }

    private JEditorPane crearVisorHtml() {
        JEditorPane visor = new JEditorPane();
        visor.setContentType("text/html");
        visor.setEditable(false);
        visor.setOpaque(false);
        visor.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        visor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        visor.setFont(Tema.texto(14f));
        visor.setForeground(Tema.TEXTO);
        return visor;
    }

    private JComponent crearBarraEstado() {
        barraEstado = new JLabel("Listo.");
        barraEstado.setFont(Tema.texto(12.5f));
        barraEstado.setForeground(Tema.TEXTO_SECUNDARIO);
        barraEstado.setBorder(BorderFactory.createEmptyBorder(4, 20, 8, 20));
        return barraEstado;
    }

    // ==================================================================
    // ACCIONES
    // ==================================================================

    private void mostrarSeccion(int indice) {
        if (indice == 0) {
            tarjetasSecciones.show(contenedorSecciones, SECCION_PASOS);
        } else if (indice == 1) {
            tarjetasSecciones.show(contenedorSecciones, SECCION_GRAFICA);
        } else {
            tarjetasSecciones.show(contenedorSecciones, SECCION_IA);
        }
    }

    /** Muestra solo los campos que necesita el método elegido. */
    private void actualizarCamposSegunMetodo() {
        MetodoCalculo metodo = panelMetodos.getSeleccionado();
        boolean definida = metodo.esDefinidaObligatoria()
                || (metodo.usaTipoIntegral() && selectorTipoIntegral.getSeleccionado() == 1);

        if (metodo.getOperacion() == MetodoCalculo.Operacion.IMPLICITA) {
            etiquetaFuncion.setText("ECUACIÓN EN x E y (USE =)");
        } else if (metodo.getOperacion() == MetodoCalculo.Operacion.LIMITE) {
            etiquetaFuncion.setText("FUNCIÓN f(x) DEL LÍMITE");
        } else if (metodo.getCategoria() == MetodoCalculo.Categoria.INTEGRALES) {
            etiquetaFuncion.setText("INTEGRANDO f(x)");
        } else {
            etiquetaFuncion.setText("FUNCIÓN f(x, y)");
        }
        filaVariable.setVisible(metodo.usaVariable());
        filaOrden.setVisible(metodo.usaOrden());
        filaPunto.setVisible(metodo.usaPunto());
        filaTipoIntegral.setVisible(metodo.usaTipoIntegral());
        filaLimites.setVisible(definida);
        filaC.setVisible(metodo.usaPuntoIntermedio());
        revalidate();
        repaint();
        mostrarEstado("Método: " + metodo.getNombre() + ". Escribe la función o pulsa \"Cargar ejemplo\".",
                Tema.TEXTO_SECUNDARIO);
    }

    /** Copia el ejemplo del método en los campos y calcula. */
    private void cargarEjemplo() {
        MetodoCalculo metodo = panelMetodos.getSeleccionado();
        campoExpresion.setText(metodo.getEjemplo());
        campoA.setText(metodo.ejemploA());
        campoB.setText(metodo.ejemploB());
        campoC.setText("1");
        campoOrden.setText("3");
        campoPunto.setText("0");
        if (metodo == MetodoCalculo.D_PARCIAL) {
            selectorVariable.setSeleccionado(1);
        } else {
            selectorVariable.setSeleccionado(0);
        }
        if (metodo.usaTipoIntegral()) {
            selectorTipoIntegral.setSeleccionado(0);
        }
        actualizarCamposSegunMetodo();
        calcular();
    }

    /** Inserta el texto de la tecla en el campo activo. */
    private void pulsarTecla(String visible, String insertar, int retroceso) {
        JTextComponent campo = campoActivo != null ? campoActivo : campoExpresion;
        if (!campo.isShowing()) {
            campo = campoExpresion;
        }
        if (visible.equals("C")) {
            campo.setText("");
        } else if (visible.equals("←")) {
            int posicion = campo.getCaretPosition();
            String texto = campo.getText();
            if (campo.getSelectedText() != null) {
                campo.replaceSelection("");
            } else if (posicion > 0) {
                campo.setText(texto.substring(0, posicion - 1) + texto.substring(posicion));
                campo.setCaretPosition(posicion - 1);
            }
        } else if (visible.contains("∞") && campo != campoExpresion) {
            // En un límite, ∞ reemplaza todo el contenido
            campo.setText(insertar);
        } else {
            campo.replaceSelection(insertar);
            int posicion = campo.getCaretPosition() - retroceso;
            campo.setCaretPosition(Math.max(0, posicion));
        }
        campo.requestFocusInWindow();
    }

    /** Ejecuta el método seleccionado. */
    private void calcular() {
        MetodoCalculo metodo = panelMetodos.getSeleccionado();
        EntradaCalculo entrada = new EntradaCalculo();
        entrada.setExpresion(campoExpresion.getText());
        entrada.setVariable(selectorVariable.getSeleccionado() == 1 ? Variable.Y : Variable.X);
        entrada.setOrden(campoOrden.getText());
        entrada.setPunto(campoPunto.getText());
        entrada.setLimiteA(campoA.getText());
        entrada.setLimiteB(campoB.getText());
        entrada.setPuntoC(campoC.getText());
        entrada.setDefinida(metodo.esDefinidaObligatoria()
                || (metodo.usaTipoIntegral() && selectorTipoIntegral.getSeleccionado() == 1));
        try {
            ResultadoCalculo resultado = calculadora.resolver(metodo, entrada);
            mostrarResultado(resultado);
        } catch (ExpresionInvalidaException e) {
            mostrarError("Expresión no válida", e.getMessage()
                    + ".  Ejemplos válidos: x^2·sin(x),  sin(x)/x,  e^(3x),  (3x + 5)/(x^2 + x - 2),  √(9 - x^2)");
        } catch (OperacionNoSoportadaException e) {
            mostrarError("Método no aplicable", e.getMessage());
        } catch (DominioInvalidoException e) {
            mostrarError("Fuera del dominio", e.getMessage());
        } catch (CalculadoraException e) {
            mostrarError("No se pudo calcular", e.getMessage());
        } catch (RuntimeException e) {
            mostrarError("Error inesperado", String.valueOf(e.getMessage()));
        }
    }

    private void mostrarResultado(ResultadoCalculo resultado) {
        ultimoResultado = resultado;
        tarjetaResultado.setColorFondo(Tema.SUPERFICIE);
        etiquetaTipoResultado.setForeground(Tema.PRIMARIO);
        etiquetaTipoResultado.setText(resultado.getTipo().getNombre().toUpperCase() + "  ·  "
                + resultado.getNombreMetodo().toUpperCase());
        textoResultado.setFont(Tema.mono(21f));
        textoResultado.setForeground(Tema.TEXTO);
        textoResultado.setText(resultado.getTextoResultado());
        botonCopiar.setEnabled(true);

        vistaPasos.setText(RenderizadorHtml.pasos(resultado));
        vistaPasos.setCaretPosition(0);
        actualizarGrafica(resultado);

        vistaExplicacion.setText(RenderizadorHtml.estadoVacio("Cálculo listo",
                "Pulsa \"Explicar con IA\" para recibir la explicación del procedimiento."));
        Color color = resultado.getNotas().isEmpty() ? Tema.EXITO : Tema.ADVERTENCIA;
        String estado = resultado.getNotas().isEmpty() ? "Cálculo realizado correctamente (" + resultado.getNombreMetodo() + ")."
                : "Calculado. Revisa la nota en el procedimiento.";
        mostrarEstado(estado, color);
    }

    private void actualizarGrafica(ResultadoCalculo resultado) {
        Graficable funcion = resultado.getExpresion();
        Variable variable = resultado.getVariable() == null ? Variable.X : resultado.getVariable();
        ResultadoCalculo.TipoOperacion tipo = resultado.getTipo();
        if (tipo == ResultadoCalculo.TipoOperacion.DERIVADA_IMPLICITA) {
            panelGrafico.graficarImplicita(resultado.getCurvaImplicita(), resultado.getTextoOriginal());
            return;
        }
        if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA) {
            panelGrafico.graficar(funcion, funcion.toString(), null, null, Variable.X, 1.0);
            panelGrafico.sombrearArea(resultado.getLimiteInferior().doubleValue(),
                    resultado.getLimiteSuperior().doubleValue());
            return;
        }
        if (tipo == ResultadoCalculo.TipoOperacion.LIMITE) {
            panelGrafico.graficar(funcion, funcion.toString(), null, null, Variable.X, 1.0);
            if (resultado.getPuntoX() != null) {
                panelGrafico.marcarPunto(resultado.getPuntoX().doubleValue(), resultado.getPuntoY().doubleValue());
            }
            return;
        }
        panelGrafico.graficar(funcion, funcion.toString(), resultado.getExpresionResultado(),
                resultado.getEtiquetaSecundaria(), variable, 1.0);
    }

    /** Muestra el error dentro de la tarjeta de resultado. */
    private void mostrarError(String titulo, String mensaje) {
        tarjetaResultado.setColorFondo(Tema.ERROR_SUAVE);
        etiquetaTipoResultado.setForeground(Tema.ERROR);
        etiquetaTipoResultado.setText(titulo.toUpperCase());
        textoResultado.setFont(Tema.texto(15f));
        textoResultado.setForeground(new Color(254, 202, 202));
        textoResultado.setText(mensaje);
        botonCopiar.setEnabled(false);
        ultimoResultado = null;
        vistaPasos.setText(RenderizadorHtml.estadoVacio("No se pudo completar el cálculo",
                "Corrige la función o los datos y vuelve a pulsar Calcular."));
        mostrarEstado(titulo + ".", Tema.ERROR);
    }

    private void mostrarEstadoInicial() {
        tarjetaResultado.setColorFondo(Tema.SUPERFICIE);
        etiquetaTipoResultado.setForeground(Tema.TEXTO_SECUNDARIO);
        etiquetaTipoResultado.setText("RESULTADO");
        textoResultado.setFont(Tema.texto(16f));
        textoResultado.setForeground(Tema.TEXTO_TENUE);
        textoResultado.setText("Elige un método en la lista, escribe la función y pulsa Calcular.");
        botonCopiar.setEnabled(false);
        vistaPasos.setText(RenderizadorHtml.estadoVacio("Aquí verás el procedimiento paso a paso",
                "Los pasos del método elegido aparecen resaltados en cian."));
        vistaExplicacion.setText(RenderizadorHtml.estadoVacio("Asistente de IA",
                "Realiza un cálculo y pulsa \"Explicar con IA\". La IA no calcula: explica los pasos del motor."));
    }

    /** Solicita la explicación al tutor en segundo plano (SwingWorker). */
    private void explicarConIA() {
        if (ultimoResultado == null) {
            vistaExplicacion.setText(RenderizadorHtml.estadoVacio("Primero realiza un cálculo",
                    "Elige un método, escribe la función y pulsa Calcular."));
            return;
        }
        final ResultadoCalculo resultado = ultimoResultado;
        final String pregunta = campoPregunta.getText();
        final TutorIA tutorActual = tutor;

        botonExplicar.setEnabled(false);
        vistaExplicacion.setText(RenderizadorHtml.cargando("Generando la explicación..."));
        iniciarAnimacionCarga();
        mostrarEstado("Consultando: " + tutorActual.describirProveedor(), Tema.TEXTO_SECUNDARIO);

        SwingWorker<String, Void> trabajador = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return tutorActual.explicar(resultado, pregunta);
            }

            @Override
            protected void done() {
                detenerAnimacionCarga();
                try {
                    String explicacion = get();
                    vistaExplicacion.setText(RenderizadorHtml.explicacion(explicacion, tutorActual.getUltimaFuente()));
                    vistaExplicacion.setCaretPosition(0);
                    Color color = tutorActual.getUltimaFuente().startsWith("Explicador Local") ? Tema.ADVERTENCIA
                            : Tema.EXITO;
                    mostrarEstado("Explicación generada por: " + tutorActual.getUltimaFuente(), color);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    vistaExplicacion.setText(RenderizadorHtml.estadoVacio("No se pudo generar la explicación",
                            String.valueOf(e.getCause().getMessage())));
                } finally {
                    botonExplicar.setEnabled(true);
                }
            }
        };
        trabajador.execute();
    }

    private void iniciarAnimacionCarga() {
        final String[] cuadros = {"Pensando   ", "Pensando.  ", "Pensando.. ", "Pensando..."};
        final int[] indice = {0};
        animacionCarga = new Timer(350, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                botonExplicar.setText(cuadros[indice[0] % cuadros.length]);
                indice[0]++;
            }
        });
        animacionCarga.start();
    }

    private void detenerAnimacionCarga() {
        if (animacionCarga != null) {
            animacionCarga.stop();
        }
        botonExplicar.setText("Explicar con IA");
    }

    private void actualizarInsigniaIA() {
        if (tutor.tieneProveedorRemoto()) {
            ConfiguracionIA configuracion = tutor.getConfiguracion();
            String nombre = configuracion.getProveedor();
            if (nombre.equals(ConfiguracionIA.PROVEEDOR_ANTHROPIC)) {
                nombre = "Claude";
            } else if (nombre.equals(ConfiguracionIA.PROVEEDOR_OPENAI)) {
                nombre = "OpenAI";
            } else if (nombre.equals(ConfiguracionIA.PROVEEDOR_OLLAMA)) {
                nombre = "Ollama";
            }
            insigniaIA.actualizar("IA: " + nombre + " · " + configuracion.getModeloEfectivo(), Tema.EXITO);
            etiquetaProveedor.setText("Proveedor: " + tutor.describirProveedor());
        } else {
            insigniaIA.actualizar("IA: Explicador local", Tema.ADVERTENCIA);
            etiquetaProveedor.setText("Proveedor: Explicador Local (sin conexión). La IA en línea se configura en "
                    + "ConfiguracionIA.java.");
        }
    }

    private void copiarResultado() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textoResultado.getText()), null);
        mostrarEstado("Resultado copiado al portapapeles.", Tema.EXITO);
    }

    private void limpiar() {
        campoExpresion.setText("");
        campoPregunta.setText("");
        panelGrafico.limpiar();
        ultimoResultado = null;
        mostrarEstadoInicial();
        mostrarEstado("Listo.", Tema.TEXTO_SECUNDARIO);
        campoExpresion.requestFocusInWindow();
    }

    /** Muestra la ayuda de sintaxis en la sección de procedimiento. */
    private void mostrarAyuda() {
        String c = Tema.hex(Tema.PRIMARIO);
        String m = Tema.familiaMono();
        String html = "<html><body style='font-family:" + Tema.familiaTexto() + "; font-size:13pt; color:"
                + Tema.hex(Tema.TEXTO) + "'>"
                + "<p><b style='color:" + c + "; font-size:15pt'>Cómo usar la calculadora</b></p>"
                + "<p>1. Elige <b>Derivadas</b> o <b>Integrales</b> y un método de la lista.<br>"
                + "2. Escribe la función (o pulsa <b>Cargar ejemplo</b>).<br>"
                + "3. Pulsa <b>Calcular</b>. Los pasos del método elegido aparecen resaltados.</p>"
                + "<p><b style='color:" + c + "; font-size:15pt'>Cómo escribir las funciones</b></p>"
                + "<table cellpadding='5' cellspacing='0'>"
                + filaAyuda("Potencias", "x^2,  x^-1,  x^(1/2),  √(x),  1/x^3", m)
                + filaAyuda("Productos", "3x,  x^2·sin(x),  x*e^x,  (x+1)(x-1)", m)
                + filaAyuda("Cocientes", "sin(x)/x,  (3x + 5)/(x^2 + x - 2)", m)
                + filaAyuda("Funciones", "sin cos tan sec csc cot  ln log  e^( )  √( )  arcsin arccos arctan", m)
                + filaAyuda("Compuestas", "sin(x^2 + 1),  e^(x^2),  ln(cos(x))", m)
                + filaAyuda("Constantes", "pi,  e", m)
                + filaAyuda("Implícita", "x^2 + y^2 = 25", m)
                + filaAyuda("Límites", "∞,  -∞,  0,  pi/2   (en 'x tiende a', a y b)", m)
                + "</table>"
                + "<p><b style='color:" + c + "'>Teclado</b><br>Escribe donde está el cursor: en la función o en los "
                + "límites. Las teclas ∞ y −∞ sirven para límites e integrales impropias.</p>"
                + "<p><b style='color:" + c + "'>Gráfica</b><br>Rueda = zoom, arrastrar = mover, doble clic = restablecer.</p>"
                + "</body></html>";
        vistaPasos.setText(html);
        vistaPasos.setCaretPosition(0);
        navegacion.setSeleccionado(0);
    }

    private String filaAyuda(String nombre, String ejemplo, String fuenteMono) {
        return "<tr><td width='110'><b>" + nombre + "</b></td><td><span style='font-family:" + fuenteMono + "; color:"
                + Tema.hex(Tema.PRIMARIO) + "'>" + RenderizadorHtml.escapar(ejemplo) + "</span></td></tr>";
    }

    private void mostrarEstado(String mensaje, Color color) {
        barraEstado.setText(mensaje);
        barraEstado.setForeground(color);
    }

    /**
     * Panel vertical que, dentro de un JScrollPane, siempre toma el ancho
     * visible (así las tarjetas nunca quedan cortadas a la derecha).
     */
    private static class PanelQueSigueAncho extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
