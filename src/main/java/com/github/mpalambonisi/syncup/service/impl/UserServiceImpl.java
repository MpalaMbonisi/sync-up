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
        String username = dto.getUsername().toLowerCase().trim();
        if(userRepository.findByUsername(username).isPresent()){
            throw new UsernameExistsException("Username already in use.");
        }

        return userRepository.save(createUserFromDTO(dto));
    }

    private User createUserFromDTO(UserRegistrationDTO dto){

        User user = new User();
        user.setUsername(dto.getUsername().toLowerCase().trim());
        user.setFirstName(capitalise(dto.getFirstName()));
        user.setLastName(capitalise(dto.getLastName()));
        user.setEmail(dto.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        return user;
    }

    private String capitalise(String word) {
        if(word == null || word.isEmpty()) return word;

        String trimmedWord = word.trim();

        if(trimmedWord.length() == 1) return trimmedWord.toUpperCase();

        return trimmedWord.substring(0,1).toUpperCase() + trimmedWord.substring(1).toLowerCase();
    }
}
