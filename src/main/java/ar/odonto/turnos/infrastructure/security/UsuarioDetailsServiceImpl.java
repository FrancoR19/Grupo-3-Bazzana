package ar.odonto.turnos.infrastructure.security;

import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carga un {@link Usuario} por {@code username} para la autenticación de Spring Security.
 * Delega la validación de la contraseña en el {@code PasswordEncoder} de {@code PasswordConfig}
 * a través del {@code DaoAuthenticationProvider} auto-configurado por Spring Boot.
 * Nunca devuelve ni expone el texto plano de la contraseña (RN-AU-02).
 */
@Service
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public UsuarioDetailsServiceImpl(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        return new UsuarioPrincipal(usuario);
    }
}
