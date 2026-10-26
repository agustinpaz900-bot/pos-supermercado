package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Reversion;
import java.sql.*;

public class ReversionDAO {

    public long insertar(Connection con, Reversion r) throws SQLException {
        String sql = "INSERT INTO reversion (detalle_id, cantidad, monto, motivo, solicitado_por, autorizado_por, fecha) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setLong(1, r.getDetalle().getId());
            ps.setInt(2, r.getCantidad());
            ps.setBigDecimal(3, r.getMonto());
            ps.setString(4, r.getMotivo());
            ps.setLong(5, r.getSolicitadoPor().getId());
            ps.setLong(6, r.getAutorizadoPor().getId());
            ps.setTimestamp(7, Timestamp.valueOf(r.getFecha()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }
}
