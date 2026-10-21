package calculadora.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Guarda TODO lo que ocurrió en un cálculo: la operación, la expresión de
 * entrada, el resultado y la lista de pasos con la regla aplicada en cada uno.
 * <p>
 * Este objeto es la "fuente de verdad" del procedimiento: el Asistente de IA
 * no calcula nada, solo explica los pasos que el motor Java registró aquí.
 * </p>
 */
public class ResultadoCalculo {

    /** Tipos de operación que puede realizar la calculadora. */
    public enum TipoOperacion {
        /** Derivada (parcial) respecto a una variable. */
        DERIVADA("Derivada"),
        /** Integral indefinida (antiderivada + C). */
        INTEGRAL_INDEFINIDA("Integral indefinida"),
        /** Integral definida en [a, b]. */
        INTEGRAL_DEFINIDA("Integral definida"),
        /** Evaluación numérica en un punto. */
        EVALUACION("Evaluación");

        private final String nombre;

        TipoOperacion(String nombre) {
            this.nombre = nombre;
        }

        /** @return nombre legible de la operación */
        public String getNombre() {
            return nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private final TipoOperacion tipo;
    private final String textoOriginal;
    private final Expresion expresion;
    private final Variable variable;
    private final List<String> pasos;

    private Expresion expresionResultado;
    private String textoResultado;
    private Double valorNumerico;
    private Double limiteInferior;
    private Double limiteSuperior;
    private double valorOtraVariable;

    /**
     * @param tipo          operación realizada
     * @param textoOriginal texto tal como lo escribió el usuario
     * @param expresion     expresión interpretada
     * @param variable      variable de trabajo
     */
    public ResultadoCalculo(TipoOperacion tipo, String textoOriginal, Expresion expresion, Variable variable) {
        this.tipo = tipo;
        this.textoOriginal = textoOriginal;
        this.expresion = expresion;
        this.variable = variable;
        this.pasos = new ArrayList<String>();
        this.valorOtraVariable = 0.0;
    }

    /**
     * Registra un paso del procedimiento.
     *
     * @param descripcion texto del paso
     */
    public void agregarPaso(String descripcion) {
        pasos.add(descripcion);
    }

    /**
     * Genera un informe numerado con todos los pasos, listo para mostrar.
     *
     * @return texto con un paso por línea
     */
    public String generarInformePasos() {
        StringBuilder informe = new StringBuilder();
        informe.append("Operación: ").append(tipo.getNombre()).append('\n');
        informe.append("Expresión: ").append(expresion).append('\n');
        if (variable != null) {
            informe.append("Variable de trabajo: ").append(variable.getSimbolo()).append('\n');
        }
        informe.append('\n').append("PROCEDIMIENTO PASO A PASO").append('\n');
        for (int i = 0; i < pasos.size(); i++) {
            informe.append(i + 1).append(". ").append(pasos.get(i)).append('\n');
        }
        informe.append('\n').append("RESULTADO: ").append(textoResultado).append('\n');
        return informe.toString();
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    /** @return tipo de operación */
    public TipoOperacion getTipo() {
        return tipo;
    }

    /** @return texto escrito por el usuario */
    public String getTextoOriginal() {
        return textoOriginal;
    }

    /** @return expresión interpretada */
    public Expresion getExpresion() {
        return expresion;
    }

    /** @return variable de trabajo */
    public Variable getVariable() {
        return variable;
    }

    /** @return lista no modificable de pasos */
    public List<String> getPasos() {
        return Collections.unmodifiableList(pasos);
    }

    /** @return expresión resultado (derivada o antiderivada) o null */
    public Expresion getExpresionResultado() {
        return expresionResultado;
    }

    /** @param expresionResultado expresión resultado */
    public void setExpresionResultado(Expresion expresionResultado) {
        this.expresionResultado = expresionResultado;
    }

    /** @return resultado en texto, listo para mostrar */
    public String getTextoResultado() {
        return textoResultado;
    }

    /** @param textoResultado resultado en texto */
    public void setTextoResultado(String textoResultado) {
        this.textoResultado = textoResultado;
    }

    /** @return valor numérico (integral definida o evaluación) o null */
    public Double getValorNumerico() {
        return valorNumerico;
    }

    /** @param valorNumerico valor numérico del resultado */
    public void setValorNumerico(Double valorNumerico) {
        this.valorNumerico = valorNumerico;
    }

    /** @return límite inferior a, o null */
    public Double getLimiteInferior() {
        return limiteInferior;
    }

    /** @return límite superior b, o null */
    public Double getLimiteSuperior() {
        return limiteSuperior;
    }

    /**
     * @param a límite inferior
     * @param b límite superior
     */
    public void setLimites(double a, double b) {
        this.limiteInferior = Double.valueOf(a);
        this.limiteSuperior = Double.valueOf(b);
    }

    /** @return valor fijo de la otra variable */
    public double getValorOtraVariable() {
        return valorOtraVariable;
    }

    /** @param valorOtraVariable valor fijo de la otra variable */
    public void setValorOtraVariable(double valorOtraVariable) {
        this.valorOtraVariable = valorOtraVariable;
    }
}
