package com.supermercado.pos.servicio;

import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;

/** Sin framework: la autorización es una verificación explícita en cada servicio. */
public final class Permisos {
    private Permisos() {}

    public static void exigir(Usuario u, String codigoPermiso) {
        if (u == null || !u.tienePermiso(codigoPermiso))
            throw new AutorizacionException("No tienes permiso para esta operación (" + codigoPermiso + ")");
    }
}
