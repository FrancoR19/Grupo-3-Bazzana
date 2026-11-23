package ar.odonto.turnos.infrastructure.security;

import ar.odonto.turnos.domain.model.Rol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Cadena de seguridad propia de C-05. Reemplaza a la permisiva de C-01
 * ({@code PermisivaSecurityAutoConfiguration.cadenaPermisiva}) mediante
 * {@code @ConditionalOnMissingBean}: como esta clase declara un bean
 * {@link SecurityFilterChain}, el de C-01 se retira sin editar archivos compartidos.
 * <p>
 * Aplica autenticación y autorización por ruta según la matriz RBAC de
 * {@code knowledge-base/03_actores_y_roles.md}. Rutas públicas sin autenticación;
 * todo lo demás requiere usuario autenticado (RN-AU-01, RN-AU-03).
 * <p>
 * CSRF desactivado: la ola 1 no tiene vistas Thymeleaf con forms (llega en C-09) y los
 * tests de los cambios previos no envían token CSRF; C-09 podrá volver a activarlo para
 * las vistas. No se emite JWT (IN-01): la autenticación se maneja con sesión de Spring
 * Security para las vistas y para el endpoint {@code POST /api/auth/login}.
 */
@Configuration(proxyBeanMethods = false)
public class SeguridadConfig {

    private static final String[] PUBLICAS = {
            "/login",
            "/api/auth/login",
            "/css/**",
            "/js/**",
            "/img/**",
            "/actuator/health",
            "/h2-console/**"
    };

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas (03_actores_y_roles.md §Rutas públicas)
                        .requestMatchers(PUBLICAS).permitAll()
                        // RBAC por ruta según knowledge-base/03_actores_y_roles.md
                        .requestMatchers(HttpMethod.GET, "/api/pacientes/**")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name())
                        .requestMatchers(HttpMethod.POST, "/api/pacientes")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name())
                        .requestMatchers(HttpMethod.PUT, "/api/pacientes/**")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name())
                        .requestMatchers(HttpMethod.GET, "/api/profesionales/**")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name(), Rol.ODONTOLOGO.name())
                        .requestMatchers(HttpMethod.POST, "/api/profesionales")
                                .hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/profesionales/**")
                                .hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/api/sillones/**")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name(), Rol.ODONTOLOGO.name())
                        .requestMatchers(HttpMethod.POST, "/api/sillones")
                                .hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/sillones/**")
                                .hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/api/prestaciones/**")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name(), Rol.ODONTOLOGO.name())
                        .requestMatchers(HttpMethod.POST, "/api/prestaciones")
                                .hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/prestaciones/**")
                                .hasRole(Rol.ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/api/consultorio/horario")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name(), Rol.ODONTOLOGO.name())
                        .requestMatchers(HttpMethod.PUT, "/api/consultorio/horario")
                                .hasRole(Rol.ADMIN.name())
                        // Link de WhatsApp: solo ADMIN y SECRETARIA (matriz RBAC; RN-WA).
                        // Regla específica ANTES de la general GET /api/turnos/** (first match wins).
                        .requestMatchers(HttpMethod.GET, "/api/turnos/*/whatsapp-link")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name())
                        // Turnos: lectura (todos autenticados); crear/cancelar (ADMIN / SECRETARIA).
                        .requestMatchers(HttpMethod.GET, "/api/turnos/**")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name(), Rol.ODONTOLOGO.name())
                        .requestMatchers(HttpMethod.POST, "/api/turnos/**")
                                .hasAnyRole(Rol.ADMIN.name(), Rol.SECRETARIA.name())
                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )
                // /login es la página por defecto de Spring Security (vista Thymeleaf en C-09).
                .formLogin(Customizer.withDefaults())
                // CSRF desactivado en esta fase (ver notas de la clase).
                .csrf(AbstractHttpConfigurer::disable)
                // Errores de seguridad como Problem Details con campo `codigo` (sin nuevos códigos).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                        .accessDeniedHandler(new RestAccessDeniedHandler())
                );
        return http.build();
    }

    @Bean
    HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
