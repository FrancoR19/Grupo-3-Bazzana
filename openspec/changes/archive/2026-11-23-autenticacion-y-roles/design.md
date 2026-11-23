# Design

## Context

C-01 dejó una configuración de seguridad permisiva (`PermisivaSecurityAutoConfiguration` con `SecurityFilterChain` que permite todo, `UsuarioActual` que devuelve `Solicitante.SISTEMA` y un `UserDetailsService` que rechaza cualquier usuario), todos marcados con `@ConditionalOnMissingBean` para que C-05 los reemplace sin editar archivos compartidos (regla anti-colisión 6 de `CHANGES.md`, design D8 de C-01). La entidad `Usuario` (con `Rol` `ADMIN`/`SECRETARIA`/`ODONTOLOGO` y `profesionalId` opcional), `UsuarioRepository` (con `findByUsername`), `PasswordConfig` (BCrypt `PasswordEncoder`), `SeedProperties` y `DatosDeEjemploDev` ya existen. El paquete `infrastructure/security/` no existe aún y es de propiedad exclusiva de C-05.

## Goals / Non-Goals

**Goals:**
- Reemplazar la seguridad permisiva de C-01 con un `SecurityFilterChain` propio de C-05 que aplique autenticación y autorización por ruta.
- Autenticar usuarios contra `Usuario` con hash BCrypt y establecer sesión de Spring Security para vistas Thymeleaf.
- Implementar `UsuarioActual`/`Solicitante` con rol y `profesionalId` vinculado.
- Exponer `POST /api/auth/login` con 401 `NO_AUTENTICADO` y 403 `ACCESO_DENEGADO` (códigos existentes, sin agregar nuevos).
- Aplicar la matriz RBAC por ruta según `knowledge-base/03_actores_y_roles.md`; rutas públicas sin autenticación.
- Rechazar `ODONTOLOGO` sin `Profesional` vinculado (RN-AU-04).
- No editar archivos compartidos de C-01 (regla anti-colisión 1 y 6).

**Non-Goals:**
- JWT no se implementa en esta fase (ver decisión IN-01 abajo); queda para etapas posteriores si la API se expone a otro cliente.
- Vista Thymeleaf propia de `/login`: se deja para C-09 (la ola 1 no agrega pantallas, por tanto no dispara el gate de `design-system`).
- Gestión de usuarios (alta/edición/baja de `Usuario`): queda fuera del alcance de C-05; solo se autentican los usuarios semilla de `DatosDeEjemploDev`.
- Autorización por anotación `@PreAuthorize` en controladores de la ola 1: los permisos se aplican por ruta en la cadena de seguridad de C-05.

## Decisions

### D1. IN-01 resuelto: sesión de Spring Security para vistas Thymeleaf; JWT solo para `/api/**` si se expone a otro cliente

**Decisión**: usar form login con sesión de Spring Security para las vistas Thymeleaf y **no** emitir JWT en esta fase. JWT solo se considera para `/api/**` si en etapas posteriores se expone la API a otro cliente (SPA o móvil).

**Razonamiento**: DD-05 eligió Thymeleaf (vistas renderizadas en el servidor, un solo proyecto y toolchain para 6 personas en 5 días). Con Thymeleaf, el navegador no envía un header `Authorization` en cada navegación sin JavaScript adicional, y guardar el token en una cookie obliga a resolver CSRF de todos modos (IN-01, `knowledge-base/10_preguntas_abiertas.md`). La sesión de Spring Security es el mecanismo natural para Thymeleaf y no agrega dependencias nuevas (Spring Security ya está en `pom.xml` por C-01).

**Alternativas consideradas:**
- JWT para todo (incluido `/api/**`): descartado para esta fase porque Thymeleaf no envía `Authorization` header por navegación normal y requeriría cookie + CSRF, agregando complejidad sin beneficio en la v1.
- OAuth2 / OpenID Connect: descartado, fuera de alcance del MVP y agregaría dependencias externas.

**Registro de la decisión**: IN-01 queda resuelto en este `design.md` (D1) y se comunica al equipo mediante el PR de C-05. El líder revisa el `design.md` antes del merge (governance CRITICO de C-05).

### D2. Estructura de paquetes de C-05 (sin tocar archivos compartidos)

- `infrastructure/security/`: `SecurityFilterChain` propio, `UserDetailsServiceImpl` sobre `Usuario`, `UsuarioActualImpl` que mapea `Usuario` → `Solicitante` (rol + `profesionalId`), y configuración de rutas públicas y RBAC por ruta.
- `application/acceso/`: caso de uso de autenticación (validar credenciales contra `UsuarioRepository` y `PasswordEncoder`).
- `web/api/auth/`: `AuthController` con `POST /api/auth/login`.
- Todos bajo `ar.odonto.turnos`, en los paquetes asignados a C-05 en `CHANGES.md` §Asignación del equipo.

**Razonamiento**: cumple la regla anti-colisión 2 (cada change agrega código solo en su propio paquete) y la 6 (seguridad desacoplada, beans propios que hacen retirar los de C-01 por `@ConditionalOnMissingBean`).

### D3. No nuevos códigos de error

Se reutilizan `NO_AUTENTICADO(401)` y `ACCESO_DENEGADO(403)` de `CodigoError` (definidos en C-01, design D7). No se agrega ningún código nuevo (regla anti-colisión 5 de `CHANGES.md`).

### D4. Contraseñas de seed solo por variables de entorno

Las contraseñas de los usuarios semilla (`admin`, `secretaria`, `ana`) se leen de `SEED_ADMIN_PASSWORD`, `SEED_SECRETARIA_PASSWORD` y `SEED_ODONTOLOGO_PASSWORD` vía `SeedProperties` y `application-dev.yml`, nunca se versionan ni se loguean (RN-AU-02, RN-DP-01, `knowledge-base/08_arquitectura_propuesta.md` §Variables de entorno).

## Risks / Trade-offs

- **[Sesión vs. JWT para `/api/**`]** → La sesión funciona para Thymeleaf pero un cliente externo (SPA/móvil) no la usaría naturalmente. Mitigación: JWT se agrega solo si se decide exponer la API a otro cliente (IN-01 resuelto, revisable en etapas posteriores).
- **[ODONTOLOGO sin Profesional]** → `Usuario` valida en el constructor que un ODONTOLOGO debe tener `profesionalId`, pero el seed o una migración podrían crear un ODONTOLOGO sin vínculo. Mitigación: `UsuarioActualImpl` rechaza al autenticar si el rol es ODONTOLOGO y `profesionalId` es nulo (RN-AU-04).
- **[CSRF con form login]** → Spring Security con sesión activa CSRF por defecto para `POST`. El login de Spring Security maneja CSRF con el token en el form; los tests con `spring-security-test` (`formLogin`) lo cubren. Para `POST /api/auth/login` consumido por un cliente no navegador, se evalúa si se necesita deshabilitar CSRF solo en esa ruta.
- **[Colisión de beans]** → C-05 declara beans con los mismos nombres/tipos que `PermisivaSecurityAutoConfiguration`; por `@ConditionalOnMissingBean`, los de C-01 se retiran solos. No se edita `PermisivaSecurityAutoConfiguration` ni `META-INF/spring/...AutoConfiguration.imports`.

## Migration Plan

No requiere migración de base de datos ni nuevas tablas: se reutiliza `Usuario` y `UsuarioRepository` de C-01. No se agregan variables de entorno obligatorias (las de seed son opcionales y ya documentadas en C-01). El despliegue es aditivo; el rollback consiste en revertir la rama `feat/autenticacion-y-roles`, tras lo cual los beans de C-01 vuelven a activarse por `@ConditionalOnMissingBean`.

## Open Questions

Ninguna. IN-01 está resuelto (D1) y queda registrado para el equipo.
