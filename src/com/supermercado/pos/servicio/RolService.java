package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.RolDAO;
import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Transaccion;
import java.util.List;
import java.util.Set;

public class RolService {
    private final RolDAO rolDao = new RolDAO();

    public List<Rol> listar() { return Transaccion.leer(rolDao::todos); }

    public List<Permiso> permisos() { return Transaccion.leer(rolDao::todosPermisos); }

    public Rol crear(Usuario actor, String nombre, int nivel) {
        Permisos.exigir(actor, "ROL_ADMIN");
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("El nombre del rol es obligatorio");
        if (nivel < 1 || nivel > 99) throw new ReglaNegocioException("El nivel debe estar entre 1 y 99");
        if (nivel >= actor.nivelMaximo())
            throw new ReglaNegocioException("No puedes crear un rol de nivel igual o superior al tuyo");
        Rol r = new Rol();
        r.setNombre(nombre.trim().toUpperCase());
        r.setNivel(nivel);
        return Transaccion.ejecutar(con -> {
            r.setId(rolDao.insertar(con, r));
            return r;
        });
    }

    public void guardarPermisos(Usuario actor, long rolId, Set<Long> permisoIds) {
        Permisos.exigir(actor, "ROL_ADMIN");
        Transaccion.ejecutar(con -> {
            Rol rol = rolDao.buscarPorId(con, rolId).orElseThrow(() -> new ReglaNegocioException("El rol no existe"));
            if (rol.getNivel() >= actor.nivelMaximo())
                throw new ReglaNegocioException("No puedes modificar un rol de nivel igual o superior al tuyo");
            for (Permiso p : rolDao.todosPermisos(con))      // no se puede dar un permiso que uno mismo no tiene
                if (permisoIds.contains(p.getId()) && !actor.tienePermiso(p.getCodigo()))
                    throw new ReglaNegocioException("No puedes conceder el permiso " + p.getCodigo() + " porque tú no lo tienes");
            rolDao.reemplazarPermisos(con, rolId, permisoIds);
            return null;
        });
    }
}
