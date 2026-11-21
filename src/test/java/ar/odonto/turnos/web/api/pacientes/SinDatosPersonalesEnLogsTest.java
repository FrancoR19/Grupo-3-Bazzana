package ar.odonto.turnos.web.api.pacientes;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN-DP-03: los logs de la capa de pacientes no incluyen DNI, teléfono ni nombre completo.
 * Captura todo lo que se loguea durante alta, edición y lectura y lo revisa campo por campo.
 */
class SinDatosPersonalesEnLogsTest extends PacientesApiTestBase {

    @Test
    void losLogsDeAltaEdicionYLecturaNoFiltranDatosPersonales() throws Exception {
        Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> capturador = new ListAppender<>();
        capturador.start();
        raiz.addAppender(capturador);
        try {
            // Canary: demuestra que el capturador sí está enganchado; sin él el test sería vacío.
            LoggerFactory.getLogger(SinDatosPersonalesEnLogsTest.class).info("canario-de-captura");
            postJson("/api/pacientes", DatosDePacientesDePrueba.altaCompleta()).andExpect(status().isCreated());
            Long id = pacientes.findByDni(DatosDePacientesDePrueba.DNI).orElseThrow().getId();
            ResultActions edicion = putJson("/api/pacientes/" + id, """
                    {"apellido":"Ejemplo","telefono":"5491100000009"}
                    """);
            edicion.andExpect(status().isOk());
            getJson("/api/pacientes").andExpect(status().isOk());
            getJson("/api/pacientes/" + id).andExpect(status().isOk());
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

        assertThat(mensajes).as("el capturador sí registró eventos (canario)")
                .anyMatch(mensaje -> mensaje.contains("canario-de-captura"));

        assertThat(mensajes).as("ningún log contiene datos personales (RN-DP-03)")
                .noneMatch(mensaje -> List.of(
                        DatosDePacientesDePrueba.DNI,
                        DatosDePacientesDePrueba.TELEFONO,
                        "5491100000009",
                        DatosDePacientesDePrueba.NOMBRE_COMPLETO,
                        DatosDePacientesDePrueba.NOMBRE + " Ejemplo")
                        .stream().anyMatch(mensaje::contains));
    }
}
