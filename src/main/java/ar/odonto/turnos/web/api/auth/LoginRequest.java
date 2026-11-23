package ar.odonto.turnos.web.api.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales de login enviadas al cliente (solo el cuerpo de la request; nunca se
 * devuelve ni se loguea la contraseña - RN-AU-02).
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
