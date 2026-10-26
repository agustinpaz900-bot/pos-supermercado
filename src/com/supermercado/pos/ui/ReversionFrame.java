package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.*;
import com.supermercado.pos.servicio.AutorizacionService;
import com.supermercado.pos.servicio.ReversionService;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Pantalla 6: reversión (buscar venta, elegir producto y cantidad, pedir autorización). */
public class ReversionFrame extends JFrame {
    private final ReversionService reversionService = new ReversionService();
    private final AutorizacionService autorizacionService = new AutorizacionService();
    private final JTextField txtFolio = new JTextField(10);
    private final JLabel lblVenta = new JLabel(" ");
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Producto", "Vendida", "Reversada", "Disponible", "Precio"}, 0) {
        @Override public boolean isCellEditable(int f, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JSpinner spCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JTextField txtMotivo = new JTextField(25);
    private List<VentaDetalle> detalles = new ArrayList<>();
    private Venta ventaActual;

    public ReversionFrame() {
        super("Reversión de producto");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 460);
        setLocationRelativeTo(null);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton btnBuscar = new JButton("Buscar venta");
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Folio de la venta:"));
        norte.add(txtFolio);
        norte.add(btnBuscar);
        norte.add(lblVenta);

        JButton btnSolicitar = new JButton("Solicitar reversión (requiere supervisor)");
        JPanel sur = new JPanel(new BorderLayout(8, 8));
        sur.add(Ui.form("Cantidad a reversar:", spCantidad, "Motivo (obligatorio):", txtMotivo), BorderLayout.CENTER);
        sur.add(btnSolicitar, BorderLayout.SOUTH);

        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(norte, BorderLayout.NORTH);
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(sur, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));

        getRootPane().setDefaultButton(btnBuscar);
        btnBuscar.addActionListener(e -> buscar());
        btnSolicitar.addActionListener(e -> solicitar());
    }

    private void buscar() {
        try {
            long folio;
            try {
                folio = Long.parseLong(txtFolio.getText().trim());
            } catch (NumberFormatException ex) {
                Ui.aviso(this, "El folio debe ser un número");
                return;
            }
            ventaActual = reversionService.buscarVenta(folio, Sesion.getTurno());
            detalles = ventaActual.getDetalles();
            modelo.setRowCount(0);
            for (VentaDetalle d : detalles)
                modelo.addRow(new Object[]{d.getProducto().getNombre(), d.getCantidad(), d.getCantidadReversada(),
                        d.disponibleParaReversar(), Dinero.q(d.getPrecioUnitario())});
            lblVenta.setText("Venta " + ventaActual.getId() + " - " + Dinero.q(ventaActual.getTotal())
                    + " - " + ventaActual.getEstado());
        } catch (RuntimeException ex) {
            modelo.setRowCount(0);
            detalles = new ArrayList<>();
            lblVenta.setText(" ");
            Ui.error(this, ex);
        }
    }

    private void solicitar() {
        try {
            int fila = tabla.getSelectedRow();
            if (fila < 0) throw new com.supermercado.pos.excepcion.ReglaNegocioException("Selecciona un producto de la tabla");
            VentaDetalle d = detalles.get(fila);
            int cantidad = (Integer) spCantidad.getValue();
            ReversionService.validarCantidad(d.getCantidad(), d.getCantidadReversada(), cantidad);
            String motivo = txtMotivo.getText().trim();
            if (motivo.isEmpty()) throw new com.supermercado.pos.excepcion.ReglaNegocioException("El motivo es obligatorio");

            BigDecimal monto = d.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
            Usuario cajero = Sesion.getUsuario();
            String resumen = "Producto : " + d.getProducto().getNombre() + "\nCantidad : " + cantidad
                    + "\nMonto    : " + Dinero.q(monto) + "\nSolicita : " + cajero.getNombreCompleto()
                    + "\nMotivo   : " + motivo;
            AutorizacionDialog dlg = new AutorizacionDialog(this, "Autorización del supervisor", resumen,
                    (u, c) -> autorizacionService.validarSupervisor(u, c, cajero));
            dlg.setVisible(true);
            Usuario supervisor = dlg.getResultado();
            if (supervisor == null) return;

            reversionService.revertir(d.getId(), cantidad, motivo, Sesion.getTurno(), cajero, supervisor);
            Ui.info(this, "Reversión registrada. Devuelva " + Dinero.q(monto) + " al cliente.\nAutorizó: "
                    + supervisor.getNombreCompleto());
            txtMotivo.setText("");
            buscar();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }
}
