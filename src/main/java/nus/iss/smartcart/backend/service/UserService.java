package nus.iss.smartcart.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nus.iss.smartcart.backend.dto.DeliveryPersonDto;
import nus.iss.smartcart.backend.model.UserRole;
import nus.iss.smartcart.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    /**
     * Returns users whose role is DELIVERY_MAN.
     */
    @Transactional(readOnly = true)
    public List<DeliveryPersonDto> getDeliveryPersonel() {
        return userRepository
                .findByRole(UserRole.DELIVERYMAN)
                .stream()
                .map(user -> new DeliveryPersonDto(
                        user.getId(),
                        user.getUsername()
                ))
                .toList();
    }
}