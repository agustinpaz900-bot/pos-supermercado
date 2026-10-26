package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Permiso;
import com.supermercado.pos.dominio.Rol;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.servicio.RolService;
import com.supermercado.pos.servicio.UsuarioService;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Pantalla 11: asignación de roles y permisos extra a un usuario. */
public class AsignacionPermisosDialog extends JDialog {
    private final UsuarioService usuarioService = new UsuarioService();
    private final Usuario objetivo;
    private final Map<Rol, JCheckBox> cajasRol = new LinkedHashMap<>();
    private final Map<Permiso, JCheckBox> cajasPermiso = new LinkedHashMap<>();
    private final Set<String> extras = new HashSet<>();

    public AsignacionPermisosDialog(Window owner, Usuario objetivo) {
        super(owner, "Roles y permisos de " + objetivo.getUsername(), ModalityType.APPLICATION_MODAL);
        this.objetivo = objetivo;
        RolService rolService = new RolService();
        List<Rol> roles = rolService.listar();
        List<Permiso> permisos = rolService.permisos();
        objetivo.getPermisosExtra().forEach(p -> extras.add(p.getCodigo()));

        JPanel panelRoles = new JPanel(new GridLayout(0, 1));
        panelRoles.setBorder(BorderFactory.createTitledBorder("Roles"));
        for (Rol r : roles) {
            JCheckBox cb = new JCheckBox(r.toString(), objetivo.getRoles().contains(r));
            cb.addActionListener(e -> recalcular());
            cajasRol.put(r, cb);
            panelRoles.add(cb);
        }
        JPanel panelPermisos = new JPanel(new GridLayout(0, 1));
        panelPermisos.setBorder(BorderFactory.createTitledBorder("Permisos (en gris: los da el rol; marca para sumar permisos extra)"));
        for (Permiso p : permisos) {
            JCheckBox cb = new JCheckBox(p.getCodigo());
            cb.addActionListener(e -> {
                if (cb.isSelected()) extras.add(p.getCodigo());
                else extras.remove(p.getCodigo());
            });
            cajasPermiso.put(p, cb);
            panelPermisos.add(cb);
        }
        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        JPanel centro = new JPanel(new GridLayout(1, 2, 8, 8));
        centro.add(new JScrollPane(panelRoles));
        centro.add(new JScrollPane(panelPermisos));
        JPanel raiz = new JPanel(new BorderLayout(8, 8));
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));

        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> guardar());
        recalcular();
        setSize(640, 420);
        setLocationRelativeTo(owner);
    }

    /** Muestra los permisos efectivos: los que da algún rol quedan marcados y bloqueados. */
    private void recalcular() {
        Set<String> porRol = new HashSet<>();
        cajasRol.forEach((rol, cb) -> {
            if (cb.isSelected()) rol.getPermisos().forEach(p -> porRol.add(p.getCodigo()));
        });
        cajasPermiso.forEach((permiso, cb) -> {
            boolean delRol = porRol.contains(permiso.getCodigo());
            cb.setEnabled(!delRol);
            cb.setSelected(delRol || extras.contains(permiso.getCodigo()));
            cb.setForeground(delRol ? Color.GRAY : Color.BLACK);
        });
    }

    private void guardar() {
        try {
            Set<Long> rolIds = new HashSet<>();
            cajasRol.forEach((rol, cb) -> {
                if (cb.isSelected()) rolIds.add(rol.getId());
            });
            Set<Long> extraIds = new HashSet<>();
            cajasPermiso.forEach((p, cb) -> {
                if (cb.isEnabled() && cb.isSelected()) extraIds.add(p.getId());
            });
            usuarioService.asignar(Sesion.getUsuario(), objetivo.getId(), rolIds, extraIds);
            Ui.info(this, "Guardado. Los cambios aplican en el próximo inicio de sesión de " + objetivo.getUsername() + ".");
            dispose();
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        }
    }
}
