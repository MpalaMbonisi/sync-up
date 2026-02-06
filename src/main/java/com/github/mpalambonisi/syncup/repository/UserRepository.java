package com.github.mpalambonisi.syncup.repository;

import com.github.mpalambonisi.syncup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String name);
}
