package calculadora.excepciones;

/**
 * Se lanza cuando el texto escrito por el usuario no es una expresión
 * matemática válida (por ejemplo: paréntesis sin cerrar, símbolos
 * desconocidos o una función mal escrita).
 */
public class ExpresionInvalidaException extends CalculadoraException {

    private static final long serialVersionUID = 1L;

    /** Posición (índice del carácter) donde se detectó el error, o -1 si no aplica. */
    private final int posicion;

    /**
     * Crea la excepción sin posición conocida.
     *
     * @param mensaje explicación del error
     */
    public ExpresionInvalidaException(String mensaje) {
        super(mensaje);
        this.posicion = -1;
    }

    /**
     * Crea la excepción indicando la posición del error dentro del texto.
     *
     * @param mensaje  explicación del error
     * @param posicion índice del carácter donde ocurrió el error (comenzando en 0)
     */
    public ExpresionInvalidaException(String mensaje, int posicion) {
        super(mensaje + " (posición " + (posicion + 1) + ")");
        this.posicion = posicion;
    }

    /**
     * @return la posición del error (comenzando en 0) o -1 si no se conoce
     */
    public int getPosicion() {
        return posicion;
    }
}
