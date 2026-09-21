package cl.duoc.banco.auth.dto;

import java.util.List;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, List<String> roles) {
}
