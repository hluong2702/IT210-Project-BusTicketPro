package org.example.busticketpro.repository;

import org.example.busticketpro.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTicketId(Long ticketId);
    Optional<Payment> findByTransactionId(String transactionId);
}
