package com.supermercado.pos;

import com.supermercado.pos.dominio.*;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.*;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Seguridad;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pruebas SIN base de datos (sin JUnit): se ejecutan en cualquier momento.
 * Las pruebas con base de datos están en PruebasBD.
 */
public class Pruebas {
    static int fallos = 0;

    static void verificar(boolean condicion, String caso) {
        System.out.println((condicion ? "OK     " : "FALLÓ  ") + caso);
        if (!condicion) fallos++;
    }

    static void esperaExcepcion(Class<? extends RuntimeException> tipo, Runnable accion, String caso) {
        try {
            accion.run();
            verificar(false, caso + " (no lanzó excepción)");
        } catch (RuntimeException e) {
            verificar(tipo.isInstance(e), caso);
        }
    }

    static BigDecimal q(String v) { return new BigDecimal(v).setScale(2); }

    // ----- datos de prueba -----
    static Permiso permiso(String codigo) {
        Permiso p = new Permiso();
        p.setCodigo(codigo);
        return p;
    }

    static Usuario usuario(long id, String nombre, int nivel, String... permisos) {
        Rol r = new Rol();
        r.setNombre("ROL" + nivel);
        r.setNivel(nivel);
        for (String c : permisos) r.getPermisos().add(permiso(c));
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(nombre);
        u.setNombreCompleto(nombre);
        u.getRoles().add(r);
        return u;
    }

    static Producto producto(long id, String precio, String iva) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Prod" + id);
        p.setPrecio(q(precio));
        p.setPorcentajeImpuesto(q(iva));
        return p;
    }

    public static void main(String[] args) {
        Usuario ana = usuario(1, "ana", 1, "TURNO_ABRIR", "VENTA_COBRAR");
        Usuario luis = usuario(2, "luis", 1, "TURNO_ABRIR");
        Usuario marta = usuario(3, "marta", 2, "REVERSION_AUTORIZAR");
        Usuario admin = usuario(4, "admin", 4, "ROL_ADMIN", "USUARIO_ADMIN");

        System.out.println("--- Seguridad de contraseñas ---");
        String h = Seguridad.hash("Clave1234".toCharArray());
        verificar(Seguridad.verificar("Clave1234".toCharArray(), h), "Contraseña correcta se acepta");
        verificar(!Seguridad.verificar("otra".toCharArray(), h), "Contraseña incorrecta se rechaza");
        verificar(!h.contains("Clave1234"), "El hash no contiene la contraseña en claro");
        verificar(!h.equals(Seguridad.hash("Clave1234".toCharArray())), "Dos hashes de la misma clave son distintos (sal aleatoria)");

        System.out.println("--- Turnos ---");
        esperaExcepcion(ReglaNegocioException.class,
                () -> new TurnoService().abrir(ana, 1L, BigDecimal.ZERO), "Abrir turno con fondo 0 -> rechazado");
        esperaExcepcion(AutorizacionException.class,
                () -> new TurnoService().abrir(usuario(9, "sinPermiso", 1), 1L, q("100")), "Abrir turno sin permiso TURNO_ABRIR -> rechazado");
        verificar(TurnoService.calcularDiferencia(q("680"), q("690")).compareTo(q("-10")) == 0,
                "Cierre contando 680 con esperado 690 -> diferencia -10.00");
        verificar(TurnoService.calcularDiferencia(q("1480"), q("1500")).compareTo(q("-20")) == 0,
                "Cierre con faltante de Q20 -> diferencia = -20.00");
        verificar(TurnoService.calcularDiferencia(q("710"), q("690")).signum() > 0, "Sobrante da diferencia positiva");

        System.out.println("--- Pagos ---");
        esperaExcepcion(ReglaNegocioException.class,
                () -> CalculadoraPagos.resolver(q("100"), new PagoDto(q("50"), q("0"))), "Pago insuficiente -> rechazado");
        List<Pago> mixto = CalculadoraPagos.resolver(q("120"), new PagoDto(q("70"), q("50")));
        Pago ef = mixto.stream().filter(p -> p.getMetodo() == MetodoPago.EFECTIVO).findFirst().orElseThrow();
        Pago tj = mixto.stream().filter(p -> p.getMetodo() == MetodoPago.TARJETA).findFirst().orElseThrow();
        verificar(ef.getMonto().compareTo(q("70")) == 0 && ef.getCambio().signum() == 0 && tj.getMonto().compareTo(q("50")) == 0,
                "Pago mixto Q50 tarjeta + Q70 efectivo: sin cambio");
        List<Pago> conCambio = CalculadoraPagos.resolver(q("120"), new PagoDto(q("100"), q("50")));
        Pago ef2 = conCambio.stream().filter(p -> p.getMetodo() == MetodoPago.EFECTIVO).findFirst().orElseThrow();
        verificar(ef2.getCambio().compareTo(q("30")) == 0 && ef2.getMonto().compareTo(q("70")) == 0,
                "Pago mixto con Q100 en efectivo: el cambio (Q30) sale solo del efectivo");
        List<Pago> unaVenta = CalculadoraPagos.resolver(q("150"), new PagoDto(q("200"), q("0")));
        verificar(unaVenta.size() == 1 && unaVenta.get(0).getMonto().compareTo(q("150")) == 0
                && unaVenta.get(0).getCambio().compareTo(q("50")) == 0, "Venta de Q150 pagada con Q200: cambio Q50, neto Q150");
        esperaExcepcion(ReglaNegocioException.class,
                () -> CalculadoraPagos.resolver(q("100"), new PagoDto(q("0"), q("150"))), "Tarjeta mayor al total -> rechazado");

        System.out.println("--- Carrito y dinero ---");
        Carrito c = new Carrito();
        c.agregar(producto(1, "30.00", "12.00"), 2);
        c.agregar(producto(1, "30.00", "12.00"), 1);
        c.agregar(producto(2, "10.50", "12.00"), 2);
        verificar(c.total().compareTo(q("111.00")) == 0, "Carrito: 3 x 30.00 + 2 x 10.50 = 111.00");
        verificar(c.impuesto().compareTo(q("11.89")) == 0, "Impuesto incluido (12%) = 11.89");
        verificar(Dinero.parsear("1,000.505").compareTo(q("1000.51")) == 0, "Texto \"1,000.505\" se redondea HALF_UP a 1000.51");
        esperaExcepcion(ReglaNegocioException.class, () -> Dinero.parsear("abc"), "Monto con letras -> rechazado");

        System.out.println("--- Efectivo y retiros ---");
        BigDecimal esperado = CalculadoraEfectivo.calcular(q("1000"), q("220"), q("500"), q("30"));
        verificar(esperado.compareTo(q("690")) == 0, "Escenario Fase 11: 1000 + 150 + 70 - 500 - 30 = 690.00");
        esperaExcepcion(ReglaNegocioException.class,
                () -> CorteService.validarRetiro(q("800"), q("400"), q("500")), "Retiro que deja la caja bajo el mínimo -> rechazado");
        CorteService.validarRetiro(q("1220"), q("500"), q("500"));
        verificar(true, "Retiro de 500 con 1220 en caja (quedan 720) -> permitido");

        System.out.println("--- Reversiones ---");
        esperaExcepcion(AutorizacionException.class,
                () -> AutorizacionService.verificarSupervisor(luis, ana), "Reversión autorizada por un cajero -> AutorizacionException");
        esperaExcepcion(AutorizacionException.class,
                () -> AutorizacionService.verificarSupervisor(ana, ana), "Reversión autorizada por el mismo usuario -> rechazada");
        esperaExcepcion(AutorizacionException.class,
                () -> AutorizacionService.verificarSupervisor(usuario(5, "sup2", 2), ana), "Supervisor sin permiso REVERSION_AUTORIZAR -> rechazada");
        esperaExcepcion(AutorizacionException.class,
                () -> AutorizacionService.verificarSupervisor(usuario(6, "mismoNivel", 1, "REVERSION_AUTORIZAR"), ana),
                "Autorizador de nivel igual al del cajero -> rechazada");
        AutorizacionService.verificarSupervisor(marta, ana);
        verificar(true, "Marta (supervisor, nivel 2) puede autorizar a Ana (cajero, nivel 1)");
        ReversionService.validarCantidad(3, 0, 2);
        esperaExcepcion(ReglaNegocioException.class,
                () -> ReversionService.validarCantidad(3, 2, 2), "Reversión parcial dos veces que supera lo vendido -> rechazada en la segunda");
        esperaExcepcion(ReglaNegocioException.class, () -> ReversionService.validarCantidad(3, 0, 0), "Reversar 0 unidades -> rechazado");

        System.out.println("--- Administración de usuarios ---");
        esperaExcepcion(ReglaNegocioException.class, () -> UsuarioService.validarClave("corta".toCharArray()), "Contraseña menor a 8 caracteres -> rechazada");
        esperaExcepcion(ReglaNegocioException.class, () -> UsuarioService.validarUsername("con espacio"), "Username con espacios -> rechazado");
        Rol supervisor = new Rol();
        supervisor.setNombre("SUPERVISOR");
        supervisor.setNivel(2);
        Rol adminRol = new Rol();
        adminRol.setNombre("ADMIN");
        adminRol.setNivel(4);
        UsuarioService.validarNivelesAsignables(Set.of(supervisor), admin);
        verificar(true, "Admin (nivel 4) puede asignar SUPERVISOR (nivel 2)");
        esperaExcepcion(ReglaNegocioException.class,
                () -> UsuarioService.validarNivelesAsignables(Set.of(adminRol), admin), "No se puede asignar un rol de nivel igual al propio");
        esperaExcepcion(AutorizacionException.class,
                () -> new UsuarioService().crear(ana, "nuevo", "Nuevo", "Clave1234".toCharArray(), new HashSet<>(Set.of(1L))),
                "Un cajero no puede crear usuarios (sin USUARIO_ADMIN)");
        esperaExcepcion(ReglaNegocioException.class,
                () -> new UsuarioService().asignar(admin, 4L, Set.of(1L), Set.of()), "Nadie puede modificar sus propios roles");

        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nFALLARON " + fallos + " PRUEBA(S)");
        System.exit(fallos == 0 ? 0 : 1);
    }
}
