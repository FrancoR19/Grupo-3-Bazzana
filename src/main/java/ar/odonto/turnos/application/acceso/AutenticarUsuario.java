package ar.odonto.turnos.application.acceso;

import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Rol;
import ar.odonto.turnos.domain.model.Solicitante;
import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de autenticación: valida credenciales contra {@link UsuarioRepository} y
 * {@link PasswordEncoder} (BCrypt) y devuelve la identidad del solicitante
 * ({@link Solicitante}: rol y {@code profesionalId} vinculado).
 * <p>
 * No expone el hash ni el texto plano de la contraseña (RN-AU-02).
 * RN-AU-04: un {@code ODONTOLOGO} sin {@code Profesional} vinculado se rechaza.
 */
@Service
@Transactional(readOnly = true)
public class AutenticarUsuario {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public AutenticarUsuario(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    public Solicitante ejecutar(String username, String password) {
        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new ReglaDeNegocioException(CodigoError.NO_AUTENTICADO,
                        "Usuario o contraseña inválidos"));
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new ReglaDeNegocioException(CodigoError.NO_AUTENTICADO,
                    "Usuario o contraseña inválidos");
        }
        if (!usuario.isActivo()) {
            throw new ReglaDeNegocioException(CodigoError.ACCESO_DENEGADO,
                    "El usuario está dado de baja");
        }
        if (usuario.getRol() == Rol.ODONTOLOGO && usuario.getProfesional() == null) {
            throw new ReglaDeNegocioException(CodigoError.NO_AUTENTICADO,
                    "El odontólogo no está vinculado a un profesional (RN-AU-04)");
        }
        return new Solicitante(usuario.getUsername(), usuario.getRol(),
                usuario.getProfesional() != null ? usuario.getProfesional().getId() : null);
    }
}
