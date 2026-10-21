package calculadora.excepciones;

/**
 * Se lanza cuando la expresión es válida, pero la operación pedida
 * requiere una técnica que el motor todavía no implementa
 * (por ejemplo, integración por partes o regla del producto).
 */
public class OperacionNoSoportadaException extends CalculadoraException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación de qué técnica haría falta
     */
    public OperacionNoSoportadaException(String mensaje) {
        super(mensaje);
    }
}
