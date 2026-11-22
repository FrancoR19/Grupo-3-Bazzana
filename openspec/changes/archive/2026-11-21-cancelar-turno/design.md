# Design

## Context

C-01 dejó el esqueleto (error handler con `ProblemDetail` + `codigo`, `Clock` inyectable, `TurnosProperties` con `cancelacion-horas-minimas: 6`) y C-02 dejó la spec `turnos` archivada, el caso de uso `CrearTurno`, `ConsultarTurno` y el `ValidadorDeTurno` puro en `domain/rules/turnos/`. El modelo ya trae `Turno.marcarCancelado(cuando)` que solo transiciona `RESERVADO → CANCELADO` y tira `TURNO_YA_CANCELADO`, y `CodigoError` ya contiene `CANCELACION_TARDIA` y `TURNO_YA_CANCELADO` con sus 422. `TurnoRepository` ya excluye los `CANCELADO` de todos los chequeos de choque, o sea que la "liberación" del horario (RN-CA-02) está garantida por el lado de la creación. Ver `proposal.md` para la motivación.

## Goals / Non-Goals

**Goals:**
- Expuesta la cancelación por API (`POST /api/turnos/{id}/cancelacion`) con las 4 reglas RN-CA, en el mismo estilo por capas que C-02.
- Reglas de cancelación en Java puro, testeables sin levantar Spring (J1) con `Clock` fijo.
- Probar de punta a punta que cancelar libera el hueco para `CrearTurno`.

**Non-Goals:**
- UI del botón "cancelar" (C-10) y enforcement de roles SECRETARIA/ADMIN (C-05, que aplica el endpoint con su mapa de rutas).
- Notificar al paciente por WhatsApp al cancelar (C-06/flujo 4).
- Estados ASISTIO/AUSENTE, reprogramación de turnos, listas de espera (US-112/US-114, futuros).
- Editar archivos de C-02 (`application/turnos/**`, `web/api/turnos/**`) o compartidos de C-01 (regla E1).

## Decisions

**D1. Regla en `domain/rules/cancelacion/` Java puro.** Una clase estática `ValidadorDeCancelacion` (espejo de `ValidadorDeTurno`) que recibe un record `ContextoDeCancelacion(turnoId, estado, inicio, ahora, anticipacionMinima)` y tira `ReglaDeNegocioException` con el `codigo` correspondiente. *Alternativa*: validar adentro del caso de uso con `if`s → descartada por J1 y porque C-02 ya establece el patrón validador puro con tests unitarios espejo.

**D2. Orden de validación: 404 → estado → anticipación.** En `CancelarTurno.ejecutar`: (1) `turnos.findById` inexistente → `RECURSO_INEXISTENTE` (404); (2) estado `!= RESERVADO` → `TURNO_YA_CANCELADO` (422); (3) `ahora > inicio − propiedades.anticipacionMinimaCancelacion()` → `CANCELACION_TARDIA` (422); (4) `turno.marcarCancelado(ahora)` + `save`. El orden (2) antes que (3) sale del Flujo 2 de la KB y es el que fija la spec (escenario "Turno cancelado cuyo inicio ya pasó"). *Alternativa*: delegar (2) en `marcarCancelado` después del chequeo de anticipación → descartada: un turno ya cancelado con inicio en el pasado devolvería `CANCELACION_TARDIA`.

**D3. Caso de uso propio en `application/cancelacion/CancelarTurno`.** `@Service`, `@Transactional`, constructor injection (J3), dependencias: `TurnoRepository`, `TurnosProperties`, `Clock`, `UsuarioActual` (para registrar quién canceló en el log, solo ID — D2 de AGENTS.md). Devuelve un record propio `TurnoCanceladoVista` (agrega `canceladoEn` a los campos de `TurnoVista`) armado dentro de la transacción (`open-in-view: false`). *Alternativa*: reutilizar `TurnoVista` → descartada porque no tiene `canceladoEn` y editarla significaría tocar un archivo de C-02.

**D4. Controlador propio en `web/api/cancelacion/CancelacionController`.** `@PostMapping("/api/turnos/{id}/cancelacion")` → 200 con `TurnoCanceladoResponse` (record público nuevo en el mismo paquete; incluye `estado` y `canceladoEn`). Sin lógica de negocio (J1): solo delega y traduce. *Alternativa*: agregar el método a `TurnoController` de C-02 → descartada por la regla anti-colisión de `CHANGES.md` (C-08 solo edita `web/api/cancelacion/**`). Los errores ya los traduce `ManejadorGlobalDeErrores` de C-01 (404/422 con `codigo`).

**D5. Sin cambios en la spec de creación.** El requisito existente "Los turnos cancelados no bloquean" ya cubre la exclusión en los choques; este delta solo agrega requisitos ADDED. Si al implementar apareciera un requisito nuevo que cambie comportamiento existente, volver a *propose* (E5).

**D6. Concurrencia.** No se agrega bloqueo especial: dos cancelaciones simultáneas del mismo turno pasan por la misma fila con chequeo de estado dentro de la transacción, y en el peor caso dos transacciones leen `RESERVADO` y ambas escriben `CANCELADO` con distinto `canceladoEn` (idempotencia de resultado, no de auditoría). Es el mismo nivel de garantía que PA-06 aceptó para C-02 y queda anotado en `10_preguntas_abiertas.md`; si el líder exige más, se resuelve en C-11.

## Risks / Trade-offs

- [El endpoint queda sin enforcement de rol hasta que entre C-05] → el mapa de rutas de C-05 debe cubrir `POST /api/turnos/*/cancelacion` (SECURITY: agregarlo ahí es responsabilidad de C-05; anotarlo en el PR de C-08).
- [Doble cancelación simultánea puede pisarse el `canceladoEn`] → aceptado en D6; el estado final es `CANCELADO` en ambos casos, que es lo observable.
- [Conflicto de delta si C-02 se modifica antes del archive de C-08] → C-08 se archive después de C-02 (GATE 1) y el delta solo agrega requisitos, no modifica los existentes, así el merge del delta es trivia.
- [Tests con `Clock.fixed` frágiles si se cuela `LocalDateTime.now()`] → regla D4: ningún test usa el reloj del sistema; `RelojDePrueba`/`RelojAjustable` de C-02 ya existen para eso.

## Migration Plan

Sin migración de datos: `TurnoEstado` y `canceladoEn` ya existen en el esquema desde C-01. Deploy único con el resto del monolito; rollback = revert del commit (la ruta es aditiva).

## Open Questions

- Ninguna que afecte specs, enfoque o desglose de tareas.
