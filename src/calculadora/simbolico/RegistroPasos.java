package calculadora.simbolico;

import calculadora.modelo.PasoDetallado;
import calculadora.modelo.ReglaCalculo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bitácora del procedimiento: guarda los pasos detallados y el conjunto de
 * reglas utilizadas. Puede crearse "silenciosa" (sin guardar pasos) para
 * cálculos internos, como las derivadas de la regla de L'Hôpital.
 */
public class RegistroPasos {

    private final List<PasoDetallado> pasos;
    private final Set<ReglaCalculo> usadas;
    private final boolean activo;

    /**
     * @param activo true para guardar los pasos; false para un registro silencioso
     */
    public RegistroPasos(boolean activo) {
        this.pasos = new ArrayList<PasoDetallado>();
        this.usadas = new LinkedHashSet<ReglaCalculo>();
        this.activo = activo;
    }

    /**
     * Crea y registra un paso.
     *
     * @param regla       regla aplicada
     * @param titulo      título del paso
     * @param profundidad nivel de anidamiento
     * @param expresion   expresión sobre la que actúa
     * @param detalle     fórmula o explicación
     * @return el paso creado (para completar su resultado más tarde)
     */
    public PasoDetallado agregar(ReglaCalculo regla, String titulo, int profundidad, String expresion, String detalle) {
        usadas.add(regla);
        PasoDetallado paso = new PasoDetallado(regla, titulo, profundidad);
        paso.setExpresion(expresion);
        paso.setDetalle(detalle);
        if (activo) {
            pasos.add(paso);
        }
        return paso;
    }

    /**
     * Marca una regla como usada sin crear un paso.
     *
     * @param regla regla
     */
    public void marcar(ReglaCalculo regla) {
        usadas.add(regla);
    }

    /**
     * Guarda el estado actual para poder deshacer un intento fallido
     * (por ejemplo, una sustitución que no funcionó).
     *
     * @return marca con el estado actual
     */
    public Object[] marca() {
        return new Object[]{Integer.valueOf(pasos.size()), new LinkedHashSet<ReglaCalculo>(usadas)};
    }

    /**
     * Deshace todo lo registrado después de la marca.
     *
     * @param marca marca obtenida con {@link #marca()}
     */
    @SuppressWarnings("unchecked")
    public void restaurar(Object[] marca) {
        int tamano = ((Integer) marca[0]).intValue();
        while (pasos.size() > tamano) {
            pasos.remove(pasos.size() - 1);
        }
        usadas.clear();
        usadas.addAll((Set<ReglaCalculo>) marca[1]);
    }

    /** @return pasos registrados */
    public List<PasoDetallado> getPasos() {
        return pasos;
    }

    /** @return reglas usadas */
    public Set<ReglaCalculo> getUsadas() {
        return usadas;
    }

    /** @return true si guarda pasos */
    public boolean isActivo() {
        return activo;
    }
}
