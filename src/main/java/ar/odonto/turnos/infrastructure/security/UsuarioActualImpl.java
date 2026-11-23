package ar.odonto.turnos.infrastructure.security;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Rol;
import ar.odonto.turnos.domain.model.Solicitante;
import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioActual;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Identidad de quien hace la solicitud en curso, resuelta del usuario autenticado en la
 * sesión de Spring Security (reemplaza a {@code Solicitante.SISTEMA} de C-01).
 * <p>
 * La autenticación real de C-05 establece como {@code principal} un {@link UsuarioPrincipal},
 * por lo que la ruta principal mapea el {@link Usuario} a un {@link Solicitante} con rol y
 * {@code profesionalId}. Se acepta cualquier {@link UserDetails} (por ejemplo el que crea
 * {@code @WithMockUser} en tests) como principal derivando nombre y rol de sus autoridades.
 * <p>
 * RN-AU-04: un {@code ODONTOLOGO} sin {@code Profesional} vinculado no puede obtener un
 * {@link Solicitante} válido (se rechaza aunque las credenciales de login fueran correctas).
 */
@Service
public class UsuarioActualImpl implements UsuarioActual {

    @Override
    public Solicitante solicitante() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ReglaDeNegocioException(CodigoError.NO_AUTENTICADO,
                    "No hay usuario autenticado en la sesión");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UsuarioPrincipal usuarioPrincipal) {
            return solicitanteDeUsuario(usuarioPrincipal.getUsuario());
        }
        if (principal instanceof UserDetails detalles) {
            // Fallback para cualquier principal de Spring Security (p. ej. @WithMockUser en tests).
            return new Solicitante(detalles.getUsername(), rolDe(detalles.getAuthorities()), null);
        }
        throw new ReglaDeNegocioException(CodigoError.NO_AUTENTICADO, "Credenciales no reconocidas");
    }

    private Solicitante solicitanteDeUsuario(Usuario usuario) {
        if (usuario.getRol() == Rol.ODONTOLOGO && usuario.getProfesional() == null) {
            throw new ReglaDeNegocioException(CodigoError.ACCESO_DENEGADO,
                    "El odontólogo no está vinculado a un profesional (RN-AU-04)");
        }
        return new Solicitante(usuario.getUsername(), usuario.getRol(),
                usuario.getProfesional() != null ? usuario.getProfesional().getId() : null);
    }

    private Rol rolDe(Iterable<? extends GrantedAuthority> autoridades) {
        for (GrantedAuthority autoridad : autoridades) {
            String rol = autoridad.getAuthority();
            if (rol.startsWith("ROLE_")) {
                rol = rol.substring("ROLE_".length());
            }
            try {
                return Rol.valueOf(rol);
            } catch (IllegalArgumentException ignored) {
                // sigue buscando
            }
        }
        throw new ReglaDeNegocioException(CodigoError.ACCESO_DENEGADO,
                "Rol no reconocido en las autoridades del usuario");
    }
}
