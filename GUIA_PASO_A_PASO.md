# Guía paso a paso — Sistema POS para supermercado (Java + Swing + Oracle XE)

**Cómo usar esta guía:** hazla en orden, de arriba hacia abajo. Cada paso termina con un **✅ Comprobación**:
si lo que ves es igual a lo que dice la comprobación, sigue. Si no, **no sigas**: ve a la sección
"Si algo sale mal" (al final) o cópiame el mensaje de error exacto.

**Lo importante:** el proyecto ya está escrito y compilado (Java 21). Tú **no tienes que programar**:
tienes que instalar 3 cosas, correr 3 scripts de base de datos y ejecutar la aplicación.

> Símbolos: 🖥️ = escribir en la consola (CMD) · 🗄️ = escribir en SQL Plus (la consola de Oracle)

---

## PARTE A — Instalar lo necesario (una sola vez)

### Paso A1 — Instalar Java (JDK 21)
1. Entra a **https://adoptium.net** y descarga **Temurin 21 (LTS)**, instalador de Windows (`.msi`).
2. Ábrelo. En la pantalla "Custom Setup" activa las opciones **"Add to PATH"** y **"Set JAVA_HOME variable"**.
3. Termina la instalación.

✅ **Comprobación:** abre una consola nueva (tecla Windows → escribe `cmd` → Enter) y escribe:
```
🖥️ javac -version
```
Debe responder algo como `javac 21.0.x`. Si dice "no se reconoce", cierra la consola y ábrela otra vez.

### Paso A2 — Instalar Oracle Database XE 21c
1. Descarga **Oracle Database 21c Express Edition for Windows** desde
   **https://www.oracle.com/database/technologies/xe-downloads.html** (pide crear una cuenta gratis de Oracle).
2. Descomprime el `.zip`, ejecuta `setup.exe` y sigue el asistente con "Siguiente".
3. Te pedirá **una contraseña** (para los usuarios SYS y SYSTEM). **Escríbela en un papel**: la usarás en el Paso C1.
4. Al terminar muestra una pantalla con puertos (1521 y 5500). No cambies nada.

✅ **Comprobación:** tecla Windows + R → escribe `services.msc` → Enter. Busca **OracleServiceXE** y
**OracleOracleDBXE21TNSListener**: ambos deben decir **"En ejecución"**.

### Paso A3 — Descargar el driver de conexión (ojdbc11.jar)
Es el "cable" que permite a Java hablar con Oracle. Es solo un archivo.
1. Entra a **https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc11/**
   (también sirve la página oficial: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html).
2. Abre la carpeta de la versión más alta que empiece con `23.` y descarga el archivo que se llama
   exactamente `ojdbc11-23.x.x.x.jar` (**no** el que dice `-sources` ni `-javadoc`).
3. Más adelante (Paso B2) lo copiarás a la carpeta `lib` del proyecto **con el nombre `ojdbc11.jar`**.

---

## PARTE B — Colocar el proyecto

### Paso B1 — Descomprimir
Descomprime `pos.zip` en **`C:\pos`**. (Usa justo esa ruta: sin espacios ni acentos, evita problemas.)
Debes ver dentro: `lib`, `sql`, `src`, `config.properties`, `compilar.bat`, `ejecutar.bat`, etc.

### Paso B2 — Poner el driver
Copia el `.jar` descargado en el Paso A3 a `C:\pos\lib\` y **renómbralo** a `ojdbc11.jar`.

✅ **Comprobación:** existe el archivo `C:\pos\lib\ojdbc11.jar`.

---

## PARTE C — Crear la base de datos

### Paso C1 — Crear los usuarios de la base (como SYSTEM)
1. Tecla Windows → escribe **SQL Plus** → abre "SQL Plus" (se instaló con Oracle).
2. Te pide `Enter user-name:` → escribe `system` y Enter. Luego la contraseña del Paso A2 (no se ve al escribir).
3. Escribe (una sola línea):
```
🗄️ @C:\pos\sql\00_crear_usuario_pos.sql
```
✅ **Comprobación:** salen mensajes `Session altered.`, `User created.`, `Grant succeeded.` (dos veces los últimos dos).
Escribe `exit` y Enter para salir.

> Se crearon dos usuarios: **pos** (datos reales) y **pos_test** (solo para pruebas).

### Paso C2 — Crear las tablas y los datos (como POS)
1. Abre otra vez **SQL Plus**. En `Enter user-name:` escribe exactamente:
```
pos/pos123@//localhost:1521/XEPDB1
```
2. Ejecuta, **uno por uno y en este orden**:
```
🗄️ @C:\pos\sql\01_esquema.sql
🗄️ @C:\pos\sql\02_datos_iniciales.sql
🗄️ @C:\pos\sql\03_usuarios_prueba_opcional.sql
```
✅ **Comprobación del 01:** muchos `Table created.` e `Index created.` (sin errores `ORA-`).
✅ **Comprobación del 02:** muchos `1 row created.` y al final `Commit complete.`
✅ **Comprobación del 03:** `1 row created.` varias veces y `Commit complete.`

Verificación final, escribe:
```
🗄️ SELECT username FROM usuario;
```
Debe listar: `admin`, `ana`, `luis`, `marta`. Luego `exit`.

> El script 03 es opcional: crea a ana, luis y marta (contraseña `Clave1234`) para que no tengas que crearlos a mano.
> Si prefieres crearlos tú desde la pantalla de Usuarios (como pide el ejercicio), no corras el 03 y salta a la sección G.

### Paso C3 — Preparar la base de pruebas (solo si harás el Paso F2)
Repite el Paso C2 pero con el usuario `pos_test` (contraseña `pos123`):
```
pos_test/pos123@//localhost:1521/XEPDB1
```
y corre **los tres scripts** (01, 02 y **03 obligatorio** esta vez).

---

## PARTE D — Compilar y probar la conexión

### Paso D1 — Compilar
```
🖥️ cd C:\pos
🖥️ compilar.bat
```
✅ **Comprobación:** el último mensaje dice **`Compilacion terminada correctamente.`** (y existe la carpeta `out`).

### Paso D2 — Probar la conexión (Checkpoint de la Fase 0)
```
🖥️ probar_conexion.bat
```
✅ **Comprobación:** imprime **`Conectado a Oracle XE`**.

---

## PARTE E — Ejecutar la aplicación

### Paso E1 — Abrir el sistema
```
🖥️ ejecutar.bat
```
Se abre la ventana **Login**. Entra con **usuario `admin`** y **contraseña `Admin123!`**.

✅ **Comprobación (Fase 3):** entra al **Menú principal** y arriba dice "Usuario: Administrador del sistema | sin turno abierto".
Cierra sesión (botón) y prueba una contraseña mala: debe decir "Usuario o contraseña inválidos".

### Paso E2 — Recorrer las pantallas como administrador
En el menú del admin hay botones para: **Usuarios**, **Roles y permisos**, **Productos**, **Cajas**. Ábrelos
y mira que cargan datos. (El admin no tiene botón de venta porque su rol es administrar; las ventas las hace un cajero.)

Cada botón solo aparece si el usuario tiene el permiso. Eso es lo que pide el ejercicio ("navegación por permisos").

---

## PARTE F — Pruebas automáticas

### Paso F1 — Pruebas sin base de datos
```
🖥️ pruebas.bat
```
✅ **Comprobación:** todas las líneas dicen `OK` y al final **`TODAS LAS PRUEBAS PASARON`** (son 35 casos).

### Paso F2 — Pruebas con base de datos (reproducen el escenario de la Fase 11)
Necesitas haber hecho el Paso C3.
```
🖥️ pruebas_bd.bat
```
✅ **Comprobación:** termina con **`TODAS LAS PRUEBAS CON BASE DE DATOS PASARON`**. Dentro verás
`Efectivo esperado de Ana = Q690.00` y `diferencia = -20.00 (faltante)`.

> Estas pruebas usan el usuario `pos_test`, por eso no ensucian tus datos reales.

---

## PARTE G — Escenario de validación (Fase 11) usando las pantallas

Es el guion del profesor. Hazlo **en orden**. Para tener dos cajeros a la vez, abre **una segunda consola**
y ejecuta `ejecutar.bat` otra vez (serán dos ventanas del programa).

| # | Quién | Qué hacer | Resultado esperado |
|---|-------|-----------|--------------------|
| 1 | admin | **Usuarios → Nuevo usuario**: `ana` / Ana López / `Clave1234` / rol CAJERO. Repite con `luis` (CAJERO) y `marta` (SUPERVISOR). *(Omite si corriste el script 03.)* | Aparecen en la lista |
| 2 | ana | Cerrar sesión del admin, entrar como `ana`. **Abrir turno**: Caja 1, efectivo inicial `1000` | Se abre el punto de venta |
| 3a | ana | Código `PRUEBA30`, Enter **5 veces** (o cambia la cantidad a 5). **Cobrar (F12)**: efectivo recibido `200` → Aceptar | Total Q150.00, **cambio Q50.00**. Anota el **folio** |
| 3b | ana | Código `PRUEBA10` ×**8**. Cobrar: efectivo `0`, tarjeta `80` | Total Q80.00, sin cambio |
| 3c | ana | Código `PRUEBA30` ×**4**. Cobrar: efectivo `70`, tarjeta `50` | Total Q120.00, sin cambio |
| 4 | luis | En la 2ª ventana entra como `luis` → Abrir turno | **La Caja 1 ya no aparece en la lista** (está ocupada). Elige Caja 2, fondo `800` |
| 5 | ana | **Corte intermedio**: monto `500` → "Entregar a bóveda" → en las credenciales escribe `marta` / `Clave1234` | Sale el **recibo de bóveda** con folio `BOV-…` |
| 6 | ana | **Reversión**: folio de la venta de Q150, selecciona la fila, cantidad `1`, motivo "Producto dañado" → Solicitar. **Primero** credenciales de `luis` | Dice que no puede autorizar (falla) |
| 6b | ana | Vuelve a solicitar con `marta` / `Clave1234` | "Reversión registrada. Devuelva Q30.00" |
| 7 | ana | **Cerrar turno**. Cuenta **Q680**: escribe en la tabla 3 billetes de 200, 1 de 50, 1 de 20 y 1 de 10 → "Declarar y cerrar" | Esperado **Q690.00**, contado Q680.00, diferencia **−Q10.00 (FALTANTE)** |

La cuenta del esperado: 1,000 + 150 + 70 − 500 − 30 = **Q690.00**. (El 70 es lo que entró en efectivo de la venta mixta;
el 150 es neto, porque el cambio de Q50 no cuenta.)

> Nota: el ejercicio dice "luis intenta abrir la Caja 1 → debe fallar". En la pantalla eso se cumple de forma
> preventiva (la caja ocupada ni aparece). El intento "a la fuerza" por código lo prueba `pruebas_bd.bat`.

---

## PARTE H — Entregables

### Paso H1 — Repositorio Git con commits por fase
Instala Git (https://git-scm.com) si no lo tienes. En la consola:
```
🖥️ cd C:\pos
🖥️ git init
🖥️ git add .gitignore config.properties compilar.bat compilar.sh ejecutar.bat ejecutar.sh probar_conexion.bat sql/00_crear_usuario_pos.sql src/com/supermercado/pos/PruebaConexion.java src/com/supermercado/pos/excepcion src/com/supermercado/pos/util/Conexion.java src/com/supermercado/pos/util/Config.java src/com/supermercado/pos/util/Transaccion.java src/com/supermercado/pos/util/Dinero.java
🖥️ git commit -m "fase-0-preparacion"
🖥️ git add src/com/supermercado/pos/dominio sql/01_esquema.sql
🖥️ git commit -m "fase-1-modelo"
🖥️ git add src/com/supermercado/pos/dao sql/02_datos_iniciales.sql sql/03_usuarios_prueba_opcional.sql
🖥️ git commit -m "fase-2-dao"
🖥️ git add src/com/supermercado/pos/util/Seguridad.java src/com/supermercado/pos/util/GenerarHash.java src/com/supermercado/pos/util/Sesion.java src/com/supermercado/pos/servicio/AuthService.java src/com/supermercado/pos/servicio/Permisos.java src/com/supermercado/pos/ui/Ui.java src/com/supermercado/pos/ui/LoginFrame.java src/com/supermercado/pos/ui/MenuPrincipalFrame.java src/com/supermercado/pos/Main.java
🖥️ git commit -m "fase-3-login"
🖥️ git add src/com/supermercado/pos/servicio/TurnoService.java src/com/supermercado/pos/servicio/CalculadoraEfectivo.java src/com/supermercado/pos/ui/AperturaTurnoFrame.java src/com/supermercado/pos/servicio/CatalogoService.java
🖥️ git commit -m "fase-4-apertura-turno"
🖥️ git add src/com/supermercado/pos/servicio/VentaService.java src/com/supermercado/pos/servicio/CalculadoraPagos.java src/com/supermercado/pos/ui/PosFrame.java src/com/supermercado/pos/ui/PagoDialog.java src/com/supermercado/pos/util/Ticket.java
🖥️ git commit -m "fase-5-cobro"
🖥️ git add src/com/supermercado/pos/servicio/CorteService.java src/com/supermercado/pos/ui/CorteIntermedioDialog.java
🖥️ git commit -m "fase-6-corte-intermedio"
🖥️ git add src/com/supermercado/pos/servicio/AutorizacionService.java src/com/supermercado/pos/servicio/ReversionService.java src/com/supermercado/pos/ui/AutorizacionDialog.java src/com/supermercado/pos/ui/ReversionFrame.java
🖥️ git commit -m "fase-7-reversion"
🖥️ git add src/com/supermercado/pos/ui/CierreTurnoFrame.java
🖥️ git commit -m "fase-8-cierre-turno"
🖥️ git add src/com/supermercado/pos/servicio/UsuarioService.java src/com/supermercado/pos/servicio/RolService.java src/com/supermercado/pos/ui/UsuariosFrame.java src/com/supermercado/pos/ui/RolesFrame.java src/com/supermercado/pos/ui/AsignacionPermisosDialog.java src/com/supermercado/pos/ui/ProductosFrame.java src/com/supermercado/pos/ui/CajasFrame.java
🖥️ git commit -m "fase-9-administracion"
🖥️ git add src/com/supermercado/pos/Pruebas.java src/com/supermercado/pos/PruebasBD.java pruebas.bat pruebas_bd.bat
🖥️ git commit -m "fase-10-pruebas"
🖥️ git add README.md GUIA_PASO_A_PASO.md docs
🖥️ git commit -m "fase-11-validacion-y-documentacion"
```
(Si un `git commit` dice "nothing to commit", es que ese grupo ya estaba agregado; sigue con el siguiente.
No importa que un commit intermedio no compile solo: lo importante es que el historial muestre las fases.)
Si te pide identificarte la primera vez: `git config --global user.name "Tu Nombre"` y
`git config --global user.email "tu@correo.com"`.

### Paso H2 — Diagrama entidad-relación
Abre `docs\diagrama_er.md`, copia el bloque de código `erDiagram`, pégalo en **https://mermaid.live** y usa
**Actions → PNG** para descargar la imagen. Guárdala como `docs\diagrama_er.png`.

### Paso H3 — Capturas de pantalla
Con la tecla Windows + Shift + S toma una captura de cada pantalla y guárdalas en `docs\capturas\` con nombres
como `01_login.png`. Son 12 pantallas: Login, Apertura de turno, Punto de venta, Pago, Corte intermedio,
Reversión (buscar), Autorización, Cierre de turno, Usuarios, Roles, Asignación de permisos, Productos y Cajas.
Agrega también una captura del resultado de `pruebas_bd.bat` y de la Parte G (el cierre con −Q10.00).

### Paso H4 — Responder las 3 preguntas de reflexión
Están respondidas en `docs\respuestas_reflexion.md`. Léelas y, si tu profesor lo pide, reescríbelas con tus palabras.

---

## Si algo sale mal (errores comunes)

| Mensaje | Qué significa | Qué hacer |
|---|---|---|
| `'javac' no se reconoce...` | Java no está en el PATH | Cierra y reabre la consola; si sigue, reinstala el JDK marcando "Add to PATH" |
| `ORA-12541: TNS:no listener` | Oracle está apagado | `services.msc` → inicia **OracleServiceXE** y el **TNSListener** |
| `ORA-12514 ... service` | Nombre de servicio mal escrito | Usa exactamente `//localhost:1521/XEPDB1` |
| `ORA-01017: invalid username/password` | Contraseña incorrecta | En el programa, revisa `config.properties` (`pos` / `pos123`) |
| `No suitable driver` o `ClassNotFoundException: oracle.jdbc...` | Falta el driver | Revisa que exista `C:\pos\lib\ojdbc11.jar` y vuelve a compilar |
| `ORA-00955: name is already used` | Corriste el script 01 dos veces | Como SYSTEM: `DROP USER pos CASCADE;` y repite desde el Paso C1 (crea el usuario otra vez) |
| Acentos raros en consola | Es solo la consola | Usa `chcp 65001` antes de ejecutar |
| "Ya tienes un turno abierto" al abrir | Tienes un turno sin cerrar | Entra y ciérralo con "Cerrar turno" |

**Si hay cualquier otro error:** copia el mensaje completo (o toma captura) y el número de paso donde estabas.

---

## Qué hace cada carpeta (por si te lo preguntan)

- `src/.../dominio` — las "cosas" del negocio: Usuario, Rol, Turno, Venta, Pago…
- `src/.../dao` — habla con Oracle (consultas SQL con `PreparedStatement`).
- `src/.../servicio` — las **reglas de negocio** (RN-01 a RN-09) y las transacciones (commit/rollback).
- `src/.../ui` — las ventanas (Swing).
- `src/.../util` — herramientas: conexión, hash de contraseñas (PBKDF2), sesión, dinero.
- `sql/` — scripts de la base de datos. `config.properties` — efectivo mínimo (Q500) y datos de conexión.
