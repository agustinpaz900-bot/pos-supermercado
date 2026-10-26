package com.supermercado.pos.ui;

import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.util.Dinero;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.math.BigDecimal;
import javax.swing.*;

/** Pequeñas ayudas para no repetir código en las ventanas. */
public final class Ui {
    private Ui() {}

    public static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void aviso(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Aviso", JOptionPane.ERROR_MESSAGE);
    }

    /** Muestra el mensaje adecuado según el tipo de error. */
    public static void error(Component padre, RuntimeException e) {
        if (e instanceof ReglaNegocioException || e instanceof AutorizacionException) aviso(padre, e.getMessage());
        else if (e instanceof IllegalStateException) aviso(padre, e.getMessage() == null ? "Error de base de datos" : e.getMessage());
        else {
            e.printStackTrace();
            aviso(padre, "Error inesperado: " + e);
        }
    }

    public static BigDecimal monto(String texto) { return Dinero.parsear(texto); }

    public static String q(BigDecimal v) { return Dinero.q(v); }

    /** Panel de formulario: etiqueta, componente, etiqueta, componente... */
    public static JPanel form(Object... elementos) {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        for (Object o : elementos) {
            if (o instanceof String s) p.add(new JLabel(s));
            else p.add((Component) o);
        }
        return p;
    }

    public static JPanel conMargen(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    public static JTextArea areaMonoespaciada(String texto, int filas, int columnas) {
        JTextArea a = new JTextArea(texto, filas, columnas);
        a.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        a.setEditable(false);
        return a;
    }
}
