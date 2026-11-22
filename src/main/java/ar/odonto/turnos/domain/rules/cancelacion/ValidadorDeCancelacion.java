package ar.odonto.turnos.domain.rules.cancelacion;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.TurnoEstado;

import java.time.LocalDateTime;

/**
 * Reglas de cancelación de turnos (knowledge-base/05, RN-CA), en Java puro (regla J1).
 * <p>
 * Lanza {@link ReglaDeNegocioException} con la PRIMERA regla violada, en el orden del Flujo 2 de la KB:
 * estado no RESERVADO (RN-CA-03, {@code TURNO_YA_CANCELADO}) → anticipación mínima (RN-CA-01,
 * {@code CANCELACION_TARDIA}). La inexistencia del turno se detecta antes, al cargar (design D2 de C-08).
 */
public final class ValidadorDeCancelacion {

    private ValidadorDeCancelacion() {
    }

    public static void validar(ContextoDeCancelacion c) {
        validarEstadoReservado(c);
        validarAnticipacionMinima(c);
    }

    // RN-CA-03: primero el estado, así un turno ya cancelado con inicio en el pasado no reporta CANCELACION_TARDIA
    private static void validarEstadoReservado(ContextoDeCancelacion c) {
        if (c.estado() != TurnoEstado.RESERVADO) {
            throw new ReglaDeNegocioException(CodigoError.TURNO_YA_CANCELADO);
        }
    }

    // RN-CA-01: cancelable si ahora <= inicio - anticipación mínima (el borde exacto se acepta)
    private static void validarAnticipacionMinima(ContextoDeCancelacion c) {
        LocalDateTime limite = c.inicio().minus(c.anticipacionMinima());
        if (c.ahora().isAfter(limite)) {
            throw new ReglaDeNegocioException(CodigoError.CANCELACION_TARDIA);
        }
    }
}
