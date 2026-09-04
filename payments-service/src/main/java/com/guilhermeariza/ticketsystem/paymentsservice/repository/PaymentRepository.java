package com.guilhermeariza.ticketsystem.paymentsservice.repository;

import com.guilhermeariza.ticketsystem.paymentsservice.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
