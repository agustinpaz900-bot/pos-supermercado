package com.supermercado.pos.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Conexion {
    private Conexion() {}

    public static Connection obtener() throws SQLException {
        String url = Config.get("db.url", "jdbc:oracle:thin:@//localhost:1521/XEPDB1");
        String usuario = Config.get("db.usuario", "pos");
        String clave = Config.get("db.clave", "pos123");
        return DriverManager.getConnection(url, usuario, clave);
    }
}
