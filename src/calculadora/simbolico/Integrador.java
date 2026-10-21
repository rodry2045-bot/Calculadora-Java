package calculadora.simbolico;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.OperacionNoSoportadaException;
import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ReglaCalculo;
import calculadora.modelo.TecnicaIntegracion;
import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Integración simbólica (antiderivadas) con registro del procedimiento.
 * <p>
 * Orden en el que se prueban los métodos (modo automático):
 * </p>
 * <ol>
 *   <li>Integral de una constante.</li>
 *   <li>Propiedad de la suma y la resta (se integra término a término).</li>
 *   <li>Propiedad del múltiplo constante (se saca la constante).</li>
 *   <li>Tabla de integrales inmediatas (potencia, 1/x, exponenciales,
 *       trigonométricas, arctan, arcsin), con argumento lineal.</li>
 *   <li>Identidades trigonométricas (sin², cos², tan²).</li>
 *   <li>Desarrollo de polinomios.</li>
 *   <li>Sustitución (cambio de variable u = g(x)).</li>
 *   <li>Fracciones parciales (funciones racionales).</li>
 *   <li>Distribución de productos.</li>
 *   <li>Sustitución trigonométrica.</li>
 *   <li>Integración por partes (criterio LIATE, incluye integrales cíclicas).</li>
 * </ol>
 * Al final el resultado SIEMPRE se verifica derivándolo numéricamente.
 */
public class Integrador {

    private static final int MAXIMO_LLAMADAS = 600;

    private final RegistroPasos registro;
    private final Derivador derivador;
    private int llamadas;

    /**
     * @param registro bitácora de pasos
     */
    public Integrador(RegistroPasos registro) {
        this.registro = registro;
        this.derivador = new Derivador();
    }

    /** @return bitácora usada */
    public RegistroPasos getRegistro() {
        return registro;
    }

    /**
     * Calcula una antiderivada de f respecto a v (sin la constante C) y la verifica.
     *
     * @param f       integrando
     * @param v       variable de integración
     * @param tecnica técnica elegida
     * @return antiderivada simplificada
     * @throws CalculadoraException si no se encuentra un método o la verificación falla
     */
    public Nodo integrar(Nodo f, Variable v, TecnicaIntegracion tecnica) throws CalculadoraException {
        llamadas = 0;
        Nodo g = s(f);
        Nodo antiderivada;
        if (tecnica == TecnicaIntegracion.AUTOMATICO) {
            antiderivada = integrarRec(g, v, 0, true);
        } else {
            antiderivada = forzar(g, v, tecnica);
        }
        antiderivada = s(antiderivada);
        if (!verificar(antiderivada, g, v)) {
            throw new OperacionNoSoportadaException("No se pudo verificar la antiderivada obtenida para " + g
                    + ". Intente con otro método.");
        }
        PasoDetallado paso = registro.agregar(ReglaCalculo.VERIFICACION, ReglaCalculo.VERIFICACION.getNombre(), 0,
                "d/d" + v.getSimbolo() + "[" + antiderivada + "]",
                "Al derivar el resultado se obtiene de nuevo el integrando (comprobado numéricamente)");
        paso.setResultado(g.toString());
        return antiderivada;
    }

    /**
     * Integra sin verificar ni registrar pasos de verificación (uso interno).
     */
    Nodo integrarInterno(Nodo f, Variable v) throws OperacionNoSoportadaException {
        return s(integrarRec(s(f), v, 0, true));
    }

    private Nodo forzar(Nodo g, Variable v, TecnicaIntegracion tecnica) throws CalculadoraException {
        Nodo resultado = null;
        String ejemplo;
        if (tecnica == TecnicaIntegracion.SUSTITUCION) {
            resultado = sustitucion(g, v, 0, true);
            ejemplo = "2x·cos(x^2)  o  x·e^(x^2)";
        } else if (tecnica == TecnicaIntegracion.PARTES) {
            resultado = partes(g, v, 0);
            ejemplo = "x·e^x,  x·sin(x)  o  ln(x)";
        } else if (tecnica == TecnicaIntegracion.FRACCIONES) {
            resultado = new IntegradorRacional(this).integrar(g, v, 0);
            ejemplo = "(3x + 5)/(x^2 + x - 2)  o  1/(x^2 - 1)";
        } else {
            resultado = new IntegradorTrigonometrico(this).integrar(g, v, 0);
            ejemplo = "1/√(4 - x^2),  √(9 - x^2)  o  1/√(x^2 + 1)";
        }
        if (resultado == null) {
            throw new OperacionNoSoportadaException("El método elegido no se puede aplicar a " + g
                    + ". Ejemplos donde sí funciona: " + ejemplo + ". Pruebe con el modo Automático.");
        }
        return resultado;
    }

    // ==================================================================
    // Algoritmo principal
    // ==================================================================

    /**
     * Integra recursivamente probando los métodos en orden.
     *
     * @param g              integrando (simplificado)
     * @param v              variable
     * @param prof           profundidad del paso
     * @param permitirPartes false para evitar ciclos dentro de integración por partes
     * @return antiderivada
     * @throws OperacionNoSoportadaException si ningún método funciona
     */
    Nodo integrarRec(Nodo g, Variable v, int prof, boolean permitirPartes) throws OperacionNoSoportadaException {
        llamadas++;
        if (llamadas > MAXIMO_LLAMADAS || prof > 12) {
            throw new OperacionNoSoportadaException("La integral es demasiado compleja para el motor");
        }
        g = s(g);
        String x = v.getSimbolo();
        String integral = "∫ " + g + " d" + x;

        // 1) Constante
        if (!g.dependeDe(v)) {
            PasoDetallado paso = registro.agregar(ReglaCalculo.INT_CONSTANTE, ReglaCalculo.INT_CONSTANTE.getNombre(),
                    prof, integral, "∫k d" + x + " = k·" + x + "  con k = " + g);
            Nodo resultado = s(Nodo.mul(g, Nodo.var(v)));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // 2) Suma y resta
        if (g instanceof NodoSuma || g instanceof NodoResta) {
            List<Nodo> terminos = new ArrayList<Nodo>();
            List<Integer> signos = new ArrayList<Integer>();
            aplanarSuma(g, 1, terminos, signos);
            PasoDetallado paso = registro.agregar(ReglaCalculo.INT_SUMA, ReglaCalculo.INT_SUMA.getNombre(), prof,
                    integral, "∫(f ± g) d" + x + " = ∫f d" + x + " ± ∫g d" + x
                            + ": se integra cada término por separado");
            Nodo total = Nodo.num(0);
            for (int i = 0; i < terminos.size(); i++) {
                Nodo parcial = integrarRec(terminos.get(i), v, prof + 1, permitirPartes);
                total = signos.get(i).intValue() < 0 ? Nodo.resta(total, parcial) : Nodo.suma(total, parcial);
            }
            Nodo resultado = s(total);
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // 3) Múltiplo constante
        Nodo[] partes = separarConstante(g, v);
        if (!partes[0].esNumero(1.0)) {
            PasoDetallado paso = registro.agregar(ReglaCalculo.INT_MULTIPLO, ReglaCalculo.INT_MULTIPLO.getNombre(),
                    prof, integral, "∫c·f d" + x + " = c·∫f d" + x + "  con c = " + partes[0] + " y f = " + partes[1]);
            Nodo interior = integrarRec(partes[1], v, prof + 1, permitirPartes);
            Nodo resultado = s(Nodo.mul(partes[0], interior));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // 4) Tabla de integrales inmediatas
        Object[] inmediata = tabla(g, v);
        if (inmediata != null) {
            ReglaCalculo regla = (ReglaCalculo) inmediata[1];
            PasoDetallado paso = registro.agregar(regla, regla.getNombre(), prof, integral, (String) inmediata[2]);
            Nodo resultado = s((Nodo) inmediata[0]);
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // 5) Identidades trigonométricas
        Nodo identidad = identidadTrigonometrica(g, v);
        if (identidad != null) {
            PasoDetallado paso = registro.agregar(ReglaCalculo.IDENTIDAD_TRIG, ReglaCalculo.IDENTIDAD_TRIG.getNombre(),
                    prof, integral, "Se reescribe " + g + " = " + identidad + " para poder integrar");
            Nodo resultado = s(integrarRec(identidad, v, prof + 1, permitirPartes));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // 6) Desarrollo de polinomios: (x+1)(x-2), (x^2+1)^2...
        Polinomio polinomio = Polinomio.desde(g, v);
        if (polinomio != null) {
            Nodo desarrollado = polinomio.aNodo(v);
            if (!desarrollado.igualA(g)) {
                PasoDetallado paso = registro.agregar(ReglaCalculo.DESARROLLO, ReglaCalculo.DESARROLLO.getNombre(),
                        prof, integral, "Se desarrolla el polinomio: " + g + " = " + desarrollado);
                Nodo resultado = s(integrarRec(desarrollado, v, prof + 1, permitirPartes));
                paso.setResultado(resultado.toString());
                return resultado;
            }
        }

        // 7) Sustitución
        Nodo porSustitucion = sustitucion(g, v, prof, permitirPartes);
        if (porSustitucion != null) {
            return porSustitucion;
        }

        // 8) Fracciones parciales
        Nodo racional = new IntegradorRacional(this).integrar(g, v, prof);
        if (racional != null) {
            return racional;
        }

        // 9) Distribución: x·(x + e^x) = x^2 + x·e^x
        Nodo distribuido = distribuir(g, v);
        if (distribuido != null) {
            PasoDetallado paso = registro.agregar(ReglaCalculo.DESARROLLO, ReglaCalculo.DESARROLLO.getNombre(), prof,
                    integral, "Se aplica la propiedad distributiva: " + g + " = " + distribuido);
            Nodo resultado = s(integrarRec(distribuido, v, prof + 1, permitirPartes));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // 10) Sustitución trigonométrica
        Nodo trigonometrica = new IntegradorTrigonometrico(this).integrar(g, v, prof);
        if (trigonometrica != null) {
            return trigonometrica;
        }

        // 11) Por partes
        if (permitirPartes) {
            Nodo porPartes = partes(g, v, prof);
            if (porPartes != null) {
                return porPartes;
            }
        }
        throw new OperacionNoSoportadaException("No se encontró un método para integrar " + g
                + ". Puede que no tenga antiderivada elemental (por ejemplo e^(x^2) o sin(x)/x).");
    }

    // ==================================================================
    // Tabla de integrales inmediatas
    // ==================================================================

    /**
     * Busca g en la tabla de integrales inmediatas.
     *
     * @return {resultado, regla, fórmula} o null
     */
    private Object[] tabla(Nodo g, Variable v) {
        String x = v.getSimbolo();
        Simplificador.Monomio m = monomio(g);
        List<Nodo[]> factores = new ArrayList<Nodo[]>(m.factores.values());

        // e^(ax + b)
        if (factores.isEmpty() && m.argumentoExp != null) {
            double[] lin = lineal(m.argumentoExp, v);
            if (lin != null) {
                Nodo u = m.argumentoExp;
                return new Object[]{Nodo.div(Nodo.fn(TipoFuncion.EXP, u), Nodo.num(lin[0])), ReglaCalculo.INT_EXPONENCIAL,
                        "∫e^(k" + x + ") d" + x + " = e^(k" + x + ")/k" + notaLineal(u, lin, v)};
            }
            return null;
        }
        if (factores.size() == 1 && m.argumentoExp == null) {
            Nodo base = factores.get(0)[0];
            Nodo exponente = s(factores.get(0)[1]);
            if (exponente.esNumero()) {
                double n = ((NodoConstante) exponente).getValor();
                double[] lin = lineal(base, v);
                if (lin != null) {
                    boolean esVariable = base instanceof NodoVariable;
                    if (FormatoNumeros.sonIguales(n, -1.0)) {
                        Nodo r = Nodo.div(Nodo.fn(TipoFuncion.LN, Nodo.fn(TipoFuncion.ABS, base)), Nodo.num(lin[0]));
                        return new Object[]{r, ReglaCalculo.INT_LOGARITMO, "∫1/" + x + " d" + x + " = ln|" + x + "|"
                                + (esVariable ? "" : notaLineal(base, lin, v))};
                    }
                    Nodo r = Nodo.div(Nodo.pot(base, n + 1), Nodo.num(lin[0] * (n + 1)));
                    return new Object[]{r, ReglaCalculo.INT_POTENCIA, "∫" + x + "^n d" + x + " = " + x + "^(n+1)/(n+1)  con n = "
                            + exponente + ": se suma 1 al exponente y se divide entre el nuevo exponente"
                            + (esVariable ? "" : notaLineal(base, lin, v))};
                }
                if (base instanceof NodoFuncion) {
                    NodoFuncion funcion = (NodoFuncion) base;
                    Nodo u = funcion.getArgumento();
                    double[] linU = lineal(u, v);
                    if (linU != null) {
                        Object[] trig = tablaTrigonometrica(funcion.getTipo(), n, u, linU, v);
                        if (trig != null) {
                            return trig;
                        }
                    }
                }
                // Cuadráticas sin término lineal: 1/(a² + x²) y 1/√(a² − x²)
                Polinomio p = Polinomio.desde(base, v);
                if (p != null && p.grado() == 2 && Math.abs(p.coeficiente(1)) < 1e-12) {
                    double alfa = p.coeficiente(0);
                    double beta = p.coeficiente(2);
                    if (FormatoNumeros.sonIguales(n, -1.0) && alfa > 0 && beta > 0) {
                        Nodo r = Nodo.mul(Nodo.div(Nodo.num(1), Nodo.fn(TipoFuncion.RAIZ, Nodo.num(alfa * beta))),
                                Nodo.fn(TipoFuncion.ARCTAN, Nodo.mul(Nodo.var(v),
                                        Nodo.fn(TipoFuncion.RAIZ, Nodo.num(beta / alfa)))));
                        return new Object[]{r, ReglaCalculo.INT_INVERSA_TRIG,
                                "∫1/(a² + " + x + "²) d" + x + " = (1/a)·arctan(" + x + "/a)"};
                    }
                    if (FormatoNumeros.sonIguales(n, -0.5) && alfa > 0 && beta < 0) {
                        Nodo r = Nodo.mul(Nodo.div(Nodo.num(1), Nodo.fn(TipoFuncion.RAIZ, Nodo.num(-beta))),
                                Nodo.fn(TipoFuncion.ARCSIN, Nodo.mul(Nodo.var(v),
                                        Nodo.fn(TipoFuncion.RAIZ, Nodo.num(-beta / alfa)))));
                        return new Object[]{r, ReglaCalculo.INT_INVERSA_TRIG,
                                "∫1/√(a² - " + x + "²) d" + x + " = arcsin(" + x + "/a)"};
                    }
                }
            } else if (base.esNumero() && ((NodoConstante) base).getValor() > 0) {
                // a^(kx + b)
                double[] lin = lineal(exponente, v);
                if (lin != null) {
                    Nodo r = Nodo.div(Nodo.pot(base, exponente), Nodo.mul(Nodo.num(lin[0]), Nodo.fn(TipoFuncion.LN, base)));
                    return new Object[]{r, ReglaCalculo.INT_EXPONENCIAL,
                            "∫a^" + x + " d" + x + " = a^" + x + "/ln(a)  con a = " + base + notaLineal(exponente, lin, v)};
                }
            }
        }
        // sec(u)·tan(u) y csc(u)·cot(u)
        if (factores.size() == 2 && m.argumentoExp == null && s(factores.get(0)[1]).esNumero(1.0)
                && s(factores.get(1)[1]).esNumero(1.0) && factores.get(0)[0] instanceof NodoFuncion
                && factores.get(1)[0] instanceof NodoFuncion) {
            NodoFuncion f1 = (NodoFuncion) factores.get(0)[0];
            NodoFuncion f2 = (NodoFuncion) factores.get(1)[0];
            if (f1.getArgumento().igualA(f2.getArgumento())) {
                Nodo u = f1.getArgumento();
                double[] lin = lineal(u, v);
                if (lin != null) {
                    boolean secTan = (f1.getTipo() == TipoFuncion.SEC && f2.getTipo() == TipoFuncion.TAN)
                            || (f1.getTipo() == TipoFuncion.TAN && f2.getTipo() == TipoFuncion.SEC);
                    boolean cscCot = (f1.getTipo() == TipoFuncion.CSC && f2.getTipo() == TipoFuncion.COT)
                            || (f1.getTipo() == TipoFuncion.COT && f2.getTipo() == TipoFuncion.CSC);
                    if (secTan) {
                        return new Object[]{Nodo.div(Nodo.fn(TipoFuncion.SEC, u), Nodo.num(lin[0])),
                                ReglaCalculo.INT_TRIGONOMETRICA, "∫sec(u)·tan(u) du = sec(u)" + notaLineal(u, lin, v)};
                    }
                    if (cscCot) {
                        return new Object[]{Nodo.div(Nodo.neg(Nodo.fn(TipoFuncion.CSC, u)), Nodo.num(lin[0])),
                                ReglaCalculo.INT_TRIGONOMETRICA, "∫csc(u)·cot(u) du = -csc(u)" + notaLineal(u, lin, v)};
                    }
                }
            }
        }
        return null;
    }

    private Object[] tablaTrigonometrica(TipoFuncion tipo, double n, Nodo u, double[] lin, Variable v) {
        Nodo k = Nodo.num(lin[0]);
        String nota = notaLineal(u, lin, v);
        Nodo r = null;
        String formula = null;
        if (FormatoNumeros.sonIguales(n, 1.0)) {
            switch (tipo) {
                case SIN:
                    r = Nodo.neg(Nodo.fn(TipoFuncion.COS, u));
                    formula = "∫sin(u) du = -cos(u)";
                    break;
                case COS:
                    r = Nodo.fn(TipoFuncion.SIN, u);
                    formula = "∫cos(u) du = sin(u)";
                    break;
                case TAN:
                    r = Nodo.neg(Nodo.fn(TipoFuncion.LN, Nodo.fn(TipoFuncion.ABS, Nodo.fn(TipoFuncion.COS, u))));
                    formula = "∫tan(u) du = -ln|cos(u)|";
                    break;
                case COT:
                    r = Nodo.fn(TipoFuncion.LN, Nodo.fn(TipoFuncion.ABS, Nodo.fn(TipoFuncion.SIN, u)));
                    formula = "∫cot(u) du = ln|sin(u)|";
                    break;
                case SEC:
                    r = Nodo.fn(TipoFuncion.LN, Nodo.fn(TipoFuncion.ABS,
                            Nodo.suma(Nodo.fn(TipoFuncion.SEC, u), Nodo.fn(TipoFuncion.TAN, u))));
                    formula = "∫sec(u) du = ln|sec(u) + tan(u)|";
                    break;
                case CSC:
                    r = Nodo.neg(Nodo.fn(TipoFuncion.LN, Nodo.fn(TipoFuncion.ABS,
                            Nodo.suma(Nodo.fn(TipoFuncion.CSC, u), Nodo.fn(TipoFuncion.COT, u)))));
                    formula = "∫csc(u) du = -ln|csc(u) + cot(u)|";
                    break;
                default:
                    break;
            }
        } else if (FormatoNumeros.sonIguales(n, 2.0)) {
            if (tipo == TipoFuncion.SEC) {
                r = Nodo.fn(TipoFuncion.TAN, u);
                formula = "∫sec(u)² du = tan(u)";
            } else if (tipo == TipoFuncion.CSC) {
                r = Nodo.neg(Nodo.fn(TipoFuncion.COT, u));
                formula = "∫csc(u)² du = -cot(u)";
            }
        }
        if (r == null) {
            return null;
        }
        return new Object[]{Nodo.div(r, k), ReglaCalculo.INT_TRIGONOMETRICA, formula + nota};
    }

    private String notaLineal(Nodo u, double[] lin, Variable v) {
        if (u instanceof NodoVariable) {
            return "";
        }
        return "  (cambio de variable implícito u = " + u + ", du = " + FormatoNumeros.formatear(lin[0]) + " d"
                + v.getSimbolo() + ": se divide entre " + FormatoNumeros.formatear(lin[0]) + ")";
    }

    // ==================================================================
    // Identidades, distribución y constantes
    // ==================================================================

    private Nodo identidadTrigonometrica(Nodo g, Variable v) {
        if (!(g instanceof NodoPotencia)) {
            return null;
        }
        NodoPotencia p = (NodoPotencia) g;
        if (!p.getExponente().esNumero(2.0) || !(p.getBase() instanceof NodoFuncion)) {
            return null;
        }
        NodoFuncion f = (NodoFuncion) p.getBase();
        Nodo u = f.getArgumento();
        if (lineal(u, v) == null) {
            return null;
        }
        Nodo dosU = s(Nodo.mul(Nodo.num(2), u));
        if (f.getTipo() == TipoFuncion.SIN) {
            return Nodo.resta(Nodo.num(0.5), Nodo.div(Nodo.fn(TipoFuncion.COS, dosU), Nodo.num(2)));
        }
        if (f.getTipo() == TipoFuncion.COS) {
            return Nodo.suma(Nodo.num(0.5), Nodo.div(Nodo.fn(TipoFuncion.COS, dosU), Nodo.num(2)));
        }
        if (f.getTipo() == TipoFuncion.TAN) {
            return Nodo.resta(Nodo.pot(Nodo.fn(TipoFuncion.SEC, u), 2), Nodo.num(1));
        }
        if (f.getTipo() == TipoFuncion.COT) {
            return Nodo.resta(Nodo.pot(Nodo.fn(TipoFuncion.CSC, u), 2), Nodo.num(1));
        }
        return null;
    }

    /** Distribuye un producto que contiene una suma con exponente 1. */
    private Nodo distribuir(Nodo g, Variable v) {
        Simplificador.Monomio m = monomio(g);
        Nodo suma = null;
        String claveSuma = null;
        for (Map.Entry<String, Nodo[]> entrada : m.factores.entrySet()) {
            Nodo base = entrada.getValue()[0];
            if ((base instanceof NodoSuma || base instanceof NodoResta) && s(entrada.getValue()[1]).esNumero(1.0)
                    && base.dependeDe(v)) {
                suma = base;
                claveSuma = entrada.getKey();
                break;
            }
        }
        if (suma == null || m.factores.size() + (m.argumentoExp == null ? 0 : 1) < 2) {
            return null;
        }
        Simplificador.Monomio resto = m.copiaSinCoeficiente();
        resto.coeficiente = m.coeficiente;
        resto.factores.remove(claveSuma);
        Nodo otro = Simplificador.construir(resto);
        List<Nodo> terminos = new ArrayList<Nodo>();
        List<Integer> signos = new ArrayList<Integer>();
        aplanarSuma(suma, 1, terminos, signos);
        Nodo total = Nodo.num(0);
        for (int i = 0; i < terminos.size(); i++) {
            Nodo producto = Nodo.mul(otro, terminos.get(i));
            total = signos.get(i).intValue() < 0 ? Nodo.resta(total, producto) : Nodo.suma(total, producto);
        }
        Nodo resultado = s(total);
        if (!(resultado instanceof NodoSuma || resultado instanceof NodoResta)) {
            return null;
        }
        return resultado;
    }

    /**
     * Separa g = c·f donde c no depende de v.
     *
     * @return {c, f}
     */
    Nodo[] separarConstante(Nodo g, Variable v) {
        Simplificador.Monomio m = monomio(g);
        Simplificador.Monomio constante = new Simplificador.Monomio();
        Simplificador.Monomio variable = new Simplificador.Monomio();
        constante.coeficiente = m.coeficiente;
        for (Map.Entry<String, Nodo[]> entrada : m.factores.entrySet()) {
            Nodo base = entrada.getValue()[0];
            Nodo exponente = entrada.getValue()[1];
            if (base.dependeDe(v) || exponente.dependeDe(v)) {
                variable.factores.put(entrada.getKey(), new Nodo[]{base, exponente});
            } else {
                constante.factores.put(entrada.getKey(), new Nodo[]{base, exponente});
            }
        }
        if (m.argumentoExp != null) {
            if (m.argumentoExp.dependeDe(v)) {
                // e^(ax + b) = e^b·e^(ax): se deja junto para no complicar
                variable.argumentoExp = m.argumentoExp;
            } else {
                constante.argumentoExp = m.argumentoExp;
            }
        }
        return new Nodo[]{Simplificador.construir(constante), Simplificador.construir(variable)};
    }

    // ==================================================================
    // Sustitución
    // ==================================================================

    /**
     * Método de sustitución: se busca u = g(x) tal que el integrando sea
     * f(u)·u'(x). Se prueba cada subexpresión candidata.
     */
    Nodo sustitucion(Nodo g, Variable v, int prof, boolean permitirPartes) {
        if (prof > 6) {
            return null;
        }
        List<Nodo> candidatos = new ArrayList<Nodo>();
        Map<String, Boolean> vistos = new LinkedHashMap<String, Boolean>();
        recolectarCandidatos(g, v, candidatos, vistos);
        String x = v.getSimbolo();
        Nodo u = Nodo.var(Variable.U);

        for (int i = 0; i < candidatos.size(); i++) {
            Nodo candidato = candidatos.get(i);
            if (candidato.igualA(g)) {
                continue;
            }
            Nodo du = derivador.derivar(candidato, v);
            if (du.esNumero(0.0)) {
                continue;
            }
            Nodo cociente = s(Nodo.div(g, du));
            Nodo enU = s(cociente.reemplazar(candidato, u));
            if (enU.dependeDe(v)) {
                double[] lin = lineal(candidato, v);
                // Para funciones racionales es mejor usar fracciones parciales
                if (lin == null || IntegradorRacional.esRacional(g, v)) {
                    continue;
                }
                // Si u = ax + b, entonces x = (u − b)/a
                Nodo xEnU = Nodo.div(Nodo.resta(u, Nodo.num(lin[1])), Nodo.num(lin[0]));
                enU = s(enU.reemplazar(Nodo.var(v), xEnU));
                if (enU.dependeDe(v)) {
                    continue;
                }
            }
            if (enU.igualA(g.reemplazar(Nodo.var(v), u))) {
                continue; // no hubo progreso
            }
            Object[] marca = registro.marca();
            PasoDetallado paso = registro.agregar(ReglaCalculo.SUSTITUCION, ReglaCalculo.SUSTITUCION.getNombre(), prof,
                    "∫ " + g + " d" + x, "Sea u = " + candidato + ", entonces du = " + du + " d" + x
                            + ". La integral se transforma en ∫ " + enU + " du");
            try {
                Nodo enTerminosDeU = integrarRec(enU, Variable.U, prof + 1, permitirPartes);
                Nodo resultado = s(s(enTerminosDeU).reemplazar(u, candidato));
                PasoDetallado regreso = registro.agregar(ReglaCalculo.SUSTITUCION, "Regreso a la variable " + x,
                        prof + 1, "u = " + candidato, "Se sustituye u por " + candidato + " en " + s(enTerminosDeU));
                regreso.setResultado(resultado.toString());
                paso.setResultado(resultado.toString());
                return resultado;
            } catch (OperacionNoSoportadaException e) {
                registro.restaurar(marca);
            }
        }
        return null;
    }

    private void recolectarCandidatos(Nodo n, Variable v, List<Nodo> lista, Map<String, Boolean> vistos) {
        if (!n.dependeDe(v) || n instanceof NodoVariable) {
            return;
        }
        if (n instanceof NodoFuncion) {
            agregarCandidato(((NodoFuncion) n).getArgumento(), v, lista, vistos);
            agregarCandidato(n, v, lista, vistos);
            recolectarCandidatos(((NodoFuncion) n).getArgumento(), v, lista, vistos);
            return;
        }
        if (n instanceof NodoPotencia) {
            agregarCandidato(((NodoPotencia) n).getBase(), v, lista, vistos);
            recolectarCandidatos(((NodoPotencia) n).getBase(), v, lista, vistos);
            recolectarCandidatos(((NodoPotencia) n).getExponente(), v, lista, vistos);
            return;
        }
        if (n instanceof NodoCociente) {
            agregarCandidato(((NodoCociente) n).getDenominador(), v, lista, vistos);
        }
        if (n instanceof NodoSuma || n instanceof NodoResta) {
            agregarCandidato(n, v, lista, vistos);
        }
        if (n instanceof NodoBinario) {
            recolectarCandidatos(((NodoBinario) n).getIzquierdo(), v, lista, vistos);
            recolectarCandidatos(((NodoBinario) n).getDerecho(), v, lista, vistos);
        } else if (n instanceof NodoNegativo) {
            recolectarCandidatos(((NodoNegativo) n).getArgumento(), v, lista, vistos);
        }
    }

    private void agregarCandidato(Nodo c, Variable v, List<Nodo> lista, Map<String, Boolean> vistos) {
        if (!c.dependeDe(v) || c instanceof NodoVariable) {
            return;
        }
        String clave = c.clave();
        if (!vistos.containsKey(clave)) {
            vistos.put(clave, Boolean.TRUE);
            lista.add(c);
        }
    }

    // ==================================================================
    // Integración por partes
    // ==================================================================

    /**
     * Integración por partes con detección de integrales cíclicas
     * (como ∫e^x·sin(x) dx, donde la integral original vuelve a aparecer).
     */
    Nodo partes(Nodo g, Variable v, int prof) {
        Object[] marca = registro.marca();
        Nodo[] ak = partesCiclo(g, v, prof, 0, g);
        if (ak == null) {
            registro.restaurar(marca);
            return null;
        }
        Nodo a = ak[0];
        Nodo k = ak[1];
        if (k.esNumero(0.0)) {
            return s(a);
        }
        if (k.esNumero(1.0)) {
            registro.restaurar(marca);
            return null;
        }
        Nodo resultado = s(Nodo.div(a, Nodo.resta(Nodo.num(1), k)));
        PasoDetallado paso = registro.agregar(ReglaCalculo.PARTES, "Integral cíclica", prof, "I = ∫ " + g + " d"
                + v.getSimbolo(), "La integral original reaparece: I = " + s(a) + " + (" + k + ")·I  ⇒  I = A/(1 - k)");
        paso.setResultado(resultado.toString());
        return resultado;
    }

    /**
     * @return {A, k} tales que ∫g = A + k·∫origen, o null si no se puede
     */
    private Nodo[] partesCiclo(Nodo g, Variable v, int prof, int nivel, Nodo origen) {
        if (nivel > 3) {
            return null;
        }
        String x = v.getSimbolo();
        Nodo[] separado = separarConstante(g, v);
        Nodo constante = separado[0];
        Nodo[] uDv = elegirLiate(separado[1], v);
        if (uDv == null) {
            return null;
        }
        Nodo u = uDv[0];
        Nodo dv = uDv[1];
        Nodo du = derivador.derivar(u, v);
        Nodo vInt;
        try {
            vInt = new Integrador(new RegistroPasos(false)).integrarInterno(dv, v);
        } catch (OperacionNoSoportadaException e) {
            return null;
        }
        Nodo nueva = s(Nodo.mul(constante, Nodo.mul(vInt, du)));

        PasoDetallado paso = registro.agregar(ReglaCalculo.PARTES, ReglaCalculo.PARTES.getNombre(), prof,
                "∫ " + g + " d" + x, "∫u dv = u·v - ∫v du. Por el criterio LIATE se elige u = " + u + " y dv = " + dv
                        + " d" + x + "; entonces du = " + du + " d" + x + " y v = " + vInt + ". Queda: "
                        + s(Nodo.mul(constante, Nodo.mul(u, vInt))) + " - ∫ " + nueva + " d" + x);

        Nodo[] sub;
        Nodo razon = s(Nodo.div(nueva, origen));
        if (!razon.dependeDe(v) && razon.esConstante()) {
            sub = new Nodo[]{Nodo.num(0), razon};
        } else {
            Object[] marca = registro.marca();
            try {
                sub = new Nodo[]{integrarRec(nueva, v, prof + 1, false), Nodo.num(0)};
            } catch (OperacionNoSoportadaException e) {
                registro.restaurar(marca);
                sub = partesCiclo(nueva, v, prof + 1, nivel + 1, origen);
                if (sub == null) {
                    return null;
                }
            }
        }
        Nodo a = s(Nodo.resta(Nodo.mul(constante, Nodo.mul(u, vInt)), sub[0]));
        Nodo k = s(Nodo.neg(sub[1]));
        if (k.esNumero(0.0)) {
            paso.setResultado(a.toString());
        }
        return new Nodo[]{a, k};
    }

    /**
     * Criterio LIATE: Logarítmicas, Inversas trigonométricas, Algebraicas,
     * Trigonométricas, Exponenciales. La primera de la lista es u.
     *
     * @return {u, dv} o null
     */
    private Nodo[] elegirLiate(Nodo g, Variable v) {
        Simplificador.Monomio m = monomio(g);
        List<Nodo> factores = new ArrayList<Nodo>();
        for (Nodo[] f : m.factores.values()) {
            factores.add(s(f[1]).esNumero(1.0) ? f[0] : s(Nodo.pot(f[0], f[1])));
        }
        if (m.argumentoExp != null) {
            factores.add(Nodo.fn(TipoFuncion.EXP, m.argumentoExp));
        }
        int mejor = -1;
        int rangoMejor = 0;
        for (int i = 0; i < factores.size(); i++) {
            int r = rangoLiate(factores.get(i), v);
            if (r > rangoMejor) {
                rangoMejor = r;
                mejor = i;
            }
        }
        if (mejor < 0) {
            return null;
        }
        if (factores.size() == 1 && rangoMejor < 4) {
            return null; // solo ln, log y funciones inversas se integran por partes con dv = dx
        }
        Nodo dv = Nodo.num(m.coeficiente);
        for (int i = 0; i < factores.size(); i++) {
            if (i != mejor) {
                dv = Nodo.mul(dv, factores.get(i));
            }
        }
        return new Nodo[]{factores.get(mejor), s(dv)};
    }

    private int rangoLiate(Nodo f, Variable v) {
        if (f instanceof NodoFuncion) {
            TipoFuncion t = ((NodoFuncion) f).getTipo();
            if (t == TipoFuncion.LN || t == TipoFuncion.LOG) {
                return 5;
            }
            if (t.esInversa()) {
                return 4;
            }
            if (t.esTrigonometrica()) {
                return 2;
            }
            if (t == TipoFuncion.EXP) {
                return 1;
            }
            return 0;
        }
        if (f instanceof NodoPotencia && ((NodoPotencia) f).getBase() instanceof NodoFuncion) {
            int r = rangoLiate(((NodoPotencia) f).getBase(), v);
            return r == 5 ? 5 : (r == 2 ? 2 : 0);
        }
        if (f instanceof NodoPotencia && ((NodoPotencia) f).getBase().esNumero()) {
            return 1; // a^x
        }
        Polinomio p = Polinomio.desde(f, v);
        if (p != null && p.grado() >= 1) {
            return 3;
        }
        if (f instanceof NodoPotencia && ((NodoPotencia) f).getBase() instanceof NodoVariable
                && ((NodoPotencia) f).getExponente().esNumero()) {
            return 3;
        }
        return 0;
    }

    // ==================================================================
    // Verificación y utilidades
    // ==================================================================

    /**
     * Comprueba numéricamente que dF/dx = f en varios puntos.
     *
     * @param antiderivada F
     * @param integrando   f
     * @param v            variable
     * @return true si coinciden
     */
    public static boolean verificar(Nodo antiderivada, Nodo integrando, Variable v) {
        double[] puntos = {0.37, 0.71, 1.23, 1.71, 2.3, 3.1, 0.13, 0.52, 0.88, -0.29, -0.63, -1.4, 4.7, 7.9, -2.6};
        int validos = 0;
        for (int i = 0; i < puntos.length; i++) {
            double t = puntos[i];
            double f = integrando.evaluarEn(v, t, 1.0);
            if (Double.isNaN(f) || Double.isInfinite(f) || Math.abs(f) > 1e6) {
                continue;
            }
            double h = 1e-5 * Math.max(1, Math.abs(t));
            double fMas = antiderivada.evaluarEn(v, t + h, 1.0);
            double fMenos = antiderivada.evaluarEn(v, t - h, 1.0);
            if (Double.isNaN(fMas) || Double.isNaN(fMenos) || Double.isInfinite(fMas) || Double.isInfinite(fMenos)) {
                continue;
            }
            double derivada = (fMas - fMenos) / (2 * h);
            if (Math.abs(derivada - f) > 1e-4 * (1 + Math.abs(f))) {
                return false;
            }
            validos++;
        }
        return validos >= 2;
    }

    /**
     * @return {a, b} si u = a·v + b con a ≠ 0; null si no es lineal
     */
    static double[] lineal(Nodo u, Variable v) {
        Polinomio p = Polinomio.desde(u, v);
        if (p == null || p.grado() != 1) {
            return null;
        }
        return new double[]{p.coeficiente(1), p.coeficiente(0)};
    }

    static Simplificador.Monomio monomio(Nodo g) {
        Simplificador.Monomio m = new Simplificador.Monomio();
        Simplificador.aplanar(g, Nodo.num(1), m);
        return m;
    }

    static void aplanarSuma(Nodo f, int signo, List<Nodo> terminos, List<Integer> signos) {
        if (f instanceof NodoSuma) {
            aplanarSuma(((NodoSuma) f).getIzquierdo(), signo, terminos, signos);
            aplanarSuma(((NodoSuma) f).getDerecho(), signo, terminos, signos);
        } else if (f instanceof NodoResta) {
            aplanarSuma(((NodoResta) f).getIzquierdo(), signo, terminos, signos);
            aplanarSuma(((NodoResta) f).getDerecho(), -signo, terminos, signos);
        } else if (f instanceof NodoNegativo) {
            aplanarSuma(((NodoNegativo) f).getArgumento(), -signo, terminos, signos);
        } else {
            terminos.add(f);
            signos.add(Integer.valueOf(signo));
        }
    }

    static Nodo s(Nodo n) {
        return Simplificador.simplificar(n);
    }
}
