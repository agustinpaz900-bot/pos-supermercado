package com.supermercado.pos.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Permiso {
    private Long id;
    private String codigo;
    private String descripcion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override public boolean equals(Object o) { return o instanceof Permiso p && Objects.equals(codigo, p.codigo); }
    @Override public int hashCode() { return Objects.hashCode(codigo); }
    @Override public String toString() { return codigo; }
}
