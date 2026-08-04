package nus.iss.smartcart.backend.repository;

import nus.iss.smartcart.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
