package com.supermercado.pos.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class Usuario {
    private Long id;
    private String username;
    private String passwordHash;
    private String nombreCompleto;
    private boolean activo = true;
    private Set<Rol> roles = new HashSet<>();
    private Set<Permiso> permisosExtra = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Set<Rol> getRoles() { return roles; }
    public void setRoles(Set<Rol> roles) { this.roles = roles; }
    public Set<Permiso> getPermisosExtra() { return permisosExtra; }
    public void setPermisosExtra(Set<Permiso> permisosExtra) { this.permisosExtra = permisosExtra; }

    /** Nivel más alto entre todos sus roles (0 si no tiene roles). */
    public int nivelMaximo() {
        return roles.stream().mapToInt(Rol::getNivel).max().orElse(0);
    }

    public boolean tienePermiso(String codigo) {
        return roles.stream().flatMap(r -> r.getPermisos().stream()).anyMatch(p -> p.getCodigo().equals(codigo))
                || permisosExtra.stream().anyMatch(p -> p.getCodigo().equals(codigo));
    }

    /** Permisos que vienen de sus roles más los extra. */
    public Set<Permiso> permisosEfectivos() {
        Set<Permiso> s = new HashSet<>();
        for (Rol r : roles) s.addAll(r.getPermisos());
        s.addAll(permisosExtra);
        return s;
    }

    @Override public String toString() { return username; }
}
