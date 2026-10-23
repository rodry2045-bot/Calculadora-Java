package calculadora.pruebas;

import calculadora.excepciones.CalculadoraException;
import calculadora.excepciones.DominioInvalidoException;
import calculadora.excepciones.ExpresionInvalidaException;
import calculadora.excepciones.IAException;
import calculadora.ia.ConfiguracionIA;
import calculadora.ia.ProveedorAnthropic;
import calculadora.ia.ProveedorCompatibleOpenAI;
import calculadora.ia.TutorIA;
import calculadora.util.JsonSencillo;
import calculadora.excepciones.OperacionNoSoportadaException;
import calculadora.modelo.Calculadora;
import calculadora.modelo.EntradaCalculo;
import calculadora.modelo.MetodoCalculo;
import calculadora.modelo.Graficable;
import calculadora.modelo.ResultadoCalculo;
import calculadora.modelo.Variable;
import calculadora.simbolico.AnalizadorSimbolico;
import calculadora.simbolico.Sugeridor;
import calculadora.util.FormatoNumeros;

import java.util.List;

/**
 * Pruebas automáticas SIN JUnit.
 * <p>
 * Cada prueba compara un valor esperado con el obtenido. Al final se
 * muestra un resumen y, si alguna falla, el programa termina con código 1
 * (útil para scripts como ejecutar_pruebas.bat).
 * </p>
 */
public class PruebasCalculadora {

    private static int pruebasExitosas = 0;
    private static int pruebasFallidas = 0;

    /**
     * Punto de entrada de las pruebas.
     *
     * @param args no se usan
     */
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" PRUEBAS DE LA CALCULADORA DE CÁLCULO");
        System.out.println("==============================================");

        probarFormatoNumeros();
        probarAnalizador();
        probarDerivadas();
        probarIntegrales();
        probarIntegralesDefinidas();
        probarAplicaciones();
        probarErrores();
        probarJson();
        probarModuloIA();
        probarMotorSimbolico();
        probarListaDeMetodos();

        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println("Pruebas exitosas: " + pruebasExitosas);
        System.out.println("Pruebas fallidas: " + pruebasFallidas);
        System.out.println("----------------------------------------------");
        if (pruebasFallidas > 0) {
            System.out.println("RESULTADO: HAY PRUEBAS FALLIDAS");
            System.exit(1);
        }
        System.out.println("RESULTADO: TODAS LAS PRUEBAS PASARON");
    }

    // ------------------------------------------------------------------
    // Etapa 1: utilidades y modelo
    // ------------------------------------------------------------------

    private static void probarFormatoNumeros() {
        seccion("FormatoNumeros");
        verificarTexto("Entero 6.0", "6", FormatoNumeros.formatear(6.0));
        verificarTexto("Fracción 1/3", "1/3", FormatoNumeros.formatear(1.0 / 3.0));
        verificarTexto("Fracción -3/4", "-3/4", FormatoNumeros.formatear(-0.75));
        verificarTexto("Decimal pi", "3.141593", FormatoNumeros.formatear(Math.PI));
        verificarTexto("Coeficiente 1", "", FormatoNumeros.formatearCoeficiente(1.0));
        verificarTexto("Coeficiente -1/2", "-(1/2)", FormatoNumeros.formatearCoeficiente(-0.5));
        verificarTexto("Exponente -1", "^(-1)", FormatoNumeros.formatearExponente(-1.0));
        verificarTexto("Exponente 1/2", "^(1/2)", FormatoNumeros.formatearExponente(0.5));
        verificarTexto("Forma exacta de π/4", "π/4", FormatoNumeros.formatearExacto(Math.PI / 4));
        verificarTexto("Forma exacta de e", "e", FormatoNumeros.formatearExacto(Math.E));
        verificarTexto("Forma exacta de 1/e", "1/e", FormatoNumeros.formatearExacto(1 / Math.E));
        verificarTexto("Forma exacta de 1/√3", "√3/3", FormatoNumeros.formatearExacto(1 / Math.sqrt(3)));
        verificarTexto("Forma exacta de 2√2", "2√2", FormatoNumeros.formatearExacto(2 * Math.sqrt(2)));
    }

    // ------------------------------------------------------------------
    // Analizador: cómo se lee lo que escribe el estudiante
    // ------------------------------------------------------------------

    private static final Calculadora CALCULADORA = new Calculadora();

    private static void probarAnalizador() {
        seccion("Analizador de expresiones");
        verificarInterpretacion("3x^2 - 2x + 1", "3x^2 - 2x + 1");
        verificarInterpretacion("-x^2", "-x^2");
        verificarInterpretacion("x - x", "0");
        verificarInterpretacion("2·x × 3", "6x");
        verificarInterpretacion("e^-x", "e^(-x)");
        verificarInterpretacion("2,5x", "5x/2");
        verificarInterpretacion("x**2", "x^2");
        verificarInterpretacion("f(x) = x^2 + 1", "x^2 + 1");
        verificarInterpretacion("y = sin(x)", "sin(x)");

        seccion("Analizador: funciones sin paréntesis y nombres en español");
        verificarInterpretacion("sinx", "sin(x)");
        verificarInterpretacion("sin x", "sin(x)");
        verificarInterpretacion("sin 2x", "sin(2x)");
        verificarInterpretacion("ln x", "ln(x)");
        verificarInterpretacion("lnx + 1", "ln(x) + 1");
        verificarInterpretacion("sinx/x", "sin(x)/x");
        verificarInterpretacion("sin x cos x", "sin(x)·cos(x)");
        verificarInterpretacion("sen(x)", "sin(x)");
        verificarInterpretacion("arcsen(x)", "arcsin(x)");
        verificarInterpretacion("arctg x", "arctan(x)");
        verificarInterpretacion("raiz(x)", "√(x)");
        verificarInterpretacion("senh(x)", "sinh(x)");
        verificarInterpretacion("cosh x", "cosh(x)");
        verificarInterpretacion("5!", "120");
        verificarInterpretacion("3!x", "6x");

        seccion("Analizador: avisos de lecturas ambiguas");
        verificarAviso("e^2x", true);
        verificarAviso("1/2x", true);
        verificarAviso("√2x", true);
        verificarAviso("cosx^2", true);
        verificarAviso("e^(2x)", false);
        verificarAviso("1/(2x)", false);
        verificarAviso("3x^2y", false);

        seccion("Sugerencias para expresiones mal escritas");
        verificarTexto("son(x) → sin(x)", "sin(x)", Sugeridor.sugerir("son(x)"));
        verificarTexto("seno(x) → sin(x)", "sin(x)", Sugeridor.sugerir("seno(x)"));
        verificarTexto("sqr(x) → sqrt(x)", "sqrt(x)", Sugeridor.sugerir("sqr(x)"));
        verificarTexto("sin(x → sin(x)", "sin(x)", Sugeridor.sugerir("sin(x"));
        verificarTexto("x^2 + → x^2", "x^2", Sugeridor.sugerir("x^2 +"));
        verificarTexto("t^2 + 1 → x^2 + 1", "x^2 + 1", Sugeridor.sugerir("t^2 + 1"));
        verificarVerdadero("Una expresión correcta no necesita sugerencia", Sugeridor.sugerir("x^2") == null);
    }

    // ------------------------------------------------------------------
    // Derivadas e integrales comprobadas NUMÉRICAMENTE
    // (no se compara el texto: se comprueba que el resultado sea correcto)
    // ------------------------------------------------------------------

    private static final double[] PUNTOS = {0.37, 0.81, 1.3, 2.1, -0.6};

    private static void probarDerivadas() {
        seccion("Derivadas comprobadas numéricamente: (f(x+h) - f(x-h))/2h ≈ f'(x)");
        String[] funciones = {"x^2", "sin(x)", "e^x·x", "ln(x)", "sin(x^2)", "(x^2+1)^3", "√x", "x^x", "2^x",
            "log(x)", "sin(2x+1)", "cos(x)^2", "tan(x)", "arcsin(x/3)", "arctan(x)", "x/(x+1)", "e^(-x^2)",
            "ln(sin(x))", "|x|", "x^(1/3)", "sec(x)", "e^sin(x)", "sin(cos(x))", "x^2·ln(x)", "sinh(x)", "cosh(2x)",
            "tanh(x^2)", "sin(x)/cos(x)", "1/cos(x)", "x^-2", "√(x^2+1)", "(x+1)/(x-1)"};
        for (int i = 0; i < funciones.length; i++) {
            EntradaCalculo e = entrada(funciones[i]);
            try {
                ResultadoCalculo r = CALCULADORA.resolver(MetodoCalculo.D_AUTOMATICO, e);
                Graficable f = r.getExpresion();
                Graficable d = r.getExpresionResultado();
                String error = null;
                for (int k = 0; k < PUNTOS.length && error == null; k++) {
                    double t = PUNTOS[k];
                    double h = 1e-6;
                    double numerica = (f.evaluarEn(Variable.X, t + h, 1) - f.evaluarEn(Variable.X, t - h, 1)) / (2 * h);
                    double obtenida = d.evaluarEn(Variable.X, t, 1);
                    if (Double.isNaN(numerica) || Double.isInfinite(numerica)) {
                        continue;
                    }
                    if (Math.abs(numerica - obtenida) > 1e-4 * (1 + Math.abs(numerica))) {
                        error = "en x = " + t + " se esperaba " + numerica + " y dio " + obtenida;
                    }
                }
                if (error == null) {
                    exito("d/dx " + funciones[i] + " = " + d);
                } else {
                    fallo("d/dx " + funciones[i] + " = " + d + " | " + error);
                }
            } catch (CalculadoraException ex) {
                fallo("d/dx " + funciones[i] + " lanzó: " + ex.getMessage());
            }
        }
    }

    private static void probarIntegrales() {
        seccion("Integrales indefinidas comprobadas: la derivada de F debe ser f");
        String[] funciones = {"x^2", "sin(x)", "1/x", "e^x", "x·e^x", "ln(x)", "x·sin(x)", "sin(x)^2", "cos^2(x)",
            "tan(x)", "1/(1+x^2)", "e^(2x)", "x·e^(x^2)", "√x", "1/√x", "sin(x)·cos(x)", "x^2·e^x", "e^x·sin(x)",
            "1/(x^2-1)", "x/(x^2+1)", "sec(x)^2", "sec(x)", "2^x", "x·ln(x)", "arctan(x)", "(2x+3)^10", "1/(x·ln(x))",
            "sin(3x)", "x^3·e^(x^2)", "1/(x^2+2x+5)", "ln(x)/x", "√(1+x^2)", "e^x/(1+e^x)", "tan(x)^2", "x·arctan(x)",
            "sin(x)^3", "cos(x)^4", "sin(x)^5", "1/(e^x+1)", "1/(1+e^(-x))", "e^x/(e^(2x)+1)", "|x|", "|2x-1|",
            "sinh(x)", "cosh(2x)", "tanh(x)", "sin(x)·sin(2x)", "cos(3x)·cos(x)", "sin(2x)·cos(5x)"};
        for (int i = 0; i < funciones.length; i++) {
            EntradaCalculo e = entrada(funciones[i]);
            try {
                ResultadoCalculo r = CALCULADORA.resolver(MetodoCalculo.I_AUTOMATICO, e);
                Graficable f = r.getExpresion();
                Graficable antiderivada = r.getExpresionResultado();
                String error = null;
                int comprobados = 0;
                for (int k = 0; k < PUNTOS.length && error == null; k++) {
                    double t = PUNTOS[k];
                    double h = 1e-5;
                    double valor = f.evaluarEn(Variable.X, t, 1);
                    double derivada = (antiderivada.evaluarEn(Variable.X, t + h, 1)
                            - antiderivada.evaluarEn(Variable.X, t - h, 1)) / (2 * h);
                    if (Double.isNaN(valor) || Double.isNaN(derivada) || Math.abs(valor) > 1e6) {
                        continue;
                    }
                    comprobados++;
                    if (Math.abs(valor - derivada) > 1e-4 * (1 + Math.abs(valor))) {
                        error = "en x = " + t + " f vale " + valor + " pero F' vale " + derivada;
                    }
                }
                if (error == null && comprobados >= 2 && r.getTextoResultado().endsWith("+ C")) {
                    exito("∫ " + funciones[i] + " dx = " + antiderivada + " + C");
                } else {
                    fallo("∫ " + funciones[i] + " dx = " + antiderivada + " | " + (error == null ? "sin puntos válidos" : error));
                }
            } catch (CalculadoraException ex) {
                fallo("∫ " + funciones[i] + " dx lanzó: " + ex.getMessage());
            }
        }
        EntradaCalculo y = entrada("x·sin(y)");
        y.setVariable(Variable.Y);
        verificarTextoResultado("Integral respecto a y", MetodoCalculo.I_AUTOMATICO, y, "∫ x·sin(y) dy = -x·cos(y) + C");
    }

    private static void probarIntegralesDefinidas() {
        seccion("Integrales definidas: valores conocidos");
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "x^2", "0", "1", null, 1.0 / 3);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "sin(x)", "0", "pi", null, 2.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "1/x", "1", "e", null, 1.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "e^x", "0", "1", null, Math.E - 1);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "√x", "0", "4", null, 16.0 / 3);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "|x|", "-1", "1", null, 1.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "sin(x)^3", "0", "pi", null, 4.0 / 3);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "1/(1+x^2)", "0", "1", null, Math.PI / 4);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "1/x", "-e", "-1", null, -1.0);

        seccion("Integrales sin antiderivada elemental: aproximación numérica");
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "e^(-x^2)", "0", "1", null, 0.746824132812427);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "sin(x)/x", "0", "1", null, 0.946083070367183);
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "e^(-x^2)", "-∞", "∞", null, Math.sqrt(Math.PI));
        verificarDefinidaSimbolica(MetodoCalculo.I_AUTOMATICO, "x^x", "0", "1", null, 0.783430510712134);

        seccion("Integrales definidas: forma exacta del resultado y otra variable");
        verificarTextoDefinida("e^x", "0", "1", Variable.X, "∫[0, 1] e^x dx = e - 1 ≈ 1.718282");
        verificarTextoDefinida("x^2", "0", "ln(2)", Variable.X, "∫[0, ln(2)] x^2 dx = ln(2)^3/3 ≈ 0.111008");
        verificarTextoDefinida("x·y", "0", "2", Variable.X, "∫[0, 2] x·y dx = 2y");
        verificarTextoDefinida("x·y^2", "0", "3", Variable.Y, "∫[0, 3] x·y^2 dy = 9x");
        verificarTextoDefinida("sin(x)", "0", "∞", Variable.X, "∫[0, ∞] sin(x) dx = no existe");
    }

    // ------------------------------------------------------------------
    // Aplicaciones: límites, recta tangente, extremos y área entre curvas
    // ------------------------------------------------------------------

    private static void probarAplicaciones() {
        seccion("Límites (incluidos los laterales)");
        verificarLimite(MetodoCalculo.A_LIMITE, "(x^2 - 1)/(x - 1)", "1", "2");
        verificarLimite(MetodoCalculo.A_LIMITE, "1/x", "0+", "∞");
        verificarLimite(MetodoCalculo.A_LIMITE, "1/x", "0-", "-∞");
        verificarLimite(MetodoCalculo.A_LIMITE, "1/x", "0", "no existe");
        verificarLimite(MetodoCalculo.A_LIMITE, "|x|/x", "0+", "1");

        seccion("Recta tangente y normal");
        EntradaCalculo t1 = entrada("x^2 - 2x + 3");
        t1.setPunto("2");
        verificarTextoResultado("Tangente a x^2 - 2x + 3 en x = 2", MetodoCalculo.A_TANGENTE, t1,
                "Tangente en x = 2:  y = 2x - 1\nNormal:  y = 4 - x/2");
        EntradaCalculo t2 = entrada("sin(x)");
        t2.setPunto("pi");
        verificarTextoResultado("Tangente a sin(x) en x = π", MetodoCalculo.A_TANGENTE, t2,
                "Tangente en x = π:  y = π - x\nNormal:  y = x - π");

        seccion("Máximos, mínimos e inflexión");
        verificarExtremos("x^3 - 3x", "-3", "3", "Máximos locales: (-1, 2)\nMínimos locales: (1, -2)\nInflexión: (0, 0)\n"
                + "Máximo absoluto 18 en x = 3;  mínimo absoluto -18 en x = -3");
        verificarExtremos("x^4 - 2x^2", "-2", "2", "Máximos locales: (0, 0)\nMínimos locales: (-1, -1),  (1, -1)\n"
                + "Inflexión: (-√3/3, -5/9),  (√3/3, -5/9)\nMáximo absoluto 8 en x = -2;  mínimo absoluto -1 en x = -1");
        verificarExtremos("|x|", "-1", "2", "Máximos locales: ninguno\nMínimos locales: (0, 0)\nInflexión: ninguno\n"
                + "Máximo absoluto 2 en x = 2;  mínimo absoluto 0 en x = 0");

        seccion("Área entre dos curvas");
        verificarArea("x", "x^2", "", "", 1.0 / 6);
        verificarArea("x", "x^2", "0", "2", 1.0);
        verificarArea("x^3", "x", "", "", 0.5);
        verificarArea("sin(x)", "cos(x)", "", "", 2 * Math.sqrt(2));
        verificarArea("e^(-x^2)", "0", "-1", "1", 1.493648265624854);
    }

    private static void probarErrores() {
        seccion("Manejo de errores (mensajes amables, nunca se cierra la aplicación)");
        verificarExcepcion("Expresión vacía", MetodoCalculo.D_AUTOMATICO, entrada("   "), ExpresionInvalidaException.class);
        verificarExcepcion("Paréntesis sin cerrar", MetodoCalculo.D_AUTOMATICO, entrada("sin(2x"),
                ExpresionInvalidaException.class);
        verificarExcepcion("Símbolo desconocido", MetodoCalculo.D_AUTOMATICO, entrada("3z + 1"),
                ExpresionInvalidaException.class);
        verificarExcepcion("Factorial de x", MetodoCalculo.D_AUTOMATICO, entrada("x!"), ExpresionInvalidaException.class);
        verificarExcepcion("Signo = fuera de la derivación implícita", MetodoCalculo.D_AUTOMATICO, entrada("x^2 = 4"),
                ExpresionInvalidaException.class);
        EntradaCalculo tangente = entrada("ln(x)");
        tangente.setPunto("0");
        verificarExcepcion("Tangente fuera del dominio", MetodoCalculo.A_TANGENTE, tangente, DominioInvalidoException.class);
        EntradaCalculo extremos = entrada("x^2");
        extremos.setLimiteA("");
        extremos.setLimiteB("");
        verificarExcepcion("Extremos sin intervalo", MetodoCalculo.A_EXTREMOS, extremos, ExpresionInvalidaException.class);
        EntradaCalculo area = entrada("x");
        area.setFuncionG("");
        verificarExcepcion("Área sin g(x)", MetodoCalculo.A_AREA_CURVAS, area, ExpresionInvalidaException.class);
        EntradaCalculo limite = entrada("x");
        limite.setPunto("abc");
        verificarExcepcion("Punto del límite inválido", MetodoCalculo.A_LIMITE, limite, ExpresionInvalidaException.class);
    }

    // ------------------------------------------------------------------
    // Etapa 4: JSON e Inteligencia Artificial (sin necesidad de red)
    // ------------------------------------------------------------------

    private static void probarJson() {
        seccion("JsonSencillo");
        String json = JsonSencillo.objeto(
                "texto", JsonSencillo.cadena("Hola \"mundo\"\nñ ∫"),
                "numero", "3.5",
                "lista", JsonSencillo.arreglo("1", "true", "null"));
        verificarTexto("Escribir objeto", "{\"texto\":\"Hola \\\"mundo\\\"\\nñ ∫\",\"numero\":3.5,"
                + "\"lista\":[1,true,null]}", json);
        Object raiz = JsonSencillo.parsear(json);
        verificarTexto("Leer texto con escapes", "Hola \"mundo\"\nñ ∫",
                String.valueOf(JsonSencillo.obtenerRuta(raiz, "texto")));
        verificarTexto("Leer número", "3.5", String.valueOf(JsonSencillo.obtenerRuta(raiz, "numero")));
        verificarTexto("Leer booleano en lista", "true", String.valueOf(JsonSencillo.obtenerRuta(raiz, "lista", 1)));
        verificarVerdadero("Ruta inexistente devuelve null", JsonSencillo.obtenerRuta(raiz, "no", 0) == null);
        Object unicode = JsonSencillo.parsear("{\"a\": \"\\u00e1rea\"}");
        verificarTexto("Leer \\u00e1", "área", String.valueOf(JsonSencillo.obtenerRuta(unicode, "a")));
        try {
            JsonSencillo.parsear("{\"a\": }");
            fallo("JSON mal formado debía fallar");
        } catch (IllegalArgumentException e) {
            exito("JSON mal formado lanza IllegalArgumentException");
        }
    }

    private static void probarModuloIA() {
        seccion("Módulo de IA");
        try {
            String respuestaAnthropic = "{\"id\":\"msg_1\",\"type\":\"message\",\"content\":"
                    + "[{\"type\":\"text\",\"text\":\"Primero se aplica la regla.\"}]}";
            verificarTexto("Extraer texto de Anthropic", "Primero se aplica la regla.",
                    ProveedorAnthropic.extraerTexto(200, respuestaAnthropic));
            String respuestaOpenAI = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hola\"}}]}";
            verificarTexto("Extraer texto de OpenAI/Ollama", "Hola",
                    ProveedorCompatibleOpenAI.extraerTexto(200, respuestaOpenAI));
        } catch (IAException e) {
            fallo("Extracción de texto: " + e.getMessage());
        }
        try {
            ProveedorAnthropic.extraerTexto(401, "{\"type\":\"error\",\"error\":{\"message\":\"invalid x-api-key\"}}");
            fallo("Un código 401 debía lanzar IAException");
        } catch (IAException e) {
            exito("Código 401 -> " + e.getMessage());
        }

        verificarTexto("Limpiar Markdown y LaTeX", "Titulo\nLa derivada de x^2 es 2x",
                TutorIA.limpiarFormato("## Titulo\nLa **derivada** de $x^2$ es `2x`"));
        String largo = "uno dos tres cuatro cinco";
        verificarTexto("Limitar a 3 palabras", "uno dos tres ...", TutorIA.limitarPalabras(largo, 3));

        try {
            EntradaCalculo datos = entrada("3x^2 + 2sin(x) - 4e^(2x)");
            datos.setLimiteA("0");
            datos.setLimiteB("1");
            datos.setDefinida(true);
            ResultadoCalculo r = CALCULADORA.resolver(MetodoCalculo.I_TFC, datos);

            // Sin clave: debe usarse el explicador local
            TutorIA tutorLocal = new TutorIA(new ConfiguracionIA("anthropic", "", "", "", 5));
            verificarVerdadero("Sin clave no hay proveedor remoto", !tutorLocal.tieneProveedorRemoto());
            String explicacion = tutorLocal.explicar(r, "");
            verificarVerdadero("Explicación local no vacía", explicacion.length() > 50);
            verificarVerdadero("Explicación local con máximo 250 palabras ("
                    + TutorIA.contarPalabras(explicacion) + ")", TutorIA.contarPalabras(explicacion) <= 251);
            verificarTexto("Fuente local", "Explicador Local", tutorLocal.getUltimaFuente());

            // Proveedor inalcanzable: debe conmutar automáticamente al explicador local
            TutorIA tutorSinRed = new TutorIA(new ConfiguracionIA("ollama", "", "modelo", "http://127.0.0.1:9/v1", 3));
            String respaldo = tutorSinRed.explicar(r, "¿Por qué no se suma C?");
            verificarVerdadero("Respaldo automático sin red", tutorSinRed.getUltimaFuente().startsWith("Explicador Local (respaldo"));
            verificarVerdadero("Respaldo con contenido", respaldo.contains("Resultado final"));

            // El mensaje para la IA incluye los pasos del motor, no pide calcular
            String mensaje = tutorLocal.construirMensaje(r, "");
            verificarVerdadero("El mensaje incluye el procedimiento", mensaje.contains("Teorema Fundamental"));
            verificarVerdadero("La instrucción prohíbe calcular", tutorLocal.construirInstruccionSistema().contains("NO calcules"));
        } catch (CalculadoraException e) {
            fallo("Módulo IA: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Motor simbólico (árbol de expresión)
    // ------------------------------------------------------------------

    private static void probarMotorSimbolico() {
        seccion("Motor simbólico: derivadas");
        verificarMetodo(MetodoCalculo.D_PRODUCTO, "x^2*sin(x)", "d/dx[x^2·sin(x)] = 2x·sin(x) + x^2·cos(x)");
        verificarMetodo(MetodoCalculo.D_COCIENTE, "sin(x)/x", "d/dx[sin(x)/x] = (x·cos(x) - sin(x))/x^2");
        verificarMetodo(MetodoCalculo.D_CADENA, "sin(x^2 + 1)", "d/dx[sin(x^2 + 1)] = 2x·cos(x^2 + 1)");
        verificarMetodo(MetodoCalculo.D_EXPONENCIAL, "e^(3x)", "d/dx[e^(3x)] = 3e^(3x)");
        verificarMetodo(MetodoCalculo.D_LOGARITMICA, "ln(x^2 + 1)", "d/dx[ln(x^2 + 1)] = 2x/(x^2 + 1)");
        verificarMetodo(MetodoCalculo.D_INVERSAS, "arctan(x^2)", "d/dx[arctan(x^2)] = 2x/(x^4 + 1)");
        verificarMetodo(MetodoCalculo.D_POTENCIA, "x^5 + 1/x^2", "d/dx[x^5 + 1/x^2] = 5x^4 - 2/x^3");
        verificarMetodo(MetodoCalculo.D_AUTOMATICO, "x^x", "d/dx[x^x] = x^x·(ln(x) + 1)");
        verificarMetodo(MetodoCalculo.D_IMPLICITA, "x^2 + y^2 = 25", "dy/dx = -x/y");
        EntradaCalculo orden = entrada("x^4 - 3x^2");
        orden.setOrden("2");
        verificarTextoResultado("Derivada de orden 2", MetodoCalculo.D_ORDEN_SUPERIOR, orden, "f'' = 12x^2 - 6");
        EntradaCalculo parcial = entrada("x^2*y^3");
        parcial.setVariable(Variable.Y);
        verificarTextoResultado("Derivada parcial respecto a y", MetodoCalculo.D_PARCIAL, parcial, "∂/∂y[x^2·y^3] = 3x^2·y^2");

        seccion("Motor simbólico: límites (L'Hôpital)");
        verificarLimite("sin(x)/x", "0", "1");
        verificarLimite("(1 - cos(x))/x^2", "0", "1/2");
        verificarLimite("(3x^2 + 1)/(x^2 - 2)", "∞", "3");
        verificarLimite("e^x/x^2", "∞", "∞");
        verificarLimite("x*ln(x)", "0", "0");
        verificarLimite("ln(x)/x", "-∞", "no existe");

        seccion("Motor simbólico: métodos de integración");
        verificarMetodo(MetodoCalculo.I_SUSTITUCION, "2x*cos(x^2)", "∫ 2x·cos(x^2) dx = sin(x^2) + C");
        verificarMetodo(MetodoCalculo.I_PARTES, "x*e^x", "∫ x·e^x dx = x·e^x - e^x + C");
        verificarMetodo(MetodoCalculo.I_PARTES, "ln(x)", "∫ ln(x) dx = x·ln(x) - x + C");
        verificarMetodo(MetodoCalculo.I_FRACCIONES, "1/(x^2 - 1)", "∫ 1/(x^2 - 1) dx = ln|x - 1|/2 - ln|x + 1|/2 + C");
        verificarMetodo(MetodoCalculo.I_SUSTITUCION_TRIG, "1/√(4 - x^2)", "∫ 1/√(4 - x^2) dx = arcsin(x/2) + C");
        verificarMetodo(MetodoCalculo.I_AUTOMATICO, "e^x*sin(x)", "∫ e^x·sin(x) dx = (sin(x)·e^x - cos(x)·e^x)/2 + C");
        verificarMetodo(MetodoCalculo.I_AUTOMATICO, "x^3/(x^2 + 1)", "∫ x^3/(x^2 + 1) dx = x^2/2 - ln(x^2 + 1)/2 + C");
        verificarMetodo(MetodoCalculo.I_AUTOMATICO, "sin(x)^2", "∫ sin(x)^2 dx = x/2 - sin(2x)/4 + C");
        try {
            CALCULADORA.resolver(MetodoCalculo.I_PARTES, entrada("x^2 + 1"));
            fallo("Partes sobre x^2 + 1 debía avisar que el método no aplica");
        } catch (CalculadoraException e) {
            verificarTipoExcepcion("Partes no aplicable a x^2 + 1", OperacionNoSoportadaException.class, e);
        }
        try {
            CALCULADORA.resolver(MetodoCalculo.I_AUTOMATICO, entrada("e^(x^2)"));
            fallo("e^(x^2) no tiene antiderivada elemental");
        } catch (CalculadoraException e) {
            verificarTipoExcepcion("e^(x^2) sin antiderivada elemental", OperacionNoSoportadaException.class, e);
        }

        seccion("Motor simbólico: integrales definidas, propiedades e impropias");
        verificarDefinidaSimbolica(MetodoCalculo.I_TFC, "x^2", "0", "3", null, 9.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_INVERSION, "x^2", "3", "0", null, -9.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_LIMITES_IGUALES, "x^3 + 1", "2", "2", null, 0.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_ADITIVIDAD, "x^2", "0", "3", "1", 9.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_IMPROPIA, "1/x^2", "1", "∞", null, 1.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_IMPROPIA, "e^(-x)", "0", "inf", null, 1.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_IMPROPIA, "1/(1 + x^2)", "-∞", "∞", null, Math.PI);
        verificarDefinidaSimbolica(MetodoCalculo.I_IMPROPIA, "1/√(x)", "0", "1", null, 2.0);
        verificarDefinidaSimbolica(MetodoCalculo.I_IMPROPIA, "ln(x)", "0", "1", null, -1.0);
        verificarDivergente("1/x", "1", "∞");
        verificarDivergente("1/x^2", "-1", "1");
    }

    private static void probarListaDeMetodos() {
        seccion("Lista de métodos: todos los ejemplos se resuelven");
        MetodoCalculo[] todos = MetodoCalculo.values();
        for (int i = 0; i < todos.length; i++) {
            MetodoCalculo m = todos[i];
            EntradaCalculo e = entrada(m.getEjemplo());
            e.setLimiteA(m.ejemploA());
            e.setLimiteB(m.ejemploB());
            e.setPuntoC("1");
            e.setOrden("3");
            e.setPunto(m.ejemploPunto());
            e.setFuncionG(m.ejemploG());
            e.setDefinida(m.esDefinidaObligatoria());
            if (m == MetodoCalculo.D_PARCIAL) {
                e.setVariable(Variable.Y);
            }
            try {
                ResultadoCalculo r = CALCULADORA.resolver(m, e);
                boolean ok = r.getTextoResultado() != null && !r.getPasosDetallados().isEmpty() && r.getNotas().isEmpty();
                verificarVerdadero("Ejemplo de " + m.getNombre(), ok);
            } catch (CalculadoraException ex) {
                fallo("Ejemplo de " + m.getNombre() + " lanzó: " + ex.getMessage());
            }
        }
    }

    private static EntradaCalculo entrada(String expresion) {
        EntradaCalculo e = new EntradaCalculo();
        e.setExpresion(expresion);
        return e;
    }

    private static void verificarMetodo(MetodoCalculo metodo, String expresion, String esperado) {
        verificarTextoResultado(metodo.getNombre() + ": " + expresion, metodo, entrada(expresion), esperado);
    }

    private static void verificarTextoResultado(String nombre, MetodoCalculo metodo, EntradaCalculo e, String esperado) {
        try {
            verificarTexto(nombre, esperado, CALCULADORA.resolver(metodo, e).getTextoResultado());
        } catch (CalculadoraException ex) {
            fallo(nombre + " lanzó: " + ex.getMessage());
        }
    }

    private static void verificarLimite(String expresion, String punto, String esperado) {
        verificarLimite(MetodoCalculo.D_LHOPITAL, expresion, punto, esperado);
    }

    private static void verificarLimite(MetodoCalculo metodo, String expresion, String punto, String esperado) {
        EntradaCalculo e = entrada(expresion);
        e.setPunto(punto);
        try {
            String texto = CALCULADORA.resolver(metodo, e).getTextoResultado();
            String obtenido = texto.substring(texto.lastIndexOf("= ") + 2);
            verificarTexto("lim x→" + punto + " de " + expresion, esperado, obtenido);
        } catch (CalculadoraException ex) {
            fallo("Límite de " + expresion + " lanzó: " + ex.getMessage());
        }
    }

    private static void verificarDefinidaSimbolica(MetodoCalculo metodo, String expresion, String a, String b, String c,
                                                   double esperado) {
        EntradaCalculo e = entrada(expresion);
        e.setLimiteA(a);
        e.setLimiteB(b);
        e.setDefinida(true);
        if (c != null) {
            e.setPuntoC(c);
        }
        String nombre = metodo.getNombre() + ": ∫[" + a + ", " + b + "] " + expresion;
        try {
            ResultadoCalculo r = CALCULADORA.resolver(metodo, e);
            if (r.getValorNumerico() == null) {
                fallo(nombre + " no devolvió valor: " + r.getTextoResultado());
            } else {
                verificarNumero(nombre, esperado, r.getValorNumerico().doubleValue());
            }
        } catch (CalculadoraException ex) {
            fallo(nombre + " lanzó: " + ex.getMessage());
        }
    }

    private static void verificarDivergente(String expresion, String a, String b) {
        EntradaCalculo e = entrada(expresion);
        e.setLimiteA(a);
        e.setLimiteB(b);
        e.setDefinida(true);
        try {
            ResultadoCalculo r = CALCULADORA.resolver(MetodoCalculo.I_IMPROPIA, e);
            verificarVerdadero("∫[" + a + ", " + b + "] " + expresion + " diverge", r.getTextoResultado().endsWith("diverge"));
        } catch (CalculadoraException ex) {
            fallo("Divergencia de " + expresion + " lanzó: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Verificaciones específicas de la calculadora
    // ------------------------------------------------------------------

    private static void verificarInterpretacion(String entrada, String esperado) {
        try {
            verificarTexto("Interpretar \"" + entrada + "\"", esperado, CALCULADORA.interpretar(entrada).toString());
        } catch (CalculadoraException ex) {
            fallo("Interpretar \"" + entrada + "\" lanzó: " + ex.getMessage());
        }
    }

    private static void verificarAviso(String entrada, boolean esperaAviso) {
        AnalizadorSimbolico analizador = new AnalizadorSimbolico();
        try {
            analizador.analizar(entrada);
            List<String> avisos = analizador.getAvisos();
            String nombre = "\"" + entrada + "\" " + (esperaAviso ? "avisa" : "no avisa")
                    + (avisos.isEmpty() ? "" : ": " + avisos.get(0));
            verificarVerdadero(nombre, avisos.isEmpty() != esperaAviso);
        } catch (CalculadoraException ex) {
            fallo("Aviso de \"" + entrada + "\" lanzó: " + ex.getMessage());
        }
    }

    private static void verificarTextoDefinida(String expresion, String a, String b, Variable v, String esperado) {
        EntradaCalculo e = entrada(expresion);
        e.setLimiteA(a);
        e.setLimiteB(b);
        e.setDefinida(true);
        e.setVariable(v);
        verificarTextoResultado("∫[" + a + ", " + b + "] " + expresion + " d" + v, MetodoCalculo.I_AUTOMATICO, e, esperado);
    }

    private static void verificarExtremos(String expresion, String a, String b, String esperado) {
        EntradaCalculo e = entrada(expresion);
        e.setLimiteA(a);
        e.setLimiteB(b);
        verificarTextoResultado("Extremos de " + expresion + " en [" + a + ", " + b + "]", MetodoCalculo.A_EXTREMOS, e,
                esperado);
    }

    private static void verificarArea(String f, String g, String a, String b, double esperado) {
        EntradaCalculo e = entrada(f);
        e.setFuncionG(g);
        e.setLimiteA(a);
        e.setLimiteB(b);
        String nombre = "Área entre " + f + " y " + g + (a.isEmpty() ? " (puntos de corte)" : " en [" + a + ", " + b + "]");
        try {
            ResultadoCalculo r = CALCULADORA.resolver(MetodoCalculo.A_AREA_CURVAS, e);
            verificarNumero(nombre, esperado, r.getValorNumerico().doubleValue());
        } catch (CalculadoraException ex) {
            fallo(nombre + " lanzó: " + ex.getMessage());
        }
    }

    private static void verificarExcepcion(String nombre, MetodoCalculo metodo, EntradaCalculo e, Class<?> tipoEsperado) {
        try {
            CALCULADORA.resolver(metodo, e);
            fallo(nombre + " | se esperaba " + tipoEsperado.getSimpleName());
        } catch (CalculadoraException ex) {
            verificarTipoExcepcion(nombre, tipoEsperado, ex);
        }
    }

    private static void verificarTipoExcepcion(String nombre, Class<?> tipoEsperado, Exception ex) {
        if (tipoEsperado.isInstance(ex)) {
            exito(nombre + " -> " + ex.getMessage());
        } else {
            fallo(nombre + " | se esperaba " + tipoEsperado.getSimpleName() + " pero fue "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Mini marco de pruebas
    // ------------------------------------------------------------------

    private static void seccion(String nombre) {
        System.out.println();
        System.out.println(">> " + nombre);
    }

    private static void verificarTexto(String nombre, String esperado, String obtenido) {
        if (esperado.equals(obtenido)) {
            exito(nombre);
        } else {
            fallo(nombre + " | esperado: [" + esperado + "] obtenido: [" + obtenido + "]");
        }
    }

    private static void verificarNumero(String nombre, double esperado, double obtenido) {
        if (Math.abs(esperado - obtenido) < 1e-6) {
            exito(nombre);
        } else {
            fallo(nombre + " | esperado: " + esperado + " obtenido: " + obtenido);
        }
    }

    private static void verificarVerdadero(String nombre, boolean condicion) {
        if (condicion) {
            exito(nombre);
        } else {
            fallo(nombre + " | la condición es falsa");
        }
    }

    private static void exito(String nombre) {
        pruebasExitosas++;
        System.out.println("  [OK]    " + nombre);
    }

    private static void fallo(String detalle) {
        pruebasFallidas++;
        System.out.println("  [FALLA] " + detalle);
    }
}
