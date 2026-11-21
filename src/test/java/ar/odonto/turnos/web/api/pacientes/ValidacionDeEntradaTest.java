package ar.odonto.turnos.web.api.pacientes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Entrada inválida en pacientes: 400 ENTRADA_INVALIDA con el campo (spec: pacientes). */
class ValidacionDeEntradaTest extends PacientesApiTestBase {

    @Test
    void nombreAusenteRechazado() throws Exception {
        postJson("/api/pacientes", """
                {"apellido":"Uno","dni":"00000001"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENTRADA_INVALIDA"))
                .andExpect(jsonPath("$.detail").value(containsString("nombre")))
                .andExpect(jsonPath("$.campos[0]").value(containsString("nombre")));
        assertThat(pacientes.count()).isZero();
    }

    @Test
    void telefonoConMasRechazado() throws Exception {
        codigo(postJson("/api/pacientes", """
                {"nombre":"Paciente","apellido":"Uno","dni":"00000001","telefono":"+5491100000001"}
                """), 400, "ENTRADA_INVALIDA");
        assertThat(pacientes.count()).isZero();
    }

    @Test
    void telefonoConEspaciosRechazado() throws Exception {
        codigo(postJson("/api/pacientes", """
                {"nombre":"Paciente","apellido":"Uno","dni":"00000001","telefono":"549 11 0000-0001"}
                """), 400, "ENTRADA_INVALIDA");
        assertThat(pacientes.count()).isZero();
    }
}
