package calculadora.modelo;

/**
 * Datos que escribe el estudiante en la interfaz (todo como texto; el motor
 * los interpreta y valida).
 */
public class EntradaCalculo {

    private String expresion = "";
    private Variable variable = Variable.X;
    private String orden = "2";
    private String punto = "0";
    private String limiteA = "0";
    private String limiteB = "1";
    private String puntoC = "1";
    private boolean definida;

    /** @return función o ecuación */
    public String getExpresion() {
        return expresion;
    }

    /** @param expresion función o ecuación */
    public void setExpresion(String expresion) {
        this.expresion = expresion;
    }

    /** @return variable de derivación */
    public Variable getVariable() {
        return variable;
    }

    /** @param variable variable de derivación */
    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    /** @return orden de la derivada */
    public String getOrden() {
        return orden;
    }

    /** @param orden orden de la derivada */
    public void setOrden(String orden) {
        this.orden = orden;
    }

    /** @return punto al que tiende x (puede ser ∞) */
    public String getPunto() {
        return punto;
    }

    /** @param punto punto al que tiende x */
    public void setPunto(String punto) {
        this.punto = punto;
    }

    /** @return límite inferior */
    public String getLimiteA() {
        return limiteA;
    }

    /** @param limiteA límite inferior */
    public void setLimiteA(String limiteA) {
        this.limiteA = limiteA;
    }

    /** @return límite superior */
    public String getLimiteB() {
        return limiteB;
    }

    /** @param limiteB límite superior */
    public void setLimiteB(String limiteB) {
        this.limiteB = limiteB;
    }

    /** @return punto intermedio c (aditividad) */
    public String getPuntoC() {
        return puntoC;
    }

    /** @param puntoC punto intermedio c */
    public void setPuntoC(String puntoC) {
        this.puntoC = puntoC;
    }

    /** @return true si la integral es definida */
    public boolean isDefinida() {
        return definida;
    }

    /** @param definida true para integral definida */
    public void setDefinida(boolean definida) {
        this.definida = definida;
    }
}
