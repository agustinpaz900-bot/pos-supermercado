package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.PagoDto;
import com.supermercado.pos.servicio.CalculadoraPagos;
import com.supermercado.pos.util.Dinero;
import java.awt.*;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Pantalla 4: pago en efectivo, tarjeta o mixto. Calcula el cambio al escribir. */
public class PagoDialog extends JDialog {
    private final BigDecimal total;
    private final JTextField txtEfectivo = new JTextField("0.00", 10);
    private final JTextField txtTarjeta = new JTextField("0.00", 10);
    private final JLabel lblInfo = new JLabel(" ");
    private PagoDto resultado;

    public PagoDialog(Window owner, BigDecimal total) {
        super(owner, "Pago", ModalityType.APPLICATION_MODAL);
        this.total = total;
        JLabel lblTotal = new JLabel("TOTAL A PAGAR: " + Dinero.q(total));
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 18f));
        lblInfo.setFont(lblInfo.getFont().deriveFont(Font.BOLD, 14f));

        JButton btnExacto = new JButton("Efectivo exacto");
        JButton btnAceptar = new JButton("Aceptar");
        JButton btnCancelar = new JButton("Cancelar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnExacto);
        botones.add(btnCancelar);
        botones.add(btnAceptar);

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.add(lblTotal, BorderLayout.NORTH);
        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(Ui.form("Efectivo recibido (Q):", txtEfectivo, "Tarjeta (Q):", txtTarjeta), BorderLayout.CENTER);
        centro.add(lblInfo, BorderLayout.SOUTH);
        p.add(centro, BorderLayout.CENTER);
        p.add(botones, BorderLayout.SOUTH);
        add(Ui.conMargen(p));
        getRootPane().setDefaultButton(btnAceptar);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalcular(); }
            public void removeUpdate(DocumentEvent e) { recalcular(); }
            public void changedUpdate(DocumentEvent e) { recalcular(); }
        };
        txtEfectivo.getDocument().addDocumentListener(dl);
        txtTarjeta.getDocument().addDocumentListener(dl);

        btnExacto.addActionListener(e -> {
            txtTarjeta.setText("0.00");
            txtEfectivo.setText(total.toPlainString());
        });
        btnCancelar.addActionListener(e -> dispose());
        btnAceptar.addActionListener(e -> {
            try {
                PagoDto dto = new PagoDto(Ui.monto(txtEfectivo.getText()), Ui.monto(txtTarjeta.getText()));
                CalculadoraPagos.resolver(total, dto);   // valida antes de cerrar
                resultado = dto;
                dispose();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        });
        recalcular();
        pack();
        setLocationRelativeTo(owner);
    }

    private void recalcular() {
        try {
            BigDecimal ef = Dinero.parsear(txtEfectivo.getText());
            BigDecimal tj = Dinero.parsear(txtTarjeta.getText());
            BigDecimal falta = total.subtract(tj).subtract(ef);
            if (falta.signum() > 0) lblInfo.setText("Falta: " + Dinero.q(falta));
            else lblInfo.setText("Cambio: " + Dinero.q(falta.negate()));
        } catch (RuntimeException e) {
            lblInfo.setText("Escribe montos válidos");
        }
    }

    /** null si el cajero canceló. */
    public PagoDto getResultado() { return resultado; }
}
