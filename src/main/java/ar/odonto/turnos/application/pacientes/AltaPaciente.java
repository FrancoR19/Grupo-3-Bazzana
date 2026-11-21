package ar.odonto.turnos.application.pacientes;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Paciente;
import ar.odonto.turnos.domain.port.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta de paciente (US-050): DNI único (409) y teléfono opcional en formato internacional. */
@Service
public class AltaPaciente {

    public record Comando(String nombre, String apellido, String dni, String telefono, String email) {
    }

    private final PacienteRepository pacientes;

    public AltaPaciente(PacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    @Transactional
    public PacienteVista ejecutar(Comando comando) {
        String dni = comando.dni().trim();
        if (pacientes.existsByDni(dni)) {
            throw new ReglaDeNegocioException(CodigoError.DATO_DUPLICADO, "Ya existe un paciente con ese DNI");
        }
        Paciente paciente = new Paciente(comando.nombre().trim(), comando.apellido().trim(), dni,
                comando.telefono(), comando.email());
        return PacienteVista.de(pacientes.save(paciente));
    }
}
