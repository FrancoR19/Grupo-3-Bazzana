package ar.odonto.turnos.web.api.pacientes;

/**
 * Fixtures de pacientes SIEMPRE ficticios (RN-DP-01 / regla D1): DNI formato 0000000X y
 * teléfono 549110000000X. Nunca usar datos reales de pacientes.
 */
final class DatosDePacientesDePrueba {

    static final String NOMBRE = "Paciente";
    static final String APELLIDO = "Uno";
    static final String DNI = "00000001";
    static final String TELEFONO = "5491100000001";
    static final String EMAIL = "paciente.uno@example.com";
    static final String NOMBRE_COMPLETO = NOMBRE + " " + APELLIDO;

    private DatosDePacientesDePrueba() {
    }

    static String altaCompleta() {
        return """
                {"nombre":"%s","apellido":"%s","dni":"%s","telefono":"%s","email":"%s"}
                """.formatted(NOMBRE, APELLIDO, DNI, TELEFONO, EMAIL);
    }
}
