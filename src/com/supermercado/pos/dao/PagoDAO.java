package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.MetodoPago;
import com.supermercado.pos.dominio.Pago;
import java.math.BigDecimal;
import java.sql.*;

public class PagoDAO {

    public long insertar(Connection con, Pago p) throws SQLException {
        String sql = "INSERT INTO pago (venta_id, metodo, monto, recibido, cambio) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setLong(1, p.getVenta().getId());
            ps.setString(2, p.getMetodo().name());
            ps.setBigDecimal(3, p.getMonto());
            ps.setBigDecimal(4, p.getRecibido());
            ps.setBigDecimal(5, p.getCambio());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    /** Suma lo aplicado a las ventas del turno con ese método (efectivo ya es neto: sin el cambio). */
    public BigDecimal sumaPorMetodo(Connection con, long turnoId, MetodoPago metodo) throws SQLException {
        String sql = "SELECT NVL(SUM(p.monto), 0) FROM pago p JOIN venta v ON v.id = p.venta_id "
                + "WHERE v.turno_id = ? AND p.metodo = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, turnoId);
            ps.setString(2, metodo.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }
}
