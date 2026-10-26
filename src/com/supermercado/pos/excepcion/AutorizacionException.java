package com.supermercado.pos.excepcion;

/** Se lanza cuando las credenciales o los permisos no son suficientes. */
public class AutorizacionException extends RuntimeException {
    public AutorizacionException(String mensaje) { super(mensaje); }
}
