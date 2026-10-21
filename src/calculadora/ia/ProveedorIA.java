package calculadora.ia;

import calculadora.excepciones.IAException;

/**
 * Contrato común para cualquier proveedor de Inteligencia Artificial.
 * <p>
 * Gracias a esta interfaz (polimorfismo), el {@link TutorIA} puede trabajar
 * con Anthropic, OpenAI, Ollama o el explicador local sin saber cuál es.
 * Agregar un proveedor nuevo solo requiere implementar estos métodos.
 * </p>
 */
public interface ProveedorIA {

    /**
     * @return nombre legible del proveedor, por ejemplo "Anthropic (claude-haiku-5-5)"
     */
    String getNombre();

    /**
     * Envía las instrucciones y la consulta al modelo y devuelve su respuesta.
     *
     * @param instruccionSistema rol e instrucciones generales para el modelo
     * @param mensajeUsuario     consulta concreta (contiene los pasos del motor)
     * @return texto de la respuesta
     * @throws IAException si no hay red, la clave es inválida o la respuesta es incorrecta
     */
    String generarRespuesta(String instruccionSistema, String mensajeUsuario) throws IAException;
}
