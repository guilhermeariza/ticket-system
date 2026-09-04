package com.guilhermeariza.ticketsystem.ordersservice.repository;

import com.guilhermeariza.ticketsystem.ordersservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
