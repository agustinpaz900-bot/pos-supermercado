# Nombre: Gerber Rolando Agustin Paz

# carnet: 090- 24- 27263

# Sistema POS para supermercado

Aplicación de escritorio en **Java 21 puro** (Swing + JDBC + Oracle XE 21c), sin frameworks.
Curso Programación II, sección 1 — Proyecto integrador.

## Ejecución rápida (Windows)

1. Instalar JDK 21 y Oracle XE 21c. Copiar `ojdbc11.jar` a `lib/`.
2. Como SYSTEM ejecutar `sql/00\_crear\_usuario\_pos.sql`; como `pos` ejecutar `sql/01\_esquema.sql`, `02\_datos\_iniciales.sql` (y opcional `03\_usuarios\_prueba\_opcional.sql`).
3. `compilar.bat` y luego `ejecutar.bat`.
4. Pruebas: `pruebas.bat` (sin BD) y `pruebas\_bd.bat` (con BD, usuario `pos\_test`).

Instrucciones detalladas: `GUIA\_PASO\_A\_PASO.md`.

## Usuarios de prueba

|Usuario|Contraseña|Rol|
|-|-|-|
|admin|Admin123!|ADMIN (nivel 4)|
|ana, luis|Clave1234|CAJERO (nivel 1) — script 03|
|marta|Clave1234|SUPERVISOR (nivel 2) — script 03|

Productos de prueba: `PRUEBA30` (Q30), `PRUEBA10` (Q10), `7400001`…`7400005`.

## Estructura

`dominio/` modelo · `dao/` JDBC · `servicio/` reglas de negocio y transacciones · `ui/` ventanas Swing · `util/` conexión, PBKDF2, sesión · `excepcion/`.

## Decisiones de diseño

* Dinero siempre `BigDecimal`, escala 2, `HALF\_UP`.
* Contraseñas con PBKDF2WithHmacSHA256, 120 000 iteraciones y sal aleatoria (`iteraciones:sal:hash`).
* Cada operación de escritura es una transacción (`Transaccion.ejecutar`): commit o rollback.
* RN-01 también a nivel de BD con índices únicos basados en expresión (caja y cajero).
* Reversión: se re-autentica a otro usuario (permiso `REVERSION\_AUTORIZAR`, nivel mayor, no el mismo); los intentos quedan en `auditoria\_autorizacion`; máximo 3 intentos en pantalla.
* Cierre de turno "ciego": el cajero cuenta antes de ver el esperado.
* Los servicios vuelven a verificar permisos y reglas (no solo la interfaz).

