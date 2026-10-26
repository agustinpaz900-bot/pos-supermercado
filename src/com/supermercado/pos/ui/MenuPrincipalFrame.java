package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/** Menú principal: cada botón se habilita solo si el usuario tiene el permiso. */
public class MenuPrincipalFrame extends JFrame {
    private final JLabel lblEstado = new JLabel();
    private final List<Runnable> actualizadores = new ArrayList<>();

    public MenuPrincipalFrame() {
        super("POS Supermercado - Menú principal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel botones = new JPanel(new GridLayout(0, 2, 10, 10));

        agregar(botones, "Abrir turno", "TURNO_ABRIR", false, this::abrirTurno);
        agregar(botones, "Punto de venta", "VENTA_COBRAR", false, this::irAVentas);
        agregar(botones, "Corte intermedio", "CORTE_INTERMEDIO", true, () -> new CorteIntermedioDialog(this).setVisible(true));
        agregar(botones, "Reversión", "REVERSION_SOLICITAR", true, () -> new ReversionFrame().setVisible(true));
        agregar(botones, "Cerrar turno", "TURNO_CERRAR", true, () -> new CierreTurnoFrame().setVisible(true));
        agregar(botones, "Usuarios", "USUARIO_ADMIN", false, () -> new UsuariosFrame().setVisible(true));
        agregar(botones, "Roles y permisos", "ROL_ADMIN", false, () -> new RolesFrame().setVisible(true));
        agregar(botones, "Productos", "CATALOGO_ADMIN", false, () -> new ProductosFrame().setVisible(true));
        agregar(botones, "Cajas", "CATALOGO_ADMIN", false, () -> new CajasFrame().setVisible(true));

        JButton salir = new JButton("Cerrar sesión");
        salir.addActionListener(e -> {
            Sesion.cerrar();
            dispose();
            new LoginFrame().setVisible(true);
        });
        botones.add(salir);

        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.add(lblEstado, BorderLayout.NORTH);
        raiz.add(botones, BorderLayout.CENTER);
        add(Ui.conMargen(raiz));

        addWindowListener(new WindowAdapter() {
            @Override public void windowActivated(WindowEvent e) { refrescar(); }
        });
        refrescar();
        pack();
        setSize(Math.max(getWidth(), 480), getHeight());
        setLocationRelativeTo(null);
    }

    /** requiereTurno = true: además del permiso, el botón necesita un turno abierto. */
    private void agregar(JPanel panel, String texto, String permiso, boolean requiereTurno, Runnable accion) {
        Usuario u = Sesion.getUsuario();
        if (!u.tienePermiso(permiso)) return;   // sin permiso: el botón ni aparece
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            try {
                accion.run();
            } catch (RuntimeException ex) {
                Ui.error(this, ex);
            }
        });
        panel.add(b);
        actualizadores.add(() -> b.setEnabled(!requiereTurno || Sesion.getTurno() != null));
    }

    private void refrescar() {
        Usuario u = Sesion.getUsuario();
        if (u == null) return;
        String turno = Sesion.getTurno() == null ? "sin turno abierto"
                : "Caja " + Sesion.getTurno().getCaja().getNumero() + " (turno abierto)";
        lblEstado.setText("Usuario: " + u.getNombreCompleto() + "  |  " + turno);
        actualizadores.forEach(Runnable::run);
    }

    private void abrirTurno() {
        if (Sesion.getTurno() != null) Ui.info(this, "Ya tienes un turno abierto");
        else new AperturaTurnoFrame().setVisible(true);
    }

    private void irAVentas() {
        if (Sesion.getTurno() != null) new PosFrame().setVisible(true);
        else new AperturaTurnoFrame().setVisible(true);
    }
}
