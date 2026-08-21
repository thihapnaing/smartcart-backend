package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CategoryResponse;
import nus.iss.smartcart.backend.model.Category;
import nus.iss.smartcart.backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        List<Category> categoryList = categoryRepository.findAll();
        return categoryList.stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
