package calculadora.simbolico;

import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

/**
 * Cálculo NUMÉRICO de límites.
 * <p>
 * Se evalúa la función en puntos cada vez más cercanos al valor de
 * interés (o cada vez más grandes, si se tiende a ±∞) y se observa la
 * tendencia: si los valores se estabilizan el límite es finito; si crecen
 * sin cota el límite es ±∞; si oscilan, el límite no existe.
 * </p>
 * Se usa para clasificar las formas indeterminadas de L'Hôpital y para
 * evaluar las integrales impropias.
 */
public final class Limites {

    /** Tipo de resultado de un límite. */
    public enum Tipo {
        /** El límite es un número real. */
        FINITO,
        /** El límite es +∞. */
        MAS_INFINITO,
        /** El límite es −∞. */
        MENOS_INFINITO,
        /** El límite no existe (oscila o los laterales difieren). */
        NO_EXISTE
    }

    /** Resultado de un límite. */
    public static final class Resultado {
        private final Tipo tipo;
        private final double valor;

        Resultado(Tipo tipo, double valor) {
            this.tipo = tipo;
            this.valor = valor;
        }

        /** @return tipo de resultado */
        public Tipo getTipo() {
            return tipo;
        }

        /** @return valor (solo si es finito; ±∞ si es infinito) */
        public double getValor() {
            return valor;
        }

        /** @return true si es un número real */
        public boolean esFinito() {
            return tipo == Tipo.FINITO;
        }

        /** @return true si es +∞ o −∞ */
        public boolean esInfinito() {
            return tipo == Tipo.MAS_INFINITO || tipo == Tipo.MENOS_INFINITO;
        }

        /** @return true si es 0 */
        public boolean esCero() {
            return tipo == Tipo.FINITO && Math.abs(valor) < 1e-9;
        }

        /** @return texto como "3/2", "π/4", "∞", "-∞" o "no existe" */
        public String texto() {
            if (tipo == Tipo.MAS_INFINITO) {
                return "∞";
            }
            if (tipo == Tipo.MENOS_INFINITO) {
                return "-∞";
            }
            if (tipo == Tipo.NO_EXISTE) {
                return "no existe";
            }
            return FormatoNumeros.formatearExacto(valor);
        }
    }

    /** Constructor privado: clase utilitaria. */
    private Limites() {
    }

    /**
     * Calcula el límite de f cuando v tiende a {@code a}.
     *
     * @param f    función
     * @param v    variable
     * @param a    punto (puede ser ±∞)
     * @param lado 0 = bilateral, +1 = por la derecha, −1 = por la izquierda
     * @return resultado del límite
     */
    public static Resultado limite(Nodo f, Variable v, double a, int lado) {
        if (Double.isInfinite(a)) {
            return enInfinito(f, v, a > 0 ? 1 : -1);
        }
        if (lado != 0) {
            return lateral(f, v, a, lado);
        }
        // Si la función solo está definida de un lado (como ln x o √x en 0),
        // se usa ese límite lateral
        boolean definidaDerecha = definidaCerca(f, v, a, 1);
        boolean definidaIzquierda = definidaCerca(f, v, a, -1);
        if (definidaDerecha && !definidaIzquierda) {
            return lateral(f, v, a, 1);
        }
        if (definidaIzquierda && !definidaDerecha) {
            return lateral(f, v, a, -1);
        }
        Resultado derecha = lateral(f, v, a, 1);
        Resultado izquierda = lateral(f, v, a, -1);
        if (derecha.tipo == izquierda.tipo) {
            if (derecha.tipo != Tipo.FINITO) {
                return derecha;
            }
            if (Math.abs(derecha.valor - izquierda.valor) < 1e-5 * (1 + Math.abs(derecha.valor))) {
                return derecha;
            }
        }
        return new Resultado(Tipo.NO_EXISTE, Double.NaN);
    }

    private static boolean definidaCerca(Nodo f, Variable v, double a, int lado) {
        int definidos = 0;
        for (int k = 3; k <= 7; k++) {
            if (!Double.isNaN(evaluar(f, v, a + lado * Math.pow(10, -k)))) {
                definidos++;
            }
        }
        return definidos >= 3;
    }

    private static Resultado lateral(Nodo f, Variable v, double a, int lado) {
        double directo = evaluar(f, v, a);
        double[] valores = new double[8];
        for (int k = 0; k < valores.length; k++) {
            double h = Math.pow(10, -(k + 1));
            valores[k] = evaluar(f, v, a + lado * h);
        }
        if (esReal(directo)) {
            double cercano = evaluar(f, v, a + lado * 1e-7);
            if (esReal(cercano) && Math.abs(cercano - directo) < 1e-4 * (1 + Math.abs(directo))) {
                return new Resultado(Tipo.FINITO, redondear(directo));
            }
            // Continua pero con pendiente muy grande (como √x en 0): los valores
            // se acercan cada vez más al valor directo
            boolean seAcerca = true;
            for (int k = 3; k < valores.length - 1; k++) {
                if (!esReal(valores[k]) || Math.abs(valores[k + 1] - directo) > Math.abs(valores[k] - directo)) {
                    seAcerca = false;
                }
            }
            if (seAcerca && Math.abs(valores[valores.length - 1] - directo) < 1e-2 * (1 + Math.abs(directo))) {
                return new Resultado(Tipo.FINITO, redondear(directo));
            }
        }
        // Se busca el primer par de valores consecutivos que ya coinciden
        for (int k = 4; k + 1 < valores.length; k++) {
            if (esReal(valores[k]) && esReal(valores[k + 1])
                    && Math.abs(valores[k] - valores[k + 1]) < 1e-4 * (1 + Math.abs(valores[k + 1]))
                    && Math.abs(valores[k + 1]) < 1e7) {
                // Se avanza mientras las diferencias sigan disminuyendo (más precisión)
                int j = k;
                while (j + 2 < valores.length && esReal(valores[j + 2])
                        && Math.abs(valores[j + 2] - valores[j + 1]) < Math.abs(valores[j + 1] - valores[j])) {
                    j++;
                }
                return new Resultado(Tipo.FINITO, redondear(valores[j + 1]));
            }
        }
        return clasificar(valores, 4, 5);
    }

    private static Resultado enInfinito(Nodo f, Variable v, int signo) {
        double[] valores = new double[9];
        for (int k = 0; k < valores.length; k++) {
            valores[k] = evaluar(f, v, signo * Math.pow(10, k + 1));
        }
        return clasificar(valores, valores.length - 2, valores.length - 1);
    }

    /**
     * Clasifica una sucesión de valores que se acercan al punto de interés.
     *
     * @param valores  valores en orden de acercamiento
     * @param penultimo índice usado para comprobar convergencia
     * @param ultimo    índice del valor más confiable
     */
    private static Resultado clasificar(double[] valores, int penultimo, int ultimo) {
        int n = valores.length;
        double fin = valores[n - 1];

        // Infinito "evidente": valores enormes del mismo signo
        if (Double.isInfinite(fin) || (esReal(fin) && Math.abs(fin) > 1e8 && Math.abs(fin) > Math.abs(valores[n - 3]))) {
            return new Resultado(fin > 0 ? Tipo.MAS_INFINITO : Tipo.MENOS_INFINITO, fin > 0 ? Double.POSITIVE_INFINITY
                    : Double.NEGATIVE_INFINITY);
        }
        for (int k = n - 4; k < n; k++) {
            if (!esReal(valores[k])) {
                return new Resultado(Tipo.NO_EXISTE, Double.NaN);
            }
        }

        // Convergencia
        double a = valores[penultimo];
        double b = valores[ultimo];
        if (Math.abs(a - b) < 1e-4 * (1 + Math.abs(b))) {
            return new Resultado(Tipo.FINITO, redondear(b));
        }

        // Crecimiento lento pero sin cota (como ln x o √x): monótono y sin frenarse
        boolean creciente = true;
        boolean decreciente = true;
        for (int k = n - 5; k < n - 1; k++) {
            if (valores[k + 1] <= valores[k]) {
                creciente = false;
            }
            if (valores[k + 1] >= valores[k]) {
                decreciente = false;
            }
        }
        double diferenciaFinal = Math.abs(valores[n - 1] - valores[n - 2]);
        double diferenciaAnterior = Math.abs(valores[n - 3] - valores[n - 4]);
        boolean sinFrenar = diferenciaFinal >= 0.4 * diferenciaAnterior;
        boolean magnitudCrece = Math.abs(fin) > 1.4 * Math.abs(valores[n - 5]) && Math.abs(fin) > 5;
        if ((creciente || decreciente) && sinFrenar && magnitudCrece) {
            return new Resultado(creciente ? Tipo.MAS_INFINITO : Tipo.MENOS_INFINITO,
                    creciente ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY);
        }
        return new Resultado(Tipo.NO_EXISTE, Double.NaN);
    }

    private static double evaluar(Nodo f, Variable v, double t) {
        return f.evaluarEn(v, t, 1.0);
    }

    private static boolean esReal(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    /**
     * Redondea un valor numérico a un entero, una fracción o un múltiplo de π
     * cuando está muy cerca de uno de ellos.
     *
     * @param v valor aproximado
     * @return valor redondeado
     */
    public static double redondear(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-6) {
            return Math.rint(v) == 0.0 ? 0.0 : Math.rint(v);
        }
        for (int d = 2; d <= 100; d++) {
            double n = Math.rint(v * d);
            if (Math.abs(n / d - v) < 1e-7 * (1 + Math.abs(v))) {
                return n / d;
            }
        }
        double multiploPi = v / Math.PI;
        for (int d = 1; d <= 12; d++) {
            double n = Math.rint(multiploPi * d);
            if (n != 0 && Math.abs(n / d - multiploPi) < 1e-7) {
                return n / d * Math.PI;
            }
        }
        return v;
    }
}
