package ar.odonto.turnos.web.api.pacientes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Escenarios de alta de paciente (spec: pacientes, US-050). */
class AltaPacienteTest extends PacientesApiTestBase {

    @Test
    void altaDePacienteCompleto() throws Exception {
        postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/pacientes/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value(DatosDePacientesDePrueba.NOMBRE))
                .andExpect(jsonPath("$.apellido").value(DatosDePacientesDePrueba.APELLIDO))
                .andExpect(jsonPath("$.dni").value(DatosDePacientesDePrueba.DNI))
                .andExpect(jsonPath("$.telefono").value(DatosDePacientesDePrueba.TELEFONO))
                .andExpect(jsonPath("$.email").value(DatosDePacientesDePrueba.EMAIL));
        assertThat(pacientes.count()).isEqualTo(1);
    }

    @Test
    void altaSinTelefono() throws Exception {
        postJson("/api/pacientes", """
                {"nombre":"Paciente","apellido":"Dos","dni":"00000002"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.telefono").value(nullValue()))
                .andExpect(jsonPath("$.email").value(nullValue()));
        assertThat(pacientes.count()).isEqualTo(1);
    }

    @Test
    void dniDuplicadoRechazado() throws Exception {
        postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta()).andExpect(status().isCreated());

        codigo(postJson("/api/pacientes", """
                {"nombre":"Otro","apellido":"Paciente","dni":"00000001"}
                """), 409, "DATO_DUPLICADO");

        assertThat(pacientes.count()).isEqualTo(1);
        assertThat(pacientes.findByDni("00000001")).hasValueSatisfying(paciente ->
                assertThat(paciente.getNombre()).isEqualTo(DatosDePacientesDePrueba.NOMBRE));
    }
}
