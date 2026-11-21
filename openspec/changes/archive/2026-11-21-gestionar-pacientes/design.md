# Design

## Context

C-01 (ya archivado) aporta todo lo compartido: entidad `Paciente`, puerto `PacienteRepository` (con `existsByDni`/`findByDni`), catálogo `CodigoError` (con `DATO_DUPLICADO`, `RECURSO_INEXISTENTE`, `ENTRADA_INVALIDA`), `ManejadorGlobalDeErrores` (ProblemDetail con campo `codigo`) y una `SecurityFilterChain` permisiva reemplazable. C-03 (`configuracion-consultorio`) ya mergeó el patrón de casos de uso `@Service` + `Comando`/`Vista` + controller delgado, que este change repite. Ver propuesta en `proposal.md` y requisitos en `specs/pacientes/spec.md`.

## Goals / Non-Goals

**Goals:**
- Alta, edición, lectura y búsqueda de pacientes por API, con un test por escenario de la spec.
- Cero ediciones en archivos compartidos de C-01 (reglas anti-colisión 1, 4 y 5 de `CHANGES.md`).
- Verificable por test que la capa no filtra DNI/teléfono/nombre completo a los logs (RN-DP-03).

**Non-Goals:**
- Reglas de acceso por rol: la matriz RBAC de pacientes la aplica C-05 por ruta (anti-colisión 6); este change no agrega anotaciones de seguridad.
- Pantallas Thymeleaf (C-09) y link de WhatsApp (C-06).
- Baja/borrado de pacientes: no existe `DELETE` ni flag de activo en el modelo.
- Historia clínica o datos de salud (fuera del MVP, US-102).

## Decisions

1. **Paquetes propios**: `application/pacientes/**` (casos de uso + `PacienteVista`) y `web/api/pacientes/**` (controller + records request/response), con tests espejo en `src/test/.../application/pacientes/` y `src/test/.../web/api/pacientes/`. Alternativa descartada: reutilizar `application/configuracion/` (colisionaría con el paquete de C-03).
2. **Reutilizar el puerto de C-01 sin editarlo**: la búsqueda `q` se resuelve **en el caso de uso** filtrando sobre `findAll()` (nombre, apellido o DNI, insensible a mayúsculas) y ordenando por apellido, nombre. Alternativas descartadas: agregar `searchBy...` al puerto compartido o crear un `JpaPacienteRepository` nuevo en `infrastructure/persistence/` — ambos son archivos de C-01 (anti-colisión 4).
3. **Patrón de casos de uso espejo de C-03**: clases `@Service` con inyección por constructor (J3), `record Comando` de entrada y `PacienteVista` (record) de salida — la API nunca devuelve la entidad JPA (J2). Ninguna regla vive en el controller (J1); `@Transactional` solo en los casos de uso.
4. **DNI duplicado**: chequeo explícito `existsByDni` → `ReglaDeNegocioException(DATO_DUPLICADO)` → 409, sin guardar. Respaldado por el `unique` de la columna y el manejo global de `DataIntegrityViolationException` ya existente (409 `DATO_DUPLICADO` ante la carrera), que no requiere tocar el manejador.
5. **Teléfono: formato, no regla de negocio** → `@Pattern(regexp = "\\d{8,15}")` sobre los records de request (permite `null`, rechaza `+`, espacios y letras) → 400 `ENTRADA_INVALIDA` vía el validador global. Criterio heredado de C-03: formato inválido = 400; solo las invariantes de dominio son 422. Alternativa descartada: 422, que implicaría agregar un código o reabrir `CodigoError` (compartido).
6. **DNI inmutable en la edición** (espejo de la matrícula en C-03): si `dni` viene distinto del actual → 400 `ENTRADA_INVALIDA` antes de consultar unicidad; si viene igual o `null`, no se consulta el puerto. Edición parcial: campos `null` conservan el valor actual.
7. **Validación de entrada con Bean Validation** en los records de request (`@NotBlank` en nombre, apellido y dni; `@Email` opcional en email) → 400 `ENTRADA_INVALIDA` con el campo, antes de cualquier regla de negocio.
8. **RN-DP-03 verificado por test**: un test de API agrega un `ListAppender` de Logback al logger raíz, ejecuta alta, edición y listado con un paciente ficticio, y afirma que ningún evento de log contiene el DNI, el teléfono ni "nombre apellido" juntos. Se apoya en que `Paciente.toString()` (C-01) ya solo expone el `id`.
9. **Datos ficticios**: fixtures creados en el paquete de tests del change (DNI `0000000X`, teléfono `549110000000X`, nombres tipo "Paciente Uno"), sin tocar `support/DatosDePrueba.java` ni el seed (RN-DP-01, reglas D1 y anti-colisión 7).

## Risks / Trade-offs

- [Carrera entre dos altas con el mismo DNI] → el chequeo previo no es atómico; la constraint `unique` + el manejo global de integridad devuelven 409 `DATO_DUPLICADO` igual. Cubierto con un test de integridad si se quiere.
- [`findAll()` en memoria para la búsqueda] → suficiente para consultorios de 1-5 profesionales (MVP); si la base crece, se deriva una query en una interfaz del propio paquete de pacientes, sin tocar C-01.
- [No se puede vaciar el teléfono desde la edición] → `""` es rechazado por el patrón y `null` conserva el actual; el alta sin teléfono ya cubre el caso de uso de C-06 (`PACIENTE_SIN_TELEFONO`). Limitación aceptada y documentada.
- [El test de logs puede ser frágil si otro componente loguea durante la operación] → se captura solo durante las operaciones de pacientes y se afirma ausencia de PII; si aparece un log ajeno con datos, es un hallazgo real, no un falso positivo.
- [Gobierno MEDIO (datos personales)] → sin datos reales en ningún artefacto (D1) y revisión del PR enfocada en logs y fixtures.

## Migration Plan

Sin despliegue ni migración de datos: es una capability nueva sobre la base H2 en memoria. Rollback = revertir el PR (no deja estado persistido). Se archiva el change al verificar `mvnw.cmd test` en verde.

## Open Questions

Ninguna que cambie los specs, el enfoque o el desglose de tareas.
