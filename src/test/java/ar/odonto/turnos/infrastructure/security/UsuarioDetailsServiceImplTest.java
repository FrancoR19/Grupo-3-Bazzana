package ar.odonto.turnos.infrastructure.security;

import ar.odonto.turnos.domain.model.Rol;
import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@code UserDetailsService} sobre {@link Usuario} (hash BCrypt, nunca texto plano).
 * Pruebas puras de unidad sin contexto de Spring.
 */
class UsuarioDetailsServiceImplTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioDetailsServiceImpl sut = new UsuarioDetailsServiceImpl(usuarios);

    @Test
    void cargaUsuarioExistenteConRolYHash() {
        Usuario usuario = new Usuario("secretaria", "$2a$10$hash", Rol.SECRETARIA, null);
        when(usuarios.findByUsername("secretaria")).thenReturn(Optional.of(usuario));

        UserDetails detalles = sut.loadUserByUsername("secretaria");

        assertThat(detalles.getUsername()).isEqualTo("secretaria");
        assertThat(detalles.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SECRETARIA");
        assertThat(detalles.isEnabled()).isTrue();
    }

    @Test
    void usuarioInexistenteLanzaUsernameNotFoundException() {
        when(usuarios.findByUsername("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.loadUserByUsername("noexiste"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
