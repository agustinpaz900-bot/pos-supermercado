package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Usuario;
import java.sql.*;
import java.util.*;

public class UsuarioDAO {
    private final RolDAO rolDao = new RolDAO();

    /** Carga el usuario con sus roles, los permisos de cada rol y sus permisos extra. */
    public Optional<Usuario> buscarPorUsername(Connection con, String username) throws SQLException {
        return buscar(con, "SELECT * FROM usuario WHERE username = ?", username);
    }

    public Optional<Usuario> buscarPorId(Connection con, long id) throws SQLException {
        return buscar(con, "SELECT * FROM usuario WHERE id = ?", id);
    }

    private Optional<Usuario> buscar(Connection con, String sql, Object parametro) throws SQLException {
        Usuario u;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                u = mapear(rs);
            }
        }
        cargarRolesYPermisos(con, u);
        return Optional.of(u);
    }

    public List<Usuario> listar(Connection con) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM usuario ORDER BY username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        for (Usuario u : lista) cargarRolesYPermisos(con, u);
        return lista;
    }

    public long insertar(Connection con, Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (username, password_hash, nombre_completo, activo) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID"})) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNombreCompleto());
            ps.setInt(4, u.isActivo() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    public void actualizar(Connection con, Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET nombre_completo = ?, activo = ?, password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombreCompleto());
            ps.setInt(2, u.isActivo() ? 1 : 0);
            ps.setString(3, u.getPasswordHash());
            ps.setLong(4, u.getId());
            ps.executeUpdate();
        }
    }

    /** DELETE de los roles actuales + INSERT por lotes (addBatch / executeBatch). */
    public void reemplazarRoles(Connection con, long usuarioId, Set<Long> rolIds) throws SQLException {
        reemplazar(con, "usuario_rol", "rol_id", usuarioId, rolIds);
    }

    public void reemplazarPermisosExtra(Connection con, long usuarioId, Set<Long> permisoIds) throws SQLException {
        reemplazar(con, "usuario_permiso", "permiso_id", usuarioId, permisoIds);
    }

    private void reemplazar(Connection con, String tabla, String columna, long usuarioId, Set<Long> ids) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM " + tabla + " WHERE usuario_id = ?")) {
            ps.setLong(1, usuarioId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO " + tabla + " (usuario_id, " + columna + ") VALUES (?, ?)")) {
            for (Long id : ids) {
                ps.setLong(1, usuarioId);
                ps.setLong(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void cargarRolesYPermisos(Connection con, Usuario u) throws SQLException {
        u.setRoles(rolDao.rolesDeUsuario(con, u.getId()));
        u.setPermisosExtra(rolDao.permisosExtraDeUsuario(con, u.getId()));
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setActivo(rs.getInt("activo") == 1);
        return u;
    }
}
