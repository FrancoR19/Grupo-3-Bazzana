# pacientes Specification

## Purpose

Permite registrar, editar y buscar pacientes desde la API con DNI único y teléfono en formato internacional, protegiendo los datos personales (RN-DP-01, RN-DP-03) para que la agenda pueda agendar turnos contra un `pacienteId` válido.

## Requirements

### Requirement: Alta de paciente

El sistema SHALL crear un paciente con `POST /api/pacientes` respondiendo 201 con su representación y la ubicación del recurso. `nombre`, `apellido` y `dni` son obligatorios; `telefono` y `email` son opcionales (US-050).

#### Scenario: Alta con datos ficticios

- **WHEN** se envía `POST /api/pacientes` con nombre "Paciente", apellido "Uno", DNI "00000001", teléfono "5491100000001" y email "paciente.uno@example.com"
- **THEN** la respuesta es 201 con esos mismos datos, un `id` asignado y la ubicación del recurso creado

#### Scenario: Alta sin teléfono

- **WHEN** se envía `POST /api/pacientes` sin campo `telefono`
- **THEN** la respuesta es 201 con `telefono` null y el paciente queda disponible para agendar turnos (el link de WhatsApp quedará bloqueado en C-06)

### Requirement: DNI único

El sistema SHALL rechazar con 409 y `codigo` igual a `DATO_DUPLICADO` la alta de un paciente cuyo DNI ya exista, sin modificar el registro existente.

#### Scenario: DNI duplicado en el alta

- **GIVEN** existe el paciente con DNI "00000001"
- **WHEN** se envía `POST /api/pacientes` con el mismo DNI y otro nombre
- **THEN** la respuesta es 409 con `codigo` igual a `DATO_DUPLICADO` y el paciente original conserva su nombre

### Requirement: Formato del teléfono

El sistema SHALL aceptar solo el formato internacional sin `+` ni espacios (por ejemplo `5491100000001`) y SHALL rechazar con 400 y `codigo` igual a `ENTRADA_INVALIDA` un teléfono que contenga `+` o espacios.

#### Scenario: Teléfono con signo más

- **WHEN** se envía `POST /api/pacientes` con `telefono` "+5491100000001"
- **THEN** la respuesta es 400 con `codigo` igual a `ENTRADA_INVALIDA` y el paciente no se guarda

#### Scenario: Teléfono con espacios

- **WHEN** se envía `POST /api/pacientes` con `telefono` "549 11 0000-0001"
- **THEN** la respuesta es 400 con `codigo` igual a `ENTRADA_INVALIDA`

### Requirement: Edición de paciente

El sistema SHALL permitir editar nombre, apellido, teléfono y email con `PUT /api/pacientes/{id}`, respondiendo 200 con la representación actualizada. El DNI MUST ser inmutable: si se envía distinto del actual, la solicitud SHALL rechazarse con 400 y `codigo` igual a `ENTRADA_INVALIDA`. Un id inexistente SHALL responder 404 con `codigo` igual a `RECURSO_INEXISTENTE`.

#### Scenario: Editar nombre y teléfono

- **GIVEN** existe el paciente "Paciente Uno" con DNI "00000001"
- **WHEN** se envía `PUT /api/pacientes/{id}` con apellido "Ejemplo" y teléfono "5491100000009"
- **THEN** la respuesta es 200 con el apellido y el teléfono actualizados y el DNI sigue siendo "00000001"

#### Scenario: Intento de cambiar el DNI

- **WHEN** se envía `PUT /api/pacientes/{id}` con un DNI distinto del actual
- **THEN** la respuesta es 400 con `codigo` igual a `ENTRADA_INVALIDA` y el DNI no se modifica

#### Scenario: Paciente inexistente

- **WHEN** se envía `PUT /api/pacientes/{id}` con un id que no existe
- **THEN** la respuesta es 404 con `codigo` igual a `RECURSO_INEXISTENTE`

### Requirement: Listado y búsqueda de pacientes

El sistema SHALL devolver con `GET /api/pacientes` el total de los pacientes registrados como DTOs (nunca entidades JPA). Con el parámetro opcional `q`, SHALL filtrar por coincidencia insensible a mayúsculas de nombre, apellido o DNI. Una base vacía SHALL responder 200 con lista vacía.

#### Scenario: Listado completo

- **GIVEN** existen dos pacientes ficticios
- **WHEN** se consulta `GET /api/pacientes`
- **THEN** la respuesta es 200 con ambos pacientes ordenados de forma estable y sin exponer entidades de persistencia

#### Scenario: Búsqueda por DNI parcial

- **GIVEN** existe el paciente con DNI "00000001"
- **WHEN** se consulta `GET /api/pacientes?q=0000`
- **THEN** la respuesta es 200 y contiene solo ese paciente

#### Scenario: Sin coincidencias

- **WHEN** se consulta `GET /api/pacientes?q=zzzz`
- **THEN** la respuesta es 200 con una lista vacía

### Requirement: Lectura de un paciente

El sistema SHALL devolver con `GET /api/pacientes/{id}` la representación del paciente solicitado, o 404 con `codigo` igual a `RECURSO_INEXISTENTE` si el id no existe.

#### Scenario: Paciente existente

- **GIVEN** existe el paciente con id 1
- **WHEN** se consulta `GET /api/pacientes/1`
- **THEN** la respuesta es 200 con sus datos

#### Scenario: Paciente inexistente

- **WHEN** se consulta `GET /api/pacientes/9999`
- **THEN** la respuesta es 404 con `codigo` igual a `RECURSO_INEXISTENTE`

### Requirement: Entrada inválida en pacientes

Los campos obligatorios de las solicitudes de pacientes SHALL validarse con Bean Validation antes de ejecutar cualquier regla de negocio, respondiendo 400 con `codigo` igual a `ENTRADA_INVALIDA` e indicando el campo inválido, conforme a la spec de `base-del-sistema`.

#### Scenario: Nombre ausente en el alta

- **WHEN** se envía `POST /api/pacientes` sin campo `nombre`
- **THEN** la respuesta es 400 con `codigo` igual a `ENTRADA_INVALIDA` y menciona el campo `nombre`

#### Scenario: DNI en blanco en la edición

- **WHEN** se envía `PUT /api/pacientes/{id}` con `dni` vacío o formado solo por espacios
- **THEN** la respuesta es 400 con `codigo` igual a `ENTRADA_INVALIDA` y el paciente conserva sus datos anteriores

### Requirement: Logs sin datos personales

El sistema MUST NOT escribir en ningún log el DNI, el teléfono ni el nombre completo de un paciente (RN-DP-03); los mensajes de la capa de pacientes solo identifican registros por `id`. Los errores de la API MUST NOT incluir esos datos en sus mensajes.

#### Scenario: Alta y edición sin fuga en logs

- **GIVEN** se ejecutan alta, edición y listado de un paciente con datos ficticios capturando la salida de logging
- **WHEN** se revisa la captura de logs generada por esas operaciones
- **THEN** ningún mensaje contiene el DNI, el teléfono ni el nombre y apellido juntos del paciente
