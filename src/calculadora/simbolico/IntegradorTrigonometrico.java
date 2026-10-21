package calculadora.simbolico;

import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ReglaCalculo;
import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

/**
 * SUSTITUCIÓN TRIGONOMÉTRICA para integrandos con √(a² − x²), √(a² + x²)
 * o √(x² − a²) (también con potencias −1/2, −3/2 y multiplicados por x² o 1/x²).
 * <table>
 *   <caption>Sustituciones</caption>
 *   <tr><th>Expresión</th><th>Sustitución</th><th>Identidad</th></tr>
 *   <tr><td>√(a² − x²)</td><td>x = a·sinθ</td><td>1 − sin²θ = cos²θ</td></tr>
 *   <tr><td>√(a² + x²)</td><td>x = a·tanθ</td><td>1 + tan²θ = sec²θ</td></tr>
 *   <tr><td>√(x² − a²)</td><td>x = a·secθ</td><td>sec²θ − 1 = tan²θ</td></tr>
 * </table>
 * La integral en θ se resuelve con identidades y el resultado se regresa a x
 * con el triángulo rectángulo de referencia.
 */
public class IntegradorTrigonometrico {

    private static final int SENO = 0;
    private static final int TANGENTE = 1;
    private static final int SECANTE = 2;

    private final Integrador integrador;

    /**
     * @param integrador integrador que aporta la bitácora
     */
    public IntegradorTrigonometrico(Integrador integrador) {
        this.integrador = integrador;
    }

    /**
     * @param g    integrando
     * @param v    variable
     * @param prof profundidad del paso
     * @return antiderivada o null si no corresponde a un caso de la tabla
     */
    public Nodo integrar(Nodo g, Variable v, int prof) {
        Simplificador.Monomio m = Integrador.monomio(g);
        if (m.argumentoExp != null) {
            return null;
        }
        Nodo cuadratica = null;
        double p = Double.NaN;
        double grado = 0;
        for (Nodo[] factor : m.factores.values()) {
            Nodo exponente = Simplificador.simplificar(factor[1]);
            if (!exponente.esNumero()) {
                return null;
            }
            double e = ((NodoConstante) exponente).getValor();
            if (factor[0] instanceof NodoVariable && ((NodoVariable) factor[0]).getVariable() == v) {
                grado = e;
            } else if (cuadratica == null && factor[0].dependeDe(v)) {
                cuadratica = factor[0];
                p = e;
            } else if (factor[0].dependeDe(v)) {
                return null;
            }
        }
        if (cuadratica == null) {
            return null;
        }
        Polinomio poli = Polinomio.desde(cuadratica, v);
        if (poli == null || poli.grado() != 2 || Math.abs(poli.coeficiente(1)) > 1e-12) {
            return null;
        }
        double alfa = poli.coeficiente(0);
        double beta = poli.coeficiente(2);
        int caso;
        if (alfa > 0 && beta < 0) {
            caso = SENO;
        } else if (alfa > 0 && beta > 0) {
            caso = TANGENTE;
        } else if (alfa < 0 && beta > 0) {
            caso = SECANTE;
        } else {
            return null;
        }

        double a2 = Math.abs(alfa / beta);
        Nodo a = Simplificador.simplificar(Nodo.fn(TipoFuncion.RAIZ, Nodo.num(a2)));
        Nodo cuadradoA = Nodo.num(a2);
        Nodo xv = Nodo.var(v);
        // R = √(a² ∓ x²) escrito con la expresión original: √(cuadrática)/√|β|
        Nodo r = Nodo.div(Nodo.fn(TipoFuncion.RAIZ, cuadratica), Nodo.fn(TipoFuncion.RAIZ, Nodo.num(Math.abs(beta))));
        Nodo lnSuma = Nodo.fn(TipoFuncion.LN, Nodo.fn(TipoFuncion.ABS, Nodo.suma(xv, r)));
        Nodo xSobreA = Nodo.div(xv, a);

        Nodo f0 = null;
        String integralTheta = null;
        int m2 = (int) Math.round(grado);
        if (!FormatoNumeros.esEntero(grado)) {
            return null;
        }
        if (caso == SENO) {
            if (m2 == 0 && eq(p, -0.5)) {
                f0 = Nodo.fn(TipoFuncion.ARCSIN, xSobreA);
                integralTheta = "∫ (a·cosθ)/(a·cosθ) dθ = ∫ dθ = θ";
            } else if (m2 == 0 && eq(p, 0.5)) {
                f0 = Nodo.suma(Nodo.div(Nodo.mul(xv, r), Nodo.num(2)),
                        Nodo.mul(Nodo.div(cuadradoA, Nodo.num(2)), Nodo.fn(TipoFuncion.ARCSIN, xSobreA)));
                integralTheta = "∫ a²·cos²θ dθ = (a²/2)·(θ + sinθ·cosθ)";
            } else if (m2 == 2 && eq(p, -0.5)) {
                f0 = Nodo.resta(Nodo.mul(Nodo.div(cuadradoA, Nodo.num(2)), Nodo.fn(TipoFuncion.ARCSIN, xSobreA)),
                        Nodo.div(Nodo.mul(xv, r), Nodo.num(2)));
                integralTheta = "∫ a²·sin²θ dθ = (a²/2)·(θ - sinθ·cosθ)";
            } else if (m2 == -2 && eq(p, -0.5)) {
                f0 = Nodo.neg(Nodo.div(r, Nodo.mul(cuadradoA, xv)));
                integralTheta = "(1/a²)·∫ csc²θ dθ = -cotθ/a²";
            } else if (m2 == 0 && eq(p, -1.5)) {
                f0 = Nodo.div(xv, Nodo.mul(cuadradoA, r));
                integralTheta = "(1/a²)·∫ sec²θ dθ = tanθ/a²";
            } else if (m2 == -2 && eq(p, 0.5)) {
                f0 = Nodo.resta(Nodo.neg(Nodo.div(r, xv)), Nodo.fn(TipoFuncion.ARCSIN, xSobreA));
                integralTheta = "∫ cot²θ dθ = -cotθ - θ";
            }
        } else if (caso == TANGENTE) {
            if (m2 == 0 && eq(p, -1.0)) {
                f0 = Nodo.mul(Nodo.div(Nodo.num(1), a), Nodo.fn(TipoFuncion.ARCTAN, xSobreA));
                integralTheta = "(1/a)·∫ dθ = θ/a";
            } else if (m2 == 0 && eq(p, -0.5)) {
                f0 = lnSuma;
                integralTheta = "∫ secθ dθ = ln|secθ + tanθ|";
            } else if (m2 == 0 && eq(p, 0.5)) {
                f0 = Nodo.suma(Nodo.div(Nodo.mul(xv, r), Nodo.num(2)), Nodo.mul(Nodo.div(cuadradoA, Nodo.num(2)), lnSuma));
                integralTheta = "∫ a²·sec³θ dθ = (a²/2)·(secθ·tanθ + ln|secθ + tanθ|)";
            } else if (m2 == 2 && eq(p, -0.5)) {
                f0 = Nodo.resta(Nodo.div(Nodo.mul(xv, r), Nodo.num(2)), Nodo.mul(Nodo.div(cuadradoA, Nodo.num(2)), lnSuma));
                integralTheta = "∫ a²·tan²θ·secθ dθ = (a²/2)·(secθ·tanθ - ln|secθ + tanθ|)";
            } else if (m2 == -2 && eq(p, -0.5)) {
                f0 = Nodo.neg(Nodo.div(r, Nodo.mul(cuadradoA, xv)));
                integralTheta = "(1/a²)·∫ cosθ/sin²θ dθ = -1/(a²·sinθ)";
            } else if (m2 == 0 && eq(p, -1.5)) {
                f0 = Nodo.div(xv, Nodo.mul(cuadradoA, r));
                integralTheta = "(1/a²)·∫ cosθ dθ = sinθ/a²";
            }
        } else {
            if (m2 == 0 && eq(p, -0.5)) {
                f0 = lnSuma;
                integralTheta = "∫ secθ dθ = ln|secθ + tanθ|";
            } else if (m2 == 0 && eq(p, 0.5)) {
                f0 = Nodo.resta(Nodo.div(Nodo.mul(xv, r), Nodo.num(2)), Nodo.mul(Nodo.div(cuadradoA, Nodo.num(2)), lnSuma));
                integralTheta = "∫ a²·tan²θ·secθ dθ = (a²/2)·(secθ·tanθ - ln|secθ + tanθ|)";
            } else if (m2 == 2 && eq(p, -0.5)) {
                f0 = Nodo.suma(Nodo.div(Nodo.mul(xv, r), Nodo.num(2)), Nodo.mul(Nodo.div(cuadradoA, Nodo.num(2)), lnSuma));
                integralTheta = "∫ a²·sec³θ dθ = (a²/2)·(secθ·tanθ + ln|secθ + tanθ|)";
            } else if (m2 == -2 && eq(p, -0.5)) {
                f0 = Nodo.div(r, Nodo.mul(cuadradoA, xv));
                integralTheta = "(1/a²)·∫ cosθ dθ = sinθ/a²";
            } else if (m2 == 0 && eq(p, -1.5)) {
                f0 = Nodo.neg(Nodo.div(xv, Nodo.mul(cuadradoA, r)));
                integralTheta = "-(1/a²)·∫ cscθ·cotθ dθ = -cscθ/a²";
            }
        }
        if (f0 == null) {
            return null;
        }
        double factorBeta = Math.pow(Math.abs(beta), p);
        Nodo resultado = Simplificador.simplificar(Nodo.mul(Nodo.num(m.coeficiente * factorBeta), f0));
        if (!Integrador.verificar(resultado, g, v)) {
            return null;
        }

        // Pasos
        RegistroPasos registro = integrador.getRegistro();
        String x = v.getSimbolo();
        String forma;
        String sustitucion;
        String identidad;
        String triangulo;
        if (caso == SENO) {
            forma = "a² - " + x + "²";
            sustitucion = x + " = a·sinθ,  d" + x + " = a·cosθ dθ";
            identidad = "a² - a²·sin²θ = a²·cos²θ  ⇒  √(a² - " + x + "²) = a·cosθ";
            triangulo = "sinθ = " + x + "/a,  cosθ = √(a² - " + x + "²)/a";
        } else if (caso == TANGENTE) {
            forma = "a² + " + x + "²";
            sustitucion = x + " = a·tanθ,  d" + x + " = a·sec²θ dθ";
            identidad = "a² + a²·tan²θ = a²·sec²θ  ⇒  √(a² + " + x + "²) = a·secθ";
            triangulo = "tanθ = " + x + "/a,  secθ = √(a² + " + x + "²)/a";
        } else {
            forma = x + "² - a²";
            sustitucion = x + " = a·secθ,  d" + x + " = a·secθ·tanθ dθ";
            identidad = "a²·sec²θ - a² = a²·tan²θ  ⇒  √(" + x + "² - a²) = a·tanθ";
            triangulo = "secθ = " + x + "/a,  tanθ = √(" + x + "² - a²)/a";
        }
        PasoDetallado encabezado = registro.agregar(ReglaCalculo.SUSTITUCION_TRIG,
                ReglaCalculo.SUSTITUCION_TRIG.getNombre(), prof, "∫ " + g + " d" + x,
                "La expresión " + cuadratica + " tiene la forma " + forma + " con a = " + a
                        + (FormatoNumeros.sonIguales(Math.abs(beta), 1.0) ? "" : " (después de sacar el factor "
                        + FormatoNumeros.formatear(Math.abs(beta)) + ")"));
        registro.agregar(ReglaCalculo.SUSTITUCION_TRIG, "Sustitución", prof + 1, "", sustitucion);
        registro.agregar(ReglaCalculo.IDENTIDAD_TRIG, "Identidad pitagórica", prof + 1, "", identidad);
        registro.agregar(ReglaCalculo.SUSTITUCION_TRIG, "Integral en θ", prof + 1, "", integralTheta);
        PasoDetallado regreso = registro.agregar(ReglaCalculo.SUSTITUCION_TRIG, "Regreso a " + x
                + " con el triángulo de referencia", prof + 1, "", triangulo
                + " (las constantes como ln(a) se incluyen en C)");
        regreso.setResultado(resultado.toString());
        encabezado.setResultado(resultado.toString());
        return resultado;
    }

    private static boolean eq(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }
}
