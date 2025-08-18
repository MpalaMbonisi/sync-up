package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.model.User;

public class JwtServiceImp implements JwtService{
    @Override
    public String generateToken(User user) {
        return null;
    }

    @Override
    public String extractUsername(String token) {
        return null;
    }

    @Override
    public boolean isTokenValid(String token, User user) {
        return false;
    }
}
