package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
