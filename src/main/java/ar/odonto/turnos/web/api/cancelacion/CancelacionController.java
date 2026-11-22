package ar.odonto.turnos.web.api.cancelacion;

import ar.odonto.turnos.application.cancelacion.CancelarTurno;
import ar.odonto.turnos.application.cancelacion.TurnoCanceladoVista;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de cancelación de turnos (C-08). Controlador PROPIO: no edita el de C-02 (reglas anti-colisión de
 * CHANGES.md). Sin lógica de negocio (regla J1): solo delega en el caso de uso; los errores los traduce
 * el manejador global de C-01 a Problem Details con {@code codigo} (404 / 422).
 */
@RestController
@RequestMapping("/api/turnos")
class CancelacionController {

    private final CancelarTurno cancelarTurno;

    CancelacionController(CancelarTurno cancelarTurno) {
        this.cancelarTurno = cancelarTurno;
    }

    @PostMapping("/{id}/cancelacion")
    ResponseEntity<TurnoCanceladoResponse> cancelar(@PathVariable Long id) {
        TurnoCanceladoVista cancelado = cancelarTurno.ejecutar(id);
        return ResponseEntity.ok(TurnoCanceladoResponse.de(cancelado));
    }
}
