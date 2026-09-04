package com.guilhermeariza.ticketsystem.eventsservice.repository;

import com.guilhermeariza.ticketsystem.eventsservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
}
