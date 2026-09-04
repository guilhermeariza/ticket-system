package com.guilhermeariza.ticketsystem.eventsservice.repository;

import com.guilhermeariza.ticketsystem.eventsservice.model.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TicketType t SET t.availableQuantity = t.availableQuantity - :quantity " +
           "WHERE t.id = :id AND t.availableQuantity >= :quantity")
    int decrementIfAvailable(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TicketType t SET t.availableQuantity = t.availableQuantity + :quantity WHERE t.id = :id")
    int incrementAvailable(@Param("id") Long id, @Param("quantity") int quantity);
}
