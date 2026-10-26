# Diagrama entidad-relación (pegar el bloque en https://mermaid.live)

```mermaid
erDiagram
  USUARIO ||--o{ USUARIO\_ROL : tiene
  ROL ||--o{ USUARIO\_ROL : asignado
  ROL ||--o{ ROL\_PERMISO : incluye
  PERMISO ||--o{ ROL\_PERMISO : en
  USUARIO ||--o{ USUARIO\_PERMISO : extra
  PERMISO ||--o{ USUARIO\_PERMISO : en
  CAJA ||--o{ TURNO : tiene
  USUARIO ||--o{ TURNO : cajero
  TURNO ||--o{ VENTA : registra
  TURNO ||--o{ MOVIMIENTO\_CAJA : registra
  VENTA ||--|{ VENTA\_DETALLE : contiene
  VENTA ||--|{ PAGO : cobra
  PRODUCTO ||--o{ VENTA\_DETALLE : vendido
  VENTA\_DETALLE ||--o{ REVERSION : revertido
  USUARIO ||--o{ REVERSION : solicita\_autoriza
  USUARIO ||--o{ AUDITORIA\_AUTORIZACION : intenta
  USUARIO { number id PK
    string username
    string password\_hash
    number activo }
  ROL { number id PK
    string nombre
    number nivel }
  PERMISO { number id PK
    string codigo }
  CAJA { number id PK
    number numero
    number activa }
  TURNO { number id PK
    number caja\_id FK
    number cajero\_id FK
    number fondo\_inicial
    number efectivo\_declarado
    number efectivo\_esperado
    number diferencia
    string estado }
  MOVIMIENTO\_CAJA { number id PK
    number turno\_id FK
    string tipo
    number monto }
  PRODUCTO { number id PK
    string codigo\_barras
    number precio
    number stock }
  VENTA { number id PK
    number turno\_id FK
    number total
    string estado }
  VENTA\_DETALLE { number id PK
    number venta\_id FK
    number producto\_id FK
    number cantidad
    number cantidad\_reversada
    number precio\_unitario }
  PAGO { number id PK
    number venta\_id FK
    string metodo
    number monto
    number recibido
    number cambio }
  REVERSION { number id PK
    number detalle\_id FK
    number cantidad
    number monto
    string motivo }
  AUDITORIA\_AUTORIZACION { number id PK
    string contexto
    number exito }
```

