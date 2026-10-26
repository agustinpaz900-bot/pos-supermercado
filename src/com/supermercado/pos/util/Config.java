package com.supermercado.pos.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Lee config.properties (carpeta desde donde se ejecuta el programa). */
public final class Config {
    private static final Properties P = new Properties();

    static {
        try (InputStreamReader in = new InputStreamReader(new FileInputStream("config.properties"), StandardCharsets.UTF_8)) {
            P.load(in);
        } catch (IOException e) {
            System.err.println("Aviso: no se encontró config.properties, se usan valores por defecto.");
        }
    }

    private Config() {}

    /** Una propiedad -Dclave=valor en la línea de comandos tiene prioridad (se usa en las pruebas). */
    public static String get(String clave, String porDefecto) {
        String s = System.getProperty(clave);
        return s != null ? s : P.getProperty(clave, porDefecto);
    }

    public static BigDecimal efectivoMinimo() {
        return new BigDecimal(get("efectivo.minimo", "500")).setScale(2, RoundingMode.HALF_UP);
    }
}
