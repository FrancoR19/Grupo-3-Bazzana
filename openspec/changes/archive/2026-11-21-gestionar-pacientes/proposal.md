# Proposal

## Why

Sin un alta de pacientes no se puede agendar ningún turno: `Turno` exige un `pacienteId` y el único camino para obtenerlo hoy es el seed de desarrollo. Este change habilita la Épica 6 (US-050) para que la secretaria registre, edite y busque pacientes por la API, con DNI único y teléfono en formato internacional, cumpliendo las reglas de datos personales RN-DP (Ley 25.326).

## What Changes

- Casos de uso nuevos en `application/pacientes/`: alta, edición y listado/búsqueda de `Paciente` (nombre, apellido, DNI único, teléfono E.164 sin `+`, email opcional).
- API nueva en `web/api/pacientes/`: `GET /api/pacientes` (listado con filtro de búsqueda opcional), `POST /api/pacientes`, `PUT /api/pacientes/{id}` y `GET /api/pacientes/{id}`.
- Validación de entrada con Bean Validation (campos obligatorios) y rechazo de DNI duplicado con 409 / `DATO_DUPLICADO` antes de tocar la base.
- Normalización del teléfono: se rechaza el formato con `+` o espacios; se admite el formato internacional sin `+` (por ejemplo `5491100000001`), y el alta sin teléfono (opcional, aunque bloquea el link de WhatsApp en C-06).
- RN-DP-03 verificada por test: la capa de pacientes no escribe en logs el DNI, el teléfono ni el nombre completo del paciente (solo ids).
- Sin datos reales de pacientes en fixtures, seeds ni tests (RN-DP-01 / regla D1).

## Capabilities

### New Capabilities

- `pacientes`: alta, edición, lectura y búsqueda de pacientes con DNI único, teléfono en formato internacional y protección de datos personales (RN-DP-01, RN-DP-03).

### Modified Capabilities

<!-- ninguna: no se cambian requisitos de specs existentes -->

## Impact

- **Código nuevo** (solo en los paquetes propios de C-04, anti-colisión 2): `application/pacientes/**`, `web/api/pacientes/**` y sus tests espejo (`src/test/.../application/pacientes/`, `src/test/.../web/api/pacientes/`).
- **Reutiliza sin editar** (compartidos de C-01): `domain/model/Paciente`, `domain/port/PacienteRepository`, `domain/error/CodigoError` (`DATO_DUPLICADO`, `RECURSO_INEXISTENTE`, `ENTRADA_INVALIDA`), `ManejadorGlobalDeErrores`.
- **API**: se agregan rutas nuevas bajo `/api/pacientes`; no se tocan las de C-02/C-03.
- **Seguridad**: los controladores NO llevan anotaciones de seguridad (regla 6 anti-colisión); la matriz RBAC de pacientes (ADMIN y SECRETARIA CLM, ODONTOLOGO solo los de sus turnos) la aplica C-05 por ruta.
- **Dependencias**: `C-01` (ya archivado). Sin cambios de `pom.xml`.
- **Consumidores posteriores**: C-02 (`pacienteId` al crear turnos), C-06 (teléfono para `wa.me`), C-09 (pantalla de pacientes).
