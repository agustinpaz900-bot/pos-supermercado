package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Caja;
import com.supermercado.pos.servicio.CatalogoService;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Pantalla 12 (apoyo): catálogo de cajas. */
public class CajasFrame extends JFrame {
    private final CatalogoService servicio = new CatalogoService();
    private final DefaultTableModel modelo = new DefaultTableModel(new Object[]{"Número", "Nombre", "Activa"}, 0) {
        @Override public boolean isCellEditable(int f, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private List<Caja> cajas = new ArrayList<>();

    public CajasFrame() {
        super("Cajas");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(480, 360);
        setLocationRelativeTo(null);
        JButton btnNueva = new JButton("Nueva");
        JButton btnEditar = new JButton("Editar");
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.add(btnNueva);
        sur.add(btnEditar);
        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(sur, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));
        btnNueva.addActionListener(e -> editar(null));
        btnEditar.addActionListener(e -> {
            int f = tabla.getSelectedRow();
            if (f < 0) Ui.aviso(this, "Selecciona una caja");
            else editar(cajas.get(f));
        });
        cargar();
    }

    private void cargar() {
        try {
            cajas = servicio.cajas();
            modelo.setRowCount(0);
            for (Caja c : cajas) modelo.addRow(new Object[]{c.getNumero(), c.getNombre(), c.isActiva() ? "Sí" : "No"});
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void editar(Caja original) {
        Caja c = original == null ? new Caja() : original;
        JTextField txtNumero = new JTextField(c.getNumero() == null ? "" : String.valueOf(c.getNumero()));
        JTextField txtNombre = new JTextField(c.getNombre() == null ? "" : c.getNombre());
        JCheckBox chkActiva = new JCheckBox("Activa", original == null || c.isActiva());
        int ok = JOptionPane.showConfirmDialog(this, Ui.form("Número:", txtNumero, "Nombre:", txtNombre, "", chkActiva),
                original == null ? "Nueva caja" : "Editar caja", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            c.setNumero(Integer.parseInt(txtNumero.getText().trim()));
            c.setNombre(txtNombre.getText().trim());
            c.setActiva(chkActiva.isSelected());
            servicio.guardarCaja(Sesion.getUsuario(), c);
            cargar();
        } catch (NumberFormatException ex) {
            Ui.aviso(this, "El número de caja debe ser un número entero");
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }
}
