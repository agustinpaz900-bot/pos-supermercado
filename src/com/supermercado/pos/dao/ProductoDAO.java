package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Producto;
import java.sql.*;
import java.util.*;

public class ProductoDAO {

    public Optional<Producto> buscarPorCodigo(Connection con, String codigo) throws SQLException {
        return buscar(con, "SELECT * FROM producto WHERE codigo_barras = ? AND activo = 1", codigo);
    }

    public Optional<Producto> buscarPorId(Connection con, long id) throws SQLException {
        return buscar(con, "SELECT * FROM producto WHERE id = ?", id);
    }

    private Optional<Producto> buscar(Connection con, String sql, Object parametro) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Producto> listar(Connection con) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM producto ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public long insertar(Connection con, Producto p) throws SQLException {
        String sql = "INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setString(1, p.getCodigoBarras());
            ps.setString(2, p.getNombre());
            ps.setBigDecimal(3, p.getPrecio());
            ps.setBigDecimal(4, p.getPorcentajeImpuesto());
            ps.setInt(5, p.getStock());
            ps.setInt(6, p.isActivo() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    public void actualizar(Connection con, Producto p) throws SQLException {
        String sql = "UPDATE producto SET codigo_barras = ?, nombre = ?, precio = ?, porcentaje_impuesto = ?, "
                + "stock = ?, activo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getCodigoBarras());
            ps.setString(2, p.getNombre());
            ps.setBigDecimal(3, p.getPrecio());
            ps.setBigDecimal(4, p.getPorcentajeImpuesto());
            ps.setInt(5, p.getStock());
            ps.setInt(6, p.isActivo() ? 1 : 0);
            ps.setLong(7, p.getId());
            ps.executeUpdate();
        }
    }

    /** Descuento atómico: solo actúa si hay existencias suficientes. 0 = no había stock. */
    public int descontarStock(Connection con, long productoId, int cantidad) throws SQLException {
        String sql = "UPDATE producto SET stock = stock - ? WHERE id = ? AND stock >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setLong(2, productoId);
            ps.setInt(3, cantidad);
            return ps.executeUpdate();
        }
    }

    public void reingresarStock(Connection con, long productoId, int cantidad) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE producto SET stock = stock + ? WHERE id = ?")) {
            ps.setInt(1, cantidad);
            ps.setLong(2, productoId);
            ps.executeUpdate();
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getLong("id"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNombre(rs.getString("nombre"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setPorcentajeImpuesto(rs.getBigDecimal("porcentaje_impuesto"));
        p.setStock(rs.getInt("stock"));
        p.setActivo(rs.getInt("activo") == 1);
        return p;
    }
}
