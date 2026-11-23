package ar.odonto.turnos.web.api.auth;

import ar.odonto.turnos.application.acceso.AutenticarUsuario;
import ar.odonto.turnos.domain.error.CodigoError;
import ar.odonto.turnos.domain.error.ReglaDeNegocioException;
import ar.odonto.turnos.domain.model.Usuario;
import ar.odonto.turnos.domain.port.UsuarioRepository;
import ar.odonto.turnos.infrastructure.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code POST /api/auth/login}: autentica contra {@link AutenticarUsuario}, establece la
 * sesión de Spring Security (sin JWT, IN-01) y devuelve 200. Credenciales inválidas ->
 * 401 {@code NO_AUTENTICADO}; ODONTOLOGO sin profesional -> 401 {@code NO_AUTENTICADO}
 * (RN-AU-04). El controlador no contiene lógica de negocio (J1): solo delega y
 * traduce a respuesta HTTP.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AutenticarUsuario autenticarUsuario;
    private final UsuarioRepository usuarios;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    public AuthController(AutenticarUsuario autenticarUsuario,
            UsuarioRepository usuarios,
            HttpSessionSecurityContextRepository securityContextRepository) {
        this.autenticarUsuario = autenticarUsuario;
        this.usuarios = usuarios;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest solicitud,
            HttpServletRequest request, HttpServletResponse response) {
        var identidad = autenticarUsuario.ejecutar(solicitud.username(), solicitud.password());
        // Establecer sesión de Spring Security para vistas Thymeleaf (IN-01, sin JWT).
        // El principal es un UsuarioPrincipal para que UsuarioActualImpl resuelva el Solicitante.
        Usuario usuario = usuarios.findByUsername(identidad.usuario())
                .orElseThrow(() -> new ReglaDeNegocioException(CodigoError.NO_AUTENTICADO,
                        "Usuario no encontrado: " + identidad.usuario()));
        var autoridades = List.of(
                new SimpleGrantedAuthority("ROLE_" + identidad.rol().name()));
        Authentication auth = new UsernamePasswordAuthenticationToken(new UsuarioPrincipal(usuario), null, autoridades);
        // Renovar el ID de sesión tras autenticación exitosa (mitiga fijación de sesión).
        // getSession() garantiza que exista una sesión antes de cambiarle el ID.
        request.getSession();
        request.changeSessionId();
        SecurityContextHolder.getContext().setAuthentication(auth);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        return ResponseEntity.ok(new LoginResponse(identidad.usuario(), identidad.rol().name(), identidad.profesionalId()));
    }
}
