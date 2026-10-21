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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal de la calculadora (Swing).
 * <p>
 * Está organizada en un método por panel para que el código sea fácil de leer:
 * </p>
 * <ul>
 *   <li>{@link #crearPanelEntrada()}: expresión, operación, variable y límites.</li>
 *   <li>{@link #crearPanelResultado()}: pestaña "Resultado y pasos".</li>
 *   <li>{@link #crearPanelGrafica()}: pestaña "Gráfica 2D".</li>
 *   <li>{@link #crearPanelAsistente()}: pestaña "Asistente IA".</li>
 *   <li>{@link #crearBarraEstado()}: mensajes en la parte inferior.</li>
 * </ul>
 * La llamada a la IA se hace en un {@link SwingWorker} para que la ventana
 * no se congele mientras se espera la respuesta de la red.
 */
public class VentanaCalculadora extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String OP_DERIVADA = "Derivada";
    private static final String OP_INDEFINIDA = "Integral indefinida";
    private static final String OP_DEFINIDA = "Integral definida [a, b]";
    private static final String OP_EVALUAR = "Evaluar en un punto";

    private static final String[] EJEMPLOS = {
        "-- Ejemplos --",
        "3x^2 - 2x + 1",
        "x^2*y + 3y",
        "2sin(3x) + cos(x)",
        "e^(2x) - 4/x",
        "y*sin(x)",
        "x^(1/2) + 1/x^2",
        "4ln(x) + 5",
        "3x^2 + 2sin(x) - 4e^(2x)"
    };

    // Motor y tutor
    private final transient Calculadora calculadora;
    private transient TutorIA tutor;
    private transient ResultadoCalculo ultimoResultado;

    // Componentes de entrada
    private JTextField campoExpresion;
    private JComboBox<String> comboEjemplos;
    private JComboBox<String> comboOperacion;
    private JComboBox<Variable> comboVariable;
    private JLabel etiquetaA;
    private JLabel etiquetaB;
    private JLabel etiquetaOtra;
    private JTextField campoA;
    private JTextField campoB;
    private JTextField campoOtra;
    private JButton botonCalcular;

    // Componentes de salida
    private JLabel etiquetaResultado;
    private JTextArea areaPasos;
    private PanelGrafico2D panelGrafico;
    private JTextArea areaExplicacion;
    private JTextField campoPregunta;
    private JButton botonExplicar;
    private JLabel etiquetaProveedor;
    private JLabel barraEstado;
    private JTabbedPane pestanas;

    /** Construye y organiza la ventana. */
    public VentanaCalculadora() {
        super("Calculadora de Cálculo con Asistente de IA");
        this.calculadora = new Calculadora();
        this.tutor = new TutorIA(ConfiguracionIA.cargar());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));

        JPanel contenido = new JPanel(new BorderLayout(8, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        contenido.add(crearPanelEntrada(), BorderLayout.NORTH);

        pestanas = new JTabbedPane();
        pestanas.addTab("Resultado y pasos", crearPanelResultado());
        pestanas.addTab("Gráfica 2D", crearPanelGrafica());
        pestanas.addTab("Asistente IA", crearPanelAsistente());
        contenido.add(pestanas, BorderLayout.CENTER);

        contenido.add(crearBarraEstado(), BorderLayout.SOUTH);
        setContentPane(contenido);

        actualizarCamposSegunOperacion();
        pack();
        setSize(1050, 760);
        setLocationRelativeTo(null);
    }

    // ==================================================================
    // Construcción de paneles
    // ==================================================================

    /**
     * Panel superior con los datos de entrada.
     *
     * @return panel de entrada
     */
    private JPanel crearPanelEntrada() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Datos de entrada"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        // Fila 0: expresión
        c.gridx = 0;
        c.gridy = 0;
        panel.add(new JLabel("Función f:"), c);

        campoExpresion = new JTextField("3x^2 - 2x + 1", 30);
        campoExpresion.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        campoExpresion.setToolTipText("Ejemplos: 3x^2 - 2x + 1, x^2*y, 2sin(3x), e^(2x), 1/x, 4ln(x)");
        campoExpresion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcular();
            }
        });
        c.gridx = 1;
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        panel.add(campoExpresion, c);

        comboEjemplos = new JComboBox<String>(EJEMPLOS);
        comboEjemplos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                usarEjemplo();
            }
        });
        c.gridx = 5;
        c.gridwidth = 2;
        c.weightx = 0.0;
        panel.add(comboEjemplos, c);

        // Fila 1: operación y variable
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        panel.add(new JLabel("Operación:"), c);

        comboOperacion = new JComboBox<String>(new String[]{OP_DERIVADA, OP_INDEFINIDA, OP_DEFINIDA, OP_EVALUAR});
        comboOperacion.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarCamposSegunOperacion();
            }
        });
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(comboOperacion, c);

        c.gridx = 2;
        c.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Variable:"), c);

        comboVariable = new JComboBox<Variable>(Variable.values());
        comboVariable.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarCamposSegunOperacion();
            }
        });
        c.gridx = 3;
        panel.add(comboVariable, c);

        // Fila 2: límites y valor de la otra variable
        c.gridy = 2;
        c.gridx = 0;
        etiquetaA = new JLabel("Límite a:");
        panel.add(etiquetaA, c);
        campoA = new JTextField("0", 7);
        campoA.setToolTipText("Admite números, fracciones y pi: 0, 1/2, pi/2");
        c.gridx = 1;
        panel.add(campoA, c);

        c.gridx = 2;
        etiquetaB = new JLabel("Límite b:");
        panel.add(etiquetaB, c);
        campoB = new JTextField("1", 7);
        campoB.setToolTipText("Admite números, fracciones y pi: 1, 3/2, pi");
        c.gridx = 3;
        panel.add(campoB, c);

        c.gridx = 4;
        etiquetaOtra = new JLabel("Valor fijo de y:");
        panel.add(etiquetaOtra, c);
        campoOtra = new JTextField("1", 6);
        campoOtra.setToolTipText("Valor numérico de la otra variable, usado al evaluar y graficar");
        c.gridx = 5;
        panel.add(campoOtra, c);

        // Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        botonCalcular = new JButton("Calcular");
        botonCalcular.setFont(botonCalcular.getFont().deriveFont(Font.BOLD));
        botonCalcular.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calcular();
            }
        });
        JButton botonLimpiar = new JButton("Limpiar");
        botonLimpiar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                limpiar();
            }
        });
        JButton botonAyuda = new JButton("Ayuda");
        botonAyuda.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarAyuda();
            }
        });
        botones.add(botonAyuda);
        botones.add(botonLimpiar);
        botones.add(botonCalcular);
        c.gridy = 1;
        c.gridx = 4;
        c.gridwidth = 3;
        c.anchor = GridBagConstraints.EAST;
        panel.add(botones, c);

        getRootPane().setDefaultButton(botonCalcular);
        return panel;
    }

    /**
     * Pestaña con el resultado y el procedimiento paso a paso.
     *
     * @return panel de resultados
     */
    private JPanel crearPanelResultado() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        etiquetaResultado = new JLabel("Escriba una función y pulse Calcular.");
        etiquetaResultado.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        etiquetaResultado.setForeground(new Color(25, 70, 160));
        etiquetaResultado.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
        panel.add(etiquetaResultado, BorderLayout.NORTH);

        areaPasos = new JTextArea();
        areaPasos.setEditable(false);
        areaPasos.setLineWrap(true);
        areaPasos.setWrapStyleWord(true);
        areaPasos.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        areaPasos.setMargin(new Insets(8, 8, 8, 8));
        panel.add(new JScrollPane(areaPasos), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Pestaña con el graficador.
     *
     * @return panel de la gráfica
     */
    private JPanel crearPanelGrafica() {
        JPanel panel = new JPanel(new BorderLayout());
        panelGrafico = new PanelGrafico2D();
        panel.add(panelGrafico, BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton botonRestablecer = new JButton("Restablecer vista");
        botonRestablecer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panelGrafico.restablecerVista();
            }
        });
        barra.add(botonRestablecer);
        barra.add(new JLabel("Rueda del ratón: zoom  |  Arrastrar: mover  |  Doble clic: restablecer  |  "
                + "Línea azul: f  |  Línea roja discontinua: derivada o antiderivada  |  Verde: área"));
        panel.add(barra, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Pestaña del asistente de IA.
     *
     * @return panel del asistente
     */
    private JPanel crearPanelAsistente() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        etiquetaProveedor = new JLabel();
        actualizarEtiquetaProveedor();
        panel.add(etiquetaProveedor, BorderLayout.NORTH);

        areaExplicacion = new JTextArea("Realice un cálculo y pulse \"Explicar con IA\". "
                + "La IA no calcula: solo explica los pasos que obtuvo el motor Java.");
        areaExplicacion.setEditable(false);
        areaExplicacion.setLineWrap(true);
        areaExplicacion.setWrapStyleWord(true);
        areaExplicacion.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
        areaExplicacion.setMargin(new Insets(10, 10, 10, 10));
        panel.add(new JScrollPane(areaExplicacion), BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout(6, 6));
        inferior.add(new JLabel("Pregunta opcional:"), BorderLayout.WEST);
        campoPregunta = new JTextField();
        campoPregunta.setToolTipText("Por ejemplo: ¿por qué la derivada de una constante es cero?");
        inferior.add(campoPregunta, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton botonRecargar = new JButton("Recargar configuración");
        botonRecargar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tutor = new TutorIA(ConfiguracionIA.cargar());
                actualizarEtiquetaProveedor();
                mostrarEstado("Configuración de IA recargada.");
            }
        });
        botonExplicar = new JButton("Explicar con IA");
        botonExplicar.setFont(botonExplicar.getFont().deriveFont(Font.BOLD));
        botonExplicar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                explicarConIA();
            }
        });
        botones.add(botonRecargar);
        botones.add(botonExplicar);
        inferior.add(botones, BorderLayout.EAST);
        panel.add(inferior, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Barra de estado inferior.
     *
     * @return etiqueta de estado
     */
    private JLabel crearBarraEstado() {
        barraEstado = new JLabel("Listo.");
        barraEstado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 4, 2, 4)));
        return barraEstado;
    }

    // ==================================================================
    // Acciones
    // ==================================================================

    /** Habilita o deshabilita los campos según la operación elegida. */
    private void actualizarCamposSegunOperacion() {
        String operacion = (String) comboOperacion.getSelectedItem();
        Variable variable = (Variable) comboVariable.getSelectedItem();
        String s = variable.getSimbolo();
        String otra = variable.otra().getSimbolo();

        boolean esDefinida = OP_DEFINIDA.equals(operacion);
        boolean esEvaluar = OP_EVALUAR.equals(operacion);

        etiquetaA.setText(esEvaluar ? "Valor de " + s + ":" : "Límite a:");
        etiquetaB.setText("Límite b:");
        etiquetaOtra.setText("Valor fijo de " + otra + ":");

        campoA.setEnabled(esDefinida || esEvaluar);
        etiquetaA.setEnabled(esDefinida || esEvaluar);
        campoB.setEnabled(esDefinida);
        etiquetaB.setEnabled(esDefinida);
    }

    /** Copia el ejemplo elegido en el campo de la expresión. */
    private void usarEjemplo() {
        int indice = comboEjemplos.getSelectedIndex();
        if (indice > 0) {
            campoExpresion.setText(EJEMPLOS[indice]);
            comboEjemplos.setSelectedIndex(0);
            campoExpresion.requestFocusInWindow();
        }
    }

    /** Ejecuta la operación seleccionada y muestra resultados y gráfica. */
    private void calcular() {
        String texto = campoExpresion.getText();
        String operacion = (String) comboOperacion.getSelectedItem();
        Variable variable = (Variable) comboVariable.getSelectedItem();

        try {
            double valorOtra = leerNumero(campoOtra, "valor fijo de " + variable.otra().getSimbolo());
            ResultadoCalculo resultado;

            if (OP_DERIVADA.equals(operacion)) {
                resultado = calculadora.derivar(texto, variable);
            } else if (OP_INDEFINIDA.equals(operacion)) {
                resultado = calculadora.integrarIndefinida(texto, variable);
            } else if (OP_DEFINIDA.equals(operacion)) {
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
            mostrarError("Expresión no válida", "Revise la expresión escrita.\n\n" + e.getMessage()
                    + "\n\nEjemplos válidos: 3x^2 - 2x + 1,  x^2*y,  2sin(3x),  e^(2x),  1/x,  4ln(x)");
        } catch (OperacionNoSoportadaException e) {
            mostrarError("Operación aún no soportada", e.getMessage()
                    + "\n\nEsta calculadora resuelve reglas básicas; la técnica requerida se estudia más adelante.");
        } catch (DominioInvalidoException e) {
            mostrarError("Fuera del dominio", e.getMessage());
        } catch (CalculadoraException e) {
            mostrarError("No se pudo calcular", e.getMessage());
        } catch (RuntimeException e) {
            mostrarError("Error inesperado", "Ocurrió un error inesperado: " + e.getMessage());
        }
    }

    /**
     * Muestra el resultado en la pestaña de pasos y actualiza la gráfica.
     */
    private void mostrarResultado(ResultadoCalculo resultado, Variable variable, double valorOtra) {
        ultimoResultado = resultado;
        etiquetaResultado.setText(resultado.getTextoResultado());
        areaPasos.setText(resultado.generarInformePasos());
        areaPasos.setCaretPosition(0);

        Expresion funcion = resultado.getExpresion();
        ResultadoCalculo.TipoOperacion tipo = resultado.getTipo();
        String s = variable.getSimbolo();

        if (tipo == ResultadoCalculo.TipoOperacion.DERIVADA) {
            panelGrafico.graficar(funcion, funcion.toString(), resultado.getExpresionResultado(),
                    "f' = " + resultado.getExpresionResultado(), variable, valorOtra);
        } else if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_INDEFINIDA) {
            panelGrafico.graficar(funcion, funcion.toString(), resultado.getExpresionResultado(),
                    "F = " + resultado.getExpresionResultado() + " (con C = 0)", variable, valorOtra);
        } else if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA) {
            panelGrafico.graficar(funcion, funcion.toString(), null, null, variable, valorOtra);
            panelGrafico.sombrearArea(resultado.getLimiteInferior().doubleValue(),
                    resultado.getLimiteSuperior().doubleValue());
        } else {
            panelGrafico.graficar(funcion, funcion.toString(), null, null, variable, valorOtra);
            double valor = leerNumeroSinError(campoA);
            panelGrafico.marcarPunto(valor, resultado.getValorNumerico().doubleValue());
        }

        areaExplicacion.setText("Cálculo listo. Pulse \"Explicar con IA\" para recibir la explicación "
                + "didáctica del procedimiento.");
        mostrarEstado("Cálculo realizado (" + tipo.getNombre() + " respecto a " + s + ").");
    }

    /** Solicita la explicación al tutor en segundo plano (SwingWorker). */
    private void explicarConIA() {
        if (ultimoResultado == null) {
            mostrarError("Sin cálculo", "Primero realice un cálculo con el botón Calcular.");
            return;
        }
        final ResultadoCalculo resultado = ultimoResultado;
        final String pregunta = campoPregunta.getText();
        final TutorIA tutorActual = tutor;

        botonExplicar.setEnabled(false);
        areaExplicacion.setText("Generando la explicación, por favor espere...");
        mostrarEstado("Consultando: " + tutorActual.describirProveedor());

        SwingWorker<String, Void> trabajador = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                // Se ejecuta en un hilo secundario: la ventana sigue respondiendo
                return tutorActual.explicar(resultado, pregunta);
            }

            @Override
            protected void done() {
                // Se ejecuta de vuelta en el hilo de Swing
                try {
                    String explicacion = get();
                    areaExplicacion.setText(explicacion);
                    areaExplicacion.setCaretPosition(0);
                    mostrarEstado("Explicación generada por: " + tutorActual.getUltimaFuente());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    areaExplicacion.setText("La explicación fue interrumpida.");
                } catch (ExecutionException e) {
                    areaExplicacion.setText("No se pudo generar la explicación: " + e.getCause().getMessage());
                } finally {
                    botonExplicar.setEnabled(true);
                }
            }
        };
        trabajador.execute();
    }

    /** Limpia los campos y los resultados. */
    private void limpiar() {
        campoExpresion.setText("");
        etiquetaResultado.setText("Escriba una función y pulse Calcular.");
        areaPasos.setText("");
        areaExplicacion.setText("");
        campoPregunta.setText("");
        panelGrafico.limpiar();
        ultimoResultado = null;
        mostrarEstado("Listo.");
        campoExpresion.requestFocusInWindow();
    }

    /** Muestra una ventana con la sintaxis admitida. */
    private void mostrarAyuda() {
        String ayuda = "SINTAXIS ADMITIDA\n\n"
                + "Variables: x, y\n"
                + "Potencias: x^2, x^-1, x^(1/2), 1/x\n"
                + "Producto: 3x, 2*x*y, x^2y (multiplicación implícita)\n"
                + "Funciones: sin(kx) o sen(kx), cos(kx), e^(kx) o exp(kx), ln(kx)\n"
                + "Constantes: pi, e\n\n"
                + "REGLAS\n"
                + "- El argumento de cada función debe ser lineal: 2x, -y, x/2.\n"
                + "- No se admite el producto de dos funciones (sin(x)cos(x)).\n"
                + "- Escriba los polinomios desarrollados: x^2 + 2x en lugar de x(x + 2).\n"
                + "- e^2x se interpreta como e^(2x).\n\n"
                + "La otra variable se trata como constante (derivada/integral parcial).\n"
                + "Para graficar y evaluar se usa su 'valor fijo'.";
        JTextArea area = new JTextArea(ayuda);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Ayuda", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================================================================
    // Utilidades de la vista
    // ==================================================================

    /**
     * Lee un número de un campo de texto. Admite expresiones constantes como
     * 1/2, pi o pi/2, reutilizando el analizador de la calculadora.
     */
    private double leerNumero(JTextField campo, String nombre) throws ExpresionInvalidaException {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            throw new ExpresionInvalidaException("Falta el " + nombre + ".");
        }
        Expresion expresion;
        try {
            expresion = calculadora.interpretar(texto);
        } catch (ExpresionInvalidaException e) {
            throw new ExpresionInvalidaException("El " + nombre + " no es un número válido: " + texto);
        }
        if (expresion.dependeDe(Variable.X) || expresion.dependeDe(Variable.Y)) {
            throw new ExpresionInvalidaException("El " + nombre + " debe ser un número, no puede contener variables.");
        }
        return expresion.evaluar(0.0, 0.0);
    }

    private double leerNumeroSinError(JTextField campo) {
        try {
            return leerNumero(campo, "valor");
        } catch (ExpresionInvalidaException e) {
            return 0.0;
        }
    }

    private void actualizarEtiquetaProveedor() {
        etiquetaProveedor.setText("Proveedor: " + tutor.describirProveedor());
    }

    private void mostrarEstado(String mensaje) {
        barraEstado.setText(mensaje);
    }

    private void mostrarError(String titulo, String mensaje) {
        mostrarEstado(titulo + ": " + primeraLinea(mensaje));
        JOptionPane.showMessageDialog(this, mensaje, titulo, JOptionPane.WARNING_MESSAGE);
    }

    private String primeraLinea(String texto) {
        int salto = texto.indexOf('\n');
        if (salto < 0) {
            return texto;
        }
        return texto.substring(0, salto);
    }
}
