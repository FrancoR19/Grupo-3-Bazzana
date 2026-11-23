package ar.odonto.turnos.infrastructure.security;

import ar.odonto.turnos.domain.error.CodigoError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Manejador de acceso denegado REST. Cuando Spring Security rechaza una petición por falta
 * de permiso (403), devuelve un Problem Detail con el campo {@code codigo}
 * {@code ACCESO_DENEGADO} (sin agregar códigos nuevos).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escribirProblema(response, HttpStatus.FORBIDDEN, CodigoError.ACCESO_DENEGADO);
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
