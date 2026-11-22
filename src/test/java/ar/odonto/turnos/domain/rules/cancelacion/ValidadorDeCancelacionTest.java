package ar.odonto.turnos.domain.rules.cancelacion;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.TurnoEstado;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tareas 1.1/1.2 de C-08: cada regla RN-CA en Java puro, sin Spring ni base de datos.
 * Contexto de la spec: turno 09:30-10:00, anticipación mínima 6 h (RN-CA-01 y RN-CA-03).
 */
class ValidadorDeCancelacionTest {

    private static final Duration SEIS_HORAS = Duration.ofHours(6);
    private static final LocalDateTime INICIO = LocalDateTime.parse("2026-11-23T09:30");

    /** Builder mínimo: por defecto, cancelación válida (13,5 h de anticipación). */
    private static ContextoDeCancelacion ctx(LocalDateTime ahora, TurnoEstado estado) {
        return new ContextoDeCancelacion(1L, estado, INICIO, ahora, SEIS_HORAS);
    }

    private static void rechaza(ContextoDeCancelacion c, CodigoError esperado) {
        assertThatThrownBy(() -> ValidadorDeCancelacion.validar(c))
                .isInstanceOfSatisfying(ReglaDeNegocioException.class,
                        e -> assertThat(e.codigo()).isEqualTo(esperado));
    }

    @Test
    void masDeSeisHoras_acepta() {
        assertThatCode(() -> ValidadorDeCancelacion.validar(
                ctx(LocalDateTime.parse("2026-11-22T20:00"), TurnoEstado.RESERVADO)))
                .doesNotThrowAnyException();
    }

    @Test
    void exactamenteSeisHoras_bordeAceptado() {
        assertThatCode(() -> ValidadorDeCancelacion.validar(
                ctx(LocalDateTime.parse("2026-11-23T03:30"), TurnoEstado.RESERVADO)))
                .doesNotThrowAnyException();
    }

    @Test
    void cincoHorasCincuentaYNueveMinutos_bordeRechazado() {
        rechaza(ctx(LocalDateTime.parse("2026-11-23T03:31"), TurnoEstado.RESERVADO),
                CodigoError.CANCELACION_TARDIA);
    }

    @Test
    void inicioYaPasado_rechazaCancelacionTardia() {
        rechaza(ctx(LocalDateTime.parse("2026-11-23T10:00"), TurnoEstado.RESERVADO),
                CodigoError.CANCELACION_TARDIA);
    }

    @Test
    void turnoYaCanceladoConInicioPasado_daEstadoNoAnticipacion() {
        // Orden D2: el estado (RN-CA-03) se chequea ANTES que la anticipación (RN-CA-01)
        rechaza(ctx(LocalDateTime.parse("2026-11-23T10:00"), TurnoEstado.CANCELADO),
                CodigoError.TURNO_YA_CANCELADO);
    }

    @Test
    void turnoYaCanceladoConAnticipacionSuficiente_daEstado() {
        rechaza(ctx(LocalDateTime.parse("2026-11-22T20:00"), TurnoEstado.CANCELADO),
                CodigoError.TURNO_YA_CANCELADO);
    }
}
