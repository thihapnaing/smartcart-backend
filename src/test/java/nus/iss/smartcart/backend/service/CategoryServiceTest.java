package nus.iss.smartcart.backend.service;

import nus.iss.smartcart.backend.dto.CategoryResponse;
import nus.iss.smartcart.backend.model.Category;
import nus.iss.smartcart.backend.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private CategoryService categoryService;

    @Test
    void getCategories_returnsPopulatedResponse() {
        Category category = mock(Category.class);
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(category.getId()).thenReturn(1L);
        when(category.getName()).thenReturn("Tops");

        List<CategoryResponse> responses = categoryService.getCategories();
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals("Tops", responses.get(0).getName());
    }
}
