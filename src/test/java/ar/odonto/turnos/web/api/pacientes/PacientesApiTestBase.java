package ar.odonto.turnos.web.api.pacientes;

import ar.odonto.turnos.domain.port.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Base de tests de API de C-04: contexto real H2 + MockMvc, cada test revierte sus datos (RN-DP-01: solo ficticios). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class PacientesApiTestBase {

    @Autowired MockMvc mvc;
    @Autowired PacienteRepository pacientes;

    static void codigo(ResultActions respuesta, int estado, String esperado) throws Exception {
        respuesta.andExpect(status().is(estado)).andExpect(jsonPath("$.codigo").value(esperado));
    }

    ResultActions postJson(String ruta, String json) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions putJson(String ruta, String json) throws Exception {
        return mvc.perform(put(ruta).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions getJson(String ruta) throws Exception {
        return mvc.perform(get(ruta));
    }
}
