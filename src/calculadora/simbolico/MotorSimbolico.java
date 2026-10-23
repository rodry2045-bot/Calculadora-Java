package calculadora.simbolico;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.DominioInvalidoException;
import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.excepciones.OperacionNoSoportadaException;
import calculadora.modelo.EntradaCalculo;
import calculadora.modelo.MetodoCalculo;
import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ReglaCalculo;
import calculadora.modelo.ResultadoCalculo;
import calculadora.modelo.TecnicaIntegracion;
import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Fachada del motor simbólico: recibe el método elegido y los datos del
 * estudiante, ejecuta la operación y devuelve un {@link ResultadoCalculo}
 * con todos los pasos y los datos para graficar.
 */
public class MotorSimbolico {

    private final AnalizadorSimbolico analizador = new AnalizadorSimbolico();

    /** Avisos de lectura de las expresiones del cálculo actual (e^2x, 1/2x...). */
    private final List<String> avisos = new ArrayList<String>();

    /** true si en la última evaluación de F(b) - F(a) algún límite no existía (oscila). */
    private boolean ultimoNoExiste;

    /**
     * Resuelve el ejercicio con el método indicado.
     *
     * @param metodo  método elegido en la lista
     * @param entrada datos escritos por el estudiante
     * @return resultado con procedimiento
     * @throws CalculadoraException si hay errores de escritura, de dominio o de soporte
     */
    public ResultadoCalculo resolver(MetodoCalculo metodo, EntradaCalculo entrada) throws CalculadoraException {
        avisos.clear();
        ResultadoCalculo resultado;
        RegistroPasos registro = new RegistroPasos(true);
        switch (metodo.getOperacion()) {
            case DERIVADA:
                resultado = derivada(entrada.getExpresion(), entrada.getVariable(), registro);
                break;
            case ORDEN_SUPERIOR:
                resultado = ordenSuperior(entrada.getExpresion(), entrada.getVariable(), entrada.getOrden(), registro);
                break;
            case IMPLICITA:
                resultado = implicita(entrada.getExpresion(), registro);
                break;
            case LIMITE:
                resultado = limite(metodo, entrada.getExpresion(), entrada.getPunto(), registro);
                break;
            case TANGENTE:
                resultado = tangente(entrada.getExpresion(), entrada.getPunto(), registro);
                break;
            case EXTREMOS:
                resultado = extremos(entrada, registro);
                break;
            case AREA_CURVAS:
                resultado = areaEntreCurvas(entrada, registro);
                break;
            case INTEGRAL:
                if (entrada.isDefinida()) {
                    resultado = integralDefinida(metodo, entrada, registro);
                } else {
                    resultado = integralIndefinida(entrada.getExpresion(), variableIntegral(entrada),
                            metodo.getTecnica(), registro);
                }
                break;
            default:
                resultado = integralDefinida(metodo, entrada, registro);
                break;
        }
        resultado.setNombreMetodo(metodo.getNombre());
        resultado.agregarPasosDetallados(registro.getPasos());
        resaltar(metodo, resultado, registro.getUsadas());
        for (int i = 0; i < avisos.size(); i++) {
            resultado.agregarNota("Lectura de la expresión: " + avisos.get(i) + ".");
        }
        return resultado;
    }

    /** Marca los pasos del método elegido y avisa si no fue necesario. */
    private void resaltar(MetodoCalculo metodo, ResultadoCalculo resultado, Set<ReglaCalculo> usadas) {
        ReglaCalculo[] reglas = metodo.getReglas();
        if (reglas.length == 0) {
            return;
        }
        boolean alguna = false;
        List<PasoDetallado> pasos = resultado.getPasosDetallados();
        for (int i = 0; i < pasos.size(); i++) {
            for (int j = 0; j < reglas.length; j++) {
                if (pasos.get(i).getRegla() == reglas[j]) {
                    pasos.get(i).setDestacado(true);
                }
            }
        }
        for (int j = 0; j < reglas.length; j++) {
            if (usadas.contains(reglas[j])) {
                alguna = true;
            }
        }
        if (!alguna) {
            resultado.agregarNota("Esta función no necesita la «" + metodo.getNombre()
                    + "». Se resolvió con las reglas que sí corresponden. Pruebe el ejemplo: " + metodo.getEjemplo());
        }
    }

    // ==================================================================
    // DERIVADAS
    // ==================================================================

    private ResultadoCalculo derivada(String texto, Variable v, RegistroPasos registro) throws CalculadoraException {
        Nodo f = analizar(texto);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.DERIVADA, texto, f, v);
        boolean parcial = f.dependeDe(v.otra());
        String simbolo = parcial ? "∂/∂" + v.getSimbolo() : "d/d" + v.getSimbolo();
        registro.agregar(parcial ? ReglaCalculo.VARIABLE_CONSTANTE : ReglaCalculo.INFORMACION, "Planteamiento", 0,
                simbolo + "[" + f + "]",
                parcial ? "Derivada parcial respecto a " + v.getSimbolo() + ": la variable " + v.otra().getSimbolo()
                        + " se trata como una constante" : "Se deriva respecto a " + v.getSimbolo());
        Derivador derivador = new Derivador(registro);
        Nodo derivada = Embellecedor.embellecer(derivador.derivar(f, v));
        resultado.setExpresionResultado(derivada);
        resultado.setTextoResultado(simbolo + "[" + f + "] = " + derivada);
        resultado.setEtiquetaSecundaria((parcial ? "∂f/∂" + v.getSimbolo() : "f'") + " = " + derivada);
        return resultado;
    }

    private ResultadoCalculo ordenSuperior(String texto, Variable v, String ordenTexto, RegistroPasos registro)
            throws CalculadoraException {
        int orden;
        try {
            orden = Integer.parseInt(ordenTexto.trim());
        } catch (NumberFormatException e) {
            throw new ExpresionInvalidaException("El orden debe ser un número entero entre 2 y 6");
        }
        if (orden < 1 || orden > 6) {
            throw new ExpresionInvalidaException("El orden debe estar entre 1 y 6");
        }
        Nodo f = analizar(texto);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.DERIVADA_ORDEN_SUPERIOR,
                texto, f, v);
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, "f = " + f,
                "Se deriva " + orden + " veces seguidas respecto a " + v.getSimbolo());
        Nodo actual = f;
        for (int k = 1; k <= orden; k++) {
            PasoDetallado encabezado = registro.agregar(ReglaCalculo.ORDEN_SUPERIOR, "Derivada de orden " + k, 0,
                    nombreDerivada(k - 1) + " = " + actual, "Se deriva " + nombreDerivada(k - 1) + " para obtener "
                            + nombreDerivada(k));
            Derivador derivador = new Derivador(registro);
            derivador.setProfundidadBase(1);
            actual = derivador.derivar(actual, v);
            encabezado.setResultado(nombreDerivada(k) + " = " + Embellecedor.embellecer(actual));
        }
        actual = Embellecedor.embellecer(actual);
        resultado.setExpresionResultado(actual);
        resultado.setTextoResultado(nombreDerivada(orden) + " = " + actual);
        resultado.setEtiquetaSecundaria(nombreDerivada(orden) + " = " + actual);
        return resultado;
    }

    private static String nombreDerivada(int k) {
        if (k == 0) {
            return "f";
        }
        if (k <= 3) {
            StringBuilder primas = new StringBuilder("f");
            for (int i = 0; i < k; i++) {
                primas.append("'");
            }
            return primas.toString();
        }
        return "f^(" + k + ")";
    }

    private ResultadoCalculo implicita(String texto, RegistroPasos registro) throws CalculadoraException {
        int igual = texto.indexOf('=');
        if (igual < 0 || texto.indexOf('=', igual + 1) >= 0) {
            throw new ExpresionInvalidaException("Escriba una ecuación con un solo signo =, por ejemplo x^2 + y^2 = 25");
        }
        Nodo izquierda = analizarLado(texto.substring(0, igual));
        Nodo derecha = analizarLado(texto.substring(igual + 1));
        Nodo f = s(Nodo.resta(izquierda, derecha));
        if (!f.dependeDe(Variable.Y)) {
            throw new ExpresionInvalidaException("La ecuación debe contener la variable y");
        }
        Derivador silencioso = new Derivador();
        Nodo lx = silencioso.derivar(izquierda, Variable.X);
        Nodo ly = silencioso.derivar(izquierda, Variable.Y);
        Nodo rx = silencioso.derivar(derecha, Variable.X);
        Nodo ry = silencioso.derivar(derecha, Variable.Y);
        Nodo coeficiente = s(Nodo.resta(ly, ry));
        if (coeficiente.esNumero(0.0)) {
            throw new DominioInvalidoException("No se puede despejar y': el coeficiente de y' es 0");
        }
        Nodo libre = s(Nodo.resta(rx, lx));
        Nodo dydx = Embellecedor.embellecer(s(Nodo.div(libre, coeficiente)));

        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.DERIVADA_IMPLICITA, texto, f,
                Variable.X);
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, izquierda + " = " + derecha,
                "y depende de x: y = y(x). Al derivar un término con y se aplica la regla de la cadena y aparece y'");
        PasoDetallado p1 = registro.agregar(ReglaCalculo.IMPLICITA, "Derivar ambos lados respecto a x", 0,
                "d/dx[" + izquierda + "] = d/dx[" + derecha + "]",
                "Lado izquierdo: " + conPrima(lx, ly) + ".  Lado derecho: " + conPrima(rx, ry));
        p1.setResultado(conPrima(lx, ly) + " = " + conPrima(rx, ry));
        registro.marcar(ReglaCalculo.CADENA);
        PasoDetallado p2 = registro.agregar(ReglaCalculo.IMPLICITA, "Agrupar los términos con y'", 0, "",
                "Se pasan los términos con y' a un lado y el resto al otro");
        p2.setResultado("(" + coeficiente + ")·y' = " + libre);
        PasoDetallado p3 = registro.agregar(ReglaCalculo.IMPLICITA, "Despejar y'", 0, "",
                "Se divide entre el coeficiente de y' (equivale a dy/dx = -F_x/F_y con F = " + f + ")");
        p3.setResultado("dy/dx = " + dydx);

        resultado.setExpresionResultado(dydx);
        resultado.setCurvaImplicita(f);
        resultado.setTextoResultado("dy/dx = " + dydx);
        return resultado;
    }

    private static String conPrima(Nodo parteX, Nodo parteY) {
        if (parteY.esNumero(0.0)) {
            return parteX.toString();
        }
        String y = parteY.esNumero(1.0) ? "y'" : "(" + parteY + ")·y'";
        if (parteX.esNumero(0.0)) {
            return y;
        }
        return parteX + " + " + y;
    }

    // ==================================================================
    // LÍMITES (sustitución directa y L'Hôpital)
    // ==================================================================

    private ResultadoCalculo limite(MetodoCalculo metodo, String texto, String puntoTexto, RegistroPasos registro)
            throws CalculadoraException {
        Nodo f = analizar(texto);
        // Límite lateral: "0+" (por la derecha) o "0-" (por la izquierda)
        int lado = 0;
        String puntoLimpio = puntoTexto == null ? "" : puntoTexto.trim();
        if (puntoLimpio.length() > 1 && puntoLimpio.indexOf('∞') < 0) {
            char ultimo = puntoLimpio.charAt(puntoLimpio.length() - 1);
            if (ultimo == '+' || ultimo == '-' || ultimo == '−') {
                lado = ultimo == '+' ? 1 : -1;
                puntoLimpio = puntoLimpio.substring(0, puntoLimpio.length() - 1).trim();
            }
        }
        double a = analizador.analizarLimite(puntoLimpio);
        if (Double.isInfinite(a)) {
            lado = 0;
        }
        String puntoBase = textoLimite(puntoLimpio, a);
        String punto = puntoBase + (lado > 0 ? "⁺" : (lado < 0 ? "⁻" : ""));
        String lim = "lim(x→" + punto + ")";
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.LIMITE, texto, s(f), Variable.X);
        String lateral = lado > 0 ? " (límite por la derecha: x se acerca con valores mayores que " + puntoBase + ")"
                : (lado < 0 ? " (límite por la izquierda: x se acerca con valores menores que " + puntoBase + ")" : "");
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, lim + " " + s(f),
                "Primero se intenta sustituir x = " + puntoBase + " directamente" + lateral);

        Nodo numerador = null;
        Nodo denominador = null;
        if (f instanceof NodoCociente) {
            numerador = s(((NodoCociente) f).getNumerador());
            denominador = s(((NodoCociente) f).getDenominador());
        } else if (f instanceof NodoProducto) {
            // Forma 0·∞: se reescribe como cociente
            Nodo p = s(((NodoProducto) f).getIzquierdo());
            Nodo q = s(((NodoProducto) f).getDerecho());
            Limites.Resultado lp = Limites.limite(p, Variable.X, a, lado);
            Limites.Resultado lq = Limites.limite(q, Variable.X, a, lado);
            if (lp.esCero() && lq.esInfinito()) {
                numerador = p;
                denominador = s(Nodo.div(Nodo.num(1), q));
            } else if (lq.esCero() && lp.esInfinito()) {
                numerador = q;
                denominador = s(Nodo.div(Nodo.num(1), p));
            }
            if (numerador != null) {
                registro.agregar(ReglaCalculo.INFORMACION, "Forma indeterminada 0·∞", 0, s(f).toString(),
                        "Se reescribe el producto como cociente: " + numerador + " / (" + denominador + ")");
            }
        }

        Limites.Resultado valor;
        if (numerador == null) {
            valor = Limites.limite(s(f), Variable.X, a, lado);
            PasoDetallado paso = registro.agregar(ReglaCalculo.SUSTITUCION_DIRECTA,
                    ReglaCalculo.SUSTITUCION_DIRECTA.getNombre(), 0, lim + " " + s(f),
                    "La expresión no es un cociente indeterminado; el límite se obtiene directamente");
            paso.setResultado(valor.texto());
        } else {
            valor = null;
            Derivador derivador = new Derivador();
            for (int iteracion = 0; iteracion < 7 && valor == null; iteracion++) {
                Limites.Resultado ln = Limites.limite(numerador, Variable.X, a, lado);
                Limites.Resultado ld = Limites.limite(denominador, Variable.X, a, lado);
                PasoDetallado evaluacion = registro.agregar(ReglaCalculo.SUSTITUCION_DIRECTA,
                        iteracion == 0 ? "Evaluar numerador y denominador" : "Evaluar de nuevo", 0,
                        lim + " (" + numerador + ")/(" + denominador + ")",
                        "Numerador → " + ln.texto() + ",  denominador → " + ld.texto());
                boolean ceroCero = ln.esCero() && ld.esCero();
                boolean infInf = ln.esInfinito() && ld.esInfinito();
                if (ceroCero || infInf) {
                    evaluacion.setResultado("Forma indeterminada " + (ceroCero ? "0/0" : "∞/∞"));
                    Nodo dn = derivador.derivar(numerador, Variable.X);
                    Nodo dd = derivador.derivar(denominador, Variable.X);
                    PasoDetallado lh = registro.agregar(ReglaCalculo.LHOPITAL, ReglaCalculo.LHOPITAL.getNombre(), 0,
                            lim + " (" + numerador + ")/(" + denominador + ")",
                            "Se derivan por separado: (" + numerador + ")' = " + dn + "   y   (" + denominador + ")' = "
                                    + dd);
                    lh.setResultado(lim + " (" + dn + ")/(" + dd + ")");
                    numerador = dn;
                    denominador = dd;
                    if (denominador.esNumero(0.0)) {
                        throw new DominioInvalidoException("Al derivar, el denominador se volvió 0; L'Hôpital no se puede aplicar");
                    }
                    continue;
                }
                if (iteracion == 0 && metodo == MetodoCalculo.D_LHOPITAL) {
                    resultado.agregarNota("Al sustituir no aparece 0/0 ni ∞/∞, así que no hace falta la regla de L'Hôpital.");
                }
                valor = Limites.limite(s(Nodo.div(numerador, denominador)), Variable.X, a, lado);
                evaluacion.setResultado("Forma determinada: el límite vale " + valor.texto());
            }
            if (valor == null) {
                throw new CalculadoraException("Después de aplicar L'Hôpital varias veces la forma sigue siendo indeterminada");
            }
        }
        if (valor.getTipo() == Limites.Tipo.NO_EXISTE) {
            resultado.agregarNota(lado == 0
                    ? "Los límites laterales no coinciden o la función oscila: el límite no existe. Pruebe con "
                    + puntoBase + "+ o " + puntoBase + "- para ver cada lado."
                    : "La función oscila o no se acerca a ningún valor: el límite no existe.");
        }
        resultado.setValorNumerico(valor.esFinito() ? Double.valueOf(valor.getValor()) : null);
        resultado.setTextoResultado(lim + " " + s(f) + " = " + valor.texto());
        if (valor.esFinito() && !Double.isInfinite(a)) {
            resultado.setPunto(a, valor.getValor());
        }
        return resultado;
    }

    // ==================================================================
    // RECTA TANGENTE
    // ==================================================================

    private ResultadoCalculo tangente(String texto, String puntoTexto, RegistroPasos registro)
            throws CalculadoraException {
        Nodo f = s(analizar(texto));
        if (f.dependeDe(Variable.Y)) {
            throw new ExpresionInvalidaException("La recta tangente se calcula para funciones de x (sin y)");
        }
        double a = analizador.analizarLimite(puntoTexto);
        if (Double.isInfinite(a)) {
            throw new ExpresionInvalidaException("El punto de tangencia debe ser un número finito");
        }
        String ta = textoLimite(puntoTexto, a);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.RECTA_TANGENTE, texto, f,
                Variable.X);
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, "f(x) = " + f + ",  a = " + ta,
                "La recta tangente pasa por el punto (a, f(a)) y su pendiente es la derivada f'(a): "
                        + "y = f(a) + f'(a)·(x - a)");

        double fa = f.evaluar(a, 1.0);
        if (Double.isNaN(fa) || Double.isInfinite(fa)) {
            throw new DominioInvalidoException("f no está definida en x = " + ta + ": ahí no hay recta tangente");
        }
        fa = Limites.redondear(fa);
        PasoDetallado p1 = registro.agregar(ReglaCalculo.RECTA_TANGENTE, "Paso 1: punto de tangencia", 0,
                "f(" + ta + ")", "Se sustituye x = " + ta + " en f(x)");
        p1.setResultado("f(" + ta + ") = " + FormatoNumeros.formatearExacto(fa) + "  →  punto ("
                + ta + ", " + FormatoNumeros.formatearExacto(fa) + ")");

        PasoDetallado p2 = registro.agregar(ReglaCalculo.RECTA_TANGENTE, "Paso 2: derivada", 0, "f'(x)",
                "Se deriva f para obtener la fórmula de la pendiente");
        Derivador derivador = new Derivador(registro);
        derivador.setProfundidadBase(1);
        Nodo df = Embellecedor.embellecer(derivador.derivar(f, Variable.X));
        p2.setResultado("f'(x) = " + df);

        double m = df.evaluar(a, 1.0);
        if (Double.isNaN(m) || Double.isInfinite(m)) {
            throw new DominioInvalidoException("f'(" + ta + ") no existe: en ese punto la tangente es vertical o la "
                    + "gráfica tiene una esquina");
        }
        m = Limites.redondear(m);
        PasoDetallado p3 = registro.agregar(ReglaCalculo.RECTA_TANGENTE, "Paso 3: pendiente", 0, "m = f'(" + ta + ")",
                "Se sustituye x = " + ta + " en la derivada");
        p3.setResultado("m = " + FormatoNumeros.formatearExacto(m));

        Nodo recta = s(Nodo.suma(Nodo.mul(Nodo.num(m), Nodo.var(Variable.X)), Nodo.num(Limites.redondear(fa - m * a))));
        PasoDetallado p4 = registro.agregar(ReglaCalculo.RECTA_TANGENTE, "Paso 4: ecuación de la recta tangente", 0,
                "y = f(a) + m·(x - a)", "y = " + FormatoNumeros.formatearExacto(fa) + " + ("
                        + FormatoNumeros.formatearExacto(m) + ")·(x - (" + ta + "))  y se simplifica");
        p4.setResultado("y = " + recta);

        String normalTexto;
        if (Math.abs(m) < 1e-12) {
            normalTexto = "x = " + ta + " (recta vertical, porque la tangente es horizontal)";
        } else {
            double mn = Limites.redondear(-1.0 / m);
            Nodo normal = s(Nodo.suma(Nodo.mul(Nodo.num(mn), Nodo.var(Variable.X)),
                    Nodo.num(Limites.redondear(fa - mn * a))));
            normalTexto = "y = " + normal;
        }
        PasoDetallado p5 = registro.agregar(ReglaCalculo.RECTA_TANGENTE, "Paso 5: recta normal", 0,
                "m_normal = -1/m", "La normal es perpendicular a la tangente: su pendiente es -1/m");
        p5.setResultado(normalTexto);

        resultado.setExpresionResultado(recta);
        resultado.setEtiquetaSecundaria("Tangente: y = " + recta);
        resultado.setTextoResultado("Tangente en x = " + ta + ":  y = " + recta + "\nNormal:  " + normalTexto);
        resultado.setValorNumerico(Double.valueOf(m));
        resultado.agregarPuntoMarcado(a, fa, "(" + ta + ", " + FormatoNumeros.formatearExacto(fa) + ")");
        return resultado;
    }

    // ==================================================================
    // MÁXIMOS, MÍNIMOS E INFLEXIÓN
    // ==================================================================

    private ResultadoCalculo extremos(EntradaCalculo entrada, RegistroPasos registro) throws CalculadoraException {
        String texto = entrada.getExpresion();
        Nodo f = s(analizar(texto));
        if (f.dependeDe(Variable.Y)) {
            throw new ExpresionInvalidaException("Los extremos se calculan para funciones de x (sin y)");
        }
        double[] intervalo = intervaloFinito(entrada, "Escriba el intervalo [a, b] donde se buscan los extremos");
        double a = intervalo[0];
        double b = intervalo[1];
        String ta = FormatoNumeros.formatearExacto(a);
        String tb = FormatoNumeros.formatearExacto(b);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.EXTREMOS, texto, f,
                Variable.X);
        resultado.setLimites(a, b);
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, "f(x) = " + f + " en [" + ta + ", " + tb + "]",
                "Los máximos y mínimos locales están donde f'(x) = 0 (o donde f' no existe). La segunda derivada dice "
                        + "si es un máximo (f'' < 0) o un mínimo (f'' > 0)");

        PasoDetallado p1 = registro.agregar(ReglaCalculo.PUNTOS_CRITICOS, "Paso 1: primera derivada", 0, "f'(x)",
                "Se deriva f");
        Derivador derivador = new Derivador(registro);
        derivador.setProfundidadBase(1);
        Nodo df = Embellecedor.embellecer(derivador.derivar(f, Variable.X));
        p1.setResultado("f'(x) = " + df);

        List<Double> criticos = interiores(BuscadorRaices.buscar(df, Variable.X, a, b, false), a, b);
        // Puntos donde f' no existe pero f sí (esquinas como |x| en 0)
        List<Double> saltos = interiores(BuscadorRaices.buscar(s(Nodo.div(Nodo.num(1), df)), Variable.X, a, b, true), a, b);
        for (int i = 0; i < saltos.size(); i++) {
            double c = saltos.get(i).doubleValue();
            double fc = f.evaluar(c, 1.0);
            if (!Double.isNaN(fc) && !Double.isInfinite(fc) && !contiene(criticos, c)) {
                criticos.add(Double.valueOf(c));
            }
        }
        ordenar(criticos);
        PasoDetallado p2 = registro.agregar(ReglaCalculo.PUNTOS_CRITICOS, "Paso 2: puntos críticos", 0,
                "f'(x) = 0", "Se buscan los valores de x en [" + ta + ", " + tb + "] donde la derivada vale 0 o no existe");
        p2.setResultado(criticos.isEmpty() ? "No hay puntos críticos en el intervalo" : "x = " + textoLista(criticos));

        PasoDetallado p3 = registro.agregar(ReglaCalculo.CRITERIO_SEGUNDA_DERIVADA, "Paso 3: segunda derivada", 0,
                "f''(x)", "Se deriva f' para usar el criterio de la segunda derivada");
        Nodo d2f = Embellecedor.embellecer(new Derivador().derivar(df, Variable.X));
        p3.setResultado("f''(x) = " + d2f);

        StringBuilder maximos = new StringBuilder();
        StringBuilder minimos = new StringBuilder();
        for (int i = 0; i < criticos.size(); i++) {
            double c = criticos.get(i).doubleValue();
            double fc = Limites.redondear(f.evaluar(c, 1.0));
            double segunda = d2f.evaluar(c, 1.0);
            String tc = FormatoNumeros.formatearExacto(c);
            String punto = "(" + tc + ", " + FormatoNumeros.formatearExacto(fc) + ")";
            String clasificacion;
            String explicacion;
            if (!Double.isNaN(segunda) && !Double.isInfinite(segunda) && Math.abs(segunda) > 1e-7) {
                explicacion = "f''(" + tc + ") = " + FormatoNumeros.formatearExacto(Limites.redondear(segunda))
                        + (segunda > 0 ? " > 0: la curva es cóncava hacia arriba" : " < 0: la curva es cóncava hacia abajo");
                clasificacion = segunda > 0 ? "mínimo" : "máximo";
            } else {
                // Criterio de la primera derivada: signo de f' antes y después
                double h = 1e-4 * (1 + Math.abs(c));
                double antes = df.evaluar(c - h, 1.0);
                double despues = df.evaluar(c + h, 1.0);
                explicacion = "f''(" + tc + ") no decide; se mira el signo de f': antes " + signo(antes)
                        + " y después " + signo(despues);
                if (antes < 0 && despues > 0) {
                    clasificacion = "mínimo";
                } else if (antes > 0 && despues < 0) {
                    clasificacion = "máximo";
                } else {
                    clasificacion = "ninguno";
                }
            }
            PasoDetallado paso = registro.agregar(ReglaCalculo.CRITERIO_SEGUNDA_DERIVADA, "Clasificar x = " + tc, 1,
                    "f''(" + tc + ")", explicacion);
            if (clasificacion.equals("mínimo")) {
                paso.setResultado("Mínimo local en " + punto);
                agregarConComa(minimos, punto);
                resultado.agregarPuntoMarcado(c, fc, "mín " + punto);
            } else if (clasificacion.equals("máximo")) {
                paso.setResultado("Máximo local en " + punto);
                agregarConComa(maximos, punto);
                resultado.agregarPuntoMarcado(c, fc, "máx " + punto);
            } else {
                paso.setResultado("Ni máximo ni mínimo en " + punto + " (la derivada no cambia de signo)");
                resultado.agregarPuntoMarcado(c, fc, punto);
            }
        }

        List<Double> inflexiones = interiores(BuscadorRaices.buscar(d2f, Variable.X, a, b, true), a, b);
        StringBuilder textoInflexiones = new StringBuilder();
        for (int i = 0; i < inflexiones.size(); i++) {
            double c = inflexiones.get(i).doubleValue();
            double fc = f.evaluar(c, 1.0);
            if (Double.isNaN(fc) || Double.isInfinite(fc)) {
                continue;
            }
            fc = Limites.redondear(fc);
            String punto = "(" + FormatoNumeros.formatearExacto(c) + ", " + FormatoNumeros.formatearExacto(fc) + ")";
            agregarConComa(textoInflexiones, punto);
            resultado.agregarPuntoMarcado(c, fc, "inflexión " + punto);
        }
        PasoDetallado p4 = registro.agregar(ReglaCalculo.CRITERIO_SEGUNDA_DERIVADA, "Paso 4: puntos de inflexión", 0,
                "f''(x) = 0", "Donde f'' cambia de signo la curva cambia de concavidad");
        p4.setResultado(textoInflexiones.length() == 0 ? "No hay puntos de inflexión en el intervalo"
                : textoInflexiones.toString());

        // Extremos absolutos: se comparan los extremos del intervalo y los puntos críticos
        List<Double> candidatos = new ArrayList<Double>();
        candidatos.add(Double.valueOf(a));
        candidatos.addAll(criticos);
        candidatos.add(Double.valueOf(b));
        double xMax = Double.NaN;
        double yMax = Double.NEGATIVE_INFINITY;
        double xMin = Double.NaN;
        double yMin = Double.POSITIVE_INFINITY;
        StringBuilder tabla = new StringBuilder();
        for (int i = 0; i < candidatos.size(); i++) {
            double c = candidatos.get(i).doubleValue();
            double fc = f.evaluar(c, 1.0);
            if (Double.isNaN(fc) || Double.isInfinite(fc)) {
                continue;
            }
            fc = Limites.redondear(fc);
            agregarConComa(tabla, "f(" + FormatoNumeros.formatearExacto(c) + ") = " + FormatoNumeros.formatearExacto(fc));
            if (fc > yMax) {
                yMax = fc;
                xMax = c;
            }
            if (fc < yMin) {
                yMin = fc;
                xMin = c;
            }
        }
        PasoDetallado p5 = registro.agregar(ReglaCalculo.PUNTOS_CRITICOS, "Paso 5: extremos absolutos en [" + ta + ", "
                + tb + "]", 0, tabla.toString(), "Se compara f en los extremos del intervalo y en los puntos críticos: "
                + "el mayor valor es el máximo absoluto y el menor, el mínimo absoluto");
        String absolutos;
        if (Double.isNaN(xMax)) {
            absolutos = "Extremos absolutos: no se pudieron calcular (f no está definida en el intervalo)";
        } else {
            absolutos = "Máximo absoluto " + FormatoNumeros.formatearExacto(yMax) + " en x = "
                    + FormatoNumeros.formatearExacto(xMax) + ";  mínimo absoluto " + FormatoNumeros.formatearExacto(yMin)
                    + " en x = " + FormatoNumeros.formatearExacto(xMin);
        }
        p5.setResultado(absolutos);

        StringBuilder resumen = new StringBuilder();
        resumen.append("Máximos locales: ").append(maximos.length() == 0 ? "ninguno" : maximos.toString());
        resumen.append("\nMínimos locales: ").append(minimos.length() == 0 ? "ninguno" : minimos.toString());
        resumen.append("\nInflexión: ").append(textoInflexiones.length() == 0 ? "ninguno" : textoInflexiones.toString());
        resumen.append("\n").append(absolutos);
        resultado.setExpresionResultado(df);
        resultado.setEtiquetaSecundaria("f' = " + df);
        resultado.setTextoResultado(resumen.toString());
        return resultado;
    }

    /** Quita los puntos que coinciden con los extremos del intervalo. */
    private static List<Double> interiores(List<Double> puntos, double a, double b) {
        List<Double> dentro = new ArrayList<Double>();
        for (int i = 0; i < puntos.size(); i++) {
            double p = puntos.get(i).doubleValue();
            if (p > a + 1e-9 && p < b - 1e-9) {
                dentro.add(puntos.get(i));
            }
        }
        return dentro;
    }

    private static String signo(double valor) {
        if (Double.isNaN(valor)) {
            return "(no existe)";
        }
        if (Math.abs(valor) < 1e-12) {
            return "0";
        }
        return valor > 0 ? "(+)" : "(-)";
    }

    private static void agregarConComa(StringBuilder texto, String parte) {
        if (texto.length() > 0) {
            texto.append(",  ");
        }
        texto.append(parte);
    }

    private static boolean contiene(List<Double> lista, double valor) {
        for (int i = 0; i < lista.size(); i++) {
            if (Math.abs(lista.get(i).doubleValue() - valor) < 1e-6) {
                return true;
            }
        }
        return false;
    }

    // ==================================================================
    // ÁREA ENTRE DOS CURVAS
    // ==================================================================

    private ResultadoCalculo areaEntreCurvas(EntradaCalculo entrada, RegistroPasos registro) throws CalculadoraException {
        String texto = entrada.getExpresion();
        Nodo f = s(analizar(texto));
        String textoG = entrada.getFuncionG();
        if (textoG == null || textoG.trim().isEmpty()) {
            throw new ExpresionInvalidaException("Escriba la segunda función g(x), por ejemplo x^2");
        }
        Nodo g = s(analizar(textoG));
        if (f.dependeDe(Variable.Y) || g.dependeDe(Variable.Y)) {
            throw new ExpresionInvalidaException("El área entre curvas se calcula con funciones de x (sin y)");
        }
        Nodo h = s(Nodo.resta(f, g));
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.AREA_ENTRE_CURVAS, texto, f,
                Variable.X);

        boolean sinA = entrada.getLimiteA() == null || entrada.getLimiteA().trim().isEmpty();
        boolean sinB = entrada.getLimiteB() == null || entrada.getLimiteB().trim().isEmpty();
        double a;
        double b;
        String ta;
        String tb;
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, "f(x) = " + f + ",  g(x) = " + g,
                "El área entre dos curvas es A = ∫[a,b] |f(x) - g(x)| dx: en cada tramo se resta la curva de abajo a la "
                        + "de arriba");
        PasoDetallado diferencia = registro.agregar(ReglaCalculo.AREA_ENTRE_CURVAS, "Paso 1: diferencia de las curvas", 0,
                "h(x) = f(x) - g(x)", "Se restan las funciones y se simplifica");
        diferencia.setResultado("h(x) = " + h);
        if (sinA && sinB) {
            List<Double> cortes = BuscadorRaices.buscar(h, Variable.X, -20, 20, false);
            if (cortes.size() < 2) {
                throw new DominioInvalidoException("Las curvas no se cortan en dos puntos entre x = -20 y x = 20. "
                        + "Escriba los límites a y b del área");
            }
            String explicacion;
            if (cortes.size() <= 4) {
                a = cortes.get(0).doubleValue();
                b = cortes.get(cortes.size() - 1).doubleValue();
                explicacion = "Como no se escribieron a y b, se usan el primer y el último punto donde las curvas "
                        + "se cortan (h(x) = 0)";
            } else {
                // Muchas intersecciones (curvas periódicas): se toma la región entre dos cortes seguidos desde x = 0
                int i = 0;
                while (i + 2 < cortes.size() && cortes.get(i).doubleValue() < -1e-9) {
                    i++;
                }
                a = cortes.get(i).doubleValue();
                b = cortes.get(i + 1).doubleValue();
                explicacion = "Las curvas se cortan muchas veces (se repiten). Como no se escribieron a y b, se toma la "
                        + "región entre dos cortes seguidos a partir de x = 0";
            }
            ta = FormatoNumeros.formatearExacto(a);
            tb = FormatoNumeros.formatearExacto(b);
            PasoDetallado corte = registro.agregar(ReglaCalculo.AREA_ENTRE_CURVAS, "Paso 2: puntos de corte", 0,
                    "f(x) = g(x)", explicacion);
            corte.setResultado((cortes.size() <= 8 ? "x = " + textoLista(cortes) + "  →  " : "") + "a = " + ta
                    + ", b = " + tb);
        } else {
            double[] intervalo = intervaloFinito(entrada, "Escriba los dos límites a y b (o deje ambos vacíos para usar "
                    + "los puntos de corte)");
            a = intervalo[0];
            b = intervalo[1];
            ta = FormatoNumeros.formatearExacto(a);
            tb = FormatoNumeros.formatearExacto(b);
        }
        resultado.setLimites(a, b);

        List<Double> internos = BuscadorRaices.buscar(h, Variable.X, a, b, true);
        List<Double> cortes = new ArrayList<Double>();
        cortes.add(Double.valueOf(a));
        for (int i = 0; i < internos.size(); i++) {
            double r = internos.get(i).doubleValue();
            if (r > a + 1e-9 && r < b - 1e-9) {
                cortes.add(Double.valueOf(r));
            }
        }
        cortes.add(Double.valueOf(b));
        if (cortes.size() > 2) {
            PasoDetallado dentro = registro.agregar(ReglaCalculo.AREA_ENTRE_CURVAS, "Cortes dentro del intervalo", 0,
                    "h(x) = 0", "Dentro de [" + ta + ", " + tb + "] las curvas se cruzan, así que cambia cuál está "
                            + "arriba. El intervalo se divide en esos puntos");
            dentro.setResultado("x = " + textoLista(cortes.subList(1, cortes.size() - 1)));
        }

        // Antiderivada de h (si existe); si no, se integra numéricamente
        Nodo antiderivada = null;
        Object[] marca = registro.marca();
        PasoDetallado pasoAnti = registro.agregar(ReglaCalculo.TFC, "Paso 3: antiderivada de h", 0, "∫ " + h + " dx",
                "Se integra la diferencia de las curvas");
        try {
            antiderivada = Embellecedor.embellecer(new Integrador(registro).integrar(h, Variable.X,
                    TecnicaIntegracion.AUTOMATICO));
            pasoAnti.setResultado("H(x) = " + antiderivada);
        } catch (OperacionNoSoportadaException e) {
            registro.restaurar(marca);
            registro.agregar(ReglaCalculo.INT_NUMERICA, "Paso 3: integración numérica", 0, "∫ " + h + " dx",
                    "h no tiene una antiderivada elemental: cada tramo se calcula numéricamente (Gauss-Kronrod)");
        }

        double area = 0;
        for (int i = 0; i + 1 < cortes.size(); i++) {
            double p = cortes.get(i).doubleValue();
            double q = cortes.get(i + 1).doubleValue();
            double tramo = Double.NaN;
            if (antiderivada != null) {
                tramo = antiderivada.evaluar(q, 1.0) - antiderivada.evaluar(p, 1.0);
            }
            if (Double.isNaN(tramo) || Double.isInfinite(tramo)) {
                IntegracionNumerica.Resultado numerico = IntegracionNumerica.integrar(h, Variable.X, p, q, 1.0);
                if (!numerico.esConfiable()) {
                    throw new DominioInvalidoException("El área no es finita entre x = "
                            + FormatoNumeros.formatearExacto(p) + " y x = " + FormatoNumeros.formatearExacto(q));
                }
                tramo = numerico.getValor();
            }
            tramo = Limites.redondear(tramo);
            double medio = h.evaluar((p + q) / 2, 1.0);
            String arriba = medio >= 0 ? "f está arriba de g" : "g está arriba de f";
            String tp = FormatoNumeros.formatearExacto(p);
            String tq = FormatoNumeros.formatearExacto(q);
            PasoDetallado paso = registro.agregar(ReglaCalculo.AREA_ENTRE_CURVAS, "Tramo [" + tp + ", " + tq + "]", 1,
                    "∫[" + tp + ", " + tq + "] h(x) dx", "En este tramo " + arriba
                            + "; el área es el valor absoluto de la integral");
            paso.setResultado(FormatoNumeros.formatearExacto(tramo) + "  →  aporta "
                    + FormatoNumeros.formatearExacto(Math.abs(tramo)));
            area += Math.abs(tramo);
        }
        area = Limites.redondear(area);
        String textoArea = FormatoNumeros.formatearExacto(area);
        String decimal = FormatoNumeros.formatearDecimal(area, 6);
        if (!textoArea.equals(decimal)) {
            textoArea = textoArea + " ≈ " + decimal;
        }
        PasoDetallado total = registro.agregar(ReglaCalculo.AREA_ENTRE_CURVAS, "Paso 4: área total", 0,
                "A = suma de los tramos", "Se suman las áreas de todos los tramos (siempre positivas)");
        total.setResultado("A = " + textoArea);

        resultado.setExpresionResultado(g);
        resultado.setCurvaBaseArea(g);
        resultado.setEtiquetaSecundaria("g = " + g);
        resultado.setValorNumerico(Double.valueOf(area));
        resultado.setTextoResultado("Área entre f y g en [" + ta + ", " + tb + "] = " + textoArea);
        return resultado;
    }

    /** Lee a y b (finitos) y los devuelve ordenados. */
    private double[] intervaloFinito(EntradaCalculo entrada, String mensajeVacio) throws ExpresionInvalidaException {
        if (entrada.getLimiteA() == null || entrada.getLimiteA().trim().isEmpty()
                || entrada.getLimiteB() == null || entrada.getLimiteB().trim().isEmpty()) {
            throw new ExpresionInvalidaException(mensajeVacio);
        }
        double a = analizador.analizarLimite(entrada.getLimiteA());
        double b = analizador.analizarLimite(entrada.getLimiteB());
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            throw new ExpresionInvalidaException("Para esta operación a y b deben ser números finitos");
        }
        if (a == b) {
            throw new ExpresionInvalidaException("El intervalo no puede tener a = b");
        }
        return new double[]{Math.min(a, b), Math.max(a, b)};
    }

    // ==================================================================
    // INTEGRALES
    // ==================================================================

    private static Variable variableIntegral(EntradaCalculo entrada) {
        return entrada.getVariable() == Variable.Y ? Variable.Y : Variable.X;
    }

    private ResultadoCalculo integralIndefinida(String texto, Variable v, TecnicaIntegracion tecnica,
                                                RegistroPasos registro) throws CalculadoraException {
        Nodo f = analizar(texto);
        String dv = " d" + v.getSimbolo();
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.INTEGRAL_INDEFINIDA, texto, f, v);
        registro.agregar(f.dependeDe(v.otra()) ? ReglaCalculo.VARIABLE_CONSTANTE : ReglaCalculo.INFORMACION,
                "Planteamiento", 0, "∫ " + f + dv,
                "Se busca una función F(" + v.getSimbolo() + ") cuya derivada sea " + f
                        + (f.dependeDe(v.otra()) ? " (" + v.otra().getSimbolo() + " se trata como constante)" : ""));
        Nodo antiderivada;
        try {
            antiderivada = Embellecedor.embellecer(new Integrador(registro).integrar(f, v, tecnica));
        } catch (OperacionNoSoportadaException e) {
            if (tecnica == TecnicaIntegracion.AUTOMATICO) {
                throw new OperacionNoSoportadaException(e.getMessage() + " Si necesita un valor, use la integral "
                        + "DEFINIDA: la calculadora la aproxima numéricamente.");
            }
            throw e;
        }
        PasoDetallado constante = registro.agregar(ReglaCalculo.INFORMACION, "Constante de integración", 0, "",
                "Toda integral indefinida lleva + C, porque la derivada de una constante es 0");
        constante.setResultado(antiderivada + " + C");
        resultado.setExpresionResultado(antiderivada);
        resultado.setTextoResultado("∫ " + f + dv + " = " + antiderivada + " + C");
        resultado.setEtiquetaSecundaria("F = " + antiderivada + "  (C = 0)");
        return resultado;
    }

    private ResultadoCalculo integralDefinida(MetodoCalculo metodo, EntradaCalculo entrada, RegistroPasos registro)
            throws CalculadoraException {
        Variable v = variableIntegral(entrada);
        Variable otra = v.otra();
        String x = v.getSimbolo();
        String dv = " d" + x;
        Nodo f = analizar(entrada.getExpresion());
        double a = analizador.analizarLimite(entrada.getLimiteA());
        double b = analizador.analizarLimite(entrada.getLimiteB());
        String ta = textoLimite(entrada.getLimiteA(), a);
        String tb = textoLimite(entrada.getLimiteB(), b);
        String integral = "∫[" + ta + ", " + tb + "] " + f + dv;
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA,
                entrada.getExpresion(), f, v);
        resultado.setLimites(a, b);

        // Propiedad de límites iguales
        if (a == b) {
            PasoDetallado paso = registro.agregar(ReglaCalculo.LIMITES_IGUALES,
                    ReglaCalculo.LIMITES_IGUALES.getNombre(), 0, integral,
                    "∫[a, a] f(" + x + ") d" + x + " = 0: si el límite inferior y el superior son iguales, el intervalo "
                            + "no tiene ancho y el área es cero (no hace falta integrar)");
            paso.setResultado("0");
            resultado.setValorNumerico(Double.valueOf(0));
            resultado.setTextoResultado(integral + " = 0");
            return resultado;
        }
        if (metodo == MetodoCalculo.I_LIMITES_IGUALES) {
            resultado.agregarNota("La propiedad de límites iguales se aplica cuando a = b. Como a ≠ b, se calculó la integral normalmente.");
        }

        boolean conOtra = f.dependeDe(otra);
        registro.agregar(conOtra ? ReglaCalculo.VARIABLE_CONSTANTE : ReglaCalculo.INFORMACION, "Planteamiento", 0,
                integral, "Se aplica el Teorema Fundamental del Cálculo: ∫[a, b] f(" + x + ") d" + x + " = F(b) - F(a)"
                        + (conOtra ? ". La variable " + otra.getSimbolo() + " se trata como una constante" : ""));
        Object[] marca = registro.marca();
        PasoDetallado antider = registro.agregar(ReglaCalculo.TFC, "TFC, paso 1: antiderivada", 0, "∫ " + f + dv,
                "Se calcula una antiderivada F(" + x + ")");
        Nodo antiderivada;
        try {
            antiderivada = Embellecedor.embellecer(new Integrador(registro).integrar(f, v, metodo.getTecnica()));
        } catch (OperacionNoSoportadaException e) {
            if (metodo.getTecnica() != TecnicaIntegracion.AUTOMATICO) {
                throw e;
            }
            if (conOtra) {
                throw new OperacionNoSoportadaException(e.getMessage() + " Como la función contiene "
                        + otra.getSimbolo() + ", no se puede aproximar numéricamente.");
            }
            registro.restaurar(marca);
            return integralNumerica(resultado, f, v, a, b, integral, registro);
        }
        antider.setResultado("F(" + x + ") = " + antiderivada);
        resultado.setExpresionResultado(antiderivada);

        if (conOtra) {
            return integralConOtraVariable(resultado, f, antiderivada, v, entrada, a, b, integral, registro);
        }

        ultimoNoExiste = false;
        double total;
        if (metodo.getOperacion() == MetodoCalculo.Operacion.ADITIVIDAD) {
            double c = analizador.analizarLimite(entrada.getPuntoC());
            if (Double.isInfinite(c)) {
                throw new ExpresionInvalidaException("El punto c debe ser un número finito");
            }
            String tc = textoLimite(entrada.getPuntoC(), c);
            PasoDetallado prop = registro.agregar(ReglaCalculo.ADITIVIDAD, ReglaCalculo.ADITIVIDAD.getNombre(), 0,
                    integral, "∫[a, b] f d" + x + " = ∫[a, c] f d" + x + " + ∫[c, b] f d" + x + "  con c = " + tc);
            double primera = evaluarTFC(antiderivada, f, v, a, c, registro, "∫[" + ta + ", " + tc + "]");
            double segunda = evaluarTFC(antiderivada, f, v, c, b, registro, "∫[" + tc + ", " + tb + "]");
            total = primera + segunda;
            prop.setResultado(FormatoNumeros.formatearExacto(primera) + " + (" + FormatoNumeros.formatearExacto(segunda)
                    + ") = " + FormatoNumeros.formatearExacto(total));
        } else if (a > b || metodo.getOperacion() == MetodoCalculo.Operacion.INVERSION) {
            double inferior = Math.min(a, b);
            double superior = Math.max(a, b);
            String ti = FormatoNumeros.formatearExacto(inferior);
            String ts = FormatoNumeros.formatearExacto(superior);
            PasoDetallado prop = registro.agregar(ReglaCalculo.INVERSION_LIMITES,
                    ReglaCalculo.INVERSION_LIMITES.getNombre(), 0, integral,
                    "∫[a, b] f d" + x + " = -∫[b, a] f d" + x + ": al intercambiar los límites cambia el signo");
            double ordenada = evaluarTFC(antiderivada, f, v, inferior, superior, registro, "∫[" + ti + ", " + ts + "]");
            total = (a > b) ? -ordenada : ordenada;
            prop.setResultado("∫[" + ti + ", " + ts + "] = " + FormatoNumeros.formatearExacto(ordenada) + "  y  ∫["
                    + ts + ", " + ti + "] = " + FormatoNumeros.formatearExacto(-ordenada));
        } else {
            total = evaluarTFC(antiderivada, f, v, a, b, registro, "∫[" + ta + ", " + tb + "]");
        }

        String texto;
        if (Double.isNaN(total) || Double.isInfinite(total)) {
            if (ultimoNoExiste) {
                texto = "no existe";
                resultado.agregarNota("La integral impropia NO EXISTE: F(" + x + ") oscila y no se acerca a ningún "
                        + "valor (no es lo mismo que irse a infinito).");
            } else {
                texto = "diverge";
                resultado.agregarNota("La integral impropia DIVERGE: el área no es un número finito.");
            }
        } else {
            total = Limites.redondear(total);
            texto = textoExacto(antiderivada, v, entrada, a, b, total);
            resultado.setValorNumerico(Double.valueOf(total));
            comprobarNumericamente(f, v, a, b, total, registro, resultado);
        }
        resultado.setTextoResultado(integral + " = " + texto);
        return resultado;
    }

    /**
     * Si F no se encuentra, la integral definida se aproxima con Gauss-Kronrod.
     */
    private ResultadoCalculo integralNumerica(ResultadoCalculo resultado, Nodo f, Variable v, double a, double b,
                                              String integral, RegistroPasos registro) {
        PasoDetallado paso = registro.agregar(ReglaCalculo.INT_NUMERICA, ReglaCalculo.INT_NUMERICA.getNombre(), 0,
                integral, "La función no tiene una antiderivada que se pueda escribir con funciones elementales (o el "
                        + "motor no la encuentra). Por eso el área se calcula numéricamente con la regla de "
                        + "Gauss-Kronrod: se suman los valores de f en puntos bien elegidos y se divide el intervalo hasta "
                        + "que el error es muy pequeño");
        IntegracionNumerica.Resultado numerico = IntegracionNumerica.integrar(f, v, a, b, 1.0);
        if (!numerico.esConfiable()) {
            paso.setResultado("El método numérico no converge: la integral probablemente diverge");
            resultado.agregarNota("No se encontró la antiderivada y el cálculo numérico no converge: la integral "
                    + "probablemente diverge (el área no es finita) o la función oscila sin parar.");
            resultado.setTextoResultado(integral + " = diverge (aproximación numérica)");
            return resultado;
        }
        double valor = numerico.getValor();
        String decimal = FormatoNumeros.formatearDecimal(valor, 8);
        paso.setResultado("≈ " + decimal + "   (error estimado menor que "
                + FormatoNumeros.formatearDecimal(Math.max(numerico.getError(), 1e-10), 10) + ")");
        resultado.agregarNota("Resultado APROXIMADO: no existe una fórmula con funciones elementales para la "
                + "antiderivada, así que el valor se calculó numéricamente (Gauss-Kronrod).");
        resultado.setValorNumerico(Double.valueOf(valor));
        double redondeado = Limites.redondear(valor);
        String exacto = FormatoNumeros.formatearExacto(redondeado);
        if (!exacto.contains(".") && Math.abs(redondeado - valor) < 1e-9) {
            resultado.setTextoResultado(integral + " = " + exacto + " ≈ " + decimal);
        } else {
            resultado.setTextoResultado(integral + " ≈ " + decimal);
        }
        return resultado;
    }

    /**
     * Integral definida de una función con la otra variable: el resultado es una
     * expresión en esa variable (por ejemplo ∫[0,2] x·y dx = 2y).
     */
    private ResultadoCalculo integralConOtraVariable(ResultadoCalculo resultado, Nodo f, Nodo antiderivada, Variable v,
                                                     EntradaCalculo entrada, double a, double b, String integral,
                                                     RegistroPasos registro) throws CalculadoraException {
        String x = v.getSimbolo();
        String otra = v.otra().getSimbolo();
        if (Double.isInfinite(a) || Double.isInfinite(b) || !puntosSingulares(f, v, a, b).isEmpty()) {
            throw new OperacionNoSoportadaException("Las integrales impropias con la otra variable (" + otra
                    + ") no están soportadas. Use límites finitos o sustituya " + otra + " por un número");
        }
        Nodo nodoA = analizador.analizarLimiteNodo(entrada.getLimiteA());
        Nodo nodoB = analizador.analizarLimiteNodo(entrada.getLimiteB());
        Nodo fb = s(antiderivada.reemplazar(Nodo.var(v), nodoB));
        Nodo fa = s(antiderivada.reemplazar(Nodo.var(v), nodoA));
        PasoDetallado p2 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 2: evaluar el límite superior", 0,
                "F(" + nodoB + ")", "Se sustituye " + x + " = " + nodoB + " en F(" + x + "); " + otra + " queda igual");
        p2.setResultado(fb.toString());
        PasoDetallado p3 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 3: evaluar el límite inferior", 0,
                "F(" + nodoA + ")", "Se sustituye " + x + " = " + nodoA + " en F(" + x + ")");
        p3.setResultado(fa.toString());
        Nodo total = Embellecedor.embellecer(s(Nodo.resta(fb, fa)));
        PasoDetallado p4 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 4: resultado neto", 0,
                "F(b) - F(a)", "R = F(b) - F(a)");
        p4.setResultado(total.toString());
        resultado.agregarNota("El resultado depende de " + otra + " porque " + otra + " se trató como una constante. "
                + "En la gráfica se usa " + otra + " = 1.");
        resultado.setTextoResultado(integral + " = " + total);
        return resultado;
    }

    /**
     * Forma exacta del resultado (por ejemplo e - 1 o π/4) cuando se puede
     * obtener sustituyendo los límites en F; si no, el número formateado.
     */
    private String textoExacto(Nodo antiderivada, Variable v, EntradaCalculo entrada, double a, double b, double total) {
        String numero = FormatoNumeros.formatearExacto(total);
        String decimal = FormatoNumeros.formatearDecimal(total, 6);
        boolean numeroSimple = !numero.contains(".");
        if (!numeroSimple && !Double.isInfinite(a) && !Double.isInfinite(b)) {
            try {
                Nodo nodoA = analizador.analizarLimiteNodo(entrada.getLimiteA());
                Nodo nodoB = analizador.analizarLimiteNodo(entrada.getLimiteB());
                Nodo exacto = Embellecedor.embellecer(s(Nodo.resta(s(antiderivada.reemplazar(Nodo.var(v), nodoB)),
                        s(antiderivada.reemplazar(Nodo.var(v), nodoA)))));
                double valor = exacto.evaluar(0, 1.0);
                String textoExacto = exacto.toString();
                boolean coincide = Math.abs(valor - total) < 1e-7 * (1 + Math.abs(total));
                if (coincide && !exacto.esNumero() && textoExacto.length() <= 40 && !textoExacto.contains(".")) {
                    return textoExacto + " ≈ " + decimal;
                }
            } catch (ExpresionInvalidaException e) {
                // Se usa el número
            }
        }
        if (!numero.equals(decimal)) {
            return numero + " ≈ " + decimal;
        }
        return numero;
    }

    /** Compara el resultado exacto con una aproximación numérica independiente. */
    private void comprobarNumericamente(Nodo f, Variable v, double a, double b, double total, RegistroPasos registro,
                                        ResultadoCalculo resultado) {
        IntegracionNumerica.Resultado numerico = IntegracionNumerica.integrar(f, v, a, b, 1.0);
        if (!numerico.esConfiable()) {
            return;
        }
        double valor = numerico.getValor();
        boolean coincide = Math.abs(valor - total) < 1e-5 * (1 + Math.abs(total));
        PasoDetallado paso = registro.agregar(ReglaCalculo.VERIFICACION, "Comprobación numérica", 0,
                "Gauss-Kronrod", "Se calcula el área de otra forma (numéricamente, sin la antiderivada) para comprobar "
                        + "el resultado");
        paso.setResultado("≈ " + FormatoNumeros.formatearDecimal(valor, 8)
                + (coincide ? "  ✓ coincide con el resultado exacto" : "  ✗ NO coincide"));
        if (!coincide) {
            resultado.agregarNota("Atención: la comprobación numérica da " + FormatoNumeros.formatearDecimal(valor, 6)
                    + ", distinto del resultado obtenido. Revise si la función tiene discontinuidades en el intervalo.");
        }
    }

    /**
     * Evalúa F(b) − F(a) con límites cuando hace falta (integrales impropias).
     *
     * @return valor (NaN o ±∞ si diverge)
     */
    private double evaluarTFC(Nodo antiderivada, Nodo f, Variable v, double a, double b, RegistroPasos registro,
                              String nombre) {
        String x = v.getSimbolo();
        List<Double> singulares = puntosSingulares(f, v, a, b);
        boolean infinitoEnExtremo = !finito(f.evaluarEn(v, a, 1.0)) || !finito(f.evaluarEn(v, b, 1.0));
        boolean impropia = Double.isInfinite(a) || Double.isInfinite(b) || !singulares.isEmpty() || infinitoEnExtremo;
        if (impropia) {
            StringBuilder motivo = new StringBuilder();
            if (Double.isInfinite(a) || Double.isInfinite(b)) {
                motivo.append("tiene un límite infinito");
            }
            if (!singulares.isEmpty() || infinitoEnExtremo) {
                if (motivo.length() > 0) {
                    motivo.append(" y ");
                }
                motivo.append("la función no está definida (se va a infinito) en ");
                if (!singulares.isEmpty()) {
                    motivo.append(x).append(" = ").append(textoLista(singulares)).append(", dentro del intervalo");
                } else {
                    motivo.append("un extremo del intervalo");
                }
            }
            registro.agregar(ReglaCalculo.IMPROPIA, ReglaCalculo.IMPROPIA.getNombre(), 0, nombre,
                    "La integral " + motivo + ". Se reemplaza el valor problemático por t y se calcula el límite de F(t)"
                            + (singulares.isEmpty() ? "" : "; el intervalo se divide en los puntos singulares"));
        }

        List<Double> cortes = new ArrayList<Double>();
        cortes.add(Double.valueOf(a));
        for (int i = 0; i < singulares.size(); i++) {
            cortes.add(singulares.get(i));
        }
        cortes.add(Double.valueOf(b));

        double total = 0;
        for (int i = 0; i + 1 < cortes.size(); i++) {
            double izquierda = cortes.get(i).doubleValue();
            double derecha = cortes.get(i + 1).doubleValue();
            Limites.Resultado fb = Limites.limite(antiderivada, v, derecha, -1);
            Limites.Resultado fa = Limites.limite(antiderivada, v, izquierda, 1);
            String tIzq = FormatoNumeros.formatearExacto(izquierda);
            String tDer = FormatoNumeros.formatearExacto(derecha);
            boolean limiteB = Double.isInfinite(derecha) || singulares.contains(Double.valueOf(derecha))
                    || !finito(f.evaluarEn(v, derecha, 1.0));
            boolean limiteA = Double.isInfinite(izquierda) || singulares.contains(Double.valueOf(izquierda))
                    || !finito(f.evaluarEn(v, izquierda, 1.0));
            PasoDetallado p2 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 2: evaluar el límite superior", 0,
                    limiteB ? "lim(t→" + tDer + (Double.isInfinite(derecha) ? "" : "-") + ") F(t)" : "F(" + tDer + ")",
                    "Se sustituye " + x + " = " + tDer + " en F(" + x + ")");
            p2.setResultado(fb.texto());
            PasoDetallado p3 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 3: evaluar el límite inferior", 0,
                    limiteA ? "lim(t→" + tIzq + (Double.isInfinite(izquierda) ? "" : "+") + ") F(t)" : "F(" + tIzq + ")",
                    "Se sustituye " + x + " = " + tIzq + " en F(" + x + ")");
            p3.setResultado(fa.texto());
            PasoDetallado p4 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 4: resultado neto", 0,
                    "F(" + tDer + ") - F(" + tIzq + ")", "R = F(b) - F(a)");
            if (!fa.esFinito() || !fb.esFinito()) {
                boolean noExiste = fa.getTipo() == Limites.Tipo.NO_EXISTE || fb.getTipo() == Limites.Tipo.NO_EXISTE;
                if (noExiste) {
                    ultimoNoExiste = true;
                    p4.setResultado("F(t) oscila y no tiene límite: la integral no existe");
                } else {
                    p4.setResultado("Uno de los límites es infinito: la integral diverge");
                }
                return Double.NaN;
            }
            double parcial = fb.getValor() - fa.getValor();
            p4.setResultado(fb.texto() + " - (" + fa.texto() + ") = " + FormatoNumeros.formatearExacto(Limites.redondear(parcial)));
            total = total + parcial;
        }
        return total;
    }

    /** Raíces reales de los denominadores polinómicos dentro de (a, b). */
    private List<Double> puntosSingulares(Nodo f, Variable v, double a, double b) {
        List<Double> puntos = new ArrayList<Double>();
        double inferior = Math.min(a, b);
        double superior = Math.max(a, b);
        Simplificador.Monomio m = Integrador.monomio(s(f));
        for (Nodo[] factor : m.factores.values()) {
            Nodo exponente = s(factor[1]);
            if (!exponente.esNumero() || ((NodoConstante) exponente).getValor() >= 0) {
                continue;
            }
            Polinomio p = Polinomio.desde(factor[0], v);
            if (p == null || p.grado() < 1) {
                continue;
            }
            List<Polinomio.Factor> factores = p.factorizar();
            for (int i = 0; i < factores.size(); i++) {
                Polinomio.Factor fac = factores.get(i);
                if (!fac.cuadratico && fac.a > inferior && fac.a < superior
                        && !puntos.contains(Double.valueOf(fac.a))) {
                    puntos.add(Double.valueOf(fac.a));
                }
            }
        }
        ordenar(puntos);
        if (a > b) {
            List<Double> invertidos = new ArrayList<Double>();
            for (int i = puntos.size() - 1; i >= 0; i--) {
                invertidos.add(puntos.get(i));
            }
            return invertidos;
        }
        return puntos;
    }

    /** Ordena de menor a mayor (método de inserción). */
    private static void ordenar(List<Double> puntos) {
        for (int i = 1; i < puntos.size(); i++) {
            Double actual = puntos.get(i);
            int j = i - 1;
            while (j >= 0 && puntos.get(j).doubleValue() > actual.doubleValue()) {
                puntos.set(j + 1, puntos.get(j));
                j--;
            }
            puntos.set(j + 1, actual);
        }
    }

    private static String textoLista(List<Double> valores) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                texto.append(", ");
            }
            texto.append(FormatoNumeros.formatearExacto(valores.get(i).doubleValue()));
        }
        return texto.toString();
    }

    private static boolean finito(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    /**
     * Texto de un límite para mostrarlo: si el número no tiene una forma exacta
     * conocida (por ejemplo ln(2)), se muestra lo que escribió el estudiante.
     */
    private static String textoLimite(String escrito, double valor) {
        String texto = FormatoNumeros.formatearExacto(valor);
        if (texto.contains(".") && escrito != null && !escrito.trim().isEmpty()) {
            return escrito.trim().replace("pi", "π");
        }
        return texto;
    }

    // ==================================================================
    // Utilidades
    // ==================================================================

    private Nodo analizar(String texto) throws ExpresionInvalidaException {
        String limpio = AnalizadorSimbolico.quitarPrefijoFuncion(texto);
        if (limpio != null && limpio.indexOf('=') >= 0) {
            throw new ExpresionInvalidaException("El signo '=' solo se usa en el método de derivación implícita");
        }
        Nodo n = analizador.analizar(limpio);
        avisos.addAll(analizador.getAvisos());
        return n;
    }

    /** Analiza un lado de una ecuación (derivación implícita). */
    private Nodo analizarLado(String texto) throws ExpresionInvalidaException {
        Nodo n = analizador.analizar(texto);
        avisos.addAll(analizador.getAvisos());
        return n;
    }

    private static Nodo s(Nodo n) {
        return Simplificador.simplificar(n);
    }
}
