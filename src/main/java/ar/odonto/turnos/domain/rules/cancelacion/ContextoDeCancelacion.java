package ar.odonto.turnos.domain.rules.cancelacion;

import ar.odonto.turnos.domain.model.TurnoEstado;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Todo lo que el {@link ValidadorDeCancelacion} necesita para decidir, ya cargado por el caso de uso
 * (design D1 de C-08). Es Java puro: se puede armar en un test sin Spring ni base de datos.
 *
 * @param turnoId              identificador del turno a cancelar (para mensajes, nunca datos de pacientes)
 * @param estado               estado actual del turno (RN-CA-03)
 * @param inicio               inicio del turno (RN-CA-01)
 * @param ahora                instante actual del reloj del sistema (zona del consultorio, RN-GL-02)
 * @param anticipacionMinima   anticipación mínima para cancelar, de {@code turnos.cancelacion-horas-minimas}
 */
public record ContextoDeCancelacion(Long turnoId, TurnoEstado estado, LocalDateTime inicio,
        LocalDateTime ahora, Duration anticipacionMinima) {

    public ContextoDeCancelacion {
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(ahora, "ahora");
        Objects.requireNonNull(anticipacionMinima, "anticipacionMinima");
    }
}
