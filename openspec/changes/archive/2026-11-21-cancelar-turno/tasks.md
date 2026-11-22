# Tasks

## 1. Regla de cancelación en dominio puro

- [x] 1.1 Crear `domain/rules/cancelacion/` con `ContextoDeCancelacion` (turnoId, estado, inicio, ahora, anticipacionMinima) y `ValidadorDeCancelacion` que valida anticipación (RN-CA-01) tirando `ReglaDeNegocioException(CANCELACION_TARDIA)`; verificar con `ValidadorDeCancelacionTest` (JUnit puro, sin Spring): acepta con más de 6 h, acepta con exactamente 6 h, rechaza con 5 h 59 min y rechaza con inicio ya pasado.
- [x] 1.2 Cubrir el orden de validación estado → anticipación en `ValidadorDeCancelacionTest` (escenario: turno ya cancelado con inicio en el pasado → `TURNO_YA_CANCELADO`, no `CANCELACION_TARDIA`); verificar que el test falla si se invierte el orden.

## 2. Caso de uso CancelarTurno

- [x] 2.1 Crear `application/cancelacion/CancelarTurno` (`@Service`, `@Transactional`, inyección por constructor) con el orden del design D2 (404 → `TURNO_YA_CANCELADO` → `CANCELACION_TARDIA` → `marcarCancelado(ahora)`), usando `TurnosProperties.anticipacionMinimaCancelacion()` y el `Clock` inyectado; verificar con test de aplicación que el turno inexistente lanza `RECURSO_INEXISTENTE`.
- [x] 2.2 Devolver el record propio `TurnoCanceladoVista` (campos de `TurnoVista` + `canceladoEn`) armado dentro de la transacción; verificar con test de aplicación que la cancelación feliz devuelve estado `CANCELADO` y `canceladoEn` igual al `Clock` fijo, y que la segunda cancelación lanza `TURNO_YA_CANCELADO` con el `canceladoEn` original intacto.
- [x] 2.3 Verificar con test de aplicación que con `cancelacionHorasMinimas = 6` el borde de exactamente 6 h guarda `CANCELADO` y el de 5 h 59 min lanza `CANCELACION_TARDIA` dejando el turno `RESERVADO` (usar `RelojDePrueba` de C-02, nunca `LocalDateTime.now()`).

## 3. Endpoint de cancelación

- [x] 3.1 Crear `web/api/cancelacion/CancelacionController` con `POST /api/turnos/{id}/cancelacion` → 200 y el record `TurnoCanceladoResponse` (más `canceladoEn`), sin lógica de negocio ni ediciones a `TurnoController`/`TurnoResponse` de C-02; verificar con test MockMvc (`CancelacionApiTest`, espejo de `TurnosApiTest` con `RelojAjustable`) que la cancelación feliz responde 200 con `estado` `CANCELADO` y `canceladoEn`.
- [x] 3.2 Agregar al `CancelacionApiTest` los escenarios de error: inexistente → 404 `RECURSO_INEXISTENTE`; segunda cancelación → 422 `TURNO_YA_CANCELADO`; menos de 6 h → 422 `CANCELACION_TARDIA` con el turno sin cambios; verificar cada uno en rojo-verde con su test.
- [x] 3.3 Verificar en `CancelacionApiTest` que la respuesta no incluye DNI ni teléfono del paciente (regla D1) y que el detalle `GET /api/turnos/{id}` posterior muestra `CANCELADO` con los datos originales.

## 4. Prueba de liberación del horario (RN-CA-02)

- [x] 4.1 Test de integración espejo (p. ej. `application/cancelacion/LiberarHorarioTest`): crea un turno con el caso de uso `CrearTurno` de C-02, lo cancela con `CancelarTurno` y vuelve a crear otro turno en el mismo horario, sillón y profesional; verificar que el segundo crea con 201/`TurnoVista` y que sin cancelar el primero habría sido `CHOQUE_SILLON`.

## 5. Verificación final

- [x] 5.1 Correr `./mvnw test` completo en verde sin tests `@Disabled` ni `-DskipTests` (regla J4), con al menos un test por escenario de `specs/turnos/spec.md`; verificar también `openspec validate cancelar-turno --strict` sin errores.
- [x] 5.2 Confirmar que no se tocó ningún archivo compartido de C-01 ni de C-02 (regla E1) revisando `git diff --stat` contra `main`.

## Workflow follow-up

- Abrir PR `feat/cancelar-turno` a `main` (nunca commitear directo a `main`, E2) con conventional commits en español (E4).
- Archivar el change con `openspec archive cancelar-turno` (C-02 ya está archivado, el delta se aplica sobre la spec `turnos`) y verificar con `openspec list --specs`.
- Marcar `[x]` en `CHANGES.md` §C-08.
- Avisar a C-10 (botón cancelar) y a C-11 que C-08 quedó en `main`.
