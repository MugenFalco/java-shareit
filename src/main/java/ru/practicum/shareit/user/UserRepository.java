package ru.practicum.shareit.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    User update(User user);

    void deleteById(Long id);

    Optional<User> findById(Long id);

    List<User> findAll();

    boolean existsByEmail(String email);
}