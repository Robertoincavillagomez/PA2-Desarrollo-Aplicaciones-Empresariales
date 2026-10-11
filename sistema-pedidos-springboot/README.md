
# Sistema de Pedidos - PA2

## Descripción

Este proyecto forma parte de la PA2 del curso Desarrollo de Aplicaciones Empresariales Avanzado de ISIL.

La aplicación permite registrar pedidos, consultar los pedidos registrados y realizar anulaciones. Para ello, utiliza Spring Boot y se comunica mediante una API REST con el servicio de inventario desarrollado con Jakarta EE y WildFly.

## Tecnologías utilizadas

- Java 21
- Spring Boot
- Spring MVC y Spring Data JPA
- JSP
- H2 Database
- Maven
- JUnit y Mockito
- API REST
- WildFly y SQL Server (servicio externo de inventario)

## Arquitectura

El sistema está dividido en dos aplicaciones:

1. **sistema-pedidos-springboot:** administra los pedidos y las anulaciones. Utiliza H2 para almacenar la información.
2. **inventario-service:** administra los productos y el stock. Se ejecuta en WildFly y utiliza SQL Server.

La comunicación entre ambas aplicaciones se realiza mediante peticiones HTTP a sus API REST.

## Requisitos

- JDK 21
- Maven o Maven Wrapper
- SQL Server Express con la base de datos InventarioDB
- Servicio inventario-service configurado y en ejecución

## Ejecución

Primero se debe iniciar el proyecto `inventario-service` en WildFly, utilizando el puerto 8082.

Después, desde la carpeta `sistema-pedidos-springboot`, ejecutar en PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación estará disponible en:

http://localhost:8081/sistema-pedidos/pedidos

Consola H2:

http://localhost:8081/sistema-pedidos/h2-console

Datos de conexión H2:

- JDBC URL: jdbc:h2:mem:pedidosdb
- Usuario: sa
- Contraseña: vacía

## Funcionalidades

### Registro de pedidos

Permite seleccionar un producto, indicar el cliente y la cantidad solicitada.

El sistema consulta el servicio de inventario para obtener los datos del producto y actualizar el stock. Después registra el pedido en H2.

### Anulación de pedidos

Endpoint:

POST /sistema-pedidos/api/v1/anulaciones

Ejemplo de solicitud:

```json
{
  "pedidoId": 2,
  "motivo": "Solicitud del cliente"
}
```

Cuando la anulación se realiza correctamente:

- Se devuelve la cantidad correspondiente al inventario.
- El pedido cambia al estado ANULADO.
- Se registra la anulación en la tabla ANULACION_PEDIDO.

También se puede consultar el historial mediante:

GET /sistema-pedidos/api/v1/anulaciones

## Validaciones y manejo de errores

El sistema contempla los siguientes casos:

- **HTTP 200:** anulación procesada correctamente.
- **HTTP 400:** solicitud con datos inválidos.
- **HTTP 404:** pedido inexistente.
- **HTTP 409:** pedido previamente anulado.
- **HTTP 503:** servicio de inventario no disponible.

Si el servicio de inventario no está disponible, el pedido conserva su estado anterior y no se registra una anulación en H2.

## Compensación de pedidos

Como funcionalidad adicional, se implementó una compensación para el registro de pedidos.

Si el inventario reserva o descuenta el stock, pero posteriormente falla el guardado del pedido en H2, el sistema intenta devolver la cantidad descontada mediante el servicio de inventario.

El proceso registra mensajes de seguimiento:

- COMPENSACION INICIADA
- COMPENSACION EXITOSA

Si la devolución también falla, se registra el incidente para su revisión manual.

Esta solución utiliza transacciones locales y una acción compensatoria. No constituye una transacción distribuida atómica entre H2 y SQL Server.

## Pruebas

Se implementaron pruebas automatizadas para comprobar la lógica del sistema y sus validaciones.

Para ejecutarlas:

```powershell
.\mvnw.cmd test
```

Resultados obtenidos durante el desarrollo:

- sistema-pedidos-springboot: 12 pruebas ejecutadas correctamente.
- inventario-service: 5 pruebas ejecutadas correctamente.

La prueba de integración de compensación utiliza H2 y un cliente de inventario simulado para verificar que se solicita la devolución cuando falla el registro local.

## Repositorio

https://github.com/Robertoincavillagomez/PA2-Desarrollo-Aplicaciones-Empresariales

## Curso

Desarrollo de Aplicaciones Empresariales Avanzado - ISIL
