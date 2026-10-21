package calculadora.simbolico;

import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplificador algebraico del árbol de expresión.
 * <p>
 * Estrategia (forma canónica):
 * </p>
 * <ol>
 *   <li><b>Productos y cocientes</b> se "aplanan" en un coeficiente numérico
 *       y una lista de factores base^exponente. Así se cancelan factores
 *       (x/x = 1), se suman exponentes (x·x² = x³) y se juntan exponenciales
 *       (e^x·e^(2x) = e^(3x)).</li>
 *   <li><b>Sumas y restas</b> se aplanan en una lista de términos; los términos
 *       semejantes se combinan (3x + 2x = 5x) y se ordenan (grado descendente).</li>
 *   <li><b>Funciones</b> con argumento constante se evalúan cuando el valor es
 *       exacto (sin(0) = 0, ln(1) = 0, arctan(1) = π/4).</li>
 * </ol>
 */
public final class Simplificador {

    /** Constructor privado: clase utilitaria. */
    private Simplificador() {
    }

    /**
     * Simplifica una expresión hasta que deja de cambiar (máximo 5 pasadas).
     *
     * @param nodo expresión
     * @return expresión simplificada
     */
    public static Nodo simplificar(Nodo nodo) {
        Nodo actual = nodo;
        for (int i = 0; i < 5; i++) {
            Nodo siguiente = paso(actual);
            if (siguiente.clave().equals(actual.clave())) {
                return siguiente;
            }
            actual = siguiente;
        }
        return actual;
    }

    private static Nodo paso(Nodo n) {
        if (n instanceof NodoConstante || n instanceof NodoVariable) {
            return n;
        }
        if (n instanceof NodoFuncion) {
            NodoFuncion f = (NodoFuncion) n;
            return simplificarFuncion(f.getTipo(), paso(f.getArgumento()));
        }
        if (n instanceof NodoSuma || n instanceof NodoResta || n instanceof NodoNegativo) {
            return simplificarSuma(n);
        }
        return simplificarProducto(n);
    }

    // ==================================================================
    // Productos, cocientes y potencias
    // ==================================================================

    /** Coeficiente numérico + factores base^exponente + suma de exponentes de e. */
    static final class Monomio {
        double coeficiente = 1.0;
        final Map<String, Nodo[]> factores = new LinkedHashMap<String, Nodo[]>();
        Nodo argumentoExp = null;

        Monomio copiaSinCoeficiente() {
            Monomio copia = new Monomio();
            for (Map.Entry<String, Nodo[]> entrada : factores.entrySet()) {
                copia.factores.put(entrada.getKey(), new Nodo[]{entrada.getValue()[0], entrada.getValue()[1]});
            }
            copia.argumentoExp = argumentoExp;
            return copia;
        }

        boolean esConstantePura() {
            return factores.isEmpty() && argumentoExp == null;
        }
    }

    private static Nodo simplificarProducto(Nodo n) {
        Monomio m = new Monomio();
        aplanar(hijosSimplificados(n), Nodo.num(1), m);
        return construir(m);
    }

    /** Simplifica los hijos directos antes de aplanar. */
    private static Nodo hijosSimplificados(Nodo n) {
        if (n instanceof NodoProducto) {
            NodoProducto p = (NodoProducto) n;
            return Nodo.mul(paso(p.getIzquierdo()), paso(p.getDerecho()));
        }
        if (n instanceof NodoCociente) {
            NodoCociente c = (NodoCociente) n;
            return Nodo.div(paso(c.getIzquierdo()), paso(c.getDerecho()));
        }
        if (n instanceof NodoPotencia) {
            NodoPotencia p = (NodoPotencia) n;
            return Nodo.pot(paso(p.getBase()), paso(p.getExponente()));
        }
        return n;
    }

    /**
     * Descompone un nodo en factores, multiplicando los exponentes por e.
     */
    static void aplanar(Nodo n, Nodo e, Monomio m) {
        if (n instanceof NodoProducto) {
            aplanar(((NodoProducto) n).getIzquierdo(), e, m);
            aplanar(((NodoProducto) n).getDerecho(), e, m);
            return;
        }
        if (n instanceof NodoCociente) {
            aplanar(((NodoCociente) n).getIzquierdo(), e, m);
            aplanar(((NodoCociente) n).getDerecho(), negar(e), m);
            return;
        }
        if (n instanceof NodoNegativo && e.esNumero() && FormatoNumeros.esEntero(valor(e))) {
            if (Math.round(valor(e)) % 2 != 0) {
                m.coeficiente = -m.coeficiente;
            }
            aplanar(((NodoNegativo) n).getArgumento(), e, m);
            return;
        }
        if (n.esNumero()) {
            double base = valor(n);
            if (e.esNumero()) {
                double exponente = valor(e);
                double resultado = Math.pow(base, exponente);
                boolean exacto = FormatoNumeros.esEntero(exponente) || esExacto(resultado);
                if (base == 0.0 && exponente > 0) {
                    m.coeficiente = 0.0;
                    return;
                }
                if (exacto && !Double.isNaN(resultado) && !Double.isInfinite(resultado)) {
                    m.coeficiente = m.coeficiente * resultado;
                    return;
                }
            }
            agregarFactor(m, n, e);
            return;
        }
        if (n instanceof NodoConstante && "e".equals(((NodoConstante) n).getSimbolo())) {
            agregarExp(m, e);
            return;
        }
        if (n instanceof NodoPotencia) {
            NodoPotencia p = (NodoPotencia) n;
            Nodo base = p.getBase();
            Nodo nuevoExponente = multiplicar(e, p.getExponente());
            if (base instanceof NodoConstante && "e".equals(((NodoConstante) base).getSimbolo())) {
                agregarExp(m, nuevoExponente);
                return;
            }
            boolean baseCompuesta = base instanceof NodoProducto || base instanceof NodoCociente
                    || base instanceof NodoNegativo || base instanceof NodoPotencia || base.esNumero()
                    || (base instanceof NodoFuncion && ((NodoFuncion) base).getTipo() == TipoFuncion.EXP)
                    || (base instanceof NodoFuncion && ((NodoFuncion) base).getTipo() == TipoFuncion.RAIZ);
            if (baseCompuesta && nuevoExponente.esNumero()) {
                aplanar(base, nuevoExponente, m);
                return;
            }
            agregarFactor(m, base, nuevoExponente);
            return;
        }
        if (n instanceof NodoFuncion) {
            NodoFuncion f = (NodoFuncion) n;
            if (f.getTipo() == TipoFuncion.RAIZ) {
                aplanar(f.getArgumento(), multiplicar(e, Nodo.num(0.5)), m);
                return;
            }
            if (f.getTipo() == TipoFuncion.EXP) {
                agregarExp(m, multiplicar(e, f.getArgumento()));
                return;
            }
        }
        agregarFactor(m, n, e);
    }

    private static void agregarFactor(Monomio m, Nodo base, Nodo e) {
        // Una suma con coeficientes enteros con factor común se reduce: (2x + 2) = 2·(x + 1)
        if ((base instanceof NodoSuma || base instanceof NodoResta) && e.esNumero()
                && FormatoNumeros.esEntero(valor(e))) {
            long comun = factorComun(base);
            if (comun > 1) {
                m.coeficiente = m.coeficiente * Math.pow(comun, valor(e));
                base = simplificarSuma(base, 1.0 / comun);
            }
        }
        String clave = base.clave();
        Nodo[] existente = m.factores.get(clave);
        if (existente == null) {
            m.factores.put(clave, new Nodo[]{base, e});
        } else {
            existente[1] = sumar(existente[1], e);
        }
    }

    private static void agregarExp(Monomio m, Nodo argumento) {
        if (m.argumentoExp == null) {
            m.argumentoExp = argumento;
        } else {
            m.argumentoExp = simplificar(Nodo.suma(m.argumentoExp, argumento));
        }
    }

    /**
     * Reconstruye un árbol a partir de un monomio aplanado.
     */
    static Nodo construir(Monomio m) {
        if (m.coeficiente == 0.0) {
            return Nodo.num(0);
        }
        List<Nodo[]> numerador = new ArrayList<Nodo[]>();
        List<Nodo[]> denominador = new ArrayList<Nodo[]>();
        for (Nodo[] factor : m.factores.values()) {
            Nodo exponente = simplificar(factor[1]);
            if (exponente.esNumero(0.0)) {
                continue;
            }
            if (exponente.esNumero() && valor(exponente) < 0) {
                denominador.add(new Nodo[]{factor[0], Nodo.num(-valor(exponente))});
            } else {
                numerador.add(new Nodo[]{factor[0], exponente});
            }
        }
        ordenar(numerador);
        ordenar(denominador);

        List<Nodo> nodosNumerador = new ArrayList<Nodo>();
        for (int i = 0; i < numerador.size(); i++) {
            nodosNumerador.add(crearFactor(numerador.get(i)[0], numerador.get(i)[1]));
        }
        if (m.argumentoExp != null) {
            Nodo argumento = simplificar(m.argumentoExp);
            if (!argumento.esNumero(0.0)) {
                if (argumento.esNumero(1.0)) {
                    nodosNumerador.add(NodoConstante.E);
                } else {
                    nodosNumerador.add(Nodo.fn(TipoFuncion.EXP, argumento));
                }
            }
        }
        List<Nodo> nodosDenominador = new ArrayList<Nodo>();
        for (int i = 0; i < denominador.size(); i++) {
            nodosDenominador.add(crearFactor(denominador.get(i)[0], denominador.get(i)[1]));
        }

        double coefNumerador = m.coeficiente;
        double coefDenominador = 1.0;
        // Un coeficiente fraccionario p/q se escribe como (p·...)/q: x^3/3, e^(2x)/2
        if (!FormatoNumeros.esEntero(m.coeficiente) && !(nodosNumerador.isEmpty() && nodosDenominador.isEmpty())) {
            long[] fraccion = FormatoNumeros.comoFraccion(m.coeficiente);
            if (fraccion != null) {
                coefNumerador = fraccion[0];
                coefDenominador = fraccion[1];
            }
        }

        Nodo arriba = multiplicarLista(coefNumerador, nodosNumerador);
        if (nodosDenominador.isEmpty()) {
            if (!FormatoNumeros.sonIguales(coefDenominador, 1.0)) {
                return Nodo.div(arriba, Nodo.num(coefDenominador));
            }
            return arriba;
        }
        Nodo abajo = multiplicarLista(coefDenominador, nodosDenominador);
        return Nodo.div(arriba, abajo);
    }

    private static Nodo crearFactor(Nodo base, Nodo exponente) {
        if (exponente.esNumero(1.0)) {
            return base;
        }
        if (exponente.esNumero(0.5)) {
            return Nodo.fn(TipoFuncion.RAIZ, base);
        }
        return Nodo.pot(base, exponente);
    }

    private static Nodo multiplicarLista(double coeficiente, List<Nodo> factores) {
        Nodo resultado = null;
        if (factores.isEmpty() || !FormatoNumeros.sonIguales(coeficiente, 1.0)) {
            resultado = Nodo.num(coeficiente);
        }
        for (int i = 0; i < factores.size(); i++) {
            if (resultado == null) {
                resultado = factores.get(i);
            } else {
                resultado = Nodo.mul(resultado, factores.get(i));
            }
        }
        return resultado;
    }

    /** Orden canónico de los factores: constantes, x, y, sumas y funciones. */
    private static void ordenar(List<Nodo[]> lista) {
        for (int i = 1; i < lista.size(); i++) {
            Nodo[] actual = lista.get(i);
            int j = i - 1;
            while (j >= 0 && comparar(lista.get(j)[0], actual[0]) > 0) {
                lista.set(j + 1, lista.get(j));
                j--;
            }
            lista.set(j + 1, actual);
        }
    }

    private static int comparar(Nodo a, Nodo b) {
        int ca = categoria(a);
        int cb = categoria(b);
        if (ca != cb) {
            return ca - cb;
        }
        return a.clave().compareTo(b.clave());
    }

    private static int categoria(Nodo n) {
        if (n instanceof NodoConstante) {
            return 0;
        }
        if (n instanceof NodoVariable) {
            return 1 + ((NodoVariable) n).getVariable().ordinal();
        }
        if (n instanceof NodoSuma || n instanceof NodoResta) {
            return 10;
        }
        if (n instanceof NodoFuncion) {
            return 20 + ((NodoFuncion) n).getTipo().ordinal();
        }
        return 50;
    }

    /** Máximo común divisor de los coeficientes enteros de una suma (1 si no aplica). */
    private static long factorComun(Nodo suma) {
        List<Nodo> terminos = new ArrayList<Nodo>();
        recolectarTerminos(suma, terminos);
        long comun = 0;
        for (int i = 0; i < terminos.size(); i++) {
            Monomio m = new Monomio();
            aplanar(terminos.get(i), Nodo.num(1), m);
            double c = Math.abs(m.coeficiente);
            if (!FormatoNumeros.esEntero(c) || c > 1e9) {
                return 1;
            }
            long entero = Math.round(c);
            comun = (comun == 0) ? entero : mcd(comun, entero);
        }
        return comun == 0 ? 1 : comun;
    }

    private static void recolectarTerminos(Nodo n, List<Nodo> terminos) {
        if (n instanceof NodoSuma || n instanceof NodoResta) {
            recolectarTerminos(((NodoBinario) n).getIzquierdo(), terminos);
            recolectarTerminos(((NodoBinario) n).getDerecho(), terminos);
        } else if (n instanceof NodoNegativo) {
            recolectarTerminos(((NodoNegativo) n).getArgumento(), terminos);
        } else {
            terminos.add(n);
        }
    }

    private static long mcd(long a, long b) {
        while (b != 0) {
            long t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    // ==================================================================
    // Sumas y restas
    // ==================================================================

    private static Nodo simplificarSuma(Nodo n) {
        return simplificarSuma(n, 1.0);
    }

    private static Nodo simplificarSuma(Nodo n, double multiplicador) {
        List<double[]> coeficientes = new ArrayList<double[]>();
        List<Monomio> monomios = new ArrayList<Monomio>();
        List<String> claves = new ArrayList<String>();
        double[] constante = {0.0};
        aplanarSuma(n, multiplicador, coeficientes, monomios, claves, constante);

        // Ordenar: polinómicos por grado descendente, luego funciones por tipo
        List<Integer> indices = new ArrayList<Integer>();
        for (int i = 0; i < monomios.size(); i++) {
            if (!FormatoNumeros.sonIguales(coeficientes.get(i)[0], 0.0)) {
                indices.add(Integer.valueOf(i));
            }
        }
        for (int i = 1; i < indices.size(); i++) {
            Integer actual = indices.get(i);
            int j = i - 1;
            while (j >= 0 && compararTerminos(monomios.get(indices.get(j).intValue()), claves.get(indices.get(j).intValue()),
                    monomios.get(actual.intValue()), claves.get(actual.intValue())) > 0) {
                indices.set(j + 1, indices.get(j));
                j--;
            }
            indices.set(j + 1, actual);
        }

        // Presentación: si el primer término es negativo, se adelanta el primer
        // término positivo (4 - x^2 en lugar de -x^2 + 4)
        if (!indices.isEmpty() && coeficientes.get(indices.get(0).intValue())[0] < 0) {
            int posicionPositivo = -1;
            for (int k = 1; k < indices.size(); k++) {
                if (coeficientes.get(indices.get(k).intValue())[0] > 0) {
                    posicionPositivo = k;
                    break;
                }
            }
            if (posicionPositivo > 0) {
                Integer positivo = indices.remove(posicionPositivo);
                indices.add(0, positivo);
            } else if (constante[0] > 0) {
                Nodo restoNegativo = null;
                for (int k = 0; k < indices.size(); k++) {
                    int i = indices.get(k).intValue();
                    Monomio m = monomios.get(i).copiaSinCoeficiente();
                    m.coeficiente = -coeficientes.get(i)[0];
                    Nodo t = construir(m);
                    restoNegativo = (restoNegativo == null) ? Nodo.resta(Nodo.num(constante[0]), t) : Nodo.resta(restoNegativo, t);
                }
                return restoNegativo;
            }
        }

        Nodo resultado = null;
        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k).intValue();
            double c = coeficientes.get(i)[0];
            Monomio m = monomios.get(i).copiaSinCoeficiente();
            if (resultado == null) {
                m.coeficiente = c;
                resultado = construir(m);
            } else if (c < 0) {
                m.coeficiente = -c;
                resultado = Nodo.resta(resultado, construir(m));
            } else {
                m.coeficiente = c;
                resultado = Nodo.suma(resultado, construir(m));
            }
        }
        double cte = constante[0];
        if (resultado == null) {
            return Nodo.num(cte);
        }
        if (FormatoNumeros.sonIguales(cte, 0.0)) {
            return resultado;
        }
        if (cte < 0) {
            return Nodo.resta(resultado, Nodo.num(-cte));
        }
        return Nodo.suma(resultado, Nodo.num(cte));
    }

    private static void aplanarSuma(Nodo n, double multiplicador, List<double[]> coeficientes, List<Monomio> monomios,
                                    List<String> claves, double[] constante) {
        if (n instanceof NodoSuma) {
            aplanarSuma(((NodoSuma) n).getIzquierdo(), multiplicador, coeficientes, monomios, claves, constante);
            aplanarSuma(((NodoSuma) n).getDerecho(), multiplicador, coeficientes, monomios, claves, constante);
            return;
        }
        if (n instanceof NodoResta) {
            aplanarSuma(((NodoResta) n).getIzquierdo(), multiplicador, coeficientes, monomios, claves, constante);
            aplanarSuma(((NodoResta) n).getDerecho(), -multiplicador, coeficientes, monomios, claves, constante);
            return;
        }
        if (n instanceof NodoNegativo) {
            aplanarSuma(((NodoNegativo) n).getArgumento(), -multiplicador, coeficientes, monomios, claves, constante);
            return;
        }
        Nodo termino = paso(n);
        Monomio m = new Monomio();
        aplanar(termino, Nodo.num(1), m);
        double c = m.coeficiente * multiplicador;
        if (m.esConstantePura()) {
            constante[0] = constante[0] + c;
            return;
        }
        // c·(a + b) dentro de una suma: se distribuye el coeficiente
        if (m.factores.size() == 1 && m.argumentoExp == null) {
            Nodo[] unico = m.factores.values().iterator().next();
            boolean esSuma = unico[0] instanceof NodoSuma || unico[0] instanceof NodoResta;
            if (esSuma && simplificar(unico[1]).esNumero(1.0)) {
                aplanarSuma(unico[0], c, coeficientes, monomios, claves, constante);
                return;
            }
        }
        Monomio sinCoeficiente = m.copiaSinCoeficiente();
        String clave = construir(sinCoeficiente).clave();
        for (int i = 0; i < claves.size(); i++) {
            if (claves.get(i).equals(clave)) {
                coeficientes.get(i)[0] = coeficientes.get(i)[0] + c;
                return;
            }
        }
        claves.add(clave);
        monomios.add(sinCoeficiente);
        coeficientes.add(new double[]{c});
    }

    private static int compararTerminos(Monomio a, String claveA, Monomio b, String claveB) {
        double gradoA = gradoPolinomico(a);
        double gradoB = gradoPolinomico(b);
        boolean polA = !Double.isNaN(gradoA);
        boolean polB = !Double.isNaN(gradoB);
        if (polA && polB) {
            if (gradoA != gradoB) {
                return gradoA > gradoB ? -1 : 1;
            }
            return claveA.compareTo(claveB);
        }
        if (polA) {
            return -1;
        }
        if (polB) {
            return 1;
        }
        int ca = categoriaTermino(a);
        int cb = categoriaTermino(b);
        if (ca != cb) {
            return ca - cb;
        }
        return claveA.compareTo(claveB);
    }

    /** Grado del término si solo contiene variables con exponentes numéricos; NaN si no. */
    private static double gradoPolinomico(Monomio m) {
        if (m.argumentoExp != null) {
            return Double.NaN;
        }
        double grado = 0;
        for (Nodo[] factor : m.factores.values()) {
            if (!(factor[0] instanceof NodoVariable) || !factor[1].esNumero()) {
                return Double.NaN;
            }
            if (((NodoVariable) factor[0]).getVariable() == Variable.Y) {
                grado = grado + valor(factor[1]) * 0.001;
            } else {
                grado = grado + valor(factor[1]);
            }
        }
        return grado;
    }

    private static int categoriaTermino(Monomio m) {
        int mayor = 0;
        for (Nodo[] factor : m.factores.values()) {
            mayor = Math.max(mayor, categoria(factor[0]));
        }
        if (m.argumentoExp != null) {
            mayor = Math.max(mayor, 20 + TipoFuncion.EXP.ordinal());
        }
        return mayor;
    }

    // ==================================================================
    // Funciones
    // ==================================================================

    private static Nodo simplificarFuncion(TipoFuncion tipo, Nodo argumento) {
        if (tipo == TipoFuncion.EXP) {
            if (argumento instanceof NodoFuncion && ((NodoFuncion) argumento).getTipo() == TipoFuncion.LN) {
                return ((NodoFuncion) argumento).getArgumento();
            }
            if (argumento.esNumero(1.0)) {
                return NodoConstante.E;
            }
        }
        if (tipo == TipoFuncion.LN) {
            if (argumento instanceof NodoFuncion && ((NodoFuncion) argumento).getTipo() == TipoFuncion.EXP) {
                return ((NodoFuncion) argumento).getArgumento();
            }
            if (argumento instanceof NodoConstante && "e".equals(((NodoConstante) argumento).getSimbolo())) {
                return Nodo.num(1);
            }
        }
        if (tipo == TipoFuncion.ABS && argumento instanceof NodoFuncion) {
            TipoFuncion interior = ((NodoFuncion) argumento).getTipo();
            if (interior == TipoFuncion.ABS || interior == TipoFuncion.EXP || interior == TipoFuncion.RAIZ) {
                return argumento;
            }
        }
        if (tipo == TipoFuncion.RAIZ) {
            // √ se trata como potencia 1/2 para cancelar con otros factores
            return simplificarProducto(Nodo.pot(argumento, 0.5));
        }
        if (argumento.esConstante()) {
            double v = tipo.aplicar(argumento.evaluar(0, 0));
            if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                if (esExacto(v)) {
                    return Nodo.num(v);
                }
                double multiploPi = v / Math.PI;
                long[] fraccion = FormatoNumeros.comoFraccion(multiploPi);
                if (fraccion != null && fraccion[1] <= 12 && tipo.esInversa()) {
                    return construirMultiploPi(fraccion);
                }
            }
        }
        return Nodo.fn(tipo, argumento);
    }

    private static Nodo construirMultiploPi(long[] fraccion) {
        Nodo resultado = NodoConstante.PI;
        if (fraccion[0] != 1) {
            resultado = Nodo.mul(Nodo.num(fraccion[0]), resultado);
        }
        if (fraccion[1] != 1) {
            resultado = Nodo.div(resultado, Nodo.num(fraccion[1]));
        }
        return resultado;
    }

    // ==================================================================
    // Utilidades numéricas
    // ==================================================================

    /**
     * @param v número
     * @return true si es entero o una fracción con denominador ≤ 100
     */
    public static boolean esExacto(double v) {
        return FormatoNumeros.esEntero(v) || FormatoNumeros.comoFraccion(v) != null;
    }

    private static double valor(Nodo n) {
        return ((NodoConstante) n).getValor();
    }

    private static Nodo negar(Nodo e) {
        if (e.esNumero()) {
            return Nodo.num(-valor(e));
        }
        return simplificar(Nodo.mul(Nodo.num(-1), e));
    }

    private static Nodo sumar(Nodo a, Nodo b) {
        if (a.esNumero() && b.esNumero()) {
            return Nodo.num(valor(a) + valor(b));
        }
        return simplificar(Nodo.suma(a, b));
    }

    private static Nodo multiplicar(Nodo a, Nodo b) {
        if (a.esNumero() && b.esNumero()) {
            return Nodo.num(valor(a) * valor(b));
        }
        if (a.esNumero(1.0)) {
            return b;
        }
        if (b.esNumero(1.0)) {
            return a;
        }
        return simplificar(Nodo.mul(a, b));
    }

    /**
     * Separa un término en coeficiente numérico y parte variable.
     *
     * @param termino expresión
     * @return arreglo {coeficiente (NodoConstante), resto}
     */
    public static Nodo[] separarCoeficiente(Nodo termino) {
        Monomio m = new Monomio();
        aplanar(simplificar(termino), Nodo.num(1), m);
        double c = m.coeficiente;
        Monomio resto = m.copiaSinCoeficiente();
        return new Nodo[]{Nodo.num(c), construir(resto)};
    }
}
