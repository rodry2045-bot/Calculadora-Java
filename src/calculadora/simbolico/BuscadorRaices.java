package calculadora.simbolico;

import calculadora.modelo.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Busca los valores de x donde una expresión vale 0 dentro de un intervalo.
 * <ul>
 *   <li>Si la expresión es un polinomio, se factoriza y las raíces son exactas.</li>
 *   <li>Si no, se recorre el intervalo en pasos pequeños: donde cambia el signo
 *       se aplica el método de bisección; donde |h| tiene un mínimo muy cercano
 *       a 0 (raíz doble, como x² en 0) se refina con búsqueda ternaria.</li>
 * </ul>
 */
public final class BuscadorRaices {

    private static final int PASOS = 4000;

    private BuscadorRaices() {
    }

    /**
     * @param h         expresión en la variable v
     * @param v         variable
     * @param a         extremo izquierdo
     * @param b         extremo derecho
     * @param soloCambioDeSigno true para aceptar solo raíces donde h cambia de signo
     * @return raíces ordenadas de menor a mayor
     */
    public static List<Double> buscar(Nodo h, Variable v, double a, double b, boolean soloCambioDeSigno) {
        List<Double> raices = new ArrayList<Double>();
        double inferior = Math.min(a, b);
        double superior = Math.max(a, b);
        Polinomio p = Polinomio.desde(Simplificador.simplificar(h), v);
        if (p != null && p.grado() >= 1) {
            List<Polinomio.Factor> factores = p.factorizar();
            for (int i = 0; i < factores.size(); i++) {
                Polinomio.Factor f = factores.get(i);
                if (f.cuadratico) {
                    continue;
                }
                boolean cambiaSigno = f.multiplicidad % 2 == 1;
                if (soloCambioDeSigno && !cambiaSigno) {
                    continue;
                }
                if (f.a >= inferior - 1e-12 && f.a <= superior + 1e-12) {
                    agregar(raices, f.a);
                }
            }
            ordenar(raices);
            return raices;
        }
        if (p != null) {
            return raices; // polinomio constante: no tiene raíces aisladas
        }

        double paso = (superior - inferior) / PASOS;
        double anteriorX = inferior;
        double anteriorY = valor(h, v, inferior);
        double escala = 0;
        for (int i = 0; i <= PASOS; i++) {
            double y = valor(h, v, inferior + i * paso);
            if (esReal(y)) {
                escala = Math.max(escala, Math.abs(y));
            }
        }
        if (!soloCambioDeSigno && esReal(anteriorY) && Math.abs(anteriorY) < 1e-12) {
            agregar(raices, inferior);
        }
        double anteriorAnteriorY = Double.NaN;
        for (int i = 1; i <= PASOS; i++) {
            double x = inferior + i * paso;
            double y = valor(h, v, x);
            if (esReal(y) && Math.abs(y) < 1e-12 && !(esReal(anteriorY) && Math.abs(anteriorY) < 1e-12)) {
                if (!soloCambioDeSigno || cambiaDeSignoEn(h, v, x, paso)) {
                    agregar(raices, x);
                }
            } else if (esReal(y) && esReal(anteriorY) && Math.signum(y) * Math.signum(anteriorY) < 0
                    && Math.max(Math.abs(y), Math.abs(anteriorY)) > 1e-10) {
                double r = biseccion(h, v, anteriorX, x);
                // Se descartan saltos de una asíntota (como tan x): ahí |h| es enorme
                double cerca = valor(h, v, r);
                if (!esReal(cerca) || Math.abs(cerca) < 1e-6 * (1 + escala)) {
                    agregar(raices, r);
                }
            } else if (!soloCambioDeSigno && esReal(y) && esReal(anteriorY) && esReal(anteriorAnteriorY)
                    && Math.abs(anteriorY) <= Math.abs(y) && Math.abs(anteriorY) < Math.abs(anteriorAnteriorY)
                    && Math.abs(anteriorY) < 1e-2 * (1 + escala)) {
                double r = ternaria(h, v, anteriorX - paso, x);
                if (Math.abs(valor(h, v, r)) < 1e-9 * (1 + escala)) {
                    agregar(raices, r);
                }
            }
            anteriorAnteriorY = anteriorY;
            anteriorX = x;
            anteriorY = y;
        }
        ordenar(raices);
        if (raices.size() > 60) {
            // Demasiados ceros: la expresión es (casi) 0 en todo un tramo, no hay raíces aisladas
            raices.clear();
        }
        return raices;
    }

    /** @return true si h tiene signos opuestos a cada lado de x */
    private static boolean cambiaDeSignoEn(Nodo h, Variable v, double x, double paso) {
        double izquierda = valor(h, v, x - paso / 2);
        double derecha = valor(h, v, x + paso / 2);
        return esReal(izquierda) && esReal(derecha) && Math.signum(izquierda) * Math.signum(derecha) < 0
                && Math.max(Math.abs(izquierda), Math.abs(derecha)) > 1e-10;
    }

    private static double biseccion(Nodo h, Variable v, double izquierda, double derecha) {
        double fi = valor(h, v, izquierda);
        for (int k = 0; k < 80; k++) {
            double medio = (izquierda + derecha) / 2;
            double fm = valor(h, v, medio);
            if (!esReal(fm) || fm == 0) {
                return medio;
            }
            if (Math.signum(fm) == Math.signum(fi)) {
                izquierda = medio;
                fi = fm;
            } else {
                derecha = medio;
            }
        }
        return Limites.redondear((izquierda + derecha) / 2);
    }

    private static double ternaria(Nodo h, Variable v, double izquierda, double derecha) {
        for (int k = 0; k < 100; k++) {
            double m1 = izquierda + (derecha - izquierda) / 3;
            double m2 = derecha - (derecha - izquierda) / 3;
            if (Math.abs(valor(h, v, m1)) < Math.abs(valor(h, v, m2))) {
                derecha = m2;
            } else {
                izquierda = m1;
            }
        }
        return Limites.redondear((izquierda + derecha) / 2);
    }

    private static void agregar(List<Double> raices, double r) {
        double limpio = Limites.redondear(r);
        for (int i = 0; i < raices.size(); i++) {
            if (Math.abs(raices.get(i).doubleValue() - limpio) < 1e-6) {
                return;
            }
        }
        raices.add(Double.valueOf(limpio));
    }

    private static void ordenar(List<Double> lista) {
        for (int i = 1; i < lista.size(); i++) {
            Double actual = lista.get(i);
            int j = i - 1;
            while (j >= 0 && lista.get(j).doubleValue() > actual.doubleValue()) {
                lista.set(j + 1, lista.get(j));
                j--;
            }
            lista.set(j + 1, actual);
        }
    }

    private static double valor(Nodo h, Variable v, double t) {
        return h.evaluarEn(v, t, 1.0);
    }

    private static boolean esReal(double y) {
        return !Double.isNaN(y) && !Double.isInfinite(y);
    }
}
