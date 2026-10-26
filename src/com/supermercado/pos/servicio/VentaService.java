package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.*;
import com.supermercado.pos.dominio.*;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Transaccion;
import java.util.List;
import java.util.Optional;

public class VentaService {
    private final ProductoDAO productoDao = new ProductoDAO();
    private final VentaDAO ventaDao = new VentaDAO();
    private final VentaDetalleDAO ventaDetalleDao = new VentaDetalleDAO();
    private final PagoDAO pagoDao = new PagoDAO();
    private final TurnoDAO turnoDao = new TurnoDAO();

    public Optional<Producto> buscarProducto(String codigoBarras) {
        return Transaccion.leer(con -> productoDao.buscarPorCodigo(con, codigoBarras.trim()));
    }

    /** Todo o nada: sin venta, sin pagos y sin cambio en el stock si algo falla. */
    public Venta cobrar(Turno turno, Carrito carrito, PagoDto pago) {
        Permisos.exigir(turno.getCajero(), "VENTA_COBRAR");
        if (carrito.estaVacio()) throw new ReglaNegocioException("El carrito está vacío");
        var total = carrito.total();
        List<Pago> pagos = CalculadoraPagos.resolver(total, pago); // falla aquí si el pago no alcanza
        return Transaccion.ejecutar(con -> {
            if (!turnoDao.estaAbierto(con, turno.getId()))   // RN-02
                throw new ReglaNegocioException("No se puede vender: el turno no está abierto");
            Venta v = new Venta();
            v.setTurno(turno);
            v.setTotal(total);
            v.setId(ventaDao.insertar(con, v));
            for (LineaCarrito l : carrito.lineas()) {
                if (productoDao.descontarStock(con, l.producto().getId(), l.cantidad()) == 0)
                    throw new ReglaNegocioException("Sin stock suficiente: " + l.producto().getNombre());
                v.getDetalles().add(ventaDetalleDao.insertar(con, v, l)); // congela el precio unitario
            }
            for (Pago p : pagos) {
                p.setVenta(v);
                p.setId(pagoDao.insertar(con, p));
                v.getPagos().add(p);
            }
            return v;
        });
    }
}
