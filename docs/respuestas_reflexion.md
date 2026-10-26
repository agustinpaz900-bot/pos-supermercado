# Preguntas de reflexión

**1. ¿Por qué VentaDetalle guarda precioUnitario en lugar de leer siempre Producto.precio?**
Porque el precio del producto cambia con el tiempo. Si la venta leyera el precio actual, un ticket de hace un mes
mostraría otro total, las reversiones devolverían un monto distinto al cobrado y el cuadre de caja no coincidiría.
Se "congela" el precio al momento de vender para conservar el historial exacto.

**2. ¿Qué pasa si dos cajas venden el último producto al mismo instante?**
`UPDATE producto SET stock = stock - ? WHERE id = ? AND stock >= ?` es una sola operación atómica: Oracle bloquea la fila,
así que una caja la ejecuta primero y la otra espera. Cuando la segunda continúa, el stock ya es 0, la condición
`stock >= ?` es falsa y no actualiza nada: `executeUpdate()` devuelve **0**. El servicio detecta ese 0, lanza
"Sin stock suficiente" y hace rollback de esa venta. Nunca queda stock negativo (además hay `CHECK (stock >= 0)`).

**3. ¿Por qué re-autenticar con credenciales en vez de un "PIN compartido" o un checkbox?**
Un PIN compartido lo conoce mucha gente y no se sabe quién autorizó; un checkbox lo marca cualquiera. Con credenciales
individuales se comprueba una persona real, con permiso y nivel suficientes; queda registrado quién autorizó (RN-07)
y en la bitácora cada intento; se puede limitar los intentos y desactivar a esa persona. Hay responsabilidad individual.
