package ar.odonto.turnos.web.api.pacientes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Escenarios de edición de paciente (spec: pacientes). */
class EditarPacienteTest extends PacientesApiTestBase {

    private Long altaFicticia() throws Exception {
        postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta()).andExpect(status().isCreated());
        return pacientes.findByDni(DatosDePacientesDePrueba.DNI).orElseThrow().getId();
    }

    @Test
    void editarApellidoYTelefonoConservandoElDni() throws Exception {
        Long id = altaFicticia();

        putJson("/api/pacientes/" + id, """
                {"apellido":"Ejemplo","telefono":"5491100000009"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.apellido").value("Ejemplo"))
                .andExpect(jsonPath("$.telefono").value("5491100000009"))
                .andExpect(jsonPath("$.dni").value(DatosDePacientesDePrueba.DNI))
                .andExpect(jsonPath("$.nombre").value(DatosDePacientesDePrueba.NOMBRE));

        assertThat(pacientes.findById(id)).hasValueSatisfying(paciente ->
                assertThat(paciente.getDni()).isEqualTo(DatosDePacientesDePrueba.DNI));
    }

    @Test
    void intentoDeCambiarElDniRechazado() throws Exception {
        Long id = altaFicticia();

        codigo(putJson("/api/pacientes/" + id, """
                {"dni":"99999999"}
                """), 400, "ENTRADA_INVALIDA");

        assertThat(pacientes.findById(id)).hasValueSatisfying(paciente ->
                assertThat(paciente.getDni()).isEqualTo(DatosDePacientesDePrueba.DNI));
    }

    @Test
    void edicionDePacienteInexistente() throws Exception {
        codigo(putJson("/api/pacientes/9999", """
                {"nombre":"Paciente"}
                """), 404, "RECURSO_INEXISTENTE");
    }

    @Test
    void dniEnBlancoRechazado() throws Exception {
        Long id = altaFicticia();

        codigo(putJson("/api/pacientes/" + id, """
                {"dni":"   "}
                """), 400, "ENTRADA_INVALIDA");

        assertThat(pacientes.findById(id)).hasValueSatisfying(paciente -> {
            assertThat(paciente.getDni()).isEqualTo(DatosDePacientesDePrueba.DNI);
            assertThat(paciente.getNombre()).isEqualTo(DatosDePacientesDePrueba.NOMBRE);
        });
    }
}
