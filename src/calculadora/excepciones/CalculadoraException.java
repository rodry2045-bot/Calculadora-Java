package calculadora.excepciones;

/**
 * Excepción base de toda la aplicación.
 * <p>
 * Todas las excepciones propias de la calculadora heredan de esta clase.
 * De esta forma, la interfaz gráfica puede capturar un único tipo
 * ({@code CalculadoraException}) y mostrar un mensaje amable al usuario.
 * </p>
 * Es una excepción "comprobada" (checked): el compilador obliga a
 * declararla o capturarla, lo cual es didáctico porque hace visible
 * en qué métodos puede fallar un cálculo.
 */
public class CalculadoraException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje descriptivo en español.
     *
     * @param mensaje explicación del problema
     */
    public CalculadoraException(String mensaje) {
        super(mensaje);
    }

    /**
     * Crea la excepción con un mensaje y la causa original.
     *
     * @param mensaje explicación del problema
     * @param causa   excepción que originó el problema
     */
    public CalculadoraException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
