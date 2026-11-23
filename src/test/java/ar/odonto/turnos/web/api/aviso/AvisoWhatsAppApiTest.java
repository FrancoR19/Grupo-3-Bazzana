package ar.odonto.turnos.web.api.aviso;

import ar.odonto.turnos.domain.model.Consultorio;
import ar.odonto.turnos.domain.model.Paciente;
import ar.odonto.turnos.domain.model.Prestacion;
import ar.odonto.turnos.domain.model.Profesional;
import ar.odonto.turnos.domain.model.Sillon;
import ar.odonto.turnos.domain.model.Turno;
import ar.odonto.turnos.domain.port.ConsultorioRepository;
import ar.odonto.turnos.domain.port.HorarioAtencionRepository;
import ar.odonto.turnos.domain.port.PacienteRepository;
import ar.odonto.turnos.domain.port.PrestacionRepository;
import ar.odonto.turnos.domain.port.ProfesionalRepository;
import ar.odonto.turnos.domain.port.SillonRepository;
import ar.odonto.turnos.domain.port.TurnoRepository;
import ar.odonto.turnos.support.DatosDePrueba;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un test por escenario de la spec {@code aviso-whatsapp} de C-06 (RN-WA-01 a RN-WA-04, RN-DP-03).
 * Arma los turnos directamente con los repositorios de C-01 (independiente de C-02).
 * Datos 100 % ficticios (regla D1 / RN-DP-01).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "usuario", roles = "ADMIN")
class AvisoWhatsAppApiTest {

    @Autowired MockMvc mvc;
    @Autowired ConsultorioRepository consultorios;
    @Autowired HorarioAtencionRepository horarios;
    @Autowired ProfesionalRepository profesionales;
    @Autowired SillonRepository sillones;
    @Autowired PacienteRepository pacientes;
    @Autowired PrestacionRepository prestaciones;
    @Autowired TurnoRepository turnos;

    Profesional ana;
    Sillon sillon1;
    Prestacion control;
    Prestacion limpieza;

    @BeforeEach
    void contextoComun() {
        Consultorio consultorio = consultorios.save(DatosDePrueba.consultorio());
        for (DayOfWeek dia : EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) {
            horarios.save(DatosDePrueba.horario(consultorio, dia));
        }
        ana = profesionales.save(DatosDePrueba.profesional("MP-0001"));
        sillon1 = sillones.save(DatosDePrueba.sillon("Sillón 1"));
        control = prestaciones.save(DatosDePrueba.prestacion("Control", 30));
        limpieza = prestaciones.save(DatosDePrueba.prestacion("Limpieza", 45));
    }

    @Nested
    class GeneracionDeLinkValido {

        @Test
        void linkGeneradoParaUnTurnoConPacienteConTelefonoValido() throws Exception {
            Paciente paciente1 = pacientes.save(DatosDePrueba.paciente(1));
            Turno turno = turnos.save(DatosDePrueba.turno(ana, sillon1, paciente1, control,
                    "2026-11-24T10:30", "2026-11-24T11:00"));

            mvc.perform(get("/api/turnos/" + turno.getId() + "/whatsapp-link"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.turnoId").value(turno.getId()))
                    .andExpect(jsonPath("$.url", startsWith("https://wa.me/5491100000001?text=")))
                    .andExpect(jsonPath("$.mensaje", containsString("2026-11-24")))
                    .andExpect(jsonPath("$.mensaje", containsString("10:30")))
                    .andExpect(jsonPath("$.mensaje", containsString("Ana Ejemplo")))
                    .andExpect(jsonPath("$.mensaje", containsString("Control")));
        }

        @Test
        void mensajeIncluyeFechaHoraProfesionalYPrestacionSinDni() throws Exception {
            Paciente paciente1 = pacientes.save(DatosDePrueba.paciente(1));
            Turno turno = turnos.save(DatosDePrueba.turno(ana, sillon1, paciente1, limpieza,
                    "2026-11-24T14:00", "2026-11-24T14:45"));

            MvcResult resultado = mvc.perform(get("/api/turnos/" + turno.getId() + "/whatsapp-link"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje", containsString("2026-11-24")))
                    .andExpect(jsonPath("$.mensaje", containsString("14:00")))
                    .andExpect(jsonPath("$.mensaje", containsString("Ana Ejemplo")))
                    .andExpect(jsonPath("$.mensaje", containsString("Limpieza")))
                    .andExpect(jsonPath("$.mensaje", not(containsString("00000001"))))
                    .andReturn();

            String respuestaJson = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
            int inicioUrl = respuestaJson.indexOf("https://wa.me/");
            int finUrl = respuestaJson.indexOf('"', inicioUrl);
            String url = respuestaJson.substring(inicioUrl, finUrl);
            String textDecodificado = URLDecoder.decode(
                    url.substring("https://wa.me/5491100000001?text=".length()),
                    StandardCharsets.UTF_8);
            assertThat(textDecodificado)
                    .contains("2026-11-24", "14:00", "Ana Ejemplo", "Limpieza")
                    .doesNotContain("00000001");
        }
    }

    @Nested
    class CodificacionYNormalizacion {

        @Test
        void caracteresConTildesEspaciosYEnieCodificadosEnElLink() throws Exception {
            Profesional inaki = profesionales.save(
                    new Profesional("Iñaki", "Muñoz", "MP-0099", "5491100000199"));
            Prestacion extraccion = prestaciones.save(DatosDePrueba.prestacion("Extracción", 40));
            Paciente paciente2 = pacientes.save(DatosDePrueba.paciente(2));
            Turno turno = turnos.save(DatosDePrueba.turno(inaki, sillon1, paciente2, extraccion,
                    "2026-11-24T11:00", "2026-11-24T11:40"));

            MvcResult resultado = mvc.perform(get("/api/turnos/" + turno.getId() + "/whatsapp-link"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url", startsWith("https://wa.me/5491100000002?text=")))
                    .andReturn();

            String respuestaJson = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
            // Extraemos la URL del JSON para validar que sea un URI válido sin espacios ni caracteres no-ASCII crudos
            int inicioUrl = respuestaJson.indexOf("https://wa.me/");
            int finUrl = respuestaJson.indexOf('"', inicioUrl);
            String url = respuestaJson.substring(inicioUrl, finUrl);

            URI uri = URI.create(url);
            assertThat(uri.getRawQuery()).doesNotContain(" ");
            String textoDecodificado = URLDecoder.decode(
                    url.substring("https://wa.me/5491100000002?text=".length()),
                    StandardCharsets.UTF_8);
            assertThat(textoDecodificado).contains("Iñaki Muñoz", "Extracción");
        }

        @Test
        void telefonoConSignoMasYEspaciosSeNormalizaASoloDigitos() throws Exception {
            Paciente pacienteConMas = pacientes.save(
                    new Paciente("Paciente", "Tres", "00000003", "+54 9 11 0000-0003", null));
            Turno turno = turnos.save(DatosDePrueba.turno(ana, sillon1, pacienteConMas, control,
                    "2026-11-24T10:30", "2026-11-24T11:00"));

            mvc.perform(get("/api/turnos/" + turno.getId() + "/whatsapp-link"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url", startsWith("https://wa.me/5491100000003?text=")));
        }
    }

    @Nested
    class ErroresDeNegocio {

        @Test
        void pacienteSinTelefonoCargadoResponde422PacienteSinTelefono() throws Exception {
            Paciente sinTel = pacientes.save(new Paciente("Paciente", "Cuatro", "00000004", null, null));
            Turno turno = turnos.save(DatosDePrueba.turno(ana, sillon1, sinTel, control,
                    "2026-11-24T10:30", "2026-11-24T11:00"));

            mvc.perform(get("/api/turnos/" + turno.getId() + "/whatsapp-link"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.codigo").value("PACIENTE_SIN_TELEFONO"));
        }

        @Test
        void pacienteConTelefonoEnBlancoResponde422PacienteSinTelefono() throws Exception {
            Paciente telBlanco = pacientes.save(new Paciente("Paciente", "Cinco", "00000005", "   ", null));
            Turno turno = turnos.save(DatosDePrueba.turno(ana, sillon1, telBlanco, control,
                    "2026-11-24T10:30", "2026-11-24T11:00"));

            mvc.perform(get("/api/turnos/" + turno.getId() + "/whatsapp-link"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.codigo").value("PACIENTE_SIN_TELEFONO"));
        }

        @Test
        void turnoInexistenteResponde404RecursoInexistente() throws Exception {
            mvc.perform(get("/api/turnos/99999/whatsapp-link"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.codigo").value("RECURSO_INEXISTENTE"));
        }
    }

    @Nested
    class PrivacidadEnLogs {

        @Test
        void generacionDeLinkSinFugaDeDatosPersonalesEnLogs() throws Exception {
            Paciente paciente = pacientes.save(
                    new Paciente("Paciente", "Uno", "00000001", "5491100000001", null));
            Turno turno = turnos.save(DatosDePrueba.turno(ana, sillon1, paciente, control,
                    "2026-11-24T10:30", "2026-11-24T11:00"));

            Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            ListAppender<ILoggingEvent> capturador = new ListAppender<>();
            capturador.start();
            raiz.addAppender(capturador);
            try {
                LoggerFactory.getLogger(AvisoWhatsAppApiTest.class).info("canario-de-captura-whatsapp");
                mvc.perform(get("/api/turnos/" + turno.getId() + "/whatsapp-link"))
                        .andExpect(status().isOk());
            } finally {
                raiz.detachAppender(capturador);
                capturador.stop();
            }

            List<String> mensajes = capturador.list.stream()
                    .flatMap(evento -> Stream.concat(
                            Stream.of(evento.getFormattedMessage(), evento.getThreadName()),
                            Arrays.stream(evento.getArgumentArray() == null ? new Object[0] : evento.getArgumentArray())
                                    .map(String::valueOf)))
                    .toList();

            assertThat(mensajes).as("el capturador registró eventos (canario)")
                    .anyMatch(m -> m.contains("canario-de-captura-whatsapp"));

            assertThat(mensajes).as("ningún log contiene DNI, teléfono ni nombre completo del paciente (RN-DP-03)")
                    .noneMatch(m -> List.of("00000001", "5491100000001", "Paciente Uno")
                            .stream().anyMatch(m::contains));
        }
    }
}
