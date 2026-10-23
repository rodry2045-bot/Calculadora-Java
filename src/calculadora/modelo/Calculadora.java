package calculadora.modelo;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.simbolico.AnalizadorSimbolico;
import calculadora.simbolico.MotorSimbolico;
import calculadora.simbolico.Nodo;
import calculadora.simbolico.Simplificador;

/**
 * FACHADA del motor matemático (patrón de diseño Fachada / Facade).
 * <p>
 * La interfaz gráfica y las pruebas solo necesitan conocer esta clase. Por
 * dentro usa el motor simbólico (árbol de expresión), que interpreta el texto,
 * aplica las reglas del método elegido y registra cada paso en un
 * {@link ResultadoCalculo}.
 * </p>
 */
public class Calculadora {

    /** Motor simbólico: deriva, integra, calcula límites y aplicaciones. */
    private final MotorSimbolico motorSimbolico;

    /** Crea la calculadora con su propio motor. */
    public Calculadora() {
        this.motorSimbolico = new MotorSimbolico();
    }

    /**
     * Resuelve un ejercicio con el método elegido en la lista (derivadas,
     * límites, integrales y aplicaciones).
     *
     * @param metodo  método elegido
     * @param entrada datos escritos por el estudiante
     * @return resultado con el procedimiento
     * @throws CalculadoraException si la expresión no es válida o el método no se puede aplicar
     */
    public ResultadoCalculo resolver(MetodoCalculo metodo, EntradaCalculo entrada) throws CalculadoraException {
        return motorSimbolico.resolver(metodo, entrada);
    }

    /**
     * Interpreta un texto y devuelve la expresión simplificada.
     *
     * @param texto expresión escrita por el usuario
     * @return árbol de la expresión, simplificado
     * @throws ExpresionInvalidaException si el texto no es válido
     */
    public Nodo interpretar(String texto) throws ExpresionInvalidaException {
        String limpio = AnalizadorSimbolico.quitarPrefijoFuncion(texto);
        return Simplificador.simplificar(new AnalizadorSimbolico().analizar(limpio));
    }
}
