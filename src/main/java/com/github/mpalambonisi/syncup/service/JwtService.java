package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.model.User;
import io.jsonwebtoken.Claims;
import java.util.function.Function;

public interface JwtService {
    String generateToken(User user);
    String extractUsername(String token);
    boolean isTokenValid(String token, User user);
    <T> T extractClaim(String token, Function<Claims, T> claimResolver);
}
