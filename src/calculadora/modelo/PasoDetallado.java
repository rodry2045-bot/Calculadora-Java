package calculadora.modelo;

/**
 * Un paso del procedimiento con su estructura completa: qué regla se aplicó,
 * a qué expresión, con qué fórmula y qué resultado se obtuvo.
 * <p>
 * La profundidad indica el nivel de anidamiento: un paso de profundidad 1
 * está "dentro" del paso anterior de profundidad 0 (por ejemplo, la derivada
 * de un factor dentro de la regla del producto).
 * </p>
 */
public class PasoDetallado {

    private final ReglaCalculo regla;
    private final String titulo;
    private final int profundidad;
    private String expresion;
    private String detalle;
    private String resultado;
    private boolean destacado;

    /**
     * @param regla       regla aplicada
     * @param titulo      título visible del paso
     * @param profundidad nivel de anidamiento (0 = principal)
     */
    public PasoDetallado(ReglaCalculo regla, String titulo, int profundidad) {
        this.regla = regla;
        this.titulo = titulo;
        this.profundidad = profundidad;
        this.expresion = "";
        this.detalle = "";
        this.resultado = "";
    }

    /** @return regla aplicada */
    public ReglaCalculo getRegla() {
        return regla;
    }

    /** @return título del paso */
    public String getTitulo() {
        return titulo;
    }

    /** @return nivel de anidamiento */
    public int getProfundidad() {
        return profundidad;
    }

    /** @return expresión sobre la que se aplica la regla */
    public String getExpresion() {
        return expresion;
    }

    /** @param expresion expresión sobre la que se aplica la regla */
    public void setExpresion(String expresion) {
        this.expresion = expresion;
    }

    /** @return fórmula o explicación */
    public String getDetalle() {
        return detalle;
    }

    /** @param detalle fórmula o explicación */
    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    /** @return resultado parcial del paso */
    public String getResultado() {
        return resultado;
    }

    /** @param resultado resultado parcial del paso */
    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    /** @return true si el paso usa el método elegido por el estudiante */
    public boolean isDestacado() {
        return destacado;
    }

    /** @param destacado true para resaltar el paso */
    public void setDestacado(boolean destacado) {
        this.destacado = destacado;
    }

    /**
     * @return el paso en una sola línea de texto (para la IA y el explicador local)
     */
    public String comoTexto() {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < profundidad; i++) {
            texto.append("  ");
        }
        texto.append(titulo);
        if (!expresion.isEmpty()) {
            texto.append(" [").append(expresion).append("]");
        }
        if (!detalle.isEmpty()) {
            texto.append(": ").append(detalle);
        }
        if (!resultado.isEmpty()) {
            texto.append(". Resultado: ").append(resultado);
        }
        texto.append(".");
        return texto.toString();
    }
}
