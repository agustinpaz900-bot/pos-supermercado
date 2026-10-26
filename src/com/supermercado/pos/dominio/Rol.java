package com.supermercado.pos.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Rol {
    private Long id;
    private String nombre;
    private int nivel;
    private Set<Permiso> permisos = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }
    public Set<Permiso> getPermisos() { return permisos; }
    public void setPermisos(Set<Permiso> permisos) { this.permisos = permisos; }

    @Override public boolean equals(Object o) { return o instanceof Rol r && Objects.equals(nombre, r.nombre); }
    @Override public int hashCode() { return Objects.hashCode(nombre); }
    @Override public String toString() { return nombre + " (nivel " + nivel + ")"; }
}
