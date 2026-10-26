package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.*;
import java.sql.*;
import java.util.*;

public class VentaDetalleDAO {
    private final ProductoDAO productoDao = new ProductoDAO();

    /** Guarda la línea congelando el precio unitario del momento de la venta. */
    public VentaDetalle insertar(Connection con, Venta v, LineaCarrito l) throws SQLException {
        String sql = "INSERT INTO venta_detalle (venta_id, producto_id, cantidad, cantidad_reversada, "
                + "precio_unitario, subtotal) VALUES (?, ?, ?, 0, ?, ?)";
        VentaDetalle d = new VentaDetalle();
        d.setVenta(v);
        d.setProducto(l.producto());
        d.setCantidad(l.cantidad());
        d.setPrecioUnitario(l.producto().getPrecio());
        d.setSubtotal(l.subtotal());
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setLong(1, v.getId());
            ps.setLong(2, l.producto().getId());
            ps.setInt(3, l.cantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.setBigDecimal(5, d.getSubtotal());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                d.setId(k.getLong(1));
            }
        }
        return d;
    }

    public List<VentaDetalle> listarPorVenta(Connection con, long ventaId) throws SQLException {
        List<VentaDetalle> lista = new ArrayList<>();
        List<Long> productoIds = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM venta_detalle WHERE venta_id = ? ORDER BY id")) {
            ps.setLong(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                    productoIds.add(rs.getLong("producto_id"));
                }
            }
        }
        for (int i = 0; i < lista.size(); i++)
            lista.get(i).setProducto(productoDao.buscarPorId(con, productoIds.get(i)).orElseThrow());
        return lista;
    }

    /** SELECT ... FOR UPDATE: bloquea la fila hasta el commit (evita reversar dos veces a la vez). */
    public VentaDetalle buscarParaActualizar(Connection con, long detalleId) throws SQLException {
        VentaDetalle d;
        long productoId;
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM venta_detalle WHERE id = ? FOR UPDATE")) {
            ps.setLong(1, detalleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new com.supermercado.pos.excepcion.ReglaNegocioException("El detalle de venta no existe");
                d = mapear(rs);
                productoId = rs.getLong("producto_id");
            }
        }
        d.setProducto(productoDao.buscarPorId(con, productoId).orElseThrow());
        return d;
    }

    public void sumarReversado(Connection con, long detalleId, int cantidad) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE venta_detalle SET cantidad_reversada = cantidad_reversada + ? WHERE id = ?")) {
            ps.setInt(1, cantidad);
            ps.setLong(2, detalleId);
            ps.executeUpdate();
        }
    }

    private VentaDetalle mapear(ResultSet rs) throws SQLException {
        VentaDetalle d = new VentaDetalle();
        d.setId(rs.getLong("id"));
        Venta v = new Venta();
        v.setId(rs.getLong("venta_id"));
        d.setVenta(v);
        d.setCantidad(rs.getInt("cantidad"));
        d.setCantidadReversada(rs.getInt("cantidad_reversada"));
        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        d.setSubtotal(rs.getBigDecimal("subtotal"));
        return d;
    }
}
