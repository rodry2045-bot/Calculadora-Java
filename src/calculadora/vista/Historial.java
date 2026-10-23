package calculadora.vista;

import calculadora.modelo.EntradaCalculo;
import calculadora.modelo.MetodoCalculo;
import calculadora.modelo.Variable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Historial de los últimos cálculos. Se guarda en un archivo de texto en la
 * carpeta del usuario para que siga ahí al volver a abrir la calculadora.
 * <p>
 * Cada línea del archivo es un cálculo con sus datos separados por tabulador.
 * </p>
 */
public class Historial {

    /** Cantidad máxima de cálculos que se recuerdan. */
    public static final int MAXIMO = 25;

    private static final String NOMBRE_ARCHIVO = ".calculadora_calculo_historial.txt";

    /** Un cálculo guardado. */
    public static final class Entrada {
        private final MetodoCalculo metodo;
        private final EntradaCalculo datos;

        Entrada(MetodoCalculo metodo, EntradaCalculo datos) {
            this.metodo = metodo;
            this.datos = datos;
        }

        /** @return método usado */
        public MetodoCalculo getMetodo() {
            return metodo;
        }

        /** @return datos escritos por el estudiante */
        public EntradaCalculo getDatos() {
            return datos;
        }

        /** @return texto corto para el menú del historial */
        public String describir() {
            StringBuilder texto = new StringBuilder();
            texto.append(datos.getExpresion());
            if (metodo.usaFuncionG()) {
                texto.append("  y  ").append(datos.getFuncionG());
            }
            if (datos.isDefinida() || metodo.usaIntervalo()) {
                texto.append("   [").append(datos.getLimiteA()).append(", ").append(datos.getLimiteB()).append("]");
            }
            if (metodo.usaPunto()) {
                texto.append("   en ").append(datos.getPunto());
            }
            texto.append("   ·   ").append(metodo.getNombre());
            String resultado = texto.toString();
            return resultado.length() > 90 ? resultado.substring(0, 87) + "..." : resultado;
        }
    }

    private final List<Entrada> entradas = new ArrayList<Entrada>();
    private final File archivo;

    /** Crea el historial y carga el archivo guardado (si existe). */
    public Historial() {
        this.archivo = new File(System.getProperty("user.home"), NOMBRE_ARCHIVO);
        cargar();
    }

    /** @return los cálculos, del más reciente al más antiguo */
    public List<Entrada> getEntradas() {
        return new ArrayList<Entrada>(entradas);
    }

    /**
     * Agrega un cálculo al principio (si ya estaba, lo sube al principio).
     *
     * @param metodo método usado
     * @param datos  datos escritos
     */
    public void agregar(MetodoCalculo metodo, EntradaCalculo datos) {
        String clave = serializar(metodo, datos);
        for (int i = entradas.size() - 1; i >= 0; i--) {
            if (serializar(entradas.get(i).metodo, entradas.get(i).datos).equals(clave)) {
                entradas.remove(i);
            }
        }
        entradas.add(0, new Entrada(metodo, copiar(datos)));
        while (entradas.size() > MAXIMO) {
            entradas.remove(entradas.size() - 1);
        }
        guardar();
    }

    /** Borra todo el historial. */
    public void limpiar() {
        entradas.clear();
        guardar();
    }

    private void cargar() {
        if (!archivo.isFile()) {
            return;
        }
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(new FileInputStream(archivo),
                StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null && entradas.size() < MAXIMO) {
                Entrada entrada = deserializar(linea);
                if (entrada != null) {
                    entradas.add(entrada);
                }
            }
        } catch (IOException e) {
            // Si no se puede leer, se empieza con el historial vacío
            entradas.clear();
        }
    }

    private void guardar() {
        try (BufferedWriter escritor = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivo),
                StandardCharsets.UTF_8))) {
            for (int i = 0; i < entradas.size(); i++) {
                escritor.write(serializar(entradas.get(i).metodo, entradas.get(i).datos));
                escritor.newLine();
            }
        } catch (IOException e) {
            // El historial es opcional: si no se puede guardar, la calculadora sigue funcionando
        }
    }

    private static String serializar(MetodoCalculo metodo, EntradaCalculo d) {
        String[] campos = {metodo.name(), d.getExpresion(), d.getVariable().name(), d.getOrden(), d.getPunto(),
            d.getLimiteA(), d.getLimiteB(), d.getPuntoC(), d.getFuncionG(), String.valueOf(d.isDefinida())};
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                linea.append('\t');
            }
            String campo = campos[i] == null ? "" : campos[i];
            linea.append(campo.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '));
        }
        return linea.toString();
    }

    private static Entrada deserializar(String linea) {
        String[] campos = linea.split("\t", -1);
        if (campos.length < 10) {
            return null;
        }
        try {
            MetodoCalculo metodo = MetodoCalculo.valueOf(campos[0]);
            EntradaCalculo d = new EntradaCalculo();
            d.setExpresion(campos[1]);
            d.setVariable(Variable.valueOf(campos[2]));
            d.setOrden(campos[3]);
            d.setPunto(campos[4]);
            d.setLimiteA(campos[5]);
            d.setLimiteB(campos[6]);
            d.setPuntoC(campos[7]);
            d.setFuncionG(campos[8]);
            d.setDefinida(Boolean.parseBoolean(campos[9]));
            return new Entrada(metodo, d);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static EntradaCalculo copiar(EntradaCalculo d) {
        return deserializar(serializar(MetodoCalculo.D_AUTOMATICO, d)).datos;
    }
}
