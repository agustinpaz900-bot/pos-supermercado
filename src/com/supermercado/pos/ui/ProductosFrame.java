package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Producto;
import com.supermercado.pos.servicio.CatalogoService;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Pantalla 12 (apoyo): catálogo de productos. */
public class ProductosFrame extends JFrame {
    private final CatalogoService servicio = new CatalogoService();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Código", "Nombre", "Precio", "IVA %", "Stock", "Activo"}, 0) {
        @Override public boolean isCellEditable(int f, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private List<Producto> productos = new ArrayList<>();

    public ProductosFrame() {
        super("Productos");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);
        JButton btnNuevo = new JButton("Nuevo");
        JButton btnEditar = new JButton("Editar");
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.add(btnNuevo);
        sur.add(btnEditar);
        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(sur, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));
        btnNuevo.addActionListener(e -> editar(null));
        btnEditar.addActionListener(e -> {
            int f = tabla.getSelectedRow();
            if (f < 0) Ui.aviso(this, "Selecciona un producto");
            else editar(productos.get(f));
        });
        cargar();
    }

    private void cargar() {
        try {
            productos = servicio.productos();
            modelo.setRowCount(0);
            for (Producto p : productos)
                modelo.addRow(new Object[]{p.getCodigoBarras(), p.getNombre(), Dinero.q(p.getPrecio()),
                        p.getPorcentajeImpuesto(), p.getStock(), p.isActivo() ? "Sí" : "No"});
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void editar(Producto original) {
        Producto p = original == null ? new Producto() : original;
        JTextField txtCodigo = new JTextField(p.getCodigoBarras() == null ? "" : p.getCodigoBarras());
        JTextField txtNombre = new JTextField(p.getNombre() == null ? "" : p.getNombre());
        JTextField txtPrecio = new JTextField(p.getPrecio() == null ? "" : p.getPrecio().toPlainString());
        JTextField txtImpuesto = new JTextField(p.getPorcentajeImpuesto() == null ? "12.00" : p.getPorcentajeImpuesto().toPlainString());
        JTextField txtStock = new JTextField(String.valueOf(p.getStock()));
        JCheckBox chkActivo = new JCheckBox("Activo", original == null || p.isActivo());
        int ok = JOptionPane.showConfirmDialog(this, Ui.form("Código de barras:", txtCodigo, "Nombre:", txtNombre,
                        "Precio (Q):", txtPrecio, "Impuesto (%):", txtImpuesto, "Stock:", txtStock, "", chkActivo),
                original == null ? "Nuevo producto" : "Editar producto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            p.setCodigoBarras(txtCodigo.getText().trim());
            p.setNombre(txtNombre.getText().trim());
            p.setPrecio(Ui.monto(txtPrecio.getText()));
            p.setPorcentajeImpuesto(new BigDecimal(txtImpuesto.getText().trim()).setScale(2, java.math.RoundingMode.HALF_UP));
            p.setStock(Integer.parseInt(txtStock.getText().trim()));
            p.setActivo(chkActivo.isSelected());
            servicio.guardarProducto(Sesion.getUsuario(), p);
            cargar();
        } catch (NumberFormatException ex) {
            Ui.aviso(this, "Revisa los números (impuesto y stock deben ser numéricos)");
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }
}
