package nus.iss.smartcart.backend.repository;

//Author: Junior

import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    // AUTHOR: Htet Nandar(Grace) - powers AdminMerchantService's merchant list.
    List<User> findByRole(UserRole role);
}