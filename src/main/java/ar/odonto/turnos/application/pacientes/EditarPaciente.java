package ar.odonto.turnos.application.pacientes;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Paciente;
import ar.odonto.turnos.domain.port.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edición de pacientes: los campos nulos conservan el valor actual (edición parcial) y el DNI es
 * inmutable, igual que la matrícula en C-03: si se envía distinto se rechaza antes de consultar unicidad.
 */
@Service
public class EditarPaciente {

    public record Comando(Long id, String nombre, String apellido, String dni, String telefono, String email) {
    }

    private final PacienteRepository pacientes;

    public EditarPaciente(PacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    @Transactional
    public PacienteVista ejecutar(Comando comando) {
        Paciente paciente = pacientes.findById(comando.id())
                .orElseThrow(() -> new ReglaDeNegocioException(CodigoError.RECURSO_INEXISTENTE,
                        "No existe el paciente indicado"));
        if (comando.dni() != null && !comando.dni().trim().equals(paciente.getDni())) {
            throw new ReglaDeNegocioException(CodigoError.ENTRADA_INVALIDA, "El DNI es inmutable");
        }
        String nombre = comando.nombre() == null ? paciente.getNombre() : comando.nombre().trim();
        String apellido = comando.apellido() == null ? paciente.getApellido() : comando.apellido().trim();
        String telefono = comando.telefono() == null ? paciente.getTelefono() : comando.telefono();
        String email = comando.email() == null || comando.email().isBlank() ? paciente.getEmail() : comando.email();
        paciente.actualizar(nombre, apellido, telefono, email);
        return PacienteVista.de(pacientes.save(paciente));
    }
}
