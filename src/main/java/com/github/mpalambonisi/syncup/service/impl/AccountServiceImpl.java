package com.github.mpalambonisi.syncup.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.mpalambonisi.syncup.exception.UserNotFoundException;
import com.github.mpalambonisi.syncup.model.User;
import com.github.mpalambonisi.syncup.repository.TaskListRepository;
import com.github.mpalambonisi.syncup.repository.UserRepository;
import com.github.mpalambonisi.syncup.service.AccountService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final TaskListRepository taskListRepository;

    @Override
    @Transactional(readOnly = true)
    public User getAccountDetails(User user) {
        return userRepository.findById(user.getId())
        .orElseThrow(() -> new UserNotFoundException("User not found!"));
    }

    @Override
    public void deleteAccount(User user) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteAccount'");
    }


}
