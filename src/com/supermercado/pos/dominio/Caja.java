package com.supermercado.pos.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Caja {
    private Long id;
    private Integer numero;
    private String nombre;
    private boolean activa = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override public String toString() { return "Caja " + numero + (nombre == null ? "" : " - " + nombre); }
}
