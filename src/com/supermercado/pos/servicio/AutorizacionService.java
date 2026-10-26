package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.AuditoriaDAO;
import com.supermercado.pos.dao.UsuarioDAO;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.Seguridad;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Re-autenticación puntual de OTRO usuario (supervisor). No cambia la sesión del cajero:
 * solo devuelve el Usuario validado. Cada intento queda en la tabla de auditoría.
 */
public class AutorizacionService {
    private final UsuarioDAO usuarioDao = new UsuarioDAO();
    private final AuditoriaDAO auditoriaDao = new AuditoriaDAO();

    public Usuario validarSupervisor(String username, char[] clave, Usuario solicitante) {
        return validar("REVERSION", username, clave, solicitante, true);
    }

    public Usuario validarReceptorBoveda(String username, char[] clave, Usuario cajero) {
        return validar("RETIRO_BOVEDA", username, clave, cajero, false);
    }

    private Usuario validar(String contexto, String username, char[] clave, Usuario solicitante, boolean esReversion) {
        try (Connection con = Conexion.obtener()) {
            try {
                Usuario u = usuarioDao.buscarPorUsername(con, username == null ? "" : username.trim())
                        .filter(Usuario::isActivo)
                        .filter(x -> Seguridad.verificar(clave, x.getPasswordHash()))
                        .orElseThrow(() -> new AutorizacionException("Credenciales inválidas"));
                if (esReversion) verificarSupervisor(u, solicitante);
                else verificarReceptor(u, solicitante);
                auditoriaDao.registrar(con, contexto, solicitante.getId(), username, true, "Autorizado");
                return u;
            } catch (AutorizacionException e) {
                auditoriaDao.registrar(con, contexto, solicitante.getId(), username, false, e.getMessage());
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error de base de datos: " + e.getMessage(), e);
        }
    }

    /** RN-06: permiso REVERSION_AUTORIZAR y nivel estrictamente mayor al del solicitante. */
    public static void verificarSupervisor(Usuario sup, Usuario solicitante) {
        if (sup == null) throw new AutorizacionException("Falta la autorización del supervisor");
        if (sup.getId() != null && sup.getId().equals(solicitante.getId()))
            throw new AutorizacionException("No puedes autorizarte a ti mismo");
        if (!sup.tienePermiso("REVERSION_AUTORIZAR"))
            throw new AutorizacionException("El usuario no puede autorizar reversiones");
        if (sup.nivelMaximo() <= solicitante.nivelMaximo())
            throw new AutorizacionException("Se requiere un rol superior al del solicitante");
    }

    /** Quien recibe el efectivo en la bóveda: otra persona y de nivel superior al cajero. */
    public static void verificarReceptor(Usuario receptor, Usuario cajero) {
        if (receptor == null) throw new AutorizacionException("Falta quien recibe el efectivo");
        if (receptor.getId() != null && receptor.getId().equals(cajero.getId()))
            throw new AutorizacionException("No puedes recibir tu propio retiro");
        if (receptor.nivelMaximo() <= cajero.nivelMaximo())
            throw new AutorizacionException("Quien recibe el efectivo debe tener un rol superior al del cajero");
    }
}
