package ar.odonto.turnos.web.api.pacientes;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lectura de un paciente por id (spec: pacientes). */
class ObtenerPacienteTest extends PacientesApiTestBase {

    @Test
    void pacienteExistente() throws Exception {
        postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta()).andExpect(status().isCreated());
        Long id = pacientes.findByDni(DatosDePacientesDePrueba.DNI).orElseThrow().getId();

        getJson("/api/pacientes/" + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.dni").value(DatosDePacientesDePrueba.DNI))
                .andExpect(jsonPath("$.telefono").value(DatosDePacientesDePrueba.TELEFONO));
    }

    @Test
    void pacienteInexistente() throws Exception {
        getJson("/api/pacientes/9999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_INEXISTENTE"));
    }
}
