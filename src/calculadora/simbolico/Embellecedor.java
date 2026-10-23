package calculadora.simbolico;

import java.util.ArrayList;
import java.util.List;

/**
 * Último retoque de un resultado para que se lea como en un libro:
 * <ul>
 *   <li>cos(u)/sin(u) → cot(u) y sin(u)/cos(u) → tan(u);</li>
 *   <li>1/cos(u) → sec(u) y 1/sin(u) → csc(u).</li>
 * </ul>
 * Solo cambia la forma de escribir, nunca el valor. Se aplica al final,
 * cuando el motor ya no necesita reconocer patrones en la expresión.
 */
public final class Embellecedor {

    private Embellecedor() {
    }

    /**
     * @param n expresión simplificada
     * @return expresión equivalente, más fácil de leer
     */
    public static Nodo embellecer(Nodo n) {
        Nodo resultado = recorrer(n);
        // Solo se acepta si sigue valiendo lo mismo (comprobación en varios puntos)
        double[] puntos = {0.37, 1.21, -0.83, 2.6};
        for (int i = 0; i < puntos.length; i++) {
            double antes = n.evaluar(puntos[i], 1.3);
            double despues = resultado.evaluar(puntos[i], 1.3);
            boolean ambosReales = !Double.isNaN(antes) && !Double.isNaN(despues);
            if (ambosReales && Math.abs(antes - despues) > 1e-7 * (1 + Math.abs(antes))) {
                return n;
            }
        }
        return resultado;
    }

    private static Nodo recorrer(Nodo n) {
        if (n instanceof NodoCociente) {
            NodoCociente c = (NodoCociente) n;
            Nodo numerador = recorrer(c.getNumerador());
            Nodo denominador = recorrer(c.getDenominador());
            Nodo cambiado = cocienteTrigonometrico(numerador, denominador);
            if (cambiado != null) {
                return cambiado;
            }
            return Nodo.div(numerador, denominador);
        }
        if (n instanceof NodoSuma) {
            Nodo izquierdo = recorrer(((NodoSuma) n).getIzquierdo());
            Nodo derecho = recorrer(((NodoSuma) n).getDerecho());
            if (esPitagorica(izquierdo, derecho)) {
                return Nodo.num(1);
            }
            return Nodo.suma(izquierdo, derecho);
        }
        if (n instanceof NodoResta) {
            return Nodo.resta(recorrer(((NodoResta) n).getIzquierdo()), recorrer(((NodoResta) n).getDerecho()));
        }
        if (n instanceof NodoProducto) {
            Nodo izquierdo = recorrer(((NodoProducto) n).getIzquierdo());
            Nodo derecho = recorrer(((NodoProducto) n).getDerecho());
            if (izquierdo.esNumero(-1.0) && derecho instanceof NodoResta) {
                return negar(derecho);
            }
            return Nodo.mul(izquierdo, derecho);
        }
        if (n instanceof NodoNegativo) {
            return negar(recorrer(((NodoNegativo) n).getArgumento()));
        }
        if (n instanceof NodoPotencia) {
            return Nodo.pot(recorrer(((NodoPotencia) n).getBase()), ((NodoPotencia) n).getExponente());
        }
        if (n instanceof NodoFuncion) {
            return Nodo.fn(((NodoFuncion) n).getTipo(), recorrer(((NodoFuncion) n).getArgumento()));
        }
        return n;
    }

    /** Cambia el signo: -(-a) = a,  -(a - b) = b - a,  -(-a - b) = a + b. */
    private static Nodo negar(Nodo interior) {
        Nodo positivo = sinSigno(interior);
        if (positivo != null) {
            return positivo;
        }
        if (interior instanceof NodoResta) {
            Nodo izquierdo = ((NodoResta) interior).getIzquierdo();
            Nodo derecho = ((NodoResta) interior).getDerecho();
            Nodo izquierdoPositivo = sinSigno(izquierdo);
            if (izquierdoPositivo != null) {
                return Nodo.suma(izquierdoPositivo, derecho);
            }
            return Nodo.resta(derecho, izquierdo);
        }
        return Nodo.neg(interior);
    }

    /** @return a si n es -a (o -1·a); null si n no es negativo */
    private static Nodo sinSigno(Nodo n) {
        if (n instanceof NodoNegativo) {
            return ((NodoNegativo) n).getArgumento();
        }
        if (n instanceof NodoProducto && ((NodoProducto) n).getIzquierdo().esNumero(-1.0)) {
            return ((NodoProducto) n).getDerecho();
        }
        return null;
    }

    /** @return true si a + b es sin(u)² + cos(u)² (identidad pitagórica) */
    private static boolean esPitagorica(Nodo a, Nodo b) {
        Nodo baseA = baseCuadrado(a);
        Nodo baseB = baseCuadrado(b);
        if (!(baseA instanceof NodoFuncion) || !(baseB instanceof NodoFuncion)) {
            return false;
        }
        NodoFuncion fa = (NodoFuncion) baseA;
        NodoFuncion fb = (NodoFuncion) baseB;
        boolean senoYCoseno = (fa.getTipo() == TipoFuncion.SIN && fb.getTipo() == TipoFuncion.COS)
                || (fa.getTipo() == TipoFuncion.COS && fb.getTipo() == TipoFuncion.SIN);
        return senoYCoseno && fa.getArgumento().igualA(fb.getArgumento());
    }

    private static Nodo baseCuadrado(Nodo n) {
        if (n instanceof NodoPotencia && ((NodoPotencia) n).getExponente().esNumero(2.0)) {
            return ((NodoPotencia) n).getBase();
        }
        return null;
    }

    /** Reemplaza cos/sin, sin/cos, 1/cos y 1/sin dentro de un cociente. */
    private static Nodo cocienteTrigonometrico(Nodo numerador, Nodo denominador) {
        // 1/cos(u)^n = sec(u)^n   y   sin(u)/cos(u)^n = tan(u)·sec(u)^(n-1)
        if (denominador instanceof NodoPotencia && ((NodoPotencia) denominador).getBase() instanceof NodoFuncion
                && ((NodoPotencia) denominador).getExponente().esNumero()) {
            NodoFuncion d = (NodoFuncion) ((NodoPotencia) denominador).getBase();
            double n = ((NodoConstante) ((NodoPotencia) denominador).getExponente()).getValor();
            boolean coseno = d.getTipo() == TipoFuncion.COS;
            boolean seno = d.getTipo() == TipoFuncion.SIN;
            if ((coseno || seno) && n >= 2 && n <= 6 && Math.abs(n - Math.rint(n)) < 1e-12) {
                Nodo u = d.getArgumento();
                TipoFuncion reciproca = coseno ? TipoFuncion.SEC : TipoFuncion.CSC;
                if (numerador.esNumero(1.0)) {
                    return Nodo.pot(Nodo.fn(reciproca, u), n);
                }
                TipoFuncion buscado = coseno ? TipoFuncion.SIN : TipoFuncion.COS;
                List<Nodo> factores = new ArrayList<Nodo>();
                aplanar(numerador, factores);
                for (int i = 0; i < factores.size(); i++) {
                    Nodo f = factores.get(i);
                    if (f instanceof NodoFuncion && ((NodoFuncion) f).getTipo() == buscado
                            && ((NodoFuncion) f).getArgumento().igualA(u)) {
                        Nodo producto = null;
                        for (int j = 0; j < factores.size(); j++) {
                            Nodo factor = j == i ? Nodo.fn(coseno ? TipoFuncion.TAN : TipoFuncion.COT, u) : factores.get(j);
                            producto = producto == null ? factor : Nodo.mul(producto, factor);
                        }
                        Nodo resto = n - 1 == 1 ? Nodo.fn(reciproca, u) : Nodo.pot(Nodo.fn(reciproca, u), n - 1);
                        return Nodo.mul(producto, resto);
                    }
                }
            }
            return null;
        }
        if (!(denominador instanceof NodoFuncion)) {
            return null;
        }
        NodoFuncion d = (NodoFuncion) denominador;
        if (d.getTipo() != TipoFuncion.SIN && d.getTipo() != TipoFuncion.COS) {
            return null;
        }
        Nodo u = d.getArgumento();
        if (numerador.esNumero(1.0)) {
            return Nodo.fn(d.getTipo() == TipoFuncion.SIN ? TipoFuncion.CSC : TipoFuncion.SEC, u);
        }
        TipoFuncion buscado = d.getTipo() == TipoFuncion.SIN ? TipoFuncion.COS : TipoFuncion.SIN;
        TipoFuncion nuevo = d.getTipo() == TipoFuncion.SIN ? TipoFuncion.COT : TipoFuncion.TAN;
        List<Nodo> factores = new ArrayList<Nodo>();
        aplanar(numerador, factores);
        for (int i = 0; i < factores.size(); i++) {
            Nodo f = factores.get(i);
            if (f instanceof NodoFuncion && ((NodoFuncion) f).getTipo() == buscado
                    && ((NodoFuncion) f).getArgumento().igualA(u)) {
                Nodo producto = null;
                for (int j = 0; j < factores.size(); j++) {
                    Nodo factor = j == i ? Nodo.fn(nuevo, u) : factores.get(j);
                    producto = producto == null ? factor : Nodo.mul(producto, factor);
                }
                return producto;
            }
        }
        return null;
    }

    private static void aplanar(Nodo n, List<Nodo> factores) {
        if (n instanceof NodoProducto) {
            aplanar(((NodoProducto) n).getIzquierdo(), factores);
            aplanar(((NodoProducto) n).getDerecho(), factores);
        } else {
            factores.add(n);
        }
    }
}
