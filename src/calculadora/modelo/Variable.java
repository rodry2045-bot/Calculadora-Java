package calculadora.modelo;

/**
 * Variables independientes que reconoce la calculadora.
 * <p>
 * Al derivar o integrar se elige una "variable de trabajo" v. La otra
 * variable se trata como una CONSTANTE (derivación e integración parcial).
 * </p>
 */
public enum Variable {

    /** Variable x. */
    X("x"),

    /** Variable y. */
    Y("y");

    /** Símbolo en minúscula que se muestra al usuario. */
    private final String simbolo;

    /**
     * @param simbolo texto que representa a la variable
     */
    Variable(String simbolo) {
        this.simbolo = simbolo;
    }

    /**
     * @return el símbolo de la variable ("x" o "y")
     */
    public String getSimbolo() {
        return simbolo;
    }

    /**
     * Devuelve la otra variable: si esta es X devuelve Y, y viceversa.
     *
     * @return la variable distinta a esta
     */
    public Variable otra() {
        if (this == X) {
            return Y;
        }
        return X;
    }

    /**
     * Convierte un carácter en Variable.
     *
     * @param caracter 'x' o 'y' (mayúscula o minúscula)
     * @return la variable correspondiente o null si no es una variable
     */
    public static Variable desdeCaracter(char caracter) {
        char minuscula = Character.toLowerCase(caracter);
        if (minuscula == 'x') {
            return X;
        }
        if (minuscula == 'y') {
            return Y;
        }
        return null;
    }

    @Override
    public String toString() {
        return simbolo;
    }
}
