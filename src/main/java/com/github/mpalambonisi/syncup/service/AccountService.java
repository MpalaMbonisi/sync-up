package com.github.mpalambonisi.syncup.service;

import com.github.mpalambonisi.syncup.model.User;

public interface AccountService {
    User getAccountDetails(User user);
    void deleteAccount(User user);
}
