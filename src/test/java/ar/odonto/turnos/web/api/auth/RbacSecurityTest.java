package ar.odonto.turnos.web.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz RBAC por ruta aplicada en la cadena de seguridad de C-05.
 * {@code @WithMockUser} simula un rol autenticado sin necesidad de base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RbacSecurityTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPuedeLeerYCrearProfesionales() throws Exception {
        mvc.perform(get("/api/profesionales"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/profesionales")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Ejemplo\",\"matricula\":\"MP-0001\",\"telefono\":\"5491100000101\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "secretaria", roles = "SECRETARIA")
    void secretariaPuedeLeerPeroNoCrearProfesionales() throws Exception {
        mvc.perform(get("/api/profesionales")).andExpect(status().isOk());
        mvc.perform(post("/api/profesionales")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Ejemplo\",\"matricula\":\"MP-0001\",\"telefono\":\"5491100000101\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @WithMockUser(username = "ana", roles = "ODONTOLOGO")
    void odontologoPuedeLeerPeroNoCrearProfesionales() throws Exception {
        mvc.perform(get("/api/profesionales")).andExpect(status().isOk());
        mvc.perform(post("/api/profesionales")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Ejemplo\",\"matricula\":\"MP-0001\",\"telefono\":\"5491100000101\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @WithMockUser(username = "ana", roles = "ODONTOLOGO")
    void odontologoEnRutaDeAdminDevuelve403() throws Exception {
        mvc.perform(post("/api/profesionales")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana\",\"apellido\":\"Ejemplo\",\"matricula\":\"MP-0001\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @WithMockUser(username = "ana", roles = "ODONTOLOGO")
    void odontologoNoPuedeObtenerLinkDeWhatsApp() throws Exception {
        mvc.perform(get("/api/turnos/9999/whatsapp-link"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    @WithMockUser(username = "secretaria", roles = "SECRETARIA")
    void secretariaPuedeObtenerLinkDeWhatsApp() throws Exception {
        // Turno inexistente => 404 RECURSO_INEXISTENTE: pasó la seguridad (no 403/401).
        mvc.perform(get("/api/turnos/9999/whatsapp-link"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_INEXISTENTE"));
    }
}
