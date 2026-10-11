
# Inventario Service — Jakarta EE 11 + SQL Server Express 2025

Proyecto académico de integración de aplicaciones empresariales, desarrollado a partir del material de clase de ISIL.

En la PA2 se amplió el servicio de inventario para permitir la devolución de stock cuando un pedido es anulado en el sistema Spring Boot.

## 1. Tecnologías utilizadas

- Java 21
- Maven
- Jakarta EE 11
- WildFly 41
- Jakarta REST
- Jakarta Enterprise Beans (EJB)
- Jakarta Persistence 3.2 (JPA)
- Jakarta Transactions (JTA)
- Bean Validation
- Microsoft JDBC Driver for SQL Server
- SQL Server Express 2025
- JUnit 5

## 2. Arquitectura

```text
Sistema de Pedidos Spring Boot / Postman
                  |
                  | HTTP + JSON
                  v
       ProductoResource (REST)
                  |
                  | @EJB
                  v
       InventarioService (@Stateless)
                  |
                  | @PersistenceContext
                  v
       EntityManager / InventarioPU
                  |
                  | InventarioDS
                  v
         SQL Server Express
```

El servicio recibe las solicitudes mediante una API REST. La lógica de negocio se ejecuta en un EJB y la información se guarda en SQL Server mediante JPA.

## 3. Preparación del entorno

Antes de ejecutar el proyecto, se necesita:

1. Tener instalado Java 21.
2. Tener IntelliJ IDEA con soporte para Maven.
3. Tener SQL Server Express y SQL Server Management Studio (SSMS).
4. Habilitar TCP/IP y configurar el puerto de SQL Server según el entorno.
5. Configurar las credenciales de conexión utilizadas por el proyecto.
6. Ejecutar el archivo `database/01_crear_inventario.sql` desde SSMS.
7. Verificar que la base de datos `InventarioDB` contenga los productos de prueba.

Los productos utilizados durante las pruebas fueron:

| Código | Producto |
|---|---|
| LAP-001 | Laptop |
| MON-001 | Monitor |
| TEC-001 | Teclado |

## 4. Ejecución del proyecto

Desde IntelliJ IDEA:

1. Abrir la carpeta `inventario-service`.
2. Esperar a que Maven cargue las dependencias.
3. Abrir el panel Maven.
4. Ejecutar `Lifecycle > test` para verificar las pruebas.
5. Ejecutar `Plugins > wildfly > wildfly:run`.
6. Esperar a que WildFly termine el despliegue.

También se puede ejecutar desde una terminal con Maven:

```bash
mvn test
mvn wildfly:run
```

El proyecto utiliza el puerto HTTP **8082**, lo que permite ejecutarlo junto al sistema de pedidos Spring Boot, que utiliza el puerto **8081**.

Para comprobar el funcionamiento, abrir:

```text
http://localhost:8082/inventario-service/api/inventario/productos
```

Si el servicio está funcionando correctamente, se mostrará la lista de productos en formato JSON.

## 5. Endpoints principales

### Consultar productos

```http
GET /inventario-service/api/inventario/productos
```

Permite consultar los productos registrados y su stock disponible.

### Consultar stock por ID

```http
GET /inventario-service/api/inventario/productos/1/stock
```

### Reservar stock por ID

```http
POST /inventario-service/api/inventario/productos/1/reservas
Content-Type: application/json
```

```json
{
  "cantidad": 2
}
```

### Devolver stock por código — PA2

```http
POST /inventario-service/api/inventario/productos/codigo/LAP-001/devoluciones
Content-Type: application/json
```

Cuerpo de la solicitud:

```json
{
  "cantidad": 2
}
```

Ejemplo de respuesta exitosa:

```json
{
  "codigo": "LAP-001",
  "cantidadDevuelta": 2,
  "stockRestante": 12
}
```

El valor de `stockRestante` depende del stock que tenga el producto antes de ejecutar la devolución.

## 6. Validaciones y respuestas HTTP

El endpoint de devoluciones contempla las siguientes situaciones:

| Código HTTP | Situación |
|---|---|
| 200 OK | La devolución se realizó correctamente. |
| 400 Bad Request | La cantidad enviada es cero, negativa o inválida. |
| 404 Not Found | No existe un producto con el código solicitado. |

Cuando la devolución es válida, el stock del producto aumenta según la cantidad recibida.

Cuando la cantidad es inválida o el producto no existe, la operación no debe modificar el inventario.

## 7. Persistencia y control de concurrencia

El inventario utiliza SQL Server para almacenar los productos y sus cantidades disponibles.

La entidad `ProductoInventario` conserva el campo `version` mediante la anotación `@Version`, que permite detectar conflictos de actualización concurrente.

La devolución se realiza dentro de la lógica de negocio del servicio y utiliza una transacción administrada por Jakarta EE.

## 8. Pruebas automatizadas

Se ejecutaron las pruebas del proyecto mediante Maven:

```bash
mvn test
```

Resultado obtenido:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Entre los casos verificados se encuentran:

- La lógica original de reserva de stock.
- La devolución de dos unidades cuando el stock inicial es ocho.
- El rechazo de una devolución con cantidad cero.
- El rechazo de una devolución con cantidad negativa.

También se realizaron pruebas manuales del endpoint REST, comprobando respuestas HTTP 200, 400 y 404.

## 9. Integración con Sistema de Pedidos Spring Boot

Este servicio se comunica con `sistema-pedidos-springboot` mediante HTTP y JSON.

Cuando se anula un pedido, Spring Boot solicita la devolución de la cantidad correspondiente al producto. El servicio de inventario procesa la solicitud y actualiza el stock en SQL Server.

Los dos proyectos se ejecutan por separado:

| Aplicación | Puerto | Base de datos |
|---|---|---|
| inventario-service | 8082 | SQL Server |
| sistema-pedidos-springboot | 8081 | H2 en memoria |

Es importante tener ambos servicios iniciados para probar una anulación completa.

## 10. Material de clase

Se conservan los recursos originales del proyecto:

- `docs/GUIA_IMPLEMENTACION_PASO_A_PASO.md`
- `docs/INTEGRACION_CON_SISTEMA_PEDIDOS.md`
- `requests/inventario-service.http`
- `openapi/inventario-service.yaml`
- `database/01_crear_inventario.sql`
- `database/02_reset_stock_demo.sql`

## 11. Consideraciones de seguridad

La configuración académica utiliza credenciales locales y `trustServerCertificate=true` para facilitar las pruebas.

En un entorno de producción, las credenciales deben almacenarse de forma segura y no incluirse directamente en el código fuente. También se debe configurar adecuadamente la validación del certificado de SQL Server.

## 12. Resultado

Se implementó el endpoint de devolución de stock manteniendo la arquitectura original de Jakarta EE, EJB y JPA.

Las pruebas automatizadas finalizaron correctamente y se verificó la integración REST con el sistema de pedidos Spring Boot.

