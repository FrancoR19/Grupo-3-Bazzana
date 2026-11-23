package ar.odonto.turnos.web.api.auth;

/**
 * Respuesta del login exitoso. No incluye token JWT (IN-01: sesión de Spring Security)
 * ni el hash de la contraseña (RN-AU-02).
 */
public record LoginResponse(String usuario, String rol, Long profesionalId) {
}
