package com.supermercado.pos.util;

import com.supermercado.pos.dominio.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/** Arma el texto del ticket de venta (para mostrarlo en un JTextArea monoespaciado). */
public final class Ticket {
    private Ticket() {}

    public static String texto(Venta v) {
        StringBuilder sb = new StringBuilder();
        sb.append("========== SUPERMERCADO ==========\n");
        sb.append("Folio : ").append(v.getId()).append('\n');
        sb.append("Fecha : ").append(v.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append('\n');
        sb.append("Caja  : ").append(v.getTurno().getCaja().getNumero()).append('\n');
        sb.append("Cajero: ").append(v.getTurno().getCajero().getNombreCompleto()).append('\n');
        sb.append("----------------------------------\n");
        BigDecimal impuesto = BigDecimal.ZERO;
        for (VentaDetalle d : v.getDetalles()) {
            sb.append(String.format("%-14.14s %3d x %8s %9s%n", d.getProducto().getNombre(),
                    d.getCantidad(), Dinero.q(d.getPrecioUnitario()), Dinero.q(d.getSubtotal())));
            BigDecimal pct = d.getProducto().getPorcentajeImpuesto();
            impuesto = impuesto.add(d.getSubtotal().multiply(pct)
                    .divide(BigDecimal.valueOf(100).add(pct), 2, RoundingMode.HALF_UP));
        }
        sb.append("----------------------------------\n");
        sb.append(String.format("%-20s %13s%n", "IVA incluido:", Dinero.q(impuesto)));
        sb.append(String.format("%-20s %13s%n", "TOTAL:", Dinero.q(v.getTotal())));
        for (Pago p : v.getPagos()) {
            sb.append(String.format("%-20s %13s%n", "Pago " + p.getMetodo() + ":", Dinero.q(p.getRecibido())));
            if (p.getCambio().signum() > 0)
                sb.append(String.format("%-20s %13s%n", "Cambio:", Dinero.q(p.getCambio())));
        }
        sb.append("\n    ¡Gracias por su compra!\n");
        return sb.toString();
    }
}
