package com.peertopeer.service.impl;

import io.jsonwebtoken.Jwts;
import com.peertopeer.entity.Users;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import com.peertopeer.service.JwtService;
import com.peertopeer.utils.JwtProperties;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.security.Key;


@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private Key key;
    private final JwtProperties jwtProperties;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    @Override
    public String generateToken(Users user) {
        return Jwts.builder()
                .setId(String.valueOf(user.getId()))
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    @Override
    public String extractId(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getId();
    }

    @Override
    public boolean isTokenValid(String token, String username) {
        String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    @Override
    public boolean isTokenExpired(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getExpiration().before(new Date());
    }
}
