package com.supermercado.pos.dao;

import java.sql.*;

/** Bitácora de intentos de autorización (exitosos o no). */
public class AuditoriaDAO {

    public void registrar(Connection con, String contexto, Long solicitanteId, String usernameIntentado,
                          boolean exito, String detalle) throws SQLException {
        String sql = "INSERT INTO auditoria_autorizacion (contexto, solicitante_id, username_intentado, exito, detalle) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, contexto);
            if (solicitanteId == null) ps.setNull(2, Types.NUMERIC);
            else ps.setLong(2, solicitanteId);
            ps.setString(3, usernameIntentado == null ? null : usernameIntentado.substring(0, Math.min(50, usernameIntentado.length())));
            ps.setInt(4, exito ? 1 : 0);
            ps.setString(5, detalle == null ? null : detalle.substring(0, Math.min(200, detalle.length())));
            ps.executeUpdate();
        }
    }
}
