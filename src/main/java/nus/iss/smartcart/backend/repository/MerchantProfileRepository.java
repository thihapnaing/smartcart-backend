package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.MerchantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantProfileRepository
        extends JpaRepository<MerchantProfile, Long> {

    Optional<MerchantProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByUen(String uen);
}