-- OPCIONAL: crea ana, luis (CAJERO) y marta (SUPERVISOR), todos con contrasena Clave1234.
-- Sirve para ahorrar tiempo y para PruebasBD. Si prefieres crearlos tu desde la pantalla de Usuarios
-- (como pide el escenario de la Fase 11), NO ejecutes este script.
INSERT INTO usuario (username, password_hash, nombre_completo) VALUES ('ana', '120000:HO/rt7Qr7LS3xduWuD5JHw==:EOXPc3xyY14LbBgwzVV36VMrr/wsHquo/Us8SjkzRIw=', 'Ana Lopez');
INSERT INTO usuario (username, password_hash, nombre_completo) VALUES ('luis', '120000:y7ovnEPMmEE9ZIQtuMAzoA==:fAEwPqVpfGqpGeckvWvdHUkpuXlnuZVG3XLOO1z5eYc=', 'Luis Perez');
INSERT INTO usuario (username, password_hash, nombre_completo) VALUES ('marta', '120000:JiF4QzaHRD3N5lk1qhsFHA==:gvCzphWsmhu3T6DSot17hAwRvk+r+gnsI0SzEuRw/xY=', 'Marta Gomez');
INSERT INTO usuario_rol (usuario_id, rol_id) SELECT u.id, r.id FROM usuario u, rol r WHERE u.username IN ('ana','luis') AND r.nombre = 'CAJERO';
INSERT INTO usuario_rol (usuario_id, rol_id) SELECT u.id, r.id FROM usuario u, rol r WHERE u.username = 'marta' AND r.nombre = 'SUPERVISOR';
COMMIT;
