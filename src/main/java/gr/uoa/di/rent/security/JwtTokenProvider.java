package gr.uoa.di.rent.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

import static gr.uoa.di.rent.security.SecurityConstants.EXPIRATION_TIME;
import static gr.uoa.di.rent.security.SecurityConstants.SECRET;

/*
 * This utility class will be used for generating a JWT after a user logs in successfully,
 * and validating the JWT sent in the Authorization header of the requests.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    public String generateToken(Authentication authentication) {
        Principal principal = (Principal) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(Long.toString(principal.getUser().getId()))
                .claim("role", principal.getUser().getRole().getName())
                .claim("pending_provider", principal.getUser().getIsPendingProvider())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature.");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token.");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token.");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token.");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }

        return false;
    }

}
