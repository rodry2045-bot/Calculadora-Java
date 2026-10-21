package calculadora.modelo;

/**
 * Técnica de integración que el estudiante puede exigir desde la lista de métodos.
 */
public enum TecnicaIntegracion {
    /** Se prueban todos los métodos en orden. */
    AUTOMATICO,
    /** Se exige el método de sustitución (cambio de variable). */
    SUSTITUCION,
    /** Se exige integración por partes. */
    PARTES,
    /** Se exige fracciones parciales. */
    FRACCIONES,
    /** Se exige sustitución trigonométrica. */
    SUSTITUCION_TRIG
}
