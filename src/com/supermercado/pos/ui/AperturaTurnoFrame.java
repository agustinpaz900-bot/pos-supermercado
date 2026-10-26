package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.dominio.Turno;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.CatalogoService;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;

/** Pantalla 2: selección de caja y apertura de turno. */
public class AperturaTurnoFrame extends JFrame {
    private final TurnoService turnoService = new TurnoService();
    private final JComboBox<Caja> cmbCaja = new JComboBox<>();
    private final JTextField txtFondo = new JTextField("0.00", 12);

    public AperturaTurnoFrame() {
        super("Apertura de turno");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Usuario u = Sesion.getUsuario();
        List<Caja> libres = new CatalogoService().cajasLibres();
        libres.forEach(cmbCaja::addItem);

        JButton btnAbrir = new JButton("Abrir turno");
        btnAbrir.setEnabled(!libres.isEmpty());
        JLabel lblResumen = new JLabel("<html>Cajero: " + u.getNombreCompleto() + "<br>Fecha y hora: "
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</html>");
        JLabel lblAyuda = new JLabel(libres.isEmpty() ? "No hay cajas libres" : "Cajas libres: " + libres.size());

        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.add(lblResumen, BorderLayout.NORTH);
        p.add(Ui.form("Caja:", cmbCaja, "Efectivo inicial (Q):", txtFondo, "", lblAyuda), BorderLayout.CENTER);
        p.add(btnAbrir, BorderLayout.SOUTH);
        add(Ui.conMargen(p));

        btnAbrir.addActionListener(e -> {
            try {
                Caja caja = (Caja) cmbCaja.getSelectedItem();
                BigDecimal fondo = Ui.monto(txtFondo.getText());   // captura monto inválido (mensaje claro)
                Turno t = turnoService.abrir(u, caja.getId(), fondo);
                Sesion.setTurno(t);
                dispose();
                if (u.tienePermiso("VENTA_COBRAR")) new PosFrame().setVisible(true);
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        });
        pack();
        setLocationRelativeTo(null);
    }
}
