package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.MovimientoCaja;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.AutorizacionService;
import com.supermercado.pos.servicio.CalculadoraEfectivo;
import com.supermercado.pos.servicio.CorteService;
import com.supermercado.pos.util.Config;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import javax.swing.*;

/** Pantalla 5: corte intermedio (retiro a bóveda). */
public class CorteIntermedioDialog extends JDialog {
    private final CorteService corteService = new CorteService();
    private final AutorizacionService autorizacionService = new AutorizacionService();
    private final JTextField txtMonto = new JTextField(12);
    private final JTextArea resumen = Ui.areaMonoespaciada("", 8, 40);

    public CorteIntermedioDialog(Window owner) {
        super(owner, "Corte intermedio (retiro a bóveda)", ModalityType.APPLICATION_MODAL);
        JButton btnEntregar = new JButton("Entregar a bóveda");
        JButton btnCerrar = new JButton("Cerrar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCerrar);
        botones.add(btnEntregar);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(resumen, BorderLayout.CENTER);
        centro.add(Ui.form("Monto a entregar (Q):", txtMonto), BorderLayout.SOUTH);
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));

        btnCerrar.addActionListener(e -> dispose());
        btnEntregar.addActionListener(e -> entregar());
        cargarResumen();
        pack();
        setLocationRelativeTo(owner);
    }

    private void cargarResumen() {
        try {
            CalculadoraEfectivo.Desglose d = corteService.desglose(Sesion.getTurno());
            resumen.setText(String.format(
                    "Fondo inicial               %12s%n(+) Ventas en efectivo (neto) %12s%n"
                            + "(-) Reversiones en efectivo %12s%n(-) Retiros anteriores        %12s%n"
                            + "----------------------------------------%n"
                            + "= Efectivo esperado en caja %12s%n%nMínimo que debe quedar: %s",
                    Dinero.q(d.fondo()), Dinero.q(d.cobros()), Dinero.q(d.reversiones()),
                    Dinero.q(d.retiros()), Dinero.q(d.esperado()), Dinero.q(Config.efectivoMinimo())));
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void entregar() {
        try {
            Turno turno = Sesion.getTurno();
            Usuario cajero = Sesion.getUsuario();
            BigDecimal monto = Ui.monto(txtMonto.getText());
            if (monto.signum() <= 0) throw new com.supermercado.pos.excepcion.ReglaNegocioException("Monto inválido");
            // Validación previa (RN-05) para no molestar al supervisor si el monto no es válido
            CorteService.validarRetiro(corteService.desglose(turno).esperado(), monto, Config.efectivoMinimo());

            String texto = "Retiro a bóveda\nMonto: " + Dinero.q(monto) + "\nCajero: " + cajero.getNombreCompleto()
                    + "\nQuien recibe debe ingresar sus credenciales.";
            AutorizacionDialog dlg = new AutorizacionDialog(this, "Recibe el efectivo", texto,
                    (u, c) -> autorizacionService.validarReceptorBoveda(u, c, cajero));
            dlg.setVisible(true);
            Usuario receptor = dlg.getResultado();
            if (receptor == null) return;

            MovimientoCaja m = corteService.retirarABoveda(turno, monto, receptor);
            String recibo = "===== RECIBO DE BÓVEDA =====\nFolio : " + m.getReferencia()
                    + "\nFecha : " + m.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "\nCaja  : " + turno.getCaja().getNumero()
                    + "\nEntrega: " + cajero.getNombreCompleto()
                    + "\nRecibe : " + receptor.getNombreCompleto()
                    + "\nMonto : " + Dinero.q(m.getMonto()) + "\n";
            JOptionPane.showMessageDialog(this, new JScrollPane(Ui.areaMonoespaciada(recibo, 9, 34)),
                    "Recibo", JOptionPane.PLAIN_MESSAGE);
            txtMonto.setText("");
            cargarResumen();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }
}
