package com.supermercado.pos.dominio;

import java.math.BigDecimal;

/**
 * Lo que el cliente entrega: efectivo (puede ser más que lo necesario, se da cambio)
 * y/o tarjeta (se cobra exacto). Los dos son opcionales (0 = no usó ese medio).
 */
public record PagoDto(BigDecimal efectivoRecibido, BigDecimal tarjeta) {
    public PagoDto {
        if (efectivoRecibido == null) efectivoRecibido = BigDecimal.ZERO;
        if (tarjeta == null) tarjeta = BigDecimal.ZERO;
    }
}
