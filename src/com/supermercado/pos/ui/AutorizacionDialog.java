package com.supermercado.pos.ui;

import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/**
 * Pantalla 7: pide usuario y contraseña de OTRA persona (supervisor). No es un login:
 * la sesión del cajero no cambia. Devuelve el Usuario validado o null si se cancela.
 */
public class AutorizacionDialog extends JDialog {
    /** Quien abre el diálogo decide cómo se valida (supervisor de reversión, receptor de bóveda...). */
    public interface Validador {
        Usuario validar(String username, char[] clave);
    }

    private static final int MAX_INTENTOS = 3;
    private final JTextField txtUsuario = new JTextField(14);
    private final JPasswordField txtClave = new JPasswordField(14);
    private final JLabel lblError = new JLabel(" ");
    private final Validador validador;
    private int intentos = 0;
    private Usuario resultado;

    public AutorizacionDialog(Window owner, String titulo, String resumen, Validador validador) {
        super(owner, titulo, ModalityType.APPLICATION_MODAL);
        this.validador = validador;
        lblError.setForeground(Color.RED);
        JButton btnOk = new JButton("Autorizar");
        JButton btnCancelar = new JButton("Cancelar");

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnOk);
        JPanel centro = new JPanel(new BorderLayout(8, 8));
        centro.add(Ui.form("Usuario del supervisor:", txtUsuario, "Contraseña:", txtClave), BorderLayout.CENTER);
        centro.add(lblError, BorderLayout.SOUTH);
        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.add(Ui.areaMonoespaciada(resumen, 6, 38), BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        add(Ui.conMargen(raiz));
        getRootPane().setDefaultButton(btnOk);

        btnCancelar.addActionListener(e -> dispose());
        btnOk.addActionListener(e -> intentar());
        pack();
        setLocationRelativeTo(owner);
    }

    private void intentar() {
        char[] clave = txtClave.getPassword();
        try {
            resultado = validador.validar(txtUsuario.getText().trim(), clave);
            dispose();
        } catch (AutorizacionException ex) {
            intentos++;
            if (intentos >= MAX_INTENTOS) {
                Ui.aviso(this, "Demasiados intentos fallidos. Autorización cancelada (quedó en la bitácora).");
                resultado = null;
                dispose();
            } else {
                lblError.setText(ex.getMessage() + " (intento " + intentos + " de " + MAX_INTENTOS + ")");
            }
        } catch (RuntimeException ex) {
            Ui.error(this, ex);
        } finally {
            Arrays.fill(clave, '\0');   // nunca dejar la contraseña del supervisor en memoria
            txtClave.setText("");
        }
    }

    /** null si se canceló o se agotaron los intentos. */
    public Usuario getResultado() { return resultado; }
}
