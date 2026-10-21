package calculadora;

import calculadora.vista.VentanaCalculadora;
import calculadora.vista.estilo.Tema;

import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import java.awt.Color;
import javax.swing.UIManager;

/**
 * Punto de entrada de la aplicación.
 * <p>
 * Toda interfaz Swing debe crearse en el hilo de despacho de eventos
 * (Event Dispatch Thread). Por eso se usa {@link SwingUtilities#invokeLater}.
 * </p>
 */
public class Main {

    /**
     * Inicia la calculadora.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        // Usar la apariencia del sistema operativo (Windows) si está disponible
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("No se pudo aplicar la apariencia del sistema: " + e.getMessage());
        }

        // Estilo de los textos de ayuda emergentes (tooltips)
        UIManager.put("ToolTip.font", Tema.texto(12.5f));
        UIManager.put("ToolTip.background", Tema.TEXTO);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 10, 6, 10));

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VentanaCalculadora ventana = new VentanaCalculadora();
                ventana.setVisible(true);
            }
        });
    }
}
