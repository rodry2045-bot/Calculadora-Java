package calculadora.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Guarda TODO lo que ocurrió en un cálculo: la operación, la expresión de
 * entrada, el resultado, la lista de pasos y los datos para graficar.
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
        /** Derivada de orden n. */
        DERIVADA_ORDEN_SUPERIOR("Derivada de orden superior"),
        /** dy/dx a partir de una ecuación. */
        DERIVADA_IMPLICITA("Derivada implícita"),
        /** Límite (con regla de L'Hôpital si hace falta). */
        LIMITE("Límite"),
        /** Integral indefinida (antiderivada + C). */
        INTEGRAL_INDEFINIDA("Integral indefinida"),
        /** Integral definida en [a, b]. */
        INTEGRAL_DEFINIDA("Integral definida"),
        /** Recta tangente y normal en un punto. */
        RECTA_TANGENTE("Recta tangente"),
        /** Máximos, mínimos y puntos de inflexión. */
        EXTREMOS("Máximos y mínimos"),
        /** Área entre dos curvas. */
        AREA_ENTRE_CURVAS("Área entre curvas"),
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
    private final Graficable expresion;
    private final Variable variable;
    private final List<String> pasos;
    private final List<PasoDetallado> pasosDetallados;
    private final List<String> notas;

    private Graficable expresionResultado;
    private String textoResultado;
    private Double valorNumerico;
    private Double limiteInferior;
    private Double limiteSuperior;
    private double valorOtraVariable;
    private String nombreMetodo;

    // Datos para la gráfica
    private Graficable curvaImplicita;
    private Double puntoX;
    private Double puntoY;
    private String etiquetaSecundaria;
    private final List<double[]> puntosMarcados = new ArrayList<double[]>();
    private final List<String> etiquetasPuntos = new ArrayList<String>();
    private Graficable curvaBaseArea;

    /**
     * @param tipo          operación realizada
     * @param textoOriginal texto tal como lo escribió el usuario
     * @param expresion     expresión interpretada
     * @param variable      variable de trabajo
     */
    public ResultadoCalculo(TipoOperacion tipo, String textoOriginal, Graficable expresion, Variable variable) {
        this.tipo = tipo;
        this.textoOriginal = textoOriginal;
        this.expresion = expresion;
        this.variable = variable;
        this.pasos = new ArrayList<String>();
        this.pasosDetallados = new ArrayList<PasoDetallado>();
        this.notas = new ArrayList<String>();
        this.valorOtraVariable = 1.0;
        this.nombreMetodo = "";
    }

    /**
     * Registra un paso del procedimiento como texto simple.
     *
     * @param descripcion texto del paso
     */
    public void agregarPaso(String descripcion) {
        pasos.add(descripcion);
    }

    /**
     * Registra un paso con estructura completa (y su versión en texto).
     *
     * @param paso paso detallado
     */
    public void agregarPasoDetallado(PasoDetallado paso) {
        pasosDetallados.add(paso);
    }

    /**
     * Copia una lista de pasos detallados (generados por el motor simbólico).
     *
     * @param lista pasos a copiar
     */
    public void agregarPasosDetallados(List<PasoDetallado> lista) {
        for (int i = 0; i < lista.size(); i++) {
            agregarPasoDetallado(lista.get(i));
        }
    }

    /**
     * Agrega una nota o advertencia para el estudiante.
     *
     * @param nota texto de la nota
     */
    public void agregarNota(String nota) {
        notas.add(nota);
    }

    /**
     * Genera un informe numerado con todos los pasos, listo para mostrar
     * o para enviarlo al Asistente de IA.
     *
     * @return texto con un paso por línea
     */
    public String generarInformePasos() {
        StringBuilder informe = new StringBuilder();
        informe.append("Operación: ").append(tipo.getNombre()).append('\n');
        if (!nombreMetodo.isEmpty()) {
            informe.append("Método elegido: ").append(nombreMetodo).append('\n');
        }
        informe.append("Expresión: ").append(expresion).append('\n');
        if (variable != null) {
            informe.append("Variable de trabajo: ").append(variable.getSimbolo()).append('\n');
        }
        informe.append('\n').append("PROCEDIMIENTO PASO A PASO").append('\n');
        List<String> lineas = getPasos();
        for (int i = 0; i < lineas.size(); i++) {
            informe.append(i + 1).append(". ").append(lineas.get(i)).append('\n');
        }
        for (int i = 0; i < notas.size(); i++) {
            informe.append("Nota: ").append(notas.get(i)).append('\n');
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
    public Graficable getExpresion() {
        return expresion;
    }

    /** @return variable de trabajo */
    public Variable getVariable() {
        return variable;
    }

    /**
     * Devuelve los pasos como texto. Si el cálculo tiene pasos detallados,
     * se convierten a texto; si no, se devuelven los pasos simples.
     *
     * @return lista no modificable de pasos
     */
    public List<String> getPasos() {
        if (pasosDetallados.isEmpty()) {
            return Collections.unmodifiableList(pasos);
        }
        List<String> lineas = new ArrayList<String>();
        for (int i = 0; i < pasosDetallados.size(); i++) {
            lineas.add(pasosDetallados.get(i).comoTexto());
        }
        return Collections.unmodifiableList(lineas);
    }

    /** @return pasos detallados (puede estar vacía en el motor por términos) */
    public List<PasoDetallado> getPasosDetallados() {
        return Collections.unmodifiableList(pasosDetallados);
    }

    /** @return notas y advertencias */
    public List<String> getNotas() {
        return Collections.unmodifiableList(notas);
    }

    /** @return expresión resultado (derivada o antiderivada) o null */
    public Graficable getExpresionResultado() {
        return expresionResultado;
    }

    /** @param expresionResultado expresión resultado */
    public void setExpresionResultado(Graficable expresionResultado) {
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

    /** @return valor numérico (integral definida, límite o evaluación) o null */
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
     * @param a límite inferior (puede ser infinito)
     * @param b límite superior (puede ser infinito)
     */
    public void setLimites(double a, double b) {
        this.limiteInferior = Double.valueOf(a);
        this.limiteSuperior = Double.valueOf(b);
    }

    /** @return valor fijo de la otra variable (para graficar) */
    public double getValorOtraVariable() {
        return valorOtraVariable;
    }

    /** @param valorOtraVariable valor fijo de la otra variable */
    public void setValorOtraVariable(double valorOtraVariable) {
        this.valorOtraVariable = valorOtraVariable;
    }

    /** @return nombre del método elegido */
    public String getNombreMetodo() {
        return nombreMetodo;
    }

    /** @param nombreMetodo nombre del método elegido */
    public void setNombreMetodo(String nombreMetodo) {
        this.nombreMetodo = nombreMetodo;
    }

    /** @return función F(x, y) cuya curva F = 0 se dibuja (derivación implícita) o null */
    public Graficable getCurvaImplicita() {
        return curvaImplicita;
    }

    /** @param curvaImplicita función F(x, y) de la ecuación F = 0 */
    public void setCurvaImplicita(Graficable curvaImplicita) {
        this.curvaImplicita = curvaImplicita;
    }

    /** @return coordenada x del punto a marcar, o null */
    public Double getPuntoX() {
        return puntoX;
    }

    /** @return coordenada y del punto a marcar, o null */
    public Double getPuntoY() {
        return puntoY;
    }

    /**
     * @param x coordenada horizontal del punto a marcar
     * @param y coordenada vertical del punto a marcar
     */
    public void setPunto(double x, double y) {
        this.puntoX = Double.valueOf(x);
        this.puntoY = Double.valueOf(y);
    }

    /** @return leyenda de la curva secundaria, o null */
    public String getEtiquetaSecundaria() {
        return etiquetaSecundaria;
    }

    /** @param etiquetaSecundaria leyenda de la curva secundaria */
    public void setEtiquetaSecundaria(String etiquetaSecundaria) {
        this.etiquetaSecundaria = etiquetaSecundaria;
    }

    /**
     * Agrega un punto destacado para la gráfica (máximo, mínimo, inflexión...).
     *
     * @param x        abscisa
     * @param y        ordenada
     * @param etiqueta texto corto que se dibuja junto al punto
     */
    public void agregarPuntoMarcado(double x, double y, String etiqueta) {
        puntosMarcados.add(new double[]{x, y});
        etiquetasPuntos.add(etiqueta);
    }

    /** @return puntos destacados (copia) */
    public List<double[]> getPuntosMarcados() {
        return new ArrayList<double[]>(puntosMarcados);
    }

    /** @return etiquetas de los puntos destacados (copia) */
    public List<String> getEtiquetasPuntos() {
        return new ArrayList<String>(etiquetasPuntos);
    }

    /** @return curva inferior del área sombreada (área entre curvas) o null */
    public Graficable getCurvaBaseArea() {
        return curvaBaseArea;
    }

    /** @param curva curva inferior del área sombreada */
    public void setCurvaBaseArea(Graficable curva) {
        this.curvaBaseArea = curva;
    }
}
