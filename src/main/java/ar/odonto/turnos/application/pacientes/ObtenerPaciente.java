package ar.odonto.turnos.application.pacientes;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.port.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectura de un paciente por id; inexistente → 404 RECURSO_INEXISTENTE. */
@Service
public class ObtenerPaciente {

    private final PacienteRepository pacientes;

    public ObtenerPaciente(PacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    @Transactional(readOnly = true)
    public PacienteVista ejecutar(Long id) {
        return pacientes.findById(id)
                .map(PacienteVista::de)
                .orElseThrow(() -> new ReglaDeNegocioException(CodigoError.RECURSO_INEXISTENTE,
                        "No existe el paciente indicado"));
    }
}
