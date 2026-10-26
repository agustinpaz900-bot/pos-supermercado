package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.servicio.RolService;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Pantalla 10: roles y matriz de permisos agrupada por módulo. */
public class RolesFrame extends JFrame {
    private final RolService rolService = new RolService();
    private final DefaultListModel<Rol> listaModelo = new DefaultListModel<>();
    private final JList<Rol> lista = new JList<>(listaModelo);
    private final Map<String, JCheckBox> casillas = new LinkedHashMap<>();
    private List<Permiso> permisos = new ArrayList<>();

    public RolesFrame() {
        super("Roles y permisos");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(760, 520);
        setLocationRelativeTo(null);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        permisos = rolService.permisos();
        // Agrupa cada permiso en su módulo
        Map<String, JPanel> modulos = new LinkedHashMap<>();
        for (String m : new String[]{"Venta", "Turno", "Reversión", "Administración"}) {
            JPanel p = new JPanel(new GridLayout(0, 1));
            p.setBorder(BorderFactory.createTitledBorder(m));
            modulos.put(m, p);
        }
        for (Permiso p : permisos) {
            JCheckBox cb = new JCheckBox(p.getCodigo() + (p.getDescripcion() == null ? "" : " - " + p.getDescripcion()));
            casillas.put(p.getCodigo(), cb);
            modulos.get(moduloDe(p.getCodigo())).add(cb);
        }
        JPanel matriz = new JPanel(new GridLayout(0, 1));
        modulos.values().forEach(matriz::add);

        JButton btnNuevo = new JButton("Nuevo rol");
        JButton btnGuardar = new JButton("Guardar permisos del rol");
        JPanel izq = new JPanel(new BorderLayout(6, 6));
        izq.add(new JScrollPane(lista), BorderLayout.CENTER);
        izq.add(btnNuevo, BorderLayout.SOUTH);
        izq.setPreferredSize(new Dimension(200, 0));

        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(izq, BorderLayout.WEST);
        raiz.add(new JScrollPane(matriz), BorderLayout.CENTER);
        raiz.add(btnGuardar, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));

        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarPermisosDelRol();
        });
        btnNuevo.addActionListener(e -> nuevoRol());
        btnGuardar.addActionListener(e -> guardar());
        cargarRoles();
    }

    private static String moduloDe(String codigo) {
        if (codigo.startsWith("VENTA")) return "Venta";
        if (codigo.startsWith("TURNO") || codigo.startsWith("CORTE")) return "Turno";
        if (codigo.startsWith("REVERSION")) return "Reversión";
        return "Administración";
    }

    private void cargarRoles() {
        try {
            listaModelo.clear();
            rolService.listar().forEach(listaModelo::addElement);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void mostrarPermisosDelRol() {
        Rol r = lista.getSelectedValue();
        casillas.forEach((codigo, cb) -> cb.setSelected(r != null
                && r.getPermisos().stream().anyMatch(p -> p.getCodigo().equals(codigo))));
    }

    private void nuevoRol() {
        JTextField txtNombre = new JTextField();
        JTextField txtNivel = new JTextField("1");
        int ok = JOptionPane.showConfirmDialog(this, Ui.form("Nombre del rol:", txtNombre, "Nivel jerárquico (número):", txtNivel),
                "Nuevo rol", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            int nivel;
            try {
                nivel = Integer.parseInt(txtNivel.getText().trim());
            } catch (NumberFormatException ex) {
                Ui.aviso(this, "El nivel debe ser un número entero");
                return;
            }
            rolService.crear(Sesion.getUsuario(), txtNombre.getText(), nivel);
            cargarRoles();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }

    private void guardar() {
        Rol r = lista.getSelectedValue();
        if (r == null) {
            Ui.aviso(this, "Selecciona un rol de la lista");
            return;
        }
        try {
            Set<Long> ids = new HashSet<>();
            for (Permiso p : permisos)
                if (casillas.get(p.getCodigo()).isSelected()) ids.add(p.getId());
            rolService.guardarPermisos(Sesion.getUsuario(), r.getId(), ids);
            Ui.info(this, "Permisos guardados. Los usuarios con este rol los recibirán en su próximo inicio de sesión.");
            int idx = lista.getSelectedIndex();
            cargarRoles();
            lista.setSelectedIndex(idx);
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }
}
