-- Datos iniciales. Ejecutar como POS (y como POS_TEST si haras PruebasBD), DESPUES de 01_esquema.sql.
-- Permisos
INSERT INTO permiso (codigo, descripcion) VALUES ('TURNO_ABRIR', 'Abrir turno');
INSERT INTO permiso (codigo, descripcion) VALUES ('VENTA_COBRAR', 'Cobrar ventas');
INSERT INTO permiso (codigo, descripcion) VALUES ('CORTE_INTERMEDIO', 'Corte intermedio (retiro a boveda)');
INSERT INTO permiso (codigo, descripcion) VALUES ('REVERSION_SOLICITAR', 'Solicitar reversiones');
INSERT INTO permiso (codigo, descripcion) VALUES ('REVERSION_AUTORIZAR', 'Autorizar reversiones');
INSERT INTO permiso (codigo, descripcion) VALUES ('TURNO_CERRAR', 'Cerrar turno');
INSERT INTO permiso (codigo, descripcion) VALUES ('USUARIO_ADMIN', 'Administrar usuarios');
INSERT INTO permiso (codigo, descripcion) VALUES ('ROL_ADMIN', 'Administrar roles y permisos');
INSERT INTO permiso (codigo, descripcion) VALUES ('CATALOGO_ADMIN', 'Administrar productos y cajas');

-- Roles
INSERT INTO rol (nombre, nivel) VALUES ('CAJERO', 1);
INSERT INTO rol (nombre, nivel) VALUES ('SUPERVISOR', 2);
INSERT INTO rol (nombre, nivel) VALUES ('GERENTE', 3);
INSERT INTO rol (nombre, nivel) VALUES ('ADMIN', 4);

-- CAJERO
INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id FROM rol r, permiso p WHERE r.nombre = 'CAJERO'
  AND p.codigo IN ('TURNO_ABRIR','VENTA_COBRAR','CORTE_INTERMEDIO','REVERSION_SOLICITAR','TURNO_CERRAR');
-- SUPERVISOR = cajero + autorizar reversiones
INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id FROM rol r, permiso p WHERE r.nombre = 'SUPERVISOR'
  AND p.codigo IN ('TURNO_ABRIR','VENTA_COBRAR','CORTE_INTERMEDIO','REVERSION_SOLICITAR','TURNO_CERRAR','REVERSION_AUTORIZAR');
-- GERENTE = supervisor + catalogo
INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id FROM rol r, permiso p WHERE r.nombre = 'GERENTE'
  AND p.codigo IN ('TURNO_ABRIR','VENTA_COBRAR','CORTE_INTERMEDIO','REVERSION_SOLICITAR','TURNO_CERRAR','REVERSION_AUTORIZAR','CATALOGO_ADMIN');
-- ADMIN = todos
INSERT INTO rol_permiso (rol_id, permiso_id)
  SELECT r.id, p.id FROM rol r, permiso p WHERE r.nombre = 'ADMIN';

-- Cajas
INSERT INTO caja (numero, nombre) VALUES (1, 'Caja 1');
INSERT INTO caja (numero, nombre) VALUES (2, 'Caja 2');
INSERT INTO caja (numero, nombre) VALUES (3, 'Caja 3');

-- Productos de prueba (precio con impuesto incluido)
INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock) VALUES ('7400001', 'Arroz 1 kg', 12.50, 12, 200);
INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock) VALUES ('7400002', 'Frijol 1 lb', 8.75, 12, 200);
INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock) VALUES ('7400003', 'Leche 1 litro', 10.00, 12, 150);
INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock) VALUES ('7400004', 'Pan frances (bolsa)', 15.00, 12, 100);
INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock) VALUES ('7400005', 'Aceite 750 ml', 32.90, 12, 80);
-- Productos con precio redondo para el escenario de validacion de la Fase 11
INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock) VALUES ('PRUEBA30', 'Producto de prueba Q30', 30.00, 12, 1000);
INSERT INTO producto (codigo_barras, nombre, precio, porcentaje_impuesto, stock) VALUES ('PRUEBA10', 'Producto de prueba Q10', 10.00, 12, 1000);

-- Usuario administrador: admin / Admin123!  (hash PBKDF2 generado con GenerarHash)
INSERT INTO usuario (username, password_hash, nombre_completo)
  VALUES ('admin', '120000:+kmKsVqYizmNk/bGc3hYdw==:B6g3CrmFHCLQByVc+/MDNK2PBfTB4beLef/AerdpCi8=', 'Administrador del sistema');
INSERT INTO usuario_rol (usuario_id, rol_id)
  SELECT u.id, r.id FROM usuario u, rol r WHERE u.username = 'admin' AND r.nombre = 'ADMIN';

COMMIT;
