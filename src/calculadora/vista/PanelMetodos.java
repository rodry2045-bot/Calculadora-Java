package calculadora.vista;

import calculadora.modelo.MetodoCalculo;
import calculadora.vista.componentes.BarraDesplazamientoModerna;
import calculadora.vista.componentes.BotonModerno;
import calculadora.vista.componentes.GrupoSegmentado;
import calculadora.vista.componentes.PanelTarjeta;
import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Columna izquierda con la LISTA DE MÉTODOS: un selector "Derivadas /
 * Integrales", la lista de leyes y reglas, y una tarjeta con la fórmula,
 * la explicación y un botón para cargar un ejemplo.
 */
public class PanelMetodos extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GrupoSegmentado selectorCategoria;
    private final DefaultListModel<MetodoCalculo> modelo;
    private final JList<MetodoCalculo> lista;
    private final JLabel etiquetaNombre;
    private final JTextArea textoFormula;
    private final JTextArea textoDescripcion;
    private final BotonModerno botonEjemplo;
    private final transient List<ActionListener> oyentesCambio;
    private final transient List<ActionListener> oyentesEjemplo;

    /** Construye el panel con la categoría Derivadas seleccionada. */
    public PanelMetodos() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        oyentesCambio = new ArrayList<ActionListener>();
        oyentesEjemplo = new ArrayList<ActionListener>();

        // ---------------- Tarjeta con la lista ----------------
        PanelTarjeta tarjetaLista = new PanelTarjeta(new BorderLayout(0, 10));
        JPanel superior = new JPanel(new BorderLayout(0, 8));
        superior.setOpaque(false);
        superior.add(PanelTarjeta.crearTitulo("Métodos y leyes"), BorderLayout.NORTH);
        selectorCategoria = new GrupoSegmentado("Derivadas", "Integrales");
        selectorCategoria.setFont(Tema.negrita(13f));
        superior.add(selectorCategoria, BorderLayout.CENTER);
        tarjetaLista.add(superior, BorderLayout.NORTH);

        modelo = new DefaultListModel<MetodoCalculo>();
        lista = new JList<MetodoCalculo>(modelo);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new RenderizadorMetodo());
        lista.setOpaque(false);
        lista.setBackground(Tema.SUPERFICIE);
        lista.setFixedCellHeight(-1);
        lista.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JScrollPane desplazamiento = new JScrollPane(lista);
        BarraDesplazamientoModerna.aplicar(desplazamiento);
        tarjetaLista.add(desplazamiento, BorderLayout.CENTER);
        add(tarjetaLista, BorderLayout.CENTER);

        // ---------------- Tarjeta de información ----------------
        PanelTarjeta tarjetaInfo = new PanelTarjeta(new BorderLayout(0, 8));
        etiquetaNombre = new JLabel(" ");
        etiquetaNombre.setFont(Tema.negrita(14.5f));
        etiquetaNombre.setForeground(Tema.TEXTO);
        tarjetaInfo.add(etiquetaNombre, BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 8));
        centro.setOpaque(false);
        textoFormula = crearTexto(Tema.mono(13.5f), Tema.PRIMARIO);
        JPanel cajaFormula = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.suavizar(g2);
                g2.setColor(Tema.CAMPO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Tema.PRIMARIO_SUAVE_FUERTE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        cajaFormula.setOpaque(false);
        cajaFormula.setBorder(BorderFactory.createEmptyBorder(9, 11, 9, 11));
        cajaFormula.add(textoFormula, BorderLayout.CENTER);
        centro.add(cajaFormula, BorderLayout.NORTH);
        textoDescripcion = crearTexto(Tema.texto(13f), Tema.TEXTO_SECUNDARIO);
        centro.add(textoDescripcion, BorderLayout.CENTER);
        tarjetaInfo.add(centro, BorderLayout.CENTER);

        botonEjemplo = new BotonModerno("Cargar ejemplo", BotonModerno.Estilo.SECUNDARIO);
        botonEjemplo.setFocusable(false);
        botonEjemplo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                avisar(oyentesEjemplo);
            }
        });
        tarjetaInfo.add(botonEjemplo, BorderLayout.SOUTH);
        add(tarjetaInfo, BorderLayout.SOUTH);

        // ---------------- Eventos ----------------
        selectorCategoria.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                llenarLista();
            }
        });
        lista.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && lista.getSelectedValue() != null) {
                    mostrarInformacion(lista.getSelectedValue());
                    avisar(oyentesCambio);
                }
            }
        });

        setPreferredSize(new Dimension(310, 400));
        llenarLista();
    }

    private JTextArea crearTexto(java.awt.Font fuente, Color color) {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(fuente);
        area.setForeground(color);
        return area;
    }

    private void llenarLista() {
        MetodoCalculo.Categoria categoria = selectorCategoria.getSeleccionado() == 0
                ? MetodoCalculo.Categoria.DERIVADAS : MetodoCalculo.Categoria.INTEGRALES;
        modelo.clear();
        MetodoCalculo[] todos = MetodoCalculo.values();
        for (int i = 0; i < todos.length; i++) {
            if (todos[i].getCategoria() == categoria) {
                modelo.addElement(todos[i]);
            }
        }
        lista.setSelectedIndex(0);
    }

    private void mostrarInformacion(MetodoCalculo metodo) {
        etiquetaNombre.setText(metodo.getNombre());
        textoFormula.setText(metodo.getFormula());
        textoDescripcion.setText(metodo.getDescripcion() + "\nEjemplo: " + metodo.getEjemplo());
    }

    private void avisar(List<ActionListener> oyentes) {
        ActionEvent evento = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "metodo");
        for (int i = 0; i < oyentes.size(); i++) {
            oyentes.get(i).actionPerformed(evento);
        }
    }

    /** @return método seleccionado */
    public MetodoCalculo getSeleccionado() {
        MetodoCalculo metodo = lista.getSelectedValue();
        return metodo == null ? MetodoCalculo.D_AUTOMATICO : metodo;
    }

    /**
     * Selecciona un método (cambiando de categoría si hace falta).
     *
     * @param metodo método a seleccionar
     */
    public void seleccionar(MetodoCalculo metodo) {
        selectorCategoria.setSeleccionado(metodo.getCategoria() == MetodoCalculo.Categoria.DERIVADAS ? 0 : 1);
        lista.setSelectedValue(metodo, true);
    }

    /** @param oyente se ejecuta cuando cambia el método seleccionado */
    public void alCambiarMetodo(ActionListener oyente) {
        oyentesCambio.add(oyente);
    }

    /** @param oyente se ejecuta al pulsar "Cargar ejemplo" */
    public void alCargarEjemplo(ActionListener oyente) {
        oyentesEjemplo.add(oyente);
    }

    /**
     * Dibuja cada método de la lista como una fila redondeada.
     */
    private static class RenderizadorMetodo extends JLabel implements ListCellRenderer<MetodoCalculo> {

        private static final long serialVersionUID = 1L;
        private boolean seleccionado;

        RenderizadorMetodo() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 10));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MetodoCalculo> lista, MetodoCalculo valor,
                                                      int indice, boolean estaSeleccionado, boolean tieneFoco) {
            setText(valor.getNombre());
            seleccionado = estaSeleccionado;
            boolean automatico = valor == MetodoCalculo.D_AUTOMATICO || valor == MetodoCalculo.I_AUTOMATICO;
            setFont(automatico || estaSeleccionado ? Tema.negrita(13f) : Tema.texto(13f));
            setForeground(estaSeleccionado ? Tema.PRIMARIO : (automatico ? Tema.TEXTO : Tema.TEXTO_SECUNDARIO));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (seleccionado) {
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.suavizar(g2);
                g2.setColor(Tema.PRIMARIO_SUAVE);
                g2.fillRoundRect(2, 1, getWidth() - 4, getHeight() - 2, 10, 10);
                g2.setColor(Tema.PRIMARIO);
                g2.fillRoundRect(2, 7, 3, getHeight() - 14, 3, 3);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
