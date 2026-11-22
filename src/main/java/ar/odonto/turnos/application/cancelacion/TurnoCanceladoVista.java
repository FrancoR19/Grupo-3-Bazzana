package ar.odonto.turnos.application.cancelacion;

import ar.odonto.turnos.domain.model.Turno;
import ar.odonto.turnos.domain.model.TurnoEstado;

import java.time.LocalDateTime;

/**
 * Datos de una cancelación para afuera del caso de uso: los campos de {@code TurnoVista} de C-02 más
 * {@code canceladoEn} (RN-CA-02/04). Se arma DENTRO de la transacción (open-in-view desactivado)
 * y no incluye DNI ni teléfono del paciente (reglas D1/D2 de AGENTS.md).
 */
public record TurnoCanceladoVista(Long id, Long pacienteId, Long profesionalId, Long sillonId, Long prestacionId,
        LocalDateTime inicio, LocalDateTime fin, boolean sobreturno, TurnoEstado estado, LocalDateTime canceladoEn,
        String creadoPor, LocalDateTime creadoEn) {

    static TurnoCanceladoVista de(Turno t) {
        return new TurnoCanceladoVista(t.getId(), t.getPaciente().getId(), t.getProfesional().getId(),
                t.getSillon().getId(), t.getPrestacion().getId(), t.getInicio(), t.getFin(), t.isSobreturno(),
                t.getEstado(), t.getCanceladoEn(), t.getCreadoPor(), t.getCreadoEn());
    }
}
