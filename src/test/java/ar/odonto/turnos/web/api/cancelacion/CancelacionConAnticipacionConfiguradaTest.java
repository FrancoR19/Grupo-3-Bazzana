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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Escenario "Tolerancia configurada distinta de 6 horas" de la spec turnos (C-08):
 * con {@code turnos.cancelacion-horas-minimas = 4}, cancelar 4 h y media antes pasa y 3 h y media antes no.
 * Contexto propio porque cambia una propiedad del contexto Spring.
 */
@SpringBootTest(properties = "turnos.cancelacion-horas-minimas=4")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "usuario", roles = "ADMIN")
class CancelacionConAnticipacionConfiguradaTest {

    @TestBean(name = "reloj")
    Clock reloj;

    static Clock reloj() {
        return new RelojAjustableDePrueba(LocalDateTime.parse("2026-11-23T08:00"));
    }

    @Autowired MockMvc mvc;
    @Autowired ConsultorioRepository consultorios;
    @Autowired HorarioAtencionRepository horarios;
    @Autowired ProfesionalRepository profesionales;
    @Autowired SillonRepository sillones;
    @Autowired PacienteRepository pacientes;
    @Autowired PrestacionRepository prestaciones;
    @Autowired TurnoRepository turnos;

    Turno turno0930;

    @BeforeEach
    void contexto() {
        ((RelojAjustableDePrueba) reloj).fijar(LocalDateTime.parse("2026-11-23T08:00"));
        Consultorio consultorio = consultorios.save(DatosDePrueba.consultorio());
        for (DayOfWeek dia : EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) {
            horarios.save(DatosDePrueba.horario(consultorio, dia));
        }
        Profesional ana = profesionales.save(DatosDePrueba.profesional("MP-0001"));
        Sillon sillon1 = sillones.save(DatosDePrueba.sillon("Sillón 1"));
        Paciente paciente1 = pacientes.save(DatosDePrueba.paciente(1));
        Prestacion control = prestaciones.save(DatosDePrueba.prestacion("Control", 30));
        turno0930 = turnos.save(DatosDePrueba.turno(ana, sillon1, paciente1, control,
                "2026-11-23T09:30", "2026-11-23T10:00"));
    }

    @Test
    void cuatroHorasYMediaAntes_conToleranciaDeCuatro_seCancela() throws Exception {
        ((RelojAjustableDePrueba) reloj).fijar(LocalDateTime.parse("2026-11-23T05:00"));
        mvc.perform(post("/api/turnos/" + turno0930.getId() + "/cancelacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }

    @Test
    void tresHorasYMediaAntes_conToleranciaDeCuatro_seRechaza() throws Exception {
        ((RelojAjustableDePrueba) reloj).fijar(LocalDateTime.parse("2026-11-23T06:00"));
        mvc.perform(post("/api/turnos/" + turno0930.getId() + "/cancelacion"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CANCELACION_TARDIA"));
    }
}
