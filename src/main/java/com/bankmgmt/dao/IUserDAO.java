package com.bankmgmt.dao;

import com.bankmgmt.model.User;

import java.util.List;
import java.util.Optional;

public interface IUserDAO {

    Optional<User> findByUsername(String username);

    Optional<User> findById(long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long insert(User user);

    void updatePassword(long userId, String passwordHash, String salt);

    void updateSecurityAnswer(long userId, String hash, String saltAnswer);

    void updateActive(long userId, boolean active);

    List<User> findAll();

    List<User> search(String query);
}
