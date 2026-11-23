package ar.odonto.turnos.infrastructure.security;

import ar.odonto.turnos.domain.error.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Punto de entrada de autenticación REST. Cuando Spring Security rechaza una petición
 * por falta de autenticación (401), devuelve un Problem Detail (RFC 7807) con el campo
 * {@code codigo} de {@link CodigoError} (sin agregar códigos nuevos).
 * No incluye trazas ni datos personales (D1/D2).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        escribirProblema(response, HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO);
    }

    private void escribirProblema(HttpServletResponse response, HttpStatus estado, CodigoError codigo)
            throws IOException {
        String cuerpo = "{ \"type\": \"about:blank\", \"title\": \"%s\", \"status\": %d, \"detail\": \"%s\", \"codigo\": \"%s\" }"
                .formatted(escape(codigo.titulo()), estado.value(), escape(codigo.titulo()), codigo.name());
        response.setStatus(estado.value());
        response.setContentType("application/problem+json;charset=UTF-8");
        response.getWriter().write(cuerpo);
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
