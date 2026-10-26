package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.*;
import com.supermercado.pos.servicio.VentaService;
import com.supermercado.pos.util.Dinero;
import com.supermercado.pos.util.Sesion;
import com.supermercado.pos.util.Ticket;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Pantalla 3: punto de venta (carrito). */
public class PosFrame extends JFrame {
    private final VentaService ventaService = new VentaService();
    private final Carrito carrito = new Carrito();
    private final JTextField txtCodigo = new JTextField(20);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Producto", "Cantidad", "Precio", "Subtotal"}, 0) {
        @Override public boolean isCellEditable(int fila, int col) { return col == 1; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JLabel lblSubtotal = new JLabel();
    private final JLabel lblImpuesto = new JLabel();
    private final JLabel lblTotal = new JLabel();
    private final List<Long> idsFila = new ArrayList<>();
    private boolean actualizando = false;

    public PosFrame() {
        super("Punto de venta - Caja " + Sesion.getTurno().getCaja().getNumero());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(850, 560);
        setLocationRelativeTo(null);

        // Barra superior: código de barras (un lector funciona como teclado que termina con Enter)
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Código de barras:"));
        norte.add(txtCodigo);
        JButton btnQuitar = new JButton("Quitar fila");
        norte.add(btnQuitar);

        // Totales
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 26f));
        JPanel totales = new JPanel(new GridLayout(3, 1));
        totales.add(lblSubtotal);
        totales.add(lblImpuesto);
        totales.add(lblTotal);

        // Botones
        JButton btnCobrar = new JButton("Cobrar (F12)");
        JButton btnCancelar = new JButton("Cancelar venta");
        JButton btnReversion = new JButton("Reversión");
        JButton btnCorte = new JButton("Corte intermedio");
        JButton btnCerrar = new JButton("Cerrar turno");
        JPanel botones = new JPanel(new GridLayout(0, 1, 6, 6));
        botones.add(btnCobrar);
        botones.add(btnCancelar);
        botones.add(btnReversion);
        botones.add(btnCorte);
        botones.add(btnCerrar);

        JPanel derecha = new JPanel(new BorderLayout(8, 8));
        derecha.add(totales, BorderLayout.NORTH);
        derecha.add(botones, BorderLayout.SOUTH);

        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(norte, BorderLayout.NORTH);
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(derecha, BorderLayout.EAST);
        add(Ui.conMargen(raiz));

        txtCodigo.addActionListener(e -> agregarPorCodigo());
        btnQuitar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila >= 0) {
                carrito.quitar(idsFila.get(fila));
                refrescar();
            }
        });
        btnCobrar.addActionListener(e -> cobrar());
        btnCancelar.addActionListener(e -> {
            if (!carrito.estaVacio() && JOptionPane.showConfirmDialog(this, "¿Cancelar la venta actual?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                carrito.vaciar();
                refrescar();
            }
        });
        btnReversion.addActionListener(e -> new ReversionFrame().setVisible(true));
        btnCorte.addActionListener(e -> new CorteIntermedioDialog(this).setVisible(true));
        btnCerrar.addActionListener(e -> new CierreTurnoFrame().setVisible(true));

        // Atajo F12 para cobrar
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "cobrar");
        getRootPane().getActionMap().put("cobrar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { cobrar(); }
        });

        // Cuando el usuario edita la cantidad en la tabla
        modelo.addTableModelListener(e -> {
            if (actualizando || e.getType() != javax.swing.event.TableModelEvent.UPDATE || e.getColumn() != 1) return;
            int fila = e.getFirstRow();
            try {
                int n = Integer.parseInt(String.valueOf(modelo.getValueAt(fila, 1)).trim());
                if (n < 1) throw new NumberFormatException();
                carrito.fijarCantidad(idsFila.get(fila), n);
            } catch (NumberFormatException ex) {
                Ui.aviso(this, "La cantidad debe ser un número entero mayor que 0");
            }
            SwingUtilities.invokeLater(this::refrescar);
        });

        // Si el turno se cerró desde otra ventana, esta ventana ya no sirve
        addWindowListener(new WindowAdapter() {
            @Override public void windowActivated(WindowEvent e) {
                if (Sesion.getTurno() == null) dispose();
                else txtCodigo.requestFocusInWindow();
            }
        });
        refrescar();
    }

    private void agregarPorCodigo() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isEmpty()) return;
        try {
            Optional<Producto> p = ventaService.buscarProducto(codigo);
            if (p.isEmpty()) Ui.aviso(this, "No existe un producto con el código " + codigo);
            else carrito.agregar(p.get(), 1);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
        txtCodigo.setText("");
        refrescar();
    }

    private void refrescar() {
        actualizando = true;
        modelo.setRowCount(0);
        idsFila.clear();
        for (LineaCarrito l : carrito.lineas()) {
            idsFila.add(l.producto().getId());
            modelo.addRow(new Object[]{l.producto().getNombre(), l.cantidad(),
                    Dinero.q(l.producto().getPrecio()), Dinero.q(l.subtotal())});
        }
        actualizando = false;
        lblSubtotal.setText("Subtotal: " + Dinero.q(carrito.subtotalSinImpuesto()));
        lblImpuesto.setText("Impuesto incluido: " + Dinero.q(carrito.impuesto()));
        lblTotal.setText("Total: " + Dinero.q(carrito.total()));
        txtCodigo.requestFocusInWindow();
    }

    private void cobrar() {
        if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();
        if (carrito.estaVacio()) {
            Ui.aviso(this, "El carrito está vacío");
            return;
        }
        PagoDialog dialogo = new PagoDialog(this, carrito.total());
        dialogo.setVisible(true);
        PagoDto pago = dialogo.getResultado();
        if (pago == null) return;
        try {
            Venta v = ventaService.cobrar(Sesion.getTurno(), carrito, pago);
            mostrarTicket(v);
            carrito.vaciar();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
        refrescar();
    }

    private void mostrarTicket(Venta v) {
        JTextArea area = Ui.areaMonoespaciada(Ticket.texto(v), 22, 36);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Ticket - folio " + v.getId(),
                JOptionPane.PLAIN_MESSAGE);
    }
}
