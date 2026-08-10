package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserProfileRepository userProfileRepository;

    @InjectMocks private UserProfileService userProfileService;

    @Test
    void getUserProfile_noUserProfileFound_throwsEntityNotFoundException() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> userProfileService.getProfileForDeliveryDetails(1L));
    }

    @Test
    void getUserProfile_userProfileFound_returnsPopulatedResponse() {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(userProfile));
        when(userProfile.getFirstName()).thenReturn("John");
        when(userProfile.getLastName()).thenReturn("Smith");
        when(userProfile.getAddress()).thenReturn("123 Rainbow Street");
        when(userProfile.getPhoneNumber()).thenReturn("91234567");
        UserProfileForDeliveryDetails response = userProfileService.getProfileForDeliveryDetails(1L);
        assertEquals("John", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        assertEquals("123 Rainbow Street", response.getAddress());
        assertEquals("91234567", response.getPhoneNumber());
    }
}
