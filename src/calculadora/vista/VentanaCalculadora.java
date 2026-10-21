package calculadora.vista;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.DominioInvalidoException;
import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.excepciones.OperacionNoSoportadaException;
import calculadora.ia.ConfiguracionIA;
import calculadora.ia.TutorIA;
import calculadora.modelo.Calculadora;
import calculadora.modelo.Expresion;
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
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal de la calculadora.
 * <p>
 * Distribución:
 * </p>
 * <pre>
 * +-------------------------------- Encabezado (degradado) ---------------------------------+
 * | Columna izquierda              | Tarjeta de resultado                                    |
 * |  - Tarjeta "Función"           |---------------------------------------------------------|
 * |  - Tarjeta "Teclado"           | [Procedimiento] [Gráfica 2D] [Asistente IA]             |
 * |  - Tarjeta "Ejemplos"          | Contenido de la sección elegida                         |
 * +------------------------------------------------------------------------------------------+
 * </pre>
 * Cada zona se construye en su propio método para que el código sea fácil
 * de leer. La IA se consulta en un {@link SwingWorker} para no congelar la ventana.
 */
public class VentanaCalculadora extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String[] OPERACIONES = {"Derivada", "∫ Indefinida", "∫ Definida", "Evaluar"};
    private static final int OP_DERIVADA = 0;
    private static final int OP_INDEFINIDA = 1;
    private static final int OP_DEFINIDA = 2;
    private static final int OP_EVALUAR = 3;

    private static final String SECCION_PASOS = "pasos";
    private static final String SECCION_GRAFICA = "grafica";
    private static final String SECCION_IA = "ia";

    private static final String[] EJEMPLOS = {
        "3x^2 - 2x + 1",
        "x^2*y + 3y",
        "2sin(3x) + cos(x)",
        "e^(2x) - 4/x",
        "y*sin(x)",
        "x^(1/2) + 1/x^2",
        "4ln(x) + 5",
        "x^3 - 3x"
    };

    /** Teclas: {texto visible, texto a insertar, posición del cursor desde el final}. */
    private static final String[][] TECLAS = {
        {"x", "x", "0"}, {"y", "y", "0"}, {"x²", "^2", "0"}, {"xⁿ", "^", "0"}, {"( )", "()", "1"}, {"←", "", "0"},
        {"sin", "sin()", "1"}, {"cos", "cos()", "1"}, {"e^", "e^()", "1"}, {"ln", "ln()", "1"}, {"√x", "^(1/2)", "0"}, {"1/x", "1/x", "0"},
        {"+", " + ", "0"}, {"−", " - ", "0"}, {"×", "*", "0"}, {"÷", "/", "0"}, {"π", "pi", "0"}, {"C", "", "0"}
    };

    // Motor y tutor
    private final transient Calculadora calculadora;
    private transient TutorIA tutor;
    private transient ResultadoCalculo ultimoResultado;

    // Entrada
    private CampoModerno campoExpresion;
    private GrupoSegmentado selectorOperacion;
    private GrupoSegmentado selectorVariable;
    private JLabel etiquetaA;
    private JLabel etiquetaB;
    private JLabel etiquetaOtra;
    private CampoModerno campoA;
    private CampoModerno campoB;
    private CampoModerno campoOtra;
    private BotonModerno botonCalcular;

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

        JPanel cuerpo = new JPanel(new BorderLayout(18, 0));
        cuerpo.setOpaque(false);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(18, 18, 6, 18));
        cuerpo.add(crearColumnaIzquierda(), BorderLayout.WEST);
        cuerpo.add(crearColumnaDerecha(), BorderLayout.CENTER);
        raiz.add(cuerpo, BorderLayout.CENTER);
        raiz.add(crearBarraEstado(), BorderLayout.SOUTH);
        setContentPane(raiz);

        actualizarCamposSegunOperacion();
        actualizarInsigniaIA();
        mostrarEstadoInicial();

        setMinimumSize(new Dimension(1120, 720));
        ajustarTamanoInicial();
        getRootPane().setDefaultButton(botonCalcular);
    }

    /** Abre la ventana con un tamaño cómodo que quepa en la pantalla. */
    private void ajustarTamanoInicial() {
        Rectangle pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int ancho = Math.min(1360, pantalla.width);
        int alto = Math.min(900, pantalla.height);
        setSize(ancho, alto);
        setLocationRelativeTo(null);
        if (pantalla.width < 1360 || pantalla.height < 860) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    // ==================================================================
    // ENCABEZADO
    // ==================================================================

    private JComponent crearEncabezado() {
        Encabezado encabezado = new Encabezado("Calculadora de Cálculo",
                "Derivadas, integrales y gráficas paso a paso  ·  Programación II");
        insigniaIA = new Encabezado.Insignia();
        encabezado.agregarDerecha(insigniaIA);

        BotonModerno botonConfigurar = new BotonModerno("Configurar IA", BotonModerno.Estilo.SOBRE_COLOR);
        botonConfigurar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirConfiguracionIA();
            }
        });
        encabezado.agregarDerecha(botonConfigurar);

        BotonModerno botonAyuda = new BotonModerno("Ayuda", BotonModerno.Estilo.SOBRE_COLOR);
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
    // COLUMNA IZQUIERDA: entrada, teclado y ejemplos
    // ==================================================================

    private JComponent crearColumnaIzquierda() {
        JPanel columna = new PanelQueSigueAncho();
        columna.setOpaque(false);
        columna.setLayout(new BoxLayout(columna, BoxLayout.Y_AXIS));
        columna.add(crearTarjetaEntrada());
        columna.add(Box.createVerticalStrut(12));
        columna.add(crearTarjetaTeclado());
        columna.add(Box.createVerticalStrut(12));
        columna.add(crearTarjetaEjemplos());
        columna.add(Box.createVerticalGlue());

        JScrollPane desplazamiento = new JScrollPane(columna,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        BarraDesplazamientoModerna.aplicar(desplazamiento);
        desplazamiento.setPreferredSize(new Dimension(430, 400));
        return desplazamiento;
    }

    /**
     * Tarjeta con la función, la operación, la variable y los límites.
     */
    private JComponent crearTarjetaEntrada() {
        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;

        // Función
        c.gridy = 0;
        c.insets = new Insets(0, 0, 6, 0);
        contenido.add(PanelTarjeta.crearTitulo("Función  f(x, y)"), c);

        campoExpresion = new CampoModerno("3x^2 - 2x + 1", 20);
        campoExpresion.setFont(Tema.mono(21f));
        campoExpresion.setRelleno(11, 14);
        campoExpresion.setTextoAyuda("Ej.: 3x^2 + 2sin(x)");
        campoExpresion.setToolTipText("Ejemplos: 3x^2 - 2x + 1, x^2*y, 2sin(3x), e^(2x), 1/x, 4ln(x)");
        c.gridy = 1;
        c.insets = new Insets(0, 0, 16, 0);
        contenido.add(campoExpresion, c);

        // Operación
        c.gridy = 2;
        c.insets = new Insets(0, 0, 6, 0);
        contenido.add(PanelTarjeta.crearTitulo("Operación"), c);
        selectorOperacion = new GrupoSegmentado(OPERACIONES);
        selectorOperacion.setFont(Tema.negrita(12.5f));
        selectorOperacion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarCamposSegunOperacion();
            }
        });
        c.gridy = 3;
        c.insets = new Insets(0, 0, 16, 0);
        contenido.add(selectorOperacion, c);

        // Variable y valor fijo de la otra variable
        c.gridwidth = 1;
        c.gridy = 4;
        c.gridx = 0;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 6, 14);
        contenido.add(PanelTarjeta.crearTitulo("Variable"), c);
        etiquetaOtra = PanelTarjeta.crearTitulo("Valor fijo de y");
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 6, 0);
        contenido.add(etiquetaOtra, c);

        selectorVariable = new GrupoSegmentado("x", "y");
        selectorVariable.setFont(Tema.mono(15f));
        selectorVariable.setPreferredSize(new Dimension(110, 40));
        selectorVariable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarCamposSegunOperacion();
            }
        });
        c.gridy = 5;
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 14, 14);
        contenido.add(selectorVariable, c);

        campoOtra = crearCampoNumero("1", "Valor numérico de la otra variable (se usa al evaluar y graficar)");
        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 14, 0);
        contenido.add(campoOtra, c);

        // Límites
        etiquetaA = PanelTarjeta.crearTitulo("Límite inferior (a)");
        etiquetaB = PanelTarjeta.crearTitulo("Límite superior (b)");
        c.gridy = 6;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 6, 10);
        contenido.add(etiquetaA, c);
        c.gridx = 2;
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 6, 0);
        contenido.add(etiquetaB, c);

        campoA = crearCampoNumero("0", "Admite números, fracciones y pi: 0, 1/2, pi/2");
        campoB = crearCampoNumero("1", "Admite números, fracciones y pi: 1, 3/2, pi");
        c.gridy = 7;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(0, 0, 18, 10);
        contenido.add(campoA, c);
        c.gridx = 2;
        c.gridwidth = 1;
        c.insets = new Insets(0, 0, 18, 0);
        contenido.add(campoB, c);

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
        c.gridy = 8;
        c.gridx = 0;
        c.gridwidth = 3;
        c.insets = new Insets(0, 0, 0, 0);
        contenido.add(botones, c);

        PanelTarjeta tarjeta = new PanelTarjeta(new BorderLayout());
        tarjeta.add(contenido, BorderLayout.CENTER);
        return limitarAltura(tarjeta);
    }

    private CampoModerno crearCampoNumero(String valor, String ayuda) {
        CampoModerno campo = new CampoModerno(valor, 6);
        campo.setFont(Tema.mono(15f));
        campo.setToolTipText(ayuda);
        return campo;
    }

    /**
     * Teclado con símbolos matemáticos que se insertan en la función.
     */
    private JComponent crearTarjetaTeclado() {
        JPanel rejilla = new JPanel(new GridLayout(3, 6, 7, 7));
        rejilla.setOpaque(false);
        for (int i = 0; i < TECLAS.length; i++) {
            final String visible = TECLAS[i][0];
            final String insertar = TECLAS[i][1];
            final int retroceso = Integer.parseInt(TECLAS[i][2]);
            BotonModerno tecla = new BotonModerno(visible, BotonModerno.Estilo.TECLA);
            if (visible.equals("sin") || visible.equals("cos") || visible.equals("ln")) {
                tecla.setFont(Tema.mono(14f));
            }
            if (visible.equals("C") || visible.equals("←")) {
                tecla.setForeground(Tema.ERROR);
            }
            tecla.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pulsarTecla(visible, insertar, retroceso);
                }
            });
            rejilla.add(tecla);
        }
        return limitarAltura(PanelTarjeta.conTitulo("Teclado matemático", rejilla));
    }

    /**
     * Chips con funciones de ejemplo.
     */
    private JComponent crearTarjetaEjemplos() {
        JPanel rejilla = new JPanel(new GridLayout(0, 2, 8, 8));
        rejilla.setOpaque(false);
        for (int i = 0; i < EJEMPLOS.length; i++) {
            final String ejemplo = EJEMPLOS[i];
            BotonModerno chip = new BotonModerno(ejemplo, BotonModerno.Estilo.CHIP);
            chip.setFont(Tema.mono(12.5f));
            chip.setToolTipText("Usar " + ejemplo);
            chip.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    campoExpresion.setText(ejemplo);
                    campoExpresion.requestFocusInWindow();
                    calcular();
                }
            });
            rejilla.add(chip);
        }
        return limitarAltura(PanelTarjeta.conTitulo("Ejemplos rápidos", rejilla));
    }

    /** Evita que BoxLayout estire verticalmente las tarjetas. */
    private JComponent limitarAltura(final JComponent componente) {
        componente.setAlignmentX(0.0f);
        JPanel envoltura = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getMaximumSize() {
                Dimension preferido = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, preferido.height);
            }
        };
        envoltura.setOpaque(false);
        envoltura.add(componente, BorderLayout.CENTER);
        return envoltura;
    }

    // ==================================================================
    // COLUMNA DERECHA: resultado y secciones
    // ==================================================================

    private JComponent crearColumnaDerecha() {
        JPanel columna = new JPanel(new BorderLayout(0, 14));
        columna.setOpaque(false);
        columna.add(crearTarjetaResultado(), BorderLayout.NORTH);
        columna.add(crearZonaSecciones(), BorderLayout.CENTER);
        return columna;
    }

    /**
     * Tarjeta destacada con el resultado final.
     */
    private JComponent crearTarjetaResultado() {
        tarjetaResultado = new PanelTarjeta(new BorderLayout(0, 6));

        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);
        etiquetaTipoResultado = PanelTarjeta.crearTitulo("Resultado");
        superior.add(etiquetaTipoResultado, BorderLayout.WEST);
        botonCopiar = new BotonModerno("Copiar", BotonModerno.Estilo.FANTASMA);
        botonCopiar.setFont(Tema.negrita(12f));
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
        textoResultado.setFont(Tema.mono(23f));
        textoResultado.setForeground(Tema.TEXTO);
        textoResultado.setRows(1);
        tarjetaResultado.add(textoResultado, BorderLayout.CENTER);
        return tarjetaResultado;
    }

    /**
     * Navegación entre Procedimiento, Gráfica y Asistente IA.
     */
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
        marco.setBackground(Color.WHITE);
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

        JPanel superior = new JPanel(new BorderLayout());
        superior.setOpaque(false);
        etiquetaProveedor = new JLabel();
        etiquetaProveedor.setFont(Tema.texto(13f));
        etiquetaProveedor.setForeground(Tema.TEXTO_SECUNDARIO);
        superior.add(etiquetaProveedor, BorderLayout.WEST);
        BotonModerno botonConfigurar = new BotonModerno("Configurar IA", BotonModerno.Estilo.FANTASMA);
        botonConfigurar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirConfiguracionIA();
            }
        });
        superior.add(botonConfigurar, BorderLayout.EAST);
        seccion.add(superior, BorderLayout.NORTH);

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
        return visor;
    }

    private JComponent crearBarraEstado() {
        barraEstado = new JLabel("Listo.");
        barraEstado.setFont(Tema.texto(12.5f));
        barraEstado.setForeground(Tema.TEXTO_SECUNDARIO);
        barraEstado.setBorder(BorderFactory.createEmptyBorder(4, 22, 10, 22));
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

    /** Habilita o deshabilita los campos según la operación elegida. */
    private void actualizarCamposSegunOperacion() {
        int operacion = selectorOperacion.getSeleccionado();
        Variable variable = variableSeleccionada();
        boolean esDefinida = operacion == OP_DEFINIDA;
        boolean esEvaluar = operacion == OP_EVALUAR;

        if (esEvaluar) {
            etiquetaA.setText("VALOR DE " + variable.getSimbolo().toUpperCase());
        } else {
            etiquetaA.setText("LÍMITE INFERIOR (a)");
        }
        etiquetaOtra.setText("VALOR FIJO DE " + variable.otra().getSimbolo().toUpperCase());

        campoA.setEnabled(esDefinida || esEvaluar);
        etiquetaA.setEnabled(esDefinida || esEvaluar);
        campoB.setEnabled(esDefinida);
        etiquetaB.setEnabled(esDefinida);
        repaint();
    }

    private Variable variableSeleccionada() {
        if (selectorVariable.getSeleccionado() == 1) {
            return Variable.Y;
        }
        return Variable.X;
    }

    /** Inserta en la función el texto de la tecla pulsada. */
    private void pulsarTecla(String visible, String insertar, int retroceso) {
        if (visible.equals("C")) {
            campoExpresion.setText("");
        } else if (visible.equals("←")) {
            int posicion = campoExpresion.getCaretPosition();
            String texto = campoExpresion.getText();
            if (campoExpresion.getSelectedText() != null) {
                campoExpresion.replaceSelection("");
            } else if (posicion > 0) {
                campoExpresion.setText(texto.substring(0, posicion - 1) + texto.substring(posicion));
                campoExpresion.setCaretPosition(posicion - 1);
            }
        } else {
            campoExpresion.replaceSelection(insertar);
            int posicion = campoExpresion.getCaretPosition() - retroceso;
            campoExpresion.setCaretPosition(Math.max(0, posicion));
        }
        campoExpresion.requestFocusInWindow();
    }

    /** Ejecuta la operación seleccionada y muestra resultados y gráfica. */
    private void calcular() {
        String texto = campoExpresion.getText();
        int operacion = selectorOperacion.getSeleccionado();
        Variable variable = variableSeleccionada();

        try {
            double valorOtra = leerNumero(campoOtra, "valor fijo de " + variable.otra().getSimbolo());
            ResultadoCalculo resultado;
            if (operacion == OP_DERIVADA) {
                resultado = calculadora.derivar(texto, variable);
            } else if (operacion == OP_INDEFINIDA) {
                resultado = calculadora.integrarIndefinida(texto, variable);
            } else if (operacion == OP_DEFINIDA) {
                double a = leerNumero(campoA, "límite a");
                double b = leerNumero(campoB, "límite b");
                resultado = calculadora.integrarDefinida(texto, variable, a, b, valorOtra);
            } else {
                double valor = leerNumero(campoA, "valor de " + variable.getSimbolo());
                double x = valor;
                double y = valorOtra;
                if (variable == Variable.Y) {
                    x = valorOtra;
                    y = valor;
                }
                resultado = calculadora.evaluar(texto, x, y);
            }
            mostrarResultado(resultado, variable, valorOtra);
        } catch (ExpresionInvalidaException e) {
            mostrarError("Expresión no válida", e.getMessage()
                    + ".  Ejemplos válidos: 3x^2 - 2x + 1,  x^2*y,  2sin(3x),  e^(2x),  1/x");
        } catch (OperacionNoSoportadaException e) {
            mostrarError("Operación aún no soportada", e.getMessage());
        } catch (DominioInvalidoException e) {
            mostrarError("Fuera del dominio", e.getMessage());
        } catch (CalculadoraException e) {
            mostrarError("No se pudo calcular", e.getMessage());
        } catch (RuntimeException e) {
            mostrarError("Error inesperado", String.valueOf(e.getMessage()));
        }
    }

    private void mostrarResultado(ResultadoCalculo resultado, Variable variable, double valorOtra) {
        ultimoResultado = resultado;

        // Tarjeta de resultado en estado normal
        tarjetaResultado.setColorFondo(Tema.SUPERFICIE);
        etiquetaTipoResultado.setForeground(Tema.PRIMARIO);
        etiquetaTipoResultado.setText(resultado.getTipo().getNombre().toUpperCase() + "  ·  RESPECTO A "
                + variable.getSimbolo().toUpperCase());
        textoResultado.setFont(Tema.mono(23f));
        textoResultado.setForeground(Tema.TEXTO);
        textoResultado.setText(resultado.getTextoResultado());
        botonCopiar.setEnabled(true);

        // Procedimiento
        vistaPasos.setText(RenderizadorHtml.pasos(resultado));
        vistaPasos.setCaretPosition(0);

        // Gráfica
        Expresion funcion = resultado.getExpresion();
        ResultadoCalculo.TipoOperacion tipo = resultado.getTipo();
        if (tipo == ResultadoCalculo.TipoOperacion.DERIVADA) {
            panelGrafico.graficar(funcion, funcion.toString(), resultado.getExpresionResultado(),
                    "f' = " + resultado.getExpresionResultado(), variable, valorOtra);
        } else if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_INDEFINIDA) {
            panelGrafico.graficar(funcion, funcion.toString(), resultado.getExpresionResultado(),
                    "F = " + resultado.getExpresionResultado() + "  (C = 0)", variable, valorOtra);
        } else if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA) {
            panelGrafico.graficar(funcion, funcion.toString(), null, null, variable, valorOtra);
            panelGrafico.sombrearArea(resultado.getLimiteInferior().doubleValue(),
                    resultado.getLimiteSuperior().doubleValue());
        } else {
            panelGrafico.graficar(funcion, funcion.toString(), null, null, variable, valorOtra);
            panelGrafico.marcarPunto(leerNumeroSinError(campoA), resultado.getValorNumerico().doubleValue());
        }

        vistaExplicacion.setText(RenderizadorHtml.estadoVacio("Cálculo listo",
                "Pulsa \"Explicar con IA\" para recibir la explicación del procedimiento."));
        mostrarEstado(tipo.getNombre() + " calculada correctamente.", Tema.EXITO);
    }

    /** Muestra el error dentro de la tarjeta de resultado (sin ventanas emergentes). */
    private void mostrarError(String titulo, String mensaje) {
        tarjetaResultado.setColorFondo(new Color(254, 242, 242));
        etiquetaTipoResultado.setForeground(Tema.ERROR);
        etiquetaTipoResultado.setText(titulo.toUpperCase());
        textoResultado.setFont(Tema.texto(15.5f));
        textoResultado.setForeground(new Color(127, 29, 29));
        textoResultado.setText(mensaje);
        botonCopiar.setEnabled(false);
        ultimoResultado = null;
        vistaPasos.setText(RenderizadorHtml.estadoVacio("No se pudo completar el cálculo",
                "Corrige la función o los datos y vuelve a pulsar Calcular."));
        mostrarEstado(titulo + ".", Tema.ERROR);
        campoExpresion.requestFocusInWindow();
    }

    private void mostrarEstadoInicial() {
        tarjetaResultado.setColorFondo(Tema.SUPERFICIE);
        etiquetaTipoResultado.setForeground(Tema.TEXTO_SECUNDARIO);
        etiquetaTipoResultado.setText("RESULTADO");
        textoResultado.setFont(Tema.texto(17f));
        textoResultado.setForeground(Tema.TEXTO_TENUE);
        textoResultado.setText("Escribe una función, elige la operación y pulsa Calcular.");
        botonCopiar.setEnabled(false);
        vistaPasos.setText(RenderizadorHtml.estadoVacio("Aquí verás el procedimiento paso a paso",
                "Cada regla aplicada aparece explicada con su resultado parcial."));
        vistaExplicacion.setText(RenderizadorHtml.estadoVacio("Asistente de IA",
                "Realiza un cálculo y pulsa \"Explicar con IA\". La IA no calcula: explica los pasos del motor."));
    }

    /** Solicita la explicación al tutor en segundo plano (SwingWorker). */
    private void explicarConIA() {
        if (ultimoResultado == null) {
            vistaExplicacion.setText(RenderizadorHtml.estadoVacio("Primero realiza un cálculo",
                    "Escribe una función a la izquierda y pulsa Calcular."));
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
                // Hilo secundario: la ventana sigue respondiendo
                return tutorActual.explicar(resultado, pregunta);
            }

            @Override
            protected void done() {
                // De vuelta en el hilo de Swing
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

    private void abrirConfiguracionIA() {
        DialogoConfiguracionIA dialogo = new DialogoConfiguracionIA(this, ConfiguracionIA.cargar());
        dialogo.setVisible(true); // modal: espera a que se cierre
        if (dialogo.seGuardo()) {
            tutor = new TutorIA(ConfiguracionIA.cargar());
            actualizarInsigniaIA();
            mostrarEstado("Configuración de IA guardada: " + tutor.describirProveedor(), Tema.EXITO);
        }
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
            insigniaIA.actualizar("IA: " + nombre + " · " + configuracion.getModeloEfectivo(),
                    new Color(74, 222, 128));
            etiquetaProveedor.setText("Proveedor: " + tutor.describirProveedor());
        } else {
            insigniaIA.actualizar("IA: Explicador local", new Color(251, 191, 36));
            etiquetaProveedor.setText("Proveedor: Explicador Local (sin conexión). Pulsa \"Configurar IA\" para usar Claude, OpenAI u Ollama.");
        }
    }

    private void copiarResultado() {
        String texto = textoResultado.getText();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto), null);
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
                + "<p><b style='color:" + c + "; font-size:15pt'>Cómo escribir las funciones</b></p>"
                + "<table cellpadding='6' cellspacing='0'>"
                + filaAyuda("Variables", "x, y", m)
                + filaAyuda("Potencias", "x^2,  x^-1,  x^(1/2),  1/x", m)
                + filaAyuda("Producto", "3x,  2*x*y,  x^2y", m)
                + filaAyuda("Funciones", "sin(kx) o sen(kx),  cos(kx),  e^(kx) o exp(kx),  ln(kx)", m)
                + filaAyuda("Constantes", "pi,  e", m)
                + "</table>"
                + "<p><b style='color:" + c + "'>Reglas</b></p>"
                + "<p>&bull; El argumento de cada función debe ser lineal: 2x, -y, x/2.<br>"
                + "&bull; No se admite el producto de dos funciones: sin(x)cos(x).<br>"
                + "&bull; Escribe los polinomios desarrollados: x^2 + 2x en lugar de x(x + 2).<br>"
                + "&bull; e^2x se interpreta como e^(2x).<br>"
                + "&bull; La otra variable se trata como constante (derivada o integral parcial); "
                + "para graficar y evaluar se usa su valor fijo.</p>"
                + "<p><b style='color:" + c + "'>Atajos</b></p>"
                + "<p>&bull; Enter: calcular.&nbsp;&nbsp; &bull; Teclado matemático: inserta símbolos donde está el cursor.<br>"
                + "&bull; Gráfica: rueda = zoom, arrastrar = mover, doble clic = restablecer.</p>"
                + "</body></html>";
        vistaPasos.setText(html);
        vistaPasos.setCaretPosition(0);
        navegacion.setSeleccionado(0);
    }

    private String filaAyuda(String nombre, String ejemplo, String fuenteMono) {
        return "<tr><td width='120'><b>" + nombre + "</b></td><td><span style='font-family:" + fuenteMono
                + "'>" + RenderizadorHtml.escapar(ejemplo) + "</span></td></tr>";
    }

    // ==================================================================
    // UTILIDADES
    // ==================================================================

    /**
     * Lee un número de un campo de texto. Admite expresiones constantes como
     * 1/2, pi o pi/2, reutilizando el analizador de la calculadora.
     */
    private double leerNumero(CampoModerno campo, String nombre) throws ExpresionInvalidaException {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            throw new ExpresionInvalidaException("Falta el " + nombre);
        }
        Expresion expresion;
        try {
            expresion = calculadora.interpretar(texto);
        } catch (ExpresionInvalidaException e) {
            throw new ExpresionInvalidaException("El " + nombre + " no es un número válido: " + texto);
        }
        if (expresion.dependeDe(Variable.X) || expresion.dependeDe(Variable.Y)) {
            throw new ExpresionInvalidaException("El " + nombre + " debe ser un número, no puede contener variables");
        }
        return expresion.evaluar(0.0, 0.0);
    }

    private double leerNumeroSinError(CampoModerno campo) {
        try {
            return leerNumero(campo, "valor");
        } catch (ExpresionInvalidaException e) {
            return 0.0;
        }
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
