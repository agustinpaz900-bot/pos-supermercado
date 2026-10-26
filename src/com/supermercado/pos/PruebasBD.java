package com.supermercado.pos;

import static com.supermercado.pos.Pruebas.esperaExcepcion;
import static com.supermercado.pos.Pruebas.verificar;

import com.supermercado.pos.dominio.*;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.*;
import java.math.BigDecimal;

/**
 * Pruebas CON base de datos: reproducen el escenario de la Fase 11.
 * Ejecutar SOLO contra el esquema de pruebas (usuario pos_test):
 *   java -Ddb.usuario=pos_test -cp ... com.supermercado.pos.PruebasBD
 * Requiere haber ejecutado 01, 02 y 03 en pos_test.
 */
public class PruebasBD {
    static BigDecimal q(String v) { return new BigDecimal(v).setScale(2); }

    static Carrito carrito(Producto p, int cantidad) {
        Carrito c = new Carrito();
        c.agregar(p, cantidad);
        return c;
    }

    public static void main(String[] args) {
        try {
            correr();
        } catch (Exception e) {
            e.printStackTrace();
            Pruebas.fallos++;
        }
        System.out.println(Pruebas.fallos == 0 ? "\nTODAS LAS PRUEBAS CON BASE DE DATOS PASARON"
                : "\nFALLARON " + Pruebas.fallos + " PRUEBA(S)");
        System.exit(Pruebas.fallos == 0 ? 0 : 1);
    }

    static void correr() {
        AuthService auth = new AuthService();
        TurnoService turnos = new TurnoService();
        VentaService ventas = new VentaService();
        CorteService cortes = new CorteService();
        ReversionService reversiones = new ReversionService();
        AutorizacionService autorizacion = new AutorizacionService();
        char[] clave = "Clave1234".toCharArray();

        System.out.println("--- Autenticación ---");
        Usuario ana = auth.autenticar("ana", clave);
        Usuario luis = auth.autenticar("luis", clave);
        Usuario marta = auth.autenticar("marta", clave);
        esperaExcepcion(AutorizacionException.class, () -> auth.autenticar("ana", "mala".toCharArray()), "Contraseña incorrecta -> AutorizacionException");
        esperaExcepcion(AutorizacionException.class, () -> auth.autenticar("noexiste", clave), "Usuario inexistente -> AutorizacionException");

        // Limpieza: si una corrida anterior dejó turnos abiertos, se cierran
        for (Usuario u : new Usuario[]{ana, luis})
            turnos.buscarAbierto(u).ifPresent(t -> turnos.cerrar(t, BigDecimal.ZERO));

        var cajas = new CatalogoService().cajas();
        long caja1 = cajas.stream().filter(c -> c.getNumero() == 1).findFirst().orElseThrow().getId();
        long caja2 = cajas.stream().filter(c -> c.getNumero() == 2).findFirst().orElseThrow().getId();

        System.out.println("--- Apertura de turnos ---");
        esperaExcepcion(ReglaNegocioException.class, () -> turnos.abrir(ana, caja1, BigDecimal.ZERO), "Abrir turno con fondo 0 -> rechazado");
        Turno turnoAna = turnos.abrir(ana, caja1, q("1000.00"));
        verificar(turnoAna.getId() != null, "Ana abre la Caja 1 con Q1,000.00");
        esperaExcepcion(ReglaNegocioException.class, () -> turnos.abrir(ana, caja2, q("100")), "Ana no puede abrir un segundo turno");
        esperaExcepcion(ReglaNegocioException.class, () -> turnos.abrir(luis, caja1, q("800")), "Luis no puede abrir la Caja 1 (ocupada)");
        Turno turnoLuis = turnos.abrir(luis, caja2, q("800.00"));
        verificar(turnoLuis.getId() != null, "Luis abre la Caja 2 con Q800.00");

        System.out.println("--- Ventas ---");
        Producto p30 = ventas.buscarProducto("PRUEBA30").orElseThrow();
        Producto p10 = ventas.buscarProducto("PRUEBA10").orElseThrow();
        int stockAntes = p30.getStock();
        esperaExcepcion(ReglaNegocioException.class,
                () -> ventas.cobrar(turnoAna, carrito(p30, 2), new PagoDto(q("10"), q("0"))), "Cobro con pago insuficiente -> rechazado");
        verificar(ventas.buscarProducto("PRUEBA30").orElseThrow().getStock() == stockAntes, "El rechazo no cambió el stock");

        Venta v1 = ventas.cobrar(turnoAna, carrito(p30, 5), new PagoDto(q("200"), q("0")));
        Venta v2 = ventas.cobrar(turnoAna, carrito(p10, 8), new PagoDto(q("0"), q("80")));
        Venta v3 = ventas.cobrar(turnoAna, carrito(p30, 4), new PagoDto(q("70"), q("50")));
        verificar(v1.getTotal().compareTo(q("150")) == 0 && v2.getTotal().compareTo(q("80")) == 0
                && v3.getTotal().compareTo(q("120")) == 0, "Tres ventas: Q150 efectivo, Q80 tarjeta, Q120 mixta");
        verificar(v1.getPagos().get(0).getCambio().compareTo(q("50")) == 0, "La venta de Q150 pagada con Q200 dio Q50 de cambio");

        System.out.println("--- Corte intermedio ---");
        esperaExcepcion(ReglaNegocioException.class, () -> cortes.retirarABoveda(turnoAna, q("800"), marta), "Retiro que deja la caja bajo el mínimo -> rechazado");
        esperaExcepcion(AutorizacionException.class, () -> cortes.retirarABoveda(turnoAna, q("500"), luis), "Recibir el retiro un cajero -> rechazado");
        MovimientoCaja retiro = cortes.retirarABoveda(turnoAna, q("500"), marta);
        verificar(retiro.getReferencia() != null && retiro.getReferencia().startsWith("BOV-"), "Retiro de Q500 a Marta con folio " + retiro.getReferencia());

        System.out.println("--- Reversión ---");
        esperaExcepcion(AutorizacionException.class, () -> autorizacion.validarSupervisor("luis", clave, ana), "Intentar autorizar con Luis (cajero) -> falla");
        esperaExcepcion(AutorizacionException.class, () -> autorizacion.validarSupervisor("marta", "mala".toCharArray(), ana), "Contraseña incorrecta de supervisor -> falla");
        Usuario supervisor = autorizacion.validarSupervisor("marta", clave, ana);
        long detalleId = reversiones.buscarVenta(v1.getId(), turnoAna).getDetalles().get(0).getId();
        reversiones.revertir(detalleId, 1, "Producto en mal estado", turnoAna, ana, supervisor);
        verificar(reversiones.buscarVenta(v1.getId(), turnoAna).getEstado() == EstadoVenta.PARCIALMENTE_REVERSADA,
                "Revertir 1 producto de Q30: la venta queda PARCIALMENTE_REVERSADA");
        esperaExcepcion(ReglaNegocioException.class,
                () -> reversiones.revertir(detalleId, 5, "Otra vez", turnoAna, ana, supervisor), "Segunda reversión que supera lo vendido -> rechazada");
        esperaExcepcion(AutorizacionException.class,
                () -> reversiones.revertir(detalleId, 1, "x", turnoAna, ana, luis), "Reversión autorizada por un cajero -> AutorizacionException");
        esperaExcepcion(ReglaNegocioException.class,
                () -> reversiones.buscarVenta(v1.getId(), turnoLuis), "Luis no puede ver ventas del turno de Ana");

        System.out.println("--- Cierre de turno ---");
        Turno cerrado = turnos.cerrar(turnoAna, q("670.00"));
        verificar(cerrado.getEfectivoEsperado().compareTo(q("690.00")) == 0, "Efectivo esperado de Ana = Q690.00");
        verificar(cerrado.getDiferencia().compareTo(q("-20.00")) == 0, "Contó Q670.00 -> diferencia = -20.00 (faltante)");
        esperaExcepcion(ReglaNegocioException.class, () -> turnos.cerrar(turnoAna, q("690")), "Cerrar un turno ya cerrado -> rechazado");
        esperaExcepcion(ReglaNegocioException.class, () -> ventas.cobrar(turnoAna, carrito(p10, 1), new PagoDto(q("10"), q("0"))),
                "Vender con el turno cerrado -> rechazado (RN-02)");

        turnos.cerrar(turnoLuis, q("800.00"));   // deja la base lista para repetir las pruebas
    }
}
