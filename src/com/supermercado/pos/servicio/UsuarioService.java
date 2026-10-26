package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dao.UsuarioDAO;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Seguridad;
import com.supermercado.pos.util.Transaccion;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class UsuarioService {
    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final RolDAO rolDao = new RolDAO();

    public List<Usuario> listar() {
        return Transaccion.leer(usuarioDao::listar);
    }

    public static void validarUsername(String username) {
        if (username == null || username.isEmpty()) throw new ReglaNegocioException("El usuario es obligatorio");
        if (!username.matches("\\S+")) throw new ReglaNegocioException("El usuario no puede tener espacios");
        if (username.length() > 50) throw new ReglaNegocioException("El usuario es demasiado largo (máx. 50)");
    }

    public static void validarClave(char[] clave) {
        if (clave == null || clave.length < 8)
            throw new ReglaNegocioException("La contraseña debe tener mínimo 8 caracteres");
    }

    /** Evita el escalamiento de privilegios: no se asignan roles de nivel igual o superior al propio. */
    public static void validarNivelesAsignables(Collection<Rol> roles, Usuario actor) {
        for (Rol r : roles)
            if (r.getNivel() >= actor.nivelMaximo())
                throw new ReglaNegocioException("No puedes asignar un rol igual o superior al tuyo");
    }

    public Usuario crear(Usuario actor, String username, String nombre, char[] clave, Set<Long> rolIds) {
        Permisos.exigir(actor, "USUARIO_ADMIN");
        String user = username == null ? "" : username.trim();
        validarUsername(user);
        validarClave(clave);
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre es obligatorio");
        if (rolIds == null || rolIds.isEmpty()) throw new ReglaNegocioException("Selecciona un rol");
        String hash = Seguridad.hash(clave);
        return Transaccion.ejecutar(con -> {
            if (usuarioDao.buscarPorUsername(con, user).isPresent())
                throw new ReglaNegocioException("El usuario \"" + user + "\" ya existe");
            validarNivelesAsignables(rolDao.buscarPorIds(con, rolIds), actor);
            Usuario u = new Usuario();
            u.setUsername(user);
            u.setNombreCompleto(nombre.trim());
            u.setPasswordHash(hash);
            u.setId(usuarioDao.insertar(con, u));
            usuarioDao.reemplazarRoles(con, u.getId(), rolIds);
            return u;
        });
    }

    /** Nunca se borran usuarios con historial: se desactivan. nuevaClave vacía = no cambiar. */
    public void actualizar(Usuario actor, long usuarioId, String nombre, boolean activo, char[] nuevaClave) {
        Permisos.exigir(actor, "USUARIO_ADMIN");
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre es obligatorio");
        boolean cambiaClave = nuevaClave != null && nuevaClave.length > 0;
        if (cambiaClave) validarClave(nuevaClave);
        boolean esElMismo = actor.getId().equals(usuarioId);
        if (esElMismo && !activo) throw new ReglaNegocioException("No puedes desactivarte a ti mismo");
        Transaccion.ejecutar(con -> {
            Usuario u = usuarioDao.buscarPorId(con, usuarioId)
                    .orElseThrow(() -> new ReglaNegocioException("El usuario no existe"));
            if (!esElMismo && u.nivelMaximo() >= actor.nivelMaximo())
                throw new ReglaNegocioException("No puedes modificar a un usuario de nivel igual o superior al tuyo");
            u.setNombreCompleto(nombre.trim());
            u.setActivo(activo);
            if (cambiaClave) u.setPasswordHash(Seguridad.hash(nuevaClave));
            usuarioDao.actualizar(con, u);
            return null;
        });
    }

    /** Asigna roles y permisos extra en una sola transacción. */
    public void asignar(Usuario actor, long usuarioId, Set<Long> rolIds, Set<Long> permisoExtraIds) {
        // Sin framework: la autorización es una verificación explícita en el servicio
        Permisos.exigir(actor, "ROL_ADMIN");
        if (actor.getId().equals(usuarioId))
            throw new ReglaNegocioException("No puedes modificar tus propios roles ni permisos");
        if (rolIds == null || rolIds.isEmpty()) throw new ReglaNegocioException("Selecciona al menos un rol");
        Transaccion.ejecutar(con -> {
            Usuario u = usuarioDao.buscarPorId(con, usuarioId)
                    .orElseThrow(() -> new ReglaNegocioException("El usuario no existe"));
            if (u.nivelMaximo() >= actor.nivelMaximo())
                throw new ReglaNegocioException("No puedes modificar a un usuario de nivel igual o superior al tuyo");
            validarNivelesAsignables(rolDao.buscarPorIds(con, rolIds), actor);
            usuarioDao.reemplazarRoles(con, usuarioId, rolIds);
            usuarioDao.reemplazarPermisosExtra(con, usuarioId, permisoExtraIds == null ? Set.of() : permisoExtraIds);
            return null;
        });
        // Los cambios aplican en el próximo inicio de sesión del usuario afectado.
    }
}
