package ar.odonto.turnos.infrastructure.security;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Rol;
import ar.odonto.turnos.domain.model.Solicitante;
import ar.odonto.turnos.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link UsuarioActual} sobre el usuario autenticado en la sesión (rol + profesionalId).
 * Pruebas puras de unidad: inyecta el {@code SecurityContextHolder} a mano.
 */
class UsuarioActualImplTest {

    private final UsuarioActualImpl sut = new UsuarioActualImpl();

    @Test
    void odontologoConProfesionalDevuelveSolicitanteConRolYProfesionalId() {
        var profesional = mock(ar.odonto.turnos.domain.model.Profesional.class);
        when(profesional.getId()).thenReturn(7L);
        var usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("ana");
        when(usuario.getRol()).thenReturn(Rol.ODONTOLOGO);
        when(usuario.getProfesional()).thenReturn(profesional);
        autenticar(new UsuarioPrincipal(usuario));

        Solicitante solicitante = sut.solicitante();

        assertThat(solicitante.usuario()).isEqualTo("ana");
        assertThat(solicitante.rol()).isEqualTo(Rol.ODONTOLOGO);
        assertThat(solicitante.profesionalId()).isEqualTo(7L);
        limpiar();
    }

    @Test
    void secretariaSinProfesionalDevuelveProfesionalIdNulo() {
        var usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("secretaria");
        when(usuario.getRol()).thenReturn(Rol.SECRETARIA);
        when(usuario.getProfesional()).thenReturn(null);
        autenticar(new UsuarioPrincipal(usuario));

        Solicitante solicitante = sut.solicitante();

        assertThat(solicitante.usuario()).isEqualTo("secretaria");
        assertThat(solicitante.rol()).isEqualTo(Rol.SECRETARIA);
        assertThat(solicitante.profesionalId()).isNull();
        limpiar();
    }

    @Test
    void sinAutenticacionLanzaNoAutenticado() {
        limpiar();
        try {
            sut.solicitante();
            throw new AssertionError("debería haber lanzado NO_AUTENTICADO");
        } catch (ReglaDeNegocioException ex) {
            assertThat(ex.codigo()).isEqualTo(CodigoError.NO_AUTENTICADO);
        }
    }

    @Test
    void odontologoSinProfesionalLanzaAccesoDenegado() {
        var usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("ana");
        when(usuario.getRol()).thenReturn(Rol.ODONTOLOGO);
        when(usuario.getProfesional()).thenReturn(null);
        autenticar(new UsuarioPrincipal(usuario));

        try {
            sut.solicitante();
            throw new AssertionError("debería haber lanzado ACCESO_DENEGADO");
        } catch (ReglaDeNegocioException ex) {
            assertThat(ex.codigo()).isEqualTo(CodigoError.ACCESO_DENEGADO);
        }
        limpiar();
    }

    @Test
    void autoridadDesconocidaLanzaAccesoDenegado() {
        autenticarConAutoridades("intruso", "ROLE_INVENTADO");
        try {
            sut.solicitante();
            throw new AssertionError("debería haber lanzado ACCESO_DENEGADO");
        } catch (ReglaDeNegocioException ex) {
            assertThat(ex.codigo()).isEqualTo(CodigoError.ACCESO_DENEGADO);
        }
        limpiar();
    }

    @Test
    void autoridadConRolConocidoDevuelveSolicitanteConEseRol() {
        autenticarConAutoridades("secretaria", "ROLE_SECRETARIA");
        Solicitante solicitante = sut.solicitante();
        assertThat(solicitante.rol()).isEqualTo(Rol.SECRETARIA);
        limpiar();
    }

    private void autenticarConAutoridades(String username, String... autoridades) {
        var userDetails = User.withUsername(username).password("x")
                .authorities(java.util.Arrays.stream(autoridades)
                        .map(SimpleGrantedAuthority::new).toList())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private void autenticar(UsuarioPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private void limpiar() {
        SecurityContextHolder.clearContext();
    }
}
