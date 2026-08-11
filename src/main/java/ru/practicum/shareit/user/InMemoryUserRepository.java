package ru.practicum.shareit.user;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<Long, User> users = new HashMap<>();
    private final Set<String> emails = new HashSet<>();
    private long nextId = 1;

    @Override
    public User save(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        emails.add(normalize(user.getEmail()));
        return user;
    }

    @Override
    public User update(User user) {
        User oldUser = users.get(user.getId());
        if (oldUser != null) {
            emails.remove(normalize(oldUser.getEmail()));
        }
        users.put(user.getId(), user);
        emails.add(normalize(user.getEmail()));
        return user;
    }

    @Override
    public void deleteById(Long id) {
        User removed = users.remove(id);
        if (removed != null) {
            emails.remove(normalize(removed.getEmail()));
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id))
                .map(user -> new User(user.getId(), user.getName(), user.getEmail()));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public boolean existsByEmail(String email) {
        return emails.contains(normalize(email));
    }

    private String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}