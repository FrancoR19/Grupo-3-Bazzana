package ar.odonto.turnos.web.api.pacientes;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Listado y búsqueda de pacientes (spec: pacientes). */
class ListarPacientesTest extends PacientesApiTestBase {

    @Test
    void listadoCompletoOrdenadoPorApellido() throws Exception {
        postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta()).andExpect(status().isCreated());
        postJson("/api/pacientes", """
                {"nombre":"Paciente","apellido":"Dos","dni":"00000002"}
                """).andExpect(status().isCreated());

        getJson("/api/pacientes")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].apellido").value("Dos"))
                .andExpect(jsonPath("$[1].apellido").value(DatosDePacientesDePrueba.APELLIDO))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].dni").value("00000002"));
    }

    @Test
    void busquedaPorDniParcial() throws Exception {
        postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta()).andExpect(status().isCreated());

        getJson("/api/pacientes?q=0000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dni").value(DatosDePacientesDePrueba.DNI));
    }

    @Test
    void busquedaSinCoincidencias() throws Exception {
        postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta()).andExpect(status().isCreated());

        getJson("/api/pacientes?q=zzzz")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listadoSobreBaseVacia() throws Exception {
        getJson("/api/pacientes")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
