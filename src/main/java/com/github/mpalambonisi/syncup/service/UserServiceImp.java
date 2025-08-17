package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService{

    UserRepository userRepository;

    @Override
    public User registerUser(UserRegistrationDTO dto) {
        // check if user exists in the database
        if (userRepository.existsByUsername(dto.getUsername())){
            throw new IllegalStateException("Username already in use.");
        }
        // encode password

        User newUser = new User(dto.getUsername(), dto.getFirstName(),
                dto.getLastName(), dto.getEmail(), dto.getPassword());

        return userRepository.save(newUser);
    }

}
