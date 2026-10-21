package calculadora.excepciones;

/**
 * Se lanza cuando falla la comunicación con un proveedor de Inteligencia
 * Artificial: no hay red, la clave es incorrecta, se agotó el tiempo de
 * espera o la respuesta no tiene el formato esperado.
 * <p>
 * El {@code TutorIA} captura esta excepción y conmuta automáticamente
 * al {@code ExplicadorLocal}, por lo que el usuario nunca se queda sin
 * explicación.
 * </p>
 */
public class IAException extends CalculadoraException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensaje explicación del fallo
     */
    public IAException(String mensaje) {
        super(mensaje);
    }

    /**
     * @param mensaje explicación del fallo
     * @param causa   excepción original (por ejemplo, un IOException)
     */
    public IAException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
