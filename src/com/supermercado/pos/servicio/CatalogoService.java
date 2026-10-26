package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.CajaDAO;
import com.supermercado.pos.dao.ProductoDAO;
import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Transaccion;
import java.math.BigDecimal;
import java.util.List;

/** Catálogo de apoyo: productos y cajas. */
public class CatalogoService {
    private final ProductoDAO productoDao = new ProductoDAO();
    private final CajaDAO cajaDao = new CajaDAO();

    public List<Producto> productos() { return Transaccion.leer(productoDao::listar); }

    public List<Caja> cajas() { return Transaccion.leer(cajaDao::listarTodas); }

    public List<Caja> cajasLibres() { return Transaccion.leer(cajaDao::listarActivasLibres); }

    public void guardarProducto(Usuario actor, Producto p) {
        Permisos.exigir(actor, "CATALOGO_ADMIN");
        if (p.getCodigoBarras() == null || p.getCodigoBarras().isBlank()) throw new ReglaNegocioException("El código de barras es obligatorio");
        if (p.getNombre() == null || p.getNombre().isBlank()) throw new ReglaNegocioException("El nombre es obligatorio");
        if (p.getPrecio() == null || p.getPrecio().signum() <= 0) throw new ReglaNegocioException("El precio debe ser mayor que 0");
        if (p.getPorcentajeImpuesto() == null || p.getPorcentajeImpuesto().signum() < 0
                || p.getPorcentajeImpuesto().compareTo(BigDecimal.valueOf(100)) > 0)
            throw new ReglaNegocioException("El impuesto debe estar entre 0 y 100");
        if (p.getStock() < 0) throw new ReglaNegocioException("El stock no puede ser negativo");
        Transaccion.ejecutar(con -> {
            if (p.getId() == null) p.setId(productoDao.insertar(con, p));
            else productoDao.actualizar(con, p);
            return null;
        });
    }

    public void guardarCaja(Usuario actor, Caja c) {
        Permisos.exigir(actor, "CATALOGO_ADMIN");
        if (c.getNumero() == null || c.getNumero() <= 0) throw new ReglaNegocioException("El número de caja debe ser mayor que 0");
        Transaccion.ejecutar(con -> {
            if (c.getId() == null) c.setId(cajaDao.insertar(con, c));
            else cajaDao.actualizar(con, c);
            return null;
        });
    }
}
