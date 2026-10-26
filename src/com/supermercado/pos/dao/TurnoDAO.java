package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class TurnoDAO {
    private final CajaDAO cajaDao = new CajaDAO();
    private final UsuarioDAO usuarioDao = new UsuarioDAO();

    public Optional<Turno> buscarAbiertoPorCajero(Connection con, long cajeroId) throws SQLException {
        String sql = "SELECT * FROM turno WHERE cajero_id = ? AND estado = 'ABIERTO'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, cajeroId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(con, rs)) : Optional.empty();
            }
        }
    }

    public boolean existeAbiertoEnCaja(Connection con, long cajaId) throws SQLException {
        return existe(con, "SELECT 1 FROM turno WHERE caja_id = ? AND estado = 'ABIERTO'", cajaId);
    }

    public boolean estaAbierto(Connection con, long turnoId) throws SQLException {
        return existe(con, "SELECT 1 FROM turno WHERE id = ? AND estado = 'ABIERTO'", turnoId);
    }

    private boolean existe(Connection con, String sql, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertar(Connection con, Turno t) throws SQLException {
        String sql = "INSERT INTO turno (caja_id, cajero_id, apertura, fondo_inicial, estado) "
                + "VALUES (?, ?, ?, ?, 'ABIERTO')";
        // En Oracle se pide la columna ID para recuperar la llave generada
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setLong(1, t.getCaja().getId());
            ps.setLong(2, t.getCajero().getId());
            ps.setTimestamp(3, Timestamp.valueOf(t.getApertura()));
            ps.setBigDecimal(4, t.getFondoInicial());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    public void cerrar(Connection con, Turno t) throws SQLException {
        String sql = "UPDATE turno SET cierre = ?, efectivo_declarado = ?, efectivo_esperado = ?, "
                + "diferencia = ?, estado = 'CERRADO' WHERE id = ? AND estado = 'ABIERTO'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(t.getCierre()));
            ps.setBigDecimal(2, t.getEfectivoDeclarado());
            ps.setBigDecimal(3, t.getEfectivoEsperado());
            ps.setBigDecimal(4, t.getDiferencia());
            ps.setLong(5, t.getId());
            ps.executeUpdate();
        }
    }

    public Optional<Turno> buscarPorId(Connection con, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM turno WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(con, rs)) : Optional.empty();
            }
        }
    }

    /** Arma el Turno columna por columna; carga Caja y Usuario con sus DAO. */
    private Turno mapear(Connection con, ResultSet rs) throws SQLException {
        Turno t = new Turno();
        t.setId(rs.getLong("id"));
        long cajaId = rs.getLong("caja_id");
        long cajeroId = rs.getLong("cajero_id");
        t.setApertura(rs.getTimestamp("apertura").toLocalDateTime());
        Timestamp cierre = rs.getTimestamp("cierre");
        if (cierre != null) t.setCierre(cierre.toLocalDateTime());
        t.setFondoInicial(rs.getBigDecimal("fondo_inicial"));
        t.setEfectivoDeclarado(rs.getBigDecimal("efectivo_declarado"));
        t.setEfectivoEsperado(rs.getBigDecimal("efectivo_esperado"));
        t.setDiferencia(rs.getBigDecimal("diferencia"));
        t.setEstado(EstadoTurno.valueOf(rs.getString("estado")));
        t.setCaja(cajaDao.buscarPorId(con, cajaId).orElseThrow());
        t.setCajero(usuarioDao.buscarPorId(con, cajeroId).orElseThrow());
        return t;
    }
}
