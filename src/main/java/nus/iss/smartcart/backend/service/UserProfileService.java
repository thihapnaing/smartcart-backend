package nus.iss.smartcart.backend.service;

import jakarta.persistence.EntityNotFoundException;
import nus.iss.smartcart.backend.dto.UserProfileForDeliveryDetails;
import nus.iss.smartcart.backend.model.UserProfile;
import nus.iss.smartcart.backend.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;
    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileForDeliveryDetails getProfileForDeliveryDetails(Long userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile is not found"));
        return UserProfileForDeliveryDetails.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .address(profile.getAddress())
                .phoneNumber(profile.getPhoneNumber())
                .build();
    }
}
