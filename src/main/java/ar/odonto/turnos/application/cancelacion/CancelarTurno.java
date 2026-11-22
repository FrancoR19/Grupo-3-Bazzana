package ar.odonto.turnos.application.cancelacion;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Turno;
import ar.odonto.turnos.domain.port.TurnoRepository;
import ar.odonto.turnos.domain.port.UsuarioActual;
import ar.odonto.turnos.domain.rules.cancelacion.ContextoDeCancelacion;
import ar.odonto.turnos.domain.rules.cancelacion.ValidadorDeCancelacion;
import ar.odonto.turnos.infrastructure.config.TurnosProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso "cancelar turno" (US-011, design D2/D3 de C-08), en una sola transacción:
 * cargar turno (404) → validar estado y anticipación (RN-CA-03/01) → transición a CANCELADO con
 * {@code canceladoEn} (RN-CA-02) → guardado. Sin borrado físico (RN-CA-04).
 */
@Service
public class CancelarTurno {

    private static final Logger log = LoggerFactory.getLogger(CancelarTurno.class);

    private final TurnoRepository turnos;
    private final TurnosProperties propiedades;
    private final UsuarioActual usuarioActual;
    private final Clock reloj;

    public CancelarTurno(TurnoRepository turnos, TurnosProperties propiedades, UsuarioActual usuarioActual,
            Clock reloj) {
        this.turnos = turnos;
        this.propiedades = propiedades;
        this.usuarioActual = usuarioActual;
        this.reloj = reloj;
    }

    @Transactional
    public TurnoCanceladoVista ejecutar(Long turnoId) {
        Turno turno = turnos.findById(turnoId)
                .orElseThrow(() -> new ReglaDeNegocioException(CodigoError.RECURSO_INEXISTENTE,
                        "No existe el turno indicado"));

        LocalDateTime ahora = LocalDateTime.now(reloj);
        ValidadorDeCancelacion.validar(new ContextoDeCancelacion(turno.getId(), turno.getEstado(), turno.getInicio(),
                ahora, propiedades.anticipacionMinimaCancelacion()));

        turno.marcarCancelado(ahora);
        turnos.save(turno);

        // Auditoría: solo IDs / usuario del operador, nunca datos de pacientes (D2)
        log.info("Turno {} cancelado por {}", turno.getId(), usuarioActual.solicitante().usuario());
        return TurnoCanceladoVista.de(turno);
    }
}
