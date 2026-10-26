package com.supermercado.pos.dao;

import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import java.sql.*;
import java.util.*;

public class RolDAO {

    public List<Permiso> todosPermisos(Connection con) throws SQLException {
        List<Permiso> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM permiso ORDER BY codigo");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearPermiso(rs));
        }
        return lista;
    }

    public List<Rol> todos(Connection con) throws SQLException {
        List<Rol> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM rol ORDER BY nivel, nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearRol(rs));
        }
        for (Rol r : lista) r.setPermisos(permisosDeRol(con, r.getId()));
        return lista;
    }

    public List<Rol> buscarPorIds(Connection con, Set<Long> ids) throws SQLException {
        List<Rol> lista = new ArrayList<>();
        if (ids.isEmpty()) return lista;
        String marcas = String.join(",", Collections.nCopies(ids.size(), "?"));
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM rol WHERE id IN (" + marcas + ")")) {
            int i = 1;
            for (Long id : ids) ps.setLong(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearRol(rs));
            }
        }
        for (Rol r : lista) r.setPermisos(permisosDeRol(con, r.getId()));
        return lista;
    }

    public Optional<Rol> buscarPorId(Connection con, long id) throws SQLException {
        List<Rol> l = buscarPorIds(con, Set.of(id));
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public long insertar(Connection con, Rol r) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO rol (nombre, nivel) VALUES (?, ?)", new String[]{"ID"})) {
            ps.setString(1, r.getNombre());
            ps.setInt(2, r.getNivel());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return k.getLong(1);
            }
        }
    }

    public void reemplazarPermisos(Connection con, long rolId, Set<Long> permisoIds) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM rol_permiso WHERE rol_id = ?")) {
            ps.setLong(1, rolId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO rol_permiso (rol_id, permiso_id) VALUES (?, ?)")) {
            for (Long pid : permisoIds) {
                ps.setLong(1, rolId);
                ps.setLong(2, pid);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Set<Permiso> permisosDeRol(Connection con, long rolId) throws SQLException {
        Set<Permiso> set = new HashSet<>();
        String sql = "SELECT p.* FROM permiso p JOIN rol_permiso rp ON rp.permiso_id = p.id WHERE rp.rol_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, rolId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(mapearPermiso(rs));
            }
        }
        return set;
    }

    public Set<Rol> rolesDeUsuario(Connection con, long usuarioId) throws SQLException {
        Set<Rol> set = new HashSet<>();
        String sql = "SELECT r.* FROM rol r JOIN usuario_rol ur ON ur.rol_id = r.id WHERE ur.usuario_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(mapearRol(rs));
            }
        }
        for (Rol r : set) r.setPermisos(permisosDeRol(con, r.getId()));
        return set;
    }

    public Set<Permiso> permisosExtraDeUsuario(Connection con, long usuarioId) throws SQLException {
        Set<Permiso> set = new HashSet<>();
        String sql = "SELECT p.* FROM permiso p JOIN usuario_permiso up ON up.permiso_id = p.id WHERE up.usuario_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) set.add(mapearPermiso(rs));
            }
        }
        return set;
    }

    private Permiso mapearPermiso(ResultSet rs) throws SQLException {
        Permiso p = new Permiso();
        p.setId(rs.getLong("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setDescripcion(rs.getString("descripcion"));
        return p;
    }

    private Rol mapearRol(ResultSet rs) throws SQLException {
        Rol r = new Rol();
        r.setId(rs.getLong("id"));
        r.setNombre(rs.getString("nombre"));
        r.setNivel(rs.getInt("nivel"));
        return r;
    }
}
