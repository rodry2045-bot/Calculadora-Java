package calculadora.simbolico;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.DominioInvalidoException;
import calculadora.excepciones.ExpresionInvalidaException;
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

    /**
     * Resuelve el ejercicio con el método indicado.
     *
     * @param metodo  método elegido en la lista
     * @param entrada datos escritos por el estudiante
     * @return resultado con procedimiento
     * @throws CalculadoraException si hay errores de escritura, de dominio o de soporte
     */
    public ResultadoCalculo resolver(MetodoCalculo metodo, EntradaCalculo entrada) throws CalculadoraException {
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
                resultado = limite(entrada.getExpresion(), entrada.getPunto(), registro);
                break;
            case INTEGRAL:
                if (entrada.isDefinida()) {
                    resultado = integralDefinida(metodo, entrada, registro);
                } else {
                    resultado = integralIndefinida(entrada.getExpresion(), metodo.getTecnica(), registro);
                }
                break;
            default:
                resultado = integralDefinida(metodo, entrada, registro);
                break;
        }
        resultado.setNombreMetodo(metodo.getNombre());
        resultado.agregarPasosDetallados(registro.getPasos());
        resaltar(metodo, resultado, registro.getUsadas());
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
        Nodo derivada = derivador.derivar(f, v);
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
            encabezado.setResultado(nombreDerivada(k) + " = " + actual);
        }
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
        Nodo izquierda = analizar(texto.substring(0, igual));
        Nodo derecha = analizar(texto.substring(igual + 1));
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
        Nodo dydx = s(Nodo.div(libre, coeficiente));

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
    // LÍMITES (L'Hôpital)
    // ==================================================================

    private ResultadoCalculo limite(String texto, String puntoTexto, RegistroPasos registro) throws CalculadoraException {
        Nodo f = analizador.analizar(texto);
        double a = analizador.analizarLimite(puntoTexto);
        String punto = FormatoNumeros.formatearExacto(a);
        String lim = "lim(x→" + punto + ")";
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.LIMITE, texto, s(f), Variable.X);
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, lim + " " + s(f),
                "Primero se intenta sustituir x = " + punto + " directamente");

        Nodo numerador = null;
        Nodo denominador = null;
        if (f instanceof NodoCociente) {
            numerador = s(((NodoCociente) f).getNumerador());
            denominador = s(((NodoCociente) f).getDenominador());
        } else if (f instanceof NodoProducto) {
            // Forma 0·∞: se reescribe como cociente
            Nodo p = s(((NodoProducto) f).getIzquierdo());
            Nodo q = s(((NodoProducto) f).getDerecho());
            Limites.Resultado lp = Limites.limite(p, Variable.X, a, 0);
            Limites.Resultado lq = Limites.limite(q, Variable.X, a, 0);
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
            valor = Limites.limite(s(f), Variable.X, a, 0);
            PasoDetallado paso = registro.agregar(ReglaCalculo.SUSTITUCION_DIRECTA,
                    ReglaCalculo.SUSTITUCION_DIRECTA.getNombre(), 0, lim + " " + s(f),
                    "La expresión no es un cociente indeterminado; el límite se obtiene directamente");
            paso.setResultado(valor.texto());
        } else {
            valor = null;
            Derivador derivador = new Derivador();
            for (int iteracion = 0; iteracion < 7 && valor == null; iteracion++) {
                Limites.Resultado ln = Limites.limite(numerador, Variable.X, a, 0);
                Limites.Resultado ld = Limites.limite(denominador, Variable.X, a, 0);
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
                if (iteracion == 0) {
                    resultado.agregarNota("Al sustituir no aparece 0/0 ni ∞/∞, así que no hace falta la regla de L'Hôpital.");
                }
                valor = Limites.limite(s(Nodo.div(numerador, denominador)), Variable.X, a, 0);
                evaluacion.setResultado("Forma determinada: el límite vale " + valor.texto());
            }
            if (valor == null) {
                throw new CalculadoraException("Después de aplicar L'Hôpital varias veces la forma sigue siendo indeterminada");
            }
        }
        if (valor.getTipo() == Limites.Tipo.NO_EXISTE) {
            resultado.agregarNota("Los límites laterales no coinciden o la función oscila: el límite no existe.");
        }
        resultado.setValorNumerico(valor.esFinito() ? Double.valueOf(valor.getValor()) : null);
        resultado.setTextoResultado(lim + " " + s(f) + " = " + valor.texto());
        if (valor.esFinito() && !Double.isInfinite(a)) {
            resultado.setPunto(a, valor.getValor());
        }
        return resultado;
    }

    // ==================================================================
    // INTEGRALES
    // ==================================================================

    private ResultadoCalculo integralIndefinida(String texto, TecnicaIntegracion tecnica, RegistroPasos registro)
            throws CalculadoraException {
        Nodo f = analizar(texto);
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.INTEGRAL_INDEFINIDA, texto, f,
                Variable.X);
        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, "∫ " + f + " dx",
                "Se busca una función F(x) cuya derivada sea " + f
                        + (f.dependeDe(Variable.Y) ? " (y se trata como constante)" : ""));
        Nodo antiderivada = new Integrador(registro).integrar(f, Variable.X, tecnica);
        PasoDetallado constante = registro.agregar(ReglaCalculo.INFORMACION, "Constante de integración", 0, "",
                "Toda integral indefinida lleva + C, porque la derivada de una constante es 0");
        constante.setResultado(antiderivada + " + C");
        resultado.setExpresionResultado(antiderivada);
        resultado.setTextoResultado("∫ " + f + " dx = " + antiderivada + " + C");
        resultado.setEtiquetaSecundaria("F = " + antiderivada + "  (C = 0)");
        return resultado;
    }

    private ResultadoCalculo integralDefinida(MetodoCalculo metodo, EntradaCalculo entrada, RegistroPasos registro)
            throws CalculadoraException {
        Nodo f = analizar(entrada.getExpresion());
        double a = analizador.analizarLimite(entrada.getLimiteA());
        double b = analizador.analizarLimite(entrada.getLimiteB());
        String ta = FormatoNumeros.formatearExacto(a);
        String tb = FormatoNumeros.formatearExacto(b);
        String integral = "∫[" + ta + ", " + tb + "] " + f + " dx";
        ResultadoCalculo resultado = new ResultadoCalculo(ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA,
                entrada.getExpresion(), f, Variable.X);
        resultado.setLimites(a, b);

        // Propiedad de límites iguales
        if (a == b) {
            PasoDetallado paso = registro.agregar(ReglaCalculo.LIMITES_IGUALES,
                    ReglaCalculo.LIMITES_IGUALES.getNombre(), 0, integral,
                    "∫[a, a] f(x) dx = 0: si el límite inferior y el superior son iguales, el intervalo no tiene ancho "
                            + "y el área es cero (no hace falta integrar)");
            paso.setResultado("0");
            resultado.setValorNumerico(Double.valueOf(0));
            resultado.setTextoResultado(integral + " = 0");
            return resultado;
        }
        if (metodo == MetodoCalculo.I_LIMITES_IGUALES) {
            resultado.agregarNota("La propiedad de límites iguales se aplica cuando a = b. Como a ≠ b, se calculó la integral normalmente.");
        }

        registro.agregar(ReglaCalculo.INFORMACION, "Planteamiento", 0, integral,
                "Se aplica el Teorema Fundamental del Cálculo: ∫[a, b] f(x) dx = F(b) - F(a)");
        PasoDetallado antider = registro.agregar(ReglaCalculo.TFC, "TFC, paso 1: antiderivada", 0, "∫ " + f + " dx",
                "Se calcula una antiderivada F(x)");
        Nodo antiderivada = new Integrador(registro).integrar(f, Variable.X, metodo.getTecnica());
        antider.setResultado("F(x) = " + antiderivada);
        resultado.setExpresionResultado(antiderivada);

        double total;
        if (metodo.getOperacion() == MetodoCalculo.Operacion.ADITIVIDAD) {
            double c = analizador.analizarLimite(entrada.getPuntoC());
            if (Double.isInfinite(c)) {
                throw new ExpresionInvalidaException("El punto c debe ser un número finito");
            }
            String tc = FormatoNumeros.formatearExacto(c);
            PasoDetallado prop = registro.agregar(ReglaCalculo.ADITIVIDAD, ReglaCalculo.ADITIVIDAD.getNombre(), 0,
                    integral, "∫[a, b] f dx = ∫[a, c] f dx + ∫[c, b] f dx  con c = " + tc);
            double primera = evaluarTFC(antiderivada, f, a, c, registro, "∫[" + ta + ", " + tc + "]");
            double segunda = evaluarTFC(antiderivada, f, c, b, registro, "∫[" + tc + ", " + tb + "]");
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
                    "∫[a, b] f dx = -∫[b, a] f dx: al intercambiar los límites cambia el signo");
            double ordenada = evaluarTFC(antiderivada, f, inferior, superior, registro, "∫[" + ti + ", " + ts + "]");
            total = (a > b) ? -ordenada : ordenada;
            prop.setResultado("∫[" + ti + ", " + ts + "] = " + FormatoNumeros.formatearExacto(ordenada) + "  y  ∫["
                    + ts + ", " + ti + "] = " + FormatoNumeros.formatearExacto(-ordenada));
        } else {
            total = evaluarTFC(antiderivada, f, a, b, registro, "∫[" + ta + ", " + tb + "]");
        }

        String texto;
        if (Double.isNaN(total) || Double.isInfinite(total)) {
            texto = "diverge";
            resultado.agregarNota("La integral impropia DIVERGE: el área no es un número finito.");
        } else {
            total = Limites.redondear(total);
            texto = FormatoNumeros.formatearExacto(total);
            String decimal = FormatoNumeros.formatearDecimal(total, 6);
            if (!texto.equals(decimal)) {
                texto = texto + " ≈ " + decimal;
            }
            resultado.setValorNumerico(Double.valueOf(total));
        }
        resultado.setTextoResultado(integral + " = " + texto);
        return resultado;
    }

    /**
     * Evalúa F(b) − F(a) con límites cuando hace falta (integrales impropias).
     *
     * @return valor (NaN o ±∞ si diverge)
     */
    private double evaluarTFC(Nodo antiderivada, Nodo f, double a, double b, RegistroPasos registro, String nombre) {
        List<Double> singulares = puntosSingulares(f, a, b);
        boolean infinitoEnExtremo = !finito(f.evaluar(a, 1.0)) || !finito(f.evaluar(b, 1.0));
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
                    motivo.append("x = ").append(textoLista(singulares)).append(", dentro del intervalo");
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
            Limites.Resultado fb = Limites.limite(antiderivada, Variable.X, derecha, -1);
            Limites.Resultado fa = Limites.limite(antiderivada, Variable.X, izquierda, 1);
            String tIzq = FormatoNumeros.formatearExacto(izquierda);
            String tDer = FormatoNumeros.formatearExacto(derecha);
            boolean limiteB = Double.isInfinite(derecha) || singulares.contains(Double.valueOf(derecha))
                    || !finito(f.evaluar(derecha, 1.0));
            boolean limiteA = Double.isInfinite(izquierda) || singulares.contains(Double.valueOf(izquierda))
                    || !finito(f.evaluar(izquierda, 1.0));
            PasoDetallado p2 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 2: evaluar el límite superior", 0,
                    limiteB ? "lim(t→" + tDer + (Double.isInfinite(derecha) ? "" : "-") + ") F(t)" : "F(" + tDer + ")",
                    "Se sustituye x = " + tDer + " en F(x)");
            p2.setResultado(fb.texto());
            PasoDetallado p3 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 3: evaluar el límite inferior", 0,
                    limiteA ? "lim(t→" + tIzq + (Double.isInfinite(izquierda) ? "" : "+") + ") F(t)" : "F(" + tIzq + ")",
                    "Se sustituye x = " + tIzq + " en F(x)");
            p3.setResultado(fa.texto());
            PasoDetallado p4 = registro.agregar(ReglaCalculo.TFC, "TFC, paso 4: resultado neto", 0,
                    "F(" + tDer + ") - F(" + tIzq + ")", "R = F(b) - F(a)");
            if (!fa.esFinito() || !fb.esFinito()) {
                p4.setResultado("Uno de los límites no es finito: la integral diverge");
                return Double.NaN;
            }
            double parcial = fb.getValor() - fa.getValor();
            p4.setResultado(fb.texto() + " - (" + fa.texto() + ") = " + FormatoNumeros.formatearExacto(Limites.redondear(parcial)));
            total = total + parcial;
        }
        return total;
    }

    /** Raíces reales de los denominadores polinómicos dentro de (a, b). */
    private List<Double> puntosSingulares(Nodo f, double a, double b) {
        List<Double> puntos = new ArrayList<Double>();
        double inferior = Math.min(a, b);
        double superior = Math.max(a, b);
        Simplificador.Monomio m = Integrador.monomio(s(f));
        for (Nodo[] factor : m.factores.values()) {
            Nodo exponente = s(factor[1]);
            if (!exponente.esNumero() || ((NodoConstante) exponente).getValor() >= 0) {
                continue;
            }
            Polinomio p = Polinomio.desde(factor[0], Variable.X);
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
        // Ordenar de menor a mayor (inserción)
        for (int i = 1; i < puntos.size(); i++) {
            Double actual = puntos.get(i);
            int j = i - 1;
            while (j >= 0 && puntos.get(j).doubleValue() > actual.doubleValue()) {
                puntos.set(j + 1, puntos.get(j));
                j--;
            }
            puntos.set(j + 1, actual);
        }
        if (a > b) {
            List<Double> invertidos = new ArrayList<Double>();
            for (int i = puntos.size() - 1; i >= 0; i--) {
                invertidos.add(puntos.get(i));
            }
            return invertidos;
        }
        return puntos;
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

    // ==================================================================
    // Utilidades
    // ==================================================================

    private Nodo analizar(String texto) throws ExpresionInvalidaException {
        if (texto != null && texto.indexOf('=') >= 0) {
            throw new ExpresionInvalidaException("El signo '=' solo se usa en el método de derivación implícita");
        }
        return analizador.analizar(texto);
    }

    private static Nodo s(Nodo n) {
        return Simplificador.simplificar(n);
    }
}
