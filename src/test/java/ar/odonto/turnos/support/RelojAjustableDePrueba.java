package ar.odonto.turnos.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj de test que se puede mover entre escenarios (por ejemplo "ahora 2026-11-22T20:00" y
 * "ahora 2026-11-23T10:00"), en la zona del consultorio. Espejo del {@code RelojAjustable} de C-02,
 * hecho público para los tests de C-08 fuera del paquete {@code web.api.turnos} (RN-GL-02).
 */
public final class RelojAjustableDePrueba extends Clock {

    public static final ZoneId ZONA_AR = ZoneId.of("America/Argentina/Buenos_Aires");

    private volatile Instant instante;

    public RelojAjustableDePrueba(LocalDateTime ahora) {
        fijar(ahora);
    }

    public void fijar(LocalDateTime ahora) {
        this.instante = ahora.atZone(ZONA_AR).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return ZONA_AR;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
