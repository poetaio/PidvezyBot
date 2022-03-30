package services.http_services;

import io.jsonwebtoken.*;

import java.util.Date;

public class JwtService {

    public static String generateToken() {
        return Jwts.builder()
                .setSubject("admin")
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + 86400000))
                .signWith(SignatureAlgorithm.HS512, System.getenv("JWT_SECRET"))
                .compact();
    }

    public static void validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(System.getenv("JWT_SECRET")).parseClaimsJws(authToken);
        } catch (SignatureException e) {
            System.out.println("Invalid JWT signature: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("Invalid JWT token: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (ExpiredJwtException e) {
            System.out.println("JWT token is expired: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("JWT token is unsupported: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("JWT claims string is empty: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }


}
