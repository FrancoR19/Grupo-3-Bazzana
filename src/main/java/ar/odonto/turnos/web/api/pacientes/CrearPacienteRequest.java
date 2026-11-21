package ar.odonto.turnos.web.api.pacientes;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de {@code POST /api/pacientes}. El teléfono es opcional; si viene, debe ser internacional sin "+". */
public record CrearPacienteRequest(@NotBlank String nombre, @NotBlank String apellido, @NotBlank String dni,
                                   @Pattern(regexp = "\\d{8,15}") String telefono, @Email String email) {
}
