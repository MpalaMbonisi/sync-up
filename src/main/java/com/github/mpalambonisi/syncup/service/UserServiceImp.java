package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImp implements UserService{
    @Override
    public User registerUser(UserRegistrationDTO dto) {
        if (false)
            throw new IllegalStateException();
        return null;
    }

}
