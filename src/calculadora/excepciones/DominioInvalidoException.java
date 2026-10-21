package calculadora.excepciones;

/**
 * Se lanza cuando un cálculo numérico sale del dominio de la función.
 * <p>
 * Ejemplos: evaluar 1/x en x = 0, integrar 1/x en un intervalo que
 * contiene al 0 (integral impropia) o evaluar una raíz par de un número
 * negativo (resultado no real).
 * </p>
 */
public class DominioInvalidoException extends CalculadoraException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación del problema de dominio
     */
    public DominioInvalidoException(String mensaje) {
        super(mensaje);
    }
}
