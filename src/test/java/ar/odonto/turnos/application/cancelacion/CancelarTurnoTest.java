package ar.odonto.turnos.application.cancelacion;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Consultorio;
import ar.odonto.turnos.domain.model.Paciente;
import ar.odonto.turnos.domain.model.Prestacion;
import ar.odonto.turnos.domain.model.Profesional;
import ar.odonto.turnos.domain.model.Sillon;
import ar.odonto.turnos.domain.model.Turno;
import ar.odonto.turnos.domain.model.TurnoEstado;
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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tareas 2.1-2.3 de C-08: caso de uso "cancelar turno" (spec turnos, RN-CA-01/03/04).
 * Contexto de la spec: turno 09:30-10:00 de Ana, Sillón 1, Paciente Uno; anticipación mínima 6 h.
 * Cada test corre en una transacción que se revierte. Datos 100 % ficticios (regla D1).
 */
@SpringBootTest
@Transactional
@WithMockUser(username = "usuario", roles = "ADMIN")
class CancelarTurnoTest {

    static final LocalDateTime AHORA = LocalDateTime.parse("2026-11-23T08:00");

    @TestBean(name = "reloj")
    Clock reloj;

    static Clock reloj() {
        return new RelojAjustableDePrueba(AHORA);
    }

    @Autowired CancelarTurno cancelarTurno;
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
        Paciente paciente1 = pacientes.save(DatosDePrueba.paciente(1));
        control = prestaciones.save(DatosDePrueba.prestacion("Control", 30));
        turno0930 = turnos.save(DatosDePrueba.turno(ana, sillon1, paciente1, control,
                "2026-11-23T09:30", "2026-11-23T10:00"));
    }

    private void fijar(String fechaHoraLocal) {
        ((RelojAjustableDePrueba) reloj).fijar(LocalDateTime.parse(fechaHoraLocal));
    }

    private void rechaza(Long id, CodigoError esperado) {
        assertThatThrownBy(() -> cancelarTurno.ejecutar(id))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    private Turno recargar() {
        return turnos.findById(turno0930.getId()).orElseThrow();
    }

    // Tarea 2.1
    @Test
    void turnoInexistente_lanzaRecursoInexistente() {
        rechaza(999999L, CodigoError.RECURSO_INEXISTENTE);
    }

    // Tarea 2.2
    @Test
    void cancelacionFeliz_devuelveCanceladoConCanceladoEn() {
        fijar("2026-11-22T20:00");
        TurnoCanceladoVista vista = cancelarTurno.ejecutar(turno0930.getId());

        assertThat(vista.estado()).isEqualTo(TurnoEstado.CANCELADO);
        assertThat(vista.canceladoEn()).isEqualTo(LocalDateTime.parse("2026-11-22T20:00"));
        assertThat(vista.inicio()).isEqualTo(LocalDateTime.parse("2026-11-23T09:30"));
        assertThat(recargar().getEstado()).isEqualTo(TurnoEstado.CANCELADO);
    }

    // Tarea 2.2
    @Test
    void segundaCancelacion_lanzaTurnoYaCanceladoConservandoElOriginal() {
        fijar("2026-11-22T20:00");
        cancelarTurno.ejecutar(turno0930.getId());

        rechaza(turno0930.getId(), CodigoError.TURNO_YA_CANCELADO);
        assertThat(recargar().getCanceladoEn()).isEqualTo(LocalDateTime.parse("2026-11-22T20:00"));
    }

    // Tarea 2.3
    @Test
    void exactamenteSeisHorasAntes_bordeAceptado() {
        fijar("2026-11-23T03:30");
        TurnoCanceladoVista vista = cancelarTurno.ejecutar(turno0930.getId());
        assertThat(vista.estado()).isEqualTo(TurnoEstado.CANCELADO);
    }

    // Tarea 2.3
    @Test
    void cincoHorasCincuentaYNueve_bordeRechazadoDejaElTurnoReservado() {
        fijar("2026-11-23T03:31");
        rechaza(turno0930.getId(), CodigoError.CANCELACION_TARDIA);
        assertThat(recargar().getEstado()).isEqualTo(TurnoEstado.RESERVADO);
        assertThat(recargar().getCanceladoEn()).isNull();
    }

    // Orden D2: ya cancelado con inicio en el pasado → estado, no anticipación
    @Test
    void turnoYaCanceladoConInicioPasado_daTurnoYaCancelado() {
        fijar("2026-11-22T20:00");
        cancelarTurno.ejecutar(turno0930.getId());
        fijar("2026-11-23T10:00");

        rechaza(turno0930.getId(), CodigoError.TURNO_YA_CANCELADO);
    }
}
