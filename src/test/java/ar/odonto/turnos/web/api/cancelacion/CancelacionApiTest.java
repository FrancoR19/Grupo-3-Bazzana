package ar.odonto.turnos.web.api.cancelacion;

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
import ar.odonto.turnos.support.RelojAjustableDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un test por escenario de la spec "turnos" de C-08 (requisitos de cancelación, RN-CA-01..04).
 * Contexto común: L-V 08:00-20:00, turno 09:30-10:00 de Ana, Sillón 1, Paciente Uno; anticipación mínima 6 h.
 * Reloj movible por test (nunca LocalDateTime.now(), regla D4). Datos 100 % ficticios (regla D1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "usuario", roles = "ADMIN")
class CancelacionApiTest {

    static final LocalDateTime AHORA = LocalDateTime.parse("2026-11-23T08:00");

    @TestBean(name = "reloj")
    Clock reloj;

    static Clock reloj() {
        return new RelojAjustableDePrueba(AHORA);
    }

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
    Paciente paciente1;
    Paciente paciente2;
    Prestacion control;
    Turno turno0930;

    @BeforeEach
    void contextoComun() {
        ((RelojAjustableDePrueba) reloj).fijar(AHORA);
        Consultorio consultorio = consultorios.save(DatosDePrueba.consultorio());
        for (DayOfWeek dia : EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) {
            horarios.save(DatosDePrueba.horario(consultorio, dia));
        }
        ana = profesionales.save(DatosDePrueba.profesional("MP-0001"));
        sillon1 = sillones.save(DatosDePrueba.sillon("Sillón 1"));
        paciente1 = pacientes.save(DatosDePrueba.paciente(1));
        paciente2 = pacientes.save(DatosDePrueba.paciente(2));
        control = prestaciones.save(DatosDePrueba.prestacion("Control", 30));
        turno0930 = turnos.save(DatosDePrueba.turno(ana, sillon1, paciente1, control,
                "2026-11-23T09:30", "2026-11-23T10:00"));
    }

    private void fijar(String fechaHoraLocal) {
        ((RelojAjustableDePrueba) reloj).fijar(LocalDateTime.parse(fechaHoraLocal));
    }

    ResultActions cancelar(Long id) throws Exception {
        return mvc.perform(post("/api/turnos/" + id + "/cancelacion"));
    }

    ResultActions crear(Long pacienteId, String inicio) throws Exception {
        String json = """
                {"pacienteId": %d, "profesionalId": %d, "sillonId": %d, "prestacionId": %d,
                 "inicio": "%s"}""".formatted(pacienteId, ana.getId(), sillon1.getId(),
                control.getId(), inicio);
        return mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    static void codigo(ResultActions r, int estado, String codigo) throws Exception {
        r.andExpect(status().is(estado)).andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Nested
    class CancelacionConAnticipacionSuficiente {

        @Test
        void cancelacionFeliz_masDeSeisHoras() throws Exception {
            fijar("2026-11-22T20:00");
            cancelar(turno0930.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADO"))
                    .andExpect(jsonPath("$.canceladoEn").value("2026-11-22T20:00:00"))
                    // Tarea 3.3: nunca DNI ni teléfono (reglas D1/D2)
                    .andExpect(jsonPath("$.dni").doesNotExist())
                    .andExpect(jsonPath("$.telefono").doesNotExist());
        }

        @Test
        void exactamenteSeisHorasAntes_bordeAceptado() throws Exception {
            fijar("2026-11-23T03:30");
            cancelar(turno0930.getId())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADO"));
        }
    }

    @Nested
    class CancelacionTardia {

        @Test
        void cincoHorasCincuentaYNueve_bordeRechazado() throws Exception {
            fijar("2026-11-23T03:31");
            codigo(cancelar(turno0930.getId()), 422, "CANCELACION_TARDIA");
            mvc.perform(get("/api/turnos/" + turno0930.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("RESERVADO"));
        }

        @Test
        void despuesDelInicioDelTurno() throws Exception {
            fijar("2026-11-23T10:00");
            codigo(cancelar(turno0930.getId()), 422, "CANCELACION_TARDIA");
        }
    }

    @Nested
    class TurnoYaCancelado {

        @Test
        void segundaCancelacion() throws Exception {
            fijar("2026-11-22T20:00");
            cancelar(turno0930.getId()).andExpect(status().isOk());

            codigo(cancelar(turno0930.getId()), 422, "TURNO_YA_CANCELADO");
            mvc.perform(get("/api/turnos/" + turno0930.getId()))
                    .andExpect(jsonPath("$.estado").value("CANCELADO"));
        }

        @Test
        void turnoYaCanceladoConInicioYaPasado() throws Exception {
            fijar("2026-11-22T20:00");
            cancelar(turno0930.getId()).andExpect(status().isOk());
            fijar("2026-11-23T10:00");

            codigo(cancelar(turno0930.getId()), 422, "TURNO_YA_CANCELADO");
        }
    }

    @Nested
    class TurnoInexistente {

        @Test
        void identificadorInexistente() throws Exception {
            codigo(cancelar(999999L), 404, "RECURSO_INEXISTENTE");
        }
    }

    @Nested
    class LaCancelacionLiberaElHorario {

        @Test
        void vuelvaACrearElMismoHuecoTrasCancelar() throws Exception {
            fijar("2026-11-22T20:00");
            cancelar(turno0930.getId()).andExpect(status().isOk());

            crear(paciente2.getId(), "2026-11-23T09:30").andExpect(status().isCreated());
        }

        @Test
        void detalleDeUnTurnoCancelado() throws Exception {
            fijar("2026-11-22T20:00");
            cancelar(turno0930.getId()).andExpect(status().isOk());

            mvc.perform(get("/api/turnos/" + turno0930.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("CANCELADO"))
                    .andExpect(jsonPath("$.inicio").value("2026-11-23T09:30:00"))
                    .andExpect(jsonPath("$.fin").value("2026-11-23T10:00:00"))
                    .andExpect(jsonPath("$.pacienteId").value(paciente1.getId()))
                    .andExpect(jsonPath("$.profesionalId").value(ana.getId()))
                    .andExpect(jsonPath("$.sillonId").value(sillon1.getId()))
                    .andExpect(jsonPath("$.prestacionId").value(control.getId()))
                    .andExpect(jsonPath("$.dni").doesNotExist())
                    .andExpect(jsonPath("$.telefono").doesNotExist());
        }
    }
}
