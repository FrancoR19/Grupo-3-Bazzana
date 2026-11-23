# Spec Delta

## Purpose

Define la autenticación de usuarios del consultorio con usuario y contraseña (hash BCrypt), la autorización por rol según la matriz RBAC, las rutas públicas y el endpoint de login, sin editar archivos compartidos de C-01 (US-040, US-041, RN-AU-01 a RN-AU-04).

## ADDED Requirements

### Requirement: Autenticación de usuario con form login y sesión

El sistema SHALL autenticar al usuario mediante `POST /api/auth/login` con credenciales de usuario y contraseña, estableciendo una sesión de Spring Security para las vistas Thymeleaf (RN-AU-01, RN-AU-02, US-040).

#### Scenario: Login correcto con credenciales válidas

- **GIVEN** existe un usuario `secretaria` con contraseña almacenada como hash BCrypt en `Usuario`
- **WHEN** se invoca `POST /api/auth/login` con el usuario y la contraseña correctos
- **THEN** la respuesta es 200 y se establece una sesión autenticada para ese usuario

#### Scenario: Credenciales inválidas rechazadas con 401

- **GIVEN** se invoca `POST /api/auth/login` con una contraseña incorrecta para un usuario existente
- **WHEN** se intenta autenticar
- **THEN** la respuesta es 401 con Problem Details y `codigo` igual a `NO_AUTENTICADO`

#### Scenario: Usuario inexistente rechazado con 401

- **GIVEN** se invoca `POST /api/auth/login` con un usuario que no existe
- **WHEN** se intenta autenticar
- **THEN** la respuesta es 401 con Problem Details y `codigo` igual a `NO_AUTENTICADO`

### Requirement: Contraseñas almacenadas con hash BCrypt

El sistema SHALL almacenar las contraseñas únicamente como hash BCrypt y MUST loguearlas en texto plano ni guardarlas sin cifrar (RN-AU-02, US-040).

#### Scenario: La contraseña nunca se registra en texto plano

- **GIVEN** un login exitoso de un usuario semilla
- **WHEN** se procesa la autenticación
- **THEN** ningún evento de log contiene la contraseña en texto plano y `Usuario.passwordHash` nunca se expone en la respuesta de la API

### Requirement: Rutas públicas sin autenticación

El sistema SHALL permitir el acceso sin autenticación a las siguientes rutas públicas: `GET /login`, `POST /api/auth/login`, recursos estáticos (`/css/**`, `/js/**`, `/img/**`), `GET /actuator/health` y `/h2-console` solo con el perfil `dev` (RN-AU-01, `03_actores_y_roles.md`).

#### Scenario: Ruta de health accesible sin login

- **GIVEN** ninguna sesión autenticada
- **WHEN** se consulta `GET /actuator/health`
- **THEN** la respuesta es 200 sin requerir autenticación

#### Scenario: Ruta de login accesible sin sesión previa

- **GIVEN** ninguna sesión autenticada
- **WHEN** se invoca `POST /api/auth/login`
- **THEN** la solicitud se procesa sin requerir una sesión previa

#### Scenario: Consola H2 solo en perfil dev

- **GIVEN** la aplicación ejecuta con un perfil distinto de `dev`
- **WHEN** se consulta `GET /h2-console`
- **THEN** la ruta no está expuesta (o redirige/requiere autenticación según el perfil activo)

### Requirement: Todo lo demás requiere autenticación

El sistema SHALL rechazar con 401 cualquier acceso a rutas no públicas cuando no exista una sesión autenticada (RN-AU-01).

#### Scenario: Acceso a ruta protegida sin autenticación

- **GIVEN** ninguna sesión autenticada
- **WHEN** se consulta una ruta protegida (por ejemplo `GET /api/agenda` o `POST /api/turnos`)
- **THEN** la respuesta es 401 con Problem Details y `codigo` igual a `NO_AUTENTICADO`

### Requirement: Autorización por rol con matriz RBAC

El sistema SHALL aplicar los permisos por rol según la matriz RBAC de `03_actores_y_roles.md`, devolviendo 403 cuando el rol autenticado no tiene acceso al recurso (RN-AU-03, US-041).

#### Scenario: ODONTOLOGO intentando acceder a ruta de ADMIN

- **GIVEN** un usuario autenticado con rol `ODONTOLOGO`
- **WHEN** se intenta acceder a una ruta de configuración exclusiva de `ADMIN` (por ejemplo `POST /api/profesionales`)
- **THEN** la respuesta es 403 con Problem Details y `codigo` igual a `ACCESO_DENEGADO`

#### Scenario: SECRETARIA accediendo a ruta permitida para su rol

- **GIVEN** un usuario autenticado con rol `SECRETARIA`
- **WHEN** se consulta `POST /api/turnos` (crear turno, permitido para SECRETARIA)
- **THEN** la respuesta no es 403 y la operación prosigue según el rol

### Requirement: ODONTOLOGO vinculado a un Profesional

El sistema SHALL rechazar la autenticación de un usuario con rol `ODONTOLOGO` que no esté vinculado a un `Profesional` (RN-AU-04, US-003).

#### Scenario: ODONTOLOGO sin profesional vinculado rechazado

- **GIVEN** existe un usuario con rol `ODONTOLOGO` y `profesionalId` nulo
- **WHEN** se invoca `POST /api/auth/login` con sus credenciales
- **THEN** la respuesta es 401 (o 403 tras la autenticación) indicando que el usuario no está vinculado a un profesional

### Requirement: Identidad del solicitante vinculada al usuario autenticado

El sistema SHALL proveer una implementación de `UsuarioActual` que devuelva un `Solicitante` con el `usuario`, el `rol` y el `profesionalId` (solo para `ODONTOLOGO`) del usuario autenticado en la sesión en curso, para que los casos de uso de C-07 y posteriores lo reciban como parámetro sin depender de Spring Security (regla anti-colisión 6 de `CHANGES.md`).

#### Scenario: Solicitante expone rol y profesionalId del usuario autenticado

- **GIVEN** un usuario autenticado con rol `ODONTOLOGO` vinculado al profesional con id 7
- **WHEN** se consulta el `Solicitante` a través de `UsuarioActual.solicitante()` durante la sesión
- **THEN** devuelve `usuario` igual al nombre de usuario, `rol` igual a `ODONTOLOGO` y `profesionalId` igual a 7

### Requirement: JWT no se usa en esta fase

El sistema SHALL usar sesión de Spring Security para las vistas Thymeleaf y MUST no emitir ni validar JWT para `/api/**` en esta fase (resolución IN-01, `knowledge-base/10_preguntas_abiertas.md` y `design.md`). JWT solo se considera para `/api/**` si se expone la API a otro cliente en etapas posteriores.

#### Scenario: No se emite token JWT en el login

- **GIVEN** un login exitoso mediante `POST /api/auth/login`
- **WHEN** se inspecciona la respuesta
- **THEN** no se devuelve ningún token JWT y la autenticación se maneja mediante sesión de Spring Security
