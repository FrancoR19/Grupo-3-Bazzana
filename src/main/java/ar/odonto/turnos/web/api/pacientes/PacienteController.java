package ar.odonto.turnos.web.api.pacientes;

import ar.odonto.turnos.application.pacientes.AltaPaciente;
import ar.odonto.turnos.application.pacientes.EditarPaciente;
import ar.odonto.turnos.application.pacientes.ListarPacientes;
import ar.odonto.turnos.application.pacientes.ObtenerPaciente;
import ar.odonto.turnos.application.pacientes.PacienteVista;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** API de pacientes de C-04: solo traduce DTOs; las reglas viven en los casos de uso. */
@RestController
@RequestMapping("/api/pacientes")
class PacienteController {

    private final AltaPaciente altaPaciente;
    private final EditarPaciente editarPaciente;
    private final ListarPacientes listarPacientes;
    private final ObtenerPaciente obtenerPaciente;

    PacienteController(AltaPaciente altaPaciente, EditarPaciente editarPaciente, ListarPacientes listarPacientes,
                       ObtenerPaciente obtenerPaciente) {
        this.altaPaciente = altaPaciente;
        this.editarPaciente = editarPaciente;
        this.listarPacientes = listarPacientes;
        this.obtenerPaciente = obtenerPaciente;
    }

    @GetMapping
    List<PacienteResponse> listar(@RequestParam(required = false) String q) {
        return listarPacientes.ejecutar(q).stream().map(PacienteResponse::de).toList();
    }

    @GetMapping("/{id}")
    PacienteResponse obtener(@PathVariable Long id) {
        return PacienteResponse.de(obtenerPaciente.ejecutar(id));
    }

    @PostMapping
    ResponseEntity<PacienteResponse> crear(@Valid @RequestBody CrearPacienteRequest request) {
        PacienteVista creada = altaPaciente.ejecutar(new AltaPaciente.Comando(request.nombre(), request.apellido(),
                request.dni(), request.telefono(), request.email()));
        return ResponseEntity.created(URI.create("/api/pacientes/" + creada.id()))
                .body(PacienteResponse.de(creada));
    }

    @PutMapping("/{id}")
    PacienteResponse editar(@PathVariable Long id, @Valid @RequestBody EditarPacienteRequest request) {
        return PacienteResponse.de(editarPaciente.ejecutar(new EditarPaciente.Comando(id, request.nombre(),
                request.apellido(), request.dni(), request.telefono(), request.email())));
    }
}
