package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.RolService;
import com.supermercado.pos.servicio.UsuarioService;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Pantalla 9: lista y alta de usuarios. */
public class UsuariosFrame extends JFrame {
    private final UsuarioService usuarioService = new UsuarioService();
    private final RolService rolService = new RolService();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Usuario", "Nombre", "Estado", "Roles"}, 0) {
        @Override public boolean isCellEditable(int f, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
    private final JTextField txtBuscar = new JTextField(18);
    private List<Usuario> usuarios = new ArrayList<>();

    public UsuariosFrame() {
        super("Usuarios");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(760, 460);
        setLocationRelativeTo(null);
        tabla.setRowSorter(sorter);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Buscar:"));
        norte.add(txtBuscar);
        txtBuscar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void filtrar() {
                String t = txtBuscar.getText().trim();
                sorter.setRowFilter(t.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(t)));
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrar(); }
        });

        JButton btnNuevo = new JButton("Nuevo usuario");
        JButton btnEditar = new JButton("Editar");
        JButton btnEstado = new JButton("Activar / Desactivar");
        JButton btnAsignar = new JButton("Roles y permisos...");
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.add(btnNuevo);
        sur.add(btnEditar);
        sur.add(btnEstado);
        sur.add(btnAsignar);

        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(norte, BorderLayout.NORTH);
        raiz.add(new JScrollPane(tabla), BorderLayout.CENTER);
        raiz.add(sur, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));

        btnNuevo.addActionListener(e -> nuevo());
        btnEditar.addActionListener(e -> editar());
        btnEstado.addActionListener(e -> cambiarEstado());
        btnAsignar.addActionListener(e -> asignar());
        cargar();
    }

    private void cargar() {
        try {
            usuarios = usuarioService.listar();
            modelo.setRowCount(0);
            for (Usuario u : usuarios) {
                StringJoiner roles = new StringJoiner(", ");
                u.getRoles().forEach(r -> roles.add(r.getNombre()));
                modelo.addRow(new Object[]{u.getId(), u.getUsername(), u.getNombreCompleto(),
                        u.isActivo() ? "Activo" : "Inactivo", roles.toString()});
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private Usuario seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Ui.aviso(this, "Selecciona un usuario de la tabla");
            return null;
        }
        return usuarios.get(tabla.convertRowIndexToModel(fila));
    }

    private void nuevo() {
        try {
            JTextField txtUser = new JTextField();
            JTextField txtNombre = new JTextField();
            JPasswordField txtClave = new JPasswordField();
            JComboBox<Rol> cmbRol = new JComboBox<>();
            rolService.listar().forEach(cmbRol::addItem);
            int ok = JOptionPane.showConfirmDialog(this,
                    Ui.form("Usuario (sin espacios):", txtUser, "Nombre completo:", txtNombre,
                            "Contraseña inicial (mín. 8):", txtClave, "Rol:", cmbRol),
                    "Nuevo usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            char[] clave = txtClave.getPassword();
            try {
                Rol rol = (Rol) cmbRol.getSelectedItem();
                usuarioService.crear(Sesion.getUsuario(), txtUser.getText(), txtNombre.getText(), clave,
                        Set.of(rol.getId()));
            } finally {
                Arrays.fill(clave, '\0');
            }
            cargar();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void editar() {
        Usuario u = seleccionado();
        if (u == null) return;
        try {
            JTextField txtNombre = new JTextField(u.getNombreCompleto());
            JPasswordField txtClave = new JPasswordField();
            int ok = JOptionPane.showConfirmDialog(this,
                    Ui.form("Usuario:", new JLabel(u.getUsername()), "Nombre completo:", txtNombre,
                            "Nueva contraseña (vacío = no cambiar):", txtClave),
                    "Editar usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return;
            char[] clave = txtClave.getPassword();
            try {
                usuarioService.actualizar(Sesion.getUsuario(), u.getId(), txtNombre.getText(), u.isActivo(), clave);
            } finally {
                Arrays.fill(clave, '\0');
            }
            cargar();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void cambiarEstado() {
        Usuario u = seleccionado();
        if (u == null) return;
        try {
            usuarioService.actualizar(Sesion.getUsuario(), u.getId(), u.getNombreCompleto(), !u.isActivo(), null);
            cargar();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void asignar() {
        Usuario u = seleccionado();
        if (u == null) return;
        new AsignacionPermisosDialog(this, u).setVisible(true);
        cargar();
    }
}
