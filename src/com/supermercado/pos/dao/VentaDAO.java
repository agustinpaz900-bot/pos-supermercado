package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class VentaDAO {

    public long insertar(Connection con, Venta v) throws SQLException {
        String sql = "INSERT INTO venta (turno_id, fecha, total, estado) VALUES (?, ?, ?, 'COMPLETADA')";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setLong(1, v.getTurno().getId());
            ps.setTimestamp(2, Timestamp.valueOf(v.getFecha()));
            ps.setBigDecimal(3, v.getTotal());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    /** Busca una venta SOLO si pertenece al turno indicado. */
    public Optional<Venta> buscarDeTurno(Connection con, long ventaId, long turnoId) throws SQLException {
        String sql = "SELECT * FROM venta WHERE id = ? AND turno_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, ventaId);
            ps.setLong(2, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Venta v = new Venta();
                v.setId(rs.getLong("id"));
                v.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                v.setTotal(rs.getBigDecimal("total"));
                v.setEstado(EstadoVenta.valueOf(rs.getString("estado")));
                return Optional.of(v);
            }
        }
    }

    /** Recalcula el estado: COMPLETADA, PARCIALMENTE_REVERSADA o ANULADA según lo reversado. */
    public void actualizarEstado(Connection con, long ventaId) throws SQLException {
        long vendidas;
        long reversadas;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT NVL(SUM(cantidad), 0), NVL(SUM(cantidad_reversada), 0) FROM venta_detalle WHERE venta_id = ?")) {
            ps.setLong(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                vendidas = rs.getLong(1);
                reversadas = rs.getLong(2);
            }
        }
        EstadoVenta estado = reversadas == 0 ? EstadoVenta.COMPLETADA
                : (reversadas >= vendidas ? EstadoVenta.ANULADA : EstadoVenta.PARCIALMENTE_REVERSADA);
        try (PreparedStatement ps = con.prepareStatement("UPDATE venta SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado.name());
            ps.setLong(2, ventaId);
            ps.executeUpdate();
        }
    }

    public int contarPorTurno(Connection con, long turnoId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM venta WHERE turno_id = ?")) {
            ps.setLong(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public BigDecimal totalPorTurno(Connection con, long turnoId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT NVL(SUM(total), 0) FROM venta WHERE turno_id = ?")) {
            ps.setLong(1, turnoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
}
