package com.github.mpalambonisi.syncup.service;


import com.github.mpalambonisi.syncup.dto.UserRegistrationDTO;
import com.github.mpalambonisi.syncup.model.User;

public interface UserService {
    User registerUser(UserRegistrationDTO dto);
}
