package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.*;
import com.supermercado.pos.dominio.*;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Transaccion;
import java.math.BigDecimal;

public class ReversionService {
    private final VentaDAO ventaDao = new VentaDAO();
    private final VentaDetalleDAO detalleDao = new VentaDetalleDAO();
    private final ProductoDAO productoDao = new ProductoDAO();
    private final ReversionDAO reversionDao = new ReversionDAO();
    private final MovimientoDAO movimientoDao = new MovimientoDAO();
    private final TurnoDAO turnoDao = new TurnoDAO();

    /** Busca la venta por folio, solo si es de tu turno actual, con sus detalles. */
    public Venta buscarVenta(long folio, Turno turno) {
        return Transaccion.leer(con -> {
            Venta v = ventaDao.buscarDeTurno(con, folio, turno.getId())
                    .orElseThrow(() -> new ReglaNegocioException("No existe la venta " + folio + " en tu turno actual"));
            v.setDetalles(detalleDao.listarPorVenta(con, folio));
            return v;
        });
    }

    public static void validarCantidad(int vendida, int yaReversada, int solicitada) {
        if (solicitada <= 0 || solicitada > vendida - yaReversada)
            throw new ReglaNegocioException("Cantidad no reversable (disponible: " + (vendida - yaReversada) + ")");
    }

    public Reversion revertir(long detalleId, int cantidad, String motivo,
                              Turno turno, Usuario cajero, Usuario supervisor) {
        Permisos.exigir(cajero, "REVERSION_SOLICITAR");
        if (motivo == null || motivo.isBlank()) throw new ReglaNegocioException("El motivo es obligatorio");
        // RN-06: se vuelve a verificar aquí para que nadie pueda saltarse la ventana de autorización
        AutorizacionService.verificarSupervisor(supervisor, cajero);
        return Transaccion.ejecutar(con -> {
            if (!turnoDao.estaAbierto(con, turno.getId()))
                throw new ReglaNegocioException("El turno no está abierto");
            // SELECT ... FOR UPDATE: bloquea la fila hasta el commit
            VentaDetalle d = detalleDao.buscarParaActualizar(con, detalleId);
            ventaDao.buscarDeTurno(con, d.getVenta().getId(), turno.getId())
                    .orElseThrow(() -> new ReglaNegocioException("La venta no pertenece a tu turno actual"));
            validarCantidad(d.getCantidad(), d.getCantidadReversada(), cantidad);
            BigDecimal monto = Dinero.escala(d.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad)));
            detalleDao.sumarReversado(con, detalleId, cantidad);
            productoDao.reingresarStock(con, d.getProducto().getId(), cantidad);
            ventaDao.actualizarEstado(con, d.getVenta().getId());   // PARCIALMENTE_REVERSADA / ANULADA
            Reversion r = Reversion.de(d, cantidad, monto, motivo.trim(), cajero, supervisor); // RN-07
            r.setId(reversionDao.insertar(con, r));
            // El dinero devuelto al cliente sale de la caja:
            movimientoDao.insertar(con, MovimientoCaja.de(turno, TipoMovimiento.REVERSION_EFECTIVO, monto, supervisor));
            return r;
        });
    }
}
