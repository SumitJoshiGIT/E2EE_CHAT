package com.e2ee.chat.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException; // Update to use the correct exception
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.e2ee.chat.model.User; // Import the User class
import org.bson.types.ObjectId;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final int jwtExpirationInMs;

    public JwtTokenProvider(@Value("${jwt.expiration}") int jwtExpirationInMs) {
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        this.jwtExpirationInMs = jwtExpirationInMs;
    }

    public String generateToken(Authentication authentication) {
        User user = (User) authentication.getPrincipal(); // Cast to User to access the profile
        if (user.getProfile() == null) {
            throw new IllegalStateException("User profile is not initialized for user: " + user.getUsername());
        }
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(user.getProfile().getId().toHexString()) // Use ObjectId as hex string
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getPidFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject(); // This will be the ObjectId hex string
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            // Invalid JWT signature
            return false;
        } catch (MalformedJwtException ex) {
            // Invalid JWT token
            return false;
        } catch (ExpiredJwtException ex) {
            // Expired JWT token
            return false;
        } catch (UnsupportedJwtException ex) {
            // Unsupported JWT token
            return false;
        } catch (IllegalArgumentException ex) {
            // JWT claims string is empty
            return false;
        }
    }
}