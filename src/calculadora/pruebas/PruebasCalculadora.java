package calculadora.pruebas;

import calculadora.excepciones.CalculadoraException;
import calculadora.modelo.Expresion;
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
