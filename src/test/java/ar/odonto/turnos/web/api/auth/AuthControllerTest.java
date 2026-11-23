package ar.odonto.turnos.web.api.auth;

import ar.odonto.turnos.domain.model.Rol;
import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/auth/login}: feliz (200 + sesión), credenciales inválidas (401),
 * usuario inexistente (401). Endpoint público (no requiere sesión previa).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void semilla() {
        if (!usuarios.existsByUsername("secretaria")) {
            usuarios.save(new Usuario("secretaria", passwordEncoder.encode("secre123"), Rol.SECRETARIA, null));
        }
    }

    @Test
    void loginCorrectoSecretariaDevuelve200YEstableceSesion() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"secretaria\",\"password\":\"secre123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("secretaria"))
                .andExpect(jsonPath("$.rol").value("SECRETARIA"))
                .andExpect(jsonPath("$.profesionalId").isEmpty())
                .andReturn();

        // La sesión de Spring Security autenticada permite acceder a una ruta protegida
        // (llega al controlador, que devuelve el error de negocio 422).
        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession();
        mvc.perform(get("/test/errores/regla/TURNO_EN_PASADO").session(sesion))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("TURNO_EN_PASADO"));
    }

    @Test
    void credencialesInvalidasDevuelven401() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"secretaria\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void usuarioInexistenteDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"noexiste\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void loginCorrectoRenuevaIdDeSesion() throws Exception {
        var sesionPrevia = new MockHttpSession();
        String idPrevio = sesionPrevia.getId();

        MvcResult resultado = mvc.perform(post("/api/auth/login")
                .session(sesionPrevia)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"secretaria\",\"password\":\"secre123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        var sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(sesion).isNotNull();
        assertThat(sesion.getId()).isNotEqualTo(idPrevio);
    }

    @Test
    void loginFallidoNoRenuevaIdDeSesion() throws Exception {
        var sesionPrevia = new MockHttpSession();
        String idPrevio = sesionPrevia.getId();

        mvc.perform(post("/api/auth/login")
                .session(sesionPrevia)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"secretaria\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(sesionPrevia.getId()).isEqualTo(idPrevio);
    }

    @Test
    void loginEsPublicoSinSesionPrevia() throws Exception {
        // Llega al controlador: no lo frena la cadena como 403 (es permitAll).
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"noexiste\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
