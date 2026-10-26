package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.MovimientoDAO;
import com.supermercado.pos.dao.TurnoDAO;
import com.supermercado.pos.dominio.MovimientoCaja;
import com.supermercado.pos.dominio.TipoMovimiento;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Config;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Transaccion;
import java.math.BigDecimal;

/** Corte intermedio: el cajero entrega efectivo a la bóveda. */
public class CorteService {
    private final MovimientoDAO movimientoDao = new MovimientoDAO();
    private final TurnoDAO turnoDao = new TurnoDAO();
    private final CalculadoraEfectivo calculadora = new CalculadoraEfectivo();

    public CalculadoraEfectivo.Desglose desglose(Turno turno) {
        return Transaccion.leer(con -> calculadora.desglose(con, turno));
    }

    /** RN-05: la caja no puede quedar con menos del efectivo mínimo operativo. */
    public static void validarRetiro(BigDecimal efectivoActual, BigDecimal monto, BigDecimal minimo) {
        if (monto == null || monto.signum() <= 0) throw new ReglaNegocioException("Monto inválido");
        if (efectivoActual.subtract(monto).compareTo(minimo) < 0)
            throw new ReglaNegocioException("La caja debe conservar al menos " + Dinero.q(minimo)
                    + " (máximo a retirar ahora: " + Dinero.q(efectivoActual.subtract(minimo).max(BigDecimal.ZERO)) + ")");
    }

    public MovimientoCaja retirarABoveda(Turno turno, BigDecimal monto, Usuario receptor) {
        Permisos.exigir(turno.getCajero(), "CORTE_INTERMEDIO");
        if (monto == null || monto.signum() <= 0) throw new ReglaNegocioException("Monto inválido");
        AutorizacionService.verificarReceptor(receptor, turno.getCajero());
        BigDecimal montoFinal = Dinero.escala(monto);
        return Transaccion.ejecutar(con -> {
            if (!turnoDao.estaAbierto(con, turno.getId()))
                throw new ReglaNegocioException("El turno no está abierto");
            BigDecimal actual = calculadora.efectivoEsperado(con, turno);
            validarRetiro(actual, montoFinal, Config.efectivoMinimo());
            MovimientoCaja m = MovimientoCaja.de(turno, TipoMovimiento.RETIRO_BOVEDA, montoFinal, receptor);
            m.setId(movimientoDao.insertar(con, m));
            m.setReferencia("BOV-" + m.getId());           // folio del recibo de bóveda
            movimientoDao.actualizarReferencia(con, m.getId(), m.getReferencia());
            return m;
        });
    }
}
