# Tasks

## 1. Alta de paciente

- [x] 1.1 Crear el paquete `application/pacientes/` con el caso de uso `AltaPaciente` (`@Service`, inyección por constructor, `record Comando`, `@Transactional`) y el record `PacienteVista`, verificando que compila con `mvnw.cmd -q compile` y que no se edita ningún archivo fuera de `application/pacientes/**`
- [x] 1.2 Verificar el alta feliz con un test de API (`POST /api/pacientes` → 201 con datos, `id` y `Location`) usando un fixture ficticio propio del paquete de tests, en verde con `mvnw.cmd test`
- [x] 1.3 Verificar el alta sin teléfono con un test de API (sin campo `telefono` → 201 con `telefono` null) en verde con `mvnw.cmd test`
- [x] 1.4 Verificar el DNI duplicado con un test de API (mismo DNI → 409 con `codigo` `DATO_DUPLICADO` y el registro original intacto) en verde con `mvnw.cmd test`

## 2. Validación de entrada y formato del teléfono

- [x] 2.1 Agregar Bean Validation a los records de request de `web/api/pacientes/` (`@NotBlank` en nombre, apellido y dni; `@Pattern("\\d{8,15}")` en teléfono; `@Email` opcional) y crear `PacienteController` con el `POST`, verificando con un test que el nombre ausente devuelve 400 con `codigo` `ENTRADA_INVALIDA` y menciona el campo
- [x] 2.2 Verificar el teléfono rechazado con `+` (`+5491100000001` → 400 `ENTRADA_INVALIDA`, sin guardar) en un test en verde con `mvnw.cmd test`
- [x] 2.3 Verificar el teléfono con espacios (`549 11 0000-0001` → 400 `ENTRADA_INVALIDA`) en un test en verde con `mvnw.cmd test`

## 3. Edición de paciente

- [x] 3.1 Crear el caso de uso `EditarPaciente` (edición parcial: campos `null` conservan el actual; DNI inmutable → 400 `ENTRADA_INVALIDA`) con el `PUT /api/pacientes/{id}`, verificando con un test que apellido y teléfono se actualizan y el DNI permanece en 200
- [x] 3.2 Verificar el intento de cambiar el DNI con un test (DNI distinto → 400 `ENTRADA_INVALIDA` y el DNI no se modifica) en verde con `mvnw.cmd test`
- [x] 3.3 Verificar la edición sobre un id inexistente con un test (→ 404 con `codigo` `RECURSO_INEXISTENTE`) en verde con `mvnw.cmd test`
- [x] 3.4 Verificar el `dni` en blanco en la edición con un test (→ 400 `ENTRADA_INVALIDA` y conserva los datos anteriores) en verde con `mvnw.cmd test`

## 4. Lectura y búsqueda de pacientes

- [x] 4.1 Crear el caso de uso `ListarPacientes` con filtro opcional `q` (nombre, apellido o DNI, insensible a mayúsculas; orden por apellido y nombre) sobre `findAll()` sin editar el puerto, verificando con un test de API que `GET /api/pacientes` devuelve todos los pacientes como DTOs
- [x] 4.2 Verificar la búsqueda por DNI parcial (`?q=0000` → solo ese paciente) y la búsqueda sin coincidencias (→ lista vacía) con tests en verde con `mvnw.cmd test`
- [x] 4.3 Verificar el listado sobre base vacía (→ 200 con lista vacía) con un test en verde con `mvnw.cmd test`
- [x] 4.4 Verificar `GET /api/pacientes/{id}` con tests: existente → 200, inexistente → 404 con `codigo` `RECURSO_INEXISTENTE`, en verde con `mvnw.cmd test`

## 5. Protección de datos (RN-DP-03 y RN-DP-01)

- [x] 5.1 Escribir el test de captura de logs (ListAppender de Logback durante alta, edición y listado de un paciente ficticio) que afirma ausencia de DNI, teléfono y nombre completo en los mensajes, en verde con `mvnw.cmd test`
- [x] 5.2 Revisar los fixtures y mensajes de todo el change confirmando solo datos ficticios (DNI `0000000X`, teléfono `549110000000X`) y logs que solo citan ids, dejando la evidencia en el resumen del PR

## 6. Verificación final

- [x] 6.1 Correr `openspec validate gestionar-pacientes --strict` sin errores ni warnings
- [x] 6.2 Correr `mvnw.cmd test` completo en verde, con al menos un test por escenario de `specs/pacientes/spec.md`
- [x] 6.3 Verificar con `git diff --name-only origin/main...HEAD` que solo aparecen rutas de C-04 (`application/pacientes/**`, `web/api/pacientes/**`, tests espejo y `openspec/changes/gestionar-pacientes/**`)

## Workflow follow-up

- Marcar el checkbox de C-04 en `CHANGES.md` y correr `openspec-archive-change` como último paso de la rama `feat/gestionar-pacientes`.
- Abrir el PR `feat(pacientes): ...` a `main` con `mvnw.cmd test` en verde tras el rebase.
