package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.MovimientoCaja;
import com.supermercado.pos.dominio.TipoMovimiento;
import java.math.BigDecimal;
import java.sql.*;

public class MovimientoDAO {

    public long insertar(Connection con, MovimientoCaja m) throws SQLException {
        String sql = "INSERT INTO movimiento_caja (turno_id, tipo, monto, autorizado_por, referencia, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setLong(1, m.getTurno().getId());
            ps.setString(2, m.getTipo().name());
            ps.setBigDecimal(3, m.getMonto());
            if (m.getAutorizadoPor() == null) ps.setNull(4, Types.NUMERIC);
            else ps.setLong(4, m.getAutorizadoPor().getId());
            ps.setString(5, m.getReferencia());
            ps.setTimestamp(6, Timestamp.valueOf(m.getFecha()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    public void actualizarReferencia(Connection con, long id, String referencia) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE movimiento_caja SET referencia = ? WHERE id = ?")) {
            ps.setString(1, referencia);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Suma el monto de movimiento_caja por turno y tipo. */
    public BigDecimal sumaPorTipo(Connection con, long turnoId, TipoMovimiento tipo) throws SQLException {
        String sql = "SELECT NVL(SUM(monto), 0) FROM movimiento_caja WHERE turno_id = ? AND tipo = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, turnoId);
            ps.setString(2, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
}
