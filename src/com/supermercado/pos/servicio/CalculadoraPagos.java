package com.supermercado.pos.servicio;

import com.supermercado.pos.dominio.MetodoPago;
import com.supermercado.pos.dominio.Pago;
import com.supermercado.pos.dominio.PagoDto;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reparte el pago entre tarjeta y efectivo. El cambio SOLO sale del efectivo. */
public final class CalculadoraPagos {
    private CalculadoraPagos() {}

    public static List<Pago> resolver(BigDecimal total, PagoDto entrada) {
        BigDecimal tarjeta = Dinero.escala(entrada.tarjeta());
        BigDecimal efectivo = Dinero.escala(entrada.efectivoRecibido());
        if (tarjeta.signum() < 0 || efectivo.signum() < 0) throw new ReglaNegocioException("Monto inválido");
        if (tarjeta.compareTo(total) > 0)
            throw new ReglaNegocioException("La tarjeta no puede cobrar más que el total de la venta");
        BigDecimal resto = total.subtract(tarjeta);          // lo que falta pagar en efectivo
        if (efectivo.compareTo(resto) < 0) throw new ReglaNegocioException("Pago insuficiente");
        BigDecimal cambio = efectivo.subtract(resto);

        List<Pago> pagos = new ArrayList<>();
        if (tarjeta.signum() > 0) {
            Pago p = new Pago();
            p.setMetodo(MetodoPago.TARJETA);
            p.setMonto(tarjeta);
            p.setRecibido(tarjeta);
            p.setCambio(BigDecimal.ZERO.setScale(2));
            pagos.add(p);
        }
        if (resto.signum() > 0) {
            Pago p = new Pago();
            p.setMetodo(MetodoPago.EFECTIVO);
            p.setMonto(resto);          // monto aplicado = recibido - cambio
            p.setRecibido(efectivo);
            p.setCambio(cambio);
            pagos.add(p);
        }
        return pagos;
    }
}
