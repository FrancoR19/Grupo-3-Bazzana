package ar.odonto.turnos.application.pacientes;

import ar.odonto.turnos.domain.model.Paciente;

/** Datos de un paciente que salen del caso de uso; nunca la entidad JPA (J2). */
public record PacienteVista(Long id, String nombre, String apellido, String dni, String telefono, String email) {

    static PacienteVista de(Paciente paciente) {
        return new PacienteVista(paciente.getId(), paciente.getNombre(), paciente.getApellido(), paciente.getDni(),
                paciente.getTelefono(), paciente.getEmail());
    }
}
