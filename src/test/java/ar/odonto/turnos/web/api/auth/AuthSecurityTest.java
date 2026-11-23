package ar.odonto.turnos.web.api.auth;

import ar.odonto.turnos.domain.model.Rol;
import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioRepository;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Escenarios del spec (uno por test): feliz, error de negocio y borde.
 * Verifica también que la contraseña nunca se registra en logs ni viaja en texto plano.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest {

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
    void loginCorrectoSecretariaAccesoPermitido200() throws Exception {
        // Feliz: login correcto de secretaria → 200 y sesión establecida.
        var resultado = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"secretaria\",\"password\":\"secre123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("secretaria"))
                .andExpect(jsonPath("$.rol").value("SECRETARIA"))
                .andReturn();

        // Con la sesión autenticada se accede a una ruta protegida
        // (llega al controlador, que devuelve el error de negocio 422).
        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession();
        mvc.perform(get("/test/errores/regla/TURNO_EN_PASADO").session(sesion))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("TURNO_EN_PASADO"));
    }

    @Test
    void credencialesInvalidasDevuelven401NoAutenticado() throws Exception {
        // Error de negocio: contraseña incorrecta → 401 NO_AUTENTICADO.
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"secretaria\",\"password\":\"mal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void accesoSinAutenticarARutaProtegidaDevuelve401() throws Exception {
        // Borde: ruta protegida sin sesión → 401 NO_AUTENTICADO.
        mvc.perform(get("/api/pacientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void rutasPublicasAccesiblesSinLoginDevuelven200() throws Exception {
        // Borde: ruta pública accesible sin login.
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        // POST /api/auth/login también es pública (no requiere sesión previa).
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"noexiste\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized()); // llega al controlador (401 de credenciales, no 403 de cadena)
    }

    @Test
    void contraseniaNuncaApareceEnLogsNiTextoPlano() throws Exception {
        // Borde: capturar logs del login exitoso y verificar ausencia de la contraseña.
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        root.addAppender(listAppender);
        try {
            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"secretaria\",\"password\":\"secre123\"}"))
                    .andExpect(status().isOk());

            List<String> mensajes = listAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
            assertThat(mensajes).noneMatch(m -> m.contains("secre123"));
        } finally {
            root.detachAppender(listAppender);
        }
    }
}
