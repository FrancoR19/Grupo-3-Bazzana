# Spec Delta

## ADDED Requirements

### Requirement: Cancelar un turno con anticipación suficiente
El sistema SHALL cancelar un turno existente en estado RESERVADO cuando falten al menos 6 horas para su inicio (`ahora <= inicio − anticipación mínima`), cambiando su estado a CANCELADO, registrando `canceladoEn` igual al "ahora" del sistema y respondiendo 200 con el turno actualizado. La anticipación mínima MUST leerse de `turnos.cancelacion-horas-minimas` y el "ahora" MUST venir del `Clock` inyectado (RN-CA-01, RN-GL-02).

#### Scenario: Cancelación feliz (más de 6 horas)
- **GIVEN** que existe el turno 09:30-10:00 y el "ahora" es el domingo 2026-11-22 a las 20:00
- **WHEN** la secretaria pide cancelar ese turno
- **THEN** la respuesta es 200 con estado CANCELADO y `canceladoEn` igual al "ahora"

#### Scenario: Exactamente 6 horas antes (borde aceptado)
- **GIVEN** que el "ahora" es el lunes 2026-11-23 a las 03:30
- **WHEN** se pide cancelar el turno 09:30-10:00
- **THEN** la respuesta es 200 con estado CANCELADO

#### Scenario: Tolerancia configurada distinta de 6 horas
- **GIVEN** que `turnos.cancelacion-horas-minimas` vale 4
- **WHEN** se pide cancelar el turno 09:30-10:00 con el "ahora" a las 05:00 (4 horas y media antes)
- **THEN** la respuesta es 200 con estado CANCELADO

### Requirement: Cancelación tardía rechazada
Si el "ahora" del sistema es posterior a `inicio − anticipación mínima`, el sistema SHALL rechazar la cancelación con 422 y `codigo` igual a `CANCELACION_TARDIA`, y el turno MUST quedar sin cambios en estado RESERVADO (RN-CA-01).

#### Scenario: 5 horas 59 minutos antes (borde rechazado)
- **GIVEN** que el "ahora" es el lunes 2026-11-23 a las 03:31
- **WHEN** se pide cancelar el turno 09:30-10:00
- **THEN** la respuesta es 422 con `codigo` igual a `CANCELACION_TARDIA`
- **AND** el turno sigue en estado RESERVADO

#### Scenario: Después del inicio del turno
- **GIVEN** que el "ahora" es el lunes 2026-11-23 a las 10:00
- **WHEN** se pide cancelar el turno 09:30-10:00
- **THEN** la respuesta es 422 con `codigo` igual a `CANCELACION_TARDIA`

### Requirement: Un turno cancelado no se vuelve a cancelar
El sistema SHALL rechazar la cancelación de un turno que no está en estado RESERVADO con 422 y `codigo` igual a `TURNO_YA_CANCELADO`, conservando su `canceladoEn` original (RN-CA-03). Este chequeo MUST ocurrir antes del chequeo de anticipación.

#### Scenario: Segunda cancelación
- **GIVEN** que el turno 09:30-10:00 ya fue cancelado
- **WHEN** se vuelve a pedir su cancelación
- **THEN** la respuesta es 422 con `codigo` igual a `TURNO_YA_CANCELADO`
- **AND** `canceladoEn` conserva el valor de la primera cancelación

#### Scenario: Turno cancelado cuyo inicio ya pasó
- **GIVEN** que el turno 09:30-10:00 ya fue cancelado y el "ahora" es el lunes 2026-11-23 a las 10:00
- **WHEN** se vuelve a pedir su cancelación
- **THEN** la respuesta es 422 con `codigo` igual a `TURNO_YA_CANCELADO`

### Requirement: Cancelar un turno inexistente
El sistema SHALL responder 404 con `codigo` igual a `RECURSO_INEXISTENTE` cuando se pida cancelar un identificador de turno que no existe.

#### Scenario: Identificador inexistente
- **WHEN** se pide cancelar el turno con identificador 9999
- **THEN** la respuesta es 404 con `codigo` igual a `RECURSO_INEXISTENTE`

### Requirement: La cancelación libera el horario
Al cancelar un turno, el sistema MUST dejar de contar ese turno en cualquier chequeo de choque de creación: un turno nuevo en el mismo horario, sillón y profesional SHALL ser aceptado (RN-CA-02). La cancelación es un cambio de estado: los datos originales del turno MUST conservarse y el turno MUST seguir siendo consultable con estado CANCELADO y su `canceladoEn` (RN-CA-04).

#### Scenario: Vuelta a crear el mismo hueco tras cancelar
- **GIVEN** que el turno 09:30-10:00 fue cancelado con anticipación
- **WHEN** se crea un turno de Control para Paciente Dos con Ana, en el Sillón 1, a las 09:30
- **THEN** la respuesta es 201

#### Scenario: Detalle de un turno cancelado
- **GIVEN** que el turno 09:30-10:00 fue cancelado con anticipación
- **WHEN** se consulta ese turno por su identificador
- **THEN** la respuesta es 200 con estado CANCELADO y los mismos datos originales (paciente, profesional, sillón, prestación, inicio y fin)
