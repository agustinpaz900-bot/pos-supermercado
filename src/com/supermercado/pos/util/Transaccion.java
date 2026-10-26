package com.supermercado.pos.util;

import com.supermercado.pos.excepcion.ReglaNegocioException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ejecuta un trabajo dentro de una transacción: si todo sale bien hace commit,
 * si algo falla hace rollback (no se guarda nada a medias).
 */
public final class Transaccion {
    private Transaccion() {}

    @FunctionalInterface
    public interface Trabajo<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    public static <T> T ejecutar(Trabajo<T> trabajo) {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1) // ORA-00001: valor duplicado en un índice o llave única
                throw new ReglaNegocioException("Ya existe un registro con ese valor (dato duplicado)");
            throw new IllegalStateException("Error de base de datos: " + e.getMessage(), e);
        }
    }

    /** Para consultas de solo lectura. */
    public static <T> T leer(Trabajo<T> trabajo) {
        try (Connection con = Conexion.obtener()) {
            return trabajo.ejecutar(con);
        } catch (SQLException e) {
            throw new IllegalStateException("Error de base de datos: " + e.getMessage(), e);
        }
    }
}
