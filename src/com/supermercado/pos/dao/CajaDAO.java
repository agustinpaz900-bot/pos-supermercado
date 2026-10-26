package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Caja;
import java.sql.*;
import java.util.*;

public class CajaDAO {

    public Optional<Caja> buscarPorId(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM caja WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Caja> listarTodas(Connection con) throws SQLException {
        return listar(con, "SELECT * FROM caja ORDER BY numero");
    }

    /** Cajas activas que no tienen un turno abierto. */
    public List<Caja> listarActivasLibres(Connection con) throws SQLException {
        return listar(con, "SELECT * FROM caja c WHERE c.activa = 1 AND NOT EXISTS "
                + "(SELECT 1 FROM turno t WHERE t.caja_id = c.id AND t.estado = 'ABIERTO') ORDER BY c.numero");
    }

    private List<Caja> listar(Connection con, String sql) throws SQLException {
        List<Caja> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public long insertar(Connection con, Caja c) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO caja (numero, nombre, activa) VALUES (?, ?, ?)", new String[]{"ID"})) {
            ps.setInt(1, c.getNumero());
            ps.setString(2, c.getNombre());
            ps.setInt(3, c.isActiva() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    public void actualizar(Connection con, Caja c) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE caja SET numero = ?, nombre = ?, activa = ? WHERE id = ?")) {
            ps.setInt(1, c.getNumero());
            ps.setString(2, c.getNombre());
            ps.setInt(3, c.isActiva() ? 1 : 0);
            ps.setLong(4, c.getId());
            ps.executeUpdate();
        }
    }

    private Caja mapear(ResultSet rs) throws SQLException {
        Caja c = new Caja();
        c.setId(rs.getLong("id"));
        c.setNumero(rs.getInt("numero"));
        c.setNombre(rs.getString("nombre"));
        c.setActiva(rs.getInt("activa") == 1);
        return c;
    }
}
