package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.servicio.AuthService;
import com.supermercado.pos.servicio.TurnoService;
import com.supermercado.pos.util.Sesion;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Pantalla 1: Login (público). */
public class LoginFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final TurnoService turnoService = new TurnoService();
    private final JTextField txtUsuario = new JTextField(16);
    private final JPasswordField txtClave = new JPasswordField(16);
    private final JLabel lblError = new JLabel(" ");

    public LoginFrame() {
        super("POS Supermercado - Iniciar sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JButton btnIngresar = new JButton("Ingresar");
        lblError.setForeground(Color.RED);

        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(Ui.form("Usuario:", txtUsuario, "Contraseña:", txtClave), BorderLayout.CENTER);
        JPanel sur = new JPanel(new GridLayout(2, 1, 4, 4));
        sur.add(lblError);
        sur.add(btnIngresar);
        centro.add(sur, BorderLayout.SOUTH);
        add(Ui.conMargen(centro));
        getRootPane().setDefaultButton(btnIngresar);   // Enter activa el botón

        btnIngresar.addActionListener(e -> {
            char[] clave = txtClave.getPassword();
            try {
                Usuario u = authService.autenticar(txtUsuario.getText().trim(), clave);
                Sesion.setUsuario(u);
                // Si el cajero ya tenía un turno abierto (cerró el programa sin cerrar turno) se retoma
                Sesion.setTurno(turnoService.buscarAbierto(u).orElse(null));
                dispose();
                new MenuPrincipalFrame().setVisible(true);
            } catch (AutorizacionException ex) {
                lblError.setText(ex.getMessage());
            } catch (IllegalStateException ex) {
                lblError.setText("No se pudo conectar a la base de datos");
                Ui.error(this, ex);
            } finally {
                Arrays.fill(clave, '\0');   // no dejes la contraseña en memoria
                txtClave.setText("");
            }
        });
        pack();
        setLocationRelativeTo(null);
    }
}
