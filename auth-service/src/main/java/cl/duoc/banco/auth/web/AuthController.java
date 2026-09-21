package cl.duoc.banco.auth.web;

import cl.duoc.banco.auth.dto.LoginRequest;
import cl.duoc.banco.auth.dto.TokenResponse;
import cl.duoc.banco.auth.service.TokenService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserDetailsService usuarios;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthController(UserDetailsService usuarios, PasswordEncoder encoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    /** Autentica al usuario y entrega un JWT con sus roles. Limitado por RateLimiter (anti fuerza bruta). */
    @PostMapping("/login")
    @RateLimiter(name = "login", fallbackMethod = "loginLimitado")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        UserDetails usuario;
        try {
            usuario = usuarios.loadUserByUsername(request.username());
        } catch (UsernameNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }
        if (!encoder.matches(request.password(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }
        return tokens.generar(usuario);
    }

    public TokenResponse loginLimitado(LoginRequest request, RequestNotPermitted ex) {
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos de login. Intente nuevamente en un minuto.");
    }

    /** Devuelve la identidad contenida en el token (sirve para verificar el JWT). */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "usuario", jwt.getSubject(),
                "roles", jwt.getClaimAsStringList("roles"),
                "emitidoPor", jwt.getClaimAsString("iss"),
                "expira", jwt.getExpiresAt().toString());
    }
}
