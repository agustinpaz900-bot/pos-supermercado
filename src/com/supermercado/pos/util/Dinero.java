package com.supermercado.pos.util;

import com.supermercado.pos.excepcion.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Todo el dinero es BigDecimal con escala 2 y HALF_UP (nunca double). */
public final class Dinero {
    private Dinero() {}

    public static BigDecimal escala(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_UP); }

    /** Convierte el texto de una caja de texto a dinero. Acepta "1,000.50" o "Q1000.5". */
    public static BigDecimal parsear(String texto) {
        if (texto == null || texto.isBlank()) throw new ReglaNegocioException("Escribe un monto");
        String limpio = texto.trim().replace("Q", "").replace("q", "").replace(",", "").trim();
        try {
            BigDecimal v = new BigDecimal(limpio);
            if (v.signum() < 0) throw new ReglaNegocioException("El monto no puede ser negativo");
            return escala(v);
        } catch (NumberFormatException e) {
            throw new ReglaNegocioException("Monto inválido: \"" + texto + "\" (usa solo números, ej. 150.50)");
        }
    }

    /** Texto con formato: Q1,250.50 */
    public static String q(BigDecimal v) {
        return v == null ? "Q0.00" : "Q" + String.format(Locale.US, "%,.2f", escala(v));
    }
}
