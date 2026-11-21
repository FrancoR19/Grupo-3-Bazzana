package ar.odonto.turnos.web.api.pacientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de {@code PUT /api/pacientes/{id}}; los campos nulos conservan el valor actual.
 * En blanco o solo espacios se rechaza (400); el DNI, si viene, debe ser idéntico al actual.
 */
public record EditarPacienteRequest(@Pattern(regexp = ".*\\S.*") String nombre,
                                    @Pattern(regexp = ".*\\S.*") String apellido,
                                    @Pattern(regexp = ".*\\S.*") String dni,
                                    @Pattern(regexp = "\\d{8,15}") String telefono,
                                    @Email String email) {
}
