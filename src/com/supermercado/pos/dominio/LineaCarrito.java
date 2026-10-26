package com.supermercado.pos.dominio;

import java.math.BigDecimal;

public record LineaCarrito(Producto producto, int cantidad) {
    public BigDecimal subtotal() {
        return producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }

    public LineaCarrito sumar(LineaCarrito otra) {
        return new LineaCarrito(producto, cantidad + otra.cantidad);
    }
}
