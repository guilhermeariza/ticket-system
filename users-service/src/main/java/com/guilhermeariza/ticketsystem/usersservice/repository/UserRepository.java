package com.guilhermeariza.ticketsystem.usersservice.repository;

import com.guilhermeariza.ticketsystem.usersservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
