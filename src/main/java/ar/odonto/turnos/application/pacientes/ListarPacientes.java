package ar.odonto.turnos.application.pacientes;

import ar.odonto.turnos.domain.model.Paciente;
import ar.odonto.turnos.domain.port.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Listado y búsqueda de pacientes. El filtro {@code q} (nombre, apellido o DNI, insensible a mayúsculas)
 * se resuelve acá sobre {@code findAll()}: no se agrega ninguna query al puerto compartido de C-01
 * (anti-colisión 4). Orden estable por apellido y nombre.
 */
@Service
public class ListarPacientes {

    private final PacienteRepository pacientes;

    public ListarPacientes(PacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    @Transactional(readOnly = true)
    public List<PacienteVista> ejecutar(String q) {
        String termino = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return pacientes.findAll().stream()
                .filter(paciente -> termino.isEmpty() || coincide(paciente, termino))
                .sorted(Comparator.comparing(Paciente::getApellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Paciente::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(PacienteVista::de)
                .toList();
    }

    private static boolean coincide(Paciente paciente, String termino) {
        return paciente.getNombre().toLowerCase(Locale.ROOT).contains(termino)
                || paciente.getApellido().toLowerCase(Locale.ROOT).contains(termino)
                || paciente.getDni().toLowerCase(Locale.ROOT).contains(termino);
    }
}
