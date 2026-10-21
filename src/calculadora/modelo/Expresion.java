package calculadora.modelo;

import calculadora.excepciones.CalculadoraException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una expresión es una SUMA de términos: t1 + t2 + ... + tn.
 * <p>
 * Gracias a la Regla de la Suma/Resta, derivar o integrar una expresión
 * consiste en derivar o integrar cada término de forma independiente y
 * conservar los signos (que están incluidos en cada coeficiente).
 * </p>
 */
public class Expresion {

    /** Lista de términos que forman la expresión. */
    private final List<Termino> terminos;

    /** Crea una expresión vacía (equivale a 0). */
    public Expresion() {
        this.terminos = new ArrayList<Termino>();
    }

    /**
     * Agrega un término al final de la expresión.
     *
     * @param termino término a agregar
     */
    public void agregar(Termino termino) {
        terminos.add(termino);
    }

    /**
     * @return lista NO modificable con los términos
     */
    public List<Termino> getTerminos() {
        return Collections.unmodifiableList(terminos);
    }

    /** @return cantidad de términos */
    public int cantidadTerminos() {
        return terminos.size();
    }

    /** @return true si la expresión no tiene términos (vale 0) */
    public boolean estaVacia() {
        return terminos.isEmpty();
    }

    /**
     * Evalúa la expresión sumando el valor de cada término.
     *
     * @param x valor de x
     * @param y valor de y
     * @return valor numérico de la expresión
     */
    public double evaluar(double x, double y) {
        double suma = 0.0;
        for (int i = 0; i < terminos.size(); i++) {
            Termino termino = terminos.get(i);
            suma = suma + termino.evaluar(x, y);
        }
        return suma;
    }

    /**
     * Evalúa la expresión como función de una sola variable.
     *
     * @param variable      variable que toma el valor indicado
     * @param valor         valor de esa variable
     * @param valorOtra     valor fijo de la otra variable
     * @return valor numérico
     */
    public double evaluarEn(Variable variable, double valor, double valorOtra) {
        if (variable == Variable.X) {
            return evaluar(valor, valorOtra);
        }
        return evaluar(valorOtra, valor);
    }

    /**
     * @param v variable consultada
     * @return true si algún término depende de v
     */
    public boolean dependeDe(Variable v) {
        for (int i = 0; i < terminos.size(); i++) {
            if (terminos.get(i).dependeDe(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deriva término a término (Regla de la Suma/Resta) y simplifica.
     *
     * @param v variable de derivación
     * @return expresión derivada
     * @throws CalculadoraException si algún término no se puede derivar
     */
    public Expresion derivar(Variable v) throws CalculadoraException {
        Expresion resultado = new Expresion();
        for (int i = 0; i < terminos.size(); i++) {
            Termino derivado = terminos.get(i).derivar(v);
            resultado.agregar(derivado);
        }
        return resultado.simplificar();
    }

    /**
     * Integra término a término (sin la constante C) y simplifica.
     *
     * @param v variable de integración
     * @return antiderivada
     * @throws CalculadoraException si algún término no se puede integrar
     */
    public Expresion integrar(Variable v) throws CalculadoraException {
        Expresion resultado = new Expresion();
        for (int i = 0; i < terminos.size(); i++) {
            Termino integrado = terminos.get(i).integrar(v);
            resultado.agregar(integrado);
        }
        return resultado.simplificar();
    }

    /**
     * Agrupa términos semejantes sumando sus coeficientes y elimina los
     * términos cuyo coeficiente es cero.
     * <p>
     * Ejemplo: 3x^2 + 2x - x^2 + 0 se convierte en 2x^2 + 2x.
     * </p>
     *
     * @return una expresión nueva ya simplificada
     */
    public Expresion simplificar() {
        List<Termino> agrupados = new ArrayList<Termino>();

        for (int i = 0; i < terminos.size(); i++) {
            Termino actual = terminos.get(i);
            String firmaActual = actual.firmaSemejanza();

            // Buscar si ya existe un término semejante en la lista agrupada
            int indiceSemejante = -1;
            for (int j = 0; j < agrupados.size(); j++) {
                if (agrupados.get(j).firmaSemejanza().equals(firmaActual)) {
                    indiceSemejante = j;
                    break;
                }
            }

            if (indiceSemejante == -1) {
                agrupados.add(actual);
            } else {
                Termino existente = agrupados.get(indiceSemejante);
                double sumaCoeficientes = existente.getCoeficiente() + actual.getCoeficiente();
                Termino combinado = existente.conParteAlgebraica(sumaCoeficientes,
                        existente.getExponenteX(), existente.getExponenteY());
                agrupados.set(indiceSemejante, combinado);
            }
        }

        // Quitar los términos nulos
        Expresion simplificada = new Expresion();
        for (int i = 0; i < agrupados.size(); i++) {
            Termino termino = agrupados.get(i);
            if (!termino.esCero()) {
                simplificada.agregar(termino);
            }
        }
        return simplificada;
    }

    /**
     * Construye el texto de la expresión cuidando los signos:
     * "3x^2 - 2x + 1" en lugar de "3x^2 + -2x + 1".
     *
     * @return texto de la expresión, o "0" si está vacía
     */
    @Override
    public String toString() {
        if (terminos.isEmpty()) {
            return "0";
        }
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < terminos.size(); i++) {
            String textoTermino = terminos.get(i).toString();
            if (i == 0) {
                texto.append(textoTermino);
            } else if (textoTermino.startsWith("-")) {
                texto.append(" - ").append(textoTermino.substring(1));
            } else {
                texto.append(" + ").append(textoTermino);
            }
        }
        return texto.toString();
    }
}
