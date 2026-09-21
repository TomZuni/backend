package cl.duoc.banco.historial.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Propaga el JWT del usuario a las llamadas entre microservicios, de modo que
 * cuentas-service tambien autentica y autoriza al usuario original.
 */
@Component
public class BearerTokenRelayInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate plantilla) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            plantilla.header("Authorization", "Bearer " + jwt.getToken().getTokenValue());
        }
    }
}
