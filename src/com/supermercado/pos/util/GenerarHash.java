package com.supermercado.pos.util;

/** Herramienta de apoyo: java -cp out com.supermercado.pos.util.GenerarHash MiClave123 */
public class GenerarHash {
    public static void main(String[] args) {
        String clave = args.length > 0 ? args[0] : "Admin123!";
        System.out.println(Seguridad.hash(clave.toCharArray()));
    }
}
