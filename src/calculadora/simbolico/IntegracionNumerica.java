package calculadora.simbolico;

import calculadora.modelo.Graficable;
import calculadora.modelo.Variable;

/**
 * Integración NUMÉRICA de una integral definida con la regla adaptativa de
 * Gauss-Kronrod (7 y 15 puntos).
 * <p>
 * Se usa cuando la función no tiene antiderivada elemental (por ejemplo
 * e^(-x²) o sin(x)/x) y también para comprobar el resultado exacto.
 * </p>
 * <ul>
 *   <li>Los nodos de Gauss nunca tocan los extremos, por eso funciona aunque la
 *       función se vaya a infinito en a o en b (como 1/√x en 0).</li>
 *   <li>Los límites infinitos se convierten en un intervalo finito con un cambio
 *       de variable: x = t/(1 - t²) para (-∞, ∞) y x = a + t/(1 - t) para [a, ∞).</li>
 *   <li>El intervalo se divide en mitades hasta que la diferencia entre la regla
 *       de 7 puntos y la de 15 puntos es menor que la tolerancia.</li>
 * </ul>
 */
public final class IntegracionNumerica {

    /** Nodos de Kronrod en [0, 1] (la regla es simétrica). */
    private static final double[] NODOS = {
        0.991455371120812639, 0.949107912342758525, 0.864864423359769073, 0.741531185599394440,
        0.586087235467691130, 0.405845151377397167, 0.207784955007898468, 0.000000000000000000
    };
    /** Pesos de Kronrod (15 puntos). */
    private static final double[] PESOS_KRONROD = {
        0.022935322010529225, 0.063092092629978553, 0.104790010322250184, 0.140653259715525919,
        0.169004726639267903, 0.190350578064785410, 0.204432940075298892, 0.209482141084727828
    };
    /** Pesos de Gauss (7 puntos) para los nodos de índice impar. */
    private static final double[] PESOS_GAUSS = {
        0.129484966168869693, 0.279705391489276668, 0.381830050505118945, 0.417959183673469388
    };

    private static final int MAXIMO_SUBINTERVALOS = 4000;

    /** Resultado de la integración numérica. */
    public static final class Resultado {
        private final double valor;
        private final double error;
        private final boolean confiable;

        Resultado(double valor, double error, boolean confiable) {
            this.valor = valor;
            this.error = error;
            this.confiable = confiable;
        }

        /** @return valor aproximado */
        public double getValor() {
            return valor;
        }

        /** @return estimación del error */
        public double getError() {
            return error;
        }

        /** @return true si el método convergió (el área parece finita) */
        public boolean esConfiable() {
            return confiable;
        }
    }

    private IntegracionNumerica() {
    }

    /**
     * Aproxima ∫[a, b] f dv. Los límites pueden ser ±∞.
     *
     * @param f         integrando
     * @param v         variable de integración
     * @param a         límite inferior
     * @param b         límite superior
     * @param valorOtra valor de la otra variable
     * @return resultado con el valor y si es confiable
     */
    public static Resultado integrar(Graficable f, Variable v, double a, double b, double valorOtra) {
        if (a == b) {
            return new Resultado(0, 0, true);
        }
        if (a > b) {
            Resultado invertido = integrar(f, v, b, a, valorOtra);
            return new Resultado(-invertido.valor, invertido.error, invertido.confiable);
        }
        Funcion g = new Funcion(f, v, a, b, valorOtra);
        int[] contador = {0};
        double[] suma = {0, 0};
        boolean ok = adaptativo(g, g.inicio, g.fin, 1e-10, 0, contador, suma);
        double valor = suma[0];
        double error = suma[1];
        boolean confiable = ok && !Double.isNaN(valor) && !Double.isInfinite(valor)
                && error <= 1e-6 * (1 + Math.abs(valor));
        return new Resultado(valor, error, confiable);
    }

    private static boolean adaptativo(Funcion g, double a, double b, double tolerancia, int nivel, int[] contador,
                                      double[] suma) {
        contador[0]++;
        double[] kg = kronrod(g, a, b);
        double diferencia = Math.abs(kg[0] - kg[1]);
        if (Double.isNaN(kg[0]) || Double.isInfinite(kg[0])) {
            suma[0] = Double.NaN;
            return false;
        }
        if (diferencia <= Math.max(tolerancia, 1e-14 * Math.abs(kg[0])) || nivel > 45
                || contador[0] > MAXIMO_SUBINTERVALOS) {
            suma[0] += kg[0];
            suma[1] += diferencia;
            return diferencia <= Math.max(tolerancia * 1e3, 1e-9 * Math.abs(kg[0]));
        }
        double medio = (a + b) / 2;
        boolean izquierda = adaptativo(g, a, medio, tolerancia / 2, nivel + 1, contador, suma);
        boolean derecha = adaptativo(g, medio, b, tolerancia / 2, nivel + 1, contador, suma);
        return izquierda && derecha;
    }

    /** @return {Kronrod 15, Gauss 7} en [a, b] */
    private static double[] kronrod(Funcion g, double a, double b) {
        double centro = (a + b) / 2;
        double radio = (b - a) / 2;
        double fc = g.valor(centro);
        double kronrod = PESOS_KRONROD[7] * fc;
        double gauss = PESOS_GAUSS[3] * fc;
        for (int i = 0; i < 7; i++) {
            double dx = radio * NODOS[i];
            double suma = g.valor(centro - dx) + g.valor(centro + dx);
            kronrod += PESOS_KRONROD[i] * suma;
            if (i % 2 == 1) {
                gauss += PESOS_GAUSS[i / 2] * suma;
            }
        }
        return new double[]{kronrod * radio, gauss * radio};
    }

    /** Integrando ya transformado al intervalo finito. */
    private static final class Funcion {
        private final Graficable f;
        private final Variable v;
        private final double a;
        private final double b;
        private final double valorOtra;
        private final int tipo;
        private final double inicio;
        private final double fin;

        Funcion(Graficable f, Variable v, double a, double b, double valorOtra) {
            this.f = f;
            this.v = v;
            this.a = a;
            this.b = b;
            this.valorOtra = valorOtra;
            if (Double.isInfinite(a) && Double.isInfinite(b)) {
                tipo = 3;
                inicio = -1;
                fin = 1;
            } else if (Double.isInfinite(b)) {
                tipo = 1;
                inicio = 0;
                fin = 1;
            } else if (Double.isInfinite(a)) {
                tipo = 2;
                inicio = 0;
                fin = 1;
            } else {
                tipo = 0;
                inicio = a;
                fin = b;
            }
        }

        double valor(double t) {
            double x;
            double jacobiano;
            if (tipo == 0) {
                x = t;
                jacobiano = 1;
            } else if (tipo == 1) {
                x = a + t / (1 - t);
                jacobiano = 1 / ((1 - t) * (1 - t));
            } else if (tipo == 2) {
                x = b - t / (1 - t);
                jacobiano = 1 / ((1 - t) * (1 - t));
            } else {
                x = t / (1 - t * t);
                jacobiano = (1 + t * t) / ((1 - t * t) * (1 - t * t));
            }
            if (Double.isInfinite(jacobiano)) {
                return 0;
            }
            double y = f.evaluarEn(v, x, valorOtra);
            if (Double.isNaN(y)) {
                return Double.NaN;
            }
            double producto = y * jacobiano;
            if (Double.isNaN(producto)) {
                return 0;
            }
            return producto;
        }
    }
}
