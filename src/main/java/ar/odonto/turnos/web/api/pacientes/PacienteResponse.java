package ar.odonto.turnos.web.api.pacientes;

import ar.odonto.turnos.application.pacientes.PacienteVista;

/** Representación HTTP de un paciente: sin entidades JPA (J2); datos sensibles bajo Ley 25.326 (RN-DP-02). */
public record PacienteResponse(Long id, String nombre, String apellido, String dni, String telefono, String email) {

    static PacienteResponse de(PacienteVista vista) {
        return new PacienteResponse(vista.id(), vista.nombre(), vista.apellido(), vista.dni(), vista.telefono(),
                vista.email());
    }
}
