package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
