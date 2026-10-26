package com.supermercado.pos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Carrito de compras en memoria (vive en la ventana hasta cobrar). */
public class Carrito {
    private final Map<Long, LineaCarrito> lineas = new LinkedHashMap<>();

    public void agregar(Producto p, int cantidad) {
        lineas.merge(p.getId(), new LineaCarrito(p, cantidad), LineaCarrito::sumar);
    }

    public void fijarCantidad(long productoId, int cantidad) {
        LineaCarrito l = lineas.get(productoId);
        if (l != null) lineas.put(productoId, new LineaCarrito(l.producto(), cantidad));
    }

    public void quitar(long productoId) { lineas.remove(productoId); }

    public Collection<LineaCarrito> lineas() { return lineas.values(); }

    public boolean estaVacio() { return lineas.isEmpty(); }

    public void vaciar() { lineas.clear(); }

    public BigDecimal total() {
        return lineas.values().stream().map(LineaCarrito::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    /** El precio ya incluye el impuesto; aquí solo se calcula cuánto es impuesto. */
    public BigDecimal impuesto() {
        BigDecimal t = BigDecimal.ZERO;
        for (LineaCarrito l : lineas.values()) {
            BigDecimal pct = l.producto().getPorcentajeImpuesto();
            t = t.add(l.subtotal().multiply(pct)
                    .divide(BigDecimal.valueOf(100).add(pct), 2, RoundingMode.HALF_UP));
        }
        return t.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal subtotalSinImpuesto() { return total().subtract(impuesto()); }
}
