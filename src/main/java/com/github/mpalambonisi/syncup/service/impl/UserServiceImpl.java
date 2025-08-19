package com.github.mpalambonisi.syncup.service.impl;

import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.exception.UsernameExistsException;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User registerUser(UserRegistrationDTO dto) {

        // check if user exists in the database
        if(userRepository.findByUsername(dto.getUsername()).isPresent()){
            throw new UsernameExistsException("Username already in use.");
        }

        User newUser = new User(dto.getUsername(), dto.getFirstName(),
                dto.getLastName(), dto.getEmail(),
                passwordEncoder.encode(dto.getPassword())); // store encoded password

        return userRepository.save(newUser);
    }

}
