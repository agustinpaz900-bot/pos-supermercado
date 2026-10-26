package com.supermercado.pos;

import com.supermercado.pos.util.Conexion;
import java.sql.Connection;
import java.sql.ResultSet;

/** Prueba de humo (Fase 0): debe imprimir "Conectado a Oracle XE". */
public class PruebaConexion {
    public static void main(String[] args) throws Exception {
        try (Connection con = Conexion.obtener();
             ResultSet rs = con.createStatement().executeQuery("SELECT 'Conectado a Oracle XE' FROM dual")) {
            rs.next();
            System.out.println(rs.getString(1));
        }
    }
}
