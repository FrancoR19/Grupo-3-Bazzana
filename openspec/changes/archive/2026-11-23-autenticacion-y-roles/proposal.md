# Proposal

## Why

El change C-01 dejó una configuración de seguridad permisiva (`SecurityFilterChain` que permite todo, `UsuarioActual` que devuelve `Solicitante.SISTEMA` y un `UserDetailsService` que rechaza cualquier usuario) como placeholder reemplazable con `@ConditionalOnMissingBean`. Sin C-05 no existe autenticación: cualquier usuario puede acceder a todas las rutas de la API y de las vistas, no hay control de acceso por rol y no hay mecanismo para identificar quién hace cada operación (US-040, US-041, RN-AU-01 a RN-AU-04). Este change agrega autenticación y autorización por rol sobre la entidad `Usuario` existente, dejando listas las vistas Thymeleaf para C-09.

## What Changes

- Nueva carpeta `infrastructure/security/` con `SecurityFilterChain` propio (reemplaza al permisivo de C-01 vía `@ConditionalOnMissingBean`, sin editar `PermisivaSecurityAutoConfiguration`), `UserDetailsService` sobre `Usuario` con hash BCrypt (`PasswordConfig` existente), e implementación de `UsuarioActual`/`Solicitante` con el rol y el `profesionalId` vinculado.
- Nueva carpeta `application/acceso/` con caso de uso de autenticación (validar credenciales y devolver identidad del solicitante).
- Nueva carpeta `web/api/auth/` con endpoint `POST /api/auth/login` (credenciales inválidas → 401 `NO_AUTENTICADO`; acceso sin permiso → 403 `ACCESO_DENEGADO`).
- Matriz RBAC por ruta según `knowledge-base/03_actores_y_roles.md` aplicada en la cadena de seguridad de C-05; rutas públicas: `/login`, `POST /api/auth/login`, estáticos (`/css/**`, `/js/**`, `/img/**`), `GET /actuator/health` y `/h2-console` solo con perfil `dev`.
- RN-AU-04: un usuario `ODONTOLOGO` sin `Profesional` vinculado se rechaza al autenticar.
- Sin nuevos códigos de error: se reutilizan `NO_AUTENTICADO(401)` y `ACCESO_DENEGADO(403)` existentes en `CodigoError` (C-01, design D7).
- `/login` es la página de login por defecto de Spring Security; la vista Thymeleaf propia se deja para C-09 (la ola 1 no agrega pantallas, por tanto no dispara el gate de `design-system`).

## Capabilities

### New Capabilities

- `acceso`: autenticación de usuarios (form login con sesión Spring Security para vistas Thymeleaf), autorización por rol con matriz RBAC por ruta, rutas públicas, endpoint `POST /api/auth/login`, implementación de `UsuarioActual`/`Solicitante` vinculado a `Usuario` y `Profesional`, y resolución de IN-01 (sesión vs JWT) con decisión registrada en `design.md`.

### Modified Capabilities

_(ninguna)_

## Impact

- **Código nuevo** (exclusivamente en los paquetes asignados a C-05 en `CHANGES.md` §Asignación del equipo): `infrastructure/security/**`, `application/acceso/**`, `web/api/auth/**` y sus tests espejo en `src/test/java/ar/odonto/turnos/`.
- **Código existente reutilizado sin modificar**: `Usuario` (`domain/model`), `UsuarioRepository` y `UsuarioActual` (`domain/port`), `Solicitante` (`domain/model`), `Rol` (`domain/model`), `Profesional` (`domain/model`), `PasswordConfig` (`infrastructure/config`), `CodigoError` (`NO_AUTENTICADO` 401 y `ACCESO_DENEGADO` 403 ya definidos en C-01), `ReglaDeNegocioException` y `ManejadorGlobalDeErrores` (`web/api/error`).
- **Archivos compartidos**: cero ediciones en archivos de C-01. C-05 declara sus propios beans en `infrastructure/security/` y los de C-01 (`PermisivaSecurityAutoConfiguration`) se retiran solos por `@ConditionalOnMissingBean` (regla anti-colisión 6 de `CHANGES.md`).
- **API**: nueva ruta `POST /api/auth/login` (200 / 401 / 403 con Problem Details y campo `codigo`). Las rutas protegidas devuelven 401 sin sesión y 403 sin el rol requerido.
- **Seguridad desacoplada**: los controladores de la ola 1 (C-02, C-03, C-04, C-06, C-07) no llevan anotaciones de seguridad; los permisos por rol se aplican por ruta en la cadena de C-05.
- **Dependencias**: solo depende de `C-01` (ya en `main`). Es bloqueante para `C-09` (GATE 2: C-03 + C-04 + C-05) y es consumido por `C-07` (vía `Solicitante` autenticado) y `C-10`.
- **Seguridad**: contraseñas solo por variables de entorno (`SEED_ADMIN_PASSWORD`, etc.); nunca en texto plano ni en logs (RN-AU-02, RN-DP-03). JWT no se usa en esta fase (ver decisión IN-01 en `design.md`).
