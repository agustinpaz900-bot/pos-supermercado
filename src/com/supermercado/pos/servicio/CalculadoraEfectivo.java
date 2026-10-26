package com.supermercado.pos.servicio;

import com.supermercado.pos.dao.MovimientoDAO;
import com.supermercado.pos.dominio.MetodoPago;
import com.supermercado.pos.dominio.TipoMovimiento;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.util.Dinero;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** RN-04: esperado = fondo + cobros en efectivo (neto) - reversiones en efectivo - retiros a bóveda. */
public class CalculadoraEfectivo {
    private final MovimientoDAO movimientoDao = new MovimientoDAO();

    public record Desglose(BigDecimal fondo, BigDecimal cobros, BigDecimal reversiones, BigDecimal retiros) {
        public BigDecimal esperado() { return calcular(fondo, cobros, retiros, reversiones); }
    }

    /** La fórmula pura (sin base de datos), fácil de probar. */
    public static BigDecimal calcular(BigDecimal fondo, BigDecimal cobros, BigDecimal retiros, BigDecimal reversiones) {
        return Dinero.escala(fondo.add(cobros).subtract(retiros).subtract(reversiones));
    }

    public Desglose desglose(Connection con, Turno t) throws SQLException {
        BigDecimal cobros = escalar(con,
                "SELECT NVL(SUM(p.monto), 0) FROM pago p JOIN venta v ON v.id = p.venta_id "
                        + "WHERE v.turno_id = ? AND p.metodo = '" + MetodoPago.EFECTIVO.name() + "'", t.getId());
        BigDecimal retiros = sumaMovimientos(con, t, TipoMovimiento.RETIRO_BOVEDA);
        BigDecimal revers = sumaMovimientos(con, t, TipoMovimiento.REVERSION_EFECTIVO);
        return new Desglose(t.getFondoInicial(), cobros, revers, retiros);
    }

    public BigDecimal efectivoEsperado(Connection con, Turno t) throws SQLException {
        return desglose(con, t).esperado();
    }

    /** Ejecuta una consulta con un solo parámetro (el id) que devuelve un número. */
    private BigDecimal escalar(Connection con, String sql, long id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    private BigDecimal sumaMovimientos(Connection con, Turno t, TipoMovimiento tipo) throws SQLException {
        return movimientoDao.sumaPorTipo(con, t.getId(), tipo);
    }
}
