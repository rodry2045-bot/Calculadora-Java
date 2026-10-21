package calculadora.simbolico;

import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

import java.util.ArrayList;
import java.util.List;

/**
 * Polinomio de una variable con coeficientes reales:
 * c[0] + c[1]·v + c[2]·v² + ... + c[n]·vⁿ.
 * <p>
 * Se usa para desarrollar productos, dividir polinomios y factorizar
 * denominadores en el método de fracciones parciales.
 * </p>
 */
public class Polinomio {

    private static final double TOLERANCIA = 1e-9;

    /** Coeficientes: el índice es la potencia. */
    private final double[] coeficientes;

    /**
     * @param coeficientes coeficientes (índice = potencia)
     */
    public Polinomio(double[] coeficientes) {
        int grado = coeficientes.length - 1;
        while (grado > 0 && Math.abs(coeficientes[grado]) < TOLERANCIA) {
            grado--;
        }
        this.coeficientes = new double[grado + 1];
        for (int i = 0; i <= grado; i++) {
            this.coeficientes[i] = limpiar(coeficientes[i]);
        }
    }

    /**
     * Intenta convertir un árbol en polinomio de la variable v.
     *
     * @param n árbol
     * @param v variable
     * @return el polinomio o null si el árbol no es polinómico en v
     */
    public static Polinomio desde(Nodo n, Variable v) {
        if (n instanceof NodoConstante) {
            return new Polinomio(new double[]{((NodoConstante) n).getValor()});
        }
        if (n instanceof NodoVariable) {
            if (((NodoVariable) n).getVariable() == v) {
                return new Polinomio(new double[]{0, 1});
            }
            return null;
        }
        if (!n.dependeDe(v)) {
            if (n.esConstante()) {
                double valor = n.evaluar(0, 0);
                if (!Double.isNaN(valor) && !Double.isInfinite(valor)) {
                    return new Polinomio(new double[]{valor});
                }
            }
            return null;
        }
        if (n instanceof NodoSuma || n instanceof NodoResta) {
            Polinomio a = desde(((NodoBinario) n).getIzquierdo(), v);
            Polinomio b = desde(((NodoBinario) n).getDerecho(), v);
            if (a == null || b == null) {
                return null;
            }
            if (n instanceof NodoSuma) {
                return a.sumar(b);
            }
            return a.sumar(b.escalar(-1));
        }
        if (n instanceof NodoNegativo) {
            Polinomio a = desde(((NodoNegativo) n).getArgumento(), v);
            return a == null ? null : a.escalar(-1);
        }
        if (n instanceof NodoProducto) {
            Polinomio a = desde(((NodoBinario) n).getIzquierdo(), v);
            Polinomio b = desde(((NodoBinario) n).getDerecho(), v);
            if (a == null || b == null) {
                return null;
            }
            return a.multiplicar(b);
        }
        if (n instanceof NodoCociente) {
            Polinomio a = desde(((NodoBinario) n).getIzquierdo(), v);
            Nodo denominador = ((NodoBinario) n).getDerecho();
            if (a == null || denominador.dependeDe(v) || !denominador.esConstante()) {
                return null;
            }
            return a.escalar(1.0 / denominador.evaluar(0, 0));
        }
        if (n instanceof NodoPotencia) {
            NodoPotencia p = (NodoPotencia) n;
            if (!p.getExponente().esNumero()) {
                return null;
            }
            double e = ((NodoConstante) p.getExponente()).getValor();
            if (!FormatoNumeros.esEntero(e) || e < 0 || e > 30) {
                return null;
            }
            Polinomio base = desde(p.getBase(), v);
            if (base == null) {
                return null;
            }
            Polinomio resultado = new Polinomio(new double[]{1});
            for (int i = 0; i < Math.round(e); i++) {
                resultado = resultado.multiplicar(base);
            }
            return resultado;
        }
        return null;
    }

    /** @return grado del polinomio */
    public int grado() {
        return coeficientes.length - 1;
    }

    /**
     * @param potencia potencia
     * @return coeficiente de esa potencia (0 si no existe)
     */
    public double coeficiente(int potencia) {
        if (potencia < 0 || potencia >= coeficientes.length) {
            return 0.0;
        }
        return coeficientes[potencia];
    }

    /** @return coeficiente principal */
    public double principal() {
        return coeficientes[coeficientes.length - 1];
    }

    /** @return true si es el polinomio cero */
    public boolean esCero() {
        return coeficientes.length == 1 && Math.abs(coeficientes[0]) < TOLERANCIA;
    }

    /**
     * @param t punto
     * @return valor del polinomio en t (método de Horner)
     */
    public double evaluar(double t) {
        double resultado = 0;
        for (int i = coeficientes.length - 1; i >= 0; i--) {
            resultado = resultado * t + coeficientes[i];
        }
        return resultado;
    }

    /**
     * @param otro polinomio
     * @return suma
     */
    public Polinomio sumar(Polinomio otro) {
        int n = Math.max(coeficientes.length, otro.coeficientes.length);
        double[] c = new double[n];
        for (int i = 0; i < n; i++) {
            c[i] = coeficiente(i) + otro.coeficiente(i);
        }
        return new Polinomio(c);
    }

    /**
     * @param factor número
     * @return polinomio multiplicado por el número
     */
    public Polinomio escalar(double factor) {
        double[] c = new double[coeficientes.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = coeficientes[i] * factor;
        }
        return new Polinomio(c);
    }

    /**
     * @param otro polinomio
     * @return producto
     */
    public Polinomio multiplicar(Polinomio otro) {
        double[] c = new double[coeficientes.length + otro.coeficientes.length - 1];
        for (int i = 0; i < coeficientes.length; i++) {
            for (int j = 0; j < otro.coeficientes.length; j++) {
                c[i + j] = c[i + j] + coeficientes[i] * otro.coeficientes[j];
            }
        }
        return new Polinomio(c);
    }

    /**
     * División larga de polinomios.
     *
     * @param divisor polinomio divisor (no nulo)
     * @return arreglo {cociente, residuo}
     */
    public Polinomio[] dividir(Polinomio divisor) {
        double[] residuo = coeficientes.clone();
        int gradoDivisor = divisor.grado();
        if (grado() < gradoDivisor) {
            return new Polinomio[]{new Polinomio(new double[]{0}), this};
        }
        double[] cociente = new double[grado() - gradoDivisor + 1];
        for (int k = grado() - gradoDivisor; k >= 0; k--) {
            double factor = residuo[k + gradoDivisor] / divisor.principal();
            cociente[k] = factor;
            for (int j = 0; j <= gradoDivisor; j++) {
                residuo[k + j] = residuo[k + j] - factor * divisor.coeficiente(j);
            }
        }
        double[] resto = new double[Math.max(1, gradoDivisor)];
        for (int i = 0; i < resto.length; i++) {
            resto[i] = residuo[i];
        }
        return new Polinomio[]{new Polinomio(cociente), new Polinomio(resto)};
    }

    /**
     * Construye el árbol del polinomio en la variable v.
     *
     * @param v variable
     * @return árbol simplificado
     */
    public Nodo aNodo(Variable v) {
        Nodo resultado = null;
        for (int i = coeficientes.length - 1; i >= 0; i--) {
            double c = coeficientes[i];
            if (Math.abs(c) < TOLERANCIA && coeficientes.length > 1) {
                continue;
            }
            Nodo termino;
            if (i == 0) {
                termino = Nodo.num(c);
            } else if (i == 1) {
                termino = Nodo.mul(Nodo.num(c), Nodo.var(v));
            } else {
                termino = Nodo.mul(Nodo.num(c), Nodo.pot(Nodo.var(v), i));
            }
            resultado = (resultado == null) ? termino : Nodo.suma(resultado, termino);
        }
        if (resultado == null) {
            return Nodo.num(0);
        }
        return Simplificador.simplificar(resultado);
    }

    // ==================================================================
    // Raíces y factorización
    // ==================================================================

    /**
     * Factor de la factorización real: lineal (v − r) o cuadrático
     * (v² + p·v + q) irreducible, con su multiplicidad.
     */
    public static class Factor {
        /** true si es cuadrático. */
        public final boolean cuadratico;
        /** Raíz r (lineal) o coeficiente p (cuadrático). */
        public final double a;
        /** Coeficiente q (solo cuadrático). */
        public final double b;
        /** Multiplicidad. */
        public int multiplicidad;

        Factor(boolean cuadratico, double a, double b) {
            this.cuadratico = cuadratico;
            this.a = a;
            this.b = b;
            this.multiplicidad = 1;
        }

        /**
         * @param t punto
         * @return valor del factor (sin multiplicidad) en t
         */
        public double evaluar(double t) {
            if (cuadratico) {
                return t * t + a * t + b;
            }
            return t - a;
        }

        /**
         * @param v variable
         * @return árbol del factor (v − r) o (v² + p·v + q)
         */
        public Nodo aNodo(Variable v) {
            if (cuadratico) {
                return new Polinomio(new double[]{b, a, 1}).aNodo(v);
            }
            return new Polinomio(new double[]{-a, 1}).aNodo(v);
        }
    }

    /**
     * Factoriza el polinomio mónico asociado en factores lineales y
     * cuadráticos irreducibles. Primero busca raíces racionales (teorema de
     * la raíz racional); el resto se resuelve con la fórmula general o con
     * el método numérico de Durand–Kerner.
     *
     * @return lista de factores con multiplicidad
     */
    public List<Factor> factorizar() {
        List<Factor> factores = new ArrayList<Factor>();
        Polinomio resto = escalar(1.0 / principal());

        // 1) Raíces racionales exactas
        boolean encontrada = true;
        while (resto.grado() > 0 && encontrada) {
            encontrada = false;
            List<Double> candidatos = resto.candidatosRacionales();
            for (int i = 0; i < candidatos.size(); i++) {
                double r = candidatos.get(i).doubleValue();
                if (Math.abs(resto.evaluar(r)) < 1e-9 * (1 + resto.normaUno())) {
                    agregarLineal(factores, r);
                    resto = resto.dividir(new Polinomio(new double[]{-r, 1}))[0];
                    encontrada = true;
                    break;
                }
            }
        }

        // 2) Lo que queda
        if (resto.grado() == 1) {
            agregarLineal(factores, -resto.coeficiente(0) / resto.coeficiente(1));
        } else if (resto.grado() == 2) {
            agregarCuadratico(factores, resto);
        } else if (resto.grado() > 2) {
            double[][] raices = resto.raicesNumericas();
            for (int i = 0; i < raices.length; i++) {
                double re = raices[i][0];
                double im = raices[i][1];
                if (Math.abs(im) < 1e-7) {
                    agregarLineal(factores, redondear(re));
                } else if (im > 0) {
                    factores.add(new Factor(true, redondear(-2 * re), redondear(re * re + im * im)));
                }
            }
        }
        return factores;
    }

    private static void agregarLineal(List<Factor> factores, double r) {
        for (int i = 0; i < factores.size(); i++) {
            Factor f = factores.get(i);
            if (!f.cuadratico && Math.abs(f.a - r) < 1e-7) {
                f.multiplicidad++;
                return;
            }
        }
        factores.add(new Factor(false, limpiar(r), 0));
    }

    private static void agregarCuadratico(List<Factor> factores, Polinomio p) {
        double a = p.coeficiente(2);
        double b = p.coeficiente(1) / a;
        double c = p.coeficiente(0) / a;
        double discriminante = b * b - 4 * c;
        if (discriminante >= -1e-12) {
            double raiz = Math.sqrt(Math.max(0, discriminante));
            agregarLineal(factores, redondear((-b + raiz) / 2));
            agregarLineal(factores, redondear((-b - raiz) / 2));
        } else {
            factores.add(new Factor(true, limpiar(b), limpiar(c)));
        }
    }

    /** Candidatos p/q del teorema de la raíz racional (si los coeficientes son racionales). */
    private List<Double> candidatosRacionales() {
        List<Double> lista = new ArrayList<Double>();
        if (Math.abs(coeficiente(0)) < TOLERANCIA) {
            lista.add(Double.valueOf(0.0));
            return lista;
        }
        // Convertir a coeficientes enteros multiplicando por los denominadores
        long multiplicador = 1;
        for (int i = 0; i < coeficientes.length; i++) {
            double c = coeficientes[i];
            if (!FormatoNumeros.esEntero(c)) {
                long[] fraccion = FormatoNumeros.comoFraccion(c);
                if (fraccion == null) {
                    return lista;
                }
                multiplicador = mcm(multiplicador, fraccion[1]);
            }
        }
        long a0 = Math.abs(Math.round(coeficiente(0) * multiplicador));
        long an = Math.abs(Math.round(principal() * multiplicador));
        if (a0 > 100000 || an > 100000) {
            return lista;
        }
        List<Long> divisoresP = divisores(a0);
        List<Long> divisoresQ = divisores(an);
        for (int i = 0; i < divisoresP.size(); i++) {
            for (int j = 0; j < divisoresQ.size(); j++) {
                double r = (double) divisoresP.get(i).longValue() / divisoresQ.get(j).longValue();
                lista.add(Double.valueOf(r));
                lista.add(Double.valueOf(-r));
            }
        }
        return lista;
    }

    private static List<Long> divisores(long n) {
        List<Long> lista = new ArrayList<Long>();
        for (long d = 1; d <= n && lista.size() < 200; d++) {
            if (n % d == 0) {
                lista.add(Long.valueOf(d));
            }
        }
        return lista;
    }

    private static long mcm(long a, long b) {
        return a / mcd(a, b) * b;
    }

    private static long mcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return Math.abs(a);
    }

    private double normaUno() {
        double suma = 0;
        for (int i = 0; i < coeficientes.length; i++) {
            suma = suma + Math.abs(coeficientes[i]);
        }
        return suma;
    }

    /**
     * Método de Durand–Kerner: aproxima todas las raíces complejas a la vez.
     *
     * @return arreglo de raíces {parte real, parte imaginaria}
     */
    private double[][] raicesNumericas() {
        int n = grado();
        double[] c = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            c[i] = coeficientes[i] / principal();
        }
        double[] re = new double[n];
        double[] im = new double[n];
        // Valores iniciales: potencias de (0.4 + 0.9i)
        double baseRe = 0.4;
        double baseIm = 0.9;
        double pr = 1;
        double pi = 0;
        for (int k = 0; k < n; k++) {
            re[k] = pr;
            im[k] = pi;
            double nr = pr * baseRe - pi * baseIm;
            double ni = pr * baseIm + pi * baseRe;
            pr = nr;
            pi = ni;
        }
        for (int iteracion = 0; iteracion < 500; iteracion++) {
            for (int k = 0; k < n; k++) {
                // Evaluar el polinomio en z_k (Horner con números complejos)
                double vr = 1;
                double vi = 0;
                for (int j = n - 1; j >= 0; j--) {
                    double tr = vr * re[k] - vi * im[k] + c[j];
                    double ti = vr * im[k] + vi * re[k];
                    vr = tr;
                    vi = ti;
                }
                // Producto de (z_k − z_j)
                double dr = 1;
                double di = 0;
                for (int j = 0; j < n; j++) {
                    if (j != k) {
                        double ar = re[k] - re[j];
                        double ai = im[k] - im[j];
                        double tr = dr * ar - di * ai;
                        double ti = dr * ai + di * ar;
                        dr = tr;
                        di = ti;
                    }
                }
                double denominador = dr * dr + di * di;
                if (denominador == 0) {
                    continue;
                }
                re[k] = re[k] - (vr * dr + vi * di) / denominador;
                im[k] = im[k] - (vi * dr - vr * di) / denominador;
            }
        }
        double[][] raices = new double[n][2];
        for (int k = 0; k < n; k++) {
            raices[k][0] = re[k];
            raices[k][1] = im[k];
        }
        return raices;
    }

    private static double redondear(double v) {
        long[] fraccion = FormatoNumeros.comoFraccion(v);
        if (FormatoNumeros.esEntero(v)) {
            return Math.rint(v);
        }
        if (fraccion == null) {
            // Tolerancia más amplia para raíces numéricas
            for (int d = 1; d <= 100; d++) {
                double n = Math.rint(v * d);
                if (Math.abs(n / d - v) < 1e-7) {
                    return n / d;
                }
            }
            return v;
        }
        return (double) fraccion[0] / fraccion[1];
    }

    private static double limpiar(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-10) {
            return Math.rint(v) == 0.0 ? 0.0 : Math.rint(v);
        }
        return v;
    }

    @Override
    public String toString() {
        return aNodo(Variable.X).toString();
    }
}
