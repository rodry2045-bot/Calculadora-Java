package calculadora.vista;

import calculadora.excepciones.IAException;
import calculadora.ia.ConfiguracionIA;
import calculadora.ia.TutorIA;
import calculadora.vista.componentes.BotonModerno;
import calculadora.vista.componentes.CampoClave;
import calculadora.vista.componentes.CampoModerno;
import calculadora.vista.componentes.GrupoSegmentado;
import calculadora.vista.componentes.PanelTarjeta;
import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Ventana para configurar el Asistente de IA sin editar archivos a mano.
 * <p>
 * Permite elegir el proveedor, escribir la clave de API, probar la conexión
 * y guardar todo en {@code config.properties}.
 * </p>
 */
public class DialogoConfiguracionIA extends JDialog {

    private static final long serialVersionUID = 1L;

    /** Valores internos de cada opción del selector, en el mismo orden. */
    private static final String[] PROVEEDORES = {
        ConfiguracionIA.PROVEEDOR_LOCAL,
        ConfiguracionIA.PROVEEDOR_ANTHROPIC,
        ConfiguracionIA.PROVEEDOR_OPENAI,
        ConfiguracionIA.PROVEEDOR_OLLAMA
    };

    private GrupoSegmentado selectorProveedor;
    private JLabel textoAyuda;
    private JLabel etiquetaClave;
    private CampoClave campoClave;
    private JCheckBox casillaMostrar;
    private CampoModerno campoModelo;
    private CampoModerno campoUrl;
    private CampoModerno campoTiempo;
    private JLabel etiquetaEstado;
    private BotonModerno botonProbar;
    private BotonModerno botonGuardar;

    private boolean guardado;

    /**
     * @param duena  ventana principal
     * @param actual configuración que se muestra al abrir
     */
    public DialogoConfiguracionIA(JFrame duena, ConfiguracionIA actual) {
        super(duena, "Configurar Asistente de IA", true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.add(crearCabecera(), BorderLayout.NORTH);
        raiz.add(crearFormulario(), BorderLayout.CENTER);
        raiz.add(crearBotones(), BorderLayout.SOUTH);
        setContentPane(raiz);

        cargarValores(actual);
        pack();
        setMinimumSize(new Dimension(620, getHeight()));
        setLocationRelativeTo(duena);
    }

    /** @return true si el usuario guardó la configuración */
    public boolean seGuardo() {
        return guardado;
    }

    // ==================================================================
    // Construcción
    // ==================================================================

    private JComponent crearCabecera() {
        JPanel cabecera = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Tema.PRIMARIO, getWidth(), getHeight(), Tema.SECUNDARIO));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        cabecera.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Asistente de IA");
        titulo.setFont(Tema.negrita(19f));
        titulo.setForeground(Color.WHITE);
        JLabel subtitulo = new JLabel("La IA no calcula: solo explica los pasos que obtiene el motor Java.");
        subtitulo.setFont(Tema.texto(13f));
        subtitulo.setForeground(new Color(224, 231, 255));
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(3));
        textos.add(subtitulo);
        cabecera.add(textos, BorderLayout.CENTER);
        return cabecera;
    }

    private JComponent crearFormulario() {
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.setBorder(BorderFactory.createEmptyBorder(18, 20, 6, 20));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;

        // Proveedor
        c.gridy = 0;
        c.insets = new Insets(0, 0, 6, 0);
        formulario.add(PanelTarjeta.crearTitulo("Proveedor"), c);
        selectorProveedor = new GrupoSegmentado("Sin conexión", "Claude", "OpenAI", "Ollama (local)");
        selectorProveedor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarSegunProveedor();
            }
        });
        c.gridy = 1;
        c.insets = new Insets(0, 0, 10, 0);
        formulario.add(selectorProveedor, c);

        textoAyuda = new JLabel();
        textoAyuda.setFont(Tema.texto(13f));
        textoAyuda.setForeground(Tema.TEXTO_SECUNDARIO);
        textoAyuda.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Tema.PRIMARIO),
                BorderFactory.createEmptyBorder(8, 12, 8, 8)));
        textoAyuda.setOpaque(true);
        textoAyuda.setBackground(Tema.PRIMARIO_SUAVE);
        c.gridy = 2;
        c.insets = new Insets(0, 0, 16, 0);
        formulario.add(textoAyuda, c);

        // Clave
        etiquetaClave = PanelTarjeta.crearTitulo("Clave de API");
        c.gridy = 3;
        c.insets = new Insets(0, 0, 6, 0);
        formulario.add(etiquetaClave, c);

        JPanel filaClave = new JPanel(new BorderLayout(10, 0));
        filaClave.setOpaque(false);
        campoClave = new CampoClave(30);
        filaClave.add(campoClave, BorderLayout.CENTER);
        casillaMostrar = new JCheckBox("Mostrar");
        casillaMostrar.setOpaque(false);
        casillaMostrar.setFont(Tema.texto(13f));
        casillaMostrar.setForeground(Tema.TEXTO_SECUNDARIO);
        casillaMostrar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                campoClave.setClaveVisible(casillaMostrar.isSelected());
            }
        });
        filaClave.add(casillaMostrar, BorderLayout.EAST);
        c.gridy = 4;
        c.insets = new Insets(0, 0, 14, 0);
        formulario.add(filaClave, c);

        // Modelo, URL y tiempo en una fila
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints f = new GridBagConstraints();
        f.fill = GridBagConstraints.HORIZONTAL;
        f.anchor = GridBagConstraints.WEST;

        campoModelo = new CampoModerno("", 14);
        campoModelo.setFont(Tema.mono(13.5f));
        campoUrl = new CampoModerno("", 18);
        campoUrl.setFont(Tema.mono(13.5f));
        campoTiempo = new CampoModerno("30", 4);

        f.gridy = 0;
        f.insets = new Insets(0, 0, 6, 12);
        f.gridx = 0;
        f.weightx = 0.4;
        fila.add(PanelTarjeta.crearTitulo("Modelo (opcional)"), f);
        f.gridx = 1;
        f.weightx = 0.6;
        fila.add(PanelTarjeta.crearTitulo("URL base (opcional)"), f);
        f.gridx = 2;
        f.weightx = 0.0;
        f.insets = new Insets(0, 0, 6, 0);
        fila.add(PanelTarjeta.crearTitulo("Espera (s)"), f);

        f.gridy = 1;
        f.insets = new Insets(0, 0, 0, 12);
        f.gridx = 0;
        f.weightx = 0.4;
        fila.add(campoModelo, f);
        f.gridx = 1;
        f.weightx = 0.6;
        fila.add(campoUrl, f);
        f.gridx = 2;
        f.weightx = 0.0;
        f.insets = new Insets(0, 0, 0, 0);
        fila.add(campoTiempo, f);

        c.gridy = 5;
        c.insets = new Insets(0, 0, 14, 0);
        formulario.add(fila, c);

        // Estado
        etiquetaEstado = new JLabel(" ");
        etiquetaEstado.setFont(Tema.negrita(13f));
        etiquetaEstado.setForeground(Tema.TEXTO_SECUNDARIO);
        c.gridy = 6;
        c.insets = new Insets(0, 0, 0, 0);
        formulario.add(etiquetaEstado, c);

        if (ConfiguracionIA.hayClaveEnEntorno()) {
            JLabel aviso = new JLabel("<html>Nota: la variable de entorno " + ConfiguracionIA.VARIABLE_ENTORNO_CLAVE
                    + " está definida y tiene prioridad sobre la clave guardada aquí.</html>");
            aviso.setFont(Tema.texto(12f));
            aviso.setForeground(Tema.ADVERTENCIA);
            c.gridy = 7;
            c.insets = new Insets(8, 0, 0, 0);
            formulario.add(aviso, c);
        }

        contenedor.add(formulario, BorderLayout.NORTH);
        return contenedor;
    }

    private JComponent crearBotones() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setOpaque(false);
        barra.setBorder(BorderFactory.createEmptyBorder(10, 20, 18, 20));

        botonProbar = new BotonModerno("Probar conexión", BotonModerno.Estilo.SECUNDARIO);
        botonProbar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                probarConexion();
            }
        });
        barra.add(botonProbar, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        BotonModerno botonCancelar = new BotonModerno("Cancelar", BotonModerno.Estilo.FANTASMA);
        botonCancelar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        botonGuardar = new BotonModerno("Guardar", BotonModerno.Estilo.PRIMARIO);
        botonGuardar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guardar();
            }
        });
        derecha.add(botonCancelar);
        derecha.add(botonGuardar);
        barra.add(derecha, BorderLayout.EAST);
        getRootPane().setDefaultButton(botonGuardar);
        return barra;
    }

    // ==================================================================
    // Lógica
    // ==================================================================

    private void cargarValores(ConfiguracionIA actual) {
        int indice = 0;
        for (int i = 0; i < PROVEEDORES.length; i++) {
            if (PROVEEDORES[i].equals(actual.getProveedor())) {
                indice = i;
            }
        }
        selectorProveedor.setSeleccionado(indice);
        campoClave.setText(actual.getApiKey());
        campoModelo.setText(actual.getModelo());
        campoUrl.setText(actual.getUrlBase());
        campoTiempo.setText(String.valueOf(actual.getTiempoEsperaSegundos()));
        actualizarSegunProveedor();
    }

    /** Cambia los textos de ayuda y habilita los campos según el proveedor. */
    private void actualizarSegunProveedor() {
        String proveedor = PROVEEDORES[selectorProveedor.getSeleccionado()];
        boolean usaClave = proveedor.equals(ConfiguracionIA.PROVEEDOR_ANTHROPIC)
                || proveedor.equals(ConfiguracionIA.PROVEEDOR_OPENAI);
        boolean esLocal = proveedor.equals(ConfiguracionIA.PROVEEDOR_LOCAL);

        campoClave.setEnabled(usaClave);
        casillaMostrar.setEnabled(usaClave);
        etiquetaClave.setEnabled(usaClave);
        campoModelo.setEnabled(!esLocal);
        campoUrl.setEnabled(!esLocal);
        campoTiempo.setEnabled(!esLocal);
        botonProbar.setEnabled(!esLocal);

        ConfiguracionIA ejemplo = new ConfiguracionIA(proveedor, "", "", "", 30);
        campoModelo.setTextoAyuda(ejemplo.getModeloEfectivo());
        campoUrl.setTextoAyuda(ejemplo.getUrlBaseEfectiva());

        String ayuda;
        if (proveedor.equals(ConfiguracionIA.PROVEEDOR_ANTHROPIC)) {
            ayuda = "<b>Claude (Anthropic).</b> Cree su clave en <b>console.anthropic.com</b> &rarr; API Keys. "
                    + "Empieza con <tt>sk-ant-</tt>.";
        } else if (proveedor.equals(ConfiguracionIA.PROVEEDOR_OPENAI)) {
            ayuda = "<b>OpenAI.</b> Cree su clave en <b>platform.openai.com</b> &rarr; API keys. "
                    + "Empieza con <tt>sk-</tt>.";
        } else if (proveedor.equals(ConfiguracionIA.PROVEEDOR_OLLAMA)) {
            ayuda = "<b>Ollama:</b> gratis y sin clave. Instale Ollama desde <b>ollama.com</b> y ejecute "
                    + "<tt>ollama pull llama3.2</tt>. Debe estar abierto mientras usa la calculadora.";
        } else {
            ayuda = "<b>Sin conexión.</b> Se usa el Explicador Local: funciona sin Internet, "
                    + "pero no responde preguntas abiertas.";
        }
        textoAyuda.setText("<html><div style='width:520px'>" + ayuda + "</div></html>");
        etiquetaEstado.setText(" ");
        pack();
    }

    /** Construye una configuración con lo escrito en el formulario. */
    private ConfiguracionIA leerFormulario() {
        String proveedor = PROVEEDORES[selectorProveedor.getSeleccionado()];
        int tiempo = 30;
        try {
            tiempo = Integer.parseInt(campoTiempo.getText().trim());
        } catch (NumberFormatException e) {
            tiempo = 30;
        }
        if (tiempo <= 0) {
            tiempo = 30;
        }
        String clave = new String(campoClave.getPassword());
        boolean usaClave = proveedor.equals(ConfiguracionIA.PROVEEDOR_ANTHROPIC)
                || proveedor.equals(ConfiguracionIA.PROVEEDOR_OPENAI);
        if (!usaClave) {
            clave = "";
        }
        return new ConfiguracionIA(proveedor, clave, campoModelo.getText(), campoUrl.getText(), tiempo);
    }

    /** Envía una consulta corta al proveedor en segundo plano. */
    private void probarConexion() {
        final ConfiguracionIA configuracion = leerFormulario();
        boolean usaClave = configuracion.getProveedor().equals(ConfiguracionIA.PROVEEDOR_ANTHROPIC)
                || configuracion.getProveedor().equals(ConfiguracionIA.PROVEEDOR_OPENAI);
        if (usaClave && !configuracion.hayClave()) {
            mostrarEstado("Escriba la clave de API antes de probar.", Tema.ADVERTENCIA);
            return;
        }
        botonProbar.setEnabled(false);
        botonProbar.setText("Probando...");
        mostrarEstado("Conectando con el proveedor...", Tema.TEXTO_SECUNDARIO);

        SwingWorker<String, Void> trabajador = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws IAException {
                return TutorIA.probarConexion(configuracion);
            }

            @Override
            protected void done() {
                try {
                    String respuesta = get();
                    mostrarEstado("Conexión correcta. Respuesta: " + respuesta, Tema.EXITO);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    mostrarEstado("Error: " + causa.getMessage(), Tema.ERROR);
                } finally {
                    botonProbar.setText("Probar conexión");
                    botonProbar.setEnabled(true);
                }
            }
        };
        trabajador.execute();
    }

    private void guardar() {
        ConfiguracionIA configuracion = leerFormulario();
        try {
            File archivo = configuracion.guardar();
            guardado = true;
            mostrarEstado("Guardado en " + archivo.getName(), Tema.EXITO);
            dispose();
        } catch (IOException e) {
            mostrarEstado("No se pudo guardar config.properties: " + e.getMessage(), Tema.ERROR);
        }
    }

    private void mostrarEstado(String texto, Color color) {
        String recortado = texto;
        if (recortado.length() > 140) {
            recortado = recortado.substring(0, 137) + "...";
        }
        etiquetaEstado.setText("<html><div style='width:520px'>" + escapar(recortado) + "</div></html>");
        etiquetaEstado.setForeground(color);
        pack();
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
