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
import calculadora.modelo.Expresion;
import calculadora.modelo.ResultadoCalculo;
import calculadora.modelo.Termino;
import calculadora.modelo.TerminoLogaritmico;
import calculadora.modelo.TerminoPolinomico;
import calculadora.modelo.Variable;
import calculadora.util.FormatoNumeros;

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
        probarTerminosPolinomicos();
        probarAnalizador();
        probarDerivadas();
        probarIntegrales();
        probarIntegralesDefinidas();
        probarErrores();
        probarJson();
        probarModuloIA();

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
    }

    private static void probarTerminosPolinomicos() {
        seccion("Términos polinómicos");
        try {
            Termino t = new TerminoPolinomico(3.0, 2.0, 0.0);
            verificarTexto("Texto 3x^2", "3x^2", t.toString());
            verificarTexto("d/dx 3x^2", "6x", t.derivar(Variable.X).toString());
            verificarTexto("∫ 3x^2 dx", "x^3", t.integrar(Variable.X).toString());

            Termino constante = TerminoPolinomico.constante(5.0);
            verificarVerdadero("d/dx 5 = 0", constante.derivar(Variable.X).esCero());
            verificarTexto("∫ 5 dx", "5x", constante.integrar(Variable.X).toString());

            Termino xCuadradaY = new TerminoPolinomico(1.0, 2.0, 1.0);
            verificarTexto("d/dx x^2y", "2xy", xCuadradaY.derivar(Variable.X).toString());
            verificarTexto("d/dy x^2y", "x^2", xCuadradaY.derivar(Variable.Y).toString());
            verificarTexto("∫ x^2y dy", "(1/2)x^2y^2", xCuadradaY.integrar(Variable.Y).toString());

            Termino inversa = new TerminoPolinomico(2.0, -1.0, 0.0);
            Termino integralInversa = inversa.integrar(Variable.X);
            verificarTexto("∫ 2/x dx", "2ln|x|", integralInversa.toString());
            verificarVerdadero("∫ 2/x dx es logarítmico", integralInversa instanceof TerminoLogaritmico);

            Expresion e = new Expresion();
            e.agregar(new TerminoPolinomico(3.0, 2.0, 0.0));
            e.agregar(new TerminoPolinomico(-2.0, 1.0, 0.0));
            e.agregar(new TerminoPolinomico(-1.0, 2.0, 0.0));
            e.agregar(TerminoPolinomico.constante(1.0));
            verificarTexto("Simplificar 3x^2 - 2x - x^2 + 1", "2x^2 - 2x + 1", e.simplificar().toString());
            verificarTexto("d/dx (3x^2 - 2x - x^2 + 1)", "4x - 2", e.derivar(Variable.X).toString());
            verificarNumero("Evaluar 2x^2 - 2x + 1 en x = 2", 5.0, e.evaluar(2.0, 0.0));
        } catch (CalculadoraException ex) {
            fallo("Excepción inesperada en términos polinómicos: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Etapa 2: analizador y fachada
    // ------------------------------------------------------------------

    private static final Calculadora CALCULADORA = new Calculadora();

    private static void probarAnalizador() {
        seccion("Analizador de expresiones");
        verificarInterpretacion("3x^2 - 2x + 1", "3x^2 - 2x + 1");
        verificarInterpretacion("x^2*y", "x^2y");
        verificarInterpretacion("2 x y + x y", "3xy");
        verificarInterpretacion("1/x", "x^(-1)");
        verificarInterpretacion("x^(1/2)", "x^(1/2)");
        verificarInterpretacion("3/4x^2", "(3/4)x^2");
        verificarInterpretacion("-x^2", "-x^2");
        verificarInterpretacion("(2x)^2", "4x^2");
        verificarInterpretacion("2sin(3x)", "2sin(3x)");
        verificarInterpretacion("sen(x) + cos(2y)", "sin(x) + cos(2y)");
        verificarInterpretacion("e^(2x)", "e^(2x)");
        verificarInterpretacion("e^x", "e^x");
        verificarInterpretacion("e^-x", "e^(-x)");
        verificarInterpretacion("exp(0.5x)", "e^((1/2)x)");
        verificarInterpretacion("4ln(x)", "4ln(x)");
        verificarInterpretacion("y*cos(x)", "ycos(x)");
        verificarInterpretacion("x - x", "0");
        verificarInterpretacion("2·x × 3", "6x");
    }

    private static void probarDerivadas() {
        seccion("Derivadas (polinomios y funciones trascendentales)");
        verificarDerivada("3x^2 - 2x + 1", Variable.X, "6x - 2");
        verificarDerivada("5", Variable.X, "0");
        verificarDerivada("x^2y", Variable.X, "2xy");
        verificarDerivada("x^2y", Variable.Y, "x^2");
        verificarDerivada("2sin(3x)", Variable.X, "6cos(3x)");
        verificarDerivada("cos(2x)", Variable.X, "-2sin(2x)");
        verificarDerivada("3e^(2x)", Variable.X, "6e^(2x)");
        verificarDerivada("4ln(x)", Variable.X, "4x^(-1)");
        verificarDerivada("y*sin(x)", Variable.X, "ycos(x)");
        verificarDerivada("y*sin(x)", Variable.Y, "sin(x)");
        verificarDerivada("1/x", Variable.X, "-x^(-2)");
        verificarDerivada("x^(1/2)", Variable.X, "(1/2)x^(-1/2)");
    }

    private static void probarIntegrales() {
        seccion("Integrales indefinidas");
        verificarIntegral("3x^2", Variable.X, "x^3");
        verificarIntegral("6x^2 + 2x + 1", Variable.X, "2x^3 + x^2 + x");
        verificarIntegral("1/x", Variable.X, "ln|x|");
        verificarIntegral("5x^(-1)", Variable.X, "5ln|x|");
        verificarIntegral("sin(2x)", Variable.X, "-(1/2)cos(2x)");
        verificarIntegral("3cos(3x)", Variable.X, "sin(3x)");
        verificarIntegral("e^(2x)", Variable.X, "(1/2)e^(2x)");
        verificarIntegral("x^2y", Variable.Y, "(1/2)x^2y^2");
        verificarIntegral("y*sin(x)", Variable.Y, "(1/2)y^2sin(x)");
        verificarIntegral("y*sin(x)", Variable.X, "-ycos(x)");
        try {
            ResultadoCalculo r = CALCULADORA.integrarIndefinida("2x", Variable.X);
            verificarVerdadero("La integral indefinida termina en + C", r.getTextoResultado().endsWith("+ C"));
        } catch (CalculadoraException e) {
            fallo("Constante de integración: " + e.getMessage());
        }
    }

    private static void probarIntegralesDefinidas() {
        seccion("Integrales definidas (Teorema Fundamental del Cálculo)");
        verificarDefinida("x^2", Variable.X, 0, 3, 0, 9.0);
        verificarDefinida("3x^2 + 1", Variable.X, 1, 2, 0, 8.0);
        verificarDefinida("sin(x)", Variable.X, 0, Math.PI, 0, 2.0);
        verificarDefinida("e^x", Variable.X, 0, 1, 0, Math.E - 1.0);
        verificarDefinida("1/x", Variable.X, 1, Math.E, 0, 1.0);
        verificarDefinida("x*y", Variable.X, 0, 2, 3, 6.0);
        verificarDefinida("x", Variable.X, 2, 0, 0, -2.0);
    }

    private static void probarErrores() {
        seccion("Manejo de errores");
        verificarExcepcionDefinida("1/x en [-1, 1] (impropia)", "1/x", -1, 1, DominioInvalidoException.class);
        verificarExcepcionDefinida("x^(1/2) en [-4, 0] (no real)", "x^(1/2)", -4, 0, DominioInvalidoException.class);
        verificarExcepcionIntegral("∫ ln(x) dx (por partes)", "ln(x)", OperacionNoSoportadaException.class);
        verificarExcepcionIntegral("∫ x·sin(x) dx (por partes)", "x*sin(x)", OperacionNoSoportadaException.class);
        verificarExcepcionIntegral("Expresión vacía", "   ", ExpresionInvalidaException.class);
        verificarExcepcionIntegral("Paréntesis sin cerrar", "sin(2x", ExpresionInvalidaException.class);
        verificarExcepcionIntegral("Símbolo desconocido", "3z + 1", ExpresionInvalidaException.class);
        verificarExcepcionIntegral("Producto de funciones", "sin(x)cos(x)", ExpresionInvalidaException.class);
        verificarExcepcionIntegral("Argumento no lineal", "sin(x^2)", ExpresionInvalidaException.class);
        try {
            CALCULADORA.derivar("x*e^x", Variable.X);
            fallo("d/dx x·e^x debía pedir la regla del producto");
        } catch (OperacionNoSoportadaException e) {
            exito("d/dx x·e^x pide la regla del producto");
        } catch (CalculadoraException e) {
            fallo("Excepción incorrecta: " + e);
        }
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
            ResultadoCalculo r = CALCULADORA.integrarDefinida("3x^2 + 2sin(x) - 4e^(2x)", Variable.X, 0, 1, 0);

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
    // Verificaciones específicas de la calculadora
    // ------------------------------------------------------------------

    private static void verificarInterpretacion(String entrada, String esperado) {
        try {
            Expresion e = CALCULADORA.interpretar(entrada);
            verificarTexto("Interpretar \"" + entrada + "\"", esperado, e.toString());
        } catch (CalculadoraException ex) {
            fallo("Interpretar \"" + entrada + "\" lanzó: " + ex.getMessage());
        }
    }

    private static void verificarDerivada(String entrada, Variable v, String esperado) {
        try {
            ResultadoCalculo r = CALCULADORA.derivar(entrada, v);
            verificarTexto("d/d" + v + " [" + entrada + "]", esperado, r.getExpresionResultado().toString());
        } catch (CalculadoraException ex) {
            fallo("d/d" + v + " [" + entrada + "] lanzó: " + ex.getMessage());
        }
    }

    private static void verificarIntegral(String entrada, Variable v, String esperado) {
        try {
            ResultadoCalculo r = CALCULADORA.integrarIndefinida(entrada, v);
            verificarTexto("∫ [" + entrada + "] d" + v, esperado, r.getExpresionResultado().toString());
        } catch (CalculadoraException ex) {
            fallo("∫ [" + entrada + "] d" + v + " lanzó: " + ex.getMessage());
        }
    }

    private static void verificarDefinida(String entrada, Variable v, double a, double b, double otra,
                                          double esperado) {
        String nombre = "∫ [" + entrada + "] d" + v + " de " + FormatoNumeros.formatear(a) + " a "
                + FormatoNumeros.formatear(b);
        try {
            ResultadoCalculo r = CALCULADORA.integrarDefinida(entrada, v, a, b, otra);
            verificarNumero(nombre, esperado, r.getValorNumerico().doubleValue());
        } catch (CalculadoraException ex) {
            fallo(nombre + " lanzó: " + ex.getMessage());
        }
    }

    private static void verificarExcepcionDefinida(String nombre, String entrada, double a, double b,
                                                   Class<?> tipoEsperado) {
        try {
            CALCULADORA.integrarDefinida(entrada, Variable.X, a, b, 0);
            fallo(nombre + " | se esperaba " + tipoEsperado.getSimpleName());
        } catch (CalculadoraException ex) {
            verificarTipoExcepcion(nombre, tipoEsperado, ex);
        }
    }

    private static void verificarExcepcionIntegral(String nombre, String entrada, Class<?> tipoEsperado) {
        try {
            CALCULADORA.integrarIndefinida(entrada, Variable.X);
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
