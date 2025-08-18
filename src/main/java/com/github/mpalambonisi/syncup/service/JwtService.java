package com.github.mpalambonisi.syncup.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Function;

public interface JwtService {
    String generateToken(UserDetails user);
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails user);
    <T> T extractClaim(String token, Function<Claims, T> claimResolver);
}
