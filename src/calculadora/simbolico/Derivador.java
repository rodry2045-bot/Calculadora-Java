package calculadora.simbolico;

import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ReglaCalculo;
import calculadora.modelo.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Derivación simbólica sobre el árbol de expresión, registrando cada regla
 * aplicada (constante, identidad, múltiplo constante, suma y resta, potencia,
 * producto, cociente, cadena, exponenciales, logarítmicas, trigonométricas,
 * inversas y derivación logarítmica).
 * <p>
 * El algoritmo es recursivo: para derivar un nodo se elige la regla según
 * su tipo y se derivan sus hijos (que quedan como pasos "anidados").
 * </p>
 */
public class Derivador {

    private final RegistroPasos registro;
    private int profundidadBase;

    /**
     * @param registro bitácora donde se guardan los pasos
     */
    public Derivador(RegistroPasos registro) {
        this.registro = registro;
    }

    /** Crea un derivador silencioso (sin pasos). */
    public Derivador() {
        this(new RegistroPasos(false));
    }

    /**
     * Deriva f respecto a v.
     *
     * @param f expresión
     * @param v variable de derivación (la otra variable se trata como constante)
     * @return derivada simplificada
     */
    public Nodo derivar(Nodo f, Variable v) {
        return s(d(f, v, profundidadBase));
    }

    /**
     * @param profundidad nivel de anidamiento del primer paso (para incluir la
     *                    derivación dentro de otro procedimiento)
     */
    public void setProfundidadBase(int profundidad) {
        this.profundidadBase = profundidad;
    }

    /** @return bitácora usada */
    public RegistroPasos getRegistro() {
        return registro;
    }

    private Nodo d(Nodo f, Variable v, int prof) {
        String x = v.getSimbolo();
        String operador = "d/d" + x + "[" + f + "]";

        // ---------------- Constante o expresión que no depende de v ----------------
        if (!f.dependeDe(v)) {
            if (f.esConstante()) {
                if (prof - profundidadBase <= 1) {
                    PasoDetallado p = registro.agregar(ReglaCalculo.CONSTANTE, ReglaCalculo.CONSTANTE.getNombre(), prof,
                            operador, "La derivada de una constante es 0");
                    p.setResultado("0");
                } else {
                    registro.marcar(ReglaCalculo.CONSTANTE);
                }
            } else if (prof - profundidadBase <= 2) {
                PasoDetallado p = registro.agregar(ReglaCalculo.VARIABLE_CONSTANTE,
                        ReglaCalculo.VARIABLE_CONSTANTE.getNombre(), prof, operador,
                        "La expresión no contiene " + x + "; al derivar respecto a " + x
                                + " se trata como una constante");
                p.setResultado("0");
            }
            return Nodo.num(0);
        }

        // ---------------- Variable ----------------
        if (f instanceof NodoVariable) {
            if (prof - profundidadBase <= 1) {
                PasoDetallado p = registro.agregar(ReglaCalculo.IDENTIDAD, ReglaCalculo.IDENTIDAD.getNombre(), prof,
                        operador, "d/d" + x + "[" + x + "] = 1");
                p.setResultado("1");
            } else {
                registro.marcar(ReglaCalculo.IDENTIDAD);
            }
            return Nodo.num(1);
        }

        // ---------------- Suma y resta ----------------
        if (f instanceof NodoSuma || f instanceof NodoResta) {
            List<Nodo> terminos = new ArrayList<Nodo>();
            List<Integer> signos = new ArrayList<Integer>();
            aplanarSuma(f, 1, terminos, signos);
            StringBuilder formula = new StringBuilder();
            for (int i = 0; i < terminos.size(); i++) {
                if (i > 0) {
                    formula.append(signos.get(i).intValue() < 0 ? " - " : " + ");
                } else if (signos.get(i).intValue() < 0) {
                    formula.append("-");
                }
                formula.append("d/d").append(x).append("[").append(terminos.get(i)).append("]");
            }
            PasoDetallado paso = registro.agregar(ReglaCalculo.SUMA_RESTA, ReglaCalculo.SUMA_RESTA.getNombre(), prof,
                    operador, "Se deriva cada término por separado y se conservan los signos: " + formula);
            Nodo total = Nodo.num(0);
            for (int i = 0; i < terminos.size(); i++) {
                Nodo derivada = d(terminos.get(i), v, prof + 1);
                if (signos.get(i).intValue() < 0) {
                    total = Nodo.resta(total, derivada);
                } else {
                    total = Nodo.suma(total, derivada);
                }
            }
            Nodo resultado = s(total);
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // ---------------- Negación ----------------
        if (f instanceof NodoNegativo) {
            Nodo interior = ((NodoNegativo) f).getArgumento();
            PasoDetallado paso = registro.agregar(ReglaCalculo.MULTIPLO_CONSTANTE,
                    ReglaCalculo.MULTIPLO_CONSTANTE.getNombre(), prof, operador,
                    "d/d" + x + "[-f] = -f'  con f = " + interior);
            Nodo resultado = s(Nodo.neg(d(interior, v, prof + 1)));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // ---------------- Producto ----------------
        if (f instanceof NodoProducto) {
            List<Nodo> factores = new ArrayList<Nodo>();
            aplanarProducto(f, factores);
            Nodo constante = null;
            Nodo variable = null;
            for (int i = 0; i < factores.size(); i++) {
                Nodo factor = factores.get(i);
                if (factor.dependeDe(v)) {
                    variable = (variable == null) ? factor : Nodo.mul(variable, factor);
                } else {
                    constante = (constante == null) ? factor : Nodo.mul(constante, factor);
                }
            }
            if (constante != null) {
                Nodo c = s(constante);
                PasoDetallado paso = registro.agregar(ReglaCalculo.MULTIPLO_CONSTANTE,
                        ReglaCalculo.MULTIPLO_CONSTANTE.getNombre(), prof, operador,
                        "d/d" + x + "[c·f] = c·f'  con c = " + c + " y f = " + variable);
                Nodo resultado = s(Nodo.mul(c, d(variable, v, prof + 1)));
                paso.setResultado(resultado.toString());
                return resultado;
            }
            Nodo primero = factores.get(0);
            Nodo resto = factores.get(1);
            for (int i = 2; i < factores.size(); i++) {
                resto = Nodo.mul(resto, factores.get(i));
            }
            PasoDetallado paso = registro.agregar(ReglaCalculo.PRODUCTO, ReglaCalculo.PRODUCTO.getNombre(), prof,
                    operador, "(f·g)' = f'·g + f·g'  con f = " + primero + " y g = " + s(resto));
            Nodo df = d(primero, v, prof + 1);
            Nodo dg = d(resto, v, prof + 1);
            Nodo resultado = s(Nodo.suma(Nodo.mul(df, resto), Nodo.mul(primero, dg)));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // ---------------- Cociente ----------------
        if (f instanceof NodoCociente) {
            Nodo num = ((NodoCociente) f).getNumerador();
            Nodo den = ((NodoCociente) f).getDenominador();
            // c/xⁿ se reescribe como c·x^(-n) y se usa la regla de la potencia
            boolean denominadorEsPotenciaDeV = (den instanceof NodoVariable && ((NodoVariable) den).getVariable() == v)
                    || (den instanceof NodoPotencia && ((NodoPotencia) den).getBase() instanceof NodoVariable
                    && ((NodoVariable) ((NodoPotencia) den).getBase()).getVariable() == v
                    && !((NodoPotencia) den).getExponente().dependeDe(v));
            if (!num.dependeDe(v) && denominadorEsPotenciaDeV) {
                Nodo n = (den instanceof NodoPotencia) ? ((NodoPotencia) den).getExponente() : Nodo.num(1);
                Nodo menosN = s(Nodo.neg(n));
                PasoDetallado paso = registro.agregar(ReglaCalculo.POTENCIA, ReglaCalculo.POTENCIA.getNombre(), prof,
                        operador, "Se reescribe " + f + " = " + s(Nodo.mul(num, Nodo.pot(Nodo.var(v), menosN)))
                                + " y se aplica d/d" + x + "[" + x + "^n] = n·" + x + "^(n-1)  con n = " + menosN);
                Nodo resultado = s(Nodo.mul(Nodo.mul(num, menosN), Nodo.pot(Nodo.var(v), s(Nodo.resta(menosN, Nodo.num(1))))));
                paso.setResultado(resultado.toString());
                return resultado;
            }
            if (!den.dependeDe(v)) {
                PasoDetallado paso = registro.agregar(ReglaCalculo.MULTIPLO_CONSTANTE,
                        ReglaCalculo.MULTIPLO_CONSTANTE.getNombre(), prof, operador,
                        "d/d" + x + "[f/c] = f'/c  con c = " + den);
                Nodo resultado = s(Nodo.div(d(num, v, prof + 1), den));
                paso.setResultado(resultado.toString());
                return resultado;
            }
            PasoDetallado paso = registro.agregar(ReglaCalculo.COCIENTE, ReglaCalculo.COCIENTE.getNombre(), prof,
                    operador, "(f/g)' = (f'·g - f·g')/g²  con f = " + num + " y g = " + den);
            Nodo dn = d(num, v, prof + 1);
            Nodo dd = d(den, v, prof + 1);
            Nodo resultado = s(Nodo.div(Nodo.resta(Nodo.mul(dn, den), Nodo.mul(num, dd)), Nodo.pot(den, 2)));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // ---------------- Potencia ----------------
        if (f instanceof NodoPotencia) {
            Nodo base = ((NodoPotencia) f).getBase();
            Nodo exponente = ((NodoPotencia) f).getExponente();
            boolean exponenteConstante = !exponente.dependeDe(v);
            boolean baseConstante = !base.dependeDe(v);

            if (exponenteConstante) {
                Nodo nMenos1 = s(Nodo.resta(exponente, Nodo.num(1)));
                if (base instanceof NodoVariable) {
                    PasoDetallado paso = registro.agregar(ReglaCalculo.POTENCIA, ReglaCalculo.POTENCIA.getNombre(),
                            prof, operador, "d/d" + x + "[" + x + "^n] = n·" + x + "^(n-1)  con n = " + exponente
                                    + ": se multiplica por el exponente y se le resta 1");
                    Nodo resultado = s(Nodo.mul(exponente, Nodo.pot(base, nMenos1)));
                    paso.setResultado(resultado.toString());
                    return resultado;
                }
                registro.marcar(ReglaCalculo.CADENA);
                PasoDetallado paso = registro.agregar(ReglaCalculo.POTENCIA,
                        ReglaCalculo.POTENCIA.getNombre() + " + regla de la cadena", prof, operador,
                        "d/d" + x + "[u^n] = n·u^(n-1)·u'  con u = " + base + " y n = " + exponente);
                Nodo du = d(base, v, prof + 1);
                Nodo resultado = s(Nodo.mul(Nodo.mul(exponente, Nodo.pot(base, nMenos1)), du));
                paso.setResultado(resultado.toString());
                return resultado;
            }
            if (baseConstante) {
                boolean cadena = !(exponente instanceof NodoVariable);
                if (cadena) {
                    registro.marcar(ReglaCalculo.CADENA);
                }
                PasoDetallado paso = registro.agregar(ReglaCalculo.EXPONENCIAL,
                        ReglaCalculo.EXPONENCIAL.getNombre() + (cadena ? " + regla de la cadena" : ""), prof, operador,
                        "d/d" + x + "[a^u] = a^u·ln(a)·u'  con a = " + base + " y u = " + exponente);
                Nodo du = d(exponente, v, prof + 1);
                Nodo resultado = s(Nodo.mul(Nodo.mul(f, Nodo.fn(TipoFuncion.LN, base)), du));
                paso.setResultado(resultado.toString());
                return resultado;
            }
            PasoDetallado paso = registro.agregar(ReglaCalculo.DERIVACION_LOGARITMICA,
                    ReglaCalculo.DERIVACION_LOGARITMICA.getNombre(), prof, operador,
                    "y = f^g  ⇒  ln(y) = g·ln(f)  ⇒  y' = f^g·(g'·ln(f) + g·f'/f)  con f = " + base + " y g = "
                            + exponente);
            Nodo db = d(base, v, prof + 1);
            Nodo de = d(exponente, v, prof + 1);
            Nodo resultado = s(Nodo.mul(f, Nodo.suma(Nodo.mul(de, Nodo.fn(TipoFuncion.LN, base)),
                    Nodo.div(Nodo.mul(exponente, db), base))));
            paso.setResultado(resultado.toString());
            return resultado;
        }

        // ---------------- Funciones ----------------
        if (f instanceof NodoFuncion) {
            NodoFuncion funcion = (NodoFuncion) f;
            TipoFuncion tipo = funcion.getTipo();
            Nodo u = funcion.getArgumento();

            // ln|w|: su derivada es w'/w
            if (tipo == TipoFuncion.LN && u instanceof NodoFuncion && ((NodoFuncion) u).getTipo() == TipoFuncion.ABS) {
                Nodo w = ((NodoFuncion) u).getArgumento();
                boolean cadena = !(w instanceof NodoVariable);
                if (cadena) {
                    registro.marcar(ReglaCalculo.CADENA);
                }
                PasoDetallado paso = registro.agregar(ReglaCalculo.LOGARITMICA,
                        ReglaCalculo.LOGARITMICA.getNombre() + (cadena ? " + regla de la cadena" : ""), prof, operador,
                        "d/d" + x + "[ln|u|] = u'/u  con u = " + w);
                Nodo dw = d(w, v, prof + 1);
                Nodo resultado = s(Nodo.div(dw, w));
                paso.setResultado(resultado.toString());
                return resultado;
            }

            Nodo exterior = derivadaExterior(tipo, u);
            ReglaCalculo regla = reglaDe(tipo);
            boolean cadena = !(u instanceof NodoVariable);
            String titulo = regla == ReglaCalculo.INFORMACION ? "Derivada del valor absoluto" : regla.getNombre();
            if (cadena) {
                registro.marcar(ReglaCalculo.CADENA);
                titulo = titulo + " + regla de la cadena";
            }
            String formula = formulaDe(tipo) + (cadena ? "·u'" : "");
            PasoDetallado paso = registro.agregar(regla, titulo, prof, operador, formula + "  con u = " + u);
            Nodo resultado;
            if (cadena) {
                Nodo du = d(u, v, prof + 1);
                resultado = s(Nodo.mul(exterior, du));
            } else {
                resultado = s(exterior);
            }
            paso.setResultado(resultado.toString());
            return resultado;
        }

        throw new IllegalStateException("Nodo no reconocido: " + f);
    }

    /** Derivada de la función exterior evaluada en u: f'(u). */
    private static Nodo derivadaExterior(TipoFuncion tipo, Nodo u) {
        switch (tipo) {
            case SIN:
                return Nodo.fn(TipoFuncion.COS, u);
            case COS:
                return Nodo.neg(Nodo.fn(TipoFuncion.SIN, u));
            case TAN:
                return Nodo.pot(Nodo.fn(TipoFuncion.SEC, u), 2);
            case SEC:
                return Nodo.mul(Nodo.fn(TipoFuncion.SEC, u), Nodo.fn(TipoFuncion.TAN, u));
            case CSC:
                return Nodo.neg(Nodo.mul(Nodo.fn(TipoFuncion.CSC, u), Nodo.fn(TipoFuncion.COT, u)));
            case COT:
                return Nodo.neg(Nodo.pot(Nodo.fn(TipoFuncion.CSC, u), 2));
            case EXP:
                return Nodo.fn(TipoFuncion.EXP, u);
            case LN:
                return Nodo.div(Nodo.num(1), u);
            case LOG:
                return Nodo.div(Nodo.num(1), Nodo.mul(u, Nodo.fn(TipoFuncion.LN, Nodo.num(10))));
            case RAIZ:
                return Nodo.div(Nodo.num(1), Nodo.mul(Nodo.num(2), Nodo.fn(TipoFuncion.RAIZ, u)));
            case ARCSIN:
                return Nodo.div(Nodo.num(1), Nodo.fn(TipoFuncion.RAIZ, Nodo.resta(Nodo.num(1), Nodo.pot(u, 2))));
            case ARCCOS:
                return Nodo.neg(Nodo.div(Nodo.num(1),
                        Nodo.fn(TipoFuncion.RAIZ, Nodo.resta(Nodo.num(1), Nodo.pot(u, 2)))));
            case ARCTAN:
                return Nodo.div(Nodo.num(1), Nodo.suma(Nodo.num(1), Nodo.pot(u, 2)));
            default:
                return Nodo.div(u, Nodo.fn(TipoFuncion.ABS, u));
        }
    }

    private static ReglaCalculo reglaDe(TipoFuncion tipo) {
        if (tipo.esTrigonometrica()) {
            return ReglaCalculo.TRIGONOMETRICA;
        }
        if (tipo.esInversa()) {
            return ReglaCalculo.INVERSA;
        }
        if (tipo == TipoFuncion.EXP) {
            return ReglaCalculo.EXPONENCIAL;
        }
        if (tipo == TipoFuncion.LN || tipo == TipoFuncion.LOG) {
            return ReglaCalculo.LOGARITMICA;
        }
        if (tipo == TipoFuncion.RAIZ) {
            return ReglaCalculo.POTENCIA;
        }
        return ReglaCalculo.INFORMACION;
    }

    private static String formulaDe(TipoFuncion tipo) {
        switch (tipo) {
            case SIN:
                return "d/du[sin(u)] = cos(u)";
            case COS:
                return "d/du[cos(u)] = -sin(u)";
            case TAN:
                return "d/du[tan(u)] = sec(u)²";
            case SEC:
                return "d/du[sec(u)] = sec(u)·tan(u)";
            case CSC:
                return "d/du[csc(u)] = -csc(u)·cot(u)";
            case COT:
                return "d/du[cot(u)] = -csc(u)²";
            case EXP:
                return "d/du[e^u] = e^u";
            case LN:
                return "d/du[ln(u)] = 1/u";
            case LOG:
                return "d/du[log(u)] = 1/(u·ln(10))";
            case RAIZ:
                return "d/du[√(u)] = d/du[u^(1/2)] = 1/(2√(u))";
            case ARCSIN:
                return "d/du[arcsin(u)] = 1/√(1 - u²)";
            case ARCCOS:
                return "d/du[arccos(u)] = -1/√(1 - u²)";
            case ARCTAN:
                return "d/du[arctan(u)] = 1/(1 + u²)";
            default:
                return "d/du[|u|] = u/|u|";
        }
    }

    private static void aplanarSuma(Nodo f, int signo, List<Nodo> terminos, List<Integer> signos) {
        if (f instanceof NodoSuma) {
            aplanarSuma(((NodoSuma) f).getIzquierdo(), signo, terminos, signos);
            aplanarSuma(((NodoSuma) f).getDerecho(), signo, terminos, signos);
        } else if (f instanceof NodoResta) {
            aplanarSuma(((NodoResta) f).getIzquierdo(), signo, terminos, signos);
            aplanarSuma(((NodoResta) f).getDerecho(), -signo, terminos, signos);
        } else {
            terminos.add(f);
            signos.add(Integer.valueOf(signo));
        }
    }

    private static void aplanarProducto(Nodo f, List<Nodo> factores) {
        if (f instanceof NodoProducto) {
            aplanarProducto(((NodoProducto) f).getIzquierdo(), factores);
            aplanarProducto(((NodoProducto) f).getDerecho(), factores);
        } else {
            factores.add(f);
        }
    }

    private static Nodo s(Nodo n) {
        return Simplificador.simplificar(n);
    }
}
