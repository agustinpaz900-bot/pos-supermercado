package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla 8: corte final. Es un cierre "ciego": el cajero cuenta y declara su efectivo
 * ANTES de ver cuánto esperaba el sistema.
 */
public class CierreTurnoFrame extends JFrame {
    private static final String[] DENOMINACIONES = {"200.00", "100.00", "50.00", "20.00", "10.00", "5.00", "1.00",
            "0.50", "0.25", "0.10", "0.05", "0.01"};
    private final TurnoService turnoService = new TurnoService();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Denominación (Q)", "Cantidad", "Subtotal"}, 0) {
        @Override public boolean isCellEditable(int f, int c) { return c == 1; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblContado = new JLabel("Total contado: Q0.00");
    private boolean actualizando = false;

    public CierreTurnoFrame() {
        super("Cierre de turno");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 560);
        setLocationRelativeTo(null);
        Turno turno = Sesion.getTurno();

        TurnoService.ResumenTurno r = turnoService.resumen(turno);
        String texto = String.format("Resumen del turno - Caja %d%n%nNúmero de ventas : %d%nTotal vendido    : %s%n"
                        + "  en efectivo    : %s%n  con tarjeta     : %s%nReversiones      : %s%nRetiros a bóveda : %s%n",
                turno.getCaja().getNumero(), r.ventas(), Dinero.q(r.totalVendido()), Dinero.q(r.efectivo()),
                Dinero.q(r.tarjeta()), Dinero.q(r.reversiones()), Dinero.q(r.retiros()));

        for (String d : DENOMINACIONES) modelo.addRow(new Object[]{d, 0, "Q0.00"});
        modelo.addTableModelListener(e -> {
            if (!actualizando) recalcular();
        });

        JButton btnCerrar = new JButton("Declarar y cerrar turno");
        JPanel sur = new JPanel(new BorderLayout(8, 8));
        lblContado.setFont(lblContado.getFont().deriveFont(Font.BOLD, 16f));
        sur.add(lblContado, BorderLayout.CENTER);
        sur.add(btnCerrar, BorderLayout.SOUTH);

        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(Ui.areaMonoespaciada(texto, 8, 40), BorderLayout.NORTH);
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(sur, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));

        btnCerrar.addActionListener(e -> cerrar(turno));
    }

    /** Suma billetes y monedas: denominación x cantidad. */
    private BigDecimal recalcular() {
        actualizando = true;
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < modelo.getRowCount(); i++) {
            int cantidad;
            try {
                cantidad = Math.max(0, Integer.parseInt(String.valueOf(modelo.getValueAt(i, 1)).trim()));
            } catch (NumberFormatException ex) {
                cantidad = 0;
            }
            modelo.setValueAt(cantidad, i, 1);
            BigDecimal sub = new BigDecimal(DENOMINACIONES[i]).multiply(BigDecimal.valueOf(cantidad));
            modelo.setValueAt(Dinero.q(sub), i, 2);
            total = total.add(sub);
        }
        actualizando = false;
        lblContado.setText("Total contado: " + Dinero.q(total));
        return Dinero.escala(total);
    }

    private void cerrar(Turno turno) {
        if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();
        BigDecimal contado = recalcular();
        int ok = JOptionPane.showConfirmDialog(this, "Vas a declarar " + Dinero.q(contado)
                        + ".\nEl turno quedará CERRADO y no se podrá vender más. ¿Continuar?",
                "Confirmar cierre", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            Turno t = turnoService.cerrar(turno, contado);
            String tipo = t.getDiferencia().signum() == 0 ? "CUADRADO"
                    : (t.getDiferencia().signum() > 0 ? "SOBRANTE" : "FALTANTE");
            String reporte = String.format("====== REPORTE DE CIERRE ======%nCaja: %d   Cajero: %s%n"
                            + "Efectivo esperado : %s%nEfectivo contado  : %s%nDiferencia        : %s (%s)%n",
                    t.getCaja().getNumero(), t.getCajero().getNombreCompleto(),
                    Dinero.q(t.getEfectivoEsperado()), Dinero.q(t.getEfectivoDeclarado()),
                    Dinero.q(t.getDiferencia()), tipo);
            JOptionPane.showMessageDialog(this, new JScrollPane(Ui.areaMonoespaciada(reporte, 6, 40)),
                    "Turno cerrado", JOptionPane.PLAIN_MESSAGE);
            Sesion.setTurno(null);
            dispose();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }
}
