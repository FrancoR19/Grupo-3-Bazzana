package ar.odonto.turnos.application.acceso;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Rol;
import ar.odonto.turnos.domain.model.Solicitante;
import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Caso de uso de autenticación: valida credenciales contra {@link UsuarioRepository}
 * + {@link PasswordEncoder} (BCrypt) y devuelve el {@link Solicitante}.
 * Pruebas puras de unidad sin contexto de Spring.
 */
class AutenticarUsuarioTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AutenticarUsuario sut = new AutenticarUsuario(usuarios, encoder);

    @Test
    void credencialesValidasDevuelvenSolicitante() {
        var usuario = new Usuario("secretaria", "hash", Rol.SECRETARIA, null);
        when(usuarios.findByUsername("secretaria")).thenReturn(Optional.of(usuario));
        when(encoder.matches("secre123", "hash")).thenReturn(true);

        Solicitante solicitante = sut.ejecutar("secretaria", "secre123");

        assertThat(solicitante.usuario()).isEqualTo("secretaria");
        assertThat(solicitante.rol()).isEqualTo(Rol.SECRETARIA);
        assertThat(solicitante.profesionalId()).isNull();
    }

    @Test
    void credencialesValidasOdontologoDevuelvenProfesionalId() {
        var profesional = mock(ar.odonto.turnos.domain.model.Profesional.class);
        when(profesional.getId()).thenReturn(7L);
        var usuario = new Usuario("ana", "hash", Rol.ODONTOLOGO, profesional);
        when(usuarios.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(encoder.matches("odont123", "hash")).thenReturn(true);

        Solicitante solicitante = sut.ejecutar("ana", "odont123");

        assertThat(solicitante.rol()).isEqualTo(Rol.ODONTOLOGO);
        assertThat(solicitante.profesionalId()).isEqualTo(7L);
    }

    @Test
    void contrasenaIncorrectaLanzaNoAutenticado() {
        var usuario = new Usuario("secretaria", "hash", Rol.SECRETARIA, null);
        when(usuarios.findByUsername("secretaria")).thenReturn(Optional.of(usuario));
        when(encoder.matches("mal", "hash")).thenReturn(false);

        try {
            sut.ejecutar("secretaria", "mal");
            throw new AssertionError("debería haber lanzado NO_AUTENTICADO");
        } catch (ReglaDeNegocioException ex) {
            assertThat(ex.codigo()).isEqualTo(CodigoError.NO_AUTENTICADO);
        }
    }

    @Test
    void usuarioInexistenteLanzaNoAutenticado() {
        when(usuarios.findByUsername("noexiste")).thenReturn(Optional.empty());

        try {
            sut.ejecutar("noexiste", "x");
            throw new AssertionError("debería haber lanzado NO_AUTENTICADO");
        } catch (ReglaDeNegocioException ex) {
            assertThat(ex.codigo()).isEqualTo(CodigoError.NO_AUTENTICADO);
        }
    }

    @Test
    void usuarioDadoDeBajaLanzaAccesoDenegado() {
        var usuario = new Usuario("dadoBaja", "hash", Rol.SECRETARIA, null);
        usuario.desactivar();
        when(usuarios.findByUsername("dadoBaja")).thenReturn(Optional.of(usuario));
        when(encoder.matches("secre123", "hash")).thenReturn(true);

        try {
            sut.ejecutar("dadoBaja", "secre123");
            throw new AssertionError("debería haber lanzado ACCESO_DENEGADO");
        } catch (ReglaDeNegocioException ex) {
            assertThat(ex.codigo()).isEqualTo(CodigoError.ACCESO_DENEGADO);
        }
    }

    @Test
    void odontologoSinProfesionalLanzaNoAutenticado() {
        var usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("ana");
        when(usuario.getRol()).thenReturn(Rol.ODONTOLOGO);
        when(usuario.getProfesional()).thenReturn(null);
        when(usuarios.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(encoder.matches("odont123", "hash")).thenReturn(true);

        try {
            sut.ejecutar("ana", "odont123");
            throw new AssertionError("debería haber lanzado NO_AUTENTICADO");
        } catch (ReglaDeNegocioException ex) {
            assertThat(ex.codigo()).isEqualTo(CodigoError.NO_AUTENTICADO);
        }
    }
}
