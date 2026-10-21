package calculadora.ia;

import calculadora.modelo.ResultadoCalculo;

import java.util.List;

/**
 * Explicador que funciona SIN Internet y SIN clave de API.
 * <p>
 * Redacta una explicación didáctica a partir de los pasos que el motor
 * Java registró en el {@link ResultadoCalculo}. Es el respaldo automático
 * del {@link TutorIA} cuando no hay red, no hay clave o el proveedor falla.
 * </p>
 */
public class ExplicadorLocal {

    /**
     * Genera la explicación en texto plano (máximo 250 palabras).
     *
     * @param resultado resultado calculado por el motor
     * @param pregunta  pregunta opcional del estudiante (puede ser null o vacía)
     * @return explicación en texto plano
     */
    public String explicar(ResultadoCalculo resultado, String pregunta) {
        // 1) Introducción y cierre: se arman primero para reservar su espacio
        String introduccion = introduccion(resultado);

        StringBuilder cierre = new StringBuilder();
        cierre.append("Resultado final: ").append(resultado.getTextoResultado()).append("\n\n");
        cierre.append("Consejo: ").append(consejo(resultado));
        if (pregunta != null && !pregunta.trim().isEmpty()) {
            cierre.append("\n\nSobre tu pregunta: el explicador local no puede responder preguntas abiertas. "
                    + "Configura un proveedor de IA en config.properties para obtener una respuesta personalizada.");
        }

        // 2) Presupuesto de palabras disponible para los pasos
        int presupuesto = TutorIA.MAXIMO_PALABRAS - TutorIA.contarPalabras(introduccion)
                - TutorIA.contarPalabras(cierre.toString()) - 12;

        // 3) Resumen de cada paso mientras quepa en el presupuesto
        StringBuilder pasosTexto = new StringBuilder();
        List<String> pasos = resultado.getPasos();
        int numero = 1;
        int palabrasUsadas = 0;
        boolean seOmitieronPasos = false;
        for (int i = 0; i < pasos.size(); i++) {
            String paso = pasos.get(i);
            if (paso.startsWith("Expresión interpretada")) {
                continue; // ya se menciona en la introducción
            }
            String linea = "Paso " + numero + ": " + resumirPaso(paso);
            int palabrasLinea = TutorIA.contarPalabras(linea);
            if (palabrasUsadas + palabrasLinea > presupuesto) {
                seOmitieronPasos = true;
                break;
            }
            pasosTexto.append(linea).append('\n');
            palabrasUsadas = palabrasUsadas + palabrasLinea;
            numero++;
        }
        if (seOmitieronPasos) {
            pasosTexto.append("(El detalle completo está en la pestaña Resultado y pasos.)\n");
        }

        // 4) Unir todo
        StringBuilder texto = new StringBuilder();
        texto.append(introduccion).append("\n\n");
        texto.append(pasosTexto).append('\n');
        texto.append(cierre);
        return TutorIA.limitarPalabras(texto.toString(), TutorIA.MAXIMO_PALABRAS);
    }

    private String introduccion(ResultadoCalculo resultado) {
        String variable = "";
        if (resultado.getVariable() != null) {
            variable = resultado.getVariable().getSimbolo();
        }
        ResultadoCalculo.TipoOperacion tipo = resultado.getTipo();
        if (tipo == ResultadoCalculo.TipoOperacion.DERIVADA) {
            return "Vamos a derivar f = " + resultado.getExpresion() + " respecto a " + variable
                    + ". Derivar mide la rapidez de cambio de la función; se aplica una regla a cada término.";
        }
        if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_INDEFINIDA) {
            return "Vamos a integrar f = " + resultado.getExpresion() + " respecto a " + variable
                    + ". Integrar es buscar una función cuya derivada sea f (una antiderivada).";
        }
        if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA) {
            return "Vamos a calcular la integral definida de f = " + resultado.getExpresion()
                    + ". Representa el área neta bajo la curva entre los límites, y se obtiene con el "
                    + "Teorema Fundamental del Cálculo.";
        }
        return "Vamos a evaluar f = " + resultado.getExpresion() + " sustituyendo los valores indicados.";
    }

    /**
     * Toma un paso largo del motor y conserva lo esencial: el nombre de la
     * regla (texto antes de ':') y el resultado parcial (texto después de "Resultado:").
     */
    private String resumirPaso(String paso) {
        String nombreRegla = paso;

        // Si el paso es de un término, quitar el encabezado "Término n [..]: "
        int finEncabezado = paso.indexOf("]: ");
        if (paso.startsWith("Término") && finEncabezado > 0) {
            String encabezado = paso.substring(0, finEncabezado + 1);
            String cuerpo = paso.substring(finEncabezado + 3);
            int dosPuntos = cuerpo.indexOf(':');
            String regla = cuerpo;
            if (dosPuntos > 0) {
                regla = cuerpo.substring(0, dosPuntos);
            }
            int indiceResultado = cuerpo.indexOf("Resultado: ");
            String parcial = "";
            if (indiceResultado >= 0) {
                parcial = cuerpo.substring(indiceResultado + "Resultado: ".length());
                int punto = parcial.indexOf(". ");
                if (punto > 0) {
                    parcial = parcial.substring(0, punto);
                }
                if (parcial.endsWith(".")) {
                    parcial = parcial.substring(0, parcial.length() - 1);
                }
                return encabezado + " se aplica " + regla + " y se obtiene " + parcial + ".";
            }
            return encabezado + " " + regla + ".";
        }

        // Pasos generales: se abrevian y se acortan si son muy largos
        nombreRegla = nombreRegla.replace("Teorema Fundamental del Cálculo, paso", "TFC, paso");
        nombreRegla = nombreRegla.replace(" En la integral definida no se agrega + C porque se cancela al restar.", "");
        if (nombreRegla.length() > 170) {
            int punto = nombreRegla.indexOf(". ");
            if (punto > 0 && punto < 170) {
                nombreRegla = nombreRegla.substring(0, punto + 1);
            } else {
                nombreRegla = nombreRegla.substring(0, 167) + "...";
            }
        }
        return nombreRegla;
    }

    private String consejo(ResultadoCalculo resultado) {
        ResultadoCalculo.TipoOperacion tipo = resultado.getTipo();
        if (tipo == ResultadoCalculo.TipoOperacion.DERIVADA) {
            return "verifica cada término por separado y recuerda que la otra variable se comporta como un número.";
        }
        if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_INDEFINIDA) {
            return "comprueba tu resultado derivándolo: debes obtener la función original. No olvides la constante C.";
        }
        if (tipo == ResultadoCalculo.TipoOperacion.INTEGRAL_DEFINIDA) {
            return "observa el área sombreada en la pestaña Gráfica 2D; las zonas bajo el eje restan al resultado.";
        }
        return "revisa los signos al sustituir valores negativos.";
    }
}
