package ar.odonto.turnos.web.api.cancelacion;

import ar.odonto.turnos.application.cancelacion.TurnoCanceladoVista;
import ar.odonto.turnos.domain.model.TurnoEstado;

import java.time.LocalDateTime;

/**
 * Respuesta de la cancelación de turno (C-08): los campos del turno más {@code canceladoEn} (RN-CA-02).
 * Sin DNI ni teléfono del paciente (reglas D1/D2 de AGENTS.md).
 */
public record TurnoCanceladoResponse(Long id, Long pacienteId, Long profesionalId, Long sillonId, Long prestacionId,
        LocalDateTime inicio, LocalDateTime fin, boolean sobreturno, TurnoEstado estado, LocalDateTime canceladoEn,
        String creadoPor, LocalDateTime creadoEn) {

    static TurnoCanceladoResponse de(TurnoCanceladoVista v) {
        return new TurnoCanceladoResponse(v.id(), v.pacienteId(), v.profesionalId(), v.sillonId(), v.prestacionId(),
                v.inicio(), v.fin(), v.sobreturno(), v.estado(), v.canceladoEn(), v.creadoPor(), v.creadoEn());
    }
}
