# Proposal

## Why

La agenda solo sirve si un turno que el paciente no va a asistir puede liberarse: hoy los turnos creados en C-02 solo se consultan y se crean, y no hay forma de cancelarlos, por lo que un turno ausente bloquea sillón, profesional y paciente hasta su horario. Además, el proyecto necesita la cancelación con anticipación mínima (RN-CA) como diferencial documentado frente a los competidores relevados, y es el camino crítico hacia C-10 y C-11 (entrega 2026-11-25).

## What Changes

- Nuevo caso de uso `CancelarTurno` (`application/cancelacion/`): transición de estado `RESERVADO → CANCELADO` con registro de `canceladoEn`, sin borrado físico (RN-CA-02, RN-CA-04).
- Reglas de cancelación en dominio Java puro (`domain/rules/cancelacion/`): anticipación mínima configurable `turnos.cancelacion-horas-minimas` (default 6 h) usando el `Clock` inyectado (RN-CA-01, RN-GL-02); turno inexistente → 404; turno no `RESERVADO` → `TURNO_YA_CANCELADO` (422); `ahora > inicio − anticipación mínima` → `CANCELACION_TARDIA` (422).
- Nuevo endpoint `POST /api/turnos/{id}/cancelacion` en `web/api/cancelacion/` (controlador propio, sin editar el de C-02) que responde 200 con el turno actualizado.
- Prueba de que libera el horario (RN-CA-02): tras cancelar, un turno nuevo en el mismo horario/sillón/profesional pasa a ser aceptado por el validador de C-02.
- No cambia la creación ni la consulta de turnos; el chequeo de choques de C-02 ya excluye los turnos `CANCELADO`, por lo que aquí no se toca.

## Capabilities

### New Capabilities

_(ninguna)_

### Modified Capabilities

- `turnos`: se agregan los requisitos de cancelación de turno (anticipación mínima de 6 h, idempotencia de estado, trazabilidad sin borrado y liberación del horario) a la spec archivada de C-02; los requisitos existentes de creación y consulta no cambian.

## Impact

- **Código nuevo** (scope de C-08 en `CHANGES.md`): `domain/rules/cancelacion/**`, `application/cancelacion/**`, `web/api/cancelacion/**` y tests espejo en `src/test`.
- **Código existente reutilizado sin modificar**: `Turno.marcarCancelado` (ya valida `TURNO_YA_CANCELADO`), `CodigoError` (ya contiene `CANCELACION_TARDIA` y `TURNO_YA_CANCELADO`), `TurnosProperties.cancelacionHorasMinimas()`, `TurnoRepository`, `Clock`/`RelojConfig` de C-01.
- **Archivos compartidos**: no se editan `pom.xml`, ni `domain/model`, `domain/port`, `domain/error`, `infrastructure/persistence`, `web/api/error` (regla E1); si apareciera un hueco ahí, pedírselo al líder.
- **API**: nueva ruta `POST /api/turnos/{id}/cancelacion` (200 / 404 / 422 con Problem Detail y `codigo`). El enforcement de roles (SECRETARIA/ADMIN) corresponde a C-05, no a este change.
- **Dependencias**: requiere C-02 archivado (spec `turnos` y su código en `main`); lo consume C-10 (botón cancelar) y C-11.
