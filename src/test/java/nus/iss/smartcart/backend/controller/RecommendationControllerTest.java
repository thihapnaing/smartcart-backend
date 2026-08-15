package nus.iss.smartcart.backend.controller;

import nus.iss.smartcart.backend.model.User;
import nus.iss.smartcart.backend.repository.UserRepository;
import nus.iss.smartcart.backend.service.RecommendationOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import nus.iss.smartcart.backend.security.JwtAuthenticationFilter;


@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RecommendationOrchestratorService recommendationService;

    // Add the new UserRepository mock
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void testGetRecommendations() throws Exception {
        // Mock the authenticated user's Principal (the JWT token representation)
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("grace@example.com");

        // Mock the UserRepository to return a valid User when findByEmail is called
        User mockUser = new User();
        mockUser.setId(2L); // Simulating Grace's user ID
        mockUser.setEmail("grace@example.com");
        when(userRepository.findByEmail("grace@example.com")).thenReturn(Optional.of(mockUser));

        // Mock your service response
        when(recommendationService.getRecommendationsForUser(anyLong())).thenReturn(null); 

        // Perform the GET request WITHOUT the /{userId} in the URL, passing the mocked Principal
        mockMvc.perform(get("/api/v1/recommendations")
                .principal(mockPrincipal))
                .andExpect(status().isOk());
    }
}