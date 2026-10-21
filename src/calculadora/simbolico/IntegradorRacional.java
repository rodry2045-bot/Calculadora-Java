package calculadora.simbolico;

import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ReglaCalculo;
import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

import java.util.ArrayList;
import java.util.List;

/**
 * Integración de funciones racionales P(x)/Q(x) por FRACCIONES PARCIALES.
 * <p>
 * Procedimiento:
 * </p>
 * <ol>
 *   <li>Si grado(P) ≥ grado(Q) se dividen los polinomios: P/Q = C + R/Q.</li>
 *   <li>Se factoriza Q en factores lineales (x − r)^m y cuadráticos
 *       irreducibles (x² + px + q).</li>
 *   <li>Se plantea R/Q = Σ A/(x − r)^k + Σ (Bx + C)/(x² + px + q) y se
 *       calculan las constantes resolviendo un sistema de ecuaciones lineales.</li>
 *   <li>Cada fracción simple se integra con ln, potencia o arctan.</li>
 * </ol>
 */
public class IntegradorRacional {

    private final Integrador integrador;

    /**
     * @param integrador integrador que aporta la bitácora
     */
    public IntegradorRacional(Integrador integrador) {
        this.integrador = integrador;
    }

    /**
     * @param g    integrando
     * @param v    variable
     * @param prof profundidad del paso
     * @return antiderivada o null si g no es una función racional apta
     */
    public Nodo integrar(Nodo g, Variable v, int prof) {
        Polinomio[] nd = numeradorDenominador(g, v);
        if (nd == null) {
            return null;
        }
        Polinomio numerador = nd[0];
        Polinomio denominador = nd[1];
        if (denominador.grado() < 1) {
            return null;
        }
        List<Polinomio.Factor> factores = denominador.factorizar();
        for (int i = 0; i < factores.size(); i++) {
            if (factores.get(i).cuadratico && factores.get(i).multiplicidad > 1) {
                return null; // (x² + px + q)^2 no está soportado
            }
        }

        RegistroPasos registro = integrador.getRegistro();
        String x = v.getSimbolo();
        Nodo nNodo = numerador.aNodo(v);
        Nodo dNodo = denominador.aNodo(v);
        PasoDetallado encabezado = registro.agregar(ReglaCalculo.FRACCIONES_PARCIALES,
                ReglaCalculo.FRACCIONES_PARCIALES.getNombre(), prof, "∫ " + g + " d" + x,
                "Es una función racional P/Q con P = " + nNodo + " y Q = " + dNodo
                        + ". Se descompone en fracciones simples");

        Nodo total = Nodo.num(0);

        // 1) División si es impropia
        Polinomio residuo = numerador;
        if (numerador.grado() >= denominador.grado()) {
            Polinomio[] division = numerador.dividir(denominador);
            Polinomio cociente = division[0];
            residuo = division[1];
            Nodo cocienteNodo = cociente.aNodo(v);
            PasoDetallado paso = registro.agregar(ReglaCalculo.DIVISION_POLINOMIOS,
                    ReglaCalculo.DIVISION_POLINOMIOS.getNombre(), prof + 1, nNodo + " ÷ " + dNodo,
                    "grado(P) ≥ grado(Q), así que se divide: P/Q = " + cocienteNodo + " + (" + residuo.aNodo(v) + ")/("
                            + dNodo + ")");
            paso.setResultado(cocienteNodo + " + (" + residuo.aNodo(v) + ")/(" + dNodo + ")");
            Nodo integralCociente = integrarPolinomio(cociente, v);
            PasoDetallado pasoCociente = registro.agregar(ReglaCalculo.INT_POTENCIA, ReglaCalculo.INT_POTENCIA.getNombre(),
                    prof + 1, "∫ " + cocienteNodo + " d" + x, "Se integra el cociente término a término");
            pasoCociente.setResultado(integralCociente.toString());
            total = Nodo.suma(total, integralCociente);
        }
        if (residuo.esCero()) {
            Nodo resultado = Simplificador.simplificar(total);
            encabezado.setResultado(resultado.toString());
            return resultado;
        }

        // 2) Bases de la descomposición
        List<Nodo> bases = new ArrayList<Nodo>();
        List<double[]> descripcion = new ArrayList<double[]>(); // {tipo, índice de factor, k}
        for (int i = 0; i < factores.size(); i++) {
            Polinomio.Factor f = factores.get(i);
            if (f.cuadratico) {
                bases.add(Nodo.div(Nodo.var(v), f.aNodo(v)));
                descripcion.add(new double[]{1, i, 1});
                bases.add(Nodo.div(Nodo.num(1), f.aNodo(v)));
                descripcion.add(new double[]{2, i, 1});
            } else {
                for (int k = 1; k <= f.multiplicidad; k++) {
                    bases.add(Nodo.div(Nodo.num(1), Nodo.pot(f.aNodo(v), k)));
                    descripcion.add(new double[]{0, i, k});
                }
            }
        }

        // 3) Sistema de ecuaciones evaluando en puntos que no son raíces
        int n = bases.size();
        double[][] matriz = new double[n][n + 1];
        double t = 0.4137;
        int fila = 0;
        while (fila < n) {
            t = t + 0.7319;
            double q = denominador.evaluar(t);
            if (Math.abs(q) < 1e-6) {
                continue;
            }
            for (int j = 0; j < n; j++) {
                matriz[fila][j] = bases.get(j).evaluarEn(v, t, 1.0);
            }
            matriz[fila][n] = residuo.evaluar(t) / q;
            fila++;
        }
        double[] constantes = resolver(matriz);
        if (constantes == null) {
            registro.getPasos().remove(encabezado);
            return null;
        }

        // 4) Descomposición e integración de cada fracción simple
        Nodo descomposicion = null;
        List<String> letras = new ArrayList<String>();
        String alfabeto = "ABCDEFGHIJKLMNOP";
        StringBuilder planteamiento = new StringBuilder();
        for (int j = 0; j < n; j++) {
            constantes[j] = Limites.redondear(constantes[j]);
            letras.add(String.valueOf(alfabeto.charAt(Math.min(j, alfabeto.length() - 1))));
            if (j > 0) {
                planteamiento.append(", ");
            }
            planteamiento.append(letras.get(j)).append(" = ").append(FormatoNumeros.formatear(constantes[j]));
        }
        for (int j = 0; j < n; j++) {
            if (Math.abs(constantes[j]) < 1e-12) {
                continue;
            }
            Nodo termino = Simplificador.simplificar(Nodo.mul(Nodo.num(constantes[j]), bases.get(j)));
            descomposicion = (descomposicion == null) ? termino : Nodo.suma(descomposicion, termino);
        }
        PasoDetallado pasoDescomposicion = registro.agregar(ReglaCalculo.FRACCIONES_PARCIALES, "Descomposición",
                prof + 1, "(" + residuo.aNodo(v) + ")/(" + dNodo + ")", "Factores de Q: " + describirFactores(factores, v)
                        + ". Se plantean las constantes y se resuelve el sistema: " + planteamiento);
        pasoDescomposicion.setResultado(descomposicion == null ? "0" : descomposicion.toString());

        for (int j = 0; j < n; j++) {
            double c = constantes[j];
            if (Math.abs(c) < 1e-12) {
                continue;
            }
            double[] d = descripcion.get(j);
            Polinomio.Factor f = factores.get((int) d[1]);
            Nodo integral;
            ReglaCalculo regla;
            String formula;
            Nodo factorNodo = f.aNodo(v);
            if (d[0] == 0) {
                int k = (int) d[2];
                if (k == 1) {
                    integral = Nodo.mul(Nodo.num(c), Nodo.fn(TipoFuncion.LN, Nodo.fn(TipoFuncion.ABS, factorNodo)));
                    regla = ReglaCalculo.INT_LOGARITMO;
                    formula = "∫A/(" + x + " - r) d" + x + " = A·ln|" + x + " - r|";
                } else {
                    integral = Nodo.div(Nodo.num(-c), Nodo.mul(Nodo.num(k - 1), Nodo.pot(factorNodo, k - 1)));
                    regla = ReglaCalculo.INT_POTENCIA;
                    formula = "∫A/(" + x + " - r)^k d" + x + " = -A/((k - 1)(" + x + " - r)^(k-1))";
                }
            } else {
                double p = f.a;
                double q = f.b;
                double raiz = Math.sqrt(4 * q - p * p);
                if (d[0] == 1) {
                    // B·x/(x² + px + q) = (B/2)(2x + p)/(…) − (B·p/2)/(…)
                    Nodo parteLn = Nodo.mul(Nodo.num(c / 2), Nodo.fn(TipoFuncion.LN, factorNodo));
                    Nodo parteArctan = Nodo.mul(Nodo.num(-c * p / 2 * 2 / raiz), Nodo.fn(TipoFuncion.ARCTAN,
                            Nodo.div(Nodo.suma(Nodo.mul(Nodo.num(2), Nodo.var(v)), Nodo.num(p)), Nodo.num(raiz))));
                    integral = Nodo.suma(parteLn, parteArctan);
                    regla = ReglaCalculo.INT_LOGARITMO;
                    formula = "∫B" + x + "/(" + x + "² + p" + x + " + q) d" + x
                            + " = (B/2)·ln(" + x + "² + p" + x + " + q) - (Bp/2)·∫1/(" + x + "² + p" + x + " + q) d" + x;
                } else {
                    integral = Nodo.mul(Nodo.num(c * 2 / raiz), Nodo.fn(TipoFuncion.ARCTAN,
                            Nodo.div(Nodo.suma(Nodo.mul(Nodo.num(2), Nodo.var(v)), Nodo.num(p)), Nodo.num(raiz))));
                    regla = ReglaCalculo.INT_INVERSA_TRIG;
                    formula = "∫C/(" + x + "² + p" + x + " + q) d" + x + " = (2C/√(4q - p²))·arctan((2" + x
                            + " + p)/√(4q - p²))";
                }
            }
            Nodo integralSimplificada = Simplificador.simplificar(integral);
            PasoDetallado paso = registro.agregar(regla, regla.getNombre(), prof + 1,
                    "∫ " + Simplificador.simplificar(Nodo.mul(Nodo.num(c), bases.get(j))) + " d" + x, formula);
            paso.setResultado(integralSimplificada.toString());
            total = Nodo.suma(total, integralSimplificada);
        }
        Nodo resultado = Simplificador.simplificar(total);
        encabezado.setResultado(resultado.toString());
        return resultado;
    }

    /**
     * @param g expresión
     * @param v variable
     * @return true si g es un cociente de polinomios en v
     */
    public static boolean esRacional(Nodo g, Variable v) {
        return new IntegradorRacional(null).numeradorDenominador(g, v) != null;
    }

    /** Extrae P y Q si g = P/Q con P y Q polinomios en v. */
    private Polinomio[] numeradorDenominador(Nodo g, Variable v) {
        Simplificador.Monomio m = Integrador.monomio(g);
        if (m.argumentoExp != null) {
            return null;
        }
        Polinomio numerador = new Polinomio(new double[]{m.coeficiente});
        Polinomio denominador = new Polinomio(new double[]{1});
        boolean hayDenominador = false;
        for (Nodo[] factor : m.factores.values()) {
            Nodo exponente = Simplificador.simplificar(factor[1]);
            if (!exponente.esNumero()) {
                return null;
            }
            double e = ((NodoConstante) exponente).getValor();
            if (!FormatoNumeros.esEntero(e)) {
                return null;
            }
            Polinomio base = Polinomio.desde(factor[0], v);
            if (base == null) {
                return null;
            }
            int veces = (int) Math.abs(Math.round(e));
            for (int i = 0; i < veces; i++) {
                if (e > 0) {
                    numerador = numerador.multiplicar(base);
                } else {
                    denominador = denominador.multiplicar(base);
                    hayDenominador = true;
                }
            }
        }
        if (!hayDenominador) {
            return null;
        }
        return new Polinomio[]{numerador, denominador};
    }

    private Nodo integrarPolinomio(Polinomio p, Variable v) {
        double[] c = new double[p.grado() + 2];
        for (int i = 0; i <= p.grado(); i++) {
            c[i + 1] = p.coeficiente(i) / (i + 1);
        }
        return new Polinomio(c).aNodo(v);
    }

    private String describirFactores(List<Polinomio.Factor> factores, Variable v) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < factores.size(); i++) {
            Polinomio.Factor f = factores.get(i);
            if (i > 0) {
                texto.append("·");
            }
            texto.append("(").append(f.aNodo(v)).append(")");
            if (f.multiplicidad > 1) {
                texto.append("^").append(f.multiplicidad);
            }
        }
        return texto.toString();
    }

    /**
     * Eliminación de Gauss con pivoteo parcial.
     *
     * @param a matriz aumentada n × (n + 1)
     * @return solución o null si el sistema es singular
     */
    static double[] resolver(double[][] a) {
        int n = a.length;
        for (int columna = 0; columna < n; columna++) {
            int pivote = columna;
            for (int f = columna + 1; f < n; f++) {
                if (Math.abs(a[f][columna]) > Math.abs(a[pivote][columna])) {
                    pivote = f;
                }
            }
            if (Math.abs(a[pivote][columna]) < 1e-12) {
                return null;
            }
            double[] temporal = a[columna];
            a[columna] = a[pivote];
            a[pivote] = temporal;
            for (int f = columna + 1; f < n; f++) {
                double factor = a[f][columna] / a[columna][columna];
                for (int k = columna; k <= n; k++) {
                    a[f][k] = a[f][k] - factor * a[columna][k];
                }
            }
        }
        double[] x = new double[n];
        for (int f = n - 1; f >= 0; f--) {
            double suma = a[f][n];
            for (int k = f + 1; k < n; k++) {
                suma = suma - a[f][k] * x[k];
            }
            x[f] = suma / a[f][f];
        }
        return x;
    }
}
