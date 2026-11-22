package ar.odonto.turnos.application.cancelacion;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Consultorio;
import ar.odonto.turnos.domain.model.Paciente;
import ar.odonto.turnos.domain.model.Prestacion;
import ar.odonto.turnos.domain.model.Profesional;
import ar.odonto.turnos.domain.model.Sillon;
import ar.odonto.turnos.domain.port.ConsultorioRepository;
import ar.odonto.turnos.domain.port.HorarioAtencionRepository;
import ar.odonto.turnos.domain.port.PacienteRepository;
import ar.odonto.turnos.domain.port.PrestacionRepository;
import ar.odonto.turnos.domain.port.ProfesionalRepository;
import ar.odonto.turnos.domain.port.SillonRepository;
import ar.odonto.turnos.domain.port.TurnoRepository;
import ar.odonto.turnos.application.turnos.CrearTurno;
import ar.odonto.turnos.application.turnos.TurnoVista;
import ar.odonto.turnos.support.DatosDePrueba;
import ar.odonto.turnos.support.RelojAjustableDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tarea 4.1 de C-08: prueba de punta a punta de RN-CA-02 (la cancelación libera el horario).
 * Crea con el caso de uso de C-02, verifica que el hueco está tomado, cancela y vuelve a crear en el
 * mismo horario, sillón y profesional. Datos 100 % ficticios (regla D1).
 */
@SpringBootTest
@Transactional
class LiberarHorarioTest {

    @TestBean(name = "reloj")
    Clock reloj;

    static Clock reloj() {
        return new RelojAjustableDePrueba(LocalDateTime.parse("2026-11-23T02:00"));
    }

    @Autowired CrearTurno crearTurno;
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
    Paciente paciente1;
    Paciente paciente2;
    Prestacion control;

    @BeforeEach
    void contexto() {
        ((RelojAjustableDePrueba) reloj).fijar(LocalDateTime.parse("2026-11-23T02:00"));
        Consultorio consultorio = consultorios.save(DatosDePrueba.consultorio());
        for (DayOfWeek dia : EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)) {
            horarios.save(DatosDePrueba.horario(consultorio, dia));
        }
        ana = profesionales.save(DatosDePrueba.profesional("MP-0001"));
        sillon1 = sillones.save(DatosDePrueba.sillon("Sillón 1"));
        paciente1 = pacientes.save(DatosDePrueba.paciente(1));
        paciente2 = pacientes.save(DatosDePrueba.paciente(2));
        control = prestaciones.save(DatosDePrueba.prestacion("Control", 30));
    }

    private TurnoVista crearTurno0930(Paciente paciente) {
        return crearTurno.ejecutar(new CrearTurno.Comando(paciente.getId(), ana.getId(), sillon1.getId(),
                control.getId(), LocalDateTime.parse("2026-11-23T09:30"), false));
    }

    @Test
    void cancelarLiberaElHuecoParaUnTurnoNuevo() {
        TurnoVista primero = crearTurno0930(paciente1);

        // Sin cancelar: el hueco está tomado (esto es lo que arregla RN-CA-02)
        assertThatThrownBy(() -> crearTurno0930(paciente2))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        e -> assertThat(e.codigo()).isEqualTo(CodigoError.CHOQUE_SILLON));

        cancelarTurno.ejecutar(primero.id());

        TurnoVista segundo = crearTurno0930(paciente2);
        assertThat(segundo.id()).isNotEqualTo(primero.id());
        assertThat(segundo.estado().name()).isEqualTo("RESERVADO");
        assertThat(turnos.count()).isEqualTo(2);
    }
}
