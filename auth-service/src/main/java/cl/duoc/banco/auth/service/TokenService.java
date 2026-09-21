package cl.duoc.banco.auth.service;

import cl.duoc.banco.auth.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final long expiracionSegundos;

    public TokenService(JwtEncoder encoder,
                        @Value("${security.jwt.expiration-seconds:1800}") long expiracionSegundos) {
        this.encoder = encoder;
        this.expiracionSegundos = expiracionSegundos;
    }

    public TokenResponse generar(UserDetails usuario) {
        Instant ahora = Instant.now();
        List<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("http://localhost:8081")   // Spring exige que "iss" sea una URL valida
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(expiracionSegundos))
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", expiracionSegundos, roles);
    }
}
