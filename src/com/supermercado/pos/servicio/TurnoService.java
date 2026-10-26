package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.*;
import com.supermercado.pos.dominio.*;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Transaccion;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

public class TurnoService {
    private final TurnoDAO turnoDao = new TurnoDAO();
    private final CajaDAO cajaDao = new CajaDAO();
    private final MovimientoDAO movimientoDao = new MovimientoDAO();
    private final VentaDAO ventaDao = new VentaDAO();
    private final PagoDAO pagoDao = new PagoDAO();
    private final CalculadoraEfectivo calculadora = new CalculadoraEfectivo();

    public record ResumenTurno(int ventas, BigDecimal totalVendido, BigDecimal efectivo, BigDecimal tarjeta,
                               BigDecimal reversiones, BigDecimal retiros, BigDecimal fondo) {}

    public Optional<Turno> buscarAbierto(Usuario cajero) {
        return Transaccion.leer(con -> turnoDao.buscarAbiertoPorCajero(con, cajero.getId()));
    }

    /** RN-01, RN-02, RN-03: la apertura es una transacción (turno + movimiento de fondo inicial). */
    public Turno abrir(Usuario cajero, long cajaId, BigDecimal fondo) {
        Permisos.exigir(cajero, "TURNO_ABRIR");
        if (fondo == null || fondo.signum() <= 0)
            throw new ReglaNegocioException("Debe declarar el efectivo inicial");
        return Transaccion.ejecutar(con -> {
            if (turnoDao.buscarAbiertoPorCajero(con, cajero.getId()).isPresent())
                throw new ReglaNegocioException("Ya tienes un turno abierto");
            Caja caja = cajaDao.buscarPorId(con, cajaId)
                    .orElseThrow(() -> new ReglaNegocioException("La caja no existe"));
            if (!caja.isActiva()) throw new ReglaNegocioException("La caja está desactivada");
            if (turnoDao.existeAbiertoEnCaja(con, cajaId))
                throw new ReglaNegocioException("La caja está ocupada por otro cajero");
            Turno t = new Turno();
            t.setCaja(caja);
            t.setCajero(cajero);
            t.setApertura(LocalDateTime.now());
            t.setFondoInicial(Dinero.escala(fondo));
            try {
                t.setId(turnoDao.insertar(con, t));
            } catch (SQLException e) {
                if (e.getErrorCode() == 1) // ORA-00001: índice único (RN-01 a nivel de base de datos)
                    throw new ReglaNegocioException("La caja o el cajero ya tienen un turno abierto");
                throw e;
            }
            movimientoDao.insertar(con, MovimientoCaja.de(t, TipoMovimiento.FONDO_INICIAL, t.getFondoInicial(), null));
            return t;
        });
    }

    public ResumenTurno resumen(Turno t) {
        return Transaccion.leer(con -> {
            CalculadoraEfectivo.Desglose d = calculadora.desglose(con, t);
            return new ResumenTurno(
                    ventaDao.contarPorTurno(con, t.getId()),
                    ventaDao.totalPorTurno(con, t.getId()),
                    pagoDao.sumaPorMetodo(con, t.getId(), MetodoPago.EFECTIVO),
                    pagoDao.sumaPorMetodo(con, t.getId(), MetodoPago.TARJETA),
                    d.reversiones(), d.retiros(), d.fondo());
        });
    }

    /** Diferencia: positiva = sobrante, negativa = faltante. */
    public static BigDecimal calcularDiferencia(BigDecimal contado, BigDecimal esperado) {
        return Dinero.escala(contado.subtract(esperado));
    }

    /** RN-08: el cajero declara lo contado, el sistema calcula la diferencia y bloquea el turno. */
    public Turno cerrar(Turno t, BigDecimal contado) {
        Permisos.exigir(t.getCajero(), "TURNO_CERRAR");
        if (contado == null || contado.signum() < 0) throw new ReglaNegocioException("Efectivo contado inválido");
        BigDecimal declarado = Dinero.escala(contado);
        return Transaccion.ejecutar(con -> {
            if (!turnoDao.estaAbierto(con, t.getId()))
                throw new ReglaNegocioException("El turno ya está cerrado");
            BigDecimal esperado = calculadora.efectivoEsperado(con, t);
            t.setEfectivoEsperado(esperado);
            t.setEfectivoDeclarado(declarado);
            t.setDiferencia(calcularDiferencia(declarado, esperado));
            t.setCierre(LocalDateTime.now());
            t.setEstado(EstadoTurno.CERRADO);
            turnoDao.cerrar(con, t);
            return t;
        });
    }
}
